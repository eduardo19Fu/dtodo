package xyz.pangosoft.dtodo.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import xyz.pangosoft.dtodo.dto.DescuentoProductoDto;
import xyz.pangosoft.dtodo.dto.GuardarDescuentosProductoRequest;
import xyz.pangosoft.dtodo.dto.OpcionesDescuentoDto;
import xyz.pangosoft.dtodo.dto.ProductoDescuentosDto;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.DescuentoProducto;
import xyz.pangosoft.dtodo.model.DetalleProforma;
import xyz.pangosoft.dtodo.model.MarcaProducto;
import xyz.pangosoft.dtodo.model.NivelDescuentoCategoria;
import xyz.pangosoft.dtodo.model.PrecioMayorista;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.TipoProducto;
import xyz.pangosoft.dtodo.model.enums.CategoriaClienteEnum;
import xyz.pangosoft.dtodo.model.enums.TipoClienteEnum;
import xyz.pangosoft.dtodo.model.enums.TipoDescuentoEnum;
import xyz.pangosoft.dtodo.service.IDescuentoService;

/** Ejecuta contra H2 las consultas y el mapeo de los descuentos y del precio mayorista. */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("context-test")
@Transactional
class DescuentoRepositoryTest {

    @Autowired
    private IDescuentoService descuentoService;
    @Autowired
    private IDescuentoProductoRepository descuentoRepository;
    @Autowired
    private INivelDescuentoCategoriaRepository nivelRepository;
    @Autowired
    private IPrecioMayoristaRepository precioRepository;
    @Autowired
    private EntityManager em;

    private MarcaProducto marca;
    private TipoProducto tipo;

    private Producto producto(String codigo, String nombre, String precioVenta) {
        if (marca == null) {
            marca = MarcaProducto.builder().marca("MARCA").build();
            tipo = TipoProducto.builder().tipoProducto("TIPO").build();
            em.persist(marca);
            em.persist(tipo);
        }
        Producto producto = Producto.builder().codProducto(codigo).nombre(nombre).precioVenta(new BigDecimal(precioVenta))
                .precioCompra(new BigDecimal("1.00")).marcaProducto(marca).tipoProducto(tipo).build();
        em.persist(producto);
        return producto;
    }

    private void escenario() {
        Producto resma = producto("R1", "RESMA CARTA", "50.00");
        Producto lapiz = producto("L1", "LAPIZ HB", "2.00");
        producto("C1", "CUADERNO", "10.00");
        em.persist(DescuentoProducto.builder().producto(resma).tipo(TipoDescuentoEnum.CIENTO).cantidadMinima(100)
                .porcentaje(new BigDecimal("12.00")).activo(true).build());
        em.persist(DescuentoProducto.builder().producto(resma).tipo(TipoDescuentoEnum.FARDO).cantidadMinima(24)
                .porcentaje(new BigDecimal("15.00")).activo(true).build());
        em.persist(PrecioMayorista.builder().idProducto(lapiz.getIdProducto()).precio(new BigDecimal("1.50")).build());
        em.flush();
        em.clear();
    }

    @Test
    void laPantallaListaTodosLosProductosOSoloLosQueTienenDescuentos() {
        escenario();

        Page<ProductoDescuentosDto> todos = descuentoService.findProductos("", false, PageRequest.of(0, 10));
        Page<ProductoDescuentosDto> conDescuentos = descuentoService.findProductos("", true, PageRequest.of(0, 10, Sort.by("nombre")));

        assertEquals(3, todos.getTotalElements());
        List<String> nombres = conDescuentos.getContent().stream().map(ProductoDescuentosDto::getNombre).collect(Collectors.toList());
        assertEquals(Arrays.asList("LAPIZ HB", "RESMA CARTA"), nombres);
    }

    @Test
    void laBusquedaPorNombreOCodigoTraeLosDescuentosYElPrecioMayorista() {
        escenario();

        ProductoDescuentosDto resma = descuentoService.findProductos("resma", false, PageRequest.of(0, 10)).getContent().get(0);
        ProductoDescuentosDto lapiz = descuentoService.findProductos("l1", false, PageRequest.of(0, 10)).getContent().get(0);

        assertEquals(2, resma.getDescuentos().size());
        assertEquals(TipoDescuentoEnum.CIENTO, resma.getDescuentos().get(0).getTipo());
        assertEquals(100, resma.getDescuentos().get(0).getCantidadMinima());
        assertEquals(0, new BigDecimal("1.50").compareTo(lapiz.getPrecioMayorista()));
        assertTrue(lapiz.getDescuentos().isEmpty());
    }

    @Test
    void guardarUnProductoPersisteElPrecioMayoristaYReemplazaLosDescuentos() {
        escenario();
        Integer idResma = descuentoService.findProductos("resma", false, PageRequest.of(0, 1)).getContent().get(0).getIdProducto();

        ProductoDescuentosDto guardado = descuentoService.guardarProducto(idResma, new GuardarDescuentosProductoRequest(
                new BigDecimal("40"), Arrays.asList(new DescuentoProductoDto(TipoDescuentoEnum.FARDO, 30, new BigDecimal("18"), true))));
        em.flush();
        em.clear();

        assertEquals(0, new BigDecimal("40").compareTo(guardado.getPrecioMayorista()));
        assertEquals(1, guardado.getDescuentos().size());
        assertEquals(TipoDescuentoEnum.FARDO, guardado.getDescuentos().get(0).getTipo());
        assertEquals(1, descuentoRepository.findByProducto_IdProductoOrderByTipo(idResma).size());
        assertTrue(precioRepository.findById(idResma).isPresent());
    }

    @Test
    void lasOpcionesSalenDeLasTablasConfiguradas() {
        escenario();
        nivelRepository.save(NivelDescuentoCategoria.builder().categoria(CategoriaClienteEnum.VOLUMEN).porcentaje(new BigDecimal("12.00")).activo(true).build());
        nivelRepository.save(NivelDescuentoCategoria.builder().categoria(CategoriaClienteEnum.VOLUMEN).porcentaje(new BigDecimal("20.00")).activo(true).build());
        nivelRepository.save(NivelDescuentoCategoria.builder().categoria(CategoriaClienteEnum.VOLUMEN).porcentaje(new BigDecimal("15.00")).activo(false).build());
        Cliente cliente = Cliente.builder().nombre("Libreria Uno").tipoCliente(TipoClienteEnum.MAYORISTA)
                .categoriaDescuento(CategoriaClienteEnum.VOLUMEN).build();
        em.persist(cliente);
        em.flush();
        em.clear();
        Integer idResma = descuentoService.findProductos("resma", false, PageRequest.of(0, 1)).getContent().get(0).getIdProducto();

        OpcionesDescuentoDto opciones = descuentoService.calcularOpciones(idResma, cliente.getIdCliente(), 120);

        List<TipoDescuentoEnum> tipos = opciones.getOpciones().stream().map(o -> o.getTipo()).collect(Collectors.toList());
        assertEquals(Arrays.asList(TipoDescuentoEnum.CATEGORIA, TipoDescuentoEnum.CIENTO, TipoDescuentoEnum.FARDO,
                TipoDescuentoEnum.MANUAL), tipos);
        assertEquals(Arrays.asList(new BigDecimal("12.00"), new BigDecimal("20.00")),
                opciones.getOpciones().get(0).getNiveles());
        assertEquals(TipoDescuentoEnum.FARDO, opciones.getSugerida().getTipo());
        assertTrue(opciones.getOpciones().get(2).isAlcanzaMinimo());
    }

    @Test
    void laLineaDeProformaGuardaTipoPrecioUnitarioYPorcentajeSugerido() {
        escenario();
        Integer idResma = descuentoService.findProductos("resma", false, PageRequest.of(0, 1)).getContent().get(0).getIdProducto();
        Cliente cliente = Cliente.builder().nombre("Libreria Uno").tipoCliente(TipoClienteEnum.MAYORISTA).build();
        em.persist(cliente);
        DetalleProforma linea = new DetalleProforma();
        linea.setProducto(Producto.builder().idProducto(idResma).build());
        linea.setCantidad(120);
        linea.setDescuento(12f);
        linea.setTipoDescuento(TipoDescuentoEnum.CIENTO);
        Proforma proforma = new Proforma();
        proforma.setCliente(cliente);
        proforma.setItemsProforma(new java.util.ArrayList<>(Arrays.asList(linea)));

        descuentoService.prepararLineas(proforma);
        em.persist(linea);
        em.flush();
        em.clear();

        DetalleProforma leida = em.find(DetalleProforma.class, linea.getIdDetalle());
        assertEquals(TipoDescuentoEnum.CIENTO, leida.getTipoDescuento());
        assertEquals(0, new BigDecimal("50.00").compareTo(leida.getPrecioUnitario()));
        // Con 120 unidades el sistema sugería el fardo (15 %); el usuario eligió el ciento (12 %).
        assertEquals(0, new BigDecimal("15.00").compareTo(leida.getPorcentajeSugerido()));
    }
}
