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
- Nueva pantalla de inventario por sucursal (`/productos/inventario-sucursal`) con ajuste manual de stock en línea.
- La sesión del usuario incluye ahora su sucursal activa (mostrada en el panel lateral); el alta/edición de usuarios permite asignarla.
- `ProductoService` y el reporte PDF de inventario propagan automáticamente la sucursal activa del usuario logueado en cada consulta.

### Fuera de alcance / seguimiento pendiente
- Los listados de facturas, proformas y notas de crédito no tienen todavía un filtro de sucursal en la UI ni en el backend (los documentos ya quedan correctamente etiquetados con su sucursal; falta exponer el filtro).
- El backend confía en los IDs que envía el frontend para resolver la sucursal en vez de validarlos contra el JWT — mismo patrón que ya existía para `idUsuario`; queda documentado como riesgo conocido, no corregido en este cambio.
- La columna `productos.stock` se conserva en la base de datos por compatibilidad, pero ya no se escribe; se recomienda planear su eliminación en una migración futura.
