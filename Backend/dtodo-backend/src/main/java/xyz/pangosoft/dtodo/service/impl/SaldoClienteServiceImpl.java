package xyz.pangosoft.dtodo.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import xyz.pangosoft.dtodo.dto.PedidoSaldoDto;
import xyz.pangosoft.dtodo.dto.SaldoClienteDto;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.enums.CondicionPagoEnum;
import xyz.pangosoft.dtodo.model.enums.EstadoPedidoEnum;
import xyz.pangosoft.dtodo.repository.IClienteRepository;
import xyz.pangosoft.dtodo.repository.IProformaEntregaRepository;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.repository.IReciboCajaRepository;
import xyz.pangosoft.dtodo.service.IAmbitoVendedorService;
import xyz.pangosoft.dtodo.service.ISaldoClienteService;

@Service
@RequiredArgsConstructor
public class SaldoClienteServiceImpl implements ISaldoClienteService {

    private final IClienteRepository clienteRepository;
    private final IProformaRepository proformaRepository;
    private final IProformaEntregaRepository entregaRepository;
    private final IReciboCajaRepository reciboRepository;
    private final IAmbitoVendedorService ambito;

    @Transactional(readOnly = true)
    @Override
    public SaldoClienteDto saldo(Integer idCliente) {
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new NotFoundException("El cliente solicitado no se encuentra registrado."));
        ambito.exigirClienteEnAmbito(cliente);
        return calcular(cliente, proformaRepository.pedidosVigentesDelCliente(idCliente),
                entregaRepository.entregadoDelCliente(idCliente), reciboRepository.abonosDelCliente(idCliente));
    }

    @Transactional(readOnly = true)
    @Override
    public BigDecimal exposicionDeCredito(Integer idCliente, Long idProformaExcluir) {
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new NotFoundException("El cliente solicitado no se encuentra registrado."));
        SaldoClienteDto saldo = calcular(cliente, proformaRepository.pedidosVigentesDelCliente(idCliente),
                entregaRepository.entregadoDelCliente(idCliente), reciboRepository.abonosDelCliente(idCliente));
        BigDecimal comprometido = saldo.getPedidos().stream()
                .filter(p -> p.getCondicionPago() == CondicionPagoEnum.CREDITO && !p.getIdProforma().equals(idProformaExcluir))
                .map(p -> p.getTotal().subtract(p.getEntregado()).max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return saldo.getSaldo().max(BigDecimal.ZERO).add(comprometido).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    @Override
    public PedidoSaldoDto saldoDelPedido(Long idProforma) {
        Proforma pedido = proformaRepository.findById(idProforma)
                .orElseThrow(() -> new NotFoundException("El pedido solicitado no se encuentra registrado."));
        ambito.exigirProformaPropia(pedido);
        List<Object[]> entregas = entregaRepository.entregadoDelPedido(idProforma);
        PedidoSaldoDto dto = nuevoPedido(pedido);
        dto.setEntregado(valor(entregas));
        dto.setPrimeraEntrega(primeraEntrega(entregas));
        dto.setCobrado(cobrado(idProforma));
        dto.setPendiente(dto.getEntregado().subtract(dto.getCobrado()).max(BigDecimal.ZERO));
        dto.setVencimiento(vencimiento(dto));
        return dto;
    }

    @Transactional(readOnly = true)
    @Override
    public BigDecimal valorEntregado(Long idProforma) {
        return valor(entregaRepository.entregadoDelPedido(idProforma));
    }

    @Transactional(readOnly = true)
    @Override
    public BigDecimal cobrado(Long idProforma) {
        BigDecimal cobrado = reciboRepository.cobradoDelPedido(idProforma);
        return (cobrado == null ? BigDecimal.ZERO : cobrado).setScale(2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------------------------------ cálculo

    /**
     * Arma el estado de cuenta. {@code entregas}: filas (pedido, unidades entregadas, importe de la línea, unidades de la línea,
     * fecha de entrega). {@code abonos}: filas (pedido, pedido de origen de la factura, monto).
     */
    static SaldoClienteDto calcular(Cliente cliente, List<Proforma> pedidos, List<Object[]> entregas, List<Object[]> abonos) {
        Map<Long, List<Object[]>> entregasPorPedido = new HashMap<>();
        for (Object[] fila : entregas) {
            entregasPorPedido.computeIfAbsent((Long) fila[0], id -> new ArrayList<>()).add(fila);
        }
        Map<Long, BigDecimal> cobradoPorPedido = new HashMap<>();
        BigDecimal abonado = BigDecimal.ZERO;
        BigDecimal sinPedido = BigDecimal.ZERO;
        for (Object[] fila : abonos) {
            BigDecimal monto = (BigDecimal) fila[2];
            abonado = abonado.add(monto);
            Long destino = fila[0] != null ? (Long) fila[0] : (Long) fila[1];
            if (destino == null) {
                sinPedido = sinPedido.add(monto);
            } else {
                cobradoPorPedido.merge(destino, monto, BigDecimal::add);
            }
        }

        List<PedidoSaldoDto> lista = new ArrayList<>();
        BigDecimal entregadoTotal = BigDecimal.ZERO;
        for (Proforma pedido : pedidos) {
            List<Object[]> deEstePedido = entregasPorPedido.getOrDefault(pedido.getIdProforma(), new ArrayList<>());
            PedidoSaldoDto dto = nuevoPedido(pedido);
            dto.setEntregado(valor(deEstePedido));
            dto.setPrimeraEntrega(primeraEntrega(deEstePedido));
            dto.setCobrado(cobradoPorPedido.getOrDefault(pedido.getIdProforma(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
            dto.setPendiente(dto.getEntregado().subtract(dto.getCobrado()).max(BigDecimal.ZERO));
            entregadoTotal = entregadoTotal.add(dto.getEntregado());
            lista.add(dto);
        }

        // Los abonos sin pedido pagan primero lo más antiguo: por fecha de la primera entrega y, a igualdad, por número.
        List<PedidoSaldoDto> porAntiguedad = new ArrayList<>(lista);
        porAntiguedad.sort(Comparator.comparing(PedidoSaldoDto::getPrimeraEntrega, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PedidoSaldoDto::getIdProforma));
        BigDecimal pozo = sinPedido;
        for (PedidoSaldoDto dto : porAntiguedad) {
            if (pozo.signum() <= 0) {
                break;
            }
            if (dto.getPendiente().signum() > 0) {
                BigDecimal aplicado = pozo.min(dto.getPendiente());
                dto.setAplicadoDeAbonos(aplicado);
                dto.setPendiente(dto.getPendiente().subtract(aplicado));
                pozo = pozo.subtract(aplicado);
            }
        }
        for (PedidoSaldoDto dto : lista) {
            dto.setVencimiento(vencimiento(dto));
        }

        SaldoClienteDto saldo = new SaldoClienteDto();
        saldo.setIdCliente(cliente.getIdCliente());
        saldo.setCliente(cliente.getNombre());
        saldo.setLimiteCredito(cliente.getLimiteCredito());
        saldo.setDiasCredito(cliente.getDiasCredito());
        saldo.setEntregado(entregadoTotal.setScale(2, RoundingMode.HALF_UP));
        saldo.setAbonado(abonado.setScale(2, RoundingMode.HALF_UP));
        saldo.setSaldo(saldo.getEntregado().subtract(saldo.getAbonado()));
        if (cliente.getLimiteCredito() != null) {
            saldo.setDisponible(cliente.getLimiteCredito().subtract(saldo.getSaldo().max(BigDecimal.ZERO)));
        }
        for (Proforma pedido : pedidos) {
            PedidoSaldoDto dto = lista.stream().filter(p -> p.getIdProforma().equals(pedido.getIdProforma())).findFirst().get();
            boolean abierto = pedido.getEstadoPedido() != EstadoPedidoEnum.ENTREGADA;
            if (abierto || dto.getPendiente().signum() > 0) {
                saldo.getPedidos().add(dto);
            }
        }
        return saldo;
    }

    private static PedidoSaldoDto nuevoPedido(Proforma pedido) {
        PedidoSaldoDto dto = new PedidoSaldoDto();
        dto.setIdProforma(pedido.getIdProforma());
        dto.setNoProforma(pedido.getNoProforma());
        dto.setCondicionPago(pedido.getCondicionPago() == null ? CondicionPagoEnum.CONTADO : pedido.getCondicionPago());
        dto.setDiasCredito(pedido.getDiasCredito());
        dto.setTotal(pedido.getTotal() == null ? BigDecimal.ZERO : pedido.getTotal().setScale(2, RoundingMode.HALF_UP));
        return dto;
    }

    /** Unidades entregadas × importe de la línea / unidades de la línea, sumado y redondeado a dos decimales. */
    static BigDecimal valor(List<Object[]> entregas) {
        BigDecimal total = BigDecimal.ZERO;
        for (Object[] fila : entregas) {
            int unidadesLinea = ((Number) fila[3]).intValue();
            if (unidadesLinea <= 0) {
                continue;
            }
            BigDecimal importeLinea = fila[2] == null ? BigDecimal.ZERO : (BigDecimal) fila[2];
            total = total.add(importeLinea.multiply(new BigDecimal(((Number) fila[1]).intValue()))
                    .divide(new BigDecimal(unidadesLinea), 6, RoundingMode.HALF_UP));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private static LocalDate primeraEntrega(List<Object[]> entregas) {
        return entregas.stream().map(fila -> (LocalDate) fila[4]).filter(f -> f != null).min(Comparator.naturalOrder()).orElse(null);
    }

    private static LocalDate vencimiento(PedidoSaldoDto dto) {
        if (dto.getCondicionPago() != CondicionPagoEnum.CREDITO || dto.getPrimeraEntrega() == null || dto.getDiasCredito() == null) {
            return null;
        }
        return dto.getPrimeraEntrega().plusDays(dto.getDiasCredito());
    }
}
