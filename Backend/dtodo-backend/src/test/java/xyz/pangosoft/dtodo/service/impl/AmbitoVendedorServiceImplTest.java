package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.Factura;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.Ruta;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.CategoriaClienteEnum;
import xyz.pangosoft.dtodo.model.enums.TipoClienteEnum;
import xyz.pangosoft.dtodo.repository.IClienteRepository;
import xyz.pangosoft.dtodo.repository.IFacturaRepository;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.repository.IRutaRepository;
import xyz.pangosoft.dtodo.service.IUsuarioService;

/** Alcance del vendedor: solo sus rutas, sus clientes y sus proformas. */
class AmbitoVendedorServiceImplTest {

    private static final int ID_VENDEDOR = 7;

    private final IRutaRepository rutaRepository = mock(IRutaRepository.class);
    private final IClienteRepository clienteRepository = mock(IClienteRepository.class);
    private final IFacturaRepository facturaRepository = mock(IFacturaRepository.class);
    private final IProformaRepository proformaRepository = mock(IProformaRepository.class);
    private final IUsuarioService usuarioService = mock(IUsuarioService.class);
    private AmbitoVendedorServiceImpl ambito;

    @BeforeEach
    void preparar() {
        ambito = new AmbitoVendedorServiceImpl(rutaRepository, clienteRepository, facturaRepository,
                proformaRepository, usuarioService);
        when(rutaRepository.findIdsByVendedor(ID_VENDEDOR)).thenReturn(Arrays.asList(1, 2));
        when(usuarioService.findById(ID_VENDEDOR)).thenReturn(
                Usuario.builder().idUsuario(ID_VENDEDOR).primerNombre("Bryan").apellido("Vargas").build());
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private void iniciarSesion(String... roles) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("id_usuario", String.valueOf(ID_VENDEDOR))
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, AuthorityUtils.createAuthorityList(roles)));
    }

    private static Cliente cliente(Integer id, Integer idRuta) {
        Cliente cliente = Cliente.builder().idCliente(id).nombre("Cliente " + id).direccion("Monjas")
                .tipoCliente(TipoClienteEnum.MAYORISTA).build();
        if (idRuta != null) {
            Ruta ruta = new Ruta();
            ruta.setIdRuta(idRuta);
            cliente.setRuta(ruta);
        }
        return cliente;
    }

    private static Proforma proforma(Long id, Integer idUsuario) {
        Proforma proforma = new Proforma();
        proforma.setIdProforma(id);
        proforma.setNoProforma(id + "P");
        proforma.setUsuario(Usuario.builder().idUsuario(idUsuario).build());
        return proforma;
    }

    // ---------- quién es vendedor acotado ----------

    @Test
    void soloUnVendedorSinOtroRolGeneralEstaAcotado() {
        iniciarSesion("ROLE_VENDEDOR");
        assertTrue(ambito.esVendedor());
        assertEquals(ID_VENDEDOR, ambito.idUsuarioActual());

        iniciarSesion("ROLE_VENDEDOR", "ROLE_ADMIN");
        assertFalse(ambito.esVendedor());

        iniciarSesion("ROLE_VENDEDOR", "ROLE_COBRADOR");
        assertFalse(ambito.esVendedor());

        iniciarSesion("ROLE_COBRADOR");
        assertFalse(ambito.esVendedor());
    }

    @Test
    void sinSesionNadieEstaAcotado() {
        assertFalse(ambito.esVendedor());
        assertNull(ambito.idUsuarioActual());
    }

    // ---------- los demás roles no cambian ----------

    @Test
    void unAdministradorConservaSuAlcanceCompleto() {
        iniciarSesion("ROLE_ADMIN");
        Cliente ajeno = cliente(1, 99);
        Proforma proformaAjena = proforma(5L, 1234);

        assertNull(ambito.idsRutasPermitidas());
        assertTrue(ambito.clienteEnAmbito(ajeno));
        assertSame(ajeno, ambito.limitarLectura(ajeno));
        assertSame(ajeno, ambito.prepararAlta(ajeno));
        assertSame(ajeno, ambito.prepararEdicion(ajeno));
        assertEquals(55, ambito.forzarUsuario(55));
        ambito.exigirProformaPropia(proformaAjena);
        ambito.exigirUsuarioPropio(1234);
        ambito.prepararProforma(proformaAjena);
        assertEquals(1234, proformaAjena.getUsuario().getIdUsuario());
        verifyNoInteractions(clienteRepository, facturaRepository, proformaRepository);
    }

    // ---------- clientes ----------

    @Test
    void elVendedorSoloVeLosClientesDeSusRutas() {
        iniciarSesion("ROLE_VENDEDOR");

        assertEquals(Arrays.asList(1, 2), ambito.idsRutasPermitidas());
        assertTrue(ambito.clienteEnAmbito(cliente(1, 2)));
        assertFalse(ambito.clienteEnAmbito(cliente(2, 5)));
        assertFalse(ambito.clienteEnAmbito(cliente(3, null)));
        assertFalse(ambito.clienteEnAmbito(null));
    }

    @Test
    void unClienteDeOtraRutaSeInformaComoInexistente() {
        iniciarSesion("ROLE_VENDEDOR");
        Cliente propio = cliente(1, 1);

        assertSame(propio, ambito.limitarLectura(propio));
        assertThrows(NotFoundException.class, () -> ambito.limitarLectura(cliente(2, 5)));
    }

    @Test
    void exigeQueElClienteSeaDeUnaRutaDelVendedor() {
        iniciarSesion("ROLE_VENDEDOR");
        when(clienteRepository.findById(1)).thenReturn(Optional.of(cliente(1, 1)));
        when(clienteRepository.findById(2)).thenReturn(Optional.of(cliente(2, 5)));

        ambito.exigirClienteEnAmbito(cliente(1, null));
        ReglaNegocioException fuera = assertThrows(ReglaNegocioException.class,
                () -> ambito.exigirClienteEnAmbito(cliente(2, null)));
        assertTrue(fuera.getMessage().contains("no pertenece a sus rutas"));
        assertThrows(ReglaNegocioException.class, () -> ambito.exigirClienteEnAmbito(cliente(99, null)));
        assertThrows(ReglaNegocioException.class, () -> ambito.exigirClienteEnAmbito(null));
    }

    @Test
    void elAltaDelVendedorQuedaComoMayoristaEnSuRutaSinCategoriaNiCredito() {
        iniciarSesion("ROLE_VENDEDOR");
        Cliente nuevo = cliente(null, 2);
        nuevo.setTipoCliente(TipoClienteEnum.MINORISTA);
        nuevo.setCategoriaDescuento(CategoriaClienteEnum.VOLUMEN);
        nuevo.setLimiteCredito(new BigDecimal("5000"));
        nuevo.setDiasCredito(30);
        nuevo.setNit("4514627-6");

        Cliente resultado = ambito.prepararAlta(nuevo);

        assertEquals(TipoClienteEnum.MAYORISTA, resultado.getTipoCliente());
        assertNull(resultado.getCategoriaDescuento());
        assertNull(resultado.getLimiteCredito());
        assertNull(resultado.getDiasCredito());
    }

    @Test
    void elAltaDelVendedorExigeUnaDeSusRutas() {
        iniciarSesion("ROLE_VENDEDOR");

        assertThrows(ReglaNegocioException.class, () -> ambito.prepararAlta(cliente(null, 5)));
        assertThrows(ReglaNegocioException.class, () -> ambito.prepararAlta(cliente(null, null)));
    }

    @Test
    void elVendedorNoPuedeDuplicarUnNitYaRegistradoPeroSiUsarConsumidorFinal() {
        iniciarSesion("ROLE_VENDEDOR");
        when(clienteRepository.existsByNit("4514627-6")).thenReturn(true);
        when(clienteRepository.existsByNit("C/F")).thenReturn(true);
        Cliente conNit = cliente(null, 1);
        conNit.setNit("4514627-6");
        Cliente consumidorFinal = cliente(null, 1);
        consumidorFinal.setNit("C/F");

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> ambito.prepararAlta(conNit));
        assertTrue(error.getMessage().contains("administrador"));
        assertSame(consumidorFinal, ambito.prepararAlta(consumidorFinal));
    }

    @Test
    void elAltaDelVendedorNoSirveParaModificarUnClienteExistente() {
        iniciarSesion("ROLE_VENDEDOR");

        assertThrows(ReglaNegocioException.class, () -> ambito.prepararAlta(cliente(4, 1)));
    }

    @Test
    void elVendedorSoloModificaDireccionContactoTelefonoDeContactoYReferencia() {
        iniciarSesion("ROLE_VENDEDOR");
        Cliente existente = cliente(1, 1);
        existente.setNit("4514627-6");
        existente.setNombre("Libreria Uno");
        existente.setCategoriaDescuento(CategoriaClienteEnum.B);
        existente.setLimiteCredito(new BigDecimal("1000"));
        when(clienteRepository.findById(1)).thenReturn(Optional.of(existente));

        Cliente solicitud = cliente(1, 2);
        solicitud.setNombre("Otro nombre");
        solicitud.setNit("999");
        solicitud.setDireccion("Nueva dirección");
        solicitud.setContacto("Ana Perez");
        solicitud.setTelefonoContacto("5555-0000");
        solicitud.setReferenciaUbicacion("Frente al parque");
        solicitud.setCategoriaDescuento(CategoriaClienteEnum.VOLUMEN);
        solicitud.setLimiteCredito(new BigDecimal("99999"));

        Cliente resultado = ambito.prepararEdicion(solicitud);

        assertSame(existente, resultado);
        assertEquals("Nueva dirección", resultado.getDireccion());
        assertEquals("Ana Perez", resultado.getContacto());
        assertEquals("5555-0000", resultado.getTelefonoContacto());
        assertEquals("Frente al parque", resultado.getReferenciaUbicacion());
        assertEquals("Libreria Uno", resultado.getNombre());
        assertEquals("4514627-6", resultado.getNit());
        assertEquals(CategoriaClienteEnum.B, resultado.getCategoriaDescuento());
        assertEquals(new BigDecimal("1000"), resultado.getLimiteCredito());
        assertEquals(1, resultado.getRuta().getIdRuta());
    }

    @Test
    void elVendedorNoModificaClientesDeOtraRutaNiInexistentes() {
        iniciarSesion("ROLE_VENDEDOR");
        when(clienteRepository.findById(2)).thenReturn(Optional.of(cliente(2, 5)));
        when(clienteRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> ambito.prepararEdicion(cliente(2, 5)));
        assertThrows(NotFoundException.class, () -> ambito.prepararEdicion(cliente(99, 1)));
        assertThrows(NotFoundException.class, () -> ambito.prepararEdicion(cliente(null, 1)));
    }

    // ---------- proformas ----------

    @Test
    void elVendedorUsaSiempreSuPropioIdComoUsuarioDelListado() {
        iniciarSesion("ROLE_VENDEDOR");

        assertEquals(ID_VENDEDOR, ambito.forzarUsuario(null));
        assertEquals(ID_VENDEDOR, ambito.forzarUsuario(1234));
    }

    @Test
    void soloVeSusPropiasProformas() {
        iniciarSesion("ROLE_VENDEDOR");

        ambito.exigirProformaPropia(proforma(1L, ID_VENDEDOR));
        assertThrows(NotFoundException.class, () -> ambito.exigirProformaPropia(proforma(2L, 1234)));
        assertThrows(NotFoundException.class, () -> ambito.exigirProformaPropia(null));
    }

    @Test
    void noModificaUnaProformaYaFacturada() {
        iniciarSesion("ROLE_VENDEDOR");
        Proforma facturada = proforma(3L, ID_VENDEDOR);
        when(facturaRepository.findVigenteByProformaOrigen(3L)).thenReturn(Optional.of(new Factura()));
        when(facturaRepository.findVigenteByProformaOrigen(4L)).thenReturn(Optional.empty());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> ambito.exigirProformaSinFactura(facturada));
        assertTrue(error.getMessage().contains("ya fue facturada"));
        ambito.exigirProformaSinFactura(proforma(4L, ID_VENDEDOR));
    }

    @Test
    void laProformaDelVendedorQuedaASuNombreYSoloVaAUnClienteDeSusRutas() {
        iniciarSesion("ROLE_VENDEDOR");
        when(clienteRepository.findById(1)).thenReturn(Optional.of(cliente(1, 2)));
        when(clienteRepository.findById(2)).thenReturn(Optional.of(cliente(2, 5)));
        Proforma propia = proforma(null, 1234);
        propia.setCliente(cliente(1, null));

        ambito.prepararProforma(propia);

        assertEquals(ID_VENDEDOR, propia.getUsuario().getIdUsuario());
        Proforma aOtraRuta = proforma(null, ID_VENDEDOR);
        aOtraRuta.setCliente(cliente(2, null));
        assertThrows(ReglaNegocioException.class, () -> ambito.prepararProforma(aOtraRuta));
    }

    // ---------- facturas ----------

    private Factura factura(Long idProformaOrigen, Integer idCliente) {
        Factura factura = new Factura();
        factura.setIdProformaOrigen(idProformaOrigen);
        factura.setCliente(cliente(idCliente, null));
        factura.setUsuario(Usuario.builder().idUsuario(1234).build());
        return factura;
    }

    @Test
    void elVendedorSoloFacturaDesdeUnaProformaSuya() {
        iniciarSesion("ROLE_VENDEDOR");
        when(clienteRepository.findById(1)).thenReturn(Optional.of(cliente(1, 1)));
        when(proformaRepository.findById(10L)).thenReturn(Optional.of(proforma(10L, ID_VENDEDOR)));
        when(proformaRepository.findById(11L)).thenReturn(Optional.of(proforma(11L, 1234)));

        Factura propia = factura(10L, 1);
        ambito.prepararFactura(propia);
        assertEquals(ID_VENDEDOR, propia.getUsuario().getIdUsuario());
        assertEquals("Bryan", propia.getUsuario().getPrimerNombre());

        assertThrows(ReglaNegocioException.class, () -> ambito.prepararFactura(factura(null, 1)));
        assertThrows(NotFoundException.class, () -> ambito.prepararFactura(factura(11L, 1)));
        assertThrows(NotFoundException.class, () -> ambito.prepararFactura(factura(12L, 1)));
    }

    @Test
    void laFacturaDelVendedorSoloVaAUnClienteDeSusRutas() {
        iniciarSesion("ROLE_VENDEDOR");
        when(proformaRepository.findById(10L)).thenReturn(Optional.of(proforma(10L, ID_VENDEDOR)));
        when(clienteRepository.findById(2)).thenReturn(Optional.of(cliente(2, 5)));

        assertThrows(ReglaNegocioException.class, () -> ambito.prepararFactura(factura(10L, 2)));
    }

    // ---------- usuario ----------

    @Test
    void elVendedorSoloConsultaSuPropioUsuario() {
        iniciarSesion("ROLE_VENDEDOR");

        ambito.exigirUsuarioPropio(ID_VENDEDOR);
        assertThrows(NotFoundException.class, () -> ambito.exigirUsuarioPropio(1234));
    }
}
