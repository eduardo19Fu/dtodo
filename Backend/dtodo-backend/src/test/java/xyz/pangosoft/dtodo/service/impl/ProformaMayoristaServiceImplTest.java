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
import xyz.pangosoft.dtodo.model.enums.TipoClienteEnum;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;
import xyz.pangosoft.dtodo.repository.IClienteRepository;
import xyz.pangosoft.dtodo.service.IAmbitoVendedorService;

class ProformaMayoristaServiceImplTest {

    private final IClienteRepository clienteRepository = mock(IClienteRepository.class);
    private final IAmbitoVendedorService ambito = mock(IAmbitoVendedorService.class);
    private final ProformaMayoristaServiceImpl service = new ProformaMayoristaServiceImpl(clienteRepository, ambito);

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
}
