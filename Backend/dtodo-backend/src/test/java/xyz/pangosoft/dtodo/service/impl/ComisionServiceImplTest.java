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
    private final IUsuarioRepository usuarioRepository = mock(IUsuarioRepository.class);
    private final IUsuarioService usuarioService = mock(IUsuarioService.class);
    private final IAmbitoVendedorService ambito = mock(IAmbitoVendedorService.class);
    private final IProformaAuditoriaService auditoria = mock(IProformaAuditoriaService.class);
    private final ComisionServiceImpl service = new ComisionServiceImpl(metaRepository, liquidacionRepository, deshechaRepository,
            entregaRepository, usuarioRepository, usuarioService, ambito, auditoria);

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
    private static Object[] entregada(int idVendedor, int entregadas, String importeLinea, int unidadesLinea) {
        return new Object[] { idVendedor, entregadas, new BigDecimal(importeLinea), unidadesLinea };
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
        List<MetaComisionDto> guardadas = service.guardarMetas(pasado, Arrays.asList(dto("100000", "0.5"), dto("500000.456", "1.25")));

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
}
