package xyz.pangosoft.dtodo.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import xyz.pangosoft.dtodo.dto.ClienteDto;
import xyz.pangosoft.dtodo.dto.ClienteFiltro;
import xyz.pangosoft.dtodo.dto.RutaDto;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.Role;
import xyz.pangosoft.dtodo.model.Ruta;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.CategoriaClienteEnum;
import xyz.pangosoft.dtodo.model.enums.TipoClienteEnum;
import xyz.pangosoft.dtodo.service.IClienteService;

/** Ejecuta contra H2 las consultas de rutas y de clientes mayoristas. */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("context-test")
@Transactional
class RutaClienteRepositoryTest {

    @Autowired
    private IRutaRepository rutaRepository;
    @Autowired
    private IClienteRepository clienteRepository;
    @Autowired
    private IClienteService clienteService;
    @Autowired
    private EntityManager em;

    private Estado activo;
    private Estado inactivo;
    private Sucursal central;
    private Ruta ruta1;
    private Ruta ruta2;

    private Ruta ruta(String nombre, String zona, Usuario vendedor, Estado estado) {
        Ruta ruta = new Ruta();
        ruta.setNombre(nombre);
        ruta.setZona(zona);
        ruta.setDiasVisita("Lunes");
        ruta.setVendedor(vendedor);
        ruta.setSucursal(central);
        ruta.setEstado(estado);
        em.persist(ruta);
        return ruta;
    }

    private void escenario() {
        activo = Estado.builder().estado("ACTIVO").build();
        inactivo = Estado.builder().estado("INACTIVO").build();
        em.persist(activo);
        em.persist(inactivo);
        central = Sucursal.builder().nombre("Central").direccion("Jalapa").codigoEstablecimientoSat(1).esPrincipal(true)
                .estado(activo).build();
        em.persist(central);
        Role rolVendedor = Role.builder().idRole(50).role("ROLE_VENDEDOR").build();
        Usuario bryan = Usuario.builder().usuario("bryan").primerNombre("Bryan").apellido("Vargas").password("x").build();
        em.persist(bryan);

        ruta1 = ruta("RUTA 1", "Monjas", bryan, activo);
        ruta2 = ruta("RUTA 3", "San Pedro Pinula", null, inactivo);

        em.persist(Cliente.builder().nombre("Libreria Uno").tipoCliente(TipoClienteEnum.MAYORISTA).ruta(ruta1)
                .categoriaDescuento(CategoriaClienteEnum.VOLUMEN).codigoCliente("55551234").contacto("Ana")
                .limiteCredito(new BigDecimal("2500.50")).diasCredito(30).build());
        em.persist(Cliente.builder().nombre("Tienda Dos").tipoCliente(TipoClienteEnum.MAYORISTA).ruta(ruta1).build());
        em.persist(Cliente.builder().nombre("Cliente de tienda").build());
        em.flush();
        em.clear();
    }

    @Test
    void elListadoDeRutasIncluyeVendedorSucursalEstadoYCantidadDeClientes() {
        escenario();

        Page<RutaDto> pagina = rutaRepository.findListado("", PageRequest.of(0, 10, Sort.by("nombre")));

        assertEquals(2, pagina.getTotalElements());
        RutaDto primera = pagina.getContent().get(0);
        assertEquals("RUTA 1", primera.getNombre());
        assertEquals("Bryan Vargas", primera.getVendedor());
        assertEquals("Central", primera.getSucursal());
        assertEquals("ACTIVO", primera.getEstado());
        assertEquals(2L, primera.getTotalClientes());

        RutaDto sinVendedor = pagina.getContent().get(1);
        assertNull(sinVendedor.getIdVendedor());
        assertEquals(0L, sinVendedor.getTotalClientes());
    }

    @Test
    void elListadoDeRutasFiltraPorTextoYOrdenaPorVendedor() {
        escenario();

        assertEquals(1, rutaRepository.findListado("pinula", PageRequest.of(0, 10)).getTotalElements());
        assertEquals(1, rutaRepository.findListado("bryan", PageRequest.of(0, 10)).getTotalElements());
        Page<RutaDto> ordenado = rutaRepository.findListado("", PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "v.primerNombre")));
        assertEquals(2, ordenado.getTotalElements());
    }

    @Test
    void lasRutasActivasExcluyenLasInactivas() {
        escenario();

        List<RutaDto> activas = rutaRepository.findActivasDto();

        assertEquals(1, activas.size());
        assertEquals("RUTA 1", activas.get(0).getNombre());
    }

    @Test
    void detectaNombresRepetidosSinImportarMayusculas() {
        escenario();

        assertTrue(rutaRepository.existsByNombreIgnoreCaseAndSucursal_IdSucursal("ruta 1", central.getIdSucursal()));
        assertFalse(rutaRepository.existsByNombreIgnoreCaseAndSucursal_IdSucursalAndIdRutaNot(
                "ruta 1", central.getIdSucursal(), ruta1.getIdRuta()));
    }

    @Test
    void elListadoDeClientesTraeTipoCategoriaRutaYCredito() {
        escenario();

        Page<ClienteDto> pagina = clienteService.findListado("", TipoClienteEnum.MAYORISTA, null,
                PageRequest.of(0, 10, Sort.by("nombre")));

        assertEquals(2, pagina.getTotalElements());
        ClienteDto uno = pagina.getContent().get(0);
        assertEquals("Libreria Uno", uno.getNombre());
        assertEquals(TipoClienteEnum.MAYORISTA, uno.getTipoCliente());
        assertEquals(CategoriaClienteEnum.VOLUMEN, uno.getCategoriaDescuento());
        assertEquals("RUTA 1", uno.getRuta());
        assertEquals("55551234", uno.getCodigoCliente());
        assertEquals(0, new BigDecimal("2500.50").compareTo(uno.getLimiteCredito()));
        assertEquals(30, uno.getDiasCredito());
    }

    @Test
    void elListadoFiltraPorRutaYBuscaPorCodigoYContacto() {
        escenario();

        assertEquals(2, clienteService.findListado("", null, ruta1.getIdRuta(), PageRequest.of(0, 10)).getTotalElements());
        assertEquals(0, clienteService.findListado("", null, ruta2.getIdRuta(), PageRequest.of(0, 10)).getTotalElements());
        assertEquals(1, clienteService.findListado("55551234", null, null, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(1, clienteService.findListado("ana", null, null, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(3, clienteService.findListado("", null, null, PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    void lasConsultasDeClientesEnElRepositorioIncluyenLaRuta() {
        escenario();

        List<ClienteDto> todos = clienteRepository.consultarClientesDto();
        ClienteDto minorista = todos.stream().filter(c -> "Cliente de tienda".equals(c.getNombre())).findFirst().get();
        ClienteDto mayorista = todos.stream().filter(c -> "Libreria Uno".equals(c.getNombre())).findFirst().get();

        assertEquals(3, todos.size());
        assertEquals(TipoClienteEnum.MINORISTA, minorista.getTipoCliente());
        assertNull(minorista.getRuta());
        assertEquals("RUTA 1", mayorista.getRuta());
        assertEquals(1, clienteRepository.findListado("uno", PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    void laBusquedaPorCodigoYLaCarteraDeLaRuta() {
        escenario();

        assertTrue(clienteRepository.findByCodigoCliente("55551234").isPresent());
        assertFalse(clienteRepository.findByCodigoCliente("000").isPresent());
        assertEquals(2, clienteRepository.findByRuta_IdRutaOrderByNombre(ruta1.getIdRuta()).size());
        assertEquals(2, clienteRepository.countByRuta_IdRuta(ruta1.getIdRuta()));
    }

    @Test
    void elFiltroDeNitInvalidoSoloDevuelveLosNitEscritosQueNoSonValidos() {
        escenario();
        em.persist(Cliente.builder().nombre("Con NIT valido").nit("4514627-6").build());
        em.persist(Cliente.builder().nombre("Con telefono como NIT").nit("55551234").build());
        em.persist(Cliente.builder().nombre("Consumidor final").nit("C/F").build());
        em.persist(Cliente.builder().nombre("Con digito incorrecto").nit("4514627-5").build());
        em.flush();
        em.clear();

        Page<ClienteDto> invalidos = clienteService.findListado(
                ClienteFiltro.builder().soloNitInvalido(true).build(), PageRequest.of(0, 10, Sort.by("nombre")));

        assertEquals(2, invalidos.getTotalElements());
        assertEquals("Con digito incorrecto", invalidos.getContent().get(0).getNombre());
        assertEquals("Con telefono como NIT", invalidos.getContent().get(1).getNombre());
        assertEquals(2, clienteService.totalNitInvalidos());
    }

    @Test
    void sinClientesConNitInvalidoElFiltroNoDevuelveNada() {
        escenario();

        Page<ClienteDto> invalidos = clienteService.findListado(
                ClienteFiltro.builder().soloNitInvalido(true).build(), PageRequest.of(0, 10));

        assertEquals(0, invalidos.getTotalElements());
        assertEquals(0, clienteService.totalNitInvalidos());
    }

    @Test
    void elListadoPuedeLimitarseALasRutasPermitidas() {
        escenario();

        Page<ClienteDto> deRuta1 = clienteService.findListado(
                ClienteFiltro.builder().idsRutasPermitidas(java.util.Collections.singletonList(ruta1.getIdRuta())).build(),
                PageRequest.of(0, 10));
        Page<ClienteDto> deNinguna = clienteService.findListado(
                ClienteFiltro.builder().idsRutasPermitidas(java.util.Collections.<Integer>emptyList()).build(),
                PageRequest.of(0, 10));
        Page<ClienteDto> deRuta2 = clienteService.findListado(
                ClienteFiltro.builder().idsRutasPermitidas(java.util.Collections.singletonList(ruta2.getIdRuta())).build(),
                PageRequest.of(0, 10));

        assertEquals(2, deRuta1.getTotalElements());
        assertEquals(0, deNinguna.getTotalElements());
        assertEquals(0, deRuta2.getTotalElements());
    }
}
