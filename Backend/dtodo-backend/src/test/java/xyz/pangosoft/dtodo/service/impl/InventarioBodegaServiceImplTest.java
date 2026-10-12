package xyz.pangosoft.dtodo.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;

import xyz.pangosoft.dtodo.dto.ImportacionInventarioDto;
import xyz.pangosoft.dtodo.dto.MovimientoBodegaRequest;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.model.Bodega;
import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.InventarioBodega;
import xyz.pangosoft.dtodo.model.MarcaProducto;
import xyz.pangosoft.dtodo.model.MovimientoBodega;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.TipoProducto;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.OrigenInventarioBodegaEnum;
import xyz.pangosoft.dtodo.model.enums.TipoMovimientoBodegaEnum;
import xyz.pangosoft.dtodo.repository.IInventarioBodegaRepository;
import xyz.pangosoft.dtodo.repository.IMovimientoBodegaRepository;
import xyz.pangosoft.dtodo.repository.IProductoRepository;
import xyz.pangosoft.dtodo.service.IBodegaService;
import xyz.pangosoft.dtodo.service.IInventarioSucursalService;
import xyz.pangosoft.dtodo.service.IMarcaProductoService;
import xyz.pangosoft.dtodo.service.IProductoService;
import xyz.pangosoft.dtodo.service.ISucursalService;
import xyz.pangosoft.dtodo.service.ITipoProductoService;
import xyz.pangosoft.dtodo.service.IUsuarioService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InventarioBodegaServiceImplTest {

    private final IInventarioBodegaRepository inventarioRepo = mock(IInventarioBodegaRepository.class);
    private final IMovimientoBodegaRepository movimientoRepo = mock(IMovimientoBodegaRepository.class);
    private final IProductoRepository productoRepo = mock(IProductoRepository.class);
    private final IBodegaService bodegaService = mock(IBodegaService.class);
    private final IProductoService productoService = mock(IProductoService.class);
    private final IUsuarioService usuarioService = mock(IUsuarioService.class);
    private final ISucursalService sucursalService = mock(ISucursalService.class);
    private final IMarcaProductoService marcaService = mock(IMarcaProductoService.class);
    private final ITipoProductoService tipoService = mock(ITipoProductoService.class);
    private final IInventarioSucursalService inventarioSucursalService = mock(IInventarioSucursalService.class);

    private final InventarioBodegaServiceImpl service = new InventarioBodegaServiceImpl(
            inventarioRepo, movimientoRepo, productoRepo, bodegaService, productoService, usuarioService,
            sucursalService, marcaService, tipoService, inventarioSucursalService);

    private final Bodega bodega = Bodega.builder().idBodega(1).nombre("Principal")
            .estado(Estado.builder().idEstado(1).estado("ACTIVO").build()).build();
    private final Producto producto = Producto.builder().idProducto(5).codProducto("P-5").nombre("Cuaderno").build();
    private final Usuario usuario = Usuario.builder().idUsuario(9).usuario("bodeguero").build();

    private void prepararEntorno() {
        when(bodegaService.findActivaById(1)).thenReturn(bodega);
        when(bodegaService.findById(1)).thenReturn(bodega);
        when(productoService.findById(5)).thenReturn(producto);
        when(usuarioService.findById(9)).thenReturn(usuario);
        when(inventarioRepo.save(any(InventarioBodega.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(movimientoRepo.save(any(MovimientoBodega.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private InventarioBodega filaConStock(int stock) {
        return InventarioBodega.builder().bodega(bodega).producto(producto).stock(stock).build();
    }

    // ---------- agregar producto ----------

    @Test
    void agregarProductoNuevoCreaLaFilaConStockCeroYSumaLaCantidad() {
        prepararEntorno();
        when(inventarioRepo.findParaActualizar(1, 5)).thenReturn(Optional.empty());

        MovimientoBodega movimiento = service.agregarProducto(
                1, new MovimientoBodegaRequest(5, 10, 3, "Carga inicial"), 9);

        assertEquals(TipoMovimientoBodegaEnum.INGRESO, movimiento.getTipoMovimiento());
        assertEquals(0, movimiento.getStockInicial());
        assertEquals(10, movimiento.getStockFinal());
        assertEquals("Carga inicial", movimiento.getMotivo());
        assertEquals(usuario, movimiento.getUsuario());

        ArgumentCaptor<InventarioBodega> guardado = ArgumentCaptor.forClass(InventarioBodega.class);
        verify(inventarioRepo, atLeastOnce()).save(guardado.capture());
        assertEquals(10, guardado.getValue().getStock());
        assertEquals(3, guardado.getValue().getStockMinimo());
    }

    @Test
    void agregarProductoYaExistenteSumaALaExistenciaActual() {
        prepararEntorno();
        InventarioBodega existente = filaConStock(20);
        existente.setStockMinimo(4);
        when(inventarioRepo.findParaActualizar(1, 5)).thenReturn(Optional.of(existente));

        MovimientoBodega movimiento = service.agregarProducto(1, new MovimientoBodegaRequest(5, 5, null, null), 9);

        assertEquals(20, movimiento.getStockInicial());
        assertEquals(25, movimiento.getStockFinal());
        assertEquals(25, existente.getStock());
        assertEquals(4, existente.getStockMinimo());
    }

    @Test
    void agregarProductoRechazaCantidadCeroONegativaYStockMinimoNegativo() {
        assertThrows(BadRequestException.class,
                () -> service.agregarProducto(1, new MovimientoBodegaRequest(5, 0, null, null), 9));
        assertThrows(BadRequestException.class,
                () -> service.agregarProducto(1, new MovimientoBodegaRequest(5, -3, null, null), 9));
        assertThrows(BadRequestException.class,
                () -> service.agregarProducto(1, new MovimientoBodegaRequest(5, 3, -1, null), 9));
        assertThrows(BadRequestException.class,
                () -> service.agregarProducto(1, new MovimientoBodegaRequest(null, 3, null, null), 9));

        verifyNoInteractions(inventarioRepo, movimientoRepo);
    }

    @Test
    void agregarProductoALaBodegaInactivaSePropagaComoError() {
        when(bodegaService.findActivaById(1)).thenThrow(new BadRequestException("inactiva", null));

        assertThrows(BadRequestException.class,
                () -> service.agregarProducto(1, new MovimientoBodegaRequest(5, 1, null, null), 9));

        verify(movimientoRepo, never()).save(any(MovimientoBodega.class));
    }

    @Test
    void agregarProductoRechazaUnMotivoDemasiadoLargo() {
        String largo = "x".repeat(301);

        assertThrows(BadRequestException.class,
                () -> service.agregarProducto(1, new MovimientoBodegaRequest(5, 1, null, largo), 9));
    }

    // ---------- reducir existencias ----------

    @Test
    void reducirExistenciasRestaLaCantidadYRegistraElMotivo() {
        prepararEntorno();
        when(inventarioRepo.findParaActualizar(1, 5)).thenReturn(Optional.of(filaConStock(30)));

        MovimientoBodega movimiento = service.reducirExistencias(
                1, new MovimientoBodegaRequest(5, 8, null, "Merma por daño"), 9);

        assertEquals(TipoMovimientoBodegaEnum.REDUCCION, movimiento.getTipoMovimiento());
        assertEquals(30, movimiento.getStockInicial());
        assertEquals(22, movimiento.getStockFinal());
        assertEquals("Merma por daño", movimiento.getMotivo());
    }

    @Test
    void reducirExistenciasPermiteDejarElStockExactamenteEnCero() {
        prepararEntorno();
        when(inventarioRepo.findParaActualizar(1, 5)).thenReturn(Optional.of(filaConStock(8)));

        MovimientoBodega movimiento = service.reducirExistencias(
                1, new MovimientoBodegaRequest(5, 8, null, "Ajuste"), 9);

        assertEquals(0, movimiento.getStockFinal());
    }

    @Test
    void reducirExistenciasNoPermiteStockNegativoYNoRegistraMovimiento() {
        prepararEntorno();
        InventarioBodega fila = filaConStock(5);
        when(inventarioRepo.findParaActualizar(1, 5)).thenReturn(Optional.of(fila));

        BadRequestException error = assertThrows(BadRequestException.class, () -> service.reducirExistencias(
                1, new MovimientoBodegaRequest(5, 6, null, "Ajuste"), 9));

        assertTrue(error.getMessage().contains("Disponible: 5"));
        assertEquals(5, fila.getStock());
        verify(movimientoRepo, never()).save(any(MovimientoBodega.class));
    }

    @Test
    void reducirExistenciasDeUnProductoQueNoEstaEnLaBodegaEsStockInsuficiente() {
        prepararEntorno();
        when(inventarioRepo.findParaActualizar(1, 5)).thenReturn(Optional.empty());

        BadRequestException error = assertThrows(BadRequestException.class, () -> service.reducirExistencias(
                1, new MovimientoBodegaRequest(5, 1, null, "Ajuste"), 9));

        assertTrue(error.getMessage().contains("Disponible: 0"));
        verify(inventarioRepo, never()).save(any(InventarioBodega.class));
    }

    @Test
    void reducirExistenciasExigeMotivo() {
        assertThrows(BadRequestException.class,
                () -> service.reducirExistencias(1, new MovimientoBodegaRequest(5, 1, null, "  "), 9));
        assertThrows(BadRequestException.class,
                () -> service.reducirExistencias(1, new MovimientoBodegaRequest(5, 1, null, null), 9));
    }

    // ---------- eliminar producto ----------

    @Test
    void eliminarProductoDescuentaTodaLaExistenciaYRetiraLaFila() {
        prepararEntorno();
        InventarioBodega fila = filaConStock(7);
        when(inventarioRepo.findParaActualizar(1, 5)).thenReturn(Optional.of(fila));

        MovimientoBodega movimiento = service.eliminarProducto(1, 5, "Ya no se maneja", 9);

        assertEquals(TipoMovimientoBodegaEnum.ELIMINACION, movimiento.getTipoMovimiento());
        assertEquals(7, movimiento.getCantidad());
        assertEquals(7, movimiento.getStockInicial());
        assertEquals(0, movimiento.getStockFinal());
        verify(inventarioRepo).delete(fila);
    }

    @Test
    void eliminarProductoSinExistenciaSeRegistraConCantidadCero() {
        prepararEntorno();
        InventarioBodega fila = filaConStock(0);
        when(inventarioRepo.findParaActualizar(1, 5)).thenReturn(Optional.of(fila));

        MovimientoBodega movimiento = service.eliminarProducto(1, 5, "Limpieza", 9);

        assertEquals(0, movimiento.getCantidad());
        verify(inventarioRepo).delete(fila);
    }

    @Test
    void eliminarProductoQueNoEstaEnLaBodegaLanzaNotFound() {
        prepararEntorno();
        when(inventarioRepo.findParaActualizar(1, 5)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.eliminarProducto(1, 5, "Limpieza", 9));

        verify(inventarioRepo, never()).delete(any(InventarioBodega.class));
    }

    @Test
    void eliminarProductoExigeMotivoYProducto() {
        assertThrows(BadRequestException.class, () -> service.eliminarProducto(1, 5, "", 9));
        assertThrows(BadRequestException.class, () -> service.eliminarProducto(1, null, "Motivo", 9));
    }

    // ---------- registro de movimientos (usado por despachos) ----------

    @Test
    void registrarMovimientoDeDespachoDescuentaYGuardaElDocumentoOrigen() {
        prepararEntorno();
        when(inventarioRepo.findParaActualizar(1, 5)).thenReturn(Optional.of(filaConStock(40)));

        MovimientoBodega movimiento = service.registrarMovimiento(bodega, producto,
                TipoMovimientoBodegaEnum.DESPACHO, 15, "Despacho #3", usuario, "DESPACHO_BODEGA", 3L);

        assertEquals(25, movimiento.getStockFinal());
        assertEquals("DESPACHO_BODEGA", movimiento.getTipoDocumentoOrigen());
        assertEquals(3L, movimiento.getIdDocumentoOrigen());
    }

    @Test
    void registrarMovimientoDeAnulacionSumaAunSiLaFilaYaNoExiste() {
        prepararEntorno();
        when(inventarioRepo.findParaActualizar(1, 5)).thenReturn(Optional.empty());

        MovimientoBodega movimiento = service.registrarMovimiento(bodega, producto,
                TipoMovimientoBodegaEnum.ANULACION_DESPACHO, 4, "Cancelación", usuario, "DESPACHO_BODEGA", 3L);

        assertEquals(0, movimiento.getStockInicial());
        assertEquals(4, movimiento.getStockFinal());
    }

    @Test
    void registrarMovimientoRechazaCantidadNoPositiva() {
        assertThrows(BadRequestException.class, () -> service.registrarMovimiento(bodega, producto,
                TipoMovimientoBodegaEnum.DESPACHO, 0, "x", usuario, null, null));
    }

    @Test
    void registrarMovimientoRechazaDesbordarElMaximoEntero() {
        prepararEntorno();
        when(inventarioRepo.findParaActualizar(1, 5)).thenReturn(Optional.of(filaConStock(Integer.MAX_VALUE)));

        assertThrows(BadRequestException.class, () -> service.registrarMovimiento(bodega, producto,
                TipoMovimientoBodegaEnum.INGRESO, 1, "x", usuario, null, null));
    }

    @Test
    void obtenerStockParaActualizarDevuelveCeroSiElProductoNoEstaEnLaBodega() {
        when(inventarioRepo.findParaActualizar(1, 5)).thenReturn(Optional.empty());

        assertEquals(0, service.obtenerStockParaActualizar(bodega, producto));
        verify(inventarioRepo, never()).save(any(InventarioBodega.class));
    }

    @Test
    void obtenerStockParaActualizarDevuelveLaExistenciaActual() {
        when(inventarioRepo.findParaActualizar(1, 5)).thenReturn(Optional.of(filaConStock(12)));

        assertEquals(12, service.obtenerStockParaActualizar(bodega, producto));
    }

    // ---------- consultas ----------

    @Test
    void buscarPorCodigoSinResultadoLanzaNotFound() {
        when(inventarioRepo.findDtoPorCodigo(1, "XYZ")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.findPorCodigo(1, "XYZ"));
    }

    @Test
    void movimientosSinFechasConsultaTodoElHistorico() {
        prepararEntorno();
        service.findMovimientos(1, "", "", null, null, PageRequest.of(0, 10));

        ArgumentCaptor<LocalDateTime> desde = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> hasta = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(movimientoRepo).findListado(eq(1), desde.capture(), hasta.capture(), eq(null), eq(""), any());
        assertTrue(desde.getValue().getYear() <= 2000);
        assertTrue(hasta.getValue().getYear() >= 2999);
    }

    @Test
    void movimientosConRangoIncluyeElDiaFinalCompleto() {
        prepararEntorno();
        service.findMovimientos(1, "2026-10-01", "2026-10-02", TipoMovimientoBodegaEnum.INGRESO, " cuaderno ",
                PageRequest.of(0, 10));

        verify(movimientoRepo).findListado(eq(1), eq(LocalDateTime.of(2026, 10, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 10, 3, 0, 0)), eq(TipoMovimientoBodegaEnum.INGRESO), eq("cuaderno"), any());
    }

    @Test
    void movimientosRechazaRangosIncompletosInvertidosOMalFormados() {
        prepararEntorno();
        assertThrows(BadRequestException.class,
                () -> service.findMovimientos(1, "2026-10-01", "", null, "", PageRequest.of(0, 10)));
        assertThrows(BadRequestException.class,
                () -> service.findMovimientos(1, "2026-10-05", "2026-10-01", null, "", PageRequest.of(0, 10)));
        assertThrows(BadRequestException.class,
                () -> service.findMovimientos(1, "01/10/2026", "02/10/2026", null, "", PageRequest.of(0, 10)));
    }

    // ---------- copiar inventario ----------

    @Test
    void clonarDesdeSucursalCopiaYRegistraLosMovimientosDeImportacion() {
        prepararEntorno();
        when(inventarioRepo.existsByBodega_IdBodega(1)).thenReturn(false);
        when(sucursalService.findById(2)).thenReturn(Sucursal.builder().idSucursal(2).nombre("Norte").build());
        when(inventarioRepo.clonarDesdeSucursal(1, 2)).thenReturn(120);

        int copiados = service.clonarInventario(1, OrigenInventarioBodegaEnum.SUCURSAL, 2, 9);

        assertEquals(120, copiados);
        verify(movimientoRepo).registrarImportacionMasiva(1, 9, "Inventario copiado desde la sucursal Norte");
    }

    @Test
    void clonarDesdeOtraBodegaCopiaYRegistraLosMovimientosDeImportacion() {
        prepararEntorno();
        when(inventarioRepo.existsByBodega_IdBodega(1)).thenReturn(false);
        when(bodegaService.findById(2)).thenReturn(Bodega.builder().idBodega(2).nombre("Secundaria").build());
        when(inventarioRepo.clonarDesdeBodega(1, 2)).thenReturn(5);

        assertEquals(5, service.clonarInventario(1, OrigenInventarioBodegaEnum.BODEGA, 2, 9));
        verify(movimientoRepo).registrarImportacionMasiva(1, 9, "Inventario copiado desde la bodega Secundaria");
    }

    @Test
    void clonarRechazaUnDestinoQueYaTieneInventario() {
        prepararEntorno();
        when(inventarioRepo.existsByBodega_IdBodega(1)).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> service.clonarInventario(1, OrigenInventarioBodegaEnum.SUCURSAL, 2, 9));

        verify(inventarioRepo, never()).clonarDesdeSucursal(anyInt(), anyInt());
    }

    @Test
    void clonarRechazaCopiarUnaBodegaSobreSiMisma() {
        prepararEntorno();
        when(inventarioRepo.existsByBodega_IdBodega(1)).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> service.clonarInventario(1, OrigenInventarioBodegaEnum.BODEGA, 1, 9));
    }

    @Test
    void clonarRechazaUnOrigenSinProductosYNoRegistraMovimientos() {
        prepararEntorno();
        when(inventarioRepo.existsByBodega_IdBodega(1)).thenReturn(false);
        when(sucursalService.findById(2)).thenReturn(Sucursal.builder().idSucursal(2).nombre("Norte").build());
        when(inventarioRepo.clonarDesdeSucursal(1, 2)).thenReturn(0);

        assertThrows(BadRequestException.class,
                () -> service.clonarInventario(1, OrigenInventarioBodegaEnum.SUCURSAL, 2, 9));

        verify(movimientoRepo, never()).registrarImportacionMasiva(anyInt(), anyInt(), anyString());
    }

    @Test
    void clonarExigeOrigenEIdDeOrigen() {
        assertThrows(BadRequestException.class, () -> service.clonarInventario(1, null, 2, 9));
        assertThrows(BadRequestException.class,
                () -> service.clonarInventario(1, OrigenInventarioBodegaEnum.SUCURSAL, null, 9));
    }

    // ---------- importación desde Excel ----------

    private ByteArrayInputStream excel(Object[][] filas) throws Exception {
        return excel(new String[] { "codigo_producto", "cantidad", "stock_minimo" }, filas);
    }

    private ByteArrayInputStream excel(String[] encabezados, Object[][] filas) throws Exception {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Inventario");
            Row encabezado = hoja.createRow(0);
            for (int i = 0; i < encabezados.length; i++) {
                encabezado.createCell(i).setCellValue(encabezados[i]);
            }
            for (int i = 0; i < filas.length; i++) {
                Row fila = hoja.createRow(i + 1);
                for (int j = 0; j < filas[i].length; j++) {
                    Object valor = filas[i][j];
                    if (valor instanceof Number) {
                        fila.createCell(j).setCellValue(((Number) valor).doubleValue());
                    } else if (valor != null) {
                        fila.createCell(j).setCellValue(valor.toString());
                    }
                }
            }
            libro.write(salida);
            return new ByteArrayInputStream(salida.toByteArray());
        }
    }

    private Producto productoConCodigo(int id, String codigo) {
        return Producto.builder().idProducto(id).codProducto(codigo).nombre("Producto " + id).build();
    }

    @Test
    void importarAplicaTodasLasFilasValidasSumandoALaExistenciaActual() throws Exception {
        prepararEntorno();
        Producto p1 = productoConCodigo(11, "A-1");
        Producto p2 = productoConCodigo(12, "B-2");
        when(productoRepo.findByCodProductoIn(anyCollection())).thenReturn(List.of(p2, p1));
        InventarioBodega existenteDeP1 = InventarioBodega.builder().bodega(bodega).producto(p1).stock(10).build();
        when(inventarioRepo.findParaActualizar(1, 11)).thenReturn(Optional.of(existenteDeP1));
        when(inventarioRepo.findParaActualizar(1, 12)).thenReturn(Optional.empty());

        ImportacionInventarioDto resultado = service.importarDesdeExcel(
                1, excel(new Object[][] { { "B-2", 7, 2 }, { "A-1", 5, null } }), 9, false);

        assertTrue(resultado.getErrores().isEmpty());
        assertEquals(2, resultado.getFilasLeidas());
        assertEquals(2, resultado.getProductosImportados());
        assertEquals(12, resultado.getUnidadesImportadas());
        assertEquals(15, existenteDeP1.getStock());

        ArgumentCaptor<MovimientoBodega> movimientos = ArgumentCaptor.forClass(MovimientoBodega.class);
        verify(movimientoRepo, org.mockito.Mockito.times(2)).save(movimientos.capture());
        assertTrue(movimientos.getAllValues().stream()
                .allMatch(m -> m.getTipoMovimiento() == TipoMovimientoBodegaEnum.IMPORTACION));
    }

    @Test
    void importarRegistraLosProductosConCantidadCeroSinExistenciasYSinMovimiento() throws Exception {
        prepararEntorno();
        Producto sinStock = productoConCodigo(11, "A-1");
        Producto conStock = productoConCodigo(12, "B-2");
        when(productoRepo.findByCodProductoIn(anyCollection())).thenReturn(List.of(sinStock, conStock));
        when(inventarioRepo.findParaActualizar(1, 11)).thenReturn(Optional.empty());
        when(inventarioRepo.findParaActualizar(1, 12)).thenReturn(Optional.empty());

        ImportacionInventarioDto resultado = service.importarDesdeExcel(
                1, excel(new Object[][] { { "A-1", 0, 4 }, { "B-2", 6 } }), 9, false);

        assertTrue(resultado.getErrores().isEmpty());
        assertEquals(2, resultado.getProductosImportados());
        assertEquals(6, resultado.getUnidadesImportadas());

        ArgumentCaptor<InventarioBodega> filas = ArgumentCaptor.forClass(InventarioBodega.class);
        verify(inventarioRepo, atLeastOnce()).save(filas.capture());
        InventarioBodega filaSinStock = filas.getAllValues().stream()
                .filter(fila -> fila.getProducto().getIdProducto() == 11).reduce((a, b) -> b).orElseThrow();
        assertEquals(0, filaSinStock.getStock());
        assertEquals(4, filaSinStock.getStockMinimo());

        // Solo el producto con existencias genera movimiento de importación
        ArgumentCaptor<MovimientoBodega> movimientos = ArgumentCaptor.forClass(MovimientoBodega.class);
        verify(movimientoRepo, org.mockito.Mockito.times(1)).save(movimientos.capture());
        assertEquals(12, movimientos.getValue().getProducto().getIdProducto());
    }

    @Test
    void importarEsAtomicaSiHayUnSoloErrorNoSeAplicaNingunaFila() throws Exception {
        prepararEntorno();
        when(productoRepo.findByCodProductoIn(anyCollection())).thenReturn(List.of(productoConCodigo(11, "A-1")));

        ImportacionInventarioDto resultado = service.importarDesdeExcel(
                1, excel(new Object[][] { { "A-1", 5 }, { "NO-EXISTE", 3 } }), 9, false);

        assertEquals(1, resultado.getErrores().size());
        assertTrue(resultado.getErrores().get(0).startsWith("Fila 3:"));
        assertTrue(resultado.getErrores().get(0).contains("NO-EXISTE"));
        assertEquals(0, resultado.getProductosImportados());
        verify(inventarioRepo, never()).save(any(InventarioBodega.class));
        verify(movimientoRepo, never()).save(any(MovimientoBodega.class));
    }

    @Test
    void importarReportaCodigosRepetidosCantidadesInvalidasYErroresOrdenadosPorFila() throws Exception {
        prepararEntorno();
        when(productoRepo.findByCodProductoIn(anyCollection())).thenReturn(List.of(productoConCodigo(11, "A-1")));

        ImportacionInventarioDto resultado = service.importarDesdeExcel(1, excel(new Object[][] {
                { "A-1", 5 }, { "A-1", 2 }, { "B-9", "abc" }, { "C-3", 0 } }), 9, false);

        List<String> errores = resultado.getErrores();
        assertEquals(3, errores.size());
        assertTrue(errores.get(0).startsWith("Fila 3:") && errores.get(0).contains("repetido"));
        assertTrue(errores.get(1).startsWith("Fila 4:") && errores.get(1).contains("abc"));
        assertTrue(errores.get(2).startsWith("Fila 5:"));
    }

    @Test
    void importarRechazaUnCodigoQueCorrespondeAVariosProductos() throws Exception {
        prepararEntorno();
        when(productoRepo.findByCodProductoIn(anyCollection()))
                .thenReturn(List.of(productoConCodigo(11, "DUP"), productoConCodigo(12, "DUP")));

        ImportacionInventarioDto resultado = service.importarDesdeExcel(1, excel(new Object[][] { { "DUP", 1 } }), 9, false);

        assertEquals(1, resultado.getErrores().size());
        assertTrue(resultado.getErrores().get(0).contains("2 productos distintos"));
        verify(movimientoRepo, never()).save(any(MovimientoBodega.class));
    }

    @Test
    void importarNoDistingueMayusculasEnElCodigo() throws Exception {
        prepararEntorno();
        Producto p1 = productoConCodigo(11, "ABC-1");
        when(productoRepo.findByCodProductoIn(anyCollection())).thenReturn(List.of(p1));
        when(inventarioRepo.findParaActualizar(1, 11)).thenReturn(Optional.empty());

        ImportacionInventarioDto resultado = service.importarDesdeExcel(1, excel(new Object[][] { { "abc-1", 4 } }), 9, false);

        assertTrue(resultado.getErrores().isEmpty());
        assertEquals(1, resultado.getProductosImportados());
    }

    @Test
    void importarRechazaUnArchivoQueNoEsExcel() {
        prepararEntorno();

        assertThrows(BadRequestException.class,
                () -> service.importarDesdeExcel(1, new ByteArrayInputStream("no es excel".getBytes()), 9, false));
    }

    // ---------- importar: productos que aún no existen en el catálogo ----------

    private static final String[] CON_CATALOGO = { "codigo_producto", "cantidad", "stock_minimo", "nombre", "precio_compra",
            "precio_venta", "marca", "categoria" };

    private final Sucursal central = Sucursal.builder().idSucursal(3).nombre("Central").build();
    private final MarcaProducto maped = MarcaProducto.builder().idMarcaProducto(7).marca("MAPED").build();
    private final MarcaProducto sinMarca = MarcaProducto.builder().idMarcaProducto(15).marca("SIN MARCA").build();
    private final TipoProducto block = TipoProducto.builder().idTipoProducto(6).tipoProducto("BLOCK").build();
    private final TipoProducto plastico = TipoProducto.builder().idTipoProducto(9).tipoProducto("PLASTICO").build();

    /** Ningún código existe en el catálogo; los productos que se creen reciben ids consecutivos desde 500. */
    private void prepararProductosNuevos() {
        prepararEntorno();
        bodega.setSucursal(central);
        java.util.concurrent.atomic.AtomicInteger siguiente = new java.util.concurrent.atomic.AtomicInteger(500);
        when(productoRepo.findByCodProductoIn(anyCollection())).thenReturn(List.of());
        when(marcaService.findAll()).thenReturn(List.of(maped, sinMarca));
        when(tipoService.findAll()).thenReturn(List.of(block, plastico));
        when(productoService.save(any(Producto.class))).thenAnswer(invocation -> {
            Producto creado = invocation.getArgument(0);
            creado.setIdProducto(siguiente.getAndIncrement());
            return creado;
        });
        when(inventarioRepo.findParaActualizar(anyInt(), anyInt())).thenReturn(Optional.empty());
    }

    @Test
    void unAdministradorCreaLosProductosQueNoExistenYLosImportaALaBodega() throws Exception {
        prepararProductosNuevos();

        ImportacionInventarioDto resultado = service.importarDesdeExcel(1, excel(CON_CATALOGO, new Object[][] {
                { "NUEVO-1", 5, 2, "cuaderno rayado", 8.5, 12, "MAPED", "BLOCK" } }), 9, true);

        assertTrue(resultado.getErrores().isEmpty());
        assertEquals(1, resultado.getProductosCreados());
        assertEquals(1, resultado.getProductosImportados());
        assertEquals(5, resultado.getUnidadesImportadas());

        ArgumentCaptor<Producto> creado = ArgumentCaptor.forClass(Producto.class);
        verify(productoService).save(creado.capture());
        Producto producto = creado.getValue();
        assertEquals("NUEVO-1", producto.getCodProducto());
        assertEquals("cuaderno rayado", producto.getNombre());
        assertEquals(new java.math.BigDecimal("8.50"), producto.getPrecioCompra());
        assertEquals(new java.math.BigDecimal("12.00"), producto.getPrecioVenta());
        assertEquals(41.18f, producto.getPorcentajeGanancia(), 0.001f);
        assertEquals(maped, producto.getMarcaProducto());
        assertEquals(block, producto.getTipoProducto());

        // El producto nuevo queda visible en la sucursal de la bodega y con sus existencias en la bodega
        verify(inventarioSucursalService).obtenerOCrear(central, producto);
        ArgumentCaptor<MovimientoBodega> movimiento = ArgumentCaptor.forClass(MovimientoBodega.class);
        verify(movimientoRepo).save(movimiento.capture());
        assertEquals(TipoMovimientoBodegaEnum.IMPORTACION, movimiento.getValue().getTipoMovimiento());
        assertEquals(500, movimiento.getValue().getProducto().getIdProducto());
    }

    @Test
    void sinMarcaNiCategoriaUsaLasGenericasYCreaLaCategoriaSiNoExiste() throws Exception {
        prepararProductosNuevos();
        when(tipoService.findAll()).thenReturn(List.of(block));
        when(tipoService.save(any(TipoProducto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ImportacionInventarioDto resultado = service.importarDesdeExcel(1, excel(CON_CATALOGO, new Object[][] {
                { "N-1", 3, null, "Lapicero azul", 3.25, 5 }, { "N-2", 1, null, "Regla", 2, 4 } }), 9, true);

        assertTrue(resultado.getErrores().isEmpty());
        assertEquals(2, resultado.getProductosCreados());

        ArgumentCaptor<Producto> creados = ArgumentCaptor.forClass(Producto.class);
        verify(productoService, org.mockito.Mockito.times(2)).save(creados.capture());
        for (Producto producto : creados.getAllValues()) {
            assertEquals(sinMarca, producto.getMarcaProducto());
            assertEquals("SIN CATEGORIA", producto.getTipoProducto().getTipoProducto());
        }
        // La categoría genérica se crea una sola vez aunque haya varios productos sin categoría; la marca ya existía
        ArgumentCaptor<TipoProducto> tipoCreado = ArgumentCaptor.forClass(TipoProducto.class);
        verify(tipoService).save(tipoCreado.capture());
        assertEquals(usuario, tipoCreado.getValue().getUsuario());
        verify(marcaService, never()).save(any(MarcaProducto.class));
    }

    @Test
    void marcaYCategoriaSeBuscanSinDistinguirMayusculasTildesNiEspaciosSobrantes() throws Exception {
        prepararProductosNuevos();

        service.importarDesdeExcel(1, excel(CON_CATALOGO, new Object[][] {
                { "N-1", 1, null, "Regla", 2, 4, "  maped ", "  Pl\u00E1stico " } }), 9, true);

        ArgumentCaptor<Producto> creado = ArgumentCaptor.forClass(Producto.class);
        verify(productoService).save(creado.capture());
        assertEquals(maped, creado.getValue().getMarcaProducto());
        assertEquals(plastico, creado.getValue().getTipoProducto());
    }

    @Test
    void unProductoQueYaExisteSeUsaTalCualYSusDatosDeCatalogoSeIgnoran() throws Exception {
        prepararEntorno();
        Producto existente = productoConCodigo(11, "A-1");
        when(productoRepo.findByCodProductoIn(anyCollection())).thenReturn(List.of(existente));
        when(inventarioRepo.findParaActualizar(1, 11)).thenReturn(Optional.empty());

        ImportacionInventarioDto resultado = service.importarDesdeExcel(1, excel(CON_CATALOGO, new Object[][] {
                { "A-1", 5, null, "OTRO NOMBRE", "no es precio", "tampoco", "NO EXISTE", "NI ESTA" } }), 9, true);

        assertTrue(resultado.getErrores().isEmpty());
        assertEquals(0, resultado.getProductosCreados());
        assertEquals(1, resultado.getProductosImportados());
        verify(productoService, never()).save(any(Producto.class));
        // Sin productos nuevos ni siquiera se consultan marcas y categorías
        verifyNoInteractions(marcaService, tipoService, inventarioSucursalService);
    }

    @Test
    void unUsuarioQueNoEsAdministradorNoPuedeCrearProductosNuevos() throws Exception {
        prepararProductosNuevos();

        ImportacionInventarioDto resultado = service.importarDesdeExcel(1, excel(CON_CATALOGO, new Object[][] {
                { "NUEVO-1", 5, null, "Cuaderno", 8.5, 12 } }), 9, false);

        assertEquals(1, resultado.getErrores().size());
        assertTrue(resultado.getErrores().get(0).startsWith("Fila 2:"));
        assertTrue(resultado.getErrores().get(0).contains("administrador"));
        assertEquals(0, resultado.getProductosCreados());
        verify(productoService, never()).save(any(Producto.class));
        verify(movimientoRepo, never()).save(any(MovimientoBodega.class));
    }

    @Test
    void siHayErroresEnLosDatosDeUnProductoNuevoNoSeCreaNadaNiSiquieraLasMarcasGenericas() throws Exception {
        prepararProductosNuevos();
        when(tipoService.findAll()).thenReturn(List.of(block));

        ImportacionInventarioDto resultado = service.importarDesdeExcel(1, excel(CON_CATALOGO, new Object[][] {
                { "N-1", 1, null, null, 5, 6 },
                { "N-2", 1, null, "Dos", "abc", 6 },
                { "N-3", 1, null, "Tres", 1, null },
                { "N-4", 1, null, "Cuatro", 1, 2, "NO-EXISTE", "TAMPOCO" },
                { "N-5", 1, null, "Cinco", 1, 2 } }), 9, true);

        List<String> errores = resultado.getErrores();
        assertEquals(5, errores.size());
        assertTrue(errores.get(0).startsWith("Fila 2:") && errores.get(0).contains("falta el nombre"));
        assertTrue(errores.get(1).startsWith("Fila 3:") && errores.get(1).contains("precio de compra") && errores.get(1).contains("abc"));
        assertTrue(errores.get(2).startsWith("Fila 4:") && errores.get(2).contains("falta el precio de venta"));
        assertTrue(errores.get(3).startsWith("Fila 5:") && errores.get(3).contains("la marca \"NO-EXISTE\""));
        assertTrue(errores.get(4).startsWith("Fila 5:") && errores.get(4).contains("la categor\u00EDa \"TAMPOCO\""));
        assertEquals(0, resultado.getProductosCreados());
        assertEquals(0, resultado.getProductosImportados());
        verify(productoService, never()).save(any(Producto.class));
        verify(tipoService, never()).save(any(TipoProducto.class));
        verify(marcaService, never()).save(any(MarcaProducto.class));
        verify(inventarioSucursalService, never()).obtenerOCrear(any(), any());
        verify(inventarioRepo, never()).save(any(InventarioBodega.class));
    }

    @Test
    void rechazaNombresOCodigosDemasiadoLargosYPreciosQueNoCabenEnLaColumna() throws Exception {
        prepararProductosNuevos();

        ImportacionInventarioDto resultado = service.importarDesdeExcel(1, excel(CON_CATALOGO, new Object[][] {
                { "N-1", 1, null, "x".repeat(101), 1, 2 },
                { "c".repeat(101), 1, null, "Largo", 1, 2 },
                { "N-3", 1, null, "Caro", 1, 100000000 } }), 9, true);

        assertEquals(3, resultado.getErrores().size());
        assertTrue(resultado.getErrores().get(0).contains("nombre supera"));
        assertTrue(resultado.getErrores().get(1).contains("c\u00F3digo supera"));
        assertTrue(resultado.getErrores().get(2).contains("precio de venta"));
        verify(productoService, never()).save(any(Producto.class));
    }

    @Test
    void sinSucursalEnLaBodegaUsaLaDelUsuarioYSiNoHayNingunaRechazaLaCreacion() throws Exception {
        prepararProductosNuevos();
        bodega.setSucursal(null);

        assertThrows(BadRequestException.class, () -> service.importarDesdeExcel(1, excel(CON_CATALOGO, new Object[][] {
                { "N-1", 1, null, "Regla", 2, 4 } }), 9, true));
        verify(productoService, never()).save(any(Producto.class));

        usuario.setSucursal(central);
        service.importarDesdeExcel(1, excel(CON_CATALOGO, new Object[][] { { "N-1", 1, null, "Regla", 2, 4 } }), 9, true);

        verify(inventarioSucursalService).obtenerOCrear(eq(central), any(Producto.class));
    }

    @Test
    void mezclaProductosExistentesYNuevosEnUnMismoArchivo() throws Exception {
        prepararProductosNuevos();
        Producto existente = productoConCodigo(11, "A-1");
        when(productoRepo.findByCodProductoIn(anyCollection())).thenReturn(List.of(existente));

        ImportacionInventarioDto resultado = service.importarDesdeExcel(1, excel(CON_CATALOGO, new Object[][] {
                { "A-1", 4 }, { "N-1", 6, null, "Regla", 2, 4 } }), 9, true);

        assertTrue(resultado.getErrores().isEmpty());
        assertEquals(2, resultado.getProductosImportados());
        assertEquals(1, resultado.getProductosCreados());
        assertEquals(10, resultado.getUnidadesImportadas());
        verify(movimientoRepo, org.mockito.Mockito.times(2)).save(any(MovimientoBodega.class));
    }
}
