package xyz.pangosoft.dtodo.service.impl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import xyz.pangosoft.dtodo.dto.ChequeDto;
import xyz.pangosoft.dtodo.dto.PedidoSaldoDto;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.ReciboCaja;
import xyz.pangosoft.dtodo.model.enums.CondicionPagoEnum;
import xyz.pangosoft.dtodo.model.enums.FormaPagoReciboEnum;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.repository.IReciboCajaRepository;
import xyz.pangosoft.dtodo.repository.IRutaRepository;
import xyz.pangosoft.dtodo.service.IChequeService;
import xyz.pangosoft.dtodo.service.IReporteControlesService;
import xyz.pangosoft.dtodo.service.ISaldoClienteService;
import xyz.pangosoft.dtodo.util.ExcelTabla;
import xyz.pangosoft.dtodo.util.ExcelTabla.Columna;
import xyz.pangosoft.dtodo.util.PdfTabla;

@Service
@RequiredArgsConstructor
public class ReporteControlesServiceImpl implements IReporteControlesService {

    private static final ZoneId ZONA = ZoneId.of("America/Guatemala");
    /** Alto de fila de los listados que se llenan a mano. */
    private static final int ALTO_MANUAL = 26;
    private static final int ALTO_NORMAL = 20;

    private final IChequeService chequeService;
    private final IProformaRepository proformaRepository;
    private final IReciboCajaRepository reciboRepository;
    private final ISaldoClienteService saldoService;
    private final IRutaRepository rutaRepository;

    // ------------------------------------------------------------------------------------------ cheques

    @Transactional(readOnly = true)
    @Override
    public byte[] controlDeCheques(Integer idSucursal, String estado, String fechaInicio, String fechaFin, String formato) {
        String formatoValido = ReporteMayoristasServiceImpl.validarFormato(formato);
        LocalDate[] rango = ReporteMayoristasServiceImpl.validarRango(fechaInicio, fechaFin);
        List<ChequeDto> cheques = chequeService.todos(idSucursal, null, estado, rango[0], rango[1]);
        List<String> contexto = new ArrayList<>();
        contexto.add(periodo(rango));
        contexto.add("Estado: " + (estado == null || estado.trim().isEmpty() ? "todos" : estado.trim().toLowerCase()));
        return salida("Control de cheques", contexto, columnasCheques(), cheques, formatoValido, ALTO_NORMAL);
    }

    static List<Columna<ChequeDto>> columnasCheques() {
        List<Columna<ChequeDto>> columnas = new ArrayList<>();
        columnas.add(Columna.fecha("Fecha de pago", ChequeDto::getFechaPago));
        columnas.add(Columna.texto("Recibo", 12, ChequeDto::getNumeroRecibo));
        columnas.add(Columna.texto("Cliente", 24, ChequeDto::getCliente));
        columnas.add(Columna.texto("Banco", 14, ChequeDto::getBanco));
        columnas.add(Columna.texto("No. de cheque", 13, ChequeDto::getNumeroCheque));
        columnas.add(Columna.texto("Girador", 20, ChequeDto::getGirador));
        columnas.add(Columna.fecha("Fecha del cheque", ChequeDto::getFechaCheque));
        columnas.add(Columna.dinero("Monto", ChequeDto::getMonto, true));
        columnas.add(Columna.texto("Estado", 17, c -> c.getEstado().substring(0, 1) + c.getEstado().substring(1).toLowerCase()
                + (c.isPostfechado() ? " (postfechado)" : "")));
        columnas.add(Columna.fechaHora("Último cambio", ChequeDto::getFechaEstado));
        columnas.add(Columna.texto("Notas / motivo", 22, c -> c.getMotivoRechazo() != null ? c.getMotivoRechazo() : c.getNotas()));
        return columnas;
    }

    // ------------------------------------------------------------------------------------------ crédito

    /** Una línea del listado de despachados al crédito. */
    static final class FilaCredito {
        String cliente;
        LocalDate fechaPedido;
        String noProforma;
        BigDecimal montoTotal;
        BigDecimal abono1;
        BigDecimal abono2;
        BigDecimal abono3;
        BigDecimal saldo;
        String tipoPago;
    }

    @Transactional(readOnly = true)
    @Override
    public byte[] despachadosAlCredito(Integer idRuta, boolean soloConSaldo, String fechaInicio, String fechaFin, String formato) {
        String formatoValido = ReporteMayoristasServiceImpl.validarFormato(formato);
        ReporteMayoristasServiceImpl.validarRuta(idRuta);
        LocalDate[] rango = ReporteMayoristasServiceImpl.validarRango(fechaInicio, fechaFin);
        List<FilaCredito> filas = new ArrayList<>();
        for (Proforma pedido : proformaRepository.pedidosMayoristasEntre(desde(rango[0]), hasta(rango[1]), idRuta)) {
            if (pedido.getCondicionPago() != CondicionPagoEnum.CREDITO) {
                continue;
            }
            PedidoSaldoDto saldo = saldoService.saldoDelPedido(pedido.getIdProforma());
            if (saldo.getEntregado().signum() <= 0 || (soloConSaldo && saldo.getPendiente().signum() <= 0)) {
                continue;
            }
            filas.add(fila(pedido, saldo));
        }
        String ruta = idRuta == null ? "todas las rutas" : rutaRepository.findById(idRuta).map(r -> r.getNombre()).orElse("ruta " + idRuta);
        List<String> contexto = new ArrayList<>();
        contexto.add(periodo(rango));
        contexto.add("Ruta: " + ruta + (soloConSaldo ? " · solo con saldo pendiente" : ""));
        return salida("Listado de clientes despachados al crédito", contexto, columnasCredito(), filas, formatoValido, ALTO_MANUAL);
    }

    private FilaCredito fila(Proforma pedido, PedidoSaldoDto saldo) {
        FilaCredito fila = new FilaCredito();
        fila.cliente = pedido.getCliente() == null ? "" : pedido.getCliente().getNombre();
        fila.fechaPedido = fechaDe(pedido);
        fila.noProforma = pedido.getNoProforma();
        fila.montoTotal = saldo.getEntregado().subtract(saldo.getDevuelto()).max(BigDecimal.ZERO);
        fila.saldo = saldo.getPendiente();
        List<ReciboCaja> recibos = reciboRepository.findByProforma_IdProformaOrderByFechaRegistroAscIdReciboAsc(pedido.getIdProforma())
                .stream().filter(r -> !r.isAnulado()).collect(Collectors.toList());
        fila.abono1 = recibos.size() > 0 ? recibos.get(0).getMonto() : null;
        fila.abono2 = recibos.size() > 1 ? recibos.get(1).getMonto() : null;
        // Si hubo más de tres abonos, el último casillero los junta para que el saldo siga cuadrando.
        fila.abono3 = recibos.size() > 2 ? recibos.subList(2, recibos.size()).stream().map(ReciboCaja::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add) : null;
        Set<String> formas = new LinkedHashSet<>();
        for (ReciboCaja recibo : recibos) {
            formas.add(formaVisible(recibo.getFormaPago()));
        }
        fila.tipoPago = String.join(" / ", formas);
        return fila;
    }

    static List<Columna<FilaCredito>> columnasCredito() {
        List<Columna<FilaCredito>> columnas = new ArrayList<>();
        columnas.add(Columna.texto("Nombre cliente", 26, f -> f.cliente));
        columnas.add(Columna.fecha("Fecha pedido", f -> f.fechaPedido));
        columnas.add(Columna.texto("No. Prof.", 11, f -> f.noProforma));
        columnas.add(Columna.dinero("Monto total", f -> f.montoTotal, true));
        columnas.add(Columna.dinero("Abono 1", f -> f.abono1, true));
        columnas.add(Columna.dinero("Abono 2", f -> f.abono2, true));
        columnas.add(Columna.dinero("Abono 3", f -> f.abono3, true));
        columnas.add(Columna.dinero("Saldo pendiente", f -> f.saldo, true));
        columnas.add(Columna.texto("Tipo de pago", 14, f -> f.tipoPago));
        columnas.add(Columna.manual("Firma recibido", 16));
        return columnas;
    }

    // ------------------------------------------------------------------------------------------ proformas

    /** Una línea del listado de proformas. */
    static final class FilaProforma {
        String noProforma;
        java.time.LocalDateTime fecha;
        String cliente;
        BigDecimal total;
    }

    @Transactional(readOnly = true)
    @Override
    public byte[] listadoDeProformas(Integer idRuta, String fechaInicio, String fechaFin, String formato) {
        String formatoValido = ReporteMayoristasServiceImpl.validarFormato(formato);
        ReporteMayoristasServiceImpl.validarRuta(idRuta);
        LocalDate[] rango = ReporteMayoristasServiceImpl.validarRango(fechaInicio, fechaFin);
        List<FilaProforma> filas = new ArrayList<>();
        for (Proforma pedido : proformaRepository.pedidosMayoristasEntre(desde(rango[0]), hasta(rango[1]), idRuta)) {
            FilaProforma fila = new FilaProforma();
            fila.noProforma = pedido.getNoProforma();
            fila.fecha = pedido.getFechaEmision() == null ? null
                    : new Timestamp(pedido.getFechaEmision().getTime()).toLocalDateTime();
            fila.cliente = pedido.getCliente() == null ? "" : pedido.getCliente().getNombre();
            fila.total = pedido.getTotal();
            filas.add(fila);
        }
        filas.sort((a, b) -> b.fecha == null ? (a.fecha == null ? 0 : 1) : a.fecha == null ? -1 : b.fecha.compareTo(a.fecha));
        String ruta = idRuta == null ? "todas las rutas" : rutaRepository.findById(idRuta).map(r -> r.getNombre()).orElse("ruta " + idRuta);
        List<String> contexto = new ArrayList<>();
        contexto.add(periodo(rango));
        contexto.add("Ruta: " + ruta);
        return salida("Listado de proformas", contexto, columnasProformas(), filas, formatoValido, ALTO_MANUAL);
    }

    static List<Columna<FilaProforma>> columnasProformas() {
        List<Columna<FilaProforma>> columnas = new ArrayList<>();
        columnas.add(Columna.texto("No. Proforma", 12, f -> f.noProforma));
        columnas.add(Columna.fechaHora("Fecha", f -> f.fecha));
        columnas.add(Columna.texto("Cliente", 30, f -> f.cliente));
        columnas.add(Columna.dinero("Total", f -> f.total, true));
        columnas.add(Columna.manual("Total cajas", 8));
        columnas.add(Columna.manual("Paquetes o rollos", 9));
        columnas.add(Columna.manual("Total", 7));
        columnas.add(Columna.manual("Hora de empaque", 10));
        columnas.add(Columna.manual("Observaciones", 14));
        return columnas;
    }

    // ------------------------------------------------------------------------------------------ apoyo

    private static <T> byte[] salida(String titulo, List<String> contexto, List<Columna<T>> columnas, List<T> filas, String formato,
            int altoFila) {
        return "XLSX".equals(formato) ? ExcelTabla.generar(titulo, titulo, contexto, columnas, filas)
                : PdfTabla.generar(titulo, contexto, columnas, filas, altoFila);
    }

    private static String periodo(LocalDate[] rango) {
        return "Del " + CobranzaServiceImpl.FECHA_VISIBLE.format(rango[0]) + " al " + CobranzaServiceImpl.FECHA_VISIBLE.format(rango[1]);
    }

    private static Timestamp desde(LocalDate fecha) {
        return Timestamp.from(fecha.atStartOfDay(ZONA).toInstant());
    }

    private static Timestamp hasta(LocalDate fecha) {
        return Timestamp.from(fecha.plusDays(1).atStartOfDay(ZONA).toInstant());
    }

    private static LocalDate fechaDe(Proforma pedido) {
        return pedido.getFechaEmision() == null ? null : new Timestamp(pedido.getFechaEmision().getTime()).toLocalDateTime().toLocalDate();
    }

    private static String formaVisible(FormaPagoReciboEnum forma) {
        switch (forma) {
            case DEPOSITO:
                return "Depósito";
            case TRANSFERENCIA:
                return "Transferencia";
            case CHEQUE:
                return "Cheque";
            default:
                return "Efectivo";
        }
    }
}
