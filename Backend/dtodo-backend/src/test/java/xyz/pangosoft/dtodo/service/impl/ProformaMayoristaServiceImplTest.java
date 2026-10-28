package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.DetalleProforma;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.Ruta;
import xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum;
import xyz.pangosoft.dtodo.model.enums.TipoClienteEnum;
import xyz.pangosoft.dtodo.model.enums.TipoDescuentoEnum;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;
import xyz.pangosoft.dtodo.repository.IClienteRepository;
import xyz.pangosoft.dtodo.service.IAmbitoVendedorService;

class ProformaMayoristaServiceImplTest {

    private final IClienteRepository clienteRepository = mock(IClienteRepository.class);
    private final IAmbitoVendedorService ambito = mock(IAmbitoVendedorService.class);
    private final xyz.pangosoft.dtodo.service.ISaldoClienteService saldoService = mock(xyz.pangosoft.dtodo.service.ISaldoClienteService.class);
    private final xyz.pangosoft.dtodo.repository.IProformaRepository proformaRepository = mock(xyz.pangosoft.dtodo.repository.IProformaRepository.class);
    private final ProformaMayoristaServiceImpl service = new ProformaMayoristaServiceImpl(clienteRepository, ambito, saldoService,
            proformaRepository);

    private Cliente cliente(TipoClienteEnum tipo, Ruta ruta) {
        Cliente cliente = new Cliente();
        cliente.setIdCliente(5);
        cliente.setTipoCliente(tipo);
        cliente.setRuta(ruta);
        when(clienteRepository.findById(5)).thenReturn(Optional.of(cliente));
        return cliente;
    }

    private Proforma proforma(TipoProformaEnum tipo) {
        Proforma proforma = new Proforma();
        proforma.setTipoProforma(tipo);
        Cliente referencia = new Cliente();
        referencia.setIdCliente(5);
        proforma.setCliente(referencia);
        proforma.setItemsProforma(new ArrayList<>());
        return proforma;
    }

    @Test
    void laMayoristaTomaLaRutaDelClienteYLimpiaObservaciones() {
        Ruta ruta = new Ruta();
        ruta.setNombre("RUTA 1");
        cliente(TipoClienteEnum.MAYORISTA, ruta);
        Proforma proforma = proforma(TipoProformaEnum.MAYORISTA);
        proforma.setObservaciones("  empacar aparte  ");
        DetalleProforma linea = new DetalleProforma();
        linea.setObservacion("   ");
        proforma.getItemsProforma().add(linea);

        service.preparar(proforma);

        assertEquals(ruta, proforma.getRuta());
        assertEquals("empacar aparte", proforma.getObservaciones());
        assertNull(linea.getObservacion());
    }

    @Test
    void laMayoristaRequiereClienteMayoristaConRuta() {
        cliente(TipoClienteEnum.MINORISTA, new Ruta());
        assertThrows(ReglaNegocioException.class, () -> service.preparar(proforma(TipoProformaEnum.MAYORISTA)));

        cliente(TipoClienteEnum.MAYORISTA, null);
        assertThrows(ReglaNegocioException.class, () -> service.preparar(proforma(TipoProformaEnum.MAYORISTA)));
    }

    @Test
    void sinTipoSeTomaComoMinoristaYSeQuitaLaRuta() {
        Proforma proforma = proforma(null);
        proforma.setRuta(new Ruta());

        service.preparar(proforma);

        assertEquals(TipoProformaEnum.MINORISTA, proforma.getTipoProforma());
        assertNull(proforma.getRuta());
    }

    @Test
    void elVendedorSiempreHaceProformasMayoristas() {
        when(ambito.esVendedor()).thenReturn(true);
        cliente(TipoClienteEnum.MAYORISTA, new Ruta());

        Proforma proforma = proforma(TipoProformaEnum.MINORISTA);
        service.preparar(proforma);

        assertEquals(TipoProformaEnum.MAYORISTA, proforma.getTipoProforma());
    }

    @Test
    void lasObservacionesMuyLargasSeRechazan() {
        Proforma proforma = proforma(null);
        proforma.setObservaciones(new String(new char[501]).replace('\0', 'a'));
        assertThrows(ReglaNegocioException.class, () -> service.preparar(proforma));

        Proforma conLinea = proforma(null);
        DetalleProforma linea = new DetalleProforma();
        Producto producto = new Producto();
        producto.setNombre("CUADERNO");
        linea.setProducto(producto);
        linea.setObservacion(new String(new char[256]).replace('\0', 'a'));
        conLinea.getItemsProforma().add(linea);
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> service.preparar(conLinea));
        assertEquals(true, error.getMessage().contains("CUADERNO"));
    }

    private Proforma conLinea(TipoProformaEnum tipo, TipoDescuentoEnum tipoDescuento, float descuento) {
        Proforma proforma = proforma(tipo);
        DetalleProforma linea = new DetalleProforma();
        linea.setTipoDescuento(tipoDescuento);
        linea.setDescuento(descuento);
        proforma.getItemsProforma().add(linea);
        return proforma;
    }

    @Test
    void sobreElTopeDeDescuentoLaMayoristaQuedaPendiente() {
        Proforma proforma = conLinea(TipoProformaEnum.MAYORISTA, TipoDescuentoEnum.CATEGORIA, 20f);

        service.evaluarAutorizacion(proforma);

        assertEquals(EstadoAutorizacionEnum.PENDIENTE, proforma.getEstadoAutorizacion());
    }

    @Test
    void enElTopeOPorDebajoNoNecesitaAutorizacionYSeLimpiaLaAnterior() {
        Proforma proforma = conLinea(TipoProformaEnum.MAYORISTA, TipoDescuentoEnum.MANUAL, 15f);
        proforma.setEstadoAutorizacion(EstadoAutorizacionEnum.APROBADA);

        service.evaluarAutorizacion(proforma);

        assertNull(proforma.getEstadoAutorizacion());
    }

    @Test
    void elPrecioMayoristaFijoNuncaPideAutorizacion() {
        Proforma proforma = conLinea(TipoProformaEnum.MAYORISTA, TipoDescuentoEnum.PRECIO_MAYORISTA, 30f);

        service.evaluarAutorizacion(proforma);

        assertNull(proforma.getEstadoAutorizacion());
    }

    @Test
    void unaMinoristaNoPideAutorizacionAunqueSuDescuentoSeaAlto() {
        Proforma proforma = conLinea(TipoProformaEnum.MINORISTA, TipoDescuentoEnum.MANUAL, 50f);
        proforma.setEstadoAutorizacion(EstadoAutorizacionEnum.RECHAZADA);

        service.evaluarAutorizacion(proforma);

        assertNull(proforma.getEstadoAutorizacion());
    }

    @Test
    void unaLineaSobreElTopeBastaAunqueLasDemasEstenBien() {
        Proforma proforma = conLinea(TipoProformaEnum.MAYORISTA, TipoDescuentoEnum.MANUAL, 5f);
        DetalleProforma otra = new DetalleProforma();
        otra.setTipoDescuento(TipoDescuentoEnum.MANUAL);
        otra.setDescuento(15.5f);
        proforma.getItemsProforma().add(otra);

        service.evaluarAutorizacion(proforma);

        assertEquals(EstadoAutorizacionEnum.PENDIENTE, proforma.getEstadoAutorizacion());
        assertEquals(15d, service.descuentoMaximo());
    }

    @Test
    void laPeticionNoPuedeTraerElVencimientoYaAutorizado() {
        cliente(TipoClienteEnum.MAYORISTA, new Ruta());
        Proforma proforma = proforma(TipoProformaEnum.MAYORISTA);
        proforma.setVencimientoAutorizado(true);

        service.preparar(proforma);

        assertEquals(false, proforma.isVencimientoAutorizado());
    }

    // ------------------------------------------------------------------------------------------ condición de pago

    private Cliente conCredito(String limite, Integer dias) {
        Cliente cliente = cliente(TipoClienteEnum.MAYORISTA, new Ruta());
        cliente.setLimiteCredito(limite == null ? null : new java.math.BigDecimal(limite));
        cliente.setDiasCredito(dias);
        return cliente;
    }

    @Test
    void sinCondicionElPedidoEsDeContado() {
        conCredito("5000", 30);
        Proforma proforma = proforma(TipoProformaEnum.MAYORISTA);

        service.preparar(proforma);

        assertEquals(xyz.pangosoft.dtodo.model.enums.CondicionPagoEnum.CONTADO, proforma.getCondicionPago());
        assertNull(proforma.getDiasCredito());
    }

    @Test
    void aCreditoSoloConLimiteYDiasYGuardaLosDiasDelCliente() {
        conCredito("5000", 30);
        Proforma proforma = proforma(TipoProformaEnum.MAYORISTA);
        proforma.setCondicionPago(xyz.pangosoft.dtodo.model.enums.CondicionPagoEnum.CREDITO);

        service.preparar(proforma);

        assertEquals(xyz.pangosoft.dtodo.model.enums.CondicionPagoEnum.CREDITO, proforma.getCondicionPago());
        assertEquals(Integer.valueOf(30), proforma.getDiasCredito());
    }

    @Test
    void aCreditoSinLimiteODiasSeRechaza() {
        for (Object[] caso : new Object[][] { { null, 30 }, { "0", 30 }, { "5000", null }, { "5000", 0 }, { null, null } }) {
            conCredito((String) caso[0], (Integer) caso[1]);
            Proforma proforma = proforma(TipoProformaEnum.MAYORISTA);
            proforma.setCondicionPago(xyz.pangosoft.dtodo.model.enums.CondicionPagoEnum.CREDITO);

            assertThrows(ReglaNegocioException.class, () -> service.preparar(proforma), caso[0] + "/" + caso[1]);
        }
    }

    @Test
    void unaMinoristaSiempreEsDeContadoAunqueTraigaCredito() {
        Proforma proforma = proforma(TipoProformaEnum.MINORISTA);
        proforma.setCondicionPago(xyz.pangosoft.dtodo.model.enums.CondicionPagoEnum.CREDITO);
        proforma.setDiasCredito(45);

        service.preparar(proforma);

        assertEquals(xyz.pangosoft.dtodo.model.enums.CondicionPagoEnum.CONTADO, proforma.getCondicionPago());
        assertNull(proforma.getDiasCredito());
    }

    // ------------------------------------------------------------------------------------------ límite de crédito

    private Proforma aCredito(String total) {
        Proforma proforma = proforma(TipoProformaEnum.MAYORISTA);
        proforma.setCondicionPago(xyz.pangosoft.dtodo.model.enums.CondicionPagoEnum.CREDITO);
        DetalleProforma linea = new DetalleProforma();
        linea.setSubTotalDescuento(new java.math.BigDecimal(total));
        proforma.getItemsProforma().add(linea);
        return proforma;
    }

    private void exposicion(String valor) {
        when(saldoService.exposicionDeCredito(org.mockito.ArgumentMatchers.eq(5), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new java.math.BigDecimal(valor));
    }

    @Test
    void unPedidoACreditoQueCabeEnElLimiteNoNecesitaAutorizacion() {
        conCredito("1000", 30);
        exposicion("400.00");
        Proforma proforma = aCredito("600.00");

        service.evaluarCredito(proforma);

        assertNull(proforma.getCreditoAutorizacion());
        assertNull(proforma.getExcesoCredito());
    }

    @Test
    void unPedidoQuePasaDelLimiteQuedaPendienteConSuExceso() {
        conCredito("1000", 30);
        exposicion("400.00");
        Proforma proforma = aCredito("600.01");

        service.evaluarCredito(proforma);

        assertEquals(xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum.PENDIENTE, proforma.getCreditoAutorizacion());
        assertEquals(new java.math.BigDecimal("0.01"), proforma.getExcesoCredito());
    }

    @Test
    void lasLineasSinImporteUsanElTotalDeLaProforma() {
        conCredito("1000", 30);
        exposicion("0.00");
        Proforma proforma = aCredito("10.00");
        proforma.getItemsProforma().get(0).setSubTotalDescuento(null);
        proforma.setTotal(new java.math.BigDecimal("1500.00"));

        service.evaluarCredito(proforma);

        assertEquals(new java.math.BigDecimal("500.00"), proforma.getExcesoCredito());
    }

    @Test
    void laContadoOMinoristaNoSeEvaluaYSeLimpiaLoQueTraiga() {
        conCredito("1000", 30);
        exposicion("5000.00");
        Proforma contado = aCredito("600.00");
        contado.setCondicionPago(xyz.pangosoft.dtodo.model.enums.CondicionPagoEnum.CONTADO);
        contado.setCreditoAutorizacion(xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum.APROBADA);
        contado.setExcesoCredito(new java.math.BigDecimal("99"));

        service.evaluarCredito(contado);

        assertNull(contado.getCreditoAutorizacion());
        assertNull(contado.getExcesoCredito());
        org.mockito.Mockito.verify(saldoService, org.mockito.Mockito.never()).exposicionDeCredito(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        Proforma minorista = aCredito("600.00");
        minorista.setTipoProforma(TipoProformaEnum.MINORISTA);
        service.evaluarCredito(minorista);
        assertNull(minorista.getCreditoAutorizacion());
    }

    @Test
    void unaEdicionQueNoEmpeoraElExcesoAprobadoConservaLaAprobacion() {
        conCredito("1000", 30);
        exposicion("400.00");
        Proforma previa = new Proforma();
        previa.setIdProforma(30L);
        previa.setCreditoAutorizacion(xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum.APROBADA);
        previa.setExcesoCredito(new java.math.BigDecimal("200.00"));
        when(proformaRepository.findById(30L)).thenReturn(Optional.of(previa));

        Proforma igual = aCredito("800.00");
        igual.setIdProforma(30L);
        service.evaluarCredito(igual);
        assertEquals(xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum.APROBADA, igual.getCreditoAutorizacion());
        assertEquals(new java.math.BigDecimal("200.00"), igual.getExcesoCredito());

        Proforma mayor = aCredito("900.00");
        mayor.setIdProforma(30L);
        service.evaluarCredito(mayor);
        assertEquals(xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum.PENDIENTE, mayor.getCreditoAutorizacion());
        assertEquals(new java.math.BigDecimal("300.00"), mayor.getExcesoCredito());
    }

    @Test
    void unaEdicionDeUnPedidoRechazadoVuelveAPedirAutorizacion() {
        conCredito("1000", 30);
        exposicion("400.00");
        Proforma previa = new Proforma();
        previa.setIdProforma(30L);
        previa.setCreditoAutorizacion(xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum.RECHAZADA);
        previa.setExcesoCredito(new java.math.BigDecimal("500.00"));
        when(proformaRepository.findById(30L)).thenReturn(Optional.of(previa));
        Proforma editada = aCredito("700.00");
        editada.setIdProforma(30L);

        service.evaluarCredito(editada);

        assertEquals(xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum.PENDIENTE, editada.getCreditoAutorizacion());
    }
}
