package xyz.pangosoft.dtodo.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import xyz.pangosoft.dtodo.dto.DespachoBodegaDto;
import xyz.pangosoft.dtodo.dto.DespachoBodegaRequest;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.model.Bodega;
import xyz.pangosoft.dtodo.model.DespachoBodega;
import xyz.pangosoft.dtodo.model.DespachoBodegaDetalle;
import xyz.pangosoft.dtodo.model.MovimientoBodega;
import xyz.pangosoft.dtodo.model.MovimientoProducto;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.EstadoDespachoBodegaEnum;
import xyz.pangosoft.dtodo.model.enums.TipoMovimientoBodegaEnum;
import xyz.pangosoft.dtodo.model.enums.TipoMovimientoEnum;
import xyz.pangosoft.dtodo.repository.IDespachoBodegaRepository;
import xyz.pangosoft.dtodo.service.IBodegaService;
import xyz.pangosoft.dtodo.service.IDespachoBodegaService;
import xyz.pangosoft.dtodo.service.IInventarioBodegaService;
import xyz.pangosoft.dtodo.service.IMovimientoProductoService;
import xyz.pangosoft.dtodo.service.IProductoService;
import xyz.pangosoft.dtodo.service.ISucursalService;
import xyz.pangosoft.dtodo.service.IUsuarioService;

/**
 * Despachos de bodega hacia una sucursal.
 *
 * <p>El stock se mueve en dos tiempos: al registrar el despacho sale de la bodega (queda reservado y el
 * despacho en {@link EstadoDespachoBodegaEnum#PENDIENTE}); al aprobarlo ingresa a la sucursal destino
 * ({@link EstadoDespachoBodegaEnum#REALIZADO}); si se cancela estando pendiente regresa a la bodega
 * ({@link EstadoDespachoBodegaEnum#CANCELADO}). Un despacho resuelto no vuelve a cambiar de estado.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DespachoBodegaServiceImpl implements IDespachoBodegaService {

	static final String DOCUMENTO_ORIGEN = "DESPACHO_BODEGA";

	private final IDespachoBodegaRepository despachoRepo;

	private final IBodegaService bodegaService;

	private final IInventarioBodegaService inventarioBodegaService;

	private final IProductoService productoService;

	private final ISucursalService sucursalService;

	private final IUsuarioService usuarioService;

	private final IMovimientoProductoService movimientoProductoService;

	@Transactional(readOnly = true)
	@Override
	public Page<DespachoBodegaDto> findListado(String filtro, Integer idBodega, String estado, Pageable pageable) {
		EstadoDespachoBodegaEnum estadoFiltro = parseEstado(estado);
		try {
			return despachoRepo.findListado(filtro == null ? "" : filtro.trim(), idBodega, estadoFiltro, pageable);
		} catch (DataAccessException e) {
			log.error("Error al consultar el listado de despachos de bodega: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(
					"Ha ocurrido un error al consultar los despachos", e);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public DespachoBodega findById(Long idDespacho) {
		DespachoBodega despacho = despachoRepo.findById(idDespacho).orElseThrow(() ->
				new NotFoundException("El despacho " + idDespacho + " no se encuentra registrado en la base de datos"));
		// Se carga el detalle dentro de la transacción para no depender de la sesión abierta durante la serialización
		despacho.getItems().size();
		return despacho;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public DespachoBodega crear(DespachoBodegaRequest request, Integer idUsuario) {
		validarSolicitud(request);

		Bodega bodega = bodegaService.findActivaById(request.getIdBodega());
		Sucursal destino = resolverDestino(request, bodega);
		Usuario usuario = usuarioService.findById(idUsuario);

		DespachoBodega despacho = DespachoBodega.builder()
				.estado(EstadoDespachoBodegaEnum.PENDIENTE)
				.bodega(bodega)
				.sucursalDestino(destino)
				.usuarioDespacha(usuario)
				.recibidoPor(request.getRecibidoPor().trim())
				.observaciones(request.getObservaciones() == null || request.getObservaciones().isBlank()
						? null : request.getObservaciones().trim())
				.build();

		BigDecimal total = BigDecimal.ZERO;
		// Orden por id de producto: los bloqueos de fila se toman siempre en la misma secuencia entre transacciones
		for (Map.Entry<Integer, Integer> linea : consolidarLineas(request).entrySet()) {
			Producto producto = productoService.findById(linea.getKey());
			int cantidad = linea.getValue();
			int existencia = inventarioBodegaService.obtenerStockParaActualizar(bodega, producto);
			if (existencia < cantidad) {
				throw new BadRequestException("Existencias insuficientes en la bodega para " + producto.getNombre()
						+ ". Disponible: " + existencia + ", solicitado: " + cantidad + ".", null);
			}

			BigDecimal precio = (producto.getPrecioCompra() == null ? BigDecimal.ZERO : producto.getPrecioCompra())
					.setScale(2, RoundingMode.HALF_UP);
			BigDecimal subTotal = precio.multiply(BigDecimal.valueOf(cantidad));
			despacho.getItems().add(DespachoBodegaDetalle.builder()
					.producto(producto)
					.cantidad(cantidad)
					.precioUnitario(precio)
					.subTotal(subTotal)
					.existenciaBodega(existencia)
					.build());
			total = total.add(subTotal);
		}
		despacho.setTotal(total);

		DespachoBodega guardado;
		try {
			guardado = despachoRepo.save(despacho);
		} catch (DataAccessException e) {
			log.error("Error al registrar el despacho de bodega: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(
					"Ha ocurrido un error al registrar el despacho", e);
		}

		String motivo = "Despacho #" + guardado.getIdDespacho() + " hacia " + destino.getNombre();
		for (DespachoBodegaDetalle item : guardado.getItems()) {
			inventarioBodegaService.registrarMovimiento(bodega, item.getProducto(), TipoMovimientoBodegaEnum.DESPACHO,
					item.getCantidad(), motivo, usuario, DOCUMENTO_ORIGEN, guardado.getIdDespacho());
		}
		log.info("Despacho {} registrado desde la bodega {} hacia la sucursal {}",
				guardado.getIdDespacho(), bodega.getIdBodega(), destino.getIdSucursal());
		return guardado;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public DespachoBodega aprobar(Long idDespacho, Integer idUsuario) {
		DespachoBodega despacho = obtenerPendiente(idDespacho);
		Usuario usuario = usuarioService.findById(idUsuario);
		Sucursal destino = despacho.getSucursalDestino();
		validarSucursalActiva(destino);

		for (DespachoBodegaDetalle item : despacho.getItems()) {
			movimientoProductoService.save(MovimientoProducto.builder()
					.tipoMovimiento(TipoMovimientoEnum.ENTRADA)
					.tipoDocumentoOrigen(DOCUMENTO_ORIGEN)
					.idDocumentoOrigen(despacho.getIdDespacho())
					.usuario(usuario)
					.producto(item.getProducto())
					.sucursal(destino)
					.cantidad(item.getCantidad())
					.build());
		}

		resolver(despacho, EstadoDespachoBodegaEnum.REALIZADO, usuario);
		log.info("Despacho {} aprobado por el usuario {}", idDespacho, idUsuario);
		return despachoRepo.save(despacho);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public DespachoBodega cancelar(Long idDespacho, String motivo, Integer idUsuario, boolean esAdministrador) {
		if (motivo == null || motivo.isBlank()) {
			throw new BadRequestException("Debe indicar el motivo de la cancelación.", null);
		}
		if (motivo.length() > 300) {
			throw new BadRequestException("El motivo de la cancelación no puede superar los 300 caracteres.", null);
		}
		DespachoBodega despacho = obtenerCancelable(idDespacho, esAdministrador);
		Usuario usuario = usuarioService.findById(idUsuario);

		// Un despacho aprobado ya sumó stock a la sucursal: primero se retira de ahí (falla si la sucursal ya no lo tiene)
		boolean revertirAprobado = despacho.getEstado() == EstadoDespachoBodegaEnum.REALIZADO;
		if (revertirAprobado) {
			retirarDeSucursal(despacho, usuario);
		}

		String motivoMovimiento = (revertirAprobado ? "Reversión del despacho #" : "Cancelación del despacho #") + idDespacho;
		for (DespachoBodegaDetalle item : despacho.getItems()) {
			MovimientoBodega reintegro = inventarioBodegaService.registrarMovimiento(despacho.getBodega(),
					item.getProducto(), TipoMovimientoBodegaEnum.ANULACION_DESPACHO, item.getCantidad(),
					motivoMovimiento, usuario, DOCUMENTO_ORIGEN, idDespacho);
			log.debug("Reintegrado el movimiento {} a la bodega {}", reintegro.getIdMovimiento(), despacho.getBodega().getIdBodega());
		}

		despacho.setMotivoCancelacion(motivo.trim());
		resolver(despacho, EstadoDespachoBodegaEnum.CANCELADO, usuario);
		log.info("Despacho {} {} por el usuario {}", idDespacho, revertirAprobado ? "revertido" : "cancelado", idUsuario);
		return despachoRepo.save(despacho);
	}

	/** Deshace el ingreso que generó la aprobación: una salida por línea en el inventario de la sucursal destino. */
	private void retirarDeSucursal(DespachoBodega despacho, Usuario usuario) {
		for (DespachoBodegaDetalle item : despacho.getItems()) {
			try {
				movimientoProductoService.save(MovimientoProducto.builder()
						.tipoMovimiento(TipoMovimientoEnum.SALIDA)
						.tipoDocumentoOrigen(DOCUMENTO_ORIGEN)
						.idDocumentoOrigen(despacho.getIdDespacho())
						.usuario(usuario)
						.producto(item.getProducto())
						.sucursal(despacho.getSucursalDestino())
						.cantidad(item.getCantidad())
						.build());
			} catch (BadRequestException e) {
				throw new BadRequestException("No se puede revertir el despacho #" + despacho.getIdDespacho()
						+ " porque la sucursal \"" + despacho.getSucursalDestino().getNombre()
						+ "\" ya no cuenta con todas las unidades. " + e.getMessage(), e);
			}
		}
	}

	/** Pendiente: lo puede cancelar quien opera bodegas. Aprobado: solo un administrador puede revertirlo. */
	private DespachoBodega obtenerCancelable(Long idDespacho, boolean esAdministrador) {
		DespachoBodega despacho = despachoRepo.findParaResolver(idDespacho).orElseThrow(() ->
				new NotFoundException("El despacho " + idDespacho + " no se encuentra registrado en la base de datos"));
		if (despacho.getEstado() == EstadoDespachoBodegaEnum.CANCELADO) {
			throw new BadRequestException("El despacho #" + idDespacho + " ya fue cancelado y no admite más cambios.", null);
		}
		if (despacho.getEstado() == EstadoDespachoBodegaEnum.REALIZADO && !esAdministrador) {
			throw new BadRequestException("Solo un administrador puede revertir un despacho que ya fue aprobado.", null);
		}
		return despacho;
	}

	private DespachoBodega obtenerPendiente(Long idDespacho) {
		DespachoBodega despacho = despachoRepo.findParaResolver(idDespacho).orElseThrow(() ->
				new NotFoundException("El despacho " + idDespacho + " no se encuentra registrado en la base de datos"));
		if (despacho.getEstado() != EstadoDespachoBodegaEnum.PENDIENTE) {
			throw new BadRequestException("El despacho #" + idDespacho + " ya fue "
					+ (despacho.getEstado() == EstadoDespachoBodegaEnum.REALIZADO ? "aprobado" : "cancelado")
					+ " y no admite más cambios.", null);
		}
		return despacho;
	}

	private void resolver(DespachoBodega despacho, EstadoDespachoBodegaEnum estado, Usuario usuario) {
		despacho.setEstado(estado);
		despacho.setFechaResolucion(LocalDateTime.now());
		despacho.setUsuarioResuelve(usuario);
	}

	private Sucursal resolverDestino(DespachoBodegaRequest request, Bodega bodega) {
		Integer idDestino = request.getIdSucursalDestino();
		if (idDestino == null && bodega.getSucursal() != null) {
			idDestino = bodega.getSucursal().getIdSucursal();
		}
		if (idDestino == null) {
			throw new BadRequestException(
					"Debe indicar la sucursal destino: la bodega no tiene una sucursal asignada.", null);
		}
		Sucursal destino = sucursalService.findById(idDestino);
		validarSucursalActiva(destino);
		return destino;
	}

	private void validarSucursalActiva(Sucursal sucursal) {
		if (sucursal.getEstado() != null && !"ACTIVO".equals(sucursal.getEstado().getEstado())) {
			throw new BadRequestException("La sucursal \"" + sucursal.getNombre() + "\" está inactiva y no puede recibir despachos.", null);
		}
	}

	private void validarSolicitud(DespachoBodegaRequest request) {
		if (request == null || request.getIdBodega() == null) {
			throw new BadRequestException("Debe indicar la bodega desde la que se despacha.", null);
		}
		if (request.getRecibidoPor() == null || request.getRecibidoPor().isBlank()) {
			throw new BadRequestException("Debe indicar quién recibe el despacho.", null);
		}
		if (request.getRecibidoPor().trim().length() > 150) {
			throw new BadRequestException("El nombre de quien recibe no puede superar los 150 caracteres.", null);
		}
		if (request.getObservaciones() != null && request.getObservaciones().length() > 500) {
			throw new BadRequestException("Las observaciones no pueden superar los 500 caracteres.", null);
		}
		if (request.getItems() == null || request.getItems().isEmpty()) {
			throw new BadRequestException("El despacho debe incluir al menos un producto.", null);
		}
	}

	/** Valida cada línea y suma las cantidades de un mismo producto repetido. */
	private Map<Integer, Integer> consolidarLineas(DespachoBodegaRequest request) {
		Map<Integer, Integer> cantidades = new TreeMap<>();
		for (DespachoBodegaRequest.Linea linea : request.getItems()) {
			if (linea == null || linea.getIdProducto() == null || linea.getCantidad() == null || linea.getCantidad() <= 0) {
				throw new BadRequestException("El detalle del despacho contiene un producto o cantidad inválida.", null);
			}
			cantidades.merge(linea.getIdProducto(), linea.getCantidad(), Integer::sum);
		}
		return cantidades;
	}

	private EstadoDespachoBodegaEnum parseEstado(String estado) {
		if (estado == null || estado.isBlank()) {
			return null;
		}
		try {
			return EstadoDespachoBodegaEnum.valueOf(estado.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new BadRequestException("El estado de despacho indicado no es válido.", e);
		}
	}

}
