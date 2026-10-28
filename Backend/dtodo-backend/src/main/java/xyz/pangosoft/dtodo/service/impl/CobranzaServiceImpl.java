package xyz.pangosoft.dtodo.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import xyz.pangosoft.dtodo.dto.CobranzaClienteDto;
import xyz.pangosoft.dtodo.dto.CobranzaDto;
import xyz.pangosoft.dtodo.dto.MovimientoEstadoCuentaDto;
import xyz.pangosoft.dtodo.dto.PedidoSaldoDto;
import xyz.pangosoft.dtodo.dto.SaldoClienteDto;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.DevolucionCliente;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.ReciboCaja;
import xyz.pangosoft.dtodo.model.enums.CondicionPagoEnum;
import xyz.pangosoft.dtodo.model.enums.FormaPagoReciboEnum;
import xyz.pangosoft.dtodo.repository.IClienteRepository;
import xyz.pangosoft.dtodo.repository.IDevolucionClienteRepository;
import xyz.pangosoft.dtodo.repository.IProformaEntregaRepository;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.repository.IReciboCajaRepository;
import xyz.pangosoft.dtodo.service.ICobranzaService;

@Service
@RequiredArgsConstructor
public class CobranzaServiceImpl implements ICobranzaService {

    static final java.time.format.DateTimeFormatter FECHA_VISIBLE = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final IClienteRepository clienteRepository;
    private final IProformaRepository proformaRepository;
    private final IProformaEntregaRepository entregaRepository;
    private final IReciboCajaRepository reciboRepository;
    private final IDevolucionClienteRepository devolucionRepository;

    @Transactional(readOnly = true)
    @Override
    public CobranzaDto cuentasPorCobrar(Integer idRuta, boolean soloVencidos) {
        return cuentasPorCobrar(idRuta, soloVencidos, LocalDate.now());
    }

    @Transactional(readOnly = true)
    @Override
    public CobranzaDto cuentasPorCobrar(Integer idRuta, boolean soloVencidos, LocalDate hoy) {
        Map<Integer, List<Proforma>> pedidosPorCliente = new LinkedHashMap<>();
        for (Proforma pedido : proformaRepository.pedidosVigentes()) {
            if (pedido.getCliente() != null) {
                pedidosPorCliente.computeIfAbsent(pedido.getCliente().getIdCliente(), id -> new ArrayList<>()).add(pedido);
            }
        }
        Map<Integer, List<Object[]>> entregasPorCliente = new HashMap<>();
        for (Object[] fila : entregaRepository.entregadoDeTodos()) {
            entregasPorCliente.computeIfAbsent((Integer) fila[0], id -> new ArrayList<>())
                    .add(Arrays.copyOfRange(fila, 1, fila.length));
        }
        Map<Integer, List<Object[]>> abonosPorCliente = new HashMap<>();
        for (Object[] fila : reciboRepository.abonosDeTodos()) {
            abonosPorCliente.computeIfAbsent((Integer) fila[0], id -> new ArrayList<>())
                    .add(Arrays.copyOfRange(fila, 1, fila.length));
        }

        Map<Integer, List<Object[]>> devolucionesPorCliente = new HashMap<>();
        for (Object[] fila : devolucionRepository.devueltoDeTodos()) {
            devolucionesPorCliente.computeIfAbsent((Integer) fila[0], id -> new ArrayList<>())
                    .add(Arrays.copyOfRange(fila, 1, fila.length));
        }

        CobranzaDto cobranza = new CobranzaDto();
        cobranza.setFecha(hoy);
        for (Map.Entry<Integer, List<Proforma>> entrada : pedidosPorCliente.entrySet()) {
            Cliente cliente = entrada.getValue().get(0).getCliente();
            if (idRuta != null && (cliente.getRuta() == null || !idRuta.equals(cliente.getRuta().getIdRuta()))) {
                continue;
            }
            SaldoClienteDto saldo = SaldoClienteServiceImpl.calcular(cliente, entrada.getValue(),
                    entregasPorCliente.getOrDefault(entrada.getKey(), new ArrayList<>()),
                    abonosPorCliente.getOrDefault(entrada.getKey(), new ArrayList<>()),
                    devolucionesPorCliente.getOrDefault(entrada.getKey(), new ArrayList<>()));
            CobranzaClienteDto fila = clasificar(cliente, saldo, hoy);
            if (fila.getTotal().signum() > 0 && (!soloVencidos || fila.getVencido().signum() > 0)) {
                cobranza.getClientes().add(fila);
            }
        }
        cobranza.getClientes().sort(Comparator.comparing(CobranzaClienteDto::getVencido).reversed()
                .thenComparing(Comparator.comparing(CobranzaClienteDto::getTotal).reversed())
                .thenComparing(CobranzaClienteDto::getCliente, Comparator.nullsLast(Comparator.naturalOrder())));
        for (CobranzaClienteDto fila : cobranza.getClientes()) {
            cobranza.setPorVencer(cobranza.getPorVencer().add(fila.getPorVencer()));
            cobranza.setVencido1a30(cobranza.getVencido1a30().add(fila.getVencido1a30()));
            cobranza.setVencido31a60(cobranza.getVencido31a60().add(fila.getVencido31a60()));
            cobranza.setVencidoMasDe60(cobranza.getVencidoMasDe60().add(fila.getVencidoMasDe60()));
            cobranza.setTotal(cobranza.getTotal().add(fila.getTotal()));
            cobranza.setVencido(cobranza.getVencido().add(fila.getVencido()));
            if (fila.getVencido().signum() > 0) {
                cobranza.setClientesConVencido(cobranza.getClientesConVencido() + 1);
            }
        }
        return cobranza;
    }

    /** Reparte lo que debe cada pedido del cliente según los días que lleva vencido a la fecha de corte. */
    static CobranzaClienteDto clasificar(Cliente cliente, SaldoClienteDto saldo, LocalDate hoy) {
        CobranzaClienteDto fila = new CobranzaClienteDto();
        fila.setIdCliente(cliente.getIdCliente());
        fila.setCliente(cliente.getNombre());
        fila.setNegocio(cliente.getNombreNegocio());
        if (cliente.getRuta() != null) {
            fila.setIdRuta(cliente.getRuta().getIdRuta());
            fila.setRuta(cliente.getRuta().getNombre());
        }
        fila.setLimiteCredito(cliente.getLimiteCredito());
        for (PedidoSaldoDto pedido : saldo.getPedidos()) {
            BigDecimal pendiente = pedido.getPendiente();
            if (pendiente == null || pendiente.signum() <= 0) {
                continue;
            }
            LocalDate vence = vencimiento(pedido);
            long atraso = vence == null ? 0 : ChronoUnit.DAYS.between(vence, hoy);
            if (atraso <= 0) {
                fila.setPorVencer(fila.getPorVencer().add(pendiente));
            } else {
                if (atraso <= 30) {
                    fila.setVencido1a30(fila.getVencido1a30().add(pendiente));
                } else if (atraso <= 60) {
                    fila.setVencido31a60(fila.getVencido31a60().add(pendiente));
                } else {
                    fila.setVencidoMasDe60(fila.getVencidoMasDe60().add(pendiente));
                }
                fila.setVencido(fila.getVencido().add(pendiente));
                fila.setDiasMaxAtraso((int) Math.max(fila.getDiasMaxAtraso(), atraso));
            }
            fila.setTotal(fila.getTotal().add(pendiente));
        }
        return fila;
    }

    /** A crédito, la primera entrega más los días de crédito; al contado entregado sin cobrar, el día de la entrega. */
    static LocalDate vencimiento(PedidoSaldoDto pedido) {
        if (pedido.getCondicionPago() == CondicionPagoEnum.CREDITO && pedido.getVencimiento() != null) {
            return pedido.getVencimiento();
        }
        return pedido.getPrimeraEntrega();
    }

    // ------------------------------------------------------------------------------------------ estado de cuenta

    @Transactional(readOnly = true)
    @Override
    public List<MovimientoEstadoCuentaDto> estadoDeCuenta(Integer idCliente) {
        clienteRepository.findById(idCliente)
                .orElseThrow(() -> new NotFoundException("El cliente solicitado no se encuentra registrado."));
        return armarEstadoDeCuenta(entregaRepository.entregasDelClienteDetalle(idCliente),
                reciboRepository.findByCliente_IdClienteAndAnuladoFalseOrderByFechaPagoAscIdReciboAsc(idCliente),
                devolucionRepository.findByCliente_IdClienteOrderByFechaAscIdDevolucionAsc(idCliente));
    }

    /** Las entregas (cargos) y los recibos (abonos) por fecha; en un mismo día el cargo va antes que el abono. */
    static List<MovimientoEstadoCuentaDto> armarEstadoDeCuenta(List<Object[]> lineasEntregadas, List<ReciboCaja> recibos) {
        return armarEstadoDeCuenta(lineasEntregadas, recibos, new ArrayList<>());
    }

    /** Igual, con las devoluciones del cliente como abonos (bajan lo que debe). */
    static List<MovimientoEstadoCuentaDto> armarEstadoDeCuenta(List<Object[]> lineasEntregadas, List<ReciboCaja> recibos,
            List<DevolucionCliente> devoluciones) {
        // Una entrega puede tener varias líneas: se suman por entrega.
        Map<Long, List<Object[]>> porEntrega = new LinkedHashMap<>();
        Map<Long, Object[]> cabeceras = new HashMap<>();
        for (Object[] fila : lineasEntregadas) {
            Long idEntrega = (Long) fila[0];
            porEntrega.computeIfAbsent(idEntrega, id -> new ArrayList<>())
                    .add(new Object[] { null, fila[4], fila[5], fila[6], fila[3] });
            cabeceras.putIfAbsent(idEntrega, fila);
        }
        List<Object[]> orden = new ArrayList<>();   // {fecha, tipo (0 cargo / 1 abono), id, movimiento}
        for (Map.Entry<Long, List<Object[]>> entrega : porEntrega.entrySet()) {
            Object[] cabecera = cabeceras.get(entrega.getKey());
            MovimientoEstadoCuentaDto movimiento = new MovimientoEstadoCuentaDto();
            movimiento.setFecha((LocalDate) cabecera[3]);
            movimiento.setDocumento("Pedido " + cabecera[2]);
            movimiento.setDetalle("Entrega #" + entrega.getKey());
            movimiento.setCargo(SaldoClienteServiceImpl.valor(entrega.getValue()));
            orden.add(new Object[] { cabecera[3], 0, entrega.getKey(), movimiento });
        }
        for (ReciboCaja recibo : recibos) {
            MovimientoEstadoCuentaDto movimiento = new MovimientoEstadoCuentaDto();
            movimiento.setFecha(recibo.getFechaPago());
            movimiento.setDocumento("Recibo " + ReciboCajaServiceImpl.numeroVisible(recibo));
            movimiento.setDetalle(detalleRecibo(recibo));
            movimiento.setAbono(recibo.getMonto());
            orden.add(new Object[] { recibo.getFechaPago(), 1, recibo.getIdRecibo(), movimiento });
        }
        for (DevolucionCliente devolucion : devoluciones) {
            MovimientoEstadoCuentaDto movimiento = new MovimientoEstadoCuentaDto();
            movimiento.setFecha(devolucion.getFecha());
            movimiento.setDocumento("Devolución #" + devolucion.getIdDevolucion());
            movimiento.setDetalle(devolucion.getMotivo() == null ? "Devolución de producto"
                    : (devolucion.getMotivo().length() > 40 ? devolucion.getMotivo().substring(0, 40) + "…" : devolucion.getMotivo()));
            movimiento.setAbono(devolucion.getTotal());
            orden.add(new Object[] { devolucion.getFecha(), 1, devolucion.getIdDevolucion() + 1_000_000_000L, movimiento });
        }
        orden.sort(Comparator.comparing((Object[] o) -> (LocalDate) o[0]).thenComparing(o -> (Integer) o[1])
                .thenComparing(o -> (Long) o[2]));
        List<MovimientoEstadoCuentaDto> resultado = new ArrayList<>();
        BigDecimal corrido = BigDecimal.ZERO;
        for (Object[] o : orden) {
            MovimientoEstadoCuentaDto movimiento = (MovimientoEstadoCuentaDto) o[3];
            if (movimiento.getCargo() != null) {
                corrido = corrido.add(movimiento.getCargo());
            }
            if (movimiento.getAbono() != null) {
                corrido = corrido.subtract(movimiento.getAbono());
            }
            movimiento.setSaldo(corrido.setScale(2, RoundingMode.HALF_UP));
            movimiento.setFechaTexto(FECHA_VISIBLE.format(movimiento.getFecha()));
            resultado.add(movimiento);
        }
        return resultado;
    }

    private static String detalleRecibo(ReciboCaja recibo) {
        String texto = recibo.getFormaPago() == null ? ""
                : recibo.getFormaPago() == FormaPagoReciboEnum.DEPOSITO ? "Depósito"
                : recibo.getFormaPago() == FormaPagoReciboEnum.TRANSFERENCIA ? "Transferencia" : "Efectivo";
        if (recibo.getReferencia() != null) {
            texto += " " + recibo.getReferencia();
        }
        if (recibo.getProforma() != null) {
            texto += " · pedido " + recibo.getProforma().getNoProforma();
        }
        return texto;
    }
}
