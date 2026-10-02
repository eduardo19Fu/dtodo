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
import xyz.pangosoft.dtodo.model.MovimientoBodega;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.OrigenInventarioBodegaEnum;
import xyz.pangosoft.dtodo.model.enums.TipoMovimientoBodegaEnum;
import xyz.pangosoft.dtodo.repository.IInventarioBodegaRepository;
import xyz.pangosoft.dtodo.repository.IMovimientoBodegaRepository;
import xyz.pangosoft.dtodo.repository.IProductoRepository;
import xyz.pangosoft.dtodo.service.IBodegaService;
import xyz.pangosoft.dtodo.service.IProductoService;
import xyz.pangosoft.dtodo.service.ISucursalService;
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

    private final InventarioBodegaServiceImpl service = new InventarioBodegaServiceImpl(
            inventarioRepo, movimientoRepo, productoRepo, bodegaService, productoService, usuarioService,
            sucursalService);

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
    void clonarRechazaUnOrigenSinExistenciasYNoRegistraMovimientos() {
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
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Inventario");
            Row encabezado = hoja.createRow(0);
            encabezado.createCell(0).setCellValue("codigo_producto");
            encabezado.createCell(1).setCellValue("cantidad");
            encabezado.createCell(2).setCellValue("stock_minimo");
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
                1, excel(new Object[][] { { "B-2", 7, 2 }, { "A-1", 5, null } }), 9);

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
    void importarEsAtomicaSiHayUnSoloErrorNoSeAplicaNingunaFila() throws Exception {
        prepararEntorno();
        when(productoRepo.findByCodProductoIn(anyCollection())).thenReturn(List.of(productoConCodigo(11, "A-1")));

        ImportacionInventarioDto resultado = service.importarDesdeExcel(
                1, excel(new Object[][] { { "A-1", 5 }, { "NO-EXISTE", 3 } }), 9);

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
                { "A-1", 5 }, { "A-1", 2 }, { "B-9", "abc" }, { "C-3", 0 } }), 9);

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

        ImportacionInventarioDto resultado = service.importarDesdeExcel(1, excel(new Object[][] { { "DUP", 1 } }), 9);

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

        ImportacionInventarioDto resultado = service.importarDesdeExcel(1, excel(new Object[][] { { "abc-1", 4 } }), 9);

        assertTrue(resultado.getErrores().isEmpty());
        assertEquals(1, resultado.getProductosImportados());
    }

    @Test
    void importarRechazaUnArchivoQueNoEsExcel() {
        prepararEntorno();

        assertThrows(BadRequestException.class,
                () -> service.importarDesdeExcel(1, new ByteArrayInputStream("no es excel".getBytes()), 9));
    }
}
