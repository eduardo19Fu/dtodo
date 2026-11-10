package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import xyz.pangosoft.dtodo.dto.ChequeDto;
import xyz.pangosoft.dtodo.dto.PedidoSaldoDto;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.ReciboCaja;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.enums.CondicionPagoEnum;
import xyz.pangosoft.dtodo.model.enums.FormaPagoReciboEnum;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.repository.IReciboCajaRepository;
import xyz.pangosoft.dtodo.repository.IRutaRepository;
import xyz.pangosoft.dtodo.service.IChequeService;
import xyz.pangosoft.dtodo.service.ISaldoClienteService;

class ReporteControlesServiceImplTest {

    private final IChequeService cheques = mock(IChequeService.class);
    private final IProformaRepository proformas = mock(IProformaRepository.class);
    private final IReciboCajaRepository recibos = mock(IReciboCajaRepository.class);
    private final ISaldoClienteService saldos = mock(ISaldoClienteService.class);
    private final IRutaRepository rutas = mock(IRutaRepository.class);
    private final ReporteControlesServiceImpl service = new ReporteControlesServiceImpl(cheques, proformas, recibos, saldos, rutas);

    private static String texto(byte[] xlsx) throws Exception {
        StringBuilder texto = new StringBuilder();
        DataFormatter formato = new DataFormatter();
        try (Workbook libro = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            Sheet hoja = libro.getSheetAt(0);
            for (Row fila : hoja) {
                for (Cell celda : fila) {
                    texto.append(formato.formatCellValue(celda)).append('|');
                }
                texto.append('\n');
            }
        }
        return texto.toString();
    }

    private static void guardar(String nombre, byte[] pdf) throws Exception {
        String carpeta = System.getProperty("controles.salida");
        if (carpeta != null) {
            Files.write(new File(carpeta, nombre).toPath(), pdf);
        }
    }

    private ChequeDto cheque(String estado, boolean postfechado) {
        ChequeDto dto = new ChequeDto();
        dto.setNumeroRecibo("S2-000015");
        dto.setCliente("LIBRERIA SINAI");
        dto.setBanco("Banrural");
        dto.setNumeroCheque("0045123");
        dto.setGirador("Nestor Lemus");
        dto.setFechaPago(LocalDate.of(2026, 11, 9));
        dto.setFechaCheque(LocalDate.of(2026, 11, 20));
        dto.setMonto(new BigDecimal("1250.50"));
        dto.setEstado(estado);
        dto.setPostfechado(postfechado);
        dto.setFechaEstado(LocalDateTime.of(2026, 11, 9, 10, 0));
        return dto;
    }

    @Test
    void elControlDeChequesSaleEnExcelComoTablaYEnPdf() throws Exception {
        when(cheques.todos(any(), any(), any(), any(), any())).thenReturn(Arrays.asList(cheque("RECIBIDO", true), cheque("COBRADO", false)));

        String excel = texto(service.controlDeCheques(null, null, "2026-11-01", "2026-11-30", "XLSX"));
        byte[] pdf = service.controlDeCheques(null, null, "2026-11-01", "2026-11-30", "PDF");
        guardar("cheques.pdf", pdf);

        assertTrue(excel.contains("Control de cheques"), excel);
        assertTrue(excel.contains("Del 01/11/2026 al 30/11/2026"), excel);
        assertTrue(excel.contains("Fecha de pago|Recibo|Cliente|Banco|No. de cheque|Girador"), excel);
        assertTrue(excel.contains("Banrural"), excel);
        assertTrue(excel.contains("Recibido (postfechado)"), excel);
        assertTrue(excel.contains("Cobrado"), excel);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    private Proforma pedido(long id, String no, CondicionPagoEnum condicion, String total) {
        Proforma p = new Proforma();
        p.setIdProforma(id);
        p.setNoProforma(no);
        p.setCondicionPago(condicion);
        p.setTotal(new BigDecimal(total));
        p.setFechaEmision(new Date());
        p.setCliente(Cliente.builder().idCliente((int) id).nombre("LIBRERIA " + id).build());
        return p;
    }

    private PedidoSaldoDto saldo(String entregado, String pendiente) {
        PedidoSaldoDto dto = new PedidoSaldoDto();
        dto.setEntregado(new BigDecimal(entregado));
        dto.setPendiente(new BigDecimal(pendiente));
        return dto;
    }

    private ReciboCaja abono(String monto, FormaPagoReciboEnum forma, boolean anulado) {
        return ReciboCaja.builder().monto(new BigDecimal(monto)).formaPago(forma).anulado(anulado)
                .sucursal(Sucursal.builder().idSucursal(2).build()).build();
    }

    @Test
    void despachadosAlCreditoMuestraAbonosSaldoYTipoDePagoYOmiteLoQueNoAplica() throws Exception {
        Proforma conAbonos = pedido(1, "P-1", CondicionPagoEnum.CREDITO, "1000.00");
        Proforma pagado = pedido(2, "P-2", CondicionPagoEnum.CREDITO, "500.00");
        Proforma contado = pedido(3, "P-3", CondicionPagoEnum.CONTADO, "300.00");
        Proforma sinEntrega = pedido(4, "P-4", CondicionPagoEnum.CREDITO, "300.00");
        when(proformas.pedidosMayoristasEntre(any(), any(), any())).thenReturn(Arrays.asList(conAbonos, pagado, contado, sinEntrega));
        when(saldos.saldoDelPedido(1L)).thenReturn(saldo("1000.00", "250.00"));
        when(saldos.saldoDelPedido(2L)).thenReturn(saldo("500.00", "0.00"));
        when(saldos.saldoDelPedido(4L)).thenReturn(saldo("0.00", "0.00"));
        when(recibos.findByProforma_IdProformaOrderByFechaRegistroAscIdReciboAsc(1L)).thenReturn(Arrays.asList(
                abono("300.00", FormaPagoReciboEnum.EFECTIVO, false), abono("100.00", FormaPagoReciboEnum.DEPOSITO, false),
                abono("50.00", FormaPagoReciboEnum.CHEQUE, false), abono("300.00", FormaPagoReciboEnum.CHEQUE, true),
                abono("300.00", FormaPagoReciboEnum.EFECTIVO, true)));
        when(recibos.findByProforma_IdProformaOrderByFechaRegistroAscIdReciboAsc(2L)).thenReturn(Collections.emptyList());

        String excel = texto(service.despachadosAlCredito(null, true, "2026-11-01", "2026-11-30", "XLSX"));
        guardar("credito.pdf", service.despachadosAlCredito(null, true, "2026-11-01", "2026-11-30", "PDF"));

        assertTrue(excel.contains("Nombre cliente|Fecha pedido|No. Prof.|Monto total|Abono 1|Abono 2|Abono 3|Saldo pendiente|Tipo de pago|Firma recibido"), excel);
        assertTrue(excel.contains("LIBRERIA 1|"), excel);
        assertTrue(excel.contains("Q1,000.00|Q300.00|Q100.00|Q50.00|Q250.00|Efectivo / Depósito / Cheque"), excel);
        assertTrue(!excel.contains("LIBRERIA 2"), "el pedido ya pagado se omite cuando solo se piden los que tienen saldo");
        assertTrue(!excel.contains("LIBRERIA 3") && !excel.contains("LIBRERIA 4"), excel);

        String todos = texto(service.despachadosAlCredito(null, false, "2026-11-01", "2026-11-30", "XLSX"));
        assertTrue(todos.contains("LIBRERIA 2"), todos);
    }

    @Test
    void elListadoDeProformasTraeLasColumnasParaLlenarAMano() throws Exception {
        when(proformas.pedidosMayoristasEntre(any(), any(), eq(null))).thenReturn(Arrays.asList(
                pedido(1, "11798097P", CondicionPagoEnum.CREDITO, "243.77"), pedido(2, "94284901P", CondicionPagoEnum.CONTADO, "409.63")));

        String excel = texto(service.listadoDeProformas(null, "2026-09-04", "2026-09-10", "XLSX"));
        byte[] pdf = service.listadoDeProformas(null, "2026-09-04", "2026-09-10", "PDF");
        guardar("proformas.pdf", pdf);

        assertTrue(excel.contains("No. Proforma|Fecha|Cliente|Total|Total cajas|Paquetes o rollos|Total|Hora de empaque|Observaciones"), excel);
        assertTrue(excel.contains("11798097P"), excel);
        assertTrue(excel.contains("Q409.63"), excel);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void validaFormatoRutaYPeriodo() {
        assertThrows(BadRequestException.class, () -> service.listadoDeProformas(null, "2026-09-04", "2026-09-10", "CSV"));
        assertThrows(BadRequestException.class, () -> service.listadoDeProformas(0, "2026-09-04", "2026-09-10", "PDF"));
        assertThrows(BadRequestException.class, () -> service.despachadosAlCredito(null, true, "2026-09-10", "2026-09-04", "PDF"));
        assertThrows(BadRequestException.class, () -> service.controlDeCheques(null, null, null, null, "PDF"));
    }
}
