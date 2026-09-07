// This file can be replaced during build by using the `fileReplacements` array.
// `ng build --configuration production` replaces `environment.ts` with `environment.prod.ts`.
// `ng build --configuration test` replaces it with `environment.test.ts`.
// The list of file replacements can be found in `angular.json`.

export const environment = {
  production: false,
  /** Backend corriendo en la máquina local (perfil `dev`/`test` del backend, según cómo lo hayas levantado). */
  apiUrl: 'http://localhost:8383'
};

/*
 * For easier debugging in development mode, you can import the following file
 * to ignore zone related error stack frames such as `zone.run`, `zoneDelegate.invokeTask`.
 *
 * This import should be commented out in production mode because it will have a negative impact
 * on performance if an error is thrown.
 */
// import 'zone.js/dist/zone-error';  // Included with Angular CLI.
