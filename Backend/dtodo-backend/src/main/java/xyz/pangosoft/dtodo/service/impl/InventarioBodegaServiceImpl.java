package xyz.pangosoft.dtodo.service.impl;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import xyz.pangosoft.dtodo.dto.ImportacionInventarioDto;
import xyz.pangosoft.dtodo.dto.InventarioBodegaDto;
import xyz.pangosoft.dtodo.dto.MovimientoBodegaDto;
import xyz.pangosoft.dtodo.dto.MovimientoBodegaRequest;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.model.Bodega;
import xyz.pangosoft.dtodo.model.InventarioBodega;
import xyz.pangosoft.dtodo.model.MarcaProducto;
import xyz.pangosoft.dtodo.model.MovimientoBodega;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.TipoProducto;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.OrigenInventarioBodegaEnum;
import xyz.pangosoft.dtodo.model.enums.TipoMovimientoBodegaEnum;
import xyz.pangosoft.dtodo.repository.IInventarioBodegaRepository;
import xyz.pangosoft.dtodo.repository.IMovimientoBodegaRepository;
import xyz.pangosoft.dtodo.repository.IProductoRepository;
import xyz.pangosoft.dtodo.service.IBodegaService;
import xyz.pangosoft.dtodo.service.IInventarioBodegaService;
import xyz.pangosoft.dtodo.service.IInventarioSucursalService;
import xyz.pangosoft.dtodo.service.IMarcaProductoService;
import xyz.pangosoft.dtodo.service.IProductoService;
import xyz.pangosoft.dtodo.service.ISucursalService;
import xyz.pangosoft.dtodo.service.ITipoProductoService;
import xyz.pangosoft.dtodo.service.IUsuarioService;
import xyz.pangosoft.dtodo.util.InventarioBodegaExcel;

/**
 * Existencias y movimientos de las bodegas. Todo cambio de stock pasa por
 * {@link #aplicar}, que bloquea la fila de inventario, valida que el saldo no quede en negativo y deja
 * el movimiento registrado en la misma transacción.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventarioBodegaServiceImpl implements IInventarioBodegaService {

	// Sin rango de fechas se consulta todo el histórico: se usan límites amplios en vez de parámetros nulos
	private static final LocalDateTime DESDE_SIN_LIMITE = LocalDateTime.of(2000, 1, 1, 0, 0);
	private static final LocalDateTime HASTA_SIN_LIMITE = LocalDateTime.of(2999, 12, 31, 0, 0);
	private static final int TAMANIO_LOTE_CODIGOS = 1000;
	// Límites de las columnas de la tabla productos
	private static final int LONGITUD_MAXIMA_TEXTO_PRODUCTO = 100;
	private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("99999999.99");
	static final String MARCA_POR_DEFECTO = "SIN MARCA";
	static final String CATEGORIA_POR_DEFECTO = "SIN CATEGORIA";

	private final IInventarioBodegaRepository inventarioRepo;

	private final IMovimientoBodegaRepository movimientoRepo;

	private final IProductoRepository productoRepo;

	private final IBodegaService bodegaService;

	private final IProductoService productoService;

	private final IUsuarioService usuarioService;

	private final ISucursalService sucursalService;

	private final IMarcaProductoService marcaService;

	private final ITipoProductoService tipoService;

	private final IInventarioSucursalService inventarioSucursalService;

	@Transactional(readOnly = true)
	@Override
	public Page<InventarioBodegaDto> findListado(Integer idBodega, String filtro, Pageable pageable) {
		bodegaService.findById(idBodega);
		try {
			return inventarioRepo.findListado(idBodega, filtro == null ? "" : filtro.trim(), pageable);
		} catch (DataAccessException e) {
			log.error("Error al consultar el inventario de la bodega {}: {}", idBodega, e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(
					"Ha ocurrido un error al consultar el inventario de la bodega", e);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public InventarioBodegaDto findPorCodigo(Integer idBodega, String codigo) {
		return inventarioRepo.findDtoPorCodigo(idBodega, codigo == null ? "" : codigo.trim())
				.orElseThrow(() -> new NotFoundException(
						"El producto con código \"" + codigo + "\" no existe en el inventario de la bodega"));
	}

	@Transactional(readOnly = true)
	@Override
	public Page<MovimientoBodegaDto> findMovimientos(Integer idBodega, String fechaIni, String fechaFin,
			TipoMovimientoBodegaEnum tipo, String filtro, Pageable pageable) {
		bodegaService.findById(idBodega);
		LocalDateTime[] rango = parseRango(fechaIni, fechaFin);
		try {
			return movimientoRepo.findListado(idBodega, rango[0], rango[1], tipo,
					filtro == null ? "" : filtro.trim(), pageable);
		} catch (DataAccessException e) {
			log.error("Error al consultar los movimientos de la bodega {}: {}", idBodega, e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(
					"Ha ocurrido un error al consultar los movimientos de la bodega", e);
		}
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public MovimientoBodega agregarProducto(Integer idBodega, MovimientoBodegaRequest request, Integer idUsuario) {
		validarProductoYCantidad(request);
		if (request.getStockMinimo() != null && request.getStockMinimo() < 0) {
			throw new BadRequestException("El stock mínimo no puede ser negativo.", null);
		}
		Bodega bodega = bodegaService.findActivaById(idBodega);
		Producto producto = productoService.findById(request.getIdProducto());
		Usuario usuario = usuarioService.findById(idUsuario);

		InventarioBodega inventario = bloquear(bodega, producto, true);
		if (request.getStockMinimo() != null) {
			inventario.setStockMinimo(request.getStockMinimo());
		}
		log.info("Agregando {} unidades del producto {} a la bodega {}", request.getCantidad(), producto.getIdProducto(), idBodega);
		return aplicar(inventario, bodega, producto, TipoMovimientoBodegaEnum.INGRESO, request.getCantidad(),
				request.getMotivo(), usuario, null, null);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public MovimientoBodega reducirExistencias(Integer idBodega, MovimientoBodegaRequest request, Integer idUsuario) {
		validarProductoYCantidad(request);
		validarMotivoObligatorio(request.getMotivo());
		Bodega bodega = bodegaService.findActivaById(idBodega);
		Producto producto = productoService.findById(request.getIdProducto());
		Usuario usuario = usuarioService.findById(idUsuario);

		InventarioBodega inventario = bloquear(bodega, producto, false);
		if (inventario == null) {
			throw stockInsuficiente(producto, 0, request.getCantidad());
		}
		log.info("Reduciendo {} unidades del producto {} en la bodega {}", request.getCantidad(), producto.getIdProducto(), idBodega);
		return aplicar(inventario, bodega, producto, TipoMovimientoBodegaEnum.REDUCCION, request.getCantidad(),
				request.getMotivo(), usuario, null, null);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public MovimientoBodega eliminarProducto(Integer idBodega, Integer idProducto, String motivo, Integer idUsuario) {
		if (idProducto == null) {
			throw new BadRequestException("Debe indicar el producto a eliminar de la bodega.", null);
		}
		validarMotivoObligatorio(motivo);
		Bodega bodega = bodegaService.findActivaById(idBodega);
		Producto producto = productoService.findById(idProducto);
		Usuario usuario = usuarioService.findById(idUsuario);

		InventarioBodega inventario = bloquear(bodega, producto, false);
		if (inventario == null) {
			throw new NotFoundException("El producto no se encuentra en el inventario de la bodega");
		}
		log.info("Eliminando el producto {} del inventario de la bodega {}", idProducto, idBodega);
		MovimientoBodega movimiento = aplicar(inventario, bodega, producto, TipoMovimientoBodegaEnum.ELIMINACION,
				inventario.getStock(), motivo, usuario, null, null);
		inventarioRepo.delete(inventario);
		return movimiento;
	}

	@Transactional
	@Override
	public int obtenerStockParaActualizar(Bodega bodega, Producto producto) {
		InventarioBodega inventario = bloquear(bodega, producto, false);
		return inventario == null ? 0 : inventario.getStock();
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public MovimientoBodega registrarMovimiento(Bodega bodega, Producto producto, TipoMovimientoBodegaEnum tipo,
			int cantidad, String motivo, Usuario usuario, String tipoDocumentoOrigen, Long idDocumentoOrigen) {
		if (cantidad <= 0) {
			throw new BadRequestException("La cantidad del movimiento debe ser mayor a 0.", null);
		}
		InventarioBodega inventario = bloquear(bodega, producto, tipo.incrementaStock());
		if (inventario == null) {
			throw stockInsuficiente(producto, 0, cantidad);
		}
		return aplicar(inventario, bodega, producto, tipo, cantidad, motivo, usuario, tipoDocumentoOrigen, idDocumentoOrigen);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int clonarInventario(Integer idBodegaDestino, OrigenInventarioBodegaEnum origen, Integer idOrigen,
			Integer idUsuario) {
		if (origen == null || idOrigen == null) {
			throw new BadRequestException("Debe indicar el origen del inventario a copiar.", null);
		}
		Bodega destino = bodegaService.findActivaById(idBodegaDestino);
		usuarioService.findById(idUsuario);
		if (inventarioRepo.existsByBodega_IdBodega(idBodegaDestino)) {
			throw new BadRequestException(
					"La bodega ya cuenta con inventario registrado; no se puede copiar de nuevo.", null);
		}

		String motivo;
		int copiados;
		try {
			if (origen == OrigenInventarioBodegaEnum.SUCURSAL) {
				Sucursal sucursal = sucursalService.findById(idOrigen);
				motivo = "Inventario copiado desde la sucursal " + sucursal.getNombre();
				copiados = inventarioRepo.clonarDesdeSucursal(idBodegaDestino, idOrigen);
			} else {
				if (idOrigen.equals(idBodegaDestino)) {
					throw new BadRequestException("La bodega no puede copiar su propio inventario.", null);
				}
				Bodega bodegaOrigen = bodegaService.findById(idOrigen);
				motivo = "Inventario copiado desde la bodega " + bodegaOrigen.getNombre();
				copiados = inventarioRepo.clonarDesdeBodega(idBodegaDestino, idOrigen);
			}
			if (copiados == 0) {
				throw new BadRequestException("El origen seleccionado no tiene productos para copiar.", null);
			}
			movimientoRepo.registrarImportacionMasiva(idBodegaDestino, idUsuario, motivo);
		} catch (DataAccessException e) {
			log.error("Error al copiar el inventario hacia la bodega {}: {}", destino.getIdBodega(), e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(
					"Ha ocurrido un error al copiar el inventario hacia la bodega", e);
		}
		log.info("Se copiaron {} productos hacia la bodega {}", copiados, idBodegaDestino);
		return copiados;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public ImportacionInventarioDto importarDesdeExcel(Integer idBodega, InputStream archivo, Integer idUsuario,
			boolean esAdministrador) {
		Bodega bodega = bodegaService.findActivaById(idBodega);
		Usuario usuario = usuarioService.findById(idUsuario);

		InventarioBodegaExcel.Lectura lectura = InventarioBodegaExcel.leer(archivo);
		ImportacionInventarioDto resultado = new ImportacionInventarioDto();
		resultado.setFilasLeidas(lectura.filasLeidas());
		resultado.getErrores().addAll(lectura.errores());

		Map<String, InventarioBodegaExcel.Fila> filasPorCodigo = new LinkedHashMap<>();
		for (InventarioBodegaExcel.Fila fila : lectura.filas()) {
			InventarioBodegaExcel.Fila previa = filasPorCodigo.putIfAbsent(fila.codigo().toLowerCase(), fila);
			if (previa != null) {
				resultado.getErrores().add("Fila " + fila.numeroFila() + ": el código \"" + fila.codigo()
						+ "\" está repetido (ya aparece en la fila " + previa.numeroFila() + ").");
			}
		}

		Map<String, List<Producto>> productosPorCodigo = buscarProductos(filasPorCodigo);
		List<Aplicable> aplicables = new ArrayList<>();
		List<InventarioBodegaExcel.Fila> filasNuevas = new ArrayList<>();
		for (InventarioBodegaExcel.Fila fila : filasPorCodigo.values()) {
			List<Producto> coincidencias = productosPorCodigo.getOrDefault(fila.codigo().toLowerCase(), List.of());
			if (coincidencias.isEmpty()) {
				if (esAdministrador) {
					filasNuevas.add(fila);
				} else {
					resultado.getErrores().add("Fila " + fila.numeroFila() + ": el producto con código \"" + fila.codigo()
							+ "\" no existe en el catálogo. Solo un administrador puede crear productos nuevos al importar.");
				}
			} else if (coincidencias.size() > 1) {
				resultado.getErrores().add("Fila " + fila.numeroFila() + ": el código \"" + fila.codigo()
						+ "\" corresponde a " + coincidencias.size() + " productos distintos; no se puede importar.");
			} else {
				aplicables.add(new Aplicable(coincidencias.get(0), fila));
			}
		}

		// Los catálogos de marcas y categorías solo se consultan si hay productos por crear
		Catalogos catalogos = filasNuevas.isEmpty() ? null : cargarCatalogos();
		List<NuevoProducto> nuevos = validarProductosNuevos(filasNuevas, catalogos, resultado.getErrores());

		if (!resultado.getErrores().isEmpty()) {
			resultado.getErrores().sort(Comparator.comparingInt(InventarioBodegaServiceImpl::numeroDeFila));
			log.warn("Importación a la bodega {} rechazada: {} errores", idBodega, resultado.getErrores().size());
			return resultado;
		}

		// Recién aquí, con el archivo ya validado por completo, se crean los productos que faltaban en el catálogo
		if (!nuevos.isEmpty()) {
			Sucursal sucursalBase = sucursalParaProductosNuevos(bodega, usuario);
			for (NuevoProducto nuevo : nuevos) {
				Producto creado = crearProducto(nuevo, catalogos, usuario);
				// Todo producto debe tener al menos una fila de inventario de sucursal, igual que al crearlo desde Productos
				inventarioSucursalService.obtenerOCrear(sucursalBase, creado);
				aplicables.add(new Aplicable(creado, nuevo.fila()));
			}
			resultado.setProductosCreados(nuevos.size());
		}

		// Se procesa en orden de producto para tomar los bloqueos de fila siempre en la misma secuencia
		aplicables.sort(Comparator.comparing(aplicable -> aplicable.producto().getIdProducto()));
		for (Aplicable aplicable : aplicables) {
			InventarioBodega inventario = bloquear(bodega, aplicable.producto(), true);
			if (aplicable.fila().stockMinimo() != null) {
				inventario.setStockMinimo(aplicable.fila().stockMinimo());
			}
			// Una fila con cantidad 0 registra el producto en la bodega sin existencias y sin movimiento
			if (aplicable.fila().cantidad() > 0) {
				aplicar(inventario, bodega, aplicable.producto(), TipoMovimientoBodegaEnum.IMPORTACION,
						aplicable.fila().cantidad(), "Importación desde archivo Excel", usuario, null, null);
			} else {
				inventarioRepo.save(inventario);
			}
			resultado.setProductosImportados(resultado.getProductosImportados() + 1);
			resultado.setUnidadesImportadas(resultado.getUnidadesImportadas() + aplicable.fila().cantidad());
		}
		log.info("Se importaron {} productos a la bodega {} desde Excel ({} creados en el catálogo)",
				resultado.getProductosImportados(), idBodega, resultado.getProductosCreados());
		return resultado;
	}

	/**
	 * Valida los datos de catálogo de las filas cuyo código no existe. No escribe nada: los errores se acumulan en la
	 * lista recibida y, si hay alguno, la importación se rechaza antes de crear productos.
	 */
	private List<NuevoProducto> validarProductosNuevos(List<InventarioBodegaExcel.Fila> filas, Catalogos catalogos,
			List<String> errores) {
		List<NuevoProducto> nuevos = new ArrayList<>();
		for (InventarioBodegaExcel.Fila fila : filas) {
			InventarioBodegaExcel.DatosProducto datos = fila.producto();
			String prefijo = "Fila " + fila.numeroFila() + ": para crear el producto \"" + fila.codigo() + "\" ";
			int antes = errores.size();

			if (fila.codigo().length() > LONGITUD_MAXIMA_TEXTO_PRODUCTO) {
				errores.add("Fila " + fila.numeroFila() + ": el código supera los " + LONGITUD_MAXIMA_TEXTO_PRODUCTO + " caracteres.");
			}
			if (datos.nombre().isEmpty()) {
				errores.add(prefijo + "falta el nombre.");
			} else if (datos.nombre().length() > LONGITUD_MAXIMA_TEXTO_PRODUCTO) {
				errores.add(prefijo + "el nombre supera los " + LONGITUD_MAXIMA_TEXTO_PRODUCTO + " caracteres.");
			}
			BigDecimal compra = validarPrecio(datos.precioCompra(), "precio de compra", prefijo, errores);
			BigDecimal venta = validarPrecio(datos.precioVenta(), "precio de venta", prefijo, errores);
			MarcaProducto marca = datos.marca().isEmpty() ? null : catalogos.marcas().get(clave(datos.marca()));
			if (!datos.marca().isEmpty() && marca == null) {
				errores.add("Fila " + fila.numeroFila() + ": la marca \"" + datos.marca() + "\" no existe en el catálogo.");
			}
			TipoProducto tipo = datos.categoria().isEmpty() ? null : catalogos.tipos().get(clave(datos.categoria()));
			if (!datos.categoria().isEmpty() && tipo == null) {
				errores.add("Fila " + fila.numeroFila() + ": la categoría \"" + datos.categoria() + "\" no existe en el catálogo.");
			}
			if (errores.size() == antes) {
				nuevos.add(new NuevoProducto(fila, datos.nombre(), compra, venta, marca, tipo));
			}
		}
		return nuevos;
	}

	private BigDecimal validarPrecio(String texto, String nombre, String prefijo, List<String> errores) {
		if (texto.isEmpty()) {
			errores.add(prefijo + "falta el " + nombre + ".");
			return null;
		}
		BigDecimal valor = InventarioBodegaExcel.precio(texto);
		if (valor == null || valor.compareTo(PRECIO_MAXIMO) > 0) {
			errores.add(prefijo + "el " + nombre + " debe ser un número mayor o igual a 0 (se leyó \"" + texto + "\").");
			return null;
		}
		return valor;
	}

	/** Marca y categoría de un producto nuevo; si la fila no las trae se usan las genéricas, que se crean si faltan. */
	private Producto crearProducto(NuevoProducto nuevo, Catalogos catalogos, Usuario usuario) {
		MarcaProducto marca = nuevo.marca() != null ? nuevo.marca()
				: catalogos.marcas().computeIfAbsent(clave(MARCA_POR_DEFECTO), clave -> marcaService.save(
						MarcaProducto.builder().marca(MARCA_POR_DEFECTO).usuario(usuario).build()));
		TipoProducto tipo = nuevo.tipo() != null ? nuevo.tipo()
				: catalogos.tipos().computeIfAbsent(clave(CATEGORIA_POR_DEFECTO), clave -> tipoService.save(
						TipoProducto.builder().tipoProducto(CATEGORIA_POR_DEFECTO).usuario(usuario).build()));
		float porcentaje = nuevo.precioCompra().signum() == 0 ? 0f : nuevo.precioVenta().subtract(nuevo.precioCompra())
				.multiply(BigDecimal.valueOf(100)).divide(nuevo.precioCompra(), 2, RoundingMode.HALF_UP).floatValue();
		return productoService.save(Producto.builder()
				.codProducto(nuevo.fila().codigo())
				.nombre(nuevo.nombre())
				.precioCompra(nuevo.precioCompra())
				.precioVenta(nuevo.precioVenta())
				.porcentajeGanancia(porcentaje)
				.marcaProducto(marca)
				.tipoProducto(tipo)
				.build());
	}

	/** Sucursal donde queda visible el producto nuevo: la de la bodega o, si no tiene, la de quien importa. */
	private Sucursal sucursalParaProductosNuevos(Bodega bodega, Usuario usuario) {
		Sucursal sucursal = bodega.getSucursal() != null ? bodega.getSucursal() : usuario.getSucursal();
		if (sucursal == null) {
			throw new BadRequestException("No se pueden crear productos nuevos: la bodega no tiene una sucursal asignada "
					+ "y tu usuario tampoco. Asigna una sucursal a la bodega e inténtalo de nuevo.", null);
		}
		return sucursal;
	}

	private Catalogos cargarCatalogos() {
		Map<String, MarcaProducto> marcas = new HashMap<>();
		marcaService.findAll().forEach(marca -> marcas.putIfAbsent(clave(marca.getMarca()), marca));
		Map<String, TipoProducto> tipos = new HashMap<>();
		tipoService.findAll().forEach(tipo -> tipos.putIfAbsent(clave(tipo.getTipoProducto()), tipo));
		return new Catalogos(marcas, tipos);
	}

	/** Nombre sin tildes, en minúsculas y con los espacios normalizados, para comparar marcas y categorías. */
	private static String clave(String texto) {
		return Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "").trim().replaceAll("\\s+", " ").toLowerCase();
	}

	/** Número de fila con el que empieza un mensaje de error ("Fila 7: ..."), para ordenarlos como en el archivo. */
	private static int numeroDeFila(String error) {
		int fin = error.indexOf(':');
		try {
			return Integer.parseInt(error.substring("Fila ".length(), fin));
		} catch (RuntimeException e) {
			return Integer.MAX_VALUE;
		}
	}

	private record Aplicable(Producto producto, InventarioBodegaExcel.Fila fila) {
	}

	/** Producto por crear con sus datos ya validados; una marca o categoría nula significa "usar la genérica". */
	private record NuevoProducto(InventarioBodegaExcel.Fila fila, String nombre, BigDecimal precioCompra,
			BigDecimal precioVenta, MarcaProducto marca, TipoProducto tipo) {
	}

	private record Catalogos(Map<String, MarcaProducto> marcas, Map<String, TipoProducto> tipos) {
	}

	private Map<String, List<Producto>> buscarProductos(Map<String, InventarioBodegaExcel.Fila> filasPorCodigo) {
		List<String> codigos = filasPorCodigo.values().stream()
				.map(InventarioBodegaExcel.Fila::codigo).collect(Collectors.toList());
		Map<String, List<Producto>> productos = new HashMap<>();
		for (int i = 0; i < codigos.size(); i += TAMANIO_LOTE_CODIGOS) {
			List<String> lote = codigos.subList(i, Math.min(i + TAMANIO_LOTE_CODIGOS, codigos.size()));
			for (Producto producto : productoRepo.findByCodProductoIn(lote)) {
				productos.computeIfAbsent(producto.getCodProducto().toLowerCase(), clave -> new ArrayList<>()).add(producto);
			}
		}
		return productos;
	}

	/**
	 * Bloquea la fila de inventario de un producto en una bodega. Si no existe, con {@code crear} la crea con
	 * stock 0 y si no devuelve {@code null}.
	 */
	private InventarioBodega bloquear(Bodega bodega, Producto producto, boolean crear) {
		return inventarioRepo.findParaActualizar(bodega.getIdBodega(), producto.getIdProducto())
				.orElseGet(() -> {
					if (!crear) {
						return null;
					}
					log.info("Creando fila de inventario para el producto {} en la bodega {}",
							producto.getIdProducto(), bodega.getIdBodega());
					return inventarioRepo.save(InventarioBodega.builder()
							.bodega(bodega)
							.producto(producto)
							.stock(0)
							.build());
				});
	}

	private MovimientoBodega aplicar(InventarioBodega inventario, Bodega bodega, Producto producto,
			TipoMovimientoBodegaEnum tipo, int cantidad, String motivo, Usuario usuario,
			String tipoDocumentoOrigen, Long idDocumentoOrigen) {
		int stockInicial = inventario.getStock();
		long stockFinal = tipo.incrementaStock() ? (long) stockInicial + cantidad : (long) stockInicial - cantidad;
		if (stockFinal < 0) {
			throw stockInsuficiente(producto, stockInicial, cantidad);
		}
		if (stockFinal > Integer.MAX_VALUE) {
			throw new BadRequestException("La existencia resultante supera el máximo permitido.", null);
		}

		inventario.setStock((int) stockFinal);
		inventario.setFechaActualizacion(LocalDateTime.now());
		inventarioRepo.save(inventario);

		try {
			return movimientoRepo.save(MovimientoBodega.builder()
					.tipoMovimiento(tipo)
					.cantidad(cantidad)
					.stockInicial(stockInicial)
					.stockFinal((int) stockFinal)
					.motivo(motivo == null || motivo.isBlank() ? null : motivo.trim())
					.tipoDocumentoOrigen(tipoDocumentoOrigen)
					.idDocumentoOrigen(idDocumentoOrigen)
					.bodega(bodega)
					.producto(producto)
					.usuario(usuario)
					.build());
		} catch (DataAccessException e) {
			log.error("Error al registrar el movimiento de bodega: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(
					"Ha ocurrido un error al registrar el movimiento de bodega", e);
		}
	}

	private void validarProductoYCantidad(MovimientoBodegaRequest request) {
		if (request == null || request.getIdProducto() == null) {
			throw new BadRequestException("Debe indicar el producto.", null);
		}
		if (request.getCantidad() == null || request.getCantidad() <= 0) {
			throw new BadRequestException("La cantidad debe ser un número entero mayor a 0.", null);
		}
		if (request.getMotivo() != null && request.getMotivo().length() > 300) {
			throw new BadRequestException("El motivo no puede superar los 300 caracteres.", null);
		}
	}

	private void validarMotivoObligatorio(String motivo) {
		if (motivo == null || motivo.isBlank()) {
			throw new BadRequestException("Debe indicar el motivo del movimiento.", null);
		}
		if (motivo.length() > 300) {
			throw new BadRequestException("El motivo no puede superar los 300 caracteres.", null);
		}
	}

	private BadRequestException stockInsuficiente(Producto producto, int disponible, int solicitado) {
		String nombre = producto.getNombre() == null ? String.valueOf(producto.getIdProducto()) : producto.getNombre();
		return new BadRequestException("Existencias insuficientes en la bodega para " + nombre
				+ ". Disponible: " + disponible + ", solicitado: " + solicitado + ".", null);
	}

	private LocalDateTime[] parseRango(String fechaIni, String fechaFin) {
		boolean sinInicio = fechaIni == null || fechaIni.isBlank();
		boolean sinFin = fechaFin == null || fechaFin.isBlank();
		if (sinInicio && sinFin) {
			return new LocalDateTime[] { DESDE_SIN_LIMITE, HASTA_SIN_LIMITE };
		}
		if (sinInicio || sinFin) {
			throw new BadRequestException("Debe ingresar ambas fechas del rango.", null);
		}
		try {
			LocalDate inicio = LocalDate.parse(fechaIni.trim());
			LocalDate fin = LocalDate.parse(fechaFin.trim());
			if (fin.isBefore(inicio)) {
				throw new BadRequestException("La fecha final no puede ser anterior a la fecha inicial.", null);
			}
			return new LocalDateTime[] { inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay() };
		} catch (DateTimeParseException e) {
			throw new BadRequestException("El formato del rango de fechas no es válido.", e);
		}
	}

}
