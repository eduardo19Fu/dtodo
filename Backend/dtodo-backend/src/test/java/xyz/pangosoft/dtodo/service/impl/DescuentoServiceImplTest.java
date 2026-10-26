package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import xyz.pangosoft.dtodo.dto.DescuentoProductoDto;
import xyz.pangosoft.dtodo.dto.GuardarDescuentosProductoRequest;
import xyz.pangosoft.dtodo.dto.OpcionDescuentoDto;
import xyz.pangosoft.dtodo.dto.OpcionesDescuentoDto;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.DescuentoProducto;
import xyz.pangosoft.dtodo.model.DetalleProforma;
import xyz.pangosoft.dtodo.model.NivelDescuentoCategoria;
import xyz.pangosoft.dtodo.model.PrecioMayorista;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.enums.CategoriaClienteEnum;
import xyz.pangosoft.dtodo.model.enums.TipoClienteEnum;
import xyz.pangosoft.dtodo.model.enums.TipoDescuentoEnum;
import xyz.pangosoft.dtodo.repository.IClienteRepository;
import xyz.pangosoft.dtodo.repository.IDescuentoProductoRepository;
import xyz.pangosoft.dtodo.repository.INivelDescuentoCategoriaRepository;
import xyz.pangosoft.dtodo.repository.IPrecioMayoristaRepository;
import xyz.pangosoft.dtodo.repository.IProductoRepository;

class DescuentoServiceImplTest {

    private final INivelDescuentoCategoriaRepository nivelRepository = mock(INivelDescuentoCategoriaRepository.class);
    private final IDescuentoProductoRepository descuentoRepository = mock(IDescuentoProductoRepository.class);
    private final IPrecioMayoristaRepository precioRepository = mock(IPrecioMayoristaRepository.class);
    private final IProductoRepository productoRepository = mock(IProductoRepository.class);
    private final IClienteRepository clienteRepository = mock(IClienteRepository.class);
    private DescuentoServiceImpl service;

    private final Producto resma = Producto.builder().idProducto(1).nombre("RESMA CARTA").codProducto("R1")
            .precioVenta(new BigDecimal("50.00")).build();

    @BeforeEach
    void preparar() {
        service = new DescuentoServiceImpl(nivelRepository, descuentoRepository, precioRepository, productoRepository,
                clienteRepository);
        when(productoRepository.findById(1)).thenReturn(Optional.of(resma));
        when(precioRepository.findById(1)).thenReturn(Optional.empty());
        when(descuentoRepository.findByProducto_IdProductoOrderByTipo(1)).thenReturn(new ArrayList<>());
    }

    // ---------- datos de apoyo ----------

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }

    private void niveles(CategoriaClienteEnum categoria, String... porcentajes) {
        List<NivelDescuentoCategoria> niveles = new ArrayList<>();
        for (String porcentaje : porcentajes) {
            niveles.add(NivelDescuentoCategoria.builder().categoria(categoria).porcentaje(bd(porcentaje)).activo(true).build());
        }
        when(nivelRepository.findByCategoriaAndActivoTrueOrderByPorcentajeAsc(categoria)).thenReturn(niveles);
    }

    private void descuentoProducto(TipoDescuentoEnum tipo, int minimo, String porcentaje) {
        List<DescuentoProducto> actuales = new ArrayList<>(descuentoRepository.findByProducto_IdProductoOrderByTipo(1));
        actuales.add(DescuentoProducto.builder().producto(resma).tipo(tipo).cantidadMinima(minimo)
                .porcentaje(bd(porcentaje)).activo(true).build());
        when(descuentoRepository.findByProducto_IdProductoOrderByTipo(1)).thenReturn(actuales);
    }

    private void precioMayorista(String precio) {
        when(precioRepository.findById(1)).thenReturn(Optional.of(PrecioMayorista.builder().idProducto(1).precio(bd(precio)).build()));
    }

    private Cliente mayorista(Integer id, CategoriaClienteEnum categoria) {
        Cliente cliente = Cliente.builder().idCliente(id).nombre("Libreria Uno").tipoCliente(TipoClienteEnum.MAYORISTA)
                .categoriaDescuento(categoria).build();
        when(clienteRepository.findById(id)).thenReturn(Optional.of(cliente));
        return cliente;
    }

    private static OpcionDescuentoDto opcion(OpcionesDescuentoDto opciones, TipoDescuentoEnum tipo) {
        return opciones.getOpciones().stream().filter(o -> o.getTipo() == tipo).findFirst().orElse(null);
    }

    // ---------- opciones ----------

    @Test
    void sinClienteSoloSeOfreceElDescuentoManual() {
        OpcionesDescuentoDto opciones = service.calcularOpciones(1, null, 10);

        assertEquals(1, opciones.getOpciones().size());
        assertEquals(TipoDescuentoEnum.MANUAL, opciones.getOpciones().get(0).getTipo());
        assertNull(opciones.getSugerida());
        assertEquals(bd("50.00"), opciones.getPrecioVenta());
    }

    @Test
    void unClienteMinoristaNoRecibeDescuentosMayoristas() {
        Cliente minorista = Cliente.builder().idCliente(9).tipoCliente(TipoClienteEnum.MINORISTA).build();
        when(clienteRepository.findById(9)).thenReturn(Optional.of(minorista));
        descuentoProducto(TipoDescuentoEnum.CIENTO, 100, "12.00");
        precioMayorista("40.00");

        OpcionesDescuentoDto opciones = service.calcularOpciones(1, 9, 500);

        assertEquals(1, opciones.getOpciones().size());
        assertNull(opciones.getSugerida());
    }

    @Test
    void laCategoriaConUnSoloNivelSeSugiere() {
        mayorista(5, CategoriaClienteEnum.A);
        niveles(CategoriaClienteEnum.A, "10.00");

        OpcionesDescuentoDto opciones = service.calcularOpciones(1, 5, 1);

        OpcionDescuentoDto categoria = opcion(opciones, TipoDescuentoEnum.CATEGORIA);
        assertEquals(bd("10.00"), categoria.getPorcentaje());
        assertTrue(categoria.getNiveles().isEmpty());
        assertEquals(TipoDescuentoEnum.CATEGORIA, opciones.getSugerida().getTipo());
        assertEquals(CategoriaClienteEnum.A, opciones.getCategoria());
    }

    @Test
    void laCategoriaVolumenOfreceLosNivelesPeroNoSugiereNinguno() {
        mayorista(6, CategoriaClienteEnum.VOLUMEN);
        niveles(CategoriaClienteEnum.VOLUMEN, "12.00", "15.00", "20.00");

        OpcionesDescuentoDto opciones = service.calcularOpciones(1, 6, 1);

        OpcionDescuentoDto categoria = opcion(opciones, TipoDescuentoEnum.CATEGORIA);
        assertNull(categoria.getPorcentaje());
        assertEquals(Arrays.asList(bd("12.00"), bd("15.00"), bd("20.00")), categoria.getNiveles());
        assertNull(opciones.getSugerida());
    }

    @Test
    void elDescuentoPorCientoSoloSeSugiereAlLlegarALaCantidadMinima() {
        mayorista(6, CategoriaClienteEnum.VOLUMEN);
        niveles(CategoriaClienteEnum.VOLUMEN, "12.00", "15.00", "20.00");
        descuentoProducto(TipoDescuentoEnum.CIENTO, 100, "12.00");

        OpcionesDescuentoDto pocas = service.calcularOpciones(1, 6, 40);
        OpcionesDescuentoDto cien = service.calcularOpciones(1, 6, 120);

        assertEquals(false, opcion(pocas, TipoDescuentoEnum.CIENTO).isAlcanzaMinimo());
        assertNull(pocas.getSugerida());
        assertTrue(opcion(cien, TipoDescuentoEnum.CIENTO).isAlcanzaMinimo());
        assertEquals(TipoDescuentoEnum.CIENTO, cien.getSugerida().getTipo());
    }

    @Test
    void entreVariasOpcionesSeSugiereLaDeMayorPorcentaje() {
        mayorista(5, CategoriaClienteEnum.B);
        niveles(CategoriaClienteEnum.B, "8.00");
        descuentoProducto(TipoDescuentoEnum.FARDO, 24, "10.00");

        OpcionesDescuentoDto opciones = service.calcularOpciones(1, 5, 24);

        assertEquals(TipoDescuentoEnum.FARDO, opciones.getSugerida().getTipo());
        assertEquals(bd("10.00"), opciones.getSugerida().getPorcentaje());
    }

    @Test
    void enUnEmpateDePorcentajeSePrefiereLoMasEspecifico() {
        mayorista(5, CategoriaClienteEnum.A);
        niveles(CategoriaClienteEnum.A, "10.00");
        descuentoProducto(TipoDescuentoEnum.CIENTO, 100, "10.00");

        assertEquals(TipoDescuentoEnum.CIENTO, service.calcularOpciones(1, 5, 100).getSugerida().getTipo());
    }

    @Test
    void elPrecioMayoristaFijoSeExpresaComoPorcentajeEquivalenteYSiempreSeSugiere() {
        mayorista(5, CategoriaClienteEnum.A);
        niveles(CategoriaClienteEnum.A, "10.00");
        precioMayorista("40.00");

        OpcionesDescuentoDto opciones = service.calcularOpciones(1, 5, 1);

        OpcionDescuentoDto fijo = opcion(opciones, TipoDescuentoEnum.PRECIO_MAYORISTA);
        assertEquals(0, bd("20").compareTo(fijo.getPorcentaje()));
        assertEquals(bd("40.00"), fijo.getPrecio());
        assertEquals(bd("40.00"), opciones.getPrecioMayorista());
        assertEquals(TipoDescuentoEnum.PRECIO_MAYORISTA, opciones.getSugerida().getTipo());
    }

    @Test
    void unPrecioMayoristaNoMenorAlDeVentaSeIgnora() {
        mayorista(5, CategoriaClienteEnum.A);
        precioMayorista("50.00");

        assertNull(opcion(service.calcularOpciones(1, 5, 1), TipoDescuentoEnum.PRECIO_MAYORISTA));
    }

    @Test
    void elPorcentajeEquivalenteTieneCuatroDecimales() {
        assertEquals(bd("11.5646"), DescuentoServiceImpl.porcentajeEquivalente(bd("7.35"), bd("6.50")));
        assertEquals(bd("20.0000"), DescuentoServiceImpl.porcentajeEquivalente(bd("50"), bd("40")));
    }

    @Test
    void unProductoInexistenteEsNotFound() {
        when(productoRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.calcularOpciones(99, null, 1));
    }

    // ---------- líneas de proforma ----------

    private Proforma proforma(Integer idCliente, DetalleProforma... lineas) {
        Proforma proforma = new Proforma();
        if (idCliente != null) {
            proforma.setCliente(Cliente.builder().idCliente(idCliente).build());
        }
        proforma.setItemsProforma(new ArrayList<>(Arrays.asList(lineas)));
        return proforma;
    }

    private DetalleProforma linea(TipoDescuentoEnum tipo, Float descuento, int cantidad) {
        DetalleProforma linea = new DetalleProforma();
        linea.setProducto(Producto.builder().idProducto(1).build());
        linea.setCantidad(cantidad);
        linea.setDescuento(descuento);
        linea.setTipoDescuento(tipo);
        return linea;
    }

    @Test
    void aceptaUnaLineaDeCategoriaConElNivelCorrectoYRegistraPrecioYSugerencia() {
        mayorista(5, CategoriaClienteEnum.VOLUMEN);
        niveles(CategoriaClienteEnum.VOLUMEN, "12.00", "15.00", "20.00");
        descuentoProducto(TipoDescuentoEnum.CIENTO, 100, "12.00");
        DetalleProforma linea = linea(TipoDescuentoEnum.CATEGORIA, 15f, 120);

        service.prepararLineas(proforma(5, linea));

        assertEquals(TipoDescuentoEnum.CATEGORIA, linea.getTipoDescuento());
        assertEquals(bd("50.00"), linea.getPrecioUnitario());
        assertEquals(bd("12.00"), linea.getPorcentajeSugerido());
    }

    @Test
    void rechazaUnNivelQueNoEsDeLaCategoriaDelCliente() {
        mayorista(5, CategoriaClienteEnum.VOLUMEN);
        niveles(CategoriaClienteEnum.VOLUMEN, "12.00", "15.00", "20.00");

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.prepararLineas(proforma(5, linea(TipoDescuentoEnum.CATEGORIA, 18f, 10))));

        assertTrue(error.getMessage().contains("RESMA CARTA"));
        assertTrue(error.getMessage().contains("VOLUMEN"));
    }

    @Test
    void rechazaCategoriaSiElClienteNoTieneCategoria() {
        mayorista(5, null);

        assertThrows(ReglaNegocioException.class,
                () -> service.prepararLineas(proforma(5, linea(TipoDescuentoEnum.CATEGORIA, 10f, 10))));
    }

    @Test
    void rechazaDescuentosMayoristasParaUnClienteMinoristaOSinCliente() {
        Cliente minorista = Cliente.builder().idCliente(9).tipoCliente(TipoClienteEnum.MINORISTA).build();
        when(clienteRepository.findById(9)).thenReturn(Optional.of(minorista));
        descuentoProducto(TipoDescuentoEnum.CIENTO, 100, "12.00");

        assertThrows(ReglaNegocioException.class,
                () -> service.prepararLineas(proforma(9, linea(TipoDescuentoEnum.CIENTO, 12f, 100))));
        assertThrows(ReglaNegocioException.class,
                () -> service.prepararLineas(proforma(null, linea(TipoDescuentoEnum.PRECIO_MAYORISTA, 20f, 1))));
    }

    @Test
    void elDescuentoPorCientoDebeCoincidirConElConfiguradoAunConMenosDelMinimo() {
        mayorista(5, CategoriaClienteEnum.B);
        niveles(CategoriaClienteEnum.B, "8.00");
        descuentoProducto(TipoDescuentoEnum.CIENTO, 100, "12.00");

        DetalleProforma pocas = linea(TipoDescuentoEnum.CIENTO, 12f, 30);
        service.prepararLineas(proforma(5, pocas));
        assertEquals(TipoDescuentoEnum.CIENTO, pocas.getTipoDescuento());

        assertThrows(ReglaNegocioException.class,
                () -> service.prepararLineas(proforma(5, linea(TipoDescuentoEnum.CIENTO, 15f, 120))));
    }

    @Test
    void rechazaUnDescuentoEspecialQueElProductoNoTiene() {
        mayorista(5, CategoriaClienteEnum.B);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.prepararLineas(proforma(5, linea(TipoDescuentoEnum.FARDO, 10f, 24))));

        assertTrue(error.getMessage().contains("FARDO"));
    }

    @Test
    void elPrecioMayoristaAceptaElPorcentajeEquivalenteYRechazaOtro() {
        mayorista(5, CategoriaClienteEnum.A);
        precioMayorista("40.00");

        DetalleProforma correcta = linea(TipoDescuentoEnum.PRECIO_MAYORISTA, 20f, 3);
        service.prepararLineas(proforma(5, correcta));
        assertEquals(TipoDescuentoEnum.PRECIO_MAYORISTA, correcta.getTipoDescuento());

        assertThrows(ReglaNegocioException.class,
                () -> service.prepararLineas(proforma(5, linea(TipoDescuentoEnum.PRECIO_MAYORISTA, 25f, 3))));
    }

    @Test
    void sinPrecioMayoristaNoSePuedeUsarEseTipo() {
        mayorista(5, CategoriaClienteEnum.A);

        assertThrows(ReglaNegocioException.class,
                () -> service.prepararLineas(proforma(5, linea(TipoDescuentoEnum.PRECIO_MAYORISTA, 20f, 3))));
    }

    @Test
    void unaLineaSinTipoSeTomaComoDescuentoManualYSiempreSePermite() {
        DetalleProforma sinTipo = linea(null, 33f, 2);

        service.prepararLineas(proforma(null, sinTipo));

        assertEquals(TipoDescuentoEnum.MANUAL, sinTipo.getTipoDescuento());
        assertNull(sinTipo.getPorcentajeSugerido());
    }

    @Test
    void elDescuentoDebeEstarEntreCeroYCien() {
        assertThrows(ReglaNegocioException.class,
                () -> service.prepararLineas(proforma(null, linea(TipoDescuentoEnum.MANUAL, 101f, 1))));
        assertThrows(ReglaNegocioException.class,
                () -> service.prepararLineas(proforma(null, linea(TipoDescuentoEnum.MANUAL, -1f, 1))));
    }

    @Test
    void unaLineaSinDescuentoSeTomaComoCero() {
        DetalleProforma sinDescuento = linea(null, null, 2);

        service.prepararLineas(proforma(null, sinDescuento));

        assertEquals(TipoDescuentoEnum.MANUAL, sinDescuento.getTipoDescuento());
    }

    @Test
    void unaLineaNuevaTomaElPrecioDeHoyAunqueElClienteEnvieOtro() {
        DetalleProforma nueva = linea(null, 0f, 2);
        nueva.setPrecioUnitario(bd("1.00"));

        service.prepararLineas(proforma(null, nueva));

        assertEquals(bd("50.00"), nueva.getPrecioUnitario());
    }

    @Test
    void unaLineaExistenteConservaSuPrecioYUnaAnteriorSeDespejaDeSuSubtotal() {
        DetalleProforma conPrecio = linea(null, 0f, 2);
        conPrecio.setIdDetalle(10L);
        conPrecio.setPrecioUnitario(bd("45.00"));
        DetalleProforma anterior = linea(null, 10f, 4);
        anterior.setIdDetalle(11L);
        anterior.setSubTotal(bd("180.00"));

        service.prepararLineas(proforma(null, conPrecio, anterior));

        assertEquals(bd("45.00"), conPrecio.getPrecioUnitario());
        assertEquals(bd("45.00"), anterior.getPrecioUnitario());
    }

    @Test
    void unProductoQueYaNoExisteEnUnaLineaSeRechaza() {
        when(productoRepository.findById(77)).thenReturn(Optional.empty());
        DetalleProforma linea = linea(null, 0f, 1);
        linea.getProducto().setIdProducto(77);

        assertThrows(ReglaNegocioException.class, () -> service.prepararLineas(proforma(null, linea)));
    }

    @Test
    void unaProformaSinDetalleNoHaceNada() {
        Proforma vacia = new Proforma();
        vacia.setItemsProforma(null);

        service.prepararLineas(vacia);

        verify(productoRepository, never()).findById(any());
    }

    // ---------- administración de niveles ----------

    @Test
    void guardarNivelesCreaLosNuevosYDesactivaLosQueSeQuitan() {
        NivelDescuentoCategoria doce = NivelDescuentoCategoria.builder().categoria(CategoriaClienteEnum.VOLUMEN).porcentaje(bd("12.00")).activo(true).build();
        NivelDescuentoCategoria veinte = NivelDescuentoCategoria.builder().categoria(CategoriaClienteEnum.VOLUMEN).porcentaje(bd("20.00")).activo(true).build();
        when(nivelRepository.findByCategoriaOrderByPorcentajeAsc(CategoriaClienteEnum.VOLUMEN)).thenReturn(Arrays.asList(doce, veinte));

        service.guardarNiveles(CategoriaClienteEnum.VOLUMEN, Arrays.asList(bd("12"), bd("18")));

        assertTrue(doce.isActivo());
        assertEquals(false, veinte.isActivo());
        verify(nivelRepository).save(org.mockito.ArgumentMatchers.argThat(n ->
                n.getPorcentaje().compareTo(bd("18")) == 0 && n.isActivo() && n.getCategoria() == CategoriaClienteEnum.VOLUMEN));
    }

    @Test
    void guardarNivelesRechazaPorcentajesFueraDeRango() {
        assertThrows(ReglaNegocioException.class, () -> service.guardarNiveles(CategoriaClienteEnum.A, Collections.singletonList(bd("0"))));
        assertThrows(ReglaNegocioException.class, () -> service.guardarNiveles(CategoriaClienteEnum.A, Collections.singletonList(bd("101"))));
        assertThrows(ReglaNegocioException.class, () -> service.guardarNiveles(CategoriaClienteEnum.A, Collections.singletonList(null)));
        verify(nivelRepository, never()).save(any());
    }

    // ---------- administración de productos ----------

    private GuardarDescuentosProductoRequest solicitud(String precio, DescuentoProductoDto... descuentos) {
        return new GuardarDescuentosProductoRequest(precio == null ? null : bd(precio), Arrays.asList(descuentos));
    }

    @Test
    void guardarProductoRechazaUnPrecioMayoristaNoMenorAlDeVenta() {
        assertThrows(ReglaNegocioException.class, () -> service.guardarProducto(1, solicitud("50.00")));
        assertThrows(ReglaNegocioException.class, () -> service.guardarProducto(1, solicitud("60.00")));
        assertThrows(ReglaNegocioException.class, () -> service.guardarProducto(1, solicitud("0")));
        verify(precioRepository, never()).save(any());
    }

    @Test
    void guardarProductoGuardaElPrecioMayoristaYLoQuitaConNulo() {
        service.guardarProducto(1, solicitud("42.5"));
        verify(precioRepository).save(org.mockito.ArgumentMatchers.argThat(p -> p.getPrecio().compareTo(bd("42.50")) == 0));

        PrecioMayorista existente = PrecioMayorista.builder().idProducto(1).precio(bd("42.50")).build();
        when(precioRepository.findById(1)).thenReturn(Optional.of(existente));
        service.guardarProducto(1, solicitud(null));
        verify(precioRepository).delete(existente);
    }

    @Test
    void guardarProductoValidaLosDescuentosEspeciales() {
        DescuentoProductoDto ciento = new DescuentoProductoDto(TipoDescuentoEnum.CIENTO, 100, bd("12"), true);

        assertThrows(ReglaNegocioException.class, () -> service.guardarProducto(1,
                solicitud(null, new DescuentoProductoDto(TipoDescuentoEnum.MANUAL, 100, bd("12"), true))));
        assertThrows(ReglaNegocioException.class, () -> service.guardarProducto(1,
                solicitud(null, new DescuentoProductoDto(TipoDescuentoEnum.CIENTO, 1, bd("12"), true))));
        assertThrows(ReglaNegocioException.class, () -> service.guardarProducto(1,
                solicitud(null, new DescuentoProductoDto(TipoDescuentoEnum.CIENTO, 100, bd("0"), true))));
        assertThrows(ReglaNegocioException.class, () -> service.guardarProducto(1, solicitud(null, ciento, ciento)));
        verify(descuentoRepository, never()).save(any());
    }

    @Test
    void guardarProductoReemplazaLosDescuentosEspeciales() {
        DescuentoProducto ciento = DescuentoProducto.builder().producto(resma).tipo(TipoDescuentoEnum.CIENTO)
                .cantidadMinima(100).porcentaje(bd("12.00")).activo(true).build();
        DescuentoProducto fardo = DescuentoProducto.builder().producto(resma).tipo(TipoDescuentoEnum.FARDO)
                .cantidadMinima(24).porcentaje(bd("15.00")).activo(true).build();
        when(descuentoRepository.findByProducto_IdProductoOrderByTipo(1)).thenReturn(Arrays.asList(ciento, fardo));

        service.guardarProducto(1, solicitud(null, new DescuentoProductoDto(TipoDescuentoEnum.CIENTO, 120, bd("13"), false)));

        assertEquals(120, ciento.getCantidadMinima());
        assertEquals(bd("13.00"), ciento.getPorcentaje());
        assertEquals(false, ciento.isActivo());
        verify(descuentoRepository).delete(fardo);
        assertNotNull(service.findProducto(1));
    }
}
