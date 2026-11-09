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
import xyz.pangosoft.dtodo.dto.HistorialMetaComisionDto;
import xyz.pangosoft.dtodo.dto.LiquidarComisionDto;
import xyz.pangosoft.dtodo.dto.MetaComisionDto;
import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.ComisionDeshecha;
import xyz.pangosoft.dtodo.model.ComisionLiquidacion;
import xyz.pangosoft.dtodo.model.ComisionMeta;
import xyz.pangosoft.dtodo.model.ComisionMetaHistorial;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;
import xyz.pangosoft.dtodo.repository.IComisionDeshechaRepository;
import xyz.pangosoft.dtodo.repository.IComisionLiquidacionRepository;
import xyz.pangosoft.dtodo.repository.IComisionMetaHistorialRepository;
import xyz.pangosoft.dtodo.repository.IComisionMetaRepository;
import xyz.pangosoft.dtodo.repository.IDevolucionClienteRepository;
import xyz.pangosoft.dtodo.repository.IProformaEntregaRepository;
import xyz.pangosoft.dtodo.repository.IUsuarioRepository;
import xyz.pangosoft.dtodo.service.IAmbitoVendedorService;
import xyz.pangosoft.dtodo.service.IComisionService;
import xyz.pangosoft.dtodo.service.IProformaAuditoriaService;
import xyz.pangosoft.dtodo.service.IUsuarioService;

@Service
@RequiredArgsConstructor
public class ComisionServiceImpl implements IComisionService {

    static final String SITUACION_NORMAL = "NORMAL";
    static final String SITUACION_BLOQUE = "BLOQUE";
    static final String SITUACION_PENDIENTE = "PENDIENTE";
    static final int ANIO_MINIMO = 2020;
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final IComisionMetaRepository metaRepository;
    private final IComisionLiquidacionRepository liquidacionRepository;
    private final IComisionDeshechaRepository deshechaRepository;
    private final IProformaEntregaRepository entregaRepository;
    private final IDevolucionClienteRepository devolucionRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IUsuarioService usuarioService;
    private final IAmbitoVendedorService ambito;
    private final IProformaAuditoriaService auditoria;
    private final IComisionMetaHistorialRepository historialRepository;

    // ------------------------------------------------------------------------------------------ metas

    @Transactional(readOnly = true)
    @Override
    public List<MetaComisionDto> metas(int anio) {
        validarAnio(anio);
        return aDtos(metaRepository.findByAnioOrderByMontoMinimo(anio));
    }

    @Transactional
    @Override
    public List<MetaComisionDto> guardarMetas(int anio, List<MetaComisionDto> metas, String motivo) {
        validarAnio(anio);
        exigirSinLiquidar(anio);
        validarMetas(metas);
        List<MetaComisionDto> antes = aDtos(metaRepository.findByAnioOrderByMontoMinimo(anio));
        List<MetaComisionDto> despues = metas.stream()
                .map(meta -> new MetaComisionDto(meta.getMontoMinimo().setScale(2, RoundingMode.HALF_UP),
                        meta.getPorcentaje().setScale(2, RoundingMode.HALF_UP)))
                .sorted(Comparator.comparing(MetaComisionDto::getMontoMinimo)).collect(Collectors.toList());
        if (!antes.isEmpty() && textoDeMetas(antes).equals(textoDeMetas(despues))) {
            return antes;
        }
        // Con el año ya terminado y sin liquidar, cambiar las metas mueve pagos reales: se exige explicar por qué.
        String motivoLimpio = anioTerminado(anio)
                ? auditoria.motivoObligatorio(motivo, "cambiar las metas de un año que ya terminó")
                : auditoria.motivoOpcional(motivo);
        metaRepository.deleteByAnio(anio);
        metaRepository.flush();
        List<ComisionMeta> nuevas = despues.stream()
                .map(meta -> ComisionMeta.builder().anio(anio).montoMinimo(meta.getMontoMinimo()).porcentaje(meta.getPorcentaje()).build())
                .collect(Collectors.toList());
        List<MetaComisionDto> guardadas = aDtos(metaRepository.saveAll(nuevas));
        registrarCambioDeMetas(anio, antes.isEmpty() ? "CREAR" : "CAMBIAR", motivoLimpio, antes, guardadas);
        return guardadas;
    }

    @Transactional(readOnly = true)
    @Override
    public List<HistorialMetaComisionDto> historialMetas(int anio) {
        validarAnio(anio);
        return historialRepository.findByAnioOrderByFechaDescIdHistorialDesc(anio).stream()
                .map(h -> new HistorialMetaComisionDto(h.getAccion(), nombre(h.getUsuario()), h.getFecha(), h.getMotivo(),
                        metasDeTexto(h.getAntes()), metasDeTexto(h.getDespues())))
                .collect(Collectors.toList());
    }

    /** Deja constancia de un cambio de metas: usuario de la sesión, fecha, motivo y las metas de antes y de después. */
    private void registrarCambioDeMetas(int anio, String accion, String motivo, List<MetaComisionDto> antes, List<MetaComisionDto> despues) {
        Integer idUsuario = ambito.idUsuarioActual();
        if (idUsuario == null) {
            throw new ReglaNegocioException("No se pudo identificar al usuario de la sesión.");
        }
        historialRepository.save(ComisionMetaHistorial.builder().anio(anio).accion(accion).usuario(usuarioService.findById(idUsuario))
                .motivo(motivo).antes(textoDeMetas(antes)).despues(textoDeMetas(despues)).fecha(LocalDateTime.now()).build());
    }

    /** Las metas como texto {@code monto:porcentaje;monto:porcentaje}, ordenadas por monto. */
    static String textoDeMetas(List<MetaComisionDto> metas) {
        return metas.stream().sorted(Comparator.comparing(MetaComisionDto::getMontoMinimo))
                .map(meta -> meta.getMontoMinimo().setScale(2, RoundingMode.HALF_UP).toPlainString() + ":"
                        + meta.getPorcentaje().setScale(2, RoundingMode.HALF_UP).toPlainString())
                .collect(Collectors.joining(";"));
    }

    static List<MetaComisionDto> metasDeTexto(String texto) {
        List<MetaComisionDto> metas = new ArrayList<>();
        if (texto == null || texto.trim().isEmpty()) {
            return metas;
        }
        for (String par : texto.split(";")) {
            String[] partes = par.split(":");
            if (partes.length == 2) {
                metas.add(new MetaComisionDto(new BigDecimal(partes[0]), new BigDecimal(partes[1])));
            }
        }
        return metas;
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
        List<MetaComisionDto> copiadas = aDtos(metaRepository.saveAll(copias));
        registrarCambioDeMetas(hacia, "COPIAR", "Copiadas de las metas de " + desde, new ArrayList<>(), copiadas);
        return copiadas;
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
            avance.setTotalDevuelto(avance.getTotalDevuelto().add(fila.getMontoDevuelto()));
            avance.setTotalNeto(avance.getTotalNeto().add(fila.getMontoNeto()));
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
            marcarSituacion(fila, liquidacion.getVendedor(), liquidacion.getAnio());
            fila.setMontoEntregado(liquidacion.getMontoEntregado());
            BigDecimal devuelto = liquidacion.getMontoDevuelto() == null ? BigDecimal.ZERO : liquidacion.getMontoDevuelto();
            fila.setMontoDevuelto(devuelto);
            fila.setMontoNeto(neto(liquidacion.getMontoEntregado(), devuelto));
            fila.setMetaAlcanzada(liquidacion.getMontoMeta());
            fila.setPorcentaje(liquidacion.getPorcentaje());
            fila.setComision(liquidacion.getComision());
            fila.setCompletoAnio(liquidacion.isCompletoAnio());
            avance.getFilas().add(fila);
            avance.setTotalEntregado(avance.getTotalEntregado().add(liquidacion.getMontoEntregado()));
            avance.setTotalDevuelto(avance.getTotalDevuelto().add(fila.getMontoDevuelto()));
            avance.setTotalNeto(avance.getTotalNeto().add(fila.getMontoNeto()));
            avance.setTotalComision(avance.getTotalComision().add(liquidacion.getComision()));
        });
        return avance;
    }

    /** Todos los vendedores activos y quien haya entregado en el año, con su nivel y su comisión potencial. */
    private List<FilaComisionDto> filasEnVivo(int anio, List<ComisionMeta> metas) {
        List<Object[]> entregas = entregasDelPeriodo(anio);
        Map<Integer, Usuario> vendedores = new LinkedHashMap<>();
        usuarioRepository.findVendedoresActivos().forEach(vendedor -> vendedores.put(vendedor.getIdUsuario(), vendedor));
        for (Object[] entrega : entregas) {
            Integer idVendedor = (Integer) entrega[0];
            if (!vendedores.containsKey(idVendedor)) {
                vendedores.put(idVendedor, usuarioService.findById(idVendedor));
            }
        }
        Map<Integer, BigDecimal> entregado = entregadoPorVendedor(entregas, vendedores, anio);
        Map<Integer, BigDecimal> devuelto = devueltoPorVendedor(
                devolucionRepository.devueltoPorVendedorYFecha(LocalDate.of(anio - 1, 1, 1), LocalDate.of(anio + 1, 1, 1)), vendedores, anio);
        List<FilaComisionDto> filas = new ArrayList<>();
        for (Usuario vendedor : vendedores.values()) {
            if (ingresoDespuesDe(vendedor, anio)) {
                continue;
            }
            FilaComisionDto fila = new FilaComisionDto();
            fila.setIdVendedor(vendedor.getIdUsuario());
            fila.setVendedor(nombre(vendedor));
            fila.setFechaAlta(vendedor.getFechaRegistro());
            fila.setActivo(vendedor.isEnabled());
            marcarSituacion(fila, vendedor, anio);
            BigDecimal monto = entregado.getOrDefault(vendedor.getIdUsuario(), BigDecimal.ZERO);
            BigDecimal restado = devuelto.getOrDefault(vendedor.getIdUsuario(), BigDecimal.ZERO);
            fila.setMontoEntregado(monto);
            fila.setMontoDevuelto(restado);
            fila.setMontoNeto(neto(monto, restado));
            // El nivel se alcanza y la comisión se calcula sobre lo entregado menos lo devuelto.
            aplicarNivel(fila, fila.getMontoNeto(), metas);
            filas.add(fila);
        }
        filas.sort(Comparator.comparing(FilaComisionDto::getMontoNeto).reversed().thenComparing(FilaComisionDto::getVendedor));
        return filas;
    }

    // Un año de servicio son 365 días. Quien ingresa a mitad de año no cobra ese año: cuando cumple los 365 días (dentro del
    // año siguiente) se le paga, al cierre de ese año siguiente, todo lo entregado desde su ingreso.

    private static LocalDate fechaAlta(Usuario vendedor) {
        return vendedor.getFechaRegistro() == null ? null : vendedor.getFechaRegistro().toLocalDate();
    }

    /** Ingresó después de terminado el año: no participa en él. */
    static boolean ingresoDespuesDe(Usuario vendedor, int anio) {
        LocalDate alta = fechaAlta(vendedor);
        return alta != null && alta.getYear() > anio;
    }

    /** NORMAL, BLOQUE o PENDIENTE según cuándo ingresó el vendedor respecto del año que se liquida. */
    static String situacion(Usuario vendedor, int anio) {
        LocalDate alta = fechaAlta(vendedor);
        if (alta == null || !alta.isAfter(LocalDate.of(anio - 1, 1, 1))) {
            return SITUACION_NORMAL;
        }
        if (alta.getYear() == anio) {
            return alta.isAfter(LocalDate.of(anio, 1, 1)) ? SITUACION_PENDIENTE : SITUACION_NORMAL;
        }
        return SITUACION_BLOQUE;
    }

    /** Desde cuándo cuenta lo entregado para el vendedor en ese año. */
    static LocalDate periodoDesde(Usuario vendedor, int anio) {
        return SITUACION_NORMAL.equals(situacion(vendedor, anio)) ? LocalDate.of(anio, 1, 1) : fechaAlta(vendedor);
    }

    private static void marcarSituacion(FilaComisionDto fila, Usuario vendedor, int anio) {
        fila.setSituacion(situacion(vendedor, anio));
        fila.setPeriodoDesde(periodoDesde(vendedor, anio));
    }

    /** Entregado menos devuelto, nunca menos de cero. */
    static BigDecimal neto(BigDecimal entregado, BigDecimal devuelto) {
        return entregado.subtract(devuelto).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Lo devuelto por vendedor desde el inicio de su período de comisión (la misma fecha desde la que cuenta lo entregado). Una
     * devolución de un año posterior no cuenta, y una de lo entregado el año anterior resta en el año en que se devuelve.
     */
    static Map<Integer, BigDecimal> devueltoPorVendedor(List<Object[]> devoluciones, Map<Integer, Usuario> vendedores, int anio) {
        Map<Integer, BigDecimal> total = new HashMap<>();
        for (Object[] fila : devoluciones) {
            Integer idVendedor = (Integer) fila[0];
            Usuario vendedor = vendedores.get(idVendedor);
            LocalDate fecha = (LocalDate) fila[1];
            if (vendedor == null || fecha == null || fecha.isBefore(periodoDesde(vendedor, anio)) || fecha.getYear() > anio) {
                continue;
            }
            total.merge(idVendedor, ((BigDecimal) fila[2]).setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
        }
        return total;
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

    /** Las entregas que pueden contar en el año: desde el 1 de enero anterior (por los ingresos del año pasado) hasta el cierre. */
    private List<Object[]> entregasDelPeriodo(int anio) {
        return entregaRepository.entregadoDelPeriodo(TipoProformaEnum.MAYORISTA, LocalDate.of(anio - 1, 1, 1),
                LocalDate.of(anio + 1, 1, 1));
    }

    /**
     * Lo entregado por vendedor desde el inicio de su período: unidades entregadas × importe de la línea / unidades de la
     * línea.
     */
    static Map<Integer, BigDecimal> entregadoPorVendedor(List<Object[]> entregas, Map<Integer, Usuario> vendedores, int anio) {
        Map<Integer, BigDecimal> total = new HashMap<>();
        for (Object[] fila : entregas) {
            Integer idVendedor = (Integer) fila[0];
            Usuario vendedor = vendedores.get(idVendedor);
            LocalDate fecha = (LocalDate) fila[4];
            if (vendedor == null || fecha == null || fecha.isBefore(periodoDesde(vendedor, anio)) || fecha.getYear() > anio) {
                continue;
            }
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
        for (Map.Entry<Integer, Boolean> decidido : completaron.entrySet()) {
            FilaComisionDto fila = filas.stream().filter(f -> f.getIdVendedor().equals(decidido.getKey())).findFirst()
                    .orElseThrow(() -> new ReglaNegocioException("El usuario " + decidido.getKey() + " no es un vendedor de este año."));
            if (decidido.getValue() && SITUACION_PENDIENTE.equals(fila.getSituacion())) {
                throw new ReglaNegocioException(fila.getVendedor() + " ingresó en " + anio
                        + " y aún no cumple 365 días: su comisión se liquida el año siguiente.");
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
                    .montoDevuelto(fila.getMontoDevuelto())
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
                .map(l -> nombre(l.getVendedor()) + ": entregado Q" + l.getMontoEntregado() + ", devuelto Q" + l.getMontoDevuelto() + ", comisión Q" + l.getComision()
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
