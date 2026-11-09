package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;

import xyz.pangosoft.dtodo.dto.CobranzaClienteDto;
import xyz.pangosoft.dtodo.dto.CobranzaDto;
import xyz.pangosoft.dtodo.dto.MovimientoEstadoCuentaDto;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.repository.IClienteRepository;
import xyz.pangosoft.dtodo.repository.IRutaRepository;
import xyz.pangosoft.dtodo.service.ICobranzaService;

class ReporteCobranzaServiceImplTest {

    private final ICobranzaService cobranza = mock(ICobranzaService.class);
    private final IClienteRepository clientes = mock(IClienteRepository.class);
    private final ReporteCobranzaServiceImpl service = new ReporteCobranzaServiceImpl(cobranza, clientes, mock(IRutaRepository.class));

    private static String texto(byte[] xlsx) throws Exception {
        StringBuilder texto = new StringBuilder();
        DataFormatter formato = new DataFormatter();
        try (Workbook libro = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            for (Row fila : libro.getSheetAt(0)) {
                for (Cell celda : fila) {
                    texto.append(formato.formatCellValue(celda)).append('|');
                }
                texto.append('\n');
            }
        }
        return texto.toString();
    }

    private CobranzaDto conUnCliente() {
        CobranzaClienteDto fila = new CobranzaClienteDto();
        fila.setIdCliente(5);
        fila.setCliente("Libreria Uno");
        fila.setNegocio("Elizyov");
        fila.setRuta("RUTA 1");
        fila.setPorVencer(new BigDecimal("100.00"));
        fila.setVencido1a30(new BigDecimal("200.00"));
        fila.setVencido(new BigDecimal("200.00"));
        fila.setTotal(new BigDecimal("300.00"));
        fila.setDiasMaxAtraso(12);
        CobranzaDto dto = new CobranzaDto();
        dto.setFecha(LocalDate.of(2026, 11, 30));
        dto.setClientes(Collections.singletonList(fila));
        dto.setPorVencer(new BigDecimal("100.00"));
        dto.setVencido1a30(new BigDecimal("200.00"));
        dto.setVencido(new BigDecimal("200.00"));
        dto.setTotal(new BigDecimal("300.00"));
        return dto;
    }

    @Test
    void lasCuentasPorCobrarSeGeneranEnExcelYPdf() throws Exception {
        when(cobranza.cuentasPorCobrar(null, false)).thenReturn(conUnCliente());

        String excel = texto(service.cuentasPorCobrar(null, false, "XLSX"));
        byte[] pdf = service.cuentasPorCobrar(null, false, "PDF");

        assertTrue(excel.contains("Cuentas por cobrar"), excel);
        assertTrue(excel.contains("Al 30/11/2026"), excel);
        assertTrue(excel.contains("Cliente|Negocio|Ruta|Límite de crédito|Por vencer"), excel);
        assertTrue(excel.contains("Libreria Uno|Elizyov|RUTA 1"), excel);
        assertTrue(excel.contains("Q300.00"), excel);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void sinClientesMuestraElAvisoYElFiltroSoloVencidos() throws Exception {
        CobranzaDto vacio = new CobranzaDto();
        vacio.setFecha(LocalDate.of(2026, 11, 30));
        when(cobranza.cuentasPorCobrar(null, true)).thenReturn(vacio);

        String excel = texto(service.cuentasPorCobrar(null, true, "XLSX"));

        assertTrue(excel.contains("No hay registros"), excel);
        assertTrue(excel.contains("solo con saldo vencido"), excel);
    }

    @Test
    void validaFormatoYRuta() {
        assertThrows(BadRequestException.class, () -> service.cuentasPorCobrar(null, false, "CSV"));
        assertThrows(BadRequestException.class, () -> service.cuentasPorCobrar(0, false, "PDF"));
    }

    @Test
    void elEstadoDeCuentaSeImprimeConSusMovimientosYSaldo() throws Exception {
        Cliente cliente = Cliente.builder().idCliente(5).nombre("Libreria Uno").nit("4514627-6").codigoCliente("abc-123")
                .limiteCredito(new BigDecimal("5000")).diasCredito(30).build();
        when(clientes.findById(5)).thenReturn(Optional.of(cliente));
        MovimientoEstadoCuentaDto cargo = new MovimientoEstadoCuentaDto();
        cargo.setFechaTexto("05/10/2026");
        cargo.setDocumento("Pedido P-1");
        cargo.setDetalle("Entrega #10");
        cargo.setCargo(new BigDecimal("90.00"));
        cargo.setSaldo(new BigDecimal("90.00"));
        MovimientoEstadoCuentaDto abono = new MovimientoEstadoCuentaDto();
        abono.setFechaTexto("06/10/2026");
        abono.setDocumento("Recibo S2-000001");
        abono.setDetalle("Efectivo");
        abono.setAbono(new BigDecimal("30.00"));
        abono.setSaldo(new BigDecimal("60.00"));
        when(cobranza.estadoDeCuenta(5)).thenReturn(Arrays.asList(cargo, abono));

        byte[] pdf = service.estadoDeCuenta(5);

        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
        assertThrows(BadRequestException.class, () -> service.estadoDeCuenta(0));
        assertThrows(NotFoundException.class, () -> service.estadoDeCuenta(77));
    }
}
