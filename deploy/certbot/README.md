# Keystore SSL del backend (renovación automática)

El backend sirve HTTPS (puertos 8382 prod / 8383 test) con un keystore PKCS12 **que ya no va dentro del jar**.
El keystore se genera en el VPS desde el certificado Let's Encrypt de `dtodojalapa.xyz`
(`/etc/letsencrypt/live/dtodojalapa.xyz/`) y se guarda en `/var/apps/dtodo/ssl/dtodo.p12`.

## Cómo funciona

- `dtodo-p12-deploy-hook.sh` convierte `fullchain.pem` + `privkey.pem` a `.p12` (alias `dtodo`), valida que
  se pueda abrir y lo reemplaza de forma atómica.
- Se instala en `/etc/letsencrypt/renewal-hooks/deploy/dtodo-p12.sh`. Certbot lo ejecuta solo después de
  cada renovación exitosa; el hook reinicia `dtodo-api.service` y `dtodo-dev-api.service`.
- El workflow `Deploy` reinstala el hook y refresca el `.p12` en cada despliegue (sin reiniciar nada: el
  despliegue reinicia el servicio), por lo que el primer despliegue tras este cambio deja todo configurado.
- `application-prod.properties` y `application-test.properties` leen la ruta y la clave de
  `DTODO_SSL_KEYSTORE` y `DTODO_SSL_KEYSTORE_PASSWORD` (por defecto `file:/var/apps/dtodo/ssl/dtodo.p12` y `12345`).

## Requisitos en el VPS

- `certbot-renew.timer` habilitado (`systemctl enable --now certbot-renew.timer`).
- El usuario SSH del workflow (`VPS_USER`) debe poder ejecutar `sudo -n` sin contraseña (como mínimo
  `install` y el propio hook); si es `root` no hay nada que hacer.
- `setfacl` disponible (paquete `acl`) si algún servicio del backend corre con un usuario distinto de root.

## Operación manual

Regenerar el keystore y reiniciar los servicios:

```bash
sudo /etc/letsencrypt/renewal-hooks/deploy/dtodo-p12.sh
```

Probar la cadena completa (renovación simulada contra el entorno de pruebas de Let's Encrypt, ejecutando el hook):

```bash
sudo certbot renew --cert-name dtodojalapa.xyz --dry-run --run-deploy-hooks
```
