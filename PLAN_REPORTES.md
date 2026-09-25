# Plan de diseño e implementación del módulo de Reportes

## 1. Objetivo y alcance

Crear un módulo centralizado de **Reportes** que permita consultar y exportar información gerencial de ventas,
inventario, proformas, notas de crédito y compras, aplicando filtros consistentes por período, sucursal y usuario.

El módulo no debe reemplazar los comprobantes operativos que ya se generan desde cada transacción, como la factura,
la proforma, la nota de crédito o el comprobante de despacho individual. Esos documentos deben continuar disponibles
desde su pantalla de origen. El módulo centralizará reportes consolidados y exportaciones.

### Situación actual identificada

El proyecto ya tiene piezas que deben reutilizarse:

- PDF de póliza individual y póliza general de ventas.
- PDF de movimientos de inventario por período y sucursal.
- Excel de productos/existencias por sucursal.
- Excel de proformas por período y usuario.
- Plantillas Jasper para documentos individuales y algunos reportes antiguos.
- Un selector de rango de fechas reutilizable en Angular.
- Selección de sucursal y controles visuales ya utilizados por las pólizas y exportaciones.

También existen aspectos a corregir durante la centralización:

- Los endpoints están distribuidos entre los controladores de facturas, productos, movimientos y proformas.
- La generación Jasper está repetida en varios servicios y compila archivos `.jrxml` en cada solicitud.
- `GET /api/productos/pdf/inventario` está declarado pero devuelve `null`.
- El reporte de inventario usa `POST`, escribe directamente en `HttpServletResponse` y entrega el nombre incorrecto
  `daily-sales.pdf`.
- Hay archivos `.jasper` y `.jrxml` coexistiendo, además de plantillas antiguas sin uso identificado.
- Algunas plantillas antiguas contienen rutas de imagen y textos comerciales codificados directamente.

## 2. Plantilla a seguir para el frontend

### 2.1 Página principal del módulo

Crear `ReportesComponent` bajo `src/app/components/reportes/` y registrar la ruta `/reportes`.

La plantilla debe combinar los siguientes patrones existentes:

1. **Estructura de página y tarjetas:** seguir el encabezado, ancho máximo, espaciado y cuadrícula responsiva de
   `home.component.html` y `home.component.css`.
2. **Filtros de fecha:** reutilizar `DateRangePickerComponent`; no crear otro calendario.
3. **Campos y acciones:** seguir la apariencia de `poliza-individual`, `poliza-general`, `exportar-productos` y
   `exportar-proformas` para selects, mensajes de ayuda, estados de carga y botones PDF/Excel.
4. **Navegación:** agregar una opción de primer nivel **Reportes** en el menú lateral, con icono `fa-chart-bar`,
   visible de acuerdo con los permisos del usuario.

### 2.2 Composición visual propuesta

La pantalla tendrá cuatro zonas:

1. **Encabezado:** etiqueta “Análisis y control”, título “Reportes” y texto descriptivo.
2. **Categorías:** Ventas, Inventario, Proformas, Notas de crédito y Compras.
3. **Catálogo de tarjetas:** una tarjeta por reporte, mostrando nombre, descripción breve, formatos disponibles y
   rol requerido.
4. **Panel de configuración:** al seleccionar una tarjeta, mostrar sus filtros en la misma página o en un panel
   lateral. Evitar un componente modal diferente por cada reporte.

Cada tarjeta deberá usar una definición declarativa, por ejemplo:

```ts
interface ReporteDefinicion {
  codigo: string;
  categoria: 'VENTAS' | 'INVENTARIO' | 'PROFORMAS' | 'NOTAS_CREDITO' | 'COMPRAS';
  titulo: string;
  descripcion: string;
  icono: string;
  formatos: Array<'PDF' | 'XLSX'>;
  filtros: Array<'FECHAS' | 'SUCURSAL' | 'USUARIO' | 'PROVEEDOR' | 'ESTADO'>;
  roles: string[];
}
```

Esto permitirá usar un solo `ReporteFiltrosComponent` y un único flujo de generación.

### 2.3 Comportamiento esperado

- Seleccionar por defecto la sucursal del usuario autenticado.
- Permitir cambiar sucursal únicamente a los roles autorizados.
- Ofrecer accesos rápidos: Hoy, últimos 7 días, últimos 30 días y mes actual.
- Validar que ambas fechas existan y que la fecha final no sea anterior a la inicial.
- Deshabilitar “Generar” mientras exista una solicitud activa y mostrar un indicador de progreso.
- Abrir los PDF en una pestaña nueva y descargar los XLSX con el nombre enviado en `Content-Disposition`.
- Mostrar estado vacío cuando no existan datos y un error entendible cuando la generación falle.
- Conservar navegación por teclado, etiquetas asociadas, foco visible y textos accesibles.
- En móvil, mostrar una sola columna de tarjetas y apilar los filtros y acciones.

## 3. Listado aprobado de reportes a implementar

Los reportes de las dos entregas siguientes forman parte del alcance comprometido del módulo. La separación por
entregas define el orden de construcción y permite validar la base técnica antes de incorporar los análisis más
complejos; no representa una lista opcional.

### 3.1 Entrega 1: centralización y cobertura operativa

| Categoría | Reporte | Filtros | Formato | Rol recomendado | Estado |
|---|---|---|---|---|---|
| Ventas | Póliza individual | Fechas, sucursal, usuario | PDF | ADMIN | Reutilizar y mover al módulo |
| Ventas | Póliza general | Fechas, sucursal | PDF | ADMIN | Reutilizar y mover al módulo |
| Inventario | Movimientos y saldo de inventario | Fechas, sucursal | PDF/XLSX | ADMIN, INVENTARIO | Reutilizar PDF; agregar XLSX |
| Inventario | Existencias actuales de productos | Sucursal | XLSX | ADMIN, INVENTARIO | Reutilizar exportación |
| Proformas | Proformas emitidas | Fechas, usuario, sucursal | XLSX | ADMIN | Reutilizar y agregar sucursal |
| Notas de crédito | Resumen de notas de crédito | Fechas, sucursal, estado | PDF/XLSX | ADMIN | Crear |
| Compras | Compras por período | Fechas, sucursal, proveedor, estado | PDF/XLSX | ADMIN | Crear |

La Entrega 1 centraliza primero todo lo que ya funciona y añade reportes consolidados para módulos que actualmente
no tienen exportación gerencial.

### 3.2 Entrega 2: análisis y control gerencial

| Categoría | Reporte | Filtros | Formato | Rol recomendado | Estado |
|---|---|---|---|---|---|
| Ventas | Ventas por producto/categoría | Fechas, sucursal, categoría | PDF/XLSX | ADMIN | Crear |
| Ventas | Ventas por cliente | Fechas, sucursal, cliente | XLSX | ADMIN | Crear |
| Ventas | Rentabilidad por producto | Fechas, sucursal, categoría | PDF/XLSX | ADMIN | Crear |
| Inventario | Productos bajo stock mínimo | Sucursal, categoría | PDF/XLSX | ADMIN, INVENTARIO | Crear |
| Inventario | Kardex de producto | Fechas, sucursal, producto | PDF/XLSX | ADMIN, INVENTARIO | Crear |
| Inventario | Valorización de inventario | Fecha de corte, sucursal | PDF/XLSX | ADMIN | Crear |
| Proformas | Conversión de proformas a ventas | Fechas, sucursal, usuario | PDF/XLSX | ADMIN | Crear |
| Notas de crédito | Productos pendientes de despacho | Fechas, sucursal, cliente | PDF/XLSX | ADMIN, INVENTARIO | Crear |
| Compras | Compras por proveedor/producto | Fechas, sucursal, proveedor | XLSX | ADMIN | Crear |

La “rentabilidad” debe definirse como venta neta menos costo registrado. Antes de liberarla se debe acordar qué
costo histórico aplicar cuando el precio de compra del producto haya cambiado.

### 3.3 Fuera del catálogo central

Los siguientes son comprobantes, no reportes consolidados, y deben permanecer en sus flujos actuales:

- Factura individual.
- Proforma individual.
- Nota de crédito individual.
- Comprobante de despacho de nota de crédito.

Se pueden enlazar desde el módulo en una fase futura, pero no deben aparecer como tarjetas de reportes gerenciales.

## 4. Diseño que deben seguir los reportes

### 4.1 PDF

Definir una plantilla visual común para JasperReports:

- **Tamaño:** A4 vertical para hasta 6 columnas; A4 horizontal para tablas de 7 o más columnas.
- **Márgenes:** 24–30 puntos y área imprimible consistente.
- **Encabezado:** logo D'Todo, nombre del reporte, sucursal y período consultado.
- **Metadatos:** fecha/hora de generación y usuario que solicitó el reporte.
- **Resumen:** indicadores relevantes antes del detalle, por ejemplo total vendido, costo, ganancia, unidades o
  cantidad de documentos.
- **Tabla:** encabezado con color institucional, filas alternas suaves, números alineados a la derecha y texto a la
  izquierda.
- **Formato:** fechas `dd/MM/yyyy`, horas `HH:mm`, moneda `Q #,##0.00`, porcentajes `0.00 %` y cantidades con
  separador de miles.
- **Totales:** subtotales por agrupación cuando aplique y total general al final.
- **Pie:** “Página X de Y”, fecha de generación y aviso de uso interno.
- **Sin datos:** devolver una respuesta controlada o una página explícita “No se encontraron registros”; nunca un
  PDF vacío.

El logo, dirección, NIT y teléfonos deben obtenerse de parámetros/configuración o de la sucursal. No se deben
codificar direcciones, marcas antiguas ni rutas absolutas de Windows dentro de los `.jrxml`.

### 4.2 Excel

El Excel debe estar orientado a análisis, no a imitar visualmente el PDF:

- Una fila de encabezados congelada, autofiltro y ancho de columnas legible.
- Tipos reales de fecha y número; no exportar importes como texto.
- Una fila por registro de detalle y columnas estables/documentadas.
- Sin celdas combinadas en la tabla de datos.
- Hoja `Resumen` opcional y hoja `Detalle` para reportes con agregados.
- Nombre de archivo predecible: `{codigo}_{sucursal}_{yyyyMMdd}_{yyyyMMdd}.xlsx`.

### 4.3 Reglas funcionales comunes

- El rango debe interpretarse con la zona horaria de Guatemala e incluir el día final completo.
- Los documentos anulados deben excluirse por defecto o mostrarse claramente según el reporte.
- Todo reporte debe indicar los filtros aplicados.
- La sucursal efectiva debe validarse en el backend; no se debe confiar únicamente en el selector del frontend.
- El resultado de totales debe cuadrar con los listados operativos usando exactamente los mismos estados y reglas.

## 5. Cambios en el backend

### 5.1 Estructura nueva

Agregar:

- `controller/ReporteApiController.java`.
- `service/IReporteService.java`.
- `service/impl/ReporteServiceImpl.java`.
- `dto/ReporteFiltroDto.java` y, si se ofrece vista previa, DTOs de resumen por categoría.
- `model/enums/FormatoReporteEnum.java` y `CodigoReporteEnum.java`.
- `service/impl/JasperReportService.java` como infraestructura común de compilación, llenado y exportación.

No se debe aceptar desde el cliente el nombre de una plantilla o una consulta SQL. El backend debe mapear cada
`CodigoReporteEnum` a una implementación autorizada.

### 5.2 Contrato HTTP

Usar endpoints explícitos y consistentes:

```text
GET /api/reportes/ventas/poliza-individual
GET /api/reportes/ventas/poliza-general
GET /api/reportes/inventario/movimientos
GET /api/reportes/inventario/existencias
GET /api/reportes/proformas
GET /api/reportes/notas-credito
GET /api/reportes/compras
GET /api/reportes/ventas/por-producto
GET /api/reportes/ventas/por-cliente
GET /api/reportes/ventas/rentabilidad
GET /api/reportes/inventario/bajo-stock
GET /api/reportes/inventario/kardex
GET /api/reportes/inventario/valorizacion
GET /api/reportes/proformas/conversion
GET /api/reportes/notas-credito/pendientes-despacho
GET /api/reportes/compras/por-proveedor-producto
```

Parámetros comunes: `fechaInicio`, `fechaFin`, `idSucursal`, `formato`; agregar `idUsuario`, `idProveedor` o
`estado` únicamente donde correspondan. Las fechas se reciben como ISO `yyyy-MM-dd` y se validan antes de consultar.

Todas las respuestas de archivo deben incluir:

- `Content-Type` correcto (`application/pdf` o XLSX).
- `Content-Disposition` con nombre de archivo seguro.
- `Content-Length` cuando esté disponible.
- `Cache-Control: no-store` por tratarse de información comercial.

### 5.3 Migración de funciones existentes

1. Hacer que los endpoints nuevos deleguen inicialmente en la lógica comprobada de `FacturaServiceImpl`,
   `MovimientoProductoServiceImpl`, `ProductoServiceImpl` y `ProformaServiceImpl`.
2. Mantener temporalmente los endpoints antiguos para no romper pantallas existentes.
3. Cambiar el frontend al controlador de reportes.
4. Marcar los endpoints antiguos como obsoletos y retirarlos en una versión posterior.
5. Eliminar o implementar correctamente `/productos/pdf/inventario`; no dejar un endpoint exitoso con cuerpo nulo.

### 5.4 JasperReports

- Mantener `.jrxml` como fuente versionada.
- Compilar las plantillas una vez al iniciar o almacenarlas en caché; no compilar por cada descarga.
- Centralizar apertura/cierre de conexiones y `InputStream` con `try-with-resources`.
- Traducir errores de Jasper/SQL a `ReportGenerationException`, sin exponer consultas ni datos sensibles.
- Crear estilos compartidos o una plantilla base para encabezado, tipografía, tabla y pie.
- Auditar y retirar `.jasper`/`.jrxml` antiguos solamente después de confirmar que no tienen consumidores.

### 5.5 Consultas, rendimiento y seguridad

- Implementar consultas de solo lectura que reciban parámetros tipados.
- Evitar cargar entidades completas y relaciones innecesarias; usar DTO/proyecciones para reportes grandes.
- Revisar planes de ejecución antes de agregar índices. Como mínimo, evaluar índices compuestos sobre fecha,
  sucursal, usuario/estado en ventas, movimientos, proformas, notas de crédito y compras.
- Aplicar límites de rango configurables para PDF y recomendar XLSX para volúmenes grandes.
- Autorizar cada endpoint con `@Secured` y validar la sucursal contra el usuario autenticado.
- Registrar código de reporte, filtros no sensibles, usuario, duración y tamaño generado; nunca registrar contenido.

### 5.6 Pruebas del backend

- Unitarias: validación de fechas, sucursal, roles, nombres de archivo, mapeo de código/formato y caso sin datos.
- Repositorio: filtros por fecha inclusiva, sucursal, estado y usuario.
- Integración MVC: `200`, tipo MIME, `Content-Disposition`, `400`, `403`, `404/sin datos` y `500` controlado.
- Jasper: prueba de compilación de cada `.jrxml` y generación con un conjunto mínimo de datos.
- Conciliación: comparar totales del reporte con consultas/listados operativos conocidos.

## 6. Cambios en el frontend

### 6.1 Archivos y registro

Agregar:

- `components/reportes/reportes.component.{ts,html,css,spec.ts}`.
- `components/reportes/reporte-filtros/reporte-filtros.component.{ts,html,css,spec.ts}`.
- `services/reporte.service.ts`.
- `models/reporte-definicion.ts` y `dtos/reporte-filtro-dto.ts`.

Modificar:

- `app.module.ts` para declarar los componentes.
- `app.routing.ts` para registrar `/reportes` con `AuthGuard` y `RoleGuard`.
- `sidebar.component.html` y, si hace falta para la búsqueda, `sidebar.component.ts`.

### 6.2 Servicio y descarga

`ReporteService` debe:

- Construir `HttpParams` sin concatenar manualmente fechas o identificadores.
- Solicitar `responseType: 'blob'` y `observe: 'response'`.
- Leer el nombre del archivo desde `Content-Disposition` con un valor seguro de respaldo.
- Compartir una función para vista previa PDF y descarga XLSX.
- Revocar siempre los `ObjectURL` y retirar elementos temporales del DOM.

### 6.3 Permisos

Matriz inicial de acceso para implementar:

- `ROLE_ADMIN`: acceso a todo el catálogo y a todas las sucursales.
- `ROLE_INVENTARIO`: inventario, kardex, bajo stock y pendientes de despacho; restringido a su sucursal salvo regla
  de negocio explícita.
- `ROLE_COBRADOR`: sin reportes gerenciales en la primera entrega. Si se habilita un cierre propio, limitarlo al usuario
  autenticado y su sucursal.

Ocultar tarjetas no autorizadas mejora la experiencia, pero la autorización definitiva siempre debe estar en el
backend.

### 6.4 Pruebas del frontend

- Catálogo visible según rol.
- Filtros requeridos según el reporte seleccionado.
- Validación de rango invertido.
- Cambio y restricción de sucursal.
- Estado generando y prevención de doble clic.
- Vista previa PDF, descarga XLSX y manejo del nombre del archivo.
- Respuestas `400`, `403`, sin datos y error de servidor.
- Comportamiento responsivo y navegación con teclado.

## 7. Plan de ejecución

### Etapa 0 — Definiciones funcionales

- Confirmar las columnas, estados incluidos, roles y reglas de costo/ganancia de cada reporte aprobado.
- Crear ejemplos aprobados de un PDF vertical, un PDF horizontal y un XLSX.
- Definir criterios de conciliación para cada total.

### Etapa 1 — Base técnica del backend

- Crear controlador, interfaz, servicio común Jasper, DTOs, enums, validación y manejo de errores.
- Añadir pruebas de infraestructura y contrato HTTP.

### Etapa 2 — Base técnica del frontend

- Crear ruta, menú, catálogo de tarjetas, filtros compartidos y servicio de archivos.
- Implementar roles, estados de carga, errores y pruebas del componente.

### Etapa 3 — Migración de reportes existentes

- Migrar póliza individual, póliza general, inventario, productos y proformas.
- Comparar byte/contenido y, principalmente, totales con los reportes actuales.
- Mantener compatibilidad temporal con los endpoints anteriores.

### Etapa 4 — Cobertura operativa nueva

- Implementar resumen de notas de crédito y compras por período.
- Agregar Excel de movimientos de inventario.
- Validar rendimiento con volúmenes representativos.

### Etapa 5 — Reportes de análisis de ventas e inventario

- Implementar ventas por producto/categoría, ventas por cliente y rentabilidad por producto.
- Implementar productos bajo stock mínimo, kardex y valorización del inventario.
- Conciliar ventas, costos, ganancias, entradas, salidas y saldos contra datos operativos conocidos.
- Validar y optimizar consultas e índices con volúmenes representativos.

### Etapa 6 — Reportes de análisis comercial y abastecimiento

- Implementar conversión de proformas a ventas.
- Implementar productos de notas de crédito pendientes de despacho.
- Implementar compras por proveedor/producto.
- Validar que los indicadores respeten sucursal, usuario, estados y períodos seleccionados.

### Etapa 7 — Homologación y liberación completa

- Aplicar plantilla visual común a todos los PDF.
- Realizar pruebas de permisos, conciliación, impresión, Excel y móvil.
- Documentar endpoints, filtros, reglas y reportes obsoletos.
- Liberar progresivamente por entrega y categoría, monitoreando duración y errores de generación.
- Considerar terminado el módulo únicamente cuando estén disponibles los 16 reportes aprobados.

## 8. Criterios de aceptación

- Existe una única entrada **Reportes** en el menú y su catálogo respeta los roles.
- Los 16 reportes aprobados se generan con filtros válidos, formatos disponibles y nombres de archivo correctos.
- Los usuarios no pueden consultar sucursales o reportes fuera de sus permisos modificando la URL.
- Los totales cuadran con las operaciones del sistema para el mismo rango, sucursal y estado.
- Ninguna plantilla contiene rutas absolutas ni datos comerciales que deban provenir de configuración/sucursal.
- Los PDF se imprimen sin cortes y los XLSX conservan tipos numéricos, fechas y autofiltros.
- La generación falla de forma controlada, con mensajes útiles y sin revelar SQL o trazas internas.
- Las pruebas relevantes de Angular y Spring Boot quedan automatizadas y documentadas.

## 9. Decisiones que deben confirmarse antes de implementar

1. Si `ROLE_COBRADOR` tendrá acceso a un reporte de cierre propio.
2. Si las notas anuladas se excluyen o se incluyen como una sección separada.
3. Qué costo se utilizará para reportar rentabilidad histórica.
4. Si la exportación Excel debe incluir costo y ganancia para `ROLE_INVENTARIO`.
5. Cuál es el período máximo permitido y el comportamiento esperado para reportes sin datos.
