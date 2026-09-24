# Frontend

Aplicación Angular 13. Requiere Node.js 16.20.2 y pnpm 8.15.9.

## Desarrollo

```sh
pnpm install --frozen-lockfile
pnpm start
```

La aplicación se sirve en `http://localhost:4200/` y usa el backend local configurado en `src/environments/environment.ts`.

## Verificación

```sh
pnpm run build:test
pnpm run build:prod
pnpm run lint
pnpm run test -- --watch=false --browsers=ChromeHeadless
```

En entornos donde Chrome Headless requiera desactivar su sandbox, se puede usar `--browsers=ChromeHeadlessNoSandbox` para las pruebas locales. El build queda en `dist/frontend`.

Los tests de navegador heredados siguen disponibles con `pnpm run e2e` y requieren que el backend esté disponible.
