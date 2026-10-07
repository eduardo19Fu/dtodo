#!/usr/bin/env bash
# Genera el keystore PKCS12 que usa el backend (Spring Boot) a partir del certificado Let's Encrypt
# de dtodojalapa.xyz. Se ejecuta EN EL VPS, como root, de dos formas:
#   1. Como deploy hook de certbot (/etc/letsencrypt/renewal-hooks/deploy/): certbot lo invoca solo
#      despues de cada renovacion exitosa y define RENEWED_LINEAGE. Regenera el .p12 y reinicia el backend.
#   2. Desde el workflow de despliegue (con DTODO_NO_RESTART=1) para instalar/refrescar el .p12 antes de
#      reiniciar el jar; ahi es el propio despliegue quien reinicia el servicio.
#
# Variables opcionales:
#   DTODO_CERT_NAME            nombre del certificado en certbot            (dtodojalapa.xyz)
#   DTODO_SSL_KEYSTORE_FILE    ruta de salida del .p12                      (/var/apps/dtodo/ssl/dtodo.p12)
#   DTODO_SSL_KEYSTORE_PASSWORD clave del .p12 (debe coincidir con el backend) (12345)
#   DTODO_API_SERVICES         servicios systemd a reiniciar                (dtodo-api.service dtodo-dev-api.service)
#   DTODO_NO_RESTART=1         no reinicia ningun servicio
set -euo pipefail

CERT_NAME="${DTODO_CERT_NAME:-dtodojalapa.xyz}"
SALIDA="${DTODO_SSL_KEYSTORE_FILE:-/var/apps/dtodo/ssl/dtodo.p12}"
CLAVE="${DTODO_SSL_KEYSTORE_PASSWORD:-12345}"
SERVICIOS="${DTODO_API_SERVICES:-dtodo-api.service dtodo-dev-api.service}"
ALIAS="dtodo"

# Certbot corre este hook por cada certificado renovado; solo nos interesa el de la API.
if [ -n "${RENEWED_LINEAGE:-}" ] && [ "$(basename "$RENEWED_LINEAGE")" != "$CERT_NAME" ]; then
  echo "Certificado $(basename "$RENEWED_LINEAGE") no es $CERT_NAME; no se regenera el keystore."
  exit 0
fi

LINEAGE="${RENEWED_LINEAGE:-/etc/letsencrypt/live/$CERT_NAME}"
for archivo in fullchain.pem privkey.pem; do
  [ -r "$LINEAGE/$archivo" ] || { echo "ERROR: no se puede leer $LINEAGE/$archivo" >&2; exit 1; }
done

# El certificado debe estar vigente al menos 1 dia; si no, no tiene sentido publicarlo.
openssl x509 -in "$LINEAGE/fullchain.pem" -noout -checkend 86400 >/dev/null \
  || { echo "ERROR: el certificado de $LINEAGE esta vencido o por vencer en menos de 1 dia" >&2; exit 1; }

DIR="$(dirname "$SALIDA")"
install -d -m 0755 "$DIR"
TMP="$(mktemp "$DIR/.dtodo-p12.XXXXXX")"
trap 'rm -f "$TMP"' EXIT

openssl pkcs12 -export -in "$LINEAGE/fullchain.pem" -inkey "$LINEAGE/privkey.pem" \
  -name "$ALIAS" -passout "pass:$CLAVE" -out "$TMP"

# Validar que el .p12 se abre con la clave esperada antes de reemplazar el vigente.
openssl pkcs12 -in "$TMP" -passin "pass:$CLAVE" -nokeys -clcerts 2>/dev/null \
  | openssl x509 -noout >/dev/null

# Lectura solo para root y para el usuario con el que corre cada servicio del backend.
chmod 0600 "$TMP"
for svc in $SERVICIOS; do
  usuario="$(systemctl show -p User --value "$svc" 2>/dev/null || true)"
  if [ -n "$usuario" ] && [ "$usuario" != "root" ]; then
    setfacl -m "u:$usuario:r" "$TMP"
  fi
done

mv -f "$TMP" "$SALIDA"
trap - EXIT
echo "Keystore actualizado: $SALIDA ($(openssl pkcs12 -in "$SALIDA" -passin "pass:$CLAVE" -nokeys -clcerts 2>/dev/null | openssl x509 -noout -enddate))"

if [ "${DTODO_NO_RESTART:-0}" = "1" ]; then
  exit 0
fi

for svc in $SERVICIOS; do
  # list-unit-files: el servicio puede no existir en este servidor (p. ej. sin entorno de pruebas).
  if systemctl list-unit-files "$svc" --no-legend 2>/dev/null | grep -q "$svc"; then
    echo "Reiniciando $svc"
    systemctl restart "$svc"
  fi
done
