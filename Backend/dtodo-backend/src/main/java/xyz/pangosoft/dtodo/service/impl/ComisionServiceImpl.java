package xyz.pangosoft.dtodo.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import xyz.pangosoft.dtodo.dto.AvanceComisionDto;
import xyz.pangosoft.dtodo.dto.FilaComisionDto;
import xyz.pangosoft.dtodo.dto.LiquidarComisionDto;
import xyz.pangosoft.dtodo.dto.MetaComisionDto;
import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.ComisionDeshecha;
import xyz.pangosoft.dtodo.model.ComisionLiquidacion;
import xyz.pangosoft.dtodo.model.ComisionMeta;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;
import xyz.pangosoft.dtodo.repository.IComisionDeshechaRepository;
import xyz.pangosoft.dtodo.repository.IComisionLiquidacionRepository;
import xyz.pangosoft.dtodo.repository.IComisionMetaRepository;
import xyz.pangosoft.dtodo.repository.IProformaEntregaRepository;
import xyz.pangosoft.dtodo.repository.IUsuarioRepository;
import xyz.pangosoft.dtodo.service.IAmbitoVendedorService;
import xyz.pangosoft.dtodo.service.IComisionService;
import xyz.pangosoft.dtodo.service.IProformaAuditoriaService;
import xyz.pangosoft.dtodo.service.IUsuarioService;

@Service
@RequiredArgsConstructor
public class ComisionServiceImpl implements IComisionService {

    static final int ANIO_MINIMO = 2020;
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final IComisionMetaRepository metaRepository;
    private final IComisionLiquidacionRepository liquidacionRepository;
    private final IComisionDeshechaRepository deshechaRepository;
    private final IProformaEntregaRepository entregaRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IUsuarioService usuarioService;
    private final IAmbitoVendedorService ambito;
    private final IProformaAuditoriaService auditoria;

    // ------------------------------------------------------------------------------------------ metas

    @Transactional(readOnly = true)
    @Override
    public List<MetaComisionDto> metas(int anio) {
        validarAnio(anio);
        return aDtos(metaRepository.findByAnioOrderByMontoMinimo(anio));
    }

    @Transactional
    @Override
    public List<MetaComisionDto> guardarMetas(int anio, List<MetaComisionDto> metas) {
        validarAnio(anio);
        exigirSinLiquidar(anio);
        validarMetas(metas);
        metaRepository.deleteByAnio(anio);
        metaRepository.flush();
        List<ComisionMeta> nuevas = metas.stream()
                .map(meta -> ComisionMeta.builder().anio(anio).montoMinimo(meta.getMontoMinimo().setScale(2, RoundingMode.HALF_UP))
                        .porcentaje(meta.getPorcentaje().setScale(2, RoundingMode.HALF_UP)).build())
                .collect(Collectors.toList());
        return aDtos(metaRepository.saveAll(nuevas));
    }

    @Transactional
    @Override
    public List<MetaComisionDto> copiarMetas(int desde, int hacia) {
        validarAnio(desde);
        validarAnio(hacia);
        if (desde == hacia) {
            throw new ReglaNegocioException("Elija un año distinto para copiar las metas.");
        }
        exigirSinLiquidar(hacia);
        List<ComisionMeta> origen = metaRepository.findByAnioOrderByMontoMinimo(desde);
        if (origen.isEmpty()) {
            throw new ReglaNegocioException("El año " + desde + " no tiene metas que copiar.");
        }
        if (!metaRepository.findByAnioOrderByMontoMinimo(hacia).isEmpty()) {
            throw new ReglaNegocioException("El año " + hacia + " ya tiene metas; edítelas en lugar de copiarlas.");
        }
        List<ComisionMeta> copias = origen.stream()
                .map(meta -> ComisionMeta.builder().anio(hacia).montoMinimo(meta.getMontoMinimo()).porcentaje(meta.getPorcentaje()).build())
                .collect(Collectors.toList());
        return aDtos(metaRepository.saveAll(copias));
    }

    static void validarMetas(List<MetaComisionDto> metas) {
        if (metas == null || metas.isEmpty()) {
            throw new ReglaNegocioException("Indique al menos una meta.");
        }
        BigDecimal montoAnterior = null;
        BigDecimal porcentajeAnterior = null;
        int numero = 0;
        for (MetaComisionDto meta : metas) {
            numero++;
            if (meta == null || meta.getMontoMinimo() == null || meta.getPorcentaje() == null) {
                throw new ReglaNegocioException("La meta " + numero + " debe tener monto y porcentaje.");
            }
            if (meta.getMontoMinimo().signum() <= 0) {
                throw new ReglaNegocioException("El monto de la meta " + numero + " debe ser mayor que cero.");
            }
            if (meta.getPorcentaje().signum() <= 0 || meta.getPorcentaje().compareTo(CIEN) > 0) {
                throw new ReglaNegocioException("El porcentaje de la meta " + numero + " debe ser mayor que 0 y no pasar de 100.");
            }
            if (montoAnterior != null && meta.getMontoMinimo().compareTo(montoAnterior) <= 0) {
                throw new ReglaNegocioException("Cada meta debe pedir un monto mayor que la anterior (meta " + numero + ").");
            }
            if (porcentajeAnterior != null && meta.getPorcentaje().compareTo(porcentajeAnterior) <= 0) {
                throw new ReglaNegocioException("Cada meta debe pagar un porcentaje mayor que la anterior (meta " + numero + ").");
            }
            montoAnterior = meta.getMontoMinimo();
            porcentajeAnterior = meta.getPorcentaje();
        }
    }

    // ------------------------------------------------------------------------------------------ avance

    @Transactional(readOnly = true)
    @Override
    public AvanceComisionDto avance(int anio) {
        validarAnio(anio);
        AvanceComisionDto avance = new AvanceComisionDto();
        avance.setAnio(anio);
        avance.setAnioTerminado(anioTerminado(anio));

        List<ComisionLiquidacion> liquidadas = liquidacionRepository.findByAnio(anio);
        List<ComisionMeta> metas = metaRepository.findByAnioOrderByMontoMinimo(anio);
        avance.setMetas(aDtos(metas));

        if (!liquidadas.isEmpty()) {
            return conLoLiquidado(avance, liquidadas);
        }
        for (FilaComisionDto fila : filasEnVivo(anio, metas)) {
            avance.getFilas().add(fila);
            avance.setTotalEntregado(avance.getTotalEntregado().add(fila.getMontoEntregado()));
            avance.setTotalComision(avance.getTotalComision().add(fila.getComision()));
        }
        return avance;
    }

    private AvanceComisionDto conLoLiquidado(AvanceComisionDto avance, List<ComisionLiquidacion> liquidadas) {
        avance.setLiquidado(true);
        ComisionLiquidacion primera = liquidadas.get(0);
        avance.setFechaLiquidacion(primera.getFecha());
        avance.setLiquidadoPor(nombre(primera.getUsuario()));
        liquidadas.stream().sorted(Comparator.comparing((ComisionLiquidacion l) -> nombre(l.getVendedor()))).forEach(liquidacion -> {
            FilaComisionDto fila = new FilaComisionDto();
            fila.setIdVendedor(liquidacion.getVendedor().getIdUsuario());
            fila.setVendedor(nombre(liquidacion.getVendedor()));
            fila.setFechaAlta(liquidacion.getVendedor().getFechaRegistro());
            fila.setActivo(liquidacion.getVendedor().isEnabled());
            fila.setMontoEntregado(liquidacion.getMontoEntregado());
            fila.setMetaAlcanzada(liquidacion.getMontoMeta());
            fila.setPorcentaje(liquidacion.getPorcentaje());
            fila.setComision(liquidacion.getComision());
            fila.setCompletoAnio(liquidacion.isCompletoAnio());
            avance.getFilas().add(fila);
            avance.setTotalEntregado(avance.getTotalEntregado().add(liquidacion.getMontoEntregado()));
            avance.setTotalComision(avance.getTotalComision().add(liquidacion.getComision()));
        });
        return avance;
    }

    /** Todos los vendedores activos y quien haya entregado en el año, con su nivel y su comisión potencial. */
    private List<FilaComisionDto> filasEnVivo(int anio, List<ComisionMeta> metas) {
        Map<Integer, BigDecimal> entregado = entregadoPorVendedor(anio);
        Map<Integer, Usuario> vendedores = new LinkedHashMap<>();
        usuarioRepository.findVendedoresActivos().forEach(vendedor -> vendedores.put(vendedor.getIdUsuario(), vendedor));
        for (Integer idVendedor : entregado.keySet()) {
            if (!vendedores.containsKey(idVendedor)) {
                vendedores.put(idVendedor, usuarioService.findById(idVendedor));
            }
        }
        List<FilaComisionDto> filas = new ArrayList<>();
        for (Usuario vendedor : vendedores.values()) {
            FilaComisionDto fila = new FilaComisionDto();
            fila.setIdVendedor(vendedor.getIdUsuario());
            fila.setVendedor(nombre(vendedor));
            fila.setFechaAlta(vendedor.getFechaRegistro());
            fila.setActivo(vendedor.isEnabled());
            BigDecimal monto = entregado.getOrDefault(vendedor.getIdUsuario(), BigDecimal.ZERO);
            fila.setMontoEntregado(monto);
            aplicarNivel(fila, monto, metas);
            filas.add(fila);
        }
        filas.sort(Comparator.comparing(FilaComisionDto::getMontoEntregado).reversed().thenComparing(FilaComisionDto::getVendedor));
        return filas;
    }

    /** El nivel más alto cuya meta se alcanzó; el porcentaje es plano sobre todo lo entregado. */
    static void aplicarNivel(FilaComisionDto fila, BigDecimal monto, List<ComisionMeta> metas) {
        ComisionMeta alcanzada = null;
        ComisionMeta siguiente = null;
        for (ComisionMeta meta : metas) {
            if (monto.compareTo(meta.getMontoMinimo()) >= 0) {
                alcanzada = meta;
            } else if (siguiente == null) {
                siguiente = meta;
            }
        }
        if (alcanzada != null) {
            fila.setMetaAlcanzada(alcanzada.getMontoMinimo());
            fila.setPorcentaje(alcanzada.getPorcentaje());
            fila.setComision(comision(monto, alcanzada.getPorcentaje()));
        }
        if (siguiente != null) {
            fila.setSiguienteMeta(siguiente.getMontoMinimo());
            fila.setFaltaParaSiguiente(siguiente.getMontoMinimo().subtract(monto));
        }
    }

    static BigDecimal comision(BigDecimal monto, BigDecimal porcentaje) {
        return monto.multiply(porcentaje).divide(CIEN, 2, RoundingMode.HALF_UP);
    }

    /** Lo entregado en el año por vendedor: unidades entregadas × importe de la línea / unidades de la línea. */
    Map<Integer, BigDecimal> entregadoPorVendedor(int anio) {
        Map<Integer, BigDecimal> total = new HashMap<>();
        for (Object[] fila : entregaRepository.entregadoDelPeriodo(TipoProformaEnum.MAYORISTA, LocalDate.of(anio, 1, 1),
                LocalDate.of(anio + 1, 1, 1))) {
            Integer idVendedor = (Integer) fila[0];
            BigDecimal unidadesEntregadas = new BigDecimal(((Number) fila[1]).intValue());
            BigDecimal importeLinea = fila[2] == null ? BigDecimal.ZERO : (BigDecimal) fila[2];
            int unidadesLinea = ((Number) fila[3]).intValue();
            if (unidadesLinea <= 0) {
                continue;
            }
            BigDecimal valor = importeLinea.multiply(unidadesEntregadas).divide(new BigDecimal(unidadesLinea), 6, RoundingMode.HALF_UP);
            total.merge(idVendedor, valor, BigDecimal::add);
        }
        total.replaceAll((id, valor) -> valor.setScale(2, RoundingMode.HALF_UP));
        return total;
    }

    // ------------------------------------------------------------------------------------------ liquidar

    @Transactional
    @Override
    public AvanceComisionDto liquidar(int anio, LiquidarComisionDto decision) {
        validarAnio(anio);
        if (!anioTerminado(anio)) {
            throw new ReglaNegocioException("El año " + anio + " todavía no termina: solo se liquida un año terminado.");
        }
        if (liquidacionRepository.existsByAnio(anio)) {
            throw new ReglaNegocioException("El año " + anio + " ya está liquidado.");
        }
        List<ComisionMeta> metas = metaRepository.findByAnioOrderByMontoMinimo(anio);
        if (metas.isEmpty()) {
            throw new ReglaNegocioException("Defina las metas del año " + anio + " antes de liquidarlo.");
        }
        Map<Integer, Boolean> completaron = new HashMap<>();
        if (decision != null && decision.getDecisiones() != null) {
            for (LiquidarComisionDto.Decision d : decision.getDecisiones()) {
                if (d != null && d.getIdVendedor() != null) {
                    completaron.put(d.getIdVendedor(), d.isCompletoAnio());
                }
            }
        }
        List<FilaComisionDto> filas = filasEnVivo(anio, metas);
        for (Integer idDecidido : completaron.keySet()) {
            if (filas.stream().noneMatch(fila -> fila.getIdVendedor().equals(idDecidido))) {
                throw new ReglaNegocioException("El usuario " + idDecidido + " no es un vendedor de este año.");
            }
        }

        Integer idUsuario = ambito.idUsuarioActual();
        if (idUsuario == null) {
            throw new ReglaNegocioException("No se pudo identificar al usuario de la sesión.");
        }
        Usuario quienLiquida = usuarioService.findById(idUsuario);
        LocalDateTime ahora = LocalDateTime.now();
        List<ComisionLiquidacion> guardar = new ArrayList<>();
        for (FilaComisionDto fila : filas) {
            boolean completo = completaron.getOrDefault(fila.getIdVendedor(), false);
            guardar.add(ComisionLiquidacion.builder()
                    .anio(anio)
                    .vendedor(usuarioService.findById(fila.getIdVendedor()))
                    .montoEntregado(fila.getMontoEntregado())
                    .montoMeta(completo ? fila.getMetaAlcanzada() : null)
                    .porcentaje(completo ? fila.getPorcentaje() : BigDecimal.ZERO)
                    .comision(completo ? fila.getComision() : BigDecimal.ZERO)
                    .completoAnio(completo)
                    .usuario(quienLiquida)
                    .fecha(ahora)
                    .build());
        }
        liquidacionRepository.saveAll(guardar);
        return avance(anio);
    }

    @Transactional
    @Override
    public AvanceComisionDto deshacerLiquidacion(int anio, String motivo) {
        validarAnio(anio);
        String motivoLimpio = auditoria.motivoObligatorio(motivo, "deshacer la liquidación");
        List<ComisionLiquidacion> liquidadas = liquidacionRepository.findByAnio(anio);
        if (liquidadas.isEmpty()) {
            throw new ReglaNegocioException("El año " + anio + " no está liquidado.");
        }
        Integer idUsuario = ambito.idUsuarioActual();
        if (idUsuario == null) {
            throw new ReglaNegocioException("No se pudo identificar al usuario de la sesión.");
        }
        String resumen = liquidadas.stream()
                .map(l -> nombre(l.getVendedor()) + ": entregado Q" + l.getMontoEntregado() + ", comisión Q" + l.getComision()
                        + (l.isCompletoAnio() ? "" : " (no completó el año)"))
                .collect(Collectors.joining("; "));
        deshechaRepository.save(ComisionDeshecha.builder().anio(anio).usuario(usuarioService.findById(idUsuario)).motivo(motivoLimpio)
                .resumen(resumen.length() > 4000 ? resumen.substring(0, 4000) : resumen).fecha(LocalDateTime.now()).build());
        liquidacionRepository.deleteByAnio(anio);
        liquidacionRepository.flush();
        return avance(anio);
    }

    // ------------------------------------------------------------------------------------------ apoyo

    static void validarAnio(int anio) {
        if (anio < ANIO_MINIMO || anio > LocalDate.now().getYear() + 1) {
            throw new ReglaNegocioException("El año " + anio + " no es válido.");
        }
    }

    static boolean anioTerminado(int anio) {
        return anio < LocalDate.now().getYear();
    }

    private void exigirSinLiquidar(int anio) {
        if (liquidacionRepository.existsByAnio(anio)) {
            throw new ReglaNegocioException("El año " + anio + " ya está liquidado; deshaga la liquidación para cambiar sus metas.");
        }
    }

    private static List<MetaComisionDto> aDtos(List<ComisionMeta> metas) {
        return metas.stream().sorted(Comparator.comparing(ComisionMeta::getMontoMinimo))
                .map(meta -> new MetaComisionDto(meta.getMontoMinimo(), meta.getPorcentaje())).collect(Collectors.toList());
    }

    private static String nombre(Usuario usuario) {
        if (usuario == null) {
            return "";
        }
        return ((usuario.getPrimerNombre() == null ? "" : usuario.getPrimerNombre()) + " "
                + (usuario.getApellido() == null ? "" : usuario.getApellido())).trim();
    }
}
