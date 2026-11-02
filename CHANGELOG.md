# Changelog

Todos los cambios notables de este proyecto se documentan en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).

## [Unreleased]

### Cambiado — Devoluciones de clientes: formulario con la estructura de los demás
- El formulario de creación usa la misma estructura que Productos, Rutas y Recibos: encabezado de página, tarjeta con banner y secciones numeradas (Cliente y pedido, Productos devueltos, Datos de la devolución). Las dos últimas aparecen al elegir el pedido.
- El cliente se elige en el directorio de clientes (lupa o Enter), solo mayoristas, en lugar de escribirlo. El modal de búsqueda acepta un `tipoCliente` opcional; sin él se comporta igual que antes.
- Mientras se registra una devolución se oculta la lista de devoluciones registradas; reaparece al registrar o cancelar.

### Cambiado — Recibos de caja: búsqueda de cliente, cheque y formulario más limpio
- El campo de cliente se presenta como búsqueda (icono, "Buscar cliente", indicador de búsqueda en curso) y avisa cuando no encuentra a nadie; un botón permite quitar el cliente elegido y buscar a otro.
- Nueva forma de pago **Cheque**: pide el número de cheque como referencia, sale en el recibo impreso y en el estado de cuenta. No requiere script SQL (`forma_pago` es VARCHAR).
- El campo de referencia indica qué número va (boleta, transferencia o cheque) según la forma de pago.
- Al crear un recibo se oculta la lista de recibos emitidos; reaparece al emitir o cancelar.

### Cambiado — Menú lateral: opciones mayoristas agrupadas
- Nuevo grupo **Mayoristas** (Rutas, Descuentos, Autorizaciones, Comisiones, Indicadores), solo administrador.
- Nuevo grupo **Cuentas por cobrar** (Cobranza, Recibos de caja, Devoluciones de clientes): administrador y cobrador; Devoluciones solo administrador.
- **Pedidos** queda suelto porque lo usan a diario el vendedor, la bodega y el repartidor.
- Indicadores estaba escrito dentro del submenú de Bodegas con la clase de ítem principal; ahora vive en Mayoristas.
- Los grupos se abren solos según la ruta activa y el buscador los muestra con sus opciones. Solo frontend.

### Agregado — Anular una devolución de cliente

El administrador puede anular una devolución registrada, con motivo obligatorio. La devolución se conserva (quién, cuándo y por qué) y deja de contar.

- **Qué revierte**: lo devolvible de cada línea vuelve a subir y el valor devuelto vuelve a sumarse a lo que debe el cliente (saldo, cobranza, control de crédito, estado de cuenta). Lo que había **reingresado** al inventario sale de nuevo con un movimiento nuevo (`ANULACION_DEVOLUCION_CLIENTE`, visible en el kardex como "Anulación de devolución de cliente"); lo que se dio de baja nunca entró, así que no se toca. Si la sucursal ya no tiene esas existencias, se rechaza la anulación completa con el mensaje de stock insuficiente.
- **Cuándo no se puede**: si la devolución está ligada a una nota de crédito que sigue vigente (hay que anular primero la nota), si ya está anulada, o si las **comisiones del año de la devolución ya están liquidadas** (hay que deshacer antes la liquidación, porque las devoluciones son parte de la base de la comisión). Una devolución anulada ya no resta en las comisiones, las ventas netas del tablero ni la cobranza.
- **Historial**: queda en la línea de tiempo del pedido como "Devolución anulada", con el motivo.
- **API**: `POST /api/devoluciones-cliente/{id}/anular` con `{ "motivo": ... }`, solo `ROLE_ADMIN`.
- **Pantalla**: botón de anular por fila, marca "Anulada" y, en el detalle, quién la anuló, cuándo y por qué.
- **Base de datos** (`sql/mayoristas/20_anular_devoluciones_cliente.sql`, **requerido antes del jar**; el rollback lo revierte): columnas de anulación en `devoluciones_cliente` y el valor del ENUM de `movimientos_producto` (solo si falta).
- **Pruebas**: restauración de lo devolvible, salida del inventario solo de lo que reingresó, motivo, doble anulación, nota de crédito vigente y anulada, año liquidado, stock insuficiente, pedido inexistente, las consultas que excluyen las anuladas contra H2, la matriz de roles y los specs.

### Cambiado — Las comisiones son netas de devoluciones

La comisión de un vendedor se calcula sobre **lo entregado menos lo que los clientes devolvieron**, como las ventas del tablero de indicadores.

- **Base de la comisión**: entregado menos devuelto, nunca menos de cero. Sobre esa base se alcanza la meta y se calcula el porcentaje plano; lo que falta para la siguiente meta también se mide contra ella. Por ejemplo, con metas 300k/700k/1M/2M, quien entregó Q750,000 y tuvo Q100,000 en devoluciones queda en la meta de 300k (0.50 %) sobre Q650,000, es decir Q3,250, y no en la de 700k.
- **Qué devoluciones restan**: las de los pedidos del vendedor, por la fecha de la devolución, dentro del mismo período que cuenta lo entregado (desde el 1 de enero del año o, para quien ingresó el año anterior, desde su ingreso). Una devolución de lo entregado en un año anterior resta en el año en que se hace; una de un año posterior no cuenta.
- **Liquidación**: guarda lo devuelto junto a lo entregado (`comisiones_liquidaciones.monto_devuelto`), así un año liquidado muestra cómo se calculó y no se recalcula. Las liquidaciones anteriores quedan con devuelto 0. Al deshacer una liquidación el resumen también lo anota.
- **Pantalla y reporte**: la tabla muestra *Entregado*, *Devuelto* y *Base (neto)*, y la tarjeta de resumen la base de comisión con su desglose; el reporte PDF y Excel trae las mismas columnas y totales.
- **Base de datos** (`sql/mayoristas/19_comisiones_monto_devuelto.sql`, **requerido antes del jar**; el rollback lo revierte): `comisiones_liquidaciones.monto_devuelto`.
- **Pruebas**: base neta y cambio de meta, base nunca negativa y totales, devoluciones solo del período de cada vendedor (antes del ingreso, año posterior, año anterior), liquidación que guarda lo devuelto, año liquidado con y sin devuelto guardado, la consulta contra H2 y el reporte con las columnas nuevas.

### Agregado — Tablero de indicadores (Fase 5, paso 3)

Cierra la Fase 5 y el plan de clientes mayoristas. Sin script SQL; solo el administrador.

- **Ventas del mes por vendedor**: lo entregado a clientes mayoristas en el mes (por la fecha real de entrega, a precio de línea con descuento) **menos lo que los clientes devolvieron en el mes**, con la participación de cada vendedor en las ventas netas y la comparación con el mes anterior. Aparecen los vendedores activos aunque no hayan vendido y quien vendió sin seguir activo.
- **Pedidos entregados a tiempo**: de los pedidos entregados por completo cuya **última entrega vigente** cayó en el mes y que tenían fecha de entrega estimada, el porcentaje cuya última entrega fue en o antes de esa fecha (el mismo día cuenta a tiempo). Los pedidos sin fecha estimada no cuentan; una entrega anulada no cuenta como la última. Se muestra en total y por vendedor.
- **Cuentas por cobrar**: el saldo por cobrar de hoy (total, por vencer, vencido y cuántos clientes lo tienen vencido), con enlace a la cobranza.
- **API**: `GET /api/indicadores?anio&mes` (el mes actual si no se indican), solo `ROLE_ADMIN`; acepta de enero de 2020 al mes actual.
- **Pantalla** *Indicadores* (menú lateral): selector de mes, tres tarjetas (ventas netas con su variación, aro de porcentaje a tiempo con tono verde desde 90 %, ámbar desde 70 % y rojo por debajo, y cuentas por cobrar) y la tabla por vendedor con barra de participación.
- **Pruebas**: `IndicadoresServiceImplTest` (ventas netas, orden, participación, mes anterior, a tiempo en el borde y por vendedor, redondeo, validación de meses), la consulta de pedidos completados contra H2 (última entrega vigente, sin fecha estimada, parciales, otro mes), la matriz de roles y los specs de la pantalla y el servicio.
- **Pendiente**: las comisiones todavía se calculan sobre lo entregado sin restar devoluciones, a diferencia de estas ventas netas.

### Agregado — Historial del pedido: línea de tiempo (Fase 5, paso 2)

En el detalle de cada pedido, el botón **Ver el historial completo** muestra quién creó, editó, autorizó, preparó, empacó, cargó, entregó, devolvió y facturó, y cuándo, de lo más antiguo a lo más reciente. Sin script SQL.

- **Qué junta**: las etapas (con su responsable cuando quien lo hizo no tiene usuario), el historial de auditoría (creación, ediciones con el antes y el después, autorizaciones y rechazos de descuento, de vencimiento y de crédito, devoluciones, cambios de cliente al facturar), las entregas (unidades, fecha real, quién entregó, y su anulación), las facturas emitidas (también las anuladas) y los recibos ligados al pedido (y su anulación). Cada hecho lleva el motivo cuando lo tiene.
- **Sin importes**: la línea de tiempo no muestra montos, así que bodega y el vendedor la ven sin ver dinero.
- **Quién la ve**: administrador, bodega y vendedor (solo sus pedidos; el servidor responde 404 con los de otro). El repartidor no, porque trae el historial de auditoría. `GET /api/pedidos/{id}/historial`.
- **Pantalla**: línea vertical con un icono y color por clase de hecho (creación, edición, autorización, etapa, entrega, devolución, factura, recibo y anulación), quién y cuándo, el motivo y un detalle desplegable para los textos largos. Se carga la primera vez que se abre. Los estilos (`listing-timeline*`) quedan en `styles.css` para reutilizarlos.
- **Pruebas**: `HistorialPedidoServiceImplTest` (orden y mapeo de todas las fuentes, anulaciones, cada acción de auditoría, alcance y pedidos no mayoristas), las consultas nuevas contra H2, la matriz de roles y los specs de la pantalla y el servicio.

### Agregado — Devoluciones de clientes (Fase 5, paso 1)

El administrador registra lo que un cliente devuelve de lo ya entregado de un pedido mayorista. Se registra y queda aprobada en un solo paso.

- **Qué se devuelve**: por línea del pedido y en unidades enteras, hasta lo entregado menos lo ya devuelto. Cada línea va a **reingreso** al inventario de una sucursal (la del pedido o la que elija el administrador) o a **baja** por merma, que registra la devolución pero no mueve el inventario. Pide motivo obligatorio y una fecha no futura (hoy por defecto). El pedido se bloquea mientras se registra, para que dos devoluciones simultáneas de lo mismo no pasen de lo entregado.
- **Pedido facturado**: si el pedido tiene una factura vigente, la devolución debe ligarse a una **nota de crédito** vigente del mismo cliente emitida sobre esa factura o sobre el pedido; la pantalla ofrece las que corresponden y elige sola la única que haya. Sin factura, la nota se ignora.
- **Efecto en lo que debe el cliente**: el valor devuelto (unidades × importe de la línea con descuento / unidades de la línea) **baja el saldo** y lo pendiente del pedido, se ve como "Devuelto" en el estado de cuenta del recibo, entra en la cobranza (baja lo vencido) y en el control del límite de crédito, y aparece como abono en el estado de cuenta imprimible. Para el cobro de un pedido de contado cuenta lo entregado neto de lo devuelto.
- **Inventario**: el reingreso es una entrada nueva (`DEVOLUCION_CLIENTE`) que aparece en el kardex ("Devolución de cliente #n"), el reporte de movimientos y la valorización.
- **Historial**: deja constancia en el historial de auditoría del pedido (acción `DEVOLUCION_CLIENTE`) con el motivo y las líneas.
- **API** (`/api/devoluciones-cliente`, solo `ROLE_ADMIN`): `GET /page/{page}` (cliente y fechas; sin fechas, 90 días), `GET /{id}`, `GET /clientes/{id}/pedidos` y `POST`.
- **Pantalla** *Devoluciones de clientes* (menú lateral): formulario por pasos (cliente, pedido, cantidades y destino por línea, fecha con el selector del sistema, sucursal, nota de crédito y motivo) con el valor devuelto en vivo, y lista con detalle desplegable.
- **Base de datos** (`sql/mayoristas/18_create_devoluciones_cliente.sql`, **requerido antes del jar**; el rollback lo revierte): `devoluciones_cliente`, `devoluciones_cliente_detalle`, `proformas_detalle.cantidad_devuelta` y el valor `DEVOLUCION_CLIENTE` del ENUM de `movimientos_producto` (solo si falta).
- **No incluido**: anular una devolución (se agregó después, ver más arriba).
- **Pruebas**: `DevolucionClienteServiceImplTest` (cantidades, destinos, sucursal, fechas, motivo, pedido facturado y notas, pedidos devolubles), saldo y cobranza netos de devoluciones, las consultas contra H2, la matriz de roles y los specs de la pantalla y el servicio.

### Cambiado — Estilos de Comisiones, Recibos de caja, Cobranza y Autorizaciones

Las pantallas nuevas usaban botones y campos de Bootstrap sin estilo y no cuadraban con el resto del sistema. Ahora usan el mismo lenguaje que los listados.

- **Estilos compartidos** (`src/styles.css`, clases `listing-*`): botones de página (primario, secundario, destructivo, compacto, fantasma), botones de tabla con icono y tooltip, barra de filtros, campos etiquetados (mismo borde, radio y foco que la búsqueda de los listados), casillas, tarjetas de resumen, avisos de página, estados vacíos, píldoras de estado y tabla de datos con encabezado oscuro, pie de totales, marca de severidad por fila y vista móvil en tarjetas.
- **Comisiones**: selector de año con botones de icono, metas en filas con número y campos compartidos, resumen de entregado y a pagar, píldoras para el porcentaje, la situación y "Completó el año".
- **Recibos de caja**: botón primario en la cabecera, formulario en paneles, estado de cuenta del cliente en tarjetas, píldoras de forma de pago y de anulado, acciones con icono.
- **Cobranza**: aviso rojo de vencidos con botón, tarjetas con los tramos de antigüedad, filtros en barra, píldoras de atraso y estado vacío.
- **Autorizaciones**: botones Aprobar / Rechazar / Autorizar con el estilo compartido, contadores como píldoras y estados vacíos.

- **Selector de fechas del sistema**: los campos de fecha que habían quedado como `<input type="date">` nativo usan ahora `app-date-range-picker`, como el resto de pantallas: filtros y fecha de pago de *Recibos de caja*, filtros y fecha de entrega de *Pedidos*, y la fecha de entrega estimada de la proforma mayorista. El servidor sigue validando las fechas (no futura, hasta 7 días atrás en las entregas).

- **Franja de filtros de *Recibos emitidos***: los controles quedaban desalineados (alturas y etiquetas distintas). Ahora son fichas que comparten la anatomía del selector de fechas (cuadro de icono, rótulo en mayúsculas pequeñas y valor) y la misma altura, en una cuadrícula con las etiquetas arriba y los bordes inferiores alineados: rango de fechas, sucursal, interruptor de anulados y las acciones *Filtrar* y *Quitar filtros* (nuevo). Los estilos (`listing-filter-grid`, `listing-tile*`, `listing-switch`) quedan en `styles.css` para reutilizarlos; los campos de formulario `listing-field` miden 50 px como el selector de fechas.

### Corregido — Columna `descontado_a_mano` de las entregas

`ProformaEntrega.descontadoAMano` se traducía a la columna `descontadoamano` (la estrategia de nombres de Spring no separa la «A» aislada), pero el script 10 crea `descontado_a_mano`: registrar una entrega habría fallado en una base real. Las pruebas con H2 no lo detectaron porque H2 crea la columna con el mismo nombre que Hibernate; lo encontró el verificador de esquema al arrancar el backend en local. Ahora la columna se mapea de forma explícita.

### Agregado — Arranque local

`.claude/launch.json` incluye el backend (`.claude/run-backend.cmd`, perfil `dev`, JDK 21) además del frontend.

### Agregado — Cobranza: cuentas por cobrar, estado de cuenta y alertas de vencidos (Fase 4, paso 4)

Cierra la Fase 4. Sin script SQL nuevo.

- **Cuentas por cobrar** (`GET /api/cobranza/saldos`): una fila por cliente con deuda, repartida por antigüedad en *por vencer*, *vencido 1-30*, *31-60* y *más de 60 días*, con el total vencido, el total y los días de atraso del pedido más vencido. Un pedido a crédito vence cuando pasan sus días de crédito desde la **primera entrega**; uno de contado entregado sin cobrar (por autorización del administrador) vence el día de la entrega. El día del vencimiento todavía no cuenta como vencido. Lo que se debe de cada pedido sale del mismo cálculo del saldo: los recibos ligados al pedido lo pagan y los abonos sin pedido pagan primero lo más antiguo. Los clientes van de mayor a menor vencido; uno al día no aparece.
- **Alertas**: la pantalla muestra una franja roja con cuántos clientes tienen saldo vencido y por cuánto (de toda la cartera, sin importar los filtros) y un botón para ver solo los vencidos; las filas se marcan según el atraso (hasta 30, hasta 60 y más de 60 días).
- **Pantalla** *Cobranza* (menú lateral, administrador y cobrador): filtros por cliente, ruta y solo vencidos, totales de lo que se está viendo, y por fila la impresión del estado de cuenta.
- **Reporte "Cuentas por cobrar"** (PDF y Excel, `GET /api/cobranza/saldos/reporte`): las mismas columnas con totales, respetando los filtros de ruta y solo vencidos.
- **Estado de cuenta imprimible** (`GET /api/cobranza/clientes/{id}/estado-cuenta`): las entregas vigentes (cargos) y los recibos vigentes (abonos) del cliente por fecha, con el saldo corrido, sus datos, su límite y sus días de crédito. En un mismo día el cargo va antes que el abono; un saldo negativo es dinero a favor.
- **Roles**: solo `ROLE_ADMIN` y `ROLE_COBRADOR`.
- **Pruebas**: `CobranzaServiceImplTest` (tramos y bordes de antigüedad, contado vencido al entregar, abonos que pagan lo más antiguo, filtros, orden, estado de cuenta), `ReporteCobranzaServiceImplTest` (genera el Excel y los PDF reales), las consultas contra H2, la matriz de roles y los specs de la pantalla y el servicio.
- **No incluido**: aviso en el menú lateral ni envío de recordatorios; la alerta se ve al abrir *Cobranza*.

### Agregado — Control del límite de crédito de los clientes (Fase 4, paso 3)

Un pedido a crédito que pasa del límite del cliente queda bloqueado hasta que un administrador lo autorice.

- **Cuándo pasa del límite**: cuando el total del pedido (con descuento) más lo que el cliente ya comprometió supera su límite de crédito. Lo comprometido es su **saldo pendiente** (entregado menos recibos vigentes) **más lo que falta por entregar de sus otros pedidos a crédito vigentes**, para que dos pedidos que caben cada uno por separado no pasen del límite juntos. Un saldo a favor no suma ni resta. Se evalúa al crear y al editar el pedido; un pedido de contado o minorista nunca se evalúa.
- **Qué bloquea**: con la autorización de crédito `PENDIENTE` o `RECHAZADA` el pedido no se prepara, no se entrega y la proforma no se factura, igual que con un descuento sin autorizar. Es independiente de la autorización del descuento: un pedido puede necesitar las dos.
- **Autorización**: el administrador autoriza o rechaza desde la bandeja *Autorizaciones*, en la nueva sección "Pedidos que pasan del límite de crédito" (cliente, total, límite y cuánto pasa), con motivo obligatorio; queda en el historial del pedido (`AUTORIZACION_CREDITO` / `RECHAZO_CREDITO`). `GET /api/autorizaciones/credito`, `POST /{id}/aprobar-credito` y `/{id}/rechazar-credito`, solo `ROLE_ADMIN`.
- **Ediciones**: si el pedido ya estaba autorizado y la edición no pasa del exceso autorizado, conserva la autorización; si pasa más, o si estaba rechazado, vuelve a quedar pendiente.
- **Avisos**: al guardar la proforma, el vendedor ve que el pedido quedó pendiente de autorización y por cuánto pasa; el detalle del pedido muestra el aviso mientras esté pendiente o rechazado.
- **Base de datos** (`sql/mayoristas/17_proformas_autorizacion_credito.sql`, **requerido antes del jar**; el rollback lo revierte): `proformas.credito_autorizacion` y `proformas.exceso_credito`.
- **Pruebas**: evaluación del límite (cabe, pasa por un centavo, líneas sin importe, contado, conserva o pierde la autorización al editar), exposición de crédito (otros pedidos por entregar, saldo a favor), bloqueos de preparar, entregar y facturar, decisiones del administrador, consulta de la bandeja contra H2, matriz de roles y specs de la bandeja.

### Agregado — Condición de pago, saldo del cliente y entrega condicionada al cobro (Fase 4, paso 2)

El control del límite de crédito al confirmar el pedido llega en el paso 3; la cobranza (antigüedad, estado de cuenta imprimible, alertas), en el 4.

- **Condición de pago del pedido** (`CONTADO` o `CREDITO`): la proforma mayorista la lleva y la pantalla la ofrece. A crédito solo se acepta si el cliente tiene **límite y días de crédito**; si no, el servidor responde 422 y el pedido debe ser de contado. Los días del cliente quedan guardados en el pedido (un cambio posterior no altera los plazos ya pactados). Una proforma minorista es siempre de contado.
- **Saldo del cliente** (`GET /api/saldos/clientes/{id}`): la deuda nace al **entregar**. Saldo = valor de lo entregado (entregas vigentes, a precio de línea con descuento) menos los recibos vigentes; un saldo negativo es dinero a favor del cliente (por ejemplo, un pago adelantado). Un recibo ligado a un pedido, o a una factura suya, paga ese pedido; los abonos sin pedido se aplican a lo más antiguo (por fecha de la primera entrega). También muestra el crédito disponible (límite menos el saldo pendiente) y, en pedidos a crédito, el vencimiento (primera entrega + días de crédito). `GET /api/saldos/pedidos/{id}` da lo mismo para un pedido. Lo consultan el administrador, el cobrador y el vendedor (solo sus clientes y pedidos); bodega y el repartidor no ven dinero.
- **Entrega condicionada al cobro**: un pedido **de contado** no se entrega si lo cobrado con recibos ligados a él no cubre lo ya entregado más lo de esta entrega (responde 422 con lo que falta). Solo el **administrador** puede marcar "entregar aunque no esté cobrado"; queda anotado en las etapas del pedido, y el repartidor nunca puede. Un pedido a crédito no se condiciona al cobro.
- **Pantallas**: la proforma mayorista trae el selector de condición de pago; el detalle del pedido muestra condición, lo cobrado y el aviso de lo que falta cobrar; el formulario de recibo muestra el estado de cuenta del cliente elegido y deja escoger el pedido al que paga (propone el monto que falta).
- **Base de datos** (`sql/mayoristas/16_proformas_condicion_pago.sql`, **requerido antes del jar**; el rollback lo revierte): `proformas.condicion_pago` (CONTADO por defecto) y `proformas.dias_credito`.
- **Pruebas**: `SaldoClienteServiceImplTest` (saldo, adelantos, factura, reparto de abonos, límite), `SaldoRepositoryTest` (las consultas contra H2), condición de pago en `ProformaMayoristaServiceImplTest`, el cobro en `EntregaPedidoServiceImplTest`, la matriz de roles y los specs de pedidos, recibos y saldo.

### Agregado — Recibos de caja (Fase 4, paso 1)

Abonos y depósitos de los clientes con número propio por sucursal. Todavía no mueven ningún saldo: el saldo del cliente y el control de crédito llegan en los pasos 2 y 3.

- **Emisión**: los emiten el administrador y el cobrador desde su propia sucursal (el administrador puede elegir otra). Piden cliente, monto mayor que cero (hasta Q9,999,999.99), forma de pago (efectivo, depósito o transferencia; las dos últimas exigen referencia) y fecha de pago (hoy por defecto, nunca futura). Pueden indicar el pedido o la factura a la que pagan, que deben ser del cliente y no estar anulados; la pantalla de este paso aún no ofrece ese selector (llega con el saldo, en el paso 2), pero la API ya lo acepta.
- **Numeración**: un correlativo por sucursal con la fila bloqueada al emitir, para que dos recibos simultáneos no repitan número. Se muestra como `S2-000015`. Una sucursal sin correlativo empieza en 1.
- **Anulación**: solo el administrador, con motivo obligatorio; el recibo se conserva con quién, cuándo y por qué, y dejará de contar para el saldo. Un recibo no se anula dos veces.
- **Alcance**: el cobrador solo lista, ve e imprime los recibos de su sucursal (los de otra responden 404); el administrador ve todas o filtra por sucursal. El vendedor y el repartidor no tienen acceso.
- **Impresión**: PDF en media carta con número, sucursal, fecha, monto, cliente y NIT, concepto (pedido, factura o abono a la cuenta), forma de pago, referencia, quién emitió, línea de firma del cliente y la marca "RECIBO ANULADO" con el motivo cuando corresponde. No incluye el monto en letras.
- **API** (`/api/recibos`): `GET /page/{page}` (filtros de sucursal, cliente, fechas y anulados; sin fechas, los últimos 30 días), `GET /{id}`, `POST`, `POST /{id}/anular`, `GET /{id}/imprimir`.
- **Pantalla** *Recibos de caja* (menú lateral): formulario con búsqueda de cliente, lista con filtros, impresión y anulación. Al emitir se abre el PDF.
- **Base de datos** (`sql/mayoristas/15_create_recibos_caja.sql`, **requerido antes del jar**; el rollback lo revierte): `recibos_caja` y `recibos_correlativos` (con una fila por cada sucursal existente).
- **Pruebas**: `ReciboCajaServiceImplTest` (numeración, alcance por sucursal, validaciones, documentos del cliente, anulación), `ReciboCajaReporteTest` (la plantilla con su consulta real contra H2), la matriz de roles y los specs de la pantalla y el servicio.

### Cambiado — Código de cliente como UUID y comisión de quien ingresa a mitad de año

- **Código de cliente**: ya no se escribe ni se propone el teléfono. El servidor asigna un **UUID** al crear el cliente; no se edita (lo que mande la petición se ignora), es único y sigue sirviendo para buscar. Un cliente anterior sin código lo recibe la próxima vez que se guarda. La pantalla del cliente lo muestra de solo lectura y se quitó "Usar el teléfono". Script `sql/mayoristas/14_codigo_cliente_uuid.sql` (**requerido antes del jar**; amplía la columna a 36 caracteres y asigna UUID a los mayoristas sin código; el rollback borra los UUID para volver a 30).
- **Comisión de quien ingresa a mitad de año** (un año son 365 días; se toma como ingreso la fecha de alta del usuario): quien ingresó **antes del 1 de enero del año anterior** o ese día cobra normal; quien ingresó **durante el año anterior** cobra en un solo bloque, en la liquidación de este año, todo lo entregado **desde su ingreso** hasta el 31 de diciembre (ya cumplió los 365 días); quien ingresó **durante este año** queda *pendiente*: aún no cumple los 365 días, la liquidación no le permite cobrar y se le paga con la del año siguiente. Quien ingresa después del año no aparece. La pantalla lo indica por vendedor, y el administrador sigue marcando quién completó el año.
- **Pruebas**: bloque, pendiente, enero 1, sin fecha de alta, ingreso posterior; códigos UUID (asignado, ignorado si lo manda la petición, se regenera si choca, se conserva al actualizar).
- **Pendiente del cliente**: el vendedor de la Ruta 3 se registrará a mano después.

### Agregado — Pantalla y reporte de comisiones (Fase 3B)

Cierra la Fase 3. Pantalla *Comisiones* (menú lateral, solo administrador) y reporte del año.

- **Metas del año**: se editan en la pantalla (agregar, quitar, guardar, descartar cambios) y se pueden copiar de otro año cuando el año no tiene ninguna. En un año liquidado quedan de solo lectura.
- **Avance y liquidación**: tabla por vendedor con fecha de alta, entregado, meta alcanzada, porcentaje, comisión, lo que falta para la siguiente meta y la casilla **Completó el año** (se habilita cuando el año termina). El total a pagar suma solo a los marcados. "Liquidar el año" pide confirmación con el total; no se puede liquidar con cambios de metas sin guardar, sin metas o antes de que termine el año, y la pantalla dice por qué. Un año liquidado muestra lo guardado, quién y cuándo, y "Deshacer liquidación" pide motivo.
- **Reporte "Comisiones del año"** (PDF y Excel, `GET /api/comisiones/reporte?anio&formato`, solo `ROLE_ADMIN`): metas del año, estado (en curso, pendiente de liquidar o liquidado por quién y cuándo), una fila por vendedor y totales. Se descarga desde los botones de la pantalla de Comisiones, no desde *Reportes*, porque depende del año y de la liquidación y no de un período.
- **Pruebas**: `ReporteComisionesServiceImplTest` genera PDF y Excel reales (metas, filas, Sí/No/Sin definir, año vacío), `comisiones.component.spec.ts` y `comision.service.spec.ts`, y la matriz de roles incluye el reporte.
- **Base de datos**: sin cambios nuevos; requiere el script 13 de la 3A.

### Agregado — Cálculo y liquidación de comisiones de vendedores (Fase 3A, backend)

Solo servidor y base de datos; la pantalla y el reporte vienen en la 3B.

- **Regla**: porcentaje **plano** sobre todo lo entregado en el año por los pedidos mayoristas del vendedor, según el nivel más alto alcanzado (no por tramos). Ej.: Q750,000 con metas 300k/700k/1M/2M → nivel de 700k al 1.00 % = Q7,500. Lo entregado se toma por la fecha real de entrega, al precio con descuento de la línea del pedido; entregas anuladas y pedidos anulados no cuentan.
- **Metas por año** (`comisiones_metas`): el administrador define un juego por año (monto y porcentaje crecientes, porcentaje ≤ 100) y puede copiar las de otro año. El script siembra 2026: 300k→0.50 %, 700k→1.00 %, 1M→1.50 %, 2M→2.00 %.
- **Quién cobra**: solo quien el administrador marque como que **completó el año** al liquidar; los demás quedan en cero (sin comisión ese año).
- **Liquidación**: solo de un año terminado, con metas y no liquidado antes; se guarda por vendedor (entregado, meta, porcentaje, comisión) con quién y cuándo, y ya no se recalcula. **Deshacer** exige motivo y deja constancia en `comisiones_deshechas` con lo que se había liquidado.
- **API** (`/api/comisiones`, solo `ROLE_ADMIN`): `GET/PUT /metas`, `POST /metas/copiar`, `GET /avance`, `POST /liquidar`, `POST /deshacer`.
- **Base de datos** (`sql/mayoristas/13_create_comisiones.sql`, **requerido antes del jar**; el rollback lo revierte): tres tablas nuevas.
- **Pruebas**: `ComisionServiceImplTest` (nivel, bordes de meta, parciales, redondeo, validaciones, liquidar/deshacer) y la matriz de roles.

### Agregado — Reporte de ventas entregadas por ruta y vendedor (Fase 2C)

Cierra la Fase 2. Solo lectura, para el administrador, sin script SQL.

- **Qué cuenta**: lo que realmente se entregó al cliente. Cada unidad entregada vale lo que cuesta en su línea del pedido, con descuento (`sub_total_descuento / cantidad`); una entrega parcial suma solo lo entregado, una entrega anulada no cuenta, y el período se toma por la **fecha real de entrega**, no por la de registro ni la de emisión. Solo pedidos mayoristas.
- **Formato**: un solo reporte agrupado por ruta y, dentro de cada ruta, por vendedor, con pedidos, unidades y valor entregado, subtotal por ruta y total general. Filtros de período, ruta y vendedor (opcionales); PDF y Excel. En *Reportes*, categoría Mayoristas, "Ventas entregadas". `GET /api/reportes/mayoristas/entregado`, solo `ROLE_ADMIN`.
- **Base de las comisiones**: la Fase 3 usará estos mismos datos (monto entregado por vendedor).
- **Pruebas**: `EntregadoPorRutaVendedorReporteTest` ejecuta la consulta real contra H2 con entregas parciales, anuladas, de otro mes y de una proforma minorista; la consulta también se corrió contra MySQL. Más casos en la validación de filtros, la matriz de roles y los specs de reportes.
- **A tener presente**: el valor sale del importe de línea del pedido guardado; una proforma anterior cuyo importe de línea se guardó con otra semántica (ver las líneas de proforma históricas con `sub_total` inconsistente) se valoraría mal. Los pedidos mayoristas son nuevos y no tienen ese problema.

### Agregado — Rol Repartidor y piloto del pedido (Fase 2C)

Los pilotos entran con su propio usuario, ven solo los pedidos que se les asignaron y registran la entrega. Los reportes de lo entregado son el siguiente paso de la 2C.

- **Rol** `ROLE_REPARTIDOR` y **piloto asignado**: "Cargar y salir a ruta" exige elegir al piloto (un usuario activo con ese rol; si no hay ninguno, la pantalla avisa que el administrador debe crearlo). Administrador y bodega pueden cambiarlo mientras el pedido esté en ruta o con entrega parcial, y el cambio queda en las etapas. Nuevos `GET /api/pedidos/repartidores` y `POST /api/pedidos/{id}/repartidor`.
- **Alcance del repartidor** (lo hace valer el servidor, no la pantalla): lista y detalle solo de sus pedidos en ruta o parciales; cualquier otro pedido responde 404. No imprime picking, empaque ni nota de envío, no avanza ni regresa etapas, no cambia el piloto, no anula entregas ni lee clientes, facturas, productos con costo ni reportes. Quien combina el rol con otro rol operativo conserva el alcance de ese otro rol.
- **Entrega del repartidor**: descuenta siempre de la sucursal del pedido y nunca con la casilla "ya descontado a mano"; esas decisiones quedan para bodega y el administrador. Lo que mande la petición sobre sucursal, casilla o nombre se ignora. Un pedido sin sucursal se rechaza.
- **Pantalla**: entra directo a *Pedidos* al iniciar sesión, con la lista adaptada al celular, una columna "Piloto" para todos los roles y el formulario de entrega reducido a fecha y cantidades. La página de inicio no consulta productos ni clientes para él.
- **Cierre de lecturas sin rol** (revisión que motivó el rol): además de los clientes, `GET /api/facturas/page/{page}` estaba abierta al público (sin token y sin rol), y `GET /api/proformas/generate/{id}` (imprime una proforma con precios y cliente), `GET /api/notas-credito`, `GET /api/usuarios/cajero` y `GET /api/usuarios/page/{page}` no declaraban roles, así que cualquier usuario con sesión —un repartidor incluido— las alcanzaba. Ahora todas declaran sus roles. `ControladoresSinRolTest` falla si aparece un endpoint nuevo sin rol que no sea del catálogo público (productos, marcas, tipos de producto, roles y rangos de correlativos, que siguen públicos a propósito).
- **Base de datos** (`sql/mayoristas/12_create_rol_repartidor.sql`, **requerido antes del jar**; el rollback lo revierte): el rol y `proformas.id_repartidor`.
- **Pruebas**: alcance del repartidor en `UtilsVendedorTest` y `AmbitoVendedorServiceImplTest`, piloto y lista en `PedidoServiceImplTest`, entrega del repartidor en `EntregaPedidoServiceImplTest`, la matriz de roles (el repartidor no llega a ningún otro controlador), lecturas sin token en `LecturaPublicaClientesTest`, y specs de `PedidosComponent`, `PedidoService` y `AuthService`.
- **Por hacer en la 2C**: el reporte de ventas entregadas por ruta y vendedor.

### Corregido — Lectura pública de clientes (seguridad)

`GET /api/clientes`, `/api/clientes/{id}` y `/api/clientes/page/**` estaban abiertas al público en `SecurityConfig` y sus métodos no tenían rol: cualquiera, sin iniciar sesión, podía leer todos los clientes (nombre, NIT, dirección, teléfono, ruta y datos de crédito). Eso también dejaba sin efecto el alcance por ruta del vendedor.

- Se quitaron del acceso sin token y **todas** las lecturas de `ClienteApiController` declaran sus roles: administrador, cobrador, inventario y vendedor (este último limitado a sus rutas, como antes). Bodega y el futuro repartidor ya no leen clientes; la cantidad de NIT inválidos sigue siendo solo del administrador.
- Las lecturas públicas de productos, marcas, tipos de producto y roles no cambian: no son datos de clientes. Las facturas ya estaban protegidas por rol en cada método.
- **Pruebas**: `LecturaPublicaClientesTest` (con la seguridad real: sin token ninguna lectura de clientes responde, y toda lectura declara sus roles sin incluir a bodega ni al repartidor).
- **Cuidado al desplegar**: el frontend envía el token en cada llamada, así que no debería notarse; si alguna pantalla anterior leía clientes sin iniciar sesión, dejará de funcionar.

### Agregado — Origen de los movimientos de inventario en el kardex y en el reporte de movimientos (Fase 2B, paso 3)

Cierra la 2B. Solo reportes: sin script SQL ni cambios en la API.

- **Kardex de producto**: cada movimiento muestra su **origen** junto al documento: Factura, Entrega de pedido mayorista, Nota de crédito, Compra, Despacho de bodega, Devolución a bodega, Manual (entradas y salidas sin documento) o No registrado. Si el movimiento no guardó su documento, el origen sale del tipo (una venta sin documento es de factura). El documento ahora nombra también la entrega de pedido ("Entrega de pedido #77") y la devolución a bodega, que antes salía como "Ajuste manual".
- **Movimientos y saldo de inventario**: las salidas se separan en tres columnas, **por factura**, **por entrega de pedido** y **otras salidas** (manuales, notas de crédito, compras anuladas); las anulaciones cuentan como entradas. Las columnas de entradas, salidas y saldo no cambian.
- **Entregas en el saldo**: la entrega de pedido ya salía como salida y su anulación como entrada en el kardex, el reporte de movimientos y la valorización (paso 1); ahora además se nombran.
- **Una sola entrega de pedido**: no se distingue entre "entrega de proforma" y "de proforma mayorista" porque solo los pedidos mayoristas se entregan.
- **Pruebas**: `OrigenMovimientosReportesTest` ejecuta la consulta real de cada plantilla contra H2 en modo MySQL con todos los orígenes (incluido un tipo vacío de la carga histórica); las consultas también se corrieron contra MySQL con los datos de desarrollo.
- **No incluido**: la lista de movimientos de la pantalla *Movimientos de producto* sigue mostrando el tipo técnico (por ejemplo `ENTREGA_PROFORMA`); la salida manual que declara su proforma (no hizo falta con la casilla "ya descontado a mano").

### Cambiado — Inventario de la factura de un pedido mayorista (Fase 2B, paso 2)

La entrega y la factura de un mismo pedido ya no descuentan dos veces. Solo aplica a una factura emitida desde un pedido mayorista (`id_proforma_origen` de una proforma mayorista); una venta directa o de una proforma minorista se comporta exactamente como antes.

- **Emitir**: la factura descuenta, por producto, lo facturado menos lo que el pedido ya había descontado (por una entrega, una salida declarada "a mano" o una factura anterior). Si ya estaba todo descontado no hay movimiento. La validación de existencias exige solo lo que se va a descontar, no la cantidad completa. Lo que descontó cada línea queda en `facturas_detalle.cantidad_descontada`, y el pedido suma lo descontado a sus líneas.
- **Anular**: la factura devuelve solo lo que ella descontó, menos lo que ya se entregó físicamente, que sigue fuera del inventario. Ejemplo: facturar 10, entregar 10 y anular la factura devuelve 0. Las facturas anteriores a este control (`cantidad_descontada` nulo) devuelven la cantidad completa, como siempre.
- **Reemitir** una factura anulada de un pedido ya entregado no descuenta de nuevo.
- **Seguridad**: lo descontado que trae la petición se ignora; lo fija el servidor. El inventario sigue siendo atómico con FEL: se descuenta antes de certificar y se revierte con la transacción.
- **Base de datos** (`sql/mayoristas/11_facturas_detalle_cantidad_descontada.sql`, **requerido antes del jar**; el rollback lo revierte): una columna nula en `facturas_detalle`.
- **Pruebas**: `InventarioFacturaPedidoServiceImplTest` (17 casos de la aritmética), `EntregaFacturaIntegracionTest` (H2 con los servicios reales: entregar y facturar, facturar y entregar, entrega parcial, anular, reemitir, descontado a mano y ventas normales; el inventario queda exacto en cada secuencia) y casos nuevos en `FacturaServiceImplTest` (el camino de pedido en emitir y en anular, y que la venta normal no cambia).
- **Pendiente en la 2B**: la columna "Origen" del kardex y del reporte de movimientos (paso 3).

### Agregado — Entrega del pedido mayorista e inventario (Fase 2B, paso 1)

Un pedido en ruta se entrega al cliente, completo o por partes, y la entrega descuenta el inventario de la sucursal que elige quien la registra. Todavía no cambia el comportamiento de la factura ni de su anulación (paso 2 de la 2B) ni agrega el origen al kardex (paso 3).

- **Registrar la entrega** (`POST /api/pedidos/{id}/entregas`): administrador, bodega y vendedor (solo los suyos). El pedido debe estar `EN_RUTA` o `ENTREGADA_PARCIAL` y con el descuento autorizado. Se indica la sucursal de la que se descuenta (se propone la de la proforma), la **fecha real de entrega** (no futura, hasta 7 días atrás y no anterior a la emisión) —distinta de la fecha de registro, que pone el sistema—, quién entregó (texto opcional), observaciones y la cantidad por línea, sin pasar de lo pendiente.
- **Descuento sin duplicar**: cada línea guarda lo entregado y lo ya descontado por este pedido. La entrega descuenta solo lo entregado que todavía no estaba descontado; una casilla "ya descontado a mano" (por entrega completa) la registra sin mover inventario. Movimiento nuevo `ENTREGA_PROFORMA`, con la entrega como documento de origen. Sin existencias en la sucursal elegida responde 422 con el producto y lo disponible, y no se registra nada.
- **Etapas**: entregar todo deja el pedido `ENTREGADA`; entregar una parte, `ENTREGADA_PARCIAL`, y lo pendiente se entrega después sin repetir picking. El administrador puede **cerrar con lo entregado** un pedido parcial, con motivo, sin mover inventario.
- **Anular una entrega** (solo administrador, motivo obligatorio): reingresa a la sucursal lo que esa entrega descontó, baja lo entregado y el pedido vuelve a `EN_RUTA` (o queda parcial si tiene otras entregas). Se conserva en el historial. Si el pedido tiene una factura vigente se pide anularla primero.
- **Un pedido entregado no se edita ni se anula**; las ediciones siguen reiniciando un pedido aún no entregado.
- **Pantalla Pedidos**: botón "Registrar entrega" en la fila y el detalle, con formulario (sucursal, fecha, quién entregó, cantidades, casilla, observaciones), lista de entregas con su anulación y "Cerrar con lo entregado".
- **Base de datos** (`sql/mayoristas/10_create_entregas_pedido.sql`, **requerido antes del jar**; `99_rollback_mayoristas.sql` lo revierte menos los valores del ENUM): `cantidad_entregada` y `cantidad_descontada` en `proformas_detalle`, `proformas_entregas` y `proformas_entregas_detalle`. **`movimientos_producto.tipo_movimiento` es un ENUM de MySQL**: el script le agrega `ENTREGA_PROFORMA` y `ANULACION_ENTREGA_PROFORMA` leyendo la lista actual, sin repetir valores. También agrega `ENTREGA_PRODUCTO_NOTA`, que el código ya usaba para las notas de crédito pero no estaba en la lista de la base de desarrollo, así que entregar una nota de crédito allí fallaba al guardar.
- **Reportes**: el kardex, el reporte de movimientos y la valorización clasifican los dos tipos nuevos como salida y entrada, y el kardex los nombra ("Entrega de pedido").
- **Pruebas**: `EntregaPedidoServiceImplTest` (reglas), `EntregaPedidoIntegracionTest` (H2 con los servicios reales: el inventario queda exacto al entregar, por partes y al anular), matriz de roles y specs de `PedidosComponent` y `PedidoService`.
- **Pendiente en la 2B**: que la factura descuente solo lo no descontado y que su anulación devuelva solo lo que descontó (paso 2), y la columna "Origen" del kardex y del reporte de movimientos (paso 3).

### Agregado — Etapas del pedido mayorista y documentos de bodega (Fase 2A)

Una proforma mayorista es un pedido desde que se crea: bodega lo prepara, lo empaca y lo carga a ruta, con quién y cuándo de cada paso y los tres documentos impresos. No toca el inventario; la entrega que descuenta existencias llega en la 2B.

- **Etapas**: `CONFIRMADA` (al crearse la proforma, sin botón de confirmar) → `EN_PREPARACION` (picking) → `EMPACADA` → `EN_RUTA`. Las dos siguientes (`ENTREGADA_PARCIAL`, `ENTREGADA`) existen pero se alcanzan en la 2B. Cada paso queda en `proformas_etapas` con el usuario de la sesión, la fecha y, si quien lo hizo no tiene usuario (el personal rota), su nombre escrito.
- **Reglas** (422 con mensaje claro): no avanza un pedido con el descuento pendiente de autorización o rechazado; después de `EN_RUTA` no avanza en bodega; una proforma minorista no es un pedido. Solo el administrador puede regresar un pedido una etapa, con motivo obligatorio.
- **Editar un pedido**: si bodega ya empezó (preparación, empacado o en ruta) la edición lo devuelve a `CONFIRMADA` y lo anota en las etapas, porque la hoja de picking impresa quedó desactualizada; la pantalla de la proforma lo avisa antes de guardar. Un pedido ya entregado no se edita.
- **Documentos** (PDF, sin precios ni firma de recibido): hoja de picking y lista de empaque (`pedido_bodega.jrxml`, una plantilla con el parámetro `DOCUMENTO`; la observación general solo sale en el empaque) y nota de envío por ruta (`nota_envio.jrxml`: los pedidos empacados por cargar y los que salieron a ruta ese día). Plantillas escritas a mano; no abrir con Jaspersoft Studio 7.
- **API** (`/api/pedidos`): lista (por ruta, etapa y fechas de emisión; sin etapa trae los que faltan por entregar), detalle, `avanzar`, `retroceder`, `picking`, `empaque` y `nota-envio`. El administrador y bodega operan; el vendedor ve solo sus pedidos y no opera.
- **Pantalla *Pedidos*** (menú para administrador, bodega y vendedor): pedidos por ruta con sus unidades, el botón de la etapa siguiente, los documentos, el detalle con las etapas y la nota de envío por ruta.
- **Base de datos** (`sql/mayoristas/09_create_etapas_pedido.sql`, **requerido antes del jar**; `99_rollback_mayoristas.sql` lo revierte): `estado_pedido` en `proformas` (las mayoristas existentes quedan confirmadas) y la tabla `proformas_etapas`.
- **Supuestos por confirmar**: que la proforma mayorista queda confirmada al crearse; que el responsable de cada etapa se escribe como texto opcional además del usuario; y que un pedido con el descuento pendiente no se prepara.
- **Pruebas**: `PedidoServiceImplTest`, `PedidoRepositoryTest` (H2), `PedidoDocumentosTemplateTest`, `PedidoDocumentoServiceImplTest`, casos nuevos en `ProformaServiceImplTest`, `ProformaApiControllerAuditoriaTest` y `VendedorSeguridadTest`, y specs de `PedidosComponent`, `PedidoService` y `CreateProformaComponent`.
- **No incluido**: la entrega, el descuento de inventario y el rol repartidor (2B y 2C), y los reportes sobre lo entregado.

### Agregado — Reportes de mayoristas (Fase 1, paso 7)

Tres reportes de solo lectura para el administrador, en una categoría nueva "Mayoristas" de la pantalla de reportes. No dependen de la entrega del pedido ni agregan tablas: es el último paso de la Fase 1.

- **Clientes por ruta**: clientes mayoristas agrupados por ruta, con el vendedor, negocio, encargado, teléfono, dirección, categoría y días de crédito, y el conteo por ruta. Filtro opcional de ruta. Un cliente sin ruta aparece en "Sin ruta".
- **Clientes sin compras**: clientes mayoristas sin compras en el período, con su última compra ("Nunca" si no tienen ninguna) y los días sin comprar. Se considera compra una proforma o una factura no anulada: la proforma es el documento de venta principal y no todo se factura. Filtros de período y ruta.
- **Proformas por vendedor**: proformas mayoristas generadas y facturadas por cada vendedor en el período, con valor proformado, valor facturado, porcentaje facturado y cuántas esperan autorización. Facturada es tener una factura vigente (no anulada), aunque se haya emitido después del período. Es distinto del reporte "Conversión a ventas", que es de un solo usuario y de todo tipo de proforma.
- **API**: `GET /api/reportes/mayoristas/{clientes-ruta|clientes-sin-compras|proformas-vendedor}`, solo `ROLE_ADMIN`, PDF o XLSX. Plantillas `mayoristas_*.jrxml` escritas a mano (no abrir con Jaspersoft Studio 7). Sin script SQL.
- **Pruebas**: `ReportesMayoristasTemplateTest` (cada plantilla con datos de ejemplo), `ReporteMayoristasServiceImplTest` (validación de filtros), la matriz de roles y specs de `ReportesComponent` y `ReporteService`. Las tres consultas se ejecutaron contra MySQL para validar la sintaxis; la base de desarrollo no tiene clientes mayoristas, así que el resultado con datos reales queda por revisar.
- **No incluido**: reporte por sucursal (los clientes no tienen sucursal; la ruta sí) y acceso del vendedor a sus propios reportes.

### Agregado — Reemisión de la factura de una proforma (Fase 1, paso 6)

Una proforma cuya factura se anuló se puede volver a facturar, por ejemplo con el NIT correcto. No se facturan cantidades por partes: eso no se necesita (el cliente solo pidió reemisión y cambio de cliente).

- **Regla**: una proforma admite una sola factura *vigente*; las anuladas no cuentan. Antes bastaba una factura, anulada o no, para bloquearla. El mensaje al intentar facturar una proforma ya facturada indica anular primero esa factura.
- **Sin carrera**: al emitir se bloquea la fila de la proforma y las facturas vigentes se leen con lectura bloqueante, así dos facturas simultáneas de la misma proforma no pasan la validación a la vez (la segunda ve la primera). El índice único ya no está.
- **"Facturada" = factura vigente**: la edición del vendedor, la edición y anulación de una mayorista y la bandeja de vencidas ignoran las facturas anuladas.
- **Listado**: con una proforma reemitida hay dos facturas; el listado muestra la vigente (o, si solo hubo anuladas, la última) y no repite la proforma. El botón de facturar se habilita cuando la factura está anulada. El reporte de conversión solo une facturas no anuladas, para no duplicar proformas.
- **Cambiar cliente**: ya existía en la pantalla de facturar desde proforma (paso 0.5), y desde el paso 5 el cambio queda en el historial; no hizo falta tocarlo.
- **Base de datos** (`sql/mayoristas/08_facturas_proforma_reemision.sql`, **requerido antes del jar**; `99_rollback_mayoristas.sql` lo revierte, y falla si ya hay proformas reemitidas): se crea `idx_facturas_proforma_origen` y luego se borra `uq_facturas_proforma_origen`, en ese orden porque la llave foránea necesita un índice.
- **Pruebas**: `FacturaServiceImplTest` (reemisión y orden del bloqueo), `ProformaListadoFacturacionTest` (H2: una proforma reemitida aparece una vez), `AutorizacionProformaRepositoryTest` y specs de `ProformasComponent`.
- **No incluido**: facturar en partes con control de cantidades por línea, y comparar el detalle de la factura con el de la proforma (hoy ningún rol lo valida).

### Agregado — Auditoría y autorizaciones de proformas (Fase 1, paso 5)

Las proformas mayoristas dejan historial de quién hizo qué, y un descuento sobre el tope o una proforma vencida no se factura sin la decisión de un administrador.

- **Tope de descuento**: una proforma mayorista con una línea sobre 15 % (`dtodo.mayorista.descuento-maximo`, configurable) queda `PENDIENTE`, también un nivel Volumen de 20 %. El precio mayorista fijo no pide autorización porque lo fija el administrador. Una edición recalcula el estado: la proforma aprobada que se modifica vuelve a quedar pendiente si sigue sobre el tope. El servidor lo decide; lo que traiga la petición se ignora.
- **Bloqueo al facturar** (`POST /api/facturas/createV2`): una proforma pendiente o rechazada responde 422, igual que una vencida sin autorización. Vencer es pasar de `fecha_vencimiento`: el último día todavía se factura.
- **Bandeja de autorizaciones** (`/api/autorizaciones`, solo administrador): descuentos por autorizar (aprobar o rechazar) y proformas mayoristas vencidas, sin facturar y sin autorizar (autorizar facturar). Toda decisión exige motivo y queda en el historial. Pantalla nueva *Autorizaciones*, en el menú del administrador.
- **Auditoría** (`proformas_auditoria`, `GET /api/proformas/{id}/auditoria`): creación, modificación (con el resumen antes y después), anulación, aprobación, rechazo, vencimiento autorizado y cambio de cliente al facturar. El usuario sale del token. El historial se conserva aunque la proforma se anule (sin llave foránea). Se ve en el detalle de la proforma; el vendedor solo el de las suyas.
- **Motivo obligatorio**: editar o anular una proforma mayorista pide motivo (422 si falta, `?motivo=` en el PUT y el DELETE), y una mayorista ya facturada no se modifica ni se anula. Una minorista sigue como siempre, con motivo opcional.
- **Cambio de cliente al facturar**: si la factura sale a un cliente distinto del de la proforma, queda en el historial. La factura ya está certificada, así que un fallo al auditar se registra en el log sin ocultarla.
- **Base de datos** (`sql/mayoristas/07_create_auditoria_autorizaciones.sql`, **requerido antes del jar**; `99_rollback_mayoristas.sql` lo revierte): `estado_autorizacion` y `vencimiento_autorizado` en `proformas`, y la tabla `proformas_auditoria`.
- **Frontend**: aviso en la proforma cuando una línea pasa del tope, motivo al guardar cambios de una mayorista, aviso al crear una pendiente y distintivo "Por autorizar" / "Rechazada" en el listado.
- **Pruebas**: `AutorizacionProformaServiceImplTest`, `ProformaAuditoriaServiceImplTest`, `AutorizacionProformaRepositoryTest` (H2), `ProformaApiControllerAuditoriaTest`, `FacturaApiControllerAutorizacionTest`, casos nuevos en `ProformaMayoristaServiceImplTest`, `ProformaServiceImplTest` y `VendedorSeguridadTest`, y specs de la bandeja, el servicio y `CreateProformaComponent`.
- **No incluido**: una proforma facturada con varias facturas, la reemisión tras anular y la pantalla "Cambiar cliente" al facturar (paso 6); hoy una proforma con factura, aunque esté anulada, no se vuelve a facturar. Tampoco se bloquea la entrega de una proforma pendiente porque la entrega es de la Fase 2.

### Agregado — Proforma mayorista (Fase 1, paso 4)

Una proforma a un cliente mayorista con ruta es ahora una proforma mayorista: lleva observaciones, fecha de entrega estimada, vigencia de 15 días y dos copias impresas.

- **Tipo**: `tipo_proforma` (MINORISTA por defecto, MAYORISTA) y `id_ruta`. El servidor (`ProformaMayoristaServiceImpl.preparar`) exige un cliente mayorista con ruta, toma la ruta del cliente y, para el vendedor, siempre fija el tipo en mayorista. Los errores van como 422.
- **Observaciones**: una general del pedido (hasta 500 caracteres) y una por línea (hasta 255). Se limpian y, si exceden el largo, se rechazan en lugar de cortarse.
- **Vigencia y entrega**: `fecha_vencimiento` = emisión + 15 días, fijada al crear y que editar no extiende; `fecha_entrega_estimada` la escribe el usuario. Aún no se bloquea la facturación de una proforma vencida (paso 5).
- **Impresión** (`proforma_mayorista.jrxml`, `GET /api/proformas/generate/{id}?copia=CLIENTE|TIENDA`): encabezado con negocio, encargado, teléfono, ruta, crédito y vendedor; la observación de cada línea sale bajo el producto. La copia del cliente no imprime la observación general; la de la tienda sí. Una proforma minorista sigue usando `proforma.jrxml`. Las etiquetas del encabezado miden 12 pt de alto: con 10 se recortaban a vacío con la tipografía del servidor de pruebas.
- **Base de datos** (`sql/mayoristas/06_create_proforma_mayorista.sql`, **requerido antes del jar**; `99_rollback_mayoristas.sql` lo revierte): cinco columnas en `proformas`, su llave a `rutas`, un índice por tipo y `observacion` en `proformas_detalle`.
- **Frontend**: con un cliente mayorista con ruta aparecen la ruta, la fecha de entrega, las observaciones del pedido y un campo de observación bajo cada producto; al guardar se abren las dos copias. En el listado, las mayoristas se marcan con su ruta y al imprimir se elige la copia.
- **Pruebas**: `ProformaMayoristaPdfTemplateTest` (plantilla con datos de ejemplo: cada copia, etiquetas, observaciones, 60 líneas), `ProformaMayoristaServiceImplTest`, casos nuevos en `ProformaServiceImplTest` y specs de `CreateProformaComponent` y `ProformasComponent`.
- **No incluido**: el tope de 15 % con autorización y el bloqueo por proforma vencida (paso 5).

### Agregado — Descuentos y precio mayorista (Fase 1, paso 3)

El usuario elige, línea por línea, de dónde sale el descuento de un cliente mayorista; el sistema calcula las opciones, sugiere una y verifica que lo elegido sea coherente.

- **Tipos de descuento** (`TipoDescuentoEnum`): categoría del cliente, ciento, fardo cerrado, precio mayorista fijo y manual. Cada línea de proforma guarda el tipo, el precio unitario de ese momento (el precio histórico no se guardaba) y el porcentaje que sugería el sistema.
- **Categorías**: A (10 %), B (8 %) y Volumen (12, 15 y 20 %) son niveles configurables por el administrador; en Volumen el vendedor elige el nivel y ninguno se aplica solo.
- **Precio mayorista fijo**: opcional por producto, lo asignan los usuarios solo a los productos que quieran. Se guarda como el porcentaje equivalente sobre el precio de venta (4 decimales), de modo que factura, reportes e impresión no cambian.
- **Descuentos especiales por producto**: por ciento y por fardo cerrado, con su cantidad mínima y porcentaje. Si la cantidad no llega al mínimo se avisa, pero no se bloquea.
- **Sugerencia**: el precio mayorista fijo si el producto lo tiene; si no, el mayor porcentaje entre las opciones cuya cantidad mínima se alcanza (en un empate, fardo, luego ciento, luego categoría).
- **Validación en el servidor**: `DescuentoServiceImpl.prepararLineas` rechaza (422) una línea cuyo tipo no coincide con su porcentaje (un nivel que no es de la categoría del cliente, un descuento que el producto no tiene, un descuento mayorista para un cliente minorista...). Una línea sin tipo se toma como manual.
- **API**: `GET /api/descuentos/opciones` (administrador, cobrador, inventario y vendedor, este último solo para clientes de sus rutas); la configuración (`/categorias`, `/productos`) es solo del administrador.
- **Base de datos** (`sql/mayoristas/05_create_descuentos.sql`, **requerido antes del jar**; `99_rollback_mayoristas.sql` lo revierte): `categorias_descuento_niveles` (con los porcentajes iniciales), `precios_mayorista`, `descuentos_producto` y tres columnas en `proformas_detalle`. El precio mayorista va en una tabla aparte porque `sp_consultar_productos` se mapea directo a la entidad `Producto` y una columna nueva rompería el listado de productos.
- **Impresión**: `proforma.jrxml` imprime `precio_unitario` cuando la línea no tiene descuento y existe, en lugar del precio de venta actual del producto.
- **Frontend**: en la proforma de un cliente mayorista aparecen el tipo de descuento, el nivel (Volumen) y los avisos; el porcentaje solo se escribe a mano en el descuento manual; corregir el porcentaje de una línea la vuelve manual y cambiar de cliente pasa a manual las líneas ya tipadas. Nueva pantalla *Descuentos* (administrador) para los niveles, el precio mayorista y los descuentos por producto.
- **Pruebas**: `DescuentoServiceImplTest` (32 casos), `DescuentoRepositoryTest` (contra H2), la matriz de roles y specs de `CreateProformaComponent` y `DescuentosComponent`.
- **No incluido**: exigir autorización a partir de 15 % (paso 5; hoy el tipo y el porcentaje quedan registrados, pero no hay bloqueo) y el precio mayorista en el listado de productos.

### Agregado — Rol de vendedor y alcance por ruta (Fase 1, paso 2)

El vendedor (`ROLE_VENDEDOR`, creado en `sql/mayoristas/03`) trabaja solo con los clientes de sus rutas y con sus propias proformas. Las reglas viven en el servidor (`IAmbitoVendedorService`): el usuario sale del token, no de la petición. Un usuario que combina `ROLE_VENDEDOR` con administrador, cobrador o inventario conserva el alcance del otro rol.

- **Clientes**: el vendedor solo ve los de sus rutas (listado, búsqueda por NIT, código e id; lo ajeno responde 404). Registra clientes en una de sus rutas: quedan como mayoristas, sin categoría ni crédito (los fija el administrador) y sin repetir un NIT ya registrado. De un cliente existente solo cambia dirección, contacto, teléfono de contacto y referencia de ubicación. `/clientes/page` y `/clientes/nombre` (sin alcance por ruta, ya sin uso en el frontend) se le cierran.
- **Proformas**: el listado, el conteo, el detalle, la impresión y la edición se limitan a las suyas. Al crearlas o editarlas el autor es siempre el vendedor de la sesión y el cliente debe ser de sus rutas. No puede modificar una proforma ya facturada.
- **Facturas**: puede emitir (`createV2`), pero solo desde una proforma suya, a un cliente de sus rutas y con su propio correlativo. No anula facturas ni ve el listado general.
- **Consulta**: productos (precio de venta y existencias; el costo sigue siendo solo del administrador), su propio usuario y su correlativo; `GET /api/rutas/mias` devuelve sus rutas activas.
- **Frontend**: menú de Productos, Proformas y Clientes para el vendedor; sus pantallas piden solo sus rutas (el catálogo completo respondería 403 y lo sacaría de la pantalla); en el formulario de cliente la sección mayorista limita lo editable y oculta categoría y crédito. `AuthService.esVendedor()`.
- **Pruebas**: `AmbitoVendedorServiceImplTest`, `UtilsVendedorTest`, `VendedorSeguridadTest` (qué endpoints ve y cuáles no el vendedor) y specs de `CreateClienteComponent` y `AuthService`.
- **No incluido**: la auditoría de cambios y la autorización de descuentos (pasos 5 y 6) y que el vendedor se limite a las líneas y precios de su proforma al facturar; hoy el servidor, como para los demás roles, no compara el detalle de la factura con el de la proforma.

### Agregado — Búsqueda de cliente por código y depuración de NIT inválidos (cierre del paso 1)

- **Elegir del directorio**: en proforma y factura, el cliente elegido en el directorio se carga completo por su `id`. Antes se volvía a buscar por NIT, lo que fallaba con un cliente sin NIT (ahora válido).
- **Código de cliente**: al buscar en proforma o factura, si el valor no es un NIT registrado se prueba como código; el aviso de "registrar cliente" solo aparece si no existe ninguno de los dos.
- **NIT inválido**: filtro *Solo con NIT inválido* (solo administrador) con la cantidad, en Clientes; `GET /api/clientes/cantidad-nit-invalidos` y `nitInvalido=true` en el listado. El NIT se valida en memoria con el dígito verificador. `sql/mayoristas/diagnostico_nit_telefono.sql` (solo lectura) dimensiona el problema por ambiente. No se corrige ningún dato automáticamente.
- **Pruebas**: filtros del listado sobre H2 (`RutaClienteRepositoryTest`) y specs de búsqueda en `CreateFacturaComponent` y `CreateProformaComponent`.

### Agregado — Rutas y clientes mayoristas (Fase 1, paso 1)

Primer paso de `PLAN_MAYORISTAS.md`: catálogo de rutas y datos del cliente mayorista. Todavía no incluye el rol de vendedor, descuentos ni la proforma mayorista.

- **Base de datos** (`sql/mayoristas/`, aditivo; `99_rollback_mayoristas.sql` lo revierte): `01` tabla `rutas`, `02` columnas del cliente (`codigo_cliente`, `nombre_negocio`, `tipo_cliente`, `categoria_descuento`, `id_ruta`, `contacto`, `telefono_contacto`, `referencia_ubicacion`, `limite_credito`, `dias_credito`), `03` rol `ROLE_VENDEDOR` (el `id_role` se calcula: la columna no es autoincremental) y `04` las rutas iniciales (RUTA 1, RUTA 2, RUTA 3 y JALAPA, sin vendedor). **Aplicar `01` a `03` antes de desplegar el jar**: `VerificadorEsquema` impide arrancar si faltan las tablas o columnas.
- **Rutas**: CRUD solo para `ROLE_ADMIN` (`/api/rutas/listado`, `/{id}`, `/{id}/clientes`, `POST`, `PUT`) y `GET /api/rutas/activas` para los selectores. Una ruta puede quedar sin vendedor; si lo tiene, debe estar habilitado y tener `ROLE_VENDEDOR`. El nombre no se repite dentro de una sucursal. Se inactiva en lugar de borrarse.
- **Cliente**: tipo `MINORISTA` (por defecto) o `MAYORISTA`, categoría `A`/`B`/`VOLUMEN`, ruta, código único (por defecto el teléfono), nombre del negocio, contacto, referencia de ubicación, límite y días de crédito. Un mayorista exige una ruta activa; un minorista pierde ruta y categoría. `GET /api/clientes/codigo/{codigo}`; el listado filtra por `tipoCliente` e `idRuta` y busca también por código, negocio y contacto.
- **Corregido de paso**: `ClienteServiceImpl.save` guardaba una copia parcial y luego el objeto completo del request. Ahora, al actualizar, carga el cliente y copia solo los campos editables (la fecha de registro nunca la manda el cliente); un id inexistente responde 404 en lugar de crear un cliente nuevo.
- **Permisos**: solo `ROLE_ADMIN` registra o convierte clientes en mayoristas; el cobrador sigue creando minoristas (los datos mayoristas del request se descartan).
- **NIT opcional**: el formulario de cliente ya no lo exige; se valida al facturar (ver la entrada anterior).
- **Frontend**: módulo *Rutas* (listado con búsqueda y orden, formulario con vendedor y sucursal, entrada en el menú para administradores); en *Clientes*, filtros por tipo y ruta, columna *Tipo / Ruta* y, en el formulario, una sección *Cliente mayorista* visible solo para el administrador. `?ruta=ID` en el listado de clientes muestra la cartera de una ruta.
- **Pruebas**: `RutaServiceImplTest`, `ClienteMayoristaServiceTest`, `ClienteApiControllerMayoristaTest`, `RutaClienteRepositoryTest` (consultas contra H2) y specs de `CreateRutaComponent` y `CreateClienteComponent`.

### Agregado — Estado de facturación de las proformas y validaciones antes de facturar

Primer paso del módulo de clientes mayoristas (Fase 0.5 de `PLAN_MAYORISTAS.md`); no depende de rutas ni de mayoristas.

- **Listado y detalle de proformas**: nueva columna *Facturación* con **Sin facturar**, **Facturada** o **Factura anulada**, la serie y el número de la factura y quién la emitió. El botón de facturar se deshabilita cuando la proforma ya tiene factura. `ProformaDto` suma `idFactura`, `serieFactura`, `noFactura`, `estadoFactura` y `facturadoPor`, calculados con una unión por entidad sobre `facturas.id_proforma_origen`.
- **Proforma ya facturada**: `facturaFel` la rechaza **antes** de grabar con un mensaje que indica la factura y quién la emitió (distingue si está anulada). Antes el usuario recibía un error genérico de base de datos por el índice único `uq_facturas_proforma_origen`, que se mantiene.
- **NIT inválido**: nueva `NitValidator` (dígito verificador módulo 11; acepta `CF` y `C/F`). Un NIT que en realidad es un teléfono, o un cliente sin NIT, se rechaza antes de tocar la base de datos y de ir a la certificadora, en lugar de revertir la venta después.
- **Respuesta 422**: nueva `ReglaNegocioException` → `422 Unprocessable Entity` (401 y 403 están reservados por el interceptor del frontend). La pantalla de factura muestra el mensaje en línea y **no recarga**; `FacturaService.createV2` no abre su propio Swal ante un 422.
- **Cambiar cliente al facturar**: botón visible *Cambiar cliente* junto al cliente seleccionado, que abre el directorio, y un aviso cuando la factura va a un cliente distinto del que registra la proforma. La proforma conserva su cliente.
- **Pruebas**: `NitValidatorTest`, 5 casos nuevos en `FacturaServiceImplTest`, `ProformaListadoFacturacionTest` (ejecuta las tres consultas del listado contra H2 con una proforma facturada y otra sin facturar) y specs de `CreateFacturaComponent` y `ProformasComponent`.
- **No incluido**: volver a facturar una proforma (reemisión tras anular) y facturar por partes; llegan con la Fase 1 al reemplazar el índice único por el control de cantidades.

### Cambiado — Rediseño del comprobante de despacho (guía de despacho)

`despacho_bodega.jrxml` se rediseñó para que se lea como una guía de envío y se distinga a simple vista del comprobante de devolución: azul marino con franja dorada y chevrones «»», una **etiqueta de envío** con el total de unidades y el destino en grande, el avance en **tres pasos** (registrado, resolución, ingreso al destino) con color según el estado, una ficha de datos con celdas de formulario, y en la tabla las columnas **«Quedan»** (existencia de la bodega tras el despacho; en rojo si queda en cero) y **«Conforme»** (casilla para que quien recibe marque cada línea) con filas alternadas y pie de totales. Se agregó un bloque de **recepción en el destino** (conforme o con diferencias, fecha y hora, observaciones) y las firmas sin recuadros. La consulta suma `es_traslado` para rotular «Sucursal destino» o «Bodega destino · traslado». Los costos siguen visibles solo para el administrador. Pruebas nuevas en `BodegaTemplatesTest`; manual (8.6) actualizado.

### Agregado — Devoluciones de tienda a bodega

El módulo de Bodegas admite devolver producto de una sucursal (tienda) a una bodega. Es el flujo inverso del despacho y **requiere la aprobación de un administrador**. Disponible para `ROLE_ADMIN` y `ROLE_BODEGA` en **Bodegas › Devoluciones**.

- **Flujo**: al registrar, el stock **sale de la sucursal** (queda reservado; la devolución nace `PENDIENTE`); al aprobar (solo `ROLE_ADMIN`) **ingresa a la bodega** (`REALIZADO`); al rechazar o cancelar estando pendiente (administrador o bodega, con motivo) **regresa a la sucursal** (`CANCELADO`). Una devolución resuelta es final: no se revierte.
- **Base de datos**: `sql/bodegas/08_create_devoluciones_bodega.sql` (aditivo y repetible; `97_rollback_devoluciones_bodega.sql` lo revierte; `99_rollback_bodegas.sql` ahora también borra estas tablas). Crea `devoluciones_bodega` y `devoluciones_bodega_detalle`, que guarda el costo unitario y la existencia de la sucursal al registrar. **Aplicar antes de desplegar el jar**: `VerificadorEsquema` impide arrancar si faltan.
- **API** (`/api/devoluciones-bodega`): listado paginado con filtros, detalle, registro, `PUT /{id}/aprobar` (solo administrador), `PUT /{id}/cancelar` y `GET /sucursales/{id}/productos` (existencias de la tienda sin costos, para que Bodega elija qué devolver sin ampliar el permiso de `inventario-sucursal`). El costo (`total`, `precioUnitario`, `subTotal`) solo sale para el administrador.
- **Movimientos**: nuevo tipo `INGRESO_DEVOLUCION` en la bitácora de bodega (el reporte de movimientos lo cuenta como entrada). En la sucursal se usan `SALIDA`/`ENTRADA` con `tipo_documento_origen = 'DEVOLUCION_BODEGA'`.
- **Frontend**: listado, formulario, detalle y buscador de productos de la tienda, con el mismo esqueleto y la misma paleta ocre del despacho (identidad del módulo de Bodegas) y un recorrido *tienda → bodega* en el formulario y el detalle que distingue el flujo inverso. Menú **Devoluciones** (con la etiqueta «Nuevo», que sale de Tutorial), manual actualizado (portada, roles, acceso, sección 8.10, ciclo de existencias, reportes, preguntas frecuentes y resumen).
- **Corregido de paso**: la cabecera de *Registrar nueva bodega* salía gris azulada (heredaba la paleta de Sucursales por la clase `branch-form-page`) en lugar de ocre como su tarjeta del inicio.
- **Pruebas**: `DevolucionBodegaServiceImplTest` (14 casos), `BodegaControllersSecurityTest` ampliada y specs de servicio, listado, formulario y detalle.
- **Comprobante PDF** (`GET /api/devoluciones-bodega/{id}/comprobante`, plantilla `devolucion_bodega.jrxml`): mismo lenguaje que el comprobante de despacho (banner azul con logo, franja de estado, tabla, firmas) con elementos propios: recorrido *tienda → bodega* con ficha de unidades, línea de tiempo de la devolución, barra de «% de la existencia» por línea y fichas de totales; versión compacta (cabecera de 44 pt, datos en una sola fila, sin recuadros punteados: las firmas son una línea con su rótulo). Cambia de color y de trazado según el estado (pendiente, aprobada, rechazada). El costo, los subtotales y el total solo salen con `VER_COSTOS` (administrador). Botón de imprimir en el listado y el detalle (con aviso de carga y botón bloqueado mientras se genera el PDF) y oferta de imprimir al registrar. Al elegir un producto en el formulario de devolución el campo de cantidad recibe el foco. Pruebas en `BodegaTemplatesTest` (compilación, los tres estados con y sin costos, textos y visibilidad del costo).
- **No incluido**: reversión de una devolución ya aprobada.

### Corregido — El costo (precio de compra) solo lo ve el administrador

Una auditoría con un usuario de cada rol mostró que la interfaz ya ocultaba el costo en Productos, pero la API lo entregaba igual: `precioCompra` y `porcentajeGanancia` salían en `/api/productos`, `/productos-dto`, `/productos-activos`, `/productos/{id}` y `/productos/codigo/{codigo}` para `ROLE_COBRADOR`, `ROLE_INVENTARIO` e incluso `ROLE_BODEGA`; el módulo de Bodegas mostraba costos, totales al costo y subtotales a `ROLE_BODEGA`; y los reportes **Productos bajo stock mínimo** y **Existencias actuales** (`ROLE_INVENTARIO`) y los de bodega (`ROLE_BODEGA`) incluían columnas de costo. El margen de ganancia también cuenta porque permite calcular el costo a partir del precio de venta.

- **API**: nuevo `SoloAdministradorSerializer` (Jackson) aplicado a `Producto`, `ProductoDto` y `ProductoDtoMejorado` (`precioCompra`, `porcentajeGanancia`), `InventarioBodegaDto.precioCompra`, `DespachoBodega.total`, `DespachoBodegaDetalle.precioUnitario/subTotal` y `DespachoBodegaDto.total`. Para cualquier rol distinto de administrador (o sin sesión) esos campos salen `null`; el resto de la respuesta no cambia. Al ser a nivel de serialización cubre también el producto anidado en proformas, facturas y despachos. Las entidades no se alteran (las relaciones de producto no tienen cascada), así que ocultar el valor no puede sobrescribirlo en la base.
- **Reportes**: parámetro `VER_COSTOS` (por defecto `false`) en `bodega_existencias`, `bodega_despachos`, `despacho_bodega` (comprobante), `bajo_stock` y `productos_excel`; los servicios lo llenan con `Utils.esAdministrador()`. Sin él desaparecen las columnas y totales de costo, y en el Excel de existencias no quedan columnas vacías.
- **Frontend**: en Bodegas y Despachos el costo unitario, los subtotales, el total al costo y la columna Total del listado solo se muestran con `ROLE_ADMIN`; las descripciones de los reportes de bodega lo aclaran.
- **Pruebas**: `SoloAdministradorSerializerTest`, `UtilsEsAdministradorTest` y casos de visibilidad en las pruebas de plantillas (bodega, bajo stock y existencias). Se volvió a correr la auditoría con un usuario de cada rol: solo el administrador recibe costos.
- **Pendiente de decidir**: el formulario de producto (`/productos/create`) sigue accesible por URL a `ROLE_INVENTARIO`; la API ya le entrega el costo vacío y no puede guardar (`POST/PUT /productos` son de administrador).

### Agregado — Módulo de Bodegas y Despachos de bodega

Mantenimiento de bodegas con inventario propio, movimientos manuales, importación de inventario, despachos hacia sucursales con comprobante imprimible y reportes. El módulo está disponible para `ROLE_ADMIN` y para el nuevo rol `ROLE_BODEGA`.

**Base de datos** (`Backend/dtodo-backend/sql/bodegas/`, aplicar en orden; `99_rollback_bodegas.sql` revierte todo y es destructivo)
- `bodegas`: nombre (único), ubicación, descripción, encargado, teléfono, fecha de registro, sucursal asignada (opcional), estado (`estados` compartida: solo ACTIVO/INACTIVO) y usuario que la registró.
- `inventario_bodega` (producto × bodega, `UNIQUE`, `CHECK (stock >= 0)`) y `movimientos_bodega` (bitácora con tipo, cantidad, existencia inicial y final, motivo y documento origen). El tipo es `VARCHAR`, no `ENUM`, para poder sumar tipos sin alterar la tabla.
- `despachos_bodega` y `despachos_bodega_detalle`. El detalle guarda el costo unitario y la existencia de la bodega al momento del despacho para que el comprobante sea reproducible. `despachos_bodega_detalle.id_despacho` es `NULL` por el mismo motivo que `compras_detalle.id_compra` (`@OneToMany` unidireccional con `@JoinColumn`).
- `07_alter_despachos_bodega_traslados.sql` (aditivo y repetible; `98_rollback_traslados_bodega.sql` lo revierte): `despachos_bodega.id_sucursal_destino` pasa a `NULL`, se agrega `id_bodega_destino` (FK a `bodegas`) y un `CHECK` que exige **exactamente un destino** (sucursal o bodega). Los despachos existentes no cambian.
- `06_insert_role_bodega.sql`: crea `ROLE_BODEGA` de forma idempotente. `roles.id_role` no es `AUTO_INCREMENT` en este esquema, por eso el id se calcula en el script.
- No se necesitaron triggers, funciones ni procedimientos: la lógica transaccional vive en los servicios, igual que en Compras y Sucursales.
- **Importante al desplegar:** `VerificadorEsquema` valida al arrancar que existan las tablas y columnas de las entidades; el backend no inicia en un ambiente al que no se le hayan aplicado estos scripts.

**Backend**
- Entidades `Bodega`, `InventarioBodega`, `MovimientoBodega`, `DespachoBodega`, `DespachoBodegaDetalle`; enums `TipoMovimientoBodegaEnum`, `EstadoDespachoBodegaEnum`, `OrigenInventarioBodegaEnum`; repositorios, DTOs y servicios `BodegaServiceImpl`, `InventarioBodegaServiceImpl`, `DespachoBodegaServiceImpl`.
- Movimientos de bodega: **agregar producto** (`INGRESO`), **reducir existencias** (`REDUCCION`, con motivo obligatorio), **eliminar producto** (`ELIMINACION`: descuenta toda la existencia y retira la fila del inventario) y **despachar**. Todo cambio de stock pasa por un único método que bloquea la fila (`PESSIMISTIC_WRITE`), impide stock negativo y registra el movimiento en la misma transacción.
- La bodega es independiente del inventario de sucursal: no toca `inventario_sucursal` ni `movimientos_producto` salvo al **aprobar** un despacho.
- Ciclo de vida del despacho: nace `PENDIENTE` (pendiente de aprobación) y **el stock sale de la bodega al registrarlo** (queda reservado, así no se puede despachar dos veces la misma existencia). Al **aprobarlo** (solo `ROLE_ADMIN`) ingresa a la sucursal destino como un movimiento `ENTRADA` con origen `DESPACHO_BODEGA` y pasa a `REALIZADO`. Al **cancelarlo** (motivo obligatorio) el stock regresa a la bodega y pasa a `CANCELADO`. Un administrador también puede **revertir un despacho ya aprobado** (motivo obligatorio): las unidades salen de la sucursal destino (movimiento `SALIDA` con origen `DESPACHO_BODEGA`) y regresan a la bodega; si la sucursal ya no cuenta con todas las unidades la reversión se rechaza completa, sin tocar nada. `ROLE_BODEGA` solo puede cancelar pendientes. `CANCELADO` es estado final.
- **Traslado entre bodegas**: un despacho puede tener como destino otra bodega (`idBodegaDestino` en `DespachoBodegaRequest`, nunca junto con la sucursal ni igual a la de origen). Reutiliza el flujo completo (reserva al registrar, aprobación de administrador, cancelación, reversión, comprobante y reportes). Al aprobar suma a la bodega destino con el movimiento `INGRESO_DESPACHO`; al revertir un traslado aprobado retira de la bodega destino con `REVERSION_DESPACHO` y se rechaza completo si ya no tiene las unidades. No toca el inventario de ninguna sucursal.
- **Reducir existencias y eliminar productos son exclusivos de `ROLE_ADMIN`** (antes también `ROLE_BODEGA`), para que una diferencia de inventario no se ajuste sin que el administrador lo sepa. `ROLE_BODEGA` conserva agregar, importar, despachar y trasladar.
- La sucursal destino es la asignada a la bodega, pero puede elegirse otra; una bodega o sucursal inactiva no admite despachos. El precio de cada línea es el costo (`precio_compra`) del producto y lo asigna el servidor, igual que la existencia, el estado y los usuarios: el cliente solo envía ids y cantidades (`DespachoBodegaRequest`).
- El usuario responsable de cada operación sale del claim `id_usuario` del JWT (`Utils.obtenerIdUsuario`), no de un identificador enviado por el cliente.
- Inventario inicial: **copiar** desde una sucursal u otra bodega (solo a una bodega vacía; se copian **todos** los productos, también los de stock cero, con su stock mínimo; hay un movimiento `IMPORTACION` por cada producto con existencias) o **importar un Excel** (`.xlsx`). La cantidad puede ser 0 para registrar productos sin existencias (así el cliente ve cuáles quedaron fuera). La importación es atómica: si una fila tiene error no se aplica ninguna y se devuelve la lista de errores por número de fila. Si el producto ya está en la bodega la cantidad se **suma**. Un código que corresponde a varios productos del catálogo (existen duplicados históricos) se reporta como error en vez de adivinar. Nueva dependencia: `org.apache.poi:poi-ooxml` 5.4.1.
- **Importar Excel con productos nuevos**: el archivo admite las columnas opcionales `nombre`, `precio_compra`, `precio_venta`, `marca` y `categoria`. Si el código **no existe** en el catálogo y quien importa es `ROLE_ADMIN`, se crea el producto (nombre, precios, % de ganancia calculado, marca y categoría) con `ProductoService.save` y una fila de `inventario_sucursal` en la sucursal de la bodega (o la del usuario), igual que al crearlo desde Productos, para que no quede un producto sin inventario. Si el código **ya existe**, esas columnas se ignoran y el producto no se modifica. `marca` y `categoria` se buscan por nombre sin distinguir mayúsculas ni tildes y deben existir; vacías usan `SIN MARCA` y `SIN CATEGORIA` (esta última se crea si falta). Un usuario `ROLE_BODEGA` con códigos inexistentes recibe un error por fila ("solo un administrador puede crear productos nuevos al importar"). La importación sigue siendo atómica: ningún producto, marca ni categoría se crea hasta que todo el archivo es válido. La respuesta incluye `productosCreados`. La plantilla trae 8 columnas, dos filas de producto nuevo y una hoja de instrucciones actualizada.
- Endpoints: `/api/bodegas` (listado, detalle, alta, edición, inventario, búsqueda por código, movimientos, copiar inventario, importar Excel y plantilla) y `/api/despachos-bodega` (listado, detalle, registrar, aprobar, cancelar, comprobante PDF). Todos `@Secured` con `ROLE_ADMIN`/`ROLE_BODEGA`, salvo aprobar, reducir existencias y eliminar producto (`ROLE_ADMIN`).
- `ROLE_BODEGA` puede listar sucursales (`GET /api/sucursales`) para elegir el destino de un despacho.
- Reportes (módulo de Reportes, categoría Bodegas, PDF y XLSX): **Existencias de bodega** (valorizadas al costo, resalta las que están en o bajo el stock mínimo), **Movimientos de bodega** y **Despachos de bodega** (por período, bodega y estado; el total excluye cancelados). Comprobante de despacho en PDF (`despacho_bodega.jrxml`) con estado, existencias, totales y firmas. El kardex de producto de la sucursal identifica los ingresos por despacho de bodega.
- Pruebas unitarias nuevas: `BodegaServiceImplTest`, `InventarioBodegaServiceImplTest`, `DespachoBodegaServiceImplTest`, `InventarioBodegaExcelTest`, `BodegaReporteServiceImplTest`, `BodegaTemplatesTest` (compila y llena cada plantilla, también sin datos), `BodegaJacksonTest` (ciclos entre `Bodega`/`Sucursal`/`Usuario` contra el `ObjectMapper` real), `BodegaControllersSecurityTest` (todo endpoint declara `@Secured`; aprobar, reducir y eliminar son solo de admin) y `UtilsObtenerIdUsuarioTest`; se actualizaron `ReporteServiceImplTest` y `ReporteSelectorServiceImplTest` por los nuevos colaboradores.

**Frontend**
- Menú **Bodegas** (Listado y Despachos) y rutas `/bodegas/*` y `/despachos-bodega/*` protegidas con `RoleGuard`.
- Listado de bodegas con productos y unidades por bodega, formulario de alta/edición (con estado y sucursal asignada) y modal de detalle, con el mismo lenguaje visual que Sucursales/Compras (acento ocre propio de Bodegas).
- Inventario de bodega con pestañas **Existencias** (alerta de stock mínimo) y **Movimientos** (filtros por tipo, rango de fechas y texto), y modales para agregar producto (por código o desde el catálogo, con vista previa de la existencia resultante), reducir existencias y eliminar producto.
- Selector de origen del inventario (`app-origen-inventario-bodega`), usado en el alta de la bodega y en el modal **Importar inventario**: copia desde sucursal/bodega o Excel, con una **ilustración del formato esperado** (las 8 columnas, distinguiendo obligatorias, opcionales y las que solo aplican a productos nuevos), reglas y botón para descargar una plantilla de ejemplo. El aviso al terminar indica cuántos productos nuevos se crearon (`resumenImportacion`). Los errores del archivo se listan por fila en el modal.
- Formulario de despacho con el selector **Enviar a** (*Una sucursal* / *Otra bodega (traslado)*); el listado y el detalle muestran el destino (sucursal o bodega) y los traslados se identifican como tales. En el inventario, el rol Bodega ya no ve las acciones de reducir ni eliminar.
- Despachos: listado con filtros por estado y bodega y acciones de aprobar/cancelar, formulario de registro (mismo patrón que Registrar compra: tarjeta flotante con el total, edición de cantidad con confirmación, existencia actual y existencias que quedan por línea), buscador de productos de la bodega y detalle con comprobante imprimible.
- Inicio: tarjeta de **Bodegas** (la de **Despachos pendientes** se retiró del inicio el 2026-10-16, igual que la de devoluciones). Un usuario que solo tiene `ROLE_BODEGA` ya no consulta productos ni clientes (devolvían 403 y mostraban un aviso en cada inicio de sesión) y no ve el grupo de menú Productos.
- Reportes: nueva categoría Bodegas con filtro de bodega.
- **Manual de usuario** (`Frontend/src/assets/manual/manual-bodegas.html`): página HTML independiente, imprimible y con índice, que explica bodegas, inventario, importación (con el formato de Excel), movimientos, despachos (registrar, aprobar, cancelar y revertir), reportes y preguntas frecuentes. Se abre desde el nuevo submenú **Bodegas → Tutorial** (en una pestaña nueva). El grupo Bodegas y el Tutorial llevan una etiqueta **Nuevo** (un punto si el menú está colapsado); es una señal fija en `sidebar.component.html`, que se puede retirar cuando deje de ser novedad.
- Nuevos helpers: `PaginacionTabla` (estado de paginación reutilizable) y `AuthService.esSoloBodega()`.
- Pruebas unitarias nuevas para modelos, servicios HTTP, todos los componentes nuevos y los reportes de bodegas (306 pruebas frontend en total, todas correctas).

**Decisiones a revisar**
- Quién aprueba: se asumió que solo `ROLE_ADMIN` aprueba (para eso existe el estado pendiente); quien registró el despacho puede aprobarlo si es administrador.
- Al revertir un despacho aprobado, el campo "resuelto por" pasa a ser quien lo revirtió (no se conserva quién lo aprobó originalmente).
- Un usuario `ROLE_BODEGA` ve y opera todas las bodegas (no se limita a la de su sucursal).
- El traslado entre bodegas requiere aprobación de un administrador (decisión de negocio), igual que un despacho a sucursal.
- Crear productos desde el Excel es solo de `ROLE_ADMIN` (como `POST /api/productos`), y las marcas y categorías no se crean desde el archivo para no llenar el catálogo de duplicados por errores de escritura.

### Agregado — Módulo de Compras

Registro, listado, visualización y anulación de compras a proveedores. Cada compra suma existencias al inventario de la sucursal donde ingresa la mercadería; el módulo es exclusivo de `ROLE_ADMIN`.

**Base de datos** (`Backend/dtodo-backend/sql/compras/`)
- Tablas nuevas `paises` (catálogo simple, sembrado con 10 países comunes), `proveedores` (con país y estado ACTIVO/INACTIVO vía la tabla `estados` compartida), `compras` y `compras_detalle`.
- `compras.estado` usa un enum propio (`ACTIVA`/`ANULADA`), igual patrón que `NotaCredito`, en vez de la tabla `estados` compartida — una compra anulada nunca se edita ni se borra.
- A diferencia de `facturas_detalle`/`proformas_detalle` (que nunca persistieron el precio unitario histórico, ver nota de `sub_total` en este mismo archivo), `compras_detalle.precio_unitario` se guarda explícitamente desde el primer día para no repetir ese problema.
- `compras_detalle.id_compra` se dejó `NULL` (no `NOT NULL`): Hibernate inserta la fila de detalle sin la FK y la completa con un `UPDATE` posterior porque la relación `Compra.items` es un `@OneToMany` unidireccional con `@JoinColumn` — mismo patrón ya usado por `facturas_detalle.id_factura`. Declararla `NOT NULL` (el primer intento) rompía ese `INSERT` inicial con `Field 'id_compra' doesn't have a default value`.

**Backend**
- Nuevo módulo `Compra`/`CompraDetalle`: el enum `TipoMovimientoEnum.COMPRA`/`ELIMINAR_COMPRA` ya existía (anticipado, nunca usado) y el mecanismo de suma/resta de `InventarioSucursal` en `MovimientoProductoServiceImpl.calcularStock()` no necesitó cambios — `CompraServiceImpl.crear()` solo genera un `MovimientoProducto` tipo `COMPRA` por cada línea del detalle.
- Si una línea del detalle trae un producto sin `idProducto`, `CompraServiceImpl` lo crea primero vía `IProductoService.save()` (misma validación y normalización que el alta manual de productos) antes de guardar la compra; el producto nuevo no queda registrado si la compra falla, porque todo ocurre en la misma transacción.
- Anular una compra (`PUT /compras/anular/{id}/{idusuario}`) no permite edición del detalle — solo cambia el estado a `ANULADA` y genera un movimiento `ELIMINAR_COMPRA` por línea para revertir el stock, igual que la anulación de facturas revierte con `ANULACION_FACTURA`.
- Nuevos módulos simples `Pais` y `Proveedor` (CRUD estándar, `ROLE_ADMIN`); `Proveedor` nunca se elimina físicamente, solo se desactiva por estado, igual que `Sucursal`/`Producto`.
- Pruebas unitarias nuevas: `CompraServiceImplTest`, `ProveedorServiceImplTest`, `PaisServiceImplTest`.

**Frontend**
- Nuevos módulos `/compras` y `/proveedores`, con entrada de menú "Compras" (submenú Listado/Registrar compra/Proveedores, `ROLE_ADMIN`) siguiendo el mismo patrón de grupo desplegable que Productos/Facturas.
- El formulario de registro de compra reutiliza `app-modal-buscar-producto` (el mismo modal de Facturas/Proformas) para elegir un producto ya existente, y agrega `app-modal-crear-producto`: un modal nuevo con los mismos campos del alta normal de producto (menos la sección de sucursales, que no aplica aquí) que **no hace ninguna llamada HTTP** — solo arma el objeto en memoria; el producto se registra únicamente si la compra completa se guarda con éxito.
- Al elegir un producto desde el catálogo (modal de búsqueda), se vuelve a consultar por código con `ProductoService.getProductoByCode()` antes de agregarlo al detalle — el modal de búsqueda solo emite un DTO liviano (`ProductoDto`), no la entidad completa, mismo patrón ya usado por `create-factura`. Enviar el DTO tal cual al backend rompía la deserialización de Jackson (`400 Bad Request: Failed to read request`).
- El precio unitario de cada línea se precarga desde `producto.precioCompra` pero es editable (el precio realmente pagado en esa compra puede diferir del histórico); el total se recalcula en el backend (cantidad × precio unitario de cada línea + costo de envío) en vez de confiar en lo que envía el cliente.
- Vista de detalle de compra (modal de solo lectura, mismo componente/estilo que el detalle de Sucursales) y confirmación explícita antes de anular, con aviso de que el stock se revertirá.
- Pruebas unitarias nuevas para `CreateCompraComponent` (cálculo de totales, validaciones de `agregarLinea`), `ModalCrearProductoComponent`, `ComprasComponent`, `ProveedoresComponent` y el modelo `DetalleCompra`.

### Corregido — Enter y cantidad inicial en registro de compras

- Enter en código busca el producto; Enter en cantidad o precio agrega la línea sin activar «Guardar compra».
- La cantidad ya no se precarga con 1 al seleccionar o agregar un producto; el campo vacío muestra el placeholder 0.
- Verificación: build Angular y 105 pruebas frontend correctas.

### Corregido — Registro de compra no coincidía visualmente con Facturas/Proformas

El formulario de "Registrar compra" se rediseñó para reutilizar el mismo lenguaje visual que `create-factura`/`create-proforma`, en lugar de una tabla de Bootstrap genérica:
- El botón "Guardar compra" y el total ya no viven en un pie de página fijo al final del formulario: ahora es una tarjeta flotante (`sticky-summary`) debajo del encabezado, igual que en Facturas/Proformas, visible en todo momento mientras se hace scroll por el detalle.
- La sección de detalle reutiliza `create-proforma.component.css` como hoja de estilos compartida (mismo patrón que ya usa `create-sucursal` con `create-producto.component.css`): estado vacío ilustrado, contador de productos, y la tabla de detalle (`detail-table`) con el mismo lenguaje visual de celdas, encabezados y responsividad en móvil.
- **Cantidad y precio unitario del detalle dejaron de ser editables directamente en la tabla** (se detectó en revisión que permitía cambios accidentales sin confirmación): ahora, igual que `cantidad`/`descuento` en Facturas y Proformas, se muestran como valor de solo lectura con un botón de lápiz que abre un diálogo de confirmación (`abrirEdicionDetalle`/`confirmarEdicionDetalle`/`cancelarEdicionDetalle`) antes de aplicar el cambio. Eliminar una línea también pide confirmación explícita ahora, mismo patrón que `eliminarItem` en Facturas.
- Se agregaron accesos directos de un solo clic junto al campo de código (buscar por código, ver catálogo, registrar producto nuevo) en vez de tres controles sueltos sin agrupar visualmente.

### Agregado — Perfiles de entorno en el Frontend

- `global.ts` y `AuthService` ya no tienen la URL del backend escrita a mano con bloques comentados para alternar entre Local/Producción/Desarrollo — ahora leen `environment.apiUrl` de `src/environments/`. Se agregó `environment.test.ts` (VPS de pruebas, `dtodojalapa.xyz:8383`) junto a los ya existentes `environment.ts` (local, `localhost:8383`) y `environment.prod.ts` (producción, `dtodojalapa.xyz:8382`), con su propia configuración `test` en `angular.json` (build y serve) y los scripts `pnpm run start:test`/`pnpm run build:test`/`pnpm run build:prod`.
- Se verificaron los tres builds (`pnpm run build`, `build:test`, `build:prod`) confirmando que cada uno incrusta únicamente la URL de su propio entorno.

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
- La sesión del usuario incluye ahora su sucursal activa (mostrada en el panel lateral); el alta/edición de usuarios permite asignarla. El modal de detalle de usuario también muestra la sucursal asignada y permite reasignarla ahí mismo (con un lápiz que habilita el selector, a juego con el patrón de edición en línea que ya usa Productos), sin ir al formulario completo de edición.
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
- **Búsqueda de producto por código sin escopear por sucursal**: `GET /api/productos/codigo/{codigo}` devolvía la entidad `Producto` cruda (`.stock` = columna deprecada `productos.stock`), usada por el campo "Disponible" al registrar un movimiento de producto y por las búsquedas rápidas por código en Facturas y Proformas. Detectado en Sucursal Central (stock real 40): el campo mostraba 45, el valor de otra sucursal que había quedado en la columna deprecada por el bug de "Editar producto" ya corregido. `ProductoServiceImpl.findByCodigo()` ahora sobreescribe el stock devuelto con el de `InventarioSucursal` para la sucursal activa, mismo patrón que `findAllDtoMejorado()`. Al ser un servicio compartido, el fix cubre los tres puntos de uso sin tocarlos.
- **Listado de Movimientos de Producto sin escopear por sucursal**: `GET /api/movimientos/listado` mostraba los movimientos de todas las sucursales sin distinción — confuso para un admin de sucursal que veía movimientos de productos que nunca se habían movido en la suya. Se agregó el filtro a las tres consultas involucradas (búsqueda por rango de fechas, la vista por defecto de "últimos 500", y el corte de esos últimos 500 antes de aplicarlo), mismo patrón `resolverSucursal()`/`conSucursal()` que el resto de endpoints de productos.
- **Avisos de "sin resultados" inconsistentes**: nueve listados (Correlativos, Marcas, Clientes, Proformas, Facturas, Movimientos de Producto, Usuarios, Sucursales, Tipos de Producto) usaban el mismo `alert-info` crudo de Bootstrap, distinto del resto del sistema. Se unificaron con la misma tarjeta de icono circular suave que ya usa el vacío de Productos (`.listing-empty-state`/`.listing-empty-icon`, agregadas a `styles.css` junto al resto de tokens `.listing-*` compartidos). No se tocaron `productos.component.html` ni `search-product-modal.component.html` (código muerto, huérfano desde que existe `ListadoProductosMejoradoComponent`) ni los "empty state" que ya estaban modernizados (Notas de Crédito, modales de búsqueda de producto/cliente).
- **Select y botón de "Importar productos" sin pulir**: dentro del empty-state de Productos, usaban el `<select>` por defecto de Bootstrap y un `btn-primary` crudo, desentonando con el resto de la tarjeta. El botón ahora reutiliza `listing-page-action`/`listing-primary-action` (las mismas clases de los botones "Nuevo" en todos los listados) y el select gana una flecha personalizada, sombra interna sutil y anillo azul al enfocar, mismo lenguaje visual que el selector de sucursal del detalle de usuario.
- **Proformas, Facturas y Notas de Crédito mostraban los documentos de todos los usuarios**: sin distinguir sucursal ni creador. Ahora un usuario sin `ROLE_ADMIN` solo ve los documentos que él mismo generó (`ROLE_ADMIN` sigue viendo todo). Se agregó `idUsuario` opcional a los 8 métodos de listado de los tres módulos, con el mismo patrón `where (:idUsuario is null or u.idUsuario = :idUsuario)` ya usado para Movimientos de Producto. El filtrado se resuelve en las 3 capas de servicio frontend vía un helper que consulta `AuthService.hasRole('ROLE_ADMIN')`; los componentes de listado no necesitaron cambios.
- **Select de "registros a mostrar" sin pulir en los 11 listados del sistema**: cada uno definía la misma regla local (`display: inline-block; width: 70px`) solo para anular el `<select>` por defecto de Bootstrap sin darle estilo propio. Se movió a `styles.css` como `select.page-size-select` (el selector con tipo le gana a `.form-control` de Bootstrap sin `!important`) con el mismo lenguaje visual que el resto de selects ya pulidos: flecha personalizada, sombra interna sutil y anillo azul al enfocar. Se eliminaron las 11 reglas locales duplicadas.

### Agregado — Detalle de sucursal

- Botón "Ver detalle" en el listado de Sucursales que abre un modal de solo lectura (encargado, teléfono, código de establecimiento SAT, quién la registró, fecha de registro, estado y si es la principal), siguiendo el mismo patrón visual ya usado por Usuarios y Productos.
- La carga del detalle usa un toast ("Cargando detalle...") en vez de un spinner en el botón, igual que en Proformas/Notas de crédito.
- La barra de paginación del listado de Sucursales ya no se oculta cuando solo hay una página, para mantener el mismo pie de tabla que el resto de listados del sistema.

### Corregido — Reporte de Póliza Individual generaba un PDF vacío

Al agregar el selector de sucursal al modal de Póliza (ver más abajo) se revisó el reporte de fondo y resultó estar roto desde antes, sin relación con sucursales:

- **Consultaba tablas de un esquema anterior**: `poliza.jrxml` seguía apuntando a `tbl_documento`, `tbl_detalle_documento`, `tbl_producto` y `tbl_usuario` — ninguna existe en el esquema actual (`facturas`, `facturas_detalle`, `productos`, `usuarios`). Se reescribió la consulta contra las tablas vigentes, con el mismo join y filtro (`id_estado != 4`, fecha y usuario) que ya usa `rpt_ventas_diarias.jrxml` (plantilla huérfana con el mismo propósito, nunca conectada). El total de ventas ahora suma `sub_total_descuento` (el neto realmente cobrado) en vez de `sub_total`, que no tiene una semántica consistente entre filas (ver nota histórica sobre `sub_total` en facturas_detalle).
- **Desajuste de parámetro**: el backend (`FacturaServiceImpl.resportDailySales()`) enviaba la fecha bajo la clave `fecha`, pero la plantilla declaraba el parámetro como `fechaIni` — nunca se conectaban, así que el filtro de fecha siempre comparaba contra un valor vacío y el reporte volvía sin filas. Además el backend mandaba la fecha como `String` en vez de la `Date` ya parseada (se parseaba y nunca se usaba). Se corrigieron ambos lados.
- **Logo con ruta rota**: usaba `new File("").getAbsolutePath()+"\\src\\prstd\\images\\logo-dtodo.jpg"`, una ruta de archivo relativa al directorio de trabajo del proceso que nunca existe en el servidor — mismo patrón ya corregido en `rpt_inventario.jrxml` esta sesión. Ahora usa `getClass().getResourceAsStream(...)`.
- Cubierto con `PolizaTemplateTest` (compila la plantilla y la llena con `JREmptyDataSource` para reproducir el punto de falla de la imagen, mismo patrón que `RptInventarioTemplateTest`). La consulta SQL en sí no se probó contra una base de datos real en esta sesión — se recomienda generar el reporte en vivo para confirmarlo antes de darlo por cerrado.
- **Diseño**: se quitó el rectángulo que envolvía el logo y el título ("RESUMEN DE INGRESOS"), reemplazado por una línea doble celeste debajo del encabezado. Los montos ya no llevan una etiqueta "Q." en negrita aparte — ahora usan el mismo patrón `Q#,##0.00;¤-#,##0.00` (separador de miles, 2 decimales) que ya usan el resto de reportes del sistema (proforma, nota de crédito, despacho). Además, el encabezado de columnas ahora tiene una franja celeste tenue para distinguirse del cuerpo de la tabla, cada fila del detalle queda separada por una línea delgada (antes no había ninguna, y los nombres de producto de dos líneas se confundían con la fila siguiente), y el "Total Facturado" se resalta en celeste para diferenciarlo visualmente de los tres subtotales.

### Agregado — Elegir sucursal en reportes y exportaciones

- **Exportar Excel de Productos**: el botón (`ROLE_ADMIN`) abre ahora un modal chico para elegir la sucursal a exportar, en vez de un `<select>` suelto junto al botón — mismo patrón de modal ya usado por "Exportar proformas" y los reportes de Inventario/Póliza (encabezado con degradado, ícono, cierre con X/clic afuera/Escape, entrada animada). Se agregó `ExportarProductosComponent`; el backend no cambió (`GET /productos/excel` ya aceptaba `idSucursal` opcional).
- **Reporte de Inventario** (Movimientos de Producto): el modal de rango de fechas gana un selector de sucursal, visible solo para `ROLE_ADMIN`; cualquier otro rol sigue reportando su propia sucursal como antes.
- **Reporte de Póliza Individual** (Facturas): el modal gana un selector de sucursal para `ROLE_ADMIN` y `ROLE_COBRADOR`, que además filtra la lista de cajeros a los de la sucursal elegida (`GET /usuarios/cajero` ahora acepta `idSucursal` opcional). El `<select>` de cajero, que usaba la flecha nativa del navegador, quedó con la misma flecha personalizada que el resto de selects de estos modales.
- `GET /sucursales` se abrió a `ROLE_COBRADOR` (antes solo `ROLE_ADMIN`) para que el selector de sucursal de Póliza pueda listarlas; el resto de endpoints de Sucursales (alta, edición, importar inventario) siguen exclusivos de `ROLE_ADMIN`.
- **Sucursal activa visible en todos los listados**: Facturas, Proformas, Notas de Crédito, Movimientos de Producto, Clientes, Marcas, Categorías, Correlativos, Usuarios y Sucursales muestran ahora debajo del título el mismo subtítulo con ícono que ya tenía Productos (`<i class="fa-store"></i> Nombre de sucursal`). La regla se movió de la hoja de estilos de Productos a `styles.css` (`.sucursal-subtitle`) para compartirla entre los diez listados sin duplicarla.

### Fuera de alcance / seguimiento pendiente
- Los listados de facturas, proformas y notas de crédito no tienen todavía un filtro de sucursal en la UI ni en el backend (los documentos ya quedan correctamente etiquetados con su sucursal; falta exponer el filtro).
- El backend confía en los IDs que envía el frontend para resolver la sucursal en vez de validarlos contra el JWT — mismo patrón que ya existía para `idUsuario`; queda documentado como riesgo conocido, no corregido en este cambio.
- La columna `productos.stock` se conserva en la base de datos por compatibilidad, pero ya no se escribe; se recomienda planear su eliminación en una migración futura.
