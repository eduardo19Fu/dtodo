# Fotografías de productos: diagnóstico y propuesta

Revisión inicial de código: 12 de octubre de 2026. Las secciones de diagnóstico conservan los hallazgos iniciales. Los cambios realizados después se describen al final.

## Base Git verificada

- `git fetch origin` completado.
- Rama creada: `codex/fotografias-productos`, desde `develop` (`e16bf4ee0fa16759471543d5a9582b937b9aced5`).
- `develop` y `origin/develop` coinciden: `git rev-list --left-right --count develop...origin/develop` devuelve `0 0`.
- `origin/main` (`faaf0a5f29364137bb073a30b0d0362b9042c3aa`) está contenido en `develop`: `git rev-list --left-right --count origin/main...origin/develop` devuelve `0 2`. No hace falta integrar cambios de main.
- Los documentos no rastreados que ya estaban en el checkout se conservaron.

## Qué existe y se puede reutilizar

| Pieza | Evidencia en el repositorio | Reutilización |
| --- | --- | --- |
| Endpoint multipart | `Backend/dtodo-backend/src/main/java/xyz/pangosoft/dtodo/controller/ProductoApiController.java`, `POST /api/productos/upload`, parámetros `file` e `id` | Mantener contrato y respuesta `{ mensaje, producto }`. |
| Persistencia de referencia | `model/Producto.java`, campo `imagen`; `service/impl/ProductoServiceImpl.java`, método `upload` | Una foto por producto, con reemplazo. La imagen binaria queda fuera de la base de datos. |
| Almacenamiento | `service/impl/UploadFileServiceImpl.java` | Copia a `uploads/` con UUID; carga y eliminación. Requiere correcciones antes de reutilizarse sin reservas. |
| Lectura de imágenes | `GET /api/uploads/img/{nombreImagen}`; `auth/SecurityConfig.java` permite acceso público | Usar en `<img>` con URL del entorno. Las fotos actualmente son públicas. |
| Cliente multipart | `Frontend/src/app/services/producto.service.ts`, `uploadImage` | `FormData` + `HttpRequest` con progreso. Dejar que el navegador genere el Content-Type y boundary. |
| Modal de detalle | `components/productos/detail-producto/` | Ya incluye imagen, selector, botón de carga y progreso. Emite `productoActualizado`. |
| Datos para miniaturas | `dto/ProductoDtoMejorado.java`, `repository/IProductoRepository.java`, ambos mapeos en `ProductoServiceImpl`; `Frontend/src/app/dtos/productoDto.ts` | Listado paginado y búsqueda ya incluyen `imagen`: no pedir el detalle de cada fila. |
| Refresco de listado | `ProductosComponent.actualizarProducto` | Ya recarga la página actual después de subir desde el detalle. |

El límite global multipart declarado en `application.properties` es 10 MB para archivo y petición. No equivale a validación de fotografía. El proyecto real declara Angular 13.4 en `Frontend/package.json`, aunque AGENTS.md menciona Angular 11.

## Brechas verificadas

1. **URL de detalle fija:** la plantilla utiliza `http://localhost:8281/api/uploads/img/…`. Sustituir por un helper compartido basado en `global.url` (que usa `environment.apiUrl`), codificando el nombre con `encodeURIComponent`.
2. **Permisos incongruentes:** el modal ofrece carga a ADMIN e INVENTARIO; el endpoint acepta ADMIN y COBRADOR. Propuesta: ADMIN e INVENTARIO para subir/reemplazar, lectura para los roles que ya consultan productos. Esto requiere alinear ambas capas.
3. **Archivo vacío:** `upload` elimina la imagen anterior y guarda `imagen = ""` cuando el multipart llega vacío. Rechazarlo antes de tocar archivos o producto.
4. **Validación de contenido:** el backend no comprueba formato ni contenido; el frontend solo busca `image` en el MIME declarado. Propuesta inicial: JPEG/PNG y máximo 5 MB, con comprobación de contenido decodificable y límite de dimensiones/píxeles en backend. Los 5 MB son una decisión propuesta, no una restricción existente.
5. **Rutas y directorio:** el nombre original se concatena al UUID; `getPath` acepta nombres sin normalizar ni comprobar que queden dentro de `uploads`. Generar el nombre exclusivamente en servidor, verificar confinamiento para lectura/eliminación y crear el directorio cuando corresponda. Hacer configurable su ubicación para conservarlo entre despliegues.
6. **Reemplazo y fallo de persistencia:** se borra la foto vieja antes de `save`. Guardar primero la nueva referencia; borrar la anterior después de confirmar persistencia y limpiar el archivo nuevo si falla. Considerar concurrencia y referencias compartidas antes de borrar.
7. **Fallback de lectura:** busca `src/main/resources/static/images/no-image.jpg` como archivo del checkout y usa `!exists && !isReadable`. Usar recurso classpath o un 404 controlado y corregir la condición. La UI debe soportar errores de imagen sin bucles de recarga.
8. **Respuesta de imagen:** actualmente se devuelve Content-Disposition `attachment` sin Content-Type explícito. Proponer `inline` y el MIME del formato validado para visualización.
9. **Estados de carga:** cancelar el selector puede acceder a `.type` de un archivo inexistente; no se bloquean envíos repetidos, no hay preview local y `event.total` puede faltar. Controlar estos estados y limpiar la selección al finalizar.
10. **Detalle incompleto:** el listado pasa su DTO directamente al modal, tipado como `Producto`; no trae todos los campos/relaciones de la entidad (por ejemplo `link`). Consultar el detalle una vez al abrirlo y conservar el stock de la sucursal activa; no reemplazarlo ciegamente por el stock legado de `Producto` tras la carga.
11. **Fotos entre registros:** inventarios que apuntan al mismo `idProducto` comparten la imagen; filas históricas con el mismo código no se sincronizan en `upload`. Definir explícitamente si el catálogo debe compartir la foto entre esos registros; no eliminar archivos que aún tengan referencias.

## Propuesta de UI

Maqueta navegable: [fotografias-productos-ui.html](fotografias-productos-ui.html). Los productos e ilustraciones son ejemplos; la carga en la maqueta solo cambia la vista local y no llama al backend.

### Listado

- Incorporar miniatura de **48 × 48 px junto al nombre**, dentro de la misma columna; conservar código, precios, stock, tipo, marca, estado y opciones.
- Imagen completa con `object-fit: contain`, fondo neutro y tamaño reservado para evitar saltos. Carga diferida con `loading="lazy"`.
- Miniatura como botón con nombre accesible: «Ampliar foto de …». Abre un visor sencillo con foto, nombre y cierre; acceso por teclado y Escape.
- Ausencia o fallo: recuadro del mismo tamaño con «Sin foto», sin mostrar icono de imagen rota. El detalle mantiene disponible la carga autorizada.
- Mantener el botón «Detalle» para consultar información y gestionar la fotografía. En móvil, conservar miniatura junto al nombre y usar la adaptación existente del listado.
- No añadir una galería de tarjetas por defecto: la tabla sigue permitiendo comparar stock y precios con la densidad actual.

### Detalle y carga

- Foto de mayor tamaño con opción de ampliar. Para ADMIN/INVENTARIO: «Seleccionar foto», preview local, «Guardar foto» y «Cancelar selección».
- Texto de formatos/tamaño junto al selector; no alterar la foto guardada hasta que la carga confirme éxito.
- Durante envío: bloquear otra carga, mostrar progreso si existe total; si no, indicador indeterminado. Mantener el error en el contexto de carga y permitir reintentar.
- Tras éxito: actualizar imagen del detalle y página actual del listado; mantener búsqueda, orden, paginación y stock de la sucursal.
- Gestionar foco al abrir/cerrar visor y detalle, etiquetas accesibles y revocación de URL del preview al descartarlo o destruir el componente.

### Una foto o varias

Esta propuesta inicial conserva **una fotografía por producto**, como el modelo existente. Una galería de varias fotos exige entidad/tabla de imágenes, orden, foto principal y endpoints adicionales; no está implementada hoy. No hace falta migración SQL para aprovechar el campo actual en el listado.

## Implementación sugerida

1. Corregir validación, almacenamiento, reemplazo y permisos, manteniendo el endpoint multipart existente.
2. Centralizar URL/fallback en un componente de fotografía reutilizable por listado y detalle; agregar visor y estados de preview/carga.
3. Resolver el detalle completo y mantener contexto de inventario de sucursal.
4. Agregar regresiones de multipart, archivos, permisos y refresco de UI antes de abrir PR.

## Verificación pendiente para implementación

La revisión anterior verifica código fuente, no una subida end-to-end contra servidor o base de datos. En esta entrega no se ejecutan suites de aplicación porque solo se añaden documentación y una maqueta aislada.

- Backend: carga válida; archivo vacío; formato incorrecto; límite de tamaño; producto inexistente; permisos; rutas fuera del directorio; imagen ausente; fallo de guardado; reemplazo y concurrencia.
- Frontend: construcción de FormData sin Content-Type manual; URL por entorno; cancelar selector; preview; progreso sin total; bloqueo de doble envío; error/reintento; miniatura ausente/rota; refresco conservando contexto.
- Integración: subir JPEG/PNG, recuperar imagen, confirmar referencia en listado y búsqueda, verificar persistencia tras reinicio/despliegue y comprobar inventario por sucursal.
- Comandos de aplicación, desde sus carpetas: `.\mvnw.cmd test`; `pnpm test -- --watch=false --browsers=ChromeHeadless`; `pnpm run build:test`; `pnpm run lint`.

## Implementación realizada

- El modal utiliza la URL del backend del entorno y codifica el nombre de archivo; muestra un estado alternativo si falla la imagen. Tipo y marca soportan la entidad de la respuesta de carga y el DTO del listado.
- El endpoint de imágenes devuelve Content-Type según la extensión e indica disposición `inline`.
- La vista inicial del catálogo es Lista: miniatura de 48 px junto al nombre, código secundario, categoría/marca agrupadas, precios alineados y stock agotado destacado.
- La vista Galería utiliza los mismos datos paginados y conserva detalle, edición, ajuste de stock y permisos de costo. Cambiar de vista no lanza otra consulta ni cambia búsqueda, orden o página.
- Ambas vistas comparten plantillas de fotografía, stock y acciones. Las fotografías tienen carga diferida, fallback y visor nativo con cierre por Escape y recuperación de foco.
- En Lista de escritorio se ordena desde los encabezados, incluidos código, categoría y marca. El selector de orden se conserva en Galería y en móvil, donde los encabezados están ocultos. Los selects tienen flecha personalizada y estados de interacción. La interfaz se adapta a pantallas pequeñas sin desplazar horizontalmente el catálogo.
- Las otras brechas del diagnóstico inicial siguen siendo trabajo pendiente; este cambio no implementa una galería de múltiples fotos por producto.

Validación del rediseño:

- `pnpm exec ng test --watch=false --browsers=ChromeHeadless --include=src/app/components/productos/**/*.spec.ts`: 16 pruebas aprobadas.
- `pnpm exec ng build --configuration test`: build correcto; avisos existentes de selectores en CSS de dependencias.
- `pnpm exec eslint src/app/components/productos/productos.component.ts src/app/components/productos/productos.component.html src/app/components/productos/productos.component.spec.ts`: sin errores ni advertencias.
- Chrome con API simulada: lista/galería, foto fallida, visor/Escape/foco, stock conservado entre vistas y navegación móvil sin desbordamiento a 390 px. No se modificó inventario real.
- Capturas locales con datos de prueba: [Lista](../output/productos-lista.png), [Galería](../output/productos-galeria.png), [Lista móvil](../output/productos-lista-movil.png), [Galería móvil](../output/productos-galeria-movil.png). `output/` está ignorado por Git.
- La corrección anterior del endpoint fue verificada con `ProductoImagenControllerTest`: una prueba aprobada.
