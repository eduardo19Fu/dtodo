# Plan de implementación — Módulo de Sucursales

Fecha: 2026-08-22
Alcance: Backend (`Backend/dtodo-backend`, Spring Boot / Java, paquete real `xyz.pangosoft.dtodo`) y Frontend (`Frontend`, Angular 11).

## 1. Resumen ejecutivo

El cliente quiere operar varias **sucursales físicas**. Requisito explícito del cliente:

> El inventario de cada sucursal nueva será **una copia** del inventario actual en el momento de crearla. A partir de ahí, cada sucursal actualiza su inventario **manualmente**; no se requiere sincronización automática entre sucursales.

Esto simplifica mucho el alcance: **no hace falta un motor de sincronización**, solo:

1. Un catálogo de sucursales (CRUD administrativo).
2. Una copia (clonado) de existencias por producto al crear una sucursal nueva.
3. Que cada sucursal tenga sus propias existencias independientes desde ese momento (los movimientos de inventario, ventas, etc. de una sucursal ya no tocan el stock de otra).
4. Que las operaciones existentes (ventas, proformas, notas de crédito, correlativos/series, reportes) queden **acotadas a una sucursal** para que la contabilidad y las existencias no se mezclen.

## 2. Hallazgos clave del análisis (por qué el diseño es así)

- **Hoy el sistema es de una sola ubicación.** `Producto.stock` (`model/Producto.java:59`) es un entero único global — no existe bodega ni ubicación en el modelo JPA vigente.
- **Ya existe un intento abandonado de "tiendas".** `Backend/dtodo-backend/sql/create-tiendas-table.sql` define una tabla `tiendas` (PK `VARCHAR(150)`) que **no está referenciada en ningún lado del código Java** (controllers, repos, entidades) — es un borrador nunca conectado. También hay reportes Jasper huérfanos (`productosDisponibles.jrxml`, `productosBodega.jrxml`) que referencian una tabla legacy `tbl_producto` con columnas `stuckTienda`/`stuck_bodega` — vestigios de un sistema anterior, no del actual. Ninguno de los dos artefactos se reutiliza; sirven solo de referencia de naming.
- **El correlativo (serie de facturación) está atado al Usuario, no a una ubicación.** `Correlativo` (`model/Correlativo.java`) tiene `@ManyToOne Usuario`, y `CorrelativoServiceImpl.save()` (línea 180) impide que un usuario tenga más de un correlativo activo simultáneo. El número fiscal real de la factura, sin embargo, lo certifica la SAT vía FEL (`Factura.correlativoSat`/`serieSat`); el `Correlativo` interno es solo un contador de control.
- **FEL/SAT ya tiene el concepto de "establecimiento" pero está *hardcodeado*.** `FacturaServiceImpl.java:566` hace `datosEmisor.setCodigoEstablecimiento(1)` — un valor fijo. Guatemala exige que cada establecimiento físico registrado ante la SAT tenga su propio código de establecimiento en el DTE. Esto es la pieza fiscal que una sucursal real necesita.
- **El backend confía en los IDs que envía el frontend**, no lee el usuario/; contexto desde el `SecurityContext`/JWT en la lógica de negocio (solo se usa en `auth/*` para la infraestructura de seguridad). Esto es relevante porque significa que "aislar por sucursal" con solo un claim en el JWT no basta — hay que validar explícitamente en servicios/controllers.
- **`MovimientoProductoServiceImpl.calcularStock()`** (líneas 262-314) es el **único punto** de todo el sistema donde se actualiza `Producto.stock`. Es el lugar natural para introducir el cambio a inventario por sucursal.
- **Frontend sin lazy loading**: un solo `AppModule` con ~34 componentes declarados y rutas planas en `app.routing.ts` protegidas por `AuthGuard`+`RoleGuard`. Agregar un módulo de sucursales es mecánico (mismo patrón que `marcas-producto`/`tipos-producto`) y no rompe la arquitectura.
- **No existe ninguna referencia funcional a sucursal/tienda en el frontend** — solo el texto de marca "Tienda" en `sidebar.component.html:6`, que es cosmético.
- El patrón de `TokenInterceptor` (adjunta el `Authorization` header a cada request) es el punto natural para además propagar el ID de sucursal activa sin tener que modificar los ~11 servicios HTTP uno por uno.

## 3. Decisiones de diseño

| Decisión | Elección | Justificación |
|---|---|---|
| Modelo de inventario | Tabla nueva `inventario_sucursal` (producto × sucursal → stock), en vez de duplicar filas de `productos` | Mantiene el catálogo (nombre, precio, marca, imagen) como maestro único; solo las existencias varían por sucursal. Evita duplicar/desincronizar datos de producto que **sí** deben ser compartidos (precio, descripción). |
| Copia inicial | Clonado **una sola vez** al crear la sucursal (INSERT masivo `stock` actual → `inventario_sucursal`) | Cumple el requisito del cliente; no se implementa motor de sync continuo. |
| Usuario ↔ Sucursal | `usuarios.id_sucursal` (relación 1 sucursal por usuario, nullable) | Simplicidad para la primera versión; un cajero opera en una sucursal. Documentado como punto de extensión futura a N:M si se necesita personal itinerante. |
| Sucursal activa en sesión | Derivada del usuario logueado (`usuario.sucursal`), no seleccionable libremente por el cajero | Evita que un cajero facture "a nombre de" otra sucursal por error. `ROLE_ADMIN` puede ver/filtrar todas las sucursales en listados y reportes. |
| Correlativos/series | Se agrega `correlativos.id_sucursal` (informativo/filtro); se mantiene la regla "1 correlativo activo por usuario" ya que un usuario pertenece a 1 sucursal | Cambio mínimo sobre la regla de negocio ya validada en producción; evita reescribir `CorrelativoServiceImpl`. |
| Documentos transaccionales | `facturas`, `proformas`, `notas_credito`, `movimientos_producto` reciben `id_sucursal`, **denormalizado al momento de creación** desde la sucursal del usuario | Si un usuario cambia de sucursal en el futuro, los documentos históricos no cambian de dueño — necesario para reportes y auditoría correctos. |
| Establecimiento SAT | `sucursales.codigo_establecimiento_sat` reemplaza el `1` hardcodeado en `FacturaServiceImpl` | Requisito legal/fiscal real de Guatemala para múltiples locales. |
| Migración de datos existentes | Se crea una sucursal "Central" (`es_principal = 1`) y se backfillea todo lo existente (`usuarios`, `facturas`, `proformas`, `notas_credito`, `movimientos_producto`, y el stock actual hacia `inventario_sucursal`) contra esa sucursal | Cero downtime funcional: el sistema sigue operando igual para la sucursal única existente. |

## 4. Fases de implementación

### Fase 0 — Preparación
- Confirmar con el cliente: ¿cuántas sucursales iniciales?, ¿cada una tiene NIT/establecimiento SAT propio o comparten el mismo Emisor?, ¿un usuario puede pertenecer a más de una sucursal?
- Crear rama de trabajo y script de respaldo de BD antes de aplicar migraciones (`prstd_db`).

### Fase 1 — Catálogo de Sucursales (CRUD administrativo, sin impacto operativo)
Entrega un módulo de sucursales funcional, aislado, que **no** afecta todavía facturación ni inventario.
- Backend: entidad `Sucursal`, repositorio, servicio, controller, tabla `sucursales`.
- Frontend: listado, crear/editar, entrada en sidebar (solo `ROLE_ADMIN`).
- Migración: insertar la sucursal "Central" con los datos actuales del `Emisor`.

### Fase 2 — Usuarios y sesión con sucursal
- `usuarios.id_sucursal` + UI para asignar sucursal al crear/editar usuario.
- JWT: agregar claim `id_sucursal` (mismo patrón que `id_usuario`).
- Frontend: `AuthService` persiste `sucursal` dentro del objeto `Usuario` en `sessionStorage`; `TokenInterceptor` agrega header `X-Sucursal-Id`; sidebar/header muestran la sucursal activa (solo lectura para no-admin).
- Migración: todos los usuarios existentes quedan asignados a la sucursal "Central".

### Fase 3 — Inventario por sucursal (clonado inicial + independencia)
- Tabla `inventario_sucursal` (producto × sucursal).
- Migración: copiar `productos.stock` actual → `inventario_sucursal` para la sucursal "Central" (esto la deja funcionando exactamente igual que hoy).
- Endpoint/servicio `clonarInventario(idSucursalOrigen, idSucursalDestino)`: al crear una sucursal nueva se le ofrece elegir una sucursal origen (normalmente Central) para copiar su inventario — ejecuta el INSERT masivo una sola vez.
- Se reescribe `MovimientoProductoServiceImpl.calcularStock()` para leer/escribir `inventario_sucursal` (filtrado por `idSucursal` del movimiento) en lugar de `Producto.stock`.
- `Producto.stock` se mantiene en el modelo temporalmente por compatibilidad (ver riesgos, sección 8), pero deja de ser la fuente de verdad tras el corte.
- Frontend: listado de productos, movimientos de inventario y creación de facturas/proformas empiezan a mostrar y consumir stock por sucursal (usando la sucursal del usuario logueado).

### Fase 4 — Documentos transaccionales por sucursal
- `id_sucursal` en `facturas`, `proformas`, `notas_credito`, `movimientos_producto` (denormalizado al crear).
- `FacturaServiceImpl`: reemplazar `datosEmisor.setCodigoEstablecimiento(1)` por `sucursal.getCodigoEstablecimientoSat()`.
- Filtros de listados (facturas, proformas, notas de crédito) por sucursal — admin ve todas, cajero ve solo la suya (a nivel de query, no solo de UI).
- Migración: backfill de `id_sucursal = Central` en todo el histórico.

### Fase 5 — Reportes
- `rpt_inventario.jrxml`, `productos_excel.jrxml`, `poliza.jrxml` (ventas diarias) reciben parámetro `idSucursal`.
- Nuevo reporte opcional "inventario consolidado" (todas las sucursales, solo `ROLE_ADMIN`).

### Fase 6 — Cierre, pruebas y despliegue
- Pruebas de regresión de todo el flujo actual (que sigue funcionando igual con una sola sucursal).
- Pruebas de flujo nuevo (crear sucursal → clonar inventario → operar independiente).
- Despliegue de migraciones SQL en `prstd_db` con respaldo previo (ver memoria de migraciones anteriores del proyecto sobre `mysqlsh` y `ddl-auto=none`).

## 5. Cambios propuestos a la base de datos

Todas las tablas nuevas siguen las convenciones ya usadas en el proyecto (PK `IDENTITY`, FKs `id_x`, reutilización de la tabla `estados` para estado activo/inactivo, igual que `Producto`/`Correlativo`/`Factura`).

```sql
-- =========================================================
-- 1. Tabla de sucursales
-- =========================================================
CREATE TABLE sucursales (
    id_sucursal              INT AUTO_INCREMENT PRIMARY KEY,
    nombre                   VARCHAR(100) NOT NULL,
    direccion                VARCHAR(200) NOT NULL,
    telefono                 VARCHAR(15),
    encargado                VARCHAR(150),
    codigo_establecimiento_sat INT NOT NULL DEFAULT 1,  -- exigido por FEL/SAT para el nodo dte:Emisor
    es_principal              BOOLEAN NOT NULL DEFAULT FALSE, -- marca la sucursal "Central" creada en la migración
    fecha_registro            DATETIME NOT NULL,
    id_estado                 INT NOT NULL,
    CONSTRAINT fk_sucursal_estado FOREIGN KEY (id_estado) REFERENCES estados(id_estado)
);

-- =========================================================
-- 2. Inventario por sucursal (copia independiente de existencias)
-- =========================================================
CREATE TABLE inventario_sucursal (
    id_inventario_sucursal   BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_sucursal              INT NOT NULL,
    id_producto              INT NOT NULL,
    stock                    INT NOT NULL DEFAULT 0,
    stock_minimo             INT,
    fecha_actualizacion      DATETIME NOT NULL,
    CONSTRAINT fk_inv_sucursal FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal),
    CONSTRAINT fk_inv_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto),
    CONSTRAINT uq_inv_sucursal_producto UNIQUE (id_sucursal, id_producto)
);
CREATE INDEX idx_inv_sucursal ON inventario_sucursal(id_sucursal);
CREATE INDEX idx_inv_producto ON inventario_sucursal(id_producto);

-- =========================================================
-- 3. Usuarios ↔ Sucursal
-- =========================================================
ALTER TABLE usuarios
    ADD COLUMN id_sucursal INT NULL,
    ADD CONSTRAINT fk_usuario_sucursal FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal);

-- =========================================================
-- 4. Correlativos ↔ Sucursal (filtro/auditoría)
-- =========================================================
ALTER TABLE correlativos
    ADD COLUMN id_sucursal INT NULL,
    ADD CONSTRAINT fk_correlativo_sucursal FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal);

-- =========================================================
-- 5. Documentos transaccionales ↔ Sucursal (denormalizado)
-- =========================================================
ALTER TABLE facturas
    ADD COLUMN id_sucursal INT NULL,
    ADD CONSTRAINT fk_factura_sucursal FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal);

ALTER TABLE proformas
    ADD COLUMN id_sucursal INT NULL,
    ADD CONSTRAINT fk_proforma_sucursal FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal);

ALTER TABLE notas_credito
    ADD COLUMN id_sucursal INT NULL,
    ADD CONSTRAINT fk_nota_credito_sucursal FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal);

ALTER TABLE movimientos_producto
    ADD COLUMN id_sucursal INT NULL,
    ADD CONSTRAINT fk_movimiento_sucursal FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal);

-- Nota: se agregan como NULL primero para permitir el backfill (paso 6) sin bloquear la tabla;
-- una vez migrados los datos, considerar volverlos NOT NULL en una migración posterior.

-- =========================================================
-- 6. Migración de datos existentes (backfill a Sucursal Central)
-- =========================================================
INSERT INTO sucursales (nombre, direccion, telefono, encargado, codigo_establecimiento_sat, es_principal, fecha_registro, id_estado)
SELECT 'Sucursal Central', e.direccion, NULL, NULL, 1, TRUE, NOW(), (SELECT id_estado FROM estados WHERE estado = 'ACTIVO' LIMIT 1)
FROM emisores e LIMIT 1;

SET @id_central = (SELECT id_sucursal FROM sucursales WHERE es_principal = TRUE LIMIT 1);

UPDATE usuarios SET id_sucursal = @id_central WHERE id_sucursal IS NULL;
UPDATE correlativos SET id_sucursal = @id_central WHERE id_sucursal IS NULL;
UPDATE facturas SET id_sucursal = @id_central WHERE id_sucursal IS NULL;
UPDATE proformas SET id_sucursal = @id_central WHERE id_sucursal IS NULL;
UPDATE notas_credito SET id_sucursal = @id_central WHERE id_sucursal IS NULL;
UPDATE movimientos_producto SET id_sucursal = @id_central WHERE id_sucursal IS NULL;

INSERT INTO inventario_sucursal (id_sucursal, id_producto, stock, stock_minimo, fecha_actualizacion)
SELECT @id_central, p.id_producto, p.stock, NULL, NOW()
FROM productos p;
```

Estos scripts se guardarían siguiendo la convención ya usada en el repo (`Backend/dtodo-backend/sql/create_sucursales_table.sql`, `create_inventario_sucursal_table.sql`, `alter_*_add_id_sucursal.sql`, `migrate_sucursal_central.sql`), no como migraciones automáticas de Hibernate (recordar: el proyecto usa `ddl-auto=none`, aplicar manualmente contra `prstd_db` con respaldo previo).

## 6. Nuevos módulos propuestos

### 6.1 Backend — módulo `Sucursal`

| Capa | Archivo nuevo | Detalle |
|---|---|---|
| Modelo | `model/Sucursal.java` | Entidad JPA `@Table("sucursales")`, campos de la sección 5, `@ManyToOne Estado`. |
| Modelo | `model/InventarioSucursal.java` | `@ManyToOne Sucursal`, `@ManyToOne Producto`, `stock`, `stockMinimo`. |
| DTO | `dto/SucursalDto.java` | Proyección liviana para listados (igual patrón que `ProductoDto`). |
| DTO | `dto/InventarioSucursalDto.java` | Para listados de existencias por sucursal. |
| Repositorio | `repository/ISucursalRepository.java` | CRUD + `findByEsPrincipalTrue()`. |
| Repositorio | `repository/IInventarioSucursalRepository.java` | `findByIdSucursal(...)`, `findByIdSucursalAndIdProducto(...)`, actualización de stock. |
| Servicio | `service/ISucursalService.java` + `service/impl/SucursalServiceImpl.java` | CRUD + `clonarInventario(idSucursalOrigen, idSucursalDestino)`. |
| Servicio | `service/IInventarioSucursalService.java` + `service/impl/InventarioSucursalServiceImpl.java` | Consulta/ajuste manual de stock por sucursal; reemplaza la lógica actual de `MovimientoProductoServiceImpl.calcularStock()` para que opere sobre esta tabla. |
| Controller | `controller/SucursalApiController.java` | `GET/POST/PUT /api/sucursales`, `POST /api/sucursales/{id}/clonar-inventario/{idOrigen}`. `@Secured("ROLE_ADMIN")` para create/update/clonar. |
| Controller | `controller/InventarioSucursalApiController.java` | `GET /api/inventario-sucursal/{idSucursal}`, `PUT /api/inventario-sucursal/{idSucursal}/{idProducto}` (ajuste manual). |
| Seguridad | `auth/SecurityConfig.java` (modificado) | Registrar los nuevos endpoints (públicos de solo lectura si aplica, protegidos para escritura). |
| Seguridad | `auth/JwtTokenService.java` (modificado) | Agregar claim `id_sucursal` al access token. |

### 6.2 Frontend — módulo `sucursales`

Sigue el mismo patrón ya usado por `marcas-producto`/`tipos-producto` (list + create/edit reutilizable vía `:id`, sin detail separado).

| Archivo nuevo | Detalle |
|---|---|
| `models/sucursal.ts` | Campos espejo del backend. |
| `dtos/sucursal-dto.ts` | Para listados. |
| `models/inventario-sucursal.ts` | Para la vista de existencias por sucursal. |
| `services/sucursales/sucursal.service.ts` | Consumo de `/sucursales`, incluye `clonarInventario(...)`. |
| `services/sucursales/inventario-sucursal.service.ts` | Consumo de `/inventario-sucursal`. |
| `components/sucursales/sucursales.component.ts/.html` | Listado (DataTable), ruta `sucursales/index`, rol `ROLE_ADMIN`. |
| `components/sucursales/create-sucursal/create-sucursal.component.ts/.html` | Crear/editar, ruta `sucursales/create[/:id]`, con selector "copiar inventario desde" al crear. |
| `components/sucursales/selector-sucursal/selector-sucursal.component.ts/.html` | Widget de solo-lectura que muestra la sucursal activa del usuario logueado (en `sidebar`/`header`); para `ROLE_ADMIN` puede incluir un filtro de "ver todas". |
| `components/inventario-sucursal/inventario-sucursal.component.ts/.html` | Vista de existencias de la sucursal activa (ajuste manual de stock), ruta `productos/inventario-sucursal/index`. |

### 6.3 Archivos existentes que se modifican

**Backend:**
- `model/Usuario.java` — agregar `@ManyToOne Sucursal`.
- `model/Correlativo.java` — agregar `@ManyToOne Sucursal`.
- `model/Factura.java`, `model/Proforma.java`, `model/NotaCredito.java`, `model/MovimientoProducto.java` — agregar `@ManyToOne Sucursal`.
- `service/impl/FacturaServiceImpl.java` — línea 566, `setCodigoEstablecimiento(1)` → `sucursal.getCodigoEstablecimientoSat()`; propagar `idSucursal` al crear factura/movimiento.
- `service/impl/MovimientoProductoServiceImpl.java` — `calcularStock()` (líneas 262-314) reescrito para leer/escribir `InventarioSucursal` filtrado por sucursal.
- `service/impl/ProductoServiceImpl.java`, `repository/IProductoRepository.java` — listados de stock por sucursal (join con `inventario_sucursal`); revisar SPs `sp_consultar_productos*` para agregar parámetro `idSucursal`.
- `service/impl/CorrelativoServiceImpl.java` — al guardar, setear `idSucursal` desde el usuario.
- `service/impl/ProformaServiceImpl.java`, `service/impl/NotaCreditoServiceImpl.java`, `service/impl/DespachoNotaServiceImpl.java` — propagar `idSucursal`.
- `controller/FacturaApiController.java`, `ProformaApiController.java`, `NotaCreditoApiController.java`, `MovimientoProductoApiController.java` — filtros de listado por `idSucursal`.
- `src/main/resources/reports/rpt_inventario.jrxml`, `productos_excel.jrxml`, `poliza.jrxml` — parámetro `idSucursal`.

**Frontend:**
- `app.module.ts` — declarar los ~8 componentes nuevos.
- `app.routing.ts` — nuevas rutas (`sucursales/*`, `productos/inventario-sucursal/*`), guardadas con `AuthGuard`+`RoleGuard`.
- `components/sidebar/sidebar.component.html/.ts` — nueva entrada de menú "Sucursales" (`ROLE_ADMIN`) + widget de sucursal activa.
- `services/auth.service.ts` — persistir `sucursal` dentro de `Usuario` en `sessionStorage`.
- `models/usuario.ts`, `models/auxiliar/usuario-auxiliar.ts` — agregar campo `sucursal`.
- `components/usuarios/interceptors/token.interceptor.ts` — adjuntar header `X-Sucursal-Id` en cada request.
- `components/usuarios/create-usuario/create-usuario.component.ts/.html` — selector de sucursal al crear/editar usuario.
- `services/producto.service.ts`, `services/movimientos/movimientos-producto.service.ts`, `services/facturas/factura.service.ts`, `services/proformas/proforma.service.ts`, `services/notas-credito.service.ts` — pasar `idSucursal` en las consultas de listado.
- `components/productos/listado-productos-mejorado/*`, `components/movimientos-producto/*`, `components/facturas/*`, `components/proformas/*`, `components/notas-credito/*` — mostrar/filtrar por sucursal en las tablas.

## 7. Riesgos y consideraciones

- **`Producto.stock` como campo legacy.** Mientras dure la transición, mantenerlo sincronizado solo para la sucursal Central evita romper reportes/consultas antiguas que aún lo lean directamente; debe planearse su deprecación explícita (fecha de corte) para no terminar con dos fuentes de verdad indefinidamente.
- **El backend no valida hoy contra el JWT.** Si el frontend sigue enviando `idUsuario`/`idSucursal` en el body como hace hoy con `idUsuario`, un cliente malicioso podría enviar una sucursal distinta a la suya. Se recomienda, al menos para escritura de inventario y facturación, resolver `idSucursal` en el backend desde el JWT (`@AuthenticationPrincipal Jwt jwt`) en vez de confiar en el valor del request — es un cambio de patrón, no solo una extensión, y debe presupuestarse aparte.
- **`sql/create-tiendas-table.sql` y los reportes huérfanos con `stuckTienda`/`stuck_bodega`** no se reutilizan: son de un esquema anterior (tabla `tbl_producto` inexistente hoy). Se recomienda eliminarlos del repo al cerrar este proyecto para no confundir a futuros desarrolladores, o dejarlos con una nota explícita de que están obsoletos.
- **Roles y sucursal:** decidir si `ROLE_ADMIN` opera "sin sucursal" (ve todo) o si también se le asigna una sucursal por defecto — impacta el diseño del selector y de los filtros de listado.
- **Correlativos por sucursal vs. por usuario:** si en el futuro el cliente pide series fiscales distintas por sucursal (muy probable en Guatemala si cada sucursal es un establecimiento SAT distinto), habrá que revisar la regla de unicidad de `CorrelativoServiceImpl.save()` — se dejó fuera de este alcance porque el cliente no lo pidió explícitamente, pero el campo `id_sucursal` en `correlativos` ya queda preparado para ese cambio.
- **Tamaño del bundle/módulo Angular:** no hay lazy loading; agregar ~8 componentes es seguro pero conviene aprovechar la fase para no seguir creciendo el `AppModule` sin control si el cliente pide más módulos después.

## 8. Checklist de pruebas antes de desplegar

- [ ] Con una sola sucursal (Central), todo el flujo actual (productos, movimientos, facturación, proformas, notas de crédito, correlativos, reportes) funciona exactamente igual que antes de la migración.
- [ ] Crear una sucursal nueva clona correctamente el inventario elegido (cantidades exactas, sin afectar el inventario origen).
- [ ] Un movimiento de inventario en la sucursal nueva **no** modifica el stock de la sucursal Central ni viceversa.
- [ ] Una venta (factura) en la sucursal nueva descuenta stock solo de su propio `inventario_sucursal` y usa el `codigo_establecimiento_sat` correcto en el DTE FEL.
- [ ] Reportes de inventario/ventas filtran correctamente por sucursal; el reporte consolidado (admin) suma correctamente ambas sucursales.
- [ ] Un usuario asignado a la sucursal A no puede (ni desde el frontend ni enviando requests directos) crear/anular documentos a nombre de la sucursal B.
