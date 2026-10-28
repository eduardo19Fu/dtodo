package xyz.pangosoft.dtodo.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
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
import xyz.pangosoft.dtodo.dto.IndicadoresMesDto;
import xyz.pangosoft.dtodo.dto.IndicadoresMesDto.VentaVendedor;
import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;
import xyz.pangosoft.dtodo.repository.IDevolucionClienteRepository;
import xyz.pangosoft.dtodo.repository.IProformaEntregaRepository;
import xyz.pangosoft.dtodo.repository.IUsuarioRepository;
import xyz.pangosoft.dtodo.service.ICobranzaService;
import xyz.pangosoft.dtodo.service.IIndicadoresService;
import xyz.pangosoft.dtodo.service.IUsuarioService;

@Service
@RequiredArgsConstructor
public class IndicadoresServiceImpl implements IIndicadoresService {

    static final int ANIO_MINIMO = 2020;
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final IProformaEntregaRepository entregaRepository;
    private final IDevolucionClienteRepository devolucionRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IUsuarioService usuarioService;
    private final ICobranzaService cobranzaService;

    @Transactional(readOnly = true)
    @Override
    public IndicadoresMesDto indicadores(int anio, int mes) {
        validar(anio, mes);
        LocalDate desde = LocalDate.of(anio, mes, 1);
        LocalDate siguiente = desde.plusMonths(1);

        IndicadoresMesDto dto = new IndicadoresMesDto();
        dto.setAnio(anio);
        dto.setMes(mes);
        dto.setDesde(desde);
        dto.setHasta(siguiente.minusDays(1));

        Map<Integer, VentaVendedor> porVendedor = new LinkedHashMap<>();
        usuarioRepository.findVendedoresActivos().forEach(v -> porVendedor.put(v.getIdUsuario(), nueva(v)));

        BigDecimal entregadoTotal = BigDecimal.ZERO;
        for (Map.Entry<Integer, BigDecimal> entrada : entregadoPorVendedor(desde, siguiente).entrySet()) {
            VentaVendedor venta = porVendedor.computeIfAbsent(entrada.getKey(), id -> nueva(usuarioService.findById(id)));
            venta.setEntregado(entrada.getValue());
            entregadoTotal = entregadoTotal.add(entrada.getValue());
        }
        BigDecimal devueltoTotal = BigDecimal.ZERO;
        for (Object[] fila : devolucionRepository.devueltoDelPeriodoPorVendedor(desde, siguiente)) {
            Integer idVendedor = (Integer) fila[0];
            BigDecimal valor = ((BigDecimal) fila[1]).setScale(2, RoundingMode.HALF_UP);
            VentaVendedor venta = porVendedor.computeIfAbsent(idVendedor, id -> nueva(usuarioService.findById(id)));
            venta.setDevuelto(valor);
            devueltoTotal = devueltoTotal.add(valor);
        }
        dto.setTotalEntregado(entregadoTotal);
        dto.setTotalDevuelto(devueltoTotal);
        dto.setTotalNeto(entregadoTotal.subtract(devueltoTotal));

        int conFecha = 0;
        int aTiempo = 0;
        for (Object[] fila : entregaRepository.pedidosCompletadosDelPeriodo(desde, siguiente)) {
            Integer idVendedor = (Integer) fila[0];
            boolean puntual = !((LocalDate) fila[3]).isAfter((LocalDate) fila[2]);
            VentaVendedor venta = porVendedor.computeIfAbsent(idVendedor, id -> nueva(usuarioService.findById(id)));
            venta.setPedidosConFecha(venta.getPedidosConFecha() + 1);
            conFecha++;
            if (puntual) {
                venta.setPedidosATiempo(venta.getPedidosATiempo() + 1);
                aTiempo++;
            }
        }
        dto.setPedidosConFecha(conFecha);
        dto.setPedidosATiempo(aTiempo);
        dto.setPorcentajeATiempo(porcentaje(aTiempo, conFecha));

        for (VentaVendedor venta : porVendedor.values()) {
            venta.setNeto(venta.getEntregado().subtract(venta.getDevuelto()));
            venta.setPorcentajeATiempo(porcentaje(venta.getPedidosATiempo(), venta.getPedidosConFecha()));
            if (dto.getTotalNeto().signum() > 0) {
                venta.setParticipacion(venta.getNeto().multiply(CIEN).divide(dto.getTotalNeto(), 1, RoundingMode.HALF_UP));
            }
        }
        List<VentaVendedor> lista = new ArrayList<>(porVendedor.values());
        lista.sort(Comparator.comparing(VentaVendedor::getNeto).reversed().thenComparing(VentaVendedor::getVendedor));
        dto.setVendedores(lista);

        dto.setNetoMesAnterior(netoDelMes(desde.minusMonths(1), desde));
        dto.setCuentasPorCobrar(cuentasPorCobrar());
        return dto;
    }

    static void validar(int anio, int mes) {
        LocalDate hoy = LocalDate.now();
        if (anio < ANIO_MINIMO || mes < 1 || mes > 12 || LocalDate.of(anio, mes, 1).isAfter(hoy.withDayOfMonth(1))) {
            throw new ReglaNegocioException("El mes solicitado no es válido: debe ser de enero de " + ANIO_MINIMO + " al mes actual.");
        }
    }

    /** Lo entregado por vendedor en el período, a precio de línea con descuento. */
    private Map<Integer, BigDecimal> entregadoPorVendedor(LocalDate desde, LocalDate hastaExclusivo) {
        Map<Integer, List<Object[]>> filas = new HashMap<>();
        for (Object[] fila : entregaRepository.entregadoDelPeriodo(TipoProformaEnum.MAYORISTA, desde, hastaExclusivo)) {
            filas.computeIfAbsent((Integer) fila[0], id -> new ArrayList<>()).add(fila);
        }
        Map<Integer, BigDecimal> total = new LinkedHashMap<>();
        filas.forEach((idVendedor, lineas) -> total.put(idVendedor, SaldoClienteServiceImpl.valor(lineas)));
        return total;
    }

    private BigDecimal netoDelMes(LocalDate desde, LocalDate hastaExclusivo) {
        BigDecimal entregado = entregadoPorVendedor(desde, hastaExclusivo).values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal devuelto = devolucionRepository.devueltoDelPeriodoPorVendedor(desde, hastaExclusivo).stream()
                .map(f -> ((BigDecimal) f[1]).setScale(2, RoundingMode.HALF_UP)).reduce(BigDecimal.ZERO, BigDecimal::add);
        return entregado.subtract(devuelto);
    }

    private IndicadoresMesDto.CuentasPorCobrar cuentasPorCobrar() {
        CobranzaDto cobranza = cobranzaService.cuentasPorCobrar(null, false);
        IndicadoresMesDto.CuentasPorCobrar cuentas = new IndicadoresMesDto.CuentasPorCobrar();
        cuentas.setTotal(cobranza.getTotal());
        cuentas.setPorVencer(cobranza.getPorVencer());
        cuentas.setVencido(cobranza.getVencido());
        cuentas.setClientesConDeuda(cobranza.getClientes().size());
        cuentas.setClientesConVencido(cobranza.getClientesConVencido());
        return cuentas;
    }

    private static VentaVendedor nueva(Usuario usuario) {
        VentaVendedor venta = new VentaVendedor();
        venta.setIdVendedor(usuario.getIdUsuario());
        venta.setVendedor(((usuario.getPrimerNombre() == null ? "" : usuario.getPrimerNombre()) + " "
                + (usuario.getApellido() == null ? "" : usuario.getApellido())).trim());
        return venta;
    }

    /** Porcentaje a un decimal; nulo si no hay con qué compararse. */
    static BigDecimal porcentaje(int parte, int total) {
        if (total <= 0) {
            return null;
        }
        return new BigDecimal(parte).multiply(CIEN).divide(new BigDecimal(total), 1, RoundingMode.HALF_UP);
    }
}
