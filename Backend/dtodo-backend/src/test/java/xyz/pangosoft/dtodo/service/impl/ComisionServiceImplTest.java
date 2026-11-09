package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
import xyz.pangosoft.dtodo.service.IProformaAuditoriaService;
import xyz.pangosoft.dtodo.service.IUsuarioService;

class ComisionServiceImplTest {

    private final IComisionMetaRepository metaRepository = mock(IComisionMetaRepository.class);
    private final IComisionLiquidacionRepository liquidacionRepository = mock(IComisionLiquidacionRepository.class);
    private final IComisionDeshechaRepository deshechaRepository = mock(IComisionDeshechaRepository.class);
    private final IProformaEntregaRepository entregaRepository = mock(IProformaEntregaRepository.class);
    private final xyz.pangosoft.dtodo.repository.IDevolucionClienteRepository devolucionRepository =
            mock(xyz.pangosoft.dtodo.repository.IDevolucionClienteRepository.class);
    private final IUsuarioRepository usuarioRepository = mock(IUsuarioRepository.class);
    private final IUsuarioService usuarioService = mock(IUsuarioService.class);
    private final IAmbitoVendedorService ambito = mock(IAmbitoVendedorService.class);
    private final IProformaAuditoriaService auditoria = mock(IProformaAuditoriaService.class);
    private final xyz.pangosoft.dtodo.repository.IComisionMetaHistorialRepository historialRepository =
            mock(xyz.pangosoft.dtodo.repository.IComisionMetaHistorialRepository.class);
    private final ComisionServiceImpl service = new ComisionServiceImpl(metaRepository, liquidacionRepository, deshechaRepository,
            entregaRepository, devolucionRepository, usuarioRepository, usuarioService, ambito, auditoria, historialRepository);

    private final int pasado = LocalDate.now().getYear() - 1;
    private final Usuario ana = Usuario.builder().idUsuario(1).primerNombre("Ana").apellido("Lopez").enabled(true)
            .fechaRegistro(LocalDateTime.of(2020, 3, 1, 8, 0)).build();
    private final Usuario luis = Usuario.builder().idUsuario(2).primerNombre("Luis").apellido("Perez").enabled(true).build();
    private final Usuario irene = Usuario.builder().idUsuario(9).primerNombre("Irene").apellido("Ortega").enabled(true).build();

    @BeforeEach
    void escenario() {
        when(metaRepository.findByAnioOrderByMontoMinimo(anyInt())).thenReturn(Collections.emptyList());
        when(metaRepository.findByAnioOrderByMontoMinimo(pasado)).thenReturn(metasActuales(pasado));
        when(usuarioRepository.findVendedoresActivos()).thenReturn(Arrays.asList(ana, luis));
        when(usuarioService.findById(1)).thenReturn(ana);
        when(usuarioService.findById(2)).thenReturn(luis);
        when(usuarioService.findById(9)).thenReturn(irene);
        when(ambito.idUsuarioActual()).thenReturn(9);
        when(auditoria.motivoObligatorio(any(), any())).thenAnswer(inv -> {
            String motivo = inv.getArgument(0);
            if (motivo == null || motivo.trim().isEmpty()) {
                throw new ReglaNegocioException("Indique el motivo");
            }
            return motivo.trim();
        });
        when(metaRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditoria.motivoOpcional(any())).thenAnswer(inv -> {
            String motivo = inv.getArgument(0);
            return motivo == null || motivo.trim().isEmpty() ? null : motivo.trim();
        });
    }

    private static List<ComisionMeta> metasActuales(int anio) {
        return Arrays.asList(meta(anio, "300000.00", "0.50"), meta(anio, "700000.00", "1.00"), meta(anio, "1000000.00", "1.50"),
                meta(anio, "2000000.00", "2.00"));
    }

    private static ComisionMeta meta(int anio, String monto, String porcentaje) {
        return ComisionMeta.builder().anio(anio).montoMinimo(new BigDecimal(monto)).porcentaje(new BigDecimal(porcentaje)).build();
    }

    private static MetaComisionDto dto(String monto, String porcentaje) {
        return new MetaComisionDto(new BigDecimal(monto), new BigDecimal(porcentaje));
    }

    /** Una línea entregada: vendedor, unidades entregadas, importe de la línea y unidades de la línea. */
    private Object[] entregada(int idVendedor, int entregadas, String importeLinea, int unidadesLinea) {
        return entregadaEl(LocalDate.of(pasado, 6, 15), idVendedor, entregadas, importeLinea, unidadesLinea);
    }

    private static Object[] entregadaEl(LocalDate fecha, int idVendedor, int entregadas, String importeLinea, int unidadesLinea) {
        return new Object[] { idVendedor, entregadas, new BigDecimal(importeLinea), unidadesLinea, fecha };
    }

    private void entregado(Object[]... filas) {
        when(entregaRepository.entregadoDelPeriodo(eq(TipoProformaEnum.MAYORISTA), any(), any())).thenReturn(Arrays.asList(filas));
    }

    private static FilaComisionDto fila(AvanceComisionDto avance, int idVendedor) {
        return avance.getFilas().stream().filter(f -> f.getIdVendedor() == idVendedor).findFirst().get();
    }

    // ------------------------------------------------------------------------------------------ metas

    @Test
    void unaMetaValidaSeGuardaConDosDecimalesReemplazandoLasDelAnio() {
        List<MetaComisionDto> guardadas = service.guardarMetas(pasado, Arrays.asList(dto("100000", "0.5"), dto("500000.456", "1.25")), "ajuste del esquema");

        verify(metaRepository).deleteByAnio(pasado);
        assertEquals(2, guardadas.size());
        assertEquals(new BigDecimal("500000.46"), guardadas.get(1).getMontoMinimo());
        assertEquals(new BigDecimal("1.25"), guardadas.get(1).getPorcentaje());
    }

    @Test
    void lasMetasMalDefinidasSeRechazan() {
        assertThrows(ReglaNegocioException.class, () -> service.guardarMetas(pasado, Collections.emptyList()));
        assertThrows(ReglaNegocioException.class, () -> service.guardarMetas(pasado, null));
        assertThrows(ReglaNegocioException.class, () -> service.guardarMetas(pasado, Arrays.asList(dto("0", "1"))));
        assertThrows(ReglaNegocioException.class, () -> service.guardarMetas(pasado, Arrays.asList(dto("100", "0"))));
        assertThrows(ReglaNegocioException.class, () -> service.guardarMetas(pasado, Arrays.asList(dto("100", "100.01"))));
        assertThrows(ReglaNegocioException.class, () -> service.guardarMetas(pasado, Arrays.asList(dto("100", "1"), dto("100", "2"))));
        assertThrows(ReglaNegocioException.class, () -> service.guardarMetas(pasado, Arrays.asList(dto("100", "2"), dto("200", "2"))));
        assertThrows(ReglaNegocioException.class, () -> service.guardarMetas(pasado, Arrays.asList(dto("200", "1"), dto("100", "2"))));
        assertThrows(ReglaNegocioException.class,
                () -> service.guardarMetas(pasado, Arrays.asList(new MetaComisionDto(null, new BigDecimal("1")))));
        verify(metaRepository, never()).deleteByAnio(anyInt());
    }

    @Test
    void enUnAnioLiquidadoLasMetasNoSeEditan() {
        when(liquidacionRepository.existsByAnio(pasado)).thenReturn(true);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.guardarMetas(pasado, Arrays.asList(dto("100", "1"))));

        assertTrue(error.getMessage().contains("ya está liquidado"));
        assertThrows(ReglaNegocioException.class, () -> service.copiarMetas(pasado - 1, pasado));
    }

    @Test
    void copiarMetasPideUnOrigenConMetasYUnDestinoVacio() {
        List<MetaComisionDto> copiadas = service.copiarMetas(pasado, pasado + 1);
        assertEquals(4, copiadas.size());

        assertThrows(ReglaNegocioException.class, () -> service.copiarMetas(pasado, pasado));
        assertThrows(ReglaNegocioException.class, () -> service.copiarMetas(pasado - 1, pasado + 1));
        when(metaRepository.findByAnioOrderByMontoMinimo(pasado + 1)).thenReturn(metasActuales(pasado + 1));
        assertThrows(ReglaNegocioException.class, () -> service.copiarMetas(pasado, pasado + 1));
    }

    @Test
    void losAniosFueraDeRangoSeRechazan() {
        assertThrows(ReglaNegocioException.class, () -> service.metas(2019));
        assertThrows(ReglaNegocioException.class, () -> service.avance(LocalDate.now().getYear() + 2));
        assertEquals(4, service.metas(pasado).size());
    }

    // ------------------------------------------------------------------------------------------ cálculo

    @Test
    void laComisionEsUnPorcentajePlanoSobreTodoLoEntregadoSegunElNivelAlcanzado() {
        entregado(entregada(1, 750000, "750000.00", 750000));

        FilaComisionDto fila = fila(service.avance(pasado), 1);

        assertEquals(new BigDecimal("750000.00"), fila.getMontoEntregado());
        assertEquals(new BigDecimal("700000.00"), fila.getMetaAlcanzada());
        assertEquals(new BigDecimal("1.00"), fila.getPorcentaje());
        assertEquals(new BigDecimal("7500.00"), fila.getComision());
        assertEquals(new BigDecimal("1000000.00"), fila.getSiguienteMeta());
        assertEquals(new BigDecimal("250000.00"), fila.getFaltaParaSiguiente());
    }

    @Test
    void laMetaSeAlcanzaExactamenteEnSuMonto() {
        entregado(entregada(1, 1, "300000.00", 1), entregada(2, 1, "299999.99", 1));

        AvanceComisionDto avance = service.avance(pasado);

        assertEquals(new BigDecimal("0.50"), fila(avance, 1).getPorcentaje());
        assertEquals(new BigDecimal("1500.00"), fila(avance, 1).getComision());
        assertNull(fila(avance, 2).getMetaAlcanzada());
        assertEquals(new BigDecimal("0.00"), fila(avance, 2).getComision().setScale(2));
        assertEquals(new BigDecimal("0.01"), fila(avance, 2).getFaltaParaSiguiente());
    }

    @Test
    void enLaMetaMasAltaNoHaySiguiente() {
        entregado(entregada(1, 1, "2500000.00", 1));

        FilaComisionDto fila = fila(service.avance(pasado), 1);

        assertEquals(new BigDecimal("2.00"), fila.getPorcentaje());
        assertEquals(new BigDecimal("50000.00"), fila.getComision());
        assertNull(fila.getSiguienteMeta());
        assertNull(fila.getFaltaParaSiguiente());
    }

    @Test
    void unaEntregaParcialSumaSoloLoEntregadoAlPrecioDeLaLinea() {
        // Línea de 10 unidades por Q100: entregó 4 en una entrega y 3 en otra.
        entregado(entregada(1, 4, "100.00", 10), entregada(1, 3, "100.00", 10));

        assertEquals(new BigDecimal("70.00"), fila(service.avance(pasado), 1).getMontoEntregado());
    }

    @Test
    void elRedondeoSeHaceAlSumarNoPorCadaLinea() {
        // 1 unidad de una línea de 3 por Q10 vale 3.333333; tres líneas suman Q10.00.
        entregado(entregada(1, 1, "10.00", 3), entregada(1, 1, "10.00", 3), entregada(1, 1, "10.00", 3));

        assertEquals(new BigDecimal("10.00"), fila(service.avance(pasado), 1).getMontoEntregado());
    }

    @Test
    void unaLineaSinUnidadesNoSeCuentaNiRompe() {
        entregado(entregada(1, 5, "100.00", 0), entregada(1, 2, "50.00", 2));

        assertEquals(new BigDecimal("50.00"), fila(service.avance(pasado), 1).getMontoEntregado());
    }

    @Test
    void elAvanceTraeATodoVendedorActivoAunSinEntregasYAlQueEntregoSinSerloYa() {
        Usuario exVendedor = Usuario.builder().idUsuario(5).primerNombre("Marta").apellido("Gomez").enabled(false).build();
        when(usuarioService.findById(5)).thenReturn(exVendedor);
        entregado(entregada(1, 1, "500.00", 1), entregada(5, 1, "900.00", 1));

        AvanceComisionDto avance = service.avance(pasado);

        assertEquals(3, avance.getFilas().size());
        assertEquals("Marta Gomez", avance.getFilas().get(0).getVendedor());
        assertFalse(avance.getFilas().get(0).isActivo());
        assertEquals(new BigDecimal("0"), fila(avance, 2).getMontoEntregado());
        assertEquals(new BigDecimal("1400.00"), avance.getTotalEntregado());
        assertTrue(avance.isAnioTerminado());
        assertFalse(avance.isLiquidado());
        assertEquals(4, avance.getMetas().size());
    }

    @Test
    void elAnioEnCursoNoEstaTerminado() {
        entregado();

        AvanceComisionDto avance = service.avance(LocalDate.now().getYear());

        assertFalse(avance.isAnioTerminado());
        assertTrue(avance.getMetas().isEmpty());
    }

    // ------------------------------------------------------------------------------------------ ingreso a mitad de año

    private Usuario ingresoEl(int idUsuario, LocalDate alta) {
        Usuario usuario = Usuario.builder().idUsuario(idUsuario).primerNombre("Nuevo" + idUsuario).apellido("Vendedor").enabled(true)
                .fechaRegistro(alta.atTime(9, 0)).build();
        when(usuarioService.findById(idUsuario)).thenReturn(usuario);
        when(usuarioRepository.findVendedoresActivos()).thenReturn(Arrays.asList(ana, usuario));
        return usuario;
    }

    @Test
    void quienIngresoElAnioAnteriorCobraEnUnBloqueTodoLoEntregadoDesdeSuIngreso() {
        ingresoEl(3, LocalDate.of(pasado - 1, 7, 1));
        entregado(entregadaEl(LocalDate.of(pasado - 1, 3, 1), 3, 1, "1000.00", 1),   // antes de ingresar
                entregadaEl(LocalDate.of(pasado - 1, 9, 1), 3, 1, "200.00", 1),
                entregadaEl(LocalDate.of(pasado, 3, 1), 3, 1, "300.00", 1),
                entregadaEl(LocalDate.of(pasado + 1, 1, 5), 3, 1, "9999.00", 1));  // ya del año siguiente

        FilaComisionDto fila = fila(service.avance(pasado), 3);

        assertEquals("BLOQUE", fila.getSituacion());
        assertEquals(LocalDate.of(pasado - 1, 7, 1), fila.getPeriodoDesde());
        assertEquals(new BigDecimal("500.00"), fila.getMontoEntregado());
    }

    @Test
    void quienIngresoEsteAnioQuedaPendienteYNoSePuedeLiquidar() {
        ingresoEl(3, LocalDate.of(pasado, 6, 1));
        entregado(entregadaEl(LocalDate.of(pasado, 3, 1), 3, 1, "1000.00", 1), entregadaEl(LocalDate.of(pasado, 8, 1), 3, 1, "400.00", 1));

        FilaComisionDto fila = fila(service.avance(pasado), 3);

        assertEquals("PENDIENTE", fila.getSituacion());
        assertEquals(new BigDecimal("400.00"), fila.getMontoEntregado());
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> service.liquidar(pasado, decision(3)));
        assertTrue(error.getMessage().contains("365 días"));
        verify(liquidacionRepository, never()).saveAll(any());
    }

    @Test
    void elPendienteSiPuedeQuedarSinMarcarAlLiquidar() {
        ingresoEl(3, LocalDate.of(pasado, 6, 1));
        entregado(entregadaEl(LocalDate.of(pasado, 8, 1), 3, 1, "400.00", 1));

        service.liquidar(pasado, decision(1));

        assertEquals(2, liquidadas().size());
    }

    @Test
    void quienIngresoElPrimeroDeEneroOAntesDelAnioAnteriorEsNormal() {
        Usuario enero = ingresoEl(3, LocalDate.of(pasado, 1, 1));
        assertEquals("NORMAL", ComisionServiceImpl.situacion(enero, pasado));

        Usuario hace2 = ingresoEl(4, LocalDate.of(pasado - 1, 1, 1));
        entregado(entregadaEl(LocalDate.of(pasado - 1, 5, 1), 4, 1, "700.00", 1), entregadaEl(LocalDate.of(pasado, 5, 1), 4, 1, "50.00", 1));

        assertEquals("NORMAL", ComisionServiceImpl.situacion(hace2, pasado));
        assertEquals(new BigDecimal("50.00"), fila(service.avance(pasado), 4).getMontoEntregado());
    }

    @Test
    void quienIngresaDespuesDelAnioNoApareceEnEl() {
        ingresoEl(3, LocalDate.of(pasado + 1, 2, 1));
        entregado();

        assertTrue(service.avance(pasado).getFilas().stream().noneMatch(f -> f.getIdVendedor() == 3));
    }

    @Test
    void sinFechaDeAltaSeTrataComoDeAnioCompleto() {
        Usuario sinAlta = Usuario.builder().idUsuario(3).primerNombre("Sin").apellido("Alta").enabled(true).build();

        assertEquals("NORMAL", ComisionServiceImpl.situacion(sinAlta, pasado));
        assertEquals(LocalDate.of(pasado, 1, 1), ComisionServiceImpl.periodoDesde(sinAlta, pasado));
    }

    // ------------------------------------------------------------------------------------------ liquidar

    private LiquidarComisionDto decision(Integer... completaron) {
        LiquidarComisionDto decision = new LiquidarComisionDto();
        for (Integer idVendedor : completaron) {
            LiquidarComisionDto.Decision d = new LiquidarComisionDto.Decision();
            d.setIdVendedor(idVendedor);
            d.setCompletoAnio(true);
            decision.getDecisiones().add(d);
        }
        return decision;
    }

    @SuppressWarnings("unchecked")
    private List<ComisionLiquidacion> liquidadas() {
        ArgumentCaptor<List<ComisionLiquidacion>> captor = ArgumentCaptor.forClass(List.class);
        verify(liquidacionRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    void liquidarGuardaLaComisionSoloDeQuienCompletoElAnioYDejaEnCeroALosDemas() {
        entregado(entregada(1, 1, "800000.00", 1), entregada(2, 1, "1200000.00", 1));

        service.liquidar(pasado, decision(1));

        List<ComisionLiquidacion> guardadas = liquidadas();
        assertEquals(2, guardadas.size());
        ComisionLiquidacion deLuis = guardadas.get(0);
        ComisionLiquidacion deAna = guardadas.get(1);
        assertEquals("Luis", deLuis.getVendedor().getPrimerNombre());
        assertFalse(deLuis.isCompletoAnio());
        assertEquals(new BigDecimal("1200000.00"), deLuis.getMontoEntregado());
        assertEquals(BigDecimal.ZERO, deLuis.getComision());
        assertNull(deLuis.getMontoMeta());
        assertTrue(deAna.isCompletoAnio());
        assertEquals(new BigDecimal("8000.00"), deAna.getComision());
        assertEquals(new BigDecimal("700000.00"), deAna.getMontoMeta());
        assertEquals(new BigDecimal("1.00"), deAna.getPorcentaje());
        assertEquals(Integer.valueOf(9), deAna.getUsuario().getIdUsuario());
        assertTrue(deAna.getFecha() != null);
    }

    @Test
    void sinNingunaDecisionNadieCobra() {
        entregado(entregada(1, 1, "800000.00", 1));

        service.liquidar(pasado, null);

        assertTrue(liquidadas().stream().noneMatch(ComisionLiquidacion::isCompletoAnio));
        assertTrue(liquidadas().stream().allMatch(l -> l.getComision().signum() == 0));
    }

    @Test
    void soloSeLiquidaUnAnioTerminadoConMetasYSinLiquidarYAUnVendedorDeEseAnio() {
        entregado();
        int actual = LocalDate.now().getYear();
        when(metaRepository.findByAnioOrderByMontoMinimo(actual)).thenReturn(metasActuales(actual));
        ReglaNegocioException enCurso = assertThrows(ReglaNegocioException.class, () -> service.liquidar(actual, decision()));
        assertTrue(enCurso.getMessage().contains("todavía no termina"));

        assertThrows(ReglaNegocioException.class, () -> service.liquidar(pasado - 1, decision()));

        assertThrows(ReglaNegocioException.class, () -> service.liquidar(pasado, decision(77)));

        when(liquidacionRepository.existsByAnio(pasado)).thenReturn(true);
        ReglaNegocioException ya = assertThrows(ReglaNegocioException.class, () -> service.liquidar(pasado, decision()));
        assertTrue(ya.getMessage().contains("ya está liquidado"));
        verify(liquidacionRepository, never()).saveAll(any());
    }

    @Test
    void sinUsuarioDeSesionNoSeLiquida() {
        entregado();
        when(ambito.idUsuarioActual()).thenReturn(null);

        assertThrows(ReglaNegocioException.class, () -> service.liquidar(pasado, decision()));

        verify(liquidacionRepository, never()).saveAll(any());
    }

    @Test
    void unAnioLiquidadoMuestraLoGuardadoSinRecalcular() {
        ComisionLiquidacion guardada = ComisionLiquidacion.builder().anio(pasado).vendedor(ana).montoEntregado(new BigDecimal("800000.00"))
                .montoMeta(new BigDecimal("700000.00")).porcentaje(new BigDecimal("1.00")).comision(new BigDecimal("8000.00"))
                .completoAnio(true).usuario(irene).fecha(LocalDateTime.of(2027, 1, 5, 9, 0)).build();
        when(liquidacionRepository.findByAnio(pasado)).thenReturn(Collections.singletonList(guardada));

        AvanceComisionDto avance = service.avance(pasado);

        assertTrue(avance.isLiquidado());
        assertEquals("Irene Ortega", avance.getLiquidadoPor());
        assertEquals(1, avance.getFilas().size());
        assertEquals(new BigDecimal("8000.00"), avance.getFilas().get(0).getComision());
        assertEquals(Boolean.TRUE, avance.getFilas().get(0).getCompletoAnio());
        assertEquals(new BigDecimal("8000.00"), avance.getTotalComision());
        verify(entregaRepository, never()).entregadoDelPeriodo(any(), any(), any());
    }

    // ------------------------------------------------------------------------------------------ deshacer

    @Test
    void deshacerPideMotivoDejaConstanciaYBorraLoLiquidado() {
        ComisionLiquidacion guardada = ComisionLiquidacion.builder().anio(pasado).vendedor(ana).montoEntregado(new BigDecimal("800000.00"))
                .comision(new BigDecimal("8000.00")).completoAnio(true).usuario(irene).fecha(LocalDateTime.now()).build();
        when(liquidacionRepository.findByAnio(pasado)).thenReturn(Collections.singletonList(guardada))
                .thenReturn(new ArrayList<>());
        entregado();

        assertThrows(ReglaNegocioException.class, () -> service.deshacerLiquidacion(pasado, " "));
        verify(liquidacionRepository, never()).deleteByAnio(anyInt());

        service.deshacerLiquidacion(pasado, "  se marcó mal a un vendedor ");

        ArgumentCaptor<ComisionDeshecha> captor = ArgumentCaptor.forClass(ComisionDeshecha.class);
        verify(deshechaRepository).save(captor.capture());
        assertEquals("se marcó mal a un vendedor", captor.getValue().getMotivo());
        assertTrue(captor.getValue().getResumen().contains("Ana Lopez"));
        assertTrue(captor.getValue().getResumen().contains("8000.00"));
        verify(liquidacionRepository).deleteByAnio(pasado);
    }

    @Test
    void noSeDeshaceUnAnioQueNoEstaLiquidado() {
        assertThrows(ReglaNegocioException.class, () -> service.deshacerLiquidacion(pasado, "motivo"));

        verify(deshechaRepository, never()).save(any());
    }

    // ------------------------------------------------------------------------------------------ netas de devoluciones

    private static Object[] devueltoEl(LocalDate fecha, int idVendedor, String valor) {
        return new Object[] { idVendedor, fecha, new BigDecimal(valor) };
    }

    private void devuelto(Object[]... filas) {
        when(devolucionRepository.devueltoPorVendedorYFecha(any(), any())).thenReturn(new ArrayList<>(Arrays.asList(filas)));
    }

    @Test
    void laComisionSeCalculaSobreLoEntregadoMenosLoDevuelto() {
        // Entregó Q750,000 (nivel de 700k = 1 %) y le devolvieron Q100,000: la base es 650,000 (nivel de 300k = 0.5 %).
        entregado(entregada(1, 1, "750000.00", 1));
        devuelto(devueltoEl(LocalDate.of(pasado, 8, 1), 1, "100000.00"));

        FilaComisionDto fila = fila(service.avance(pasado), 1);

        assertEquals(new BigDecimal("750000.00"), fila.getMontoEntregado());
        assertEquals(new BigDecimal("100000.00"), fila.getMontoDevuelto());
        assertEquals(new BigDecimal("650000.00"), fila.getMontoNeto());
        assertEquals(new BigDecimal("300000.00"), fila.getMetaAlcanzada());
        assertEquals(new BigDecimal("0.50"), fila.getPorcentaje());
        assertEquals(new BigDecimal("3250.00"), fila.getComision());
        assertEquals(new BigDecimal("50000.00"), fila.getFaltaParaSiguiente());
    }

    @Test
    void laBaseNuncaEsNegativaYLosTotalesSumanLasTresColumnas() {
        entregado(entregada(1, 1, "500.00", 1), entregada(2, 1, "1000.00", 1));
        devuelto(devueltoEl(LocalDate.of(pasado, 3, 1), 1, "800.00"), devueltoEl(LocalDate.of(pasado, 4, 1), 2, "250.00"));

        AvanceComisionDto avance = service.avance(pasado);

        assertEquals(new BigDecimal("0.00"), fila(avance, 1).getMontoNeto());
        assertEquals(new BigDecimal("0.00"), fila(avance, 1).getComision().setScale(2));
        assertEquals(new BigDecimal("750.00"), fila(avance, 2).getMontoNeto());
        assertEquals(new BigDecimal("1500.00"), avance.getTotalEntregado());
        assertEquals(new BigDecimal("1050.00"), avance.getTotalDevuelto());
        assertEquals(new BigDecimal("750.00"), avance.getTotalNeto());
    }

    @Test
    void soloRestanLasDevolucionesDelPeriodoDelVendedor() {
        Usuario nuevo = Usuario.builder().idUsuario(3).primerNombre("Nuevo").apellido("Vendedor").enabled(true)
                .fechaRegistro(LocalDate.of(pasado - 1, 7, 1).atTime(9, 0)).build();
        when(usuarioService.findById(3)).thenReturn(nuevo);
        when(usuarioRepository.findVendedoresActivos()).thenReturn(Arrays.asList(ana, nuevo));
        entregado(entregadaEl(LocalDate.of(pasado, 3, 1), 3, 1, "1000.00", 1), entregada(1, 1, "1000.00", 1));
        devuelto(devueltoEl(LocalDate.of(pasado - 1, 3, 1), 3, "700.00"),   // antes de su ingreso: no resta
                devueltoEl(LocalDate.of(pasado - 1, 9, 1), 3, "100.00"),   // desde su ingreso: resta
                devueltoEl(LocalDate.of(pasado, 5, 1), 3, "50.00"),
                devueltoEl(LocalDate.of(pasado + 1, 1, 5), 3, "999.00"),   // de un año posterior: no resta
                devueltoEl(LocalDate.of(pasado - 1, 6, 1), 1, "300.00"),   // un vendedor de año completo: el año anterior no resta
                devueltoEl(LocalDate.of(pasado, 6, 1), 1, "20.00"));

        AvanceComisionDto avance = service.avance(pasado);

        assertEquals(new BigDecimal("150.00"), fila(avance, 3).getMontoDevuelto());
        assertEquals(new BigDecimal("850.00"), fila(avance, 3).getMontoNeto());
        assertEquals(new BigDecimal("20.00"), fila(avance, 1).getMontoDevuelto());
        assertEquals(new BigDecimal("980.00"), fila(avance, 1).getMontoNeto());
    }

    @Test
    void liquidarGuardaLoDevueltoYElAnioLiquidadoNoSeRecalcula() {
        entregado(entregada(1, 1, "800000.00", 1));
        devuelto(devueltoEl(LocalDate.of(pasado, 8, 1), 1, "150000.00"));

        service.liquidar(pasado, decision(1));

        ComisionLiquidacion deAna = liquidadas().stream().filter(l -> l.getVendedor().getIdUsuario() == 1).findFirst().get();
        assertEquals(new BigDecimal("800000.00"), deAna.getMontoEntregado());
        assertEquals(new BigDecimal("150000.00"), deAna.getMontoDevuelto());
        assertEquals(new BigDecimal("300000.00"), deAna.getMontoMeta());
        assertEquals(new BigDecimal("3250.00"), deAna.getComision());      // 650,000 x 0.5 %
    }

    @Test
    void unAnioLiquidadoMuestraLoDevueltoGuardadoYLasLiquidacionesAnterioresTraenCero() {
        ComisionLiquidacion conDevolucion = ComisionLiquidacion.builder().anio(pasado).vendedor(ana)
                .montoEntregado(new BigDecimal("800000.00")).montoDevuelto(new BigDecimal("150000.00")).montoMeta(new BigDecimal("300000.00"))
                .porcentaje(new BigDecimal("0.50")).comision(new BigDecimal("3250.00")).completoAnio(true).usuario(irene)
                .fecha(LocalDateTime.of(2027, 1, 5, 9, 0)).build();
        ComisionLiquidacion anterior = ComisionLiquidacion.builder().anio(pasado).vendedor(luis).montoEntregado(new BigDecimal("1000.00"))
                .comision(BigDecimal.ZERO).completoAnio(false).usuario(irene).fecha(LocalDateTime.of(2027, 1, 5, 9, 0)).build();
        when(liquidacionRepository.findByAnio(pasado)).thenReturn(Arrays.asList(conDevolucion, anterior));

        AvanceComisionDto avance = service.avance(pasado);

        assertEquals(new BigDecimal("650000.00"), fila(avance, 1).getMontoNeto());
        assertEquals(new BigDecimal("150000.00"), fila(avance, 1).getMontoDevuelto());
        assertEquals(new BigDecimal("1000.00"), fila(avance, 2).getMontoNeto());
        assertEquals(new BigDecimal("0"), fila(avance, 2).getMontoDevuelto());
        assertEquals(new BigDecimal("651000.00"), avance.getTotalNeto());
    }

    // ------------------------------------------------------------------------------------------ historial de metas

    @Test
    void cambiarLasMetasDejaConstanciaDeQuienCuandoPorQueYComoEranAntesYDespues() {
        service.guardarMetas(pasado, Arrays.asList(dto("100000", "0.5"), dto("500000", "1.25")), "  ajuste del esquema ");

        ArgumentCaptor<xyz.pangosoft.dtodo.model.ComisionMetaHistorial> captor =
                ArgumentCaptor.forClass(xyz.pangosoft.dtodo.model.ComisionMetaHistorial.class);
        verify(historialRepository).save(captor.capture());
        xyz.pangosoft.dtodo.model.ComisionMetaHistorial registro = captor.getValue();
        assertEquals("CAMBIAR", registro.getAccion());
        assertEquals(irene, registro.getUsuario());
        assertEquals("ajuste del esquema", registro.getMotivo());
        assertEquals("300000.00:0.50;700000.00:1.00;1000000.00:1.50;2000000.00:2.00", registro.getAntes());
        assertEquals("100000.00:0.50;500000.00:1.25", registro.getDespues());
        assertTrue(registro.getFecha() != null);
    }

    @Test
    void conElAnioTerminadoYSinLiquidarElMotivoEsObligatorioYNoSeCambiaNada() {
        assertThrows(ReglaNegocioException.class, () -> service.guardarMetas(pasado, Arrays.asList(dto("100000", "0.5")), "  "));
        assertThrows(ReglaNegocioException.class, () -> service.guardarMetas(pasado, Arrays.asList(dto("100000", "0.5"))));

        verify(metaRepository, never()).deleteByAnio(anyInt());
        verify(historialRepository, never()).save(any());
    }

    @Test
    void duranteElAnioEnCursoElMotivoEsOpcional() {
        int actual = LocalDate.now().getYear();
        service.guardarMetas(actual, Arrays.asList(dto("100000", "0.5")), null);

        ArgumentCaptor<xyz.pangosoft.dtodo.model.ComisionMetaHistorial> captor =
                ArgumentCaptor.forClass(xyz.pangosoft.dtodo.model.ComisionMetaHistorial.class);
        verify(historialRepository).save(captor.capture());
        assertEquals("CREAR", captor.getValue().getAccion());
        assertNull(captor.getValue().getMotivo());
        assertEquals("", captor.getValue().getAntes());
    }

    @Test
    void siLasMetasNoCambianNoSeGuardaNiSeRegistraNada() {
        List<MetaComisionDto> iguales = Arrays.asList(dto("300000", "0.5"), dto("700000", "1"), dto("1000000", "1.5"), dto("2000000", "2"));

        List<MetaComisionDto> resultado = service.guardarMetas(pasado, iguales, "sin cambios");

        assertEquals(4, resultado.size());
        verify(metaRepository, never()).deleteByAnio(anyInt());
        verify(historialRepository, never()).save(any());
    }

    @Test
    void copiarMetasTambienQuedaEnElHistorial() {
        int destino = LocalDate.now().getYear();
        when(metaRepository.findByAnioOrderByMontoMinimo(destino)).thenReturn(Collections.emptyList());

        service.copiarMetas(pasado, destino);

        ArgumentCaptor<xyz.pangosoft.dtodo.model.ComisionMetaHistorial> captor =
                ArgumentCaptor.forClass(xyz.pangosoft.dtodo.model.ComisionMetaHistorial.class);
        verify(historialRepository).save(captor.capture());
        assertEquals("COPIAR", captor.getValue().getAccion());
        assertEquals(destino, captor.getValue().getAnio().intValue());
        assertTrue(captor.getValue().getMotivo().contains(String.valueOf(pasado)));
    }

    @Test
    void elHistorialSeDevuelveConLasMetasDeAntesYDespuesYElNombreDeQuienLasCambio() {
        xyz.pangosoft.dtodo.model.ComisionMetaHistorial cambio = xyz.pangosoft.dtodo.model.ComisionMetaHistorial.builder()
                .anio(pasado).accion("CAMBIAR").usuario(irene).motivo("ajuste").antes("100.00:1.00").despues("100.00:1.00;200.00:2.00")
                .fecha(LocalDateTime.of(2026, 11, 9, 10, 0)).build();
        when(historialRepository.findByAnioOrderByFechaDescIdHistorialDesc(pasado)).thenReturn(Arrays.asList(cambio));

        List<xyz.pangosoft.dtodo.dto.HistorialMetaComisionDto> historial = service.historialMetas(pasado);

        assertEquals(1, historial.size());
        assertEquals("Irene Ortega", historial.get(0).getUsuario());
        assertEquals(1, historial.get(0).getAntes().size());
        assertEquals(2, historial.get(0).getDespues().size());
        assertEquals(new BigDecimal("2.00"), historial.get(0).getDespues().get(1).getPorcentaje());
    }
}
