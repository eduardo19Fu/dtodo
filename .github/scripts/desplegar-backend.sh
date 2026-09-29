#!/usr/bin/env bash
# Se ejecuta EN EL VPS (via `ssh ... 'bash -s' -- <args> < desplegar-backend.sh`).
# Reemplaza el jar, reinicia el servicio y espera a que la API responda. Si no responde
# (por ejemplo, porque el VerificadorEsquema detecto que falta correr un script SQL),
# restaura el jar anterior y termina con error para que el workflow quede en rojo.
#
# Uso: desplegar-backend.sh <directorio> <jar> <servicio systemd> <puerto> <archivo de log>
set -euo pipefail

DIR="$1"
JAR="$DIR/$2"
SERVICIO="$3"
PUERTO="$4"
LOG="$5"
ESPERA_MAXIMA="${ESPERA_MAXIMA:-180}"
INTERVALO="${INTERVALO:-5}"

api_responde() {
  # Cualquier codigo HTTP (incluido 401/404) indica que Tomcat ya levanto; 000 = sin conexion.
  local codigo
  codigo=$(curl -sk -o /dev/null -w '%{http_code}' --max-time 5 "https://localhost:$PUERTO/api/" || true)
  [ "$codigo" != "000" ]
}

esperar_api() {
  local transcurrido=0
  while [ "$transcurrido" -lt "$ESPERA_MAXIMA" ]; do
    sleep "$INTERVALO"
    transcurrido=$((transcurrido + INTERVALO))
    if api_responde; then
      echo "API respondiendo en el puerto $PUERTO tras ${transcurrido}s"
      return 0
    fi
  done
  return 1
}

if [ -f "$JAR" ]; then
  cp -p "$JAR" "$JAR.prev"
fi
mv "$JAR.new" "$JAR"
sudo systemctl restart "$SERVICIO"

if esperar_api; then
  exit 0
fi

echo "::error::La API no respondio en ${ESPERA_MAXIMA}s. Ultimas lineas del log:"
tail -n 60 "$LOG" 2>/dev/null || echo "(no se pudo leer $LOG)"

if [ ! -f "$JAR.prev" ]; then
  echo "::error::No hay jar anterior para restaurar."
  exit 1
fi

echo "Restaurando el jar anterior..."
mv "$JAR.prev" "$JAR"
sudo systemctl restart "$SERVICIO"
if esperar_api; then
  echo "::error::Despliegue revertido: el jar anterior esta en linea. Revisa el log de arriba (¿falta un script SQL?)."
else
  echo "::error::Despliegue revertido, pero el jar anterior TAMPOCO responde. Revisar el servidor de inmediato."
fi
exit 1
