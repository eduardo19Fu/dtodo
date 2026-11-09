package xyz.pangosoft.dtodo.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.pangosoft.dtodo.dto.CategoriaNivelesDto;
import xyz.pangosoft.dtodo.dto.DescuentoProductoDto;
import xyz.pangosoft.dtodo.dto.GuardarDescuentosProductoRequest;
import xyz.pangosoft.dtodo.dto.OpcionDescuentoDto;
import xyz.pangosoft.dtodo.dto.OpcionesDescuentoDto;
import xyz.pangosoft.dtodo.dto.ProductoDescuentosDto;
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
import xyz.pangosoft.dtodo.service.IDescuentoService;

@Service
@RequiredArgsConstructor
@Slf4j
public class DescuentoServiceImpl implements IDescuentoService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    /** Diferencia admitida entre el porcentaje que envía la pantalla (redondeado) y el configurado. */
    static final BigDecimal TOLERANCIA = new BigDecimal("0.01");

    /** Tipos que solo corresponden a un cliente mayorista. */
    private static final Set<TipoDescuentoEnum> TIPOS_MAYORISTA = EnumSet.of(TipoDescuentoEnum.CATEGORIA,
            TipoDescuentoEnum.CIENTO, TipoDescuentoEnum.FARDO, TipoDescuentoEnum.PRECIO_MAYORISTA);

    private final INivelDescuentoCategoriaRepository nivelRepository;
    private final IDescuentoProductoRepository descuentoRepository;
    private final IPrecioMayoristaRepository precioRepository;
    private final IProductoRepository productoRepository;
    private final IClienteRepository clienteRepository;

    // ------------------------------------------------------------------ opciones

    @Transactional(readOnly = true)
    @Override
    public OpcionesDescuentoDto calcularOpciones(Integer idProducto, Integer idCliente, int cantidad) {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new NotFoundException("El producto con ID " + idProducto + " no se encuentra registrado."));
        Cliente cliente = idCliente == null ? null : clienteRepository.findById(idCliente).orElse(null);
        return construirOpciones(producto, cliente, cantidad);
    }

    private OpcionesDescuentoDto construirOpciones(Producto producto, Cliente cliente, int cantidad) {
        boolean mayorista = cliente != null && cliente.getTipoCliente() == TipoClienteEnum.MAYORISTA;
        CategoriaClienteEnum categoria = mayorista ? cliente.getCategoriaDescuento() : null;
        BigDecimal precioVenta = producto.getPrecioVenta();
        BigDecimal precioMayorista = precioRepository.findById(producto.getIdProducto())
                .map(PrecioMayorista::getPrecio).orElse(null);

        List<OpcionDescuentoDto> opciones = new ArrayList<>();
        if (mayorista) {
            agregarCategoria(opciones, categoria);
            agregarEspeciales(opciones, producto.getIdProducto(), cantidad);
            agregarPrecioMayorista(opciones, precioVenta, precioMayorista);
        }
        opciones.add(OpcionDescuentoDto.builder().tipo(TipoDescuentoEnum.MANUAL).etiqueta("Descuento manual")
                .niveles(new ArrayList<>()).alcanzaMinimo(true).build());

        return OpcionesDescuentoDto.builder().idProducto(producto.getIdProducto()).precioVenta(precioVenta)
                .precioMayorista(precioMayorista).categoria(categoria).cantidad(cantidad).opciones(opciones)
                .sugerida(elegirSugerida(opciones)).build();
    }

    private void agregarCategoria(List<OpcionDescuentoDto> opciones, CategoriaClienteEnum categoria) {
        if (categoria == null) {
            return;
        }
        List<BigDecimal> niveles = nivelRepository.findByCategoriaAndActivoTrueOrderByPorcentajeAsc(categoria).stream()
                .map(NivelDescuentoCategoria::getPorcentaje).collect(Collectors.toList());
        if (niveles.isEmpty()) {
            return;
        }
        boolean unico = niveles.size() == 1;
        opciones.add(OpcionDescuentoDto.builder().tipo(TipoDescuentoEnum.CATEGORIA)
                .etiqueta("Categoría " + categoria + (unico ? " (" + texto(niveles.get(0)) + " %)" : ": elegir nivel"))
                .porcentaje(unico ? niveles.get(0) : null).niveles(unico ? new ArrayList<>() : niveles)
                .alcanzaMinimo(true).build());
    }

    private void agregarEspeciales(List<OpcionDescuentoDto> opciones, Integer idProducto, int cantidad) {
        descuentoRepository.findByProducto_IdProductoOrderByTipo(idProducto).stream()
                .filter(DescuentoProducto::isActivo).forEach(descuento -> {
                    boolean fardo = descuento.getTipo() == TipoDescuentoEnum.FARDO;
                    String que = fardo ? "Fardo cerrado" : "Por ciento";
                    opciones.add(OpcionDescuentoDto.builder().tipo(descuento.getTipo())
                            .etiqueta(que + ": " + texto(descuento.getPorcentaje()) + " % desde "
                                    + descuento.getCantidadMinima() + " unidades")
                            .porcentaje(descuento.getPorcentaje()).niveles(new ArrayList<>())
                            .cantidadMinima(descuento.getCantidadMinima())
                            .alcanzaMinimo(cantidad >= descuento.getCantidadMinima()).build());
                });
    }

    private void agregarPrecioMayorista(List<OpcionDescuentoDto> opciones, BigDecimal precioVenta, BigDecimal precioMayorista) {
        if (!aplicaPrecioMayorista(precioVenta, precioMayorista)) {
            return;
        }
        opciones.add(OpcionDescuentoDto.builder().tipo(TipoDescuentoEnum.PRECIO_MAYORISTA)
                .etiqueta("Precio mayorista: Q" + precioMayorista.setScale(2, RoundingMode.HALF_UP).toPlainString())
                .porcentaje(porcentajeEquivalente(precioVenta, precioMayorista)).niveles(new ArrayList<>())
                .alcanzaMinimo(true).precio(precioMayorista).build());
    }

    /**
     * Sugerencia del sistema: el precio mayorista fijo, si el producto lo tiene (lo definió el administrador); si
     * no, el mayor porcentaje entre las opciones que ya tienen su porcentaje y cuya cantidad mínima se alcanza. La
     * categoría con varios niveles (Volumen) no se sugiere: el vendedor elige el nivel.
     */
    static OpcionDescuentoDto elegirSugerida(List<OpcionDescuentoDto> opciones) {
        Optional<OpcionDescuentoDto> fijo = opciones.stream()
                .filter(opcion -> opcion.getTipo() == TipoDescuentoEnum.PRECIO_MAYORISTA).findFirst();
        if (fijo.isPresent()) {
            return fijo.get();
        }
        return opciones.stream()
                .filter(opcion -> opcion.getTipo() != TipoDescuentoEnum.MANUAL)
                .filter(opcion -> opcion.getPorcentaje() != null && opcion.isAlcanzaMinimo())
                .max(Comparator.comparing(OpcionDescuentoDto::getPorcentaje)
                        .thenComparing(opcion -> prioridad(opcion.getTipo())))
                .orElse(null);
    }

    /** En un empate de porcentaje se prefiere lo más específico: fardo, ciento y, al final, la categoría. */
    private static int prioridad(TipoDescuentoEnum tipo) {
        switch (tipo) {
            case FARDO: return 3;
            case CIENTO: return 2;
            default: return 1;
        }
    }

    // ------------------------------------------------------------------ administración

    @Transactional(readOnly = true)
    @Override
    public List<CategoriaNivelesDto> findCategorias() {
        return java.util.Arrays.stream(CategoriaClienteEnum.values())
                .map(categoria -> new CategoriaNivelesDto(categoria, porcentajesActivos(categoria)))
                .collect(Collectors.toList());
    }

    private List<BigDecimal> porcentajesActivos(CategoriaClienteEnum categoria) {
        return nivelRepository.findByCategoriaAndActivoTrueOrderByPorcentajeAsc(categoria).stream()
                .map(NivelDescuentoCategoria::getPorcentaje).collect(Collectors.toList());
    }

    @Transactional
    @Override
    public CategoriaNivelesDto guardarNiveles(CategoriaClienteEnum categoria, List<BigDecimal> porcentajes) {
        Set<BigDecimal> nuevos = new TreeSet<>();
        for (BigDecimal porcentaje : porcentajes == null ? new ArrayList<BigDecimal>() : porcentajes) {
            validarPorcentaje(porcentaje, "El porcentaje de la categoría " + categoria);
            nuevos.add(porcentaje.setScale(2, RoundingMode.HALF_UP));
        }

        List<NivelDescuentoCategoria> existentes = nivelRepository.findByCategoriaOrderByPorcentajeAsc(categoria);
        Set<BigDecimal> yaExisten = new TreeSet<>();
        for (NivelDescuentoCategoria nivel : existentes) {
            yaExisten.add(nivel.getPorcentaje());
            nivel.setActivo(nuevos.contains(nivel.getPorcentaje()));
        }
        nivelRepository.saveAll(existentes);
        for (BigDecimal porcentaje : nuevos) {
            if (!yaExisten.contains(porcentaje)) {
                nivelRepository.save(NivelDescuentoCategoria.builder().categoria(categoria).porcentaje(porcentaje).activo(true).build());
            }
        }
        log.info("Niveles de la categoría {} actualizados: {}", categoria, nuevos);
        return new CategoriaNivelesDto(categoria, new ArrayList<>(nuevos));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ProductoDescuentosDto> findProductos(String filtro, boolean soloConDescuentos, Pageable pageable) {
        String criterio = filtro == null ? "" : filtro.trim();
        Page<Producto> productos = descuentoRepository.buscarProductos(criterio, soloConDescuentos, pageable);
        List<Integer> ids = productos.getContent().stream().map(Producto::getIdProducto).collect(Collectors.toList());

        Map<Integer, List<DescuentoProducto>> descuentos = new HashMap<>();
        Map<Integer, BigDecimal> precios = new HashMap<>();
        if (!ids.isEmpty()) {
            descuentoRepository.findByProducto_IdProductoIn(ids).forEach(descuento ->
                    descuentos.computeIfAbsent(descuento.getProducto().getIdProducto(), k -> new ArrayList<>()).add(descuento));
            precioRepository.findByIdProductoIn(ids).forEach(precio -> precios.put(precio.getIdProducto(), precio.getPrecio()));
        }
        return productos.map(producto -> aDto(producto, precios.get(producto.getIdProducto()),
                descuentos.getOrDefault(producto.getIdProducto(), new ArrayList<>())));
    }

    @Transactional(readOnly = true)
    @Override
    public void completarResumenMayorista(List<xyz.pangosoft.dtodo.dto.ProductoDtoMejorado> productos) {
        if (productos == null || productos.isEmpty()) {
            return;
        }
        List<Integer> ids = productos.stream().map(producto -> producto.getIdProducto()).collect(Collectors.toList());
        Map<Integer, BigDecimal> precios = new HashMap<>();
        precioRepository.findByIdProductoIn(ids).forEach(precio -> precios.put(precio.getIdProducto(), precio.getPrecio()));
        Map<Integer, Set<TipoDescuentoEnum>> especiales = new HashMap<>();
        descuentoRepository.findByProducto_IdProductoIn(ids).stream().filter(DescuentoProducto::isActivo)
                .forEach(descuento -> especiales.computeIfAbsent(descuento.getProducto().getIdProducto(),
                        k -> EnumSet.noneOf(TipoDescuentoEnum.class)).add(descuento.getTipo()));
        for (xyz.pangosoft.dtodo.dto.ProductoDtoMejorado producto : productos) {
            producto.setPrecioMayorista(precios.get(producto.getIdProducto()));
            Set<TipoDescuentoEnum> tipos = especiales.getOrDefault(producto.getIdProducto(), EnumSet.noneOf(TipoDescuentoEnum.class));
            producto.setDescuentoCiento(tipos.contains(TipoDescuentoEnum.CIENTO));
            producto.setDescuentoFardo(tipos.contains(TipoDescuentoEnum.FARDO));
        }
    }

    @Transactional(readOnly = true)
    @Override
    public ProductoDescuentosDto findProducto(Integer idProducto) {
        Producto producto = cargarProducto(idProducto);
        return aDto(producto, precioRepository.findById(idProducto).map(PrecioMayorista::getPrecio).orElse(null),
                descuentoRepository.findByProducto_IdProductoOrderByTipo(idProducto));
    }

    @Transactional
    @Override
    public ProductoDescuentosDto guardarProducto(Integer idProducto, GuardarDescuentosProductoRequest solicitud) {
        Producto producto = cargarProducto(idProducto);
        guardarPrecioMayorista(producto, solicitud.getPrecioMayorista());
        guardarEspeciales(producto, solicitud.getDescuentos() == null ? new ArrayList<>() : solicitud.getDescuentos());
        return findProducto(idProducto);
    }

    private void guardarPrecioMayorista(Producto producto, BigDecimal precio) {
        if (precio == null) {
            precioRepository.findById(producto.getIdProducto()).ifPresent(precioRepository::delete);
            return;
        }
        if (precio.signum() <= 0) {
            throw new ReglaNegocioException("El precio mayorista debe ser mayor a cero.");
        }
        if (producto.getPrecioVenta() != null && precio.compareTo(producto.getPrecioVenta()) >= 0) {
            throw new ReglaNegocioException("El precio mayorista (Q" + precio.toPlainString()
                    + ") debe ser menor al precio de venta (Q" + producto.getPrecioVenta().toPlainString() + ").");
        }
        precioRepository.save(PrecioMayorista.builder().idProducto(producto.getIdProducto())
                .precio(precio.setScale(2, RoundingMode.HALF_UP)).build());
    }

    private void guardarEspeciales(Producto producto, List<DescuentoProductoDto> solicitados) {
        Map<TipoDescuentoEnum, DescuentoProductoDto> porTipo = new HashMap<>();
        for (DescuentoProductoDto dto : solicitados) {
            if (dto.getTipo() != TipoDescuentoEnum.CIENTO && dto.getTipo() != TipoDescuentoEnum.FARDO) {
                throw new ReglaNegocioException("Solo se configuran descuentos por ciento o por fardo cerrado.");
            }
            if (porTipo.put(dto.getTipo(), dto) != null) {
                throw new ReglaNegocioException("Cada producto admite un solo descuento por " + dto.getTipo().name().toLowerCase() + ".");
            }
            if (dto.getCantidadMinima() == null || dto.getCantidadMinima() < 2) {
                throw new ReglaNegocioException("La cantidad mínima del descuento por " + dto.getTipo().name().toLowerCase()
                        + " debe ser de 2 unidades o más.");
            }
            validarPorcentaje(dto.getPorcentaje(), "El porcentaje del descuento por " + dto.getTipo().name().toLowerCase());
        }

        List<DescuentoProducto> existentes = descuentoRepository.findByProducto_IdProductoOrderByTipo(producto.getIdProducto());
        for (DescuentoProducto existente : existentes) {
            DescuentoProductoDto dto = porTipo.remove(existente.getTipo());
            if (dto == null) {
                descuentoRepository.delete(existente);
            } else {
                existente.setCantidadMinima(dto.getCantidadMinima());
                existente.setPorcentaje(dto.getPorcentaje().setScale(2, RoundingMode.HALF_UP));
                existente.setActivo(dto.isActivo());
                descuentoRepository.save(existente);
            }
        }
        for (DescuentoProductoDto dto : porTipo.values()) {
            descuentoRepository.save(DescuentoProducto.builder().producto(producto).tipo(dto.getTipo())
                    .cantidadMinima(dto.getCantidadMinima()).porcentaje(dto.getPorcentaje().setScale(2, RoundingMode.HALF_UP))
                    .activo(dto.isActivo()).build());
        }
    }

    private ProductoDescuentosDto aDto(Producto producto, BigDecimal precioMayorista, List<DescuentoProducto> descuentos) {
        List<DescuentoProductoDto> filas = descuentos.stream()
                .map(d -> new DescuentoProductoDto(d.getTipo(), d.getCantidadMinima(), d.getPorcentaje(), d.isActivo()))
                .collect(Collectors.toList());
        return new ProductoDescuentosDto(producto.getIdProducto(), producto.getCodProducto(), producto.getNombre(),
                producto.getPrecioVenta(), precioMayorista, filas);
    }

    private Producto cargarProducto(Integer idProducto) {
        return productoRepository.findById(idProducto)
                .orElseThrow(() -> new NotFoundException("El producto con ID " + idProducto + " no se encuentra registrado."));
    }

    private static void validarPorcentaje(BigDecimal porcentaje, String que) {
        if (porcentaje == null || porcentaje.signum() <= 0 || porcentaje.compareTo(CIEN) > 0) {
            throw new ReglaNegocioException(que + " debe ser mayor a 0 y no pasar de 100.");
        }
    }

    // ------------------------------------------------------------------ líneas de proforma

    @Transactional(readOnly = true)
    @Override
    public void prepararLineas(Proforma proforma) {
        if (proforma.getItemsProforma() == null) {
            return;
        }
        Cliente cliente = proforma.getCliente() == null || proforma.getCliente().getIdCliente() == null ? null
                : clienteRepository.findById(proforma.getCliente().getIdCliente()).orElse(null);

        for (DetalleProforma linea : proforma.getItemsProforma()) {
            if (linea == null || linea.getProducto() == null || linea.getProducto().getIdProducto() == null) {
                continue;
            }
            Producto producto = cargarProductoDeLinea(linea);
            BigDecimal porcentaje = linea.getDescuento() == null ? BigDecimal.ZERO
                    : new BigDecimal(String.valueOf(linea.getDescuento()));
            TipoDescuentoEnum tipo = linea.getTipoDescuento() == null ? TipoDescuentoEnum.MANUAL : linea.getTipoDescuento();

            OpcionesDescuentoDto opciones = construirOpciones(producto, cliente, linea.getCantidad() == null ? 0 : linea.getCantidad());
            validarLinea(producto, tipo, porcentaje, cliente, opciones);

            linea.setTipoDescuento(tipo);
            linea.setPrecioUnitario(precioUnitario(linea, producto));
            linea.setPorcentajeSugerido(opciones.getSugerida() == null ? null : opciones.getSugerida().getPorcentaje());
        }
    }

    private Producto cargarProductoDeLinea(DetalleProforma linea) {
        return productoRepository.findById(linea.getProducto().getIdProducto())
                .orElseThrow(() -> new ReglaNegocioException("Un producto de la proforma ya no existe."));
    }

    /** El tipo elegido debe ser coherente con el porcentaje: así el tipo registrado es verdadero para los reportes. */
    private void validarLinea(Producto producto, TipoDescuentoEnum tipo, BigDecimal porcentaje, Cliente cliente,
                              OpcionesDescuentoDto opciones) {
        String linea = "Producto " + producto.getNombre() + ": ";
        if (porcentaje.signum() < 0 || porcentaje.compareTo(CIEN) > 0) {
            throw new ReglaNegocioException(linea + "el descuento debe estar entre 0 y 100 %.");
        }
        if (tipo == TipoDescuentoEnum.MANUAL) {
            return;
        }
        if (TIPOS_MAYORISTA.contains(tipo)
                && (cliente == null || cliente.getTipoCliente() != TipoClienteEnum.MAYORISTA)) {
            throw new ReglaNegocioException(linea + "el descuento " + tipo + " solo aplica a clientes mayoristas.");
        }

        if (tipo == TipoDescuentoEnum.CATEGORIA) {
            OpcionDescuentoDto categoria = buscarOpcion(opciones, tipo);
            if (categoria == null) {
                throw new ReglaNegocioException(linea + "el cliente no tiene una categoría con descuento.");
            }
            List<BigDecimal> permitidos = categoria.getPorcentaje() != null
                    ? java.util.Collections.singletonList(categoria.getPorcentaje()) : categoria.getNiveles();
            if (permitidos.stream().noneMatch(nivel -> coincide(nivel, porcentaje))) {
                throw new ReglaNegocioException(linea + "el " + texto(porcentaje) + " % no es un nivel de la categoría "
                        + opciones.getCategoria() + " del cliente.");
            }
            return;
        }

        OpcionDescuentoDto opcion = buscarOpcion(opciones, tipo);
        if (opcion == null) {
            throw new ReglaNegocioException(linea + "el producto no tiene configurado el descuento " + tipo + ".");
        }
        if (!coincide(opcion.getPorcentaje(), porcentaje)) {
            throw new ReglaNegocioException(linea + "el descuento " + tipo + " es de " + texto(opcion.getPorcentaje())
                    + " % y la línea trae " + texto(porcentaje) + " %.");
        }
    }

    private static OpcionDescuentoDto buscarOpcion(OpcionesDescuentoDto opciones, TipoDescuentoEnum tipo) {
        return opciones.getOpciones().stream().filter(opcion -> opcion.getTipo() == tipo).findFirst().orElse(null);
    }

    private static boolean coincide(BigDecimal configurado, BigDecimal recibido) {
        return configurado.subtract(recibido).abs().compareTo(TOLERANCIA) <= 0;
    }

    /**
     * Precio unitario de la línea. Una línea nueva toma el precio de venta de hoy; una existente conserva el que
     * ya tenía o, si es anterior a este cambio, se despeja de su subtotal (que en proformas es el importe bruto).
     */
    private static BigDecimal precioUnitario(DetalleProforma linea, Producto producto) {
        if (linea.getIdDetalle() != null) {
            if (linea.getPrecioUnitario() != null) {
                return linea.getPrecioUnitario();
            }
            if (linea.getSubTotal() != null && linea.getCantidad() != null && linea.getCantidad() > 0) {
                return linea.getSubTotal().divide(BigDecimal.valueOf(linea.getCantidad()), 2, RoundingMode.HALF_UP);
            }
        }
        return producto.getPrecioVenta() == null ? null : producto.getPrecioVenta().setScale(2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------ cálculos

    /** Un precio mayorista solo vale si es positivo y menor al precio de venta. */
    static boolean aplicaPrecioMayorista(BigDecimal precioVenta, BigDecimal precioMayorista) {
        return precioVenta != null && precioMayorista != null && precioMayorista.signum() > 0
                && precioMayorista.compareTo(precioVenta) < 0;
    }

    /** Porcentaje de descuento sobre el precio de venta que equivale a vender al precio mayorista. */
    static BigDecimal porcentajeEquivalente(BigDecimal precioVenta, BigDecimal precioMayorista) {
        return BigDecimal.ONE.subtract(precioMayorista.divide(precioVenta, 8, RoundingMode.HALF_UP))
                .multiply(CIEN).setScale(4, RoundingMode.HALF_UP);
    }

    private static String texto(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }
}
