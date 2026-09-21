# Registro de implementación — Módulo de Reportes

Fecha de inicio: 2026-09-19  
Plan funcional: `PLAN_REPORTES.md`

Este archivo es el punto de reanudación del trabajo. Debe actualizarse después de cada bloque implementado o
verificado.

## Estado general

- Estado: **En implementación**.
- Hito completado: **R1 — Base técnica y centralización de reportes existentes**.
- Hito siguiente: **R2 — Cobertura operativa nueva**.
- Alcance aprobado: 16 reportes distribuidos entre ventas, inventario, proformas, notas de crédito y compras.
- Última actualización: 2026-09-20.

## Cambios realizados

### 2026-09-19 — Inicio

- Se creó `PLAN_REPORTES.md` con el diseño funcional, técnico y las etapas de entrega.
- Se confirmó que los 16 reportes propuestos forman parte del alcance a implementar.
- Se auditó la implementación existente:
  - Póliza individual: `/api/facturas/daily-sales`.
  - Póliza general: `/api/facturas/general-policy`.
  - Movimientos de inventario: `/api/movimientos/inventario`.
  - Existencias/productos: `/api/productos/excel`.
  - Proformas: `/api/proformas/excel`.
- Se detectó que `/api/productos/pdf/inventario` devuelve `null` y debe corregirse o retirarse.
- Se detectó generación Jasper duplicada y compilación de `.jrxml` por solicitud en varios servicios.

### 2026-09-19 — R1: backend centralizado

- Se creó `ReporteApiController` bajo `/api/reportes`.
- Se creó la fachada `IReporteService` / `ReporteServiceImpl` con validación común de identificadores y fechas ISO.
- Se centralizaron cinco capacidades existentes sin retirar los endpoints anteriores:
  - `GET /api/reportes/ventas/poliza-individual`.
  - `GET /api/reportes/ventas/poliza-general`.
  - `GET /api/reportes/inventario/movimientos`.
  - `GET /api/reportes/inventario/existencias`.
  - `GET /api/reportes/proformas`.
- Se agregó `GET /api/reportes/proformas/usuarios` para alimentar el filtro del módulo.
- Las respuestas incorporan MIME, `Content-Disposition`, `Content-Length` y `Cache-Control: no-store`.
- Para reportes de inventario, `ROLE_INVENTARIO` queda restringido a la sucursal contenida en su JWT; un
  administrador puede seleccionar sucursal.
- Se agregó manejo explícito de `AccessDeniedException` como HTTP 403 para evitar que una selección de sucursal no
  autorizada termine como error 500.
- Se agregó `ReporteServiceImplTest` con cobertura de delegación, sucursal, formato de fecha y rango invertido.

### 2026-09-19 — R1: módulo Angular

- Se creó la ruta protegida `/reportes` para `ROLE_ADMIN` y `ROLE_INVENTARIO`.
- Se agregó **Reportes** al menú lateral y a su buscador.
- Se creó un catálogo responsivo con los 16 reportes aprobados, agrupados en cinco categorías.
- Los cinco reportes centralizados están habilitados; los once aún no implementados se muestran como
  **En preparación** y no permiten interacción.
- Se creó un panel compartido de configuración con sucursal, usuario y rango de fechas según cada reporte.
- Se creó `ReporteService` para construir `HttpParams`, consumir blobs, abrir PDF y descargar XLSX usando el nombre
  enviado por el backend.
- Se añadieron los modelos `ReporteDefinicion` y `ReporteFiltroDto`.
- Se añadió una prueba que fija el catálogo en 16 reportes y otra para rangos invertidos.
- El script `pnpm test` ahora aplica `--openssl-legacy-provider`, consistente con los scripts de build existentes.
- Se añadió el launcher optativo `ChromeHeadlessNoSandbox` para pruebas en entornos Windows sin GPU utilizable.

### 2026-09-19 — Corrección responsive del selector de fechas

- Se corrigió el calendario compartido, que quedaba recortado dentro de contenedores con scroll y producía
  desplazamiento horizontal en el panel de reportes.
- En escritorio, el calendario ahora usa posicionamiento fijo respecto al viewport y calcula automáticamente si debe
  abrirse arriba o abajo del campo según el espacio disponible.
- La posición se recalcula al desplazar cualquier contenedor y al cambiar el tamaño de la ventana.
- El ancho y alto quedan limitados al viewport para impedir que el calendario salga de pantalla.
- En móvil, el calendario se presenta como una hoja inferior de ancho completo, con fondo atenuado, scroll vertical
  interno y consideración del área segura inferior del dispositivo.
- Se conservaron la identidad visual, los accesos rápidos, la selección de rango y el comportamiento con teclado.
- Verificación posterior: build Angular correcto y 92 pruebas frontend correctas.

### 2026-09-19 — Jerarquía visual y guía de origen del panel

- Se enriqueció el encabezado del configurador con degradado sutil, línea de acento, icono con contraste, categoría,
  descripción breve y botón de cierre circular.
- Se corrigió el icono inexistente de la póliza individual por `fa-user-tie`.
- El panel ahora conserva la referencia al elemento que lo abrió y muestra una cola tipo globo de cómic apuntando a
  esa tarjeta.
- La cola selecciona automáticamente el lado superior, inferior, izquierdo o derecho y limita su desplazamiento a
  los bordes seguros del panel.
- La guía se recalcula durante scroll y resize para continuar señalando la tarjeta de origen.
- En móvil se oculta la cola para evitar ruido visual; la tarjeta permanece resaltada y el panel conserva su patrón
  compacto de hoja flotante.
- El scroll se movió al cuerpo del configurador, manteniendo encabezado y acciones visibles y permitiendo que la cola
  sobresalga sin quedar recortada.
- Verificación posterior: build Angular correcto y 92 pruebas frontend correctas.

### 2026-09-19 — Posicionamiento contextual del configurador

- El configurador ahora se ubica con relación a la tarjeta que lo abrió y evita cubrirla siempre que el espacio de
  la pantalla lo permite.
- Se prioriza el costado con espacio suficiente; cuando no existe, el panel se coloca arriba o abajo de la tarjeta.
- El ancho y la altura máxima se adaptan al espacio disponible, conservando el desplazamiento interno del formulario.
- La cola del globo se recalcula después de posicionar el panel para mantener una conexión precisa con la tarjeta
  seleccionada.
- En móvil se mantiene el panel inferior a ancho completo, sin estilos de escritorio residuales al cambiar la
  orientación.
- Verificación posterior: build Angular correcto y 92 pruebas frontend correctas.

## Decisiones técnicas vigentes

- Los endpoints existentes se conservarán durante la migración para no romper los módulos actuales.
- Los endpoints nuevos vivirán bajo `/api/reportes`.
- El backend seguirá siendo la autoridad para roles y sucursal; ocultar opciones en Angular no sustituye la
  autorización.
- Los comprobantes individuales no se trasladarán al catálogo gerencial.
- Los reportes nuevos se habilitarán en la interfaz solamente cuando su endpoint esté terminado.

### 2026-09-19 — Resumen de notas de crédito

- Se agregó `ResumenNotasCreditoReporteServiceImpl` como servicio dedicado para generar PDF y XLSX desde una única
  plantilla Jasper.
- Se creó `resumen_notas_credito.jrxml` en formato A4 horizontal, con detalle de nota, fecha, cliente, NIT,
  documento origen, vendedor, estado, importe, cantidad total e importe acumulado.
- El período incluye el día final completo mediante una fecha final exclusiva calculada en el servicio.
- Se agregó el endpoint administrativo `GET /api/reportes/notas-credito/resumen` con filtros de sucursal, período,
  estado opcional y formato.
- El encabezado del reporte muestra el nombre real de la sucursal y el estado seleccionado.
- Se habilitó la tarjeta del reporte en Angular, junto con el selector de estado y la elección PDF/XLSX.
- Se añadió una prueba de compilación, llenado y exportación PDF de la plantilla con datos representativos.
- Verificación: build Angular correcto, 92 pruebas frontend correctas, 7 pruebas backend focalizadas correctas y
  suite backend completa con 100 pruebas correctas bajo JDK 17.
- Se generó y revisó visualmente `output/pdf/resumen-notas-credito-muestra.pdf`; no presenta recortes, solapamientos
  ni problemas de legibilidad.
- A solicitud del usuario, se incorporó el logo oficial de D'TODO al encabezado y se reajustó la composición del
  título para conservar la jerarquía y separación de los filtros.
- Se preparó una segunda composición con el logo reducido y alineado al extremo derecho, un encabezado menos alto y
  el estado separado del identificador visual.
- En la tercera composición, el rectángulo azul termina antes del logo y deja una separación blanca; el logo queda
  aislado sobre el fondo blanco del documento para evitar la superposición de tonos azules.
- En la composición final propuesta, el logo vuelve al lado izquierdo, permanece sobre fondo blanco y tiene la misma
  altura que el rectángulo azul; ambos elementos se separan mediante un margen blanco.
- Se corrigió el estado sin resultados: ahora muestra un mensaje informativo, cantidad `0` e importe `Q 0.00`, sin
  valores `null`.
- Se añadió un margen seguro de 10 puntos a la columna Total, la línea de resumen y el pie de página para evitar
  recortes en el extremo derecho durante la exportación PDF.
- El pie muestra `Página X de Y` en todas las páginas, incluida la página final que contiene el resumen.
- La plantilla usa `DejaVu Sans` como fuente predeterminada, suministrada por `jasperreports-fonts`, para conservar
  métricas y glifos al compilar en AlmaLinux sin depender de fuentes de Microsoft instaladas en el sistema.
- Estado: implementación terminada y aprobada visualmente por el usuario.

### 2026-09-20 — Compras por período

- Se inició el reporte PDF/XLSX de compras filtrado por sucursal, período, proveedor opcional y estado opcional.
- Se agregó un servicio Jasper dedicado, el endpoint administrativo `GET /api/reportes/compras` y la plantilla
  `compras_periodo.jrxml`.
- La plantilla reutiliza el patrón visual aprobado: logo sobre fondo blanco, encabezado azul compacto, tabla,
  resumen, estado sin datos, fuente DejaVu Sans y paginación `Página X de Y` incluida en la página final.
- Se habilitó la tarjeta en Angular y se incorporaron los selectores de proveedor, estado y formato.
- Verificación: build Angular correcto, 92 pruebas frontend correctas, 10 pruebas backend focalizadas correctas y
  suite backend completa con 104 pruebas correctas bajo JDK 17.
- Se generó y revisó visualmente `output/pdf/compras-periodo-muestra.pdf`; no presenta recortes, solapamientos ni
  problemas de legibilidad.
- Estado: implementación terminada y aprobada visualmente por el usuario.

### 2026-09-20 — Ventas por producto o categoría

- Se inició el reporte PDF/XLSX de ventas agrupadas por producto, con filtro opcional de categoría.
- El reporte presenta unidades, venta bruta, descuentos y venta neta, excluyendo facturas anuladas.
- Se agregó un servicio Jasper dedicado y el endpoint administrativo `GET /api/reportes/ventas/productos`.
- Se habilitó la tarjeta de Angular y se añadió el selector de categorías.
- La plantilla usa el patrón visual aprobado, fuente DejaVu Sans, estado sin datos y paginación `Página X de Y`.
- Verificación: build Angular correcto, 92 pruebas frontend correctas, 13 pruebas backend focalizadas correctas y
  suite backend completa con 111 pruebas correctas bajo JDK 17.
- Se generó y revisó visualmente `output/pdf/ventas-producto-muestra.pdf`; no presenta recortes, solapamientos ni
  problemas de legibilidad.
- Estado: implementación terminada, pendiente de aprobación visual del usuario antes del commit.

### 2026-09-20 — Ventas por cliente

- Se inició el reporte PDF/XLSX de ventas acumuladas por cliente, con filtro opcional de cliente.
- El reporte presenta NIT, teléfono, facturas, unidades, venta bruta, descuentos, venta neta y última compra.
- Se agregó un servicio Jasper dedicado y el endpoint administrativo `GET /api/reportes/ventas/clientes`.
- Se habilitó la tarjeta en Angular y se añadió el selector de clientes.
- La plantilla conserva el patrón visual aprobado, paginación `Página X de Y` en PDF y tipos numéricos y fecha real
  para facilitar análisis en Excel.
- Verificación: build Angular correcto, 94 pruebas frontend correctas, 18 pruebas backend focalizadas correctas y
  suite backend completa con 118 pruebas correctas bajo JDK 17.
- Se generaron y revisaron visualmente `output/pdf/ventas-cliente-muestra.pdf` y
  `output/xlsx/ventas-cliente-muestra.xlsx`; no presentan recortes, solapamientos ni problemas de legibilidad.
- Se creó un selector reutilizable y se aplicó a sucursales, usuarios, categorías, clientes, proveedores y estados.
  Todos mantienen altura limitada, búsqueda sin distinguir acentos, cierre por Escape/clic externo y adaptación móvil.
- Los selectores remotos ahora consumen DTO ligeros generados directamente por consultas de proyección, sin cargar
  entidades completas ni sus relaciones. Las opciones quedan en caché durante la sesión de la pantalla.
- Estado: implementación terminada, pendiente de aprobación visual del usuario antes del commit.

### 2026-09-20 — Movimientos y saldo de inventario

- Se inició la modernización del reporte de movimientos y saldo con una plantilla compartida para PDF/XLSX.
- El reporte resume por producto el stock inicial, entradas, salidas y saldo final del período y sucursal.
- Se corrigió la clasificación de movimientos según los enums vigentes, incluyendo compras, anulaciones y entregas
  de notas de crédito.
- Se agregó un servicio Jasper dedicado y formato seleccionable al endpoint existente del módulo de reportes.
- La plantilla utiliza el patrón visual aprobado, fuente DejaVu Sans, estado sin datos y paginación `Página X de Y`.
- La tarjeta de Angular ahora permite seleccionar PDF o XLSX.
- Verificación: build Angular correcto, 92 pruebas frontend correctas, 11 pruebas backend focalizadas correctas y
  suite backend completa con 107 pruebas correctas bajo JDK 17.
- Se generó y revisó visualmente `output/pdf/movimientos-inventario-muestra.pdf`; no presenta recortes,
  solapamientos ni problemas de legibilidad.
- Estado: implementación terminada y aprobada visualmente por el usuario.

## Verificación acumulada

- `pnpm run build`: **correcto**.
- `pnpm test -- --watch=false --browsers=ChromeHeadlessNoSandbox`: **92 pruebas correctas**.
- Suite backend `mvn test`: **92 pruebas existentes correctas** antes de añadir las pruebas del nuevo servicio.
- `ReporteServiceImplTest`: **4 pruebas correctas**.
- `pnpm run lint`: ejecutado; continúa fallando por deuda preexistente en archivos ajenos al módulo. Los archivos
  nuevos de reportes no aparecen entre los errores. `app.routing.ts` ya tenía longitud/trailing whitespace pendiente.
- Nota del entorno: Maven Wrapper resuelve erróneamente el repositorio como `C:\.m2`; para verificar se ejecutó el
  Maven descargado por el wrapper con `-Dmaven.repo.local=C:\Users\Ramie\.m2\repository`.
- Nota del entorno: con JDK 25, `javac` reportó de forma intermitente un error fatal al leer JARs de Jasper/Mockito;
  al repetir después de generarse las clases, la compilación y las pruebas finalizaron correctamente. El proyecto
  declara Java 17 y debe validarse con JDK 17 en CI.

## Pendiente inmediato

1. Verificar y obtener aprobación visual de ventas por producto o categoría.
2. Habilitar cada opción en el catálogo solamente después de verificar su endpoint.
3. Iniciar la centralización/caché de compilación Jasper para evitar compilar `.jrxml` por solicitud.

## Riesgos o decisiones pendientes

- Definir el costo histórico usado por rentabilidad y valorización.
- Confirmar tratamiento de documentos anulados.
- Confirmar si `ROLE_COBRADOR` tendrá un cierre propio.
- Definir período máximo de consulta por formato.
