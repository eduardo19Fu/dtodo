# Changelog

Todos los cambios notables de este proyecto se documentan en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).

## [Unreleased]

### Agregado — Módulo de Sucursales

Soporte completo para operar múltiples sucursales, cada una con inventario independiente (copiado del inventario existente al crearse, y desde ahí administrado manualmente sin sincronización automática entre sucursales).

**Base de datos** (`Backend/dtodo-backend/sql/sucursales/`)
- Tablas nuevas `sucursales` y `inventario_sucursal` (stock por producto × sucursal).
- Columna `id_sucursal` agregada a `usuarios`, `correlativos`, `facturas`, `proformas`, `notas_credito` y `movimientos_producto`.
- Migración que crea una sucursal "Central", asigna a ella todos los usuarios y documentos existentes, y clona el `stock` histórico de `productos` hacia `inventario_sucursal` — el sistema sigue funcionando exactamente igual para la operación actual tras aplicarla.
- El stored procedure `sp_consultar_productos` se reescribió para leer el stock desde `inventario_sucursal` en vez de `productos.stock`.

**Backend**
- Nuevo módulo `Sucursal`: CRUD administrativo (`ROLE_ADMIN`), sin borrado (las sucursales se desactivan por estado, no se eliminan, dado que tienen documentos asociados).
- Nuevo módulo `InventarioSucursal`: listado paginado del stock por sucursal, ajuste manual de existencias, y `clonarInventario(origen, destino)` para copiar el stock al crear una sucursal nueva.
- `productos.stock` queda deprecado como fuente de verdad; todo el stock (incluida la sucursal "Central") vive en `inventario_sucursal`. Se reescribieron los puntos de lectura/escritura de stock: `MovimientoProductoServiceImpl.calcularStock()`, las consultas de listado/búsqueda de productos, y el stored procedure de inventario.
- `Usuario` se asocia 1 a 1 con `Sucursal`; el JWT incluye ahora los claims `id_sucursal`/`sucursal`.
- `Correlativo`, `Factura`, `Proforma` y `NotaCredito` quedan asociados a la sucursal del usuario que los genera (denormalizado al crearse, para que el histórico no cambie si el usuario se reasigna de sucursal después).
- El código de establecimiento SAT usado en la certificación FEL (`CodigoEstablecimiento`, antes fijo en `1`) ahora se toma de la sucursal donde se realiza la venta.
- Los reportes de inventario (`rpt_inventario`) y productos (`productos_excel`) filtran por sucursal.
- Mientras el frontend no envíe explícitamente la sucursal activa en cada request, los endpoints de productos/reportes usan como respaldo la sucursal marcada como `es_principal`, para no romper la operación existente.

**Frontend**
- Nuevo módulo de administración de sucursales (`/sucursales`), con selector de "copiar inventario desde" al crear una sucursal.
- El listado de **Productos** (que ya mostraba el stock escopeado a la sucursal activa) gana edición en línea del stock; no se creó una pantalla separada de inventario por sucursal para evitar mostrar la misma información dos veces (ver nota más abajo).
- El menú lateral "Inventario" se renombró a "Movimientos de Producto" para diferenciarlo claramente del stock actual (que ahora vive en Productos).
- La sesión del usuario incluye ahora su sucursal activa (mostrada en el panel lateral); el alta/edición de usuarios permite asignarla. El modal de detalle de usuario también muestra la sucursal asignada y permite reasignarla ahí mismo, sin ir al formulario completo de edición.
- `ProductoService` y el reporte PDF de inventario propagan automáticamente la sucursal activa del usuario logueado en cada consulta.

**Nota de diseño**: la primera versión de este cambio incluyó una pantalla independiente "Inventario por Sucursal", redundante con Productos. Se retiró del frontend tras revisión; el endpoint de backend (`GET /inventario-sucursal/{idSucursal}/listado`) se conservó por si un futuro consumidor (reporte, exportación) lo necesita, pero ningún componente lo usa actualmente. El ajuste manual de stock en línea todavía **no** genera un registro en el historial de movimientos — es una sobreescritura directa, decisión explícita para no ampliar el alcance de este cambio.

### Cambiado — Productos y búsquedas de producto ahora filtran por sucursal

Antes, el catálogo de Productos (y los buscadores de producto embebidos al crear facturas, proformas y movimientos de inventario) mostraban **todos** los productos del sistema aunque no tuvieran existencias registradas en la sucursal activa (aparecían con `Stock: 0`). Esto generaba confusión: no se distinguía "agotado" de "no se vende en esta sucursal". Ahora:

- Un producto solo aparece en el listado/búsqueda si tiene una fila en `inventario_sucursal` para la sucursal del usuario logueado (`IProductoRepository.findAllProductosDto` pasó de `LEFT JOIN` a `INNER JOIN`; `ProductoServiceImpl` agrega un filtro `EXISTS` equivalente a la búsqueda por Criteria API). Como los tres buscadores de producto (factura, proforma, movimiento de inventario) reutilizan los mismos dos métodos de `ProductoService` que el listado principal, quedaron cubiertos con el mismo cambio.
- Una sucursal sin inventario importado ya no genera error: `findAllDtoMejorado` dejó de lanzar `NoContentException` en resultado vacío (esa rama era código muerto antes de este cambio — un catálogo global vacío no era realista — y con el filtro por sucursal se volvió un caso normal; lanzarla producía un HTTP 204 sin cuerpo utilizable que rompía el cliente Angular). Ahora devuelve una página vacía normal.
- Productos muestra un estado vacío distinto cuando la sucursal activa no tiene inventario: `ROLE_ADMIN` ve un selector de sucursal origen y un botón "Importar productos" (reutiliza `clonarInventario`); el resto de roles ve un aviso para solicitarlo a un administrador.

### Corregido

- **Ciclo de deserialización Jackson entre `Sucursal` y `Usuario`**: `POST /api/sucursales` con un `usuario` anidado fallaba con `"JSON parse error: No _valueDeserializer assigned"` (HTTP 400). `Usuario.sucursal` ya ignoraba `usuario` en su propio `@JsonIgnoreProperties`, pero `Sucursal.usuario` no ignoraba `sucursal` de vuelta, así que el ciclo solo estaba roto en un sentido. Se agregó `"sucursal"` a la lista de propiedades ignoradas en `Sucursal.usuario`. Cubierto con un test permanente (`SucursalJacksonTest`) que reproduce el escenario contra el `ObjectMapper` real de la aplicación.
- **`id_estado` nunca se asignaba al crear una sucursal**: `POST /api/sucursales` fallaba con `SQLIntegrityConstraintViolationException: Column 'id_estado' cannot be null` (HTTP 500), porque `SucursalServiceImpl.save()` nunca seteaba el estado en el alta. Ahora se asigna `ACTIVO` automáticamente al crear, y se conserva el estado existente al actualizar (a menos que se envíe uno explícito).
- **Aviso de "sucursal sin inventario" en Productos**: rediseñado de alertas planas de Bootstrap a una tarjeta con icono suave, alineada en colores con el resto de badges del sistema; se corrigió además su ancho, que no coincidía con el de la tabla/controles superiores (tanto en escritorio como en el breakpoint móvil, donde antes forzaba scroll horizontal en vez de ajustarse como el resto de tarjetas).
- **Listado de Sucursales**: los badges de estado/principal pasaron de clases crudas de Bootstrap a los tokens `listing-status-pill` que usa el resto de tablas del sistema; se corrigió el ancho mínimo de la tabla, que quedaba por debajo de la suma real de sus columnas y las comprimía en vez de forzar scroll horizontal limpio.
- **La sucursal asignada no se precargaba al editar un usuario**: el `<select>` comparaba por identidad de referencia (`[ngValue]="item"` contra `usuarioAuxiliar.sucursal`), pero ambos objetos venían de llamadas HTTP distintas (`GET /api/usuarios/{id}` vs. `GET /api/sucursales`), así que aunque tuvieran el mismo `idSucursal` nunca coincidían y el selector siempre mostraba "Sin asignar". Se cambió el binding a un id numérico, igual que el selector de "sucursal origen" al importar inventario en Productos.
- **Editar un producto pisaba `productos.stock` en vez de `inventario_sucursal`**: el formulario de "Editar producto" conservaba un campo "Existencias" de antes de la migración a sucursales, ligado a `producto.stock`. Como `PUT /api/productos` guarda la entidad completa, ese campo escribía directo a la columna `productos.stock` — deprecada, sin ningún flujo de lectura — sin tocar el inventario real de ninguna sucursal; el usuario veía el mensaje de éxito de siempre pero el ajuste no existía a nivel de negocio. Se detectó al probar el aislamiento de stock entre sucursales. El campo ahora solo aparece al crear un producto nuevo (donde sí siembra el inventario inicial); además, `ProductoServiceImpl.save()` preserva el stock actual de la BD en cualquier actualización, como refuerzo por si otro cliente llega a mandarlo en el payload.
- **El ciclo Jackson `Usuario`↔`Sucursal` faltaba en 8 entidades más**: `POST /api/proformas` con un `usuario` anidado reprodujo el mismo `"JSON parse error: No _valueDeserializer assigned"` ya corregido antes para `Sucursal.usuario` — pero ese fix solo cubrió ese único punto. Cuando `Usuario` ganó el campo `sucursal`, cada entidad que embebe `Usuario` necesitaba agregar `"sucursal"` a su propio `@JsonIgnoreProperties`; quedó pendiente en `Correlativo`, `Factura`, `MarcaProducto`, `DespachoNota`, `MovimientoProducto`, `NotaCredito`, `Proforma` y `TipoProducto`. Se corrigieron las 8, cubierto con `ProformaJacksonTest` (reproduce el escenario exacto reportado).
- **Encabezado fijo con los datos de Central en los PDF de Proforma y Nota de Crédito**: `proforma.jrxml` y `rpt_nota_credito.jrxml` imprimían la dirección/teléfono de Sucursal Central como texto estático, sin importar desde qué sucursal se generara el documento — quedaron fuera del alcance de M5 (que solo cubrió los reportes de inventario y productos). Ahora ambas consultas hacen `LEFT JOIN` a `sucursales` por el `id_sucursal` del documento y el encabezado se arma dinámicamente. El NIT se mantiene fijo (es el mismo para toda la empresa). Cubierto con `RptNotaCreditoTemplateTest` (nuevo) y una extensión de `ProformaPdfTemplateTest`.
- **Card de "Productos" del Home**: mostraba un error crudo (`swal`) para usuarios `ROLE_COBRADOR` porque `GET /api/productos/cantidad-productos` estaba restringido a `ROLE_ADMIN`/`ROLE_INVENTARIO` mientras que esa card se muestra a cualquier usuario logueado (a diferencia de Ventas/Usuarios, que sí filtran por rol) — se agregó `ROLE_COBRADOR` al endpoint, y el servicio ahora degrada a 0 ante cualquier error futuro en vez de interrumpir con un `swal`. Además, el conteo en sí nunca tuvo relación con sucursales: era `SELECT get_cant_productos()`, el tamaño fijo del catálogo completo — una sucursal recién creada sin inventario importado mostraba el mismo número que el catálogo entero en vez de 0. Se agregó `IInventarioSucursalService.contarPorSucursal(idSucursal)` (mismo patrón `resolverSucursal()` que el resto de endpoints de productos) para que el conteo refleje el inventario real de la sucursal activa.
- **Card de "Ventas" del Home**: mostraba el total global de facturas del sistema para cualquier usuario, incluyendo un cobrador que solo debería ver sus propias ventas. Se agregó `AuthService.esSoloCobrador()` (usuario con únicamente `ROLE_COBRADOR`, sin `ADMIN` ni `INVENTARIO`) y `IFacturaRepository.getCantidadVentasPorUsuario(idUsuario)` (mismo criterio que `get_cant_ventas()`: excluye facturas anuladas); un cobrador puro ve solo sus ventas, cualquier otro caso conserva el conteo global de siempre.
- **PDF de inventario fallaba con `JRException: Byte data not found`**: `rpt_inventario.jrxml` cargaba su logo desde una ruta de texto plana (`"static/images/reports-image/dimsa-logo.jpeg"`, en vez de `this.getClass().getResourceAsStream(...)` como el resto de reportes) apuntando a un archivo de una marca anterior al rebrand a D'Todo que nunca existió en este repositorio — cualquier generación del reporte fallaba. Se corrige para usar el mismo `logo-dtodo.jpg` que ya usan proforma y nota de crédito. `rpt_ventas_diarias.jrxml` tiene la misma referencia rota pero es una plantilla huérfana (nada la usa), no se tocó. El test existente solo compilaba la plantilla (no detecta rutas de imagen rotas, que solo fallan al *llenar* el reporte); se agregó un caso que la llena con `JREmptyDataSource` para reproducir el punto de falla real.

### Agregado — Detalle de sucursal

- Botón "Ver detalle" en el listado de Sucursales que abre un modal de solo lectura (encargado, teléfono, código de establecimiento SAT, quién la registró, fecha de registro, estado y si es la principal), siguiendo el mismo patrón visual ya usado por Usuarios y Productos.
- La carga del detalle usa un toast ("Cargando detalle...") en vez de un spinner en el botón, igual que en Proformas/Notas de crédito.
- La barra de paginación del listado de Sucursales ya no se oculta cuando solo hay una página, para mantener el mismo pie de tabla que el resto de listados del sistema.

### Fuera de alcance / seguimiento pendiente
- Los listados de facturas, proformas y notas de crédito no tienen todavía un filtro de sucursal en la UI ni en el backend (los documentos ya quedan correctamente etiquetados con su sucursal; falta exponer el filtro).
- El backend confía en los IDs que envía el frontend para resolver la sucursal en vez de validarlos contra el JWT — mismo patrón que ya existía para `idUsuario`; queda documentado como riesgo conocido, no corregido en este cambio.
- La columna `productos.stock` se conserva en la base de datos por compatibilidad, pero ya no se escribe; se recomienda planear su eliminación en una migración futura.
