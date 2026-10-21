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

import xyz.pangosoft.dtodo.dto.DevolucionBodegaDto;
import xyz.pangosoft.dtodo.dto.DevolucionBodegaRequest;
import xyz.pangosoft.dtodo.dto.InventarioSucursalDto;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.model.Bodega;
import xyz.pangosoft.dtodo.model.DevolucionBodega;
import xyz.pangosoft.dtodo.model.DevolucionBodegaDetalle;
import xyz.pangosoft.dtodo.model.MovimientoProducto;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.EstadoDespachoBodegaEnum;
import xyz.pangosoft.dtodo.model.enums.TipoMovimientoBodegaEnum;
import xyz.pangosoft.dtodo.model.enums.TipoMovimientoEnum;
import xyz.pangosoft.dtodo.repository.IDevolucionBodegaRepository;
import xyz.pangosoft.dtodo.service.IBodegaService;
import xyz.pangosoft.dtodo.service.IDevolucionBodegaService;
import xyz.pangosoft.dtodo.service.IInventarioBodegaService;
import xyz.pangosoft.dtodo.service.IInventarioSucursalService;
import xyz.pangosoft.dtodo.service.IMovimientoProductoService;
import xyz.pangosoft.dtodo.service.IProductoService;
import xyz.pangosoft.dtodo.service.ISucursalService;
import xyz.pangosoft.dtodo.service.IUsuarioService;

/**
 * Devoluciones de producto desde una sucursal (tienda) hacia una bodega.
 *
 * <p>Es el espejo del despacho de bodega y el stock también se mueve en dos tiempos: al registrar la devolución
 * sale de la sucursal (queda reservado y la devolución en {@link EstadoDespachoBodegaEnum#PENDIENTE}); cuando un
 * administrador la aprueba ingresa al inventario de la bodega ({@link EstadoDespachoBodegaEnum#REALIZADO}); si se
 * rechaza o cancela estando pendiente regresa a la sucursal ({@link EstadoDespachoBodegaEnum#CANCELADO}). Una
 * devolución resuelta no vuelve a cambiar.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DevolucionBodegaServiceImpl implements IDevolucionBodegaService {

	static final String DOCUMENTO_ORIGEN = "DEVOLUCION_BODEGA";

	private final IDevolucionBodegaRepository devolucionRepo;

	private final IBodegaService bodegaService;

	private final IInventarioBodegaService inventarioBodegaService;

	private final IInventarioSucursalService inventarioSucursalService;

	private final IProductoService productoService;

	private final ISucursalService sucursalService;

	private final IUsuarioService usuarioService;

	private final IMovimientoProductoService movimientoProductoService;

	@Transactional(readOnly = true)
	@Override
	public Page<DevolucionBodegaDto> findListado(String filtro, Integer idBodega, String estado, Pageable pageable) {
		EstadoDespachoBodegaEnum estadoFiltro = parseEstado(estado);
		try {
			return devolucionRepo.findListado(filtro == null ? "" : filtro.trim(), idBodega, estadoFiltro, pageable);
		} catch (DataAccessException e) {
			log.error("Error al consultar el listado de devoluciones a bodega: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(
					"Ha ocurrido un error al consultar las devoluciones", e);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public Page<InventarioSucursalDto> findProductosSucursal(Integer idSucursal, String filtro, Pageable pageable) {
		sucursalService.findById(idSucursal);
		return inventarioSucursalService.findListado(idSucursal, filtro, pageable);
	}

	@Transactional(readOnly = true)
	@Override
	public DevolucionBodega findById(Long idDevolucion) {
		DevolucionBodega devolucion = devolucionRepo.findById(idDevolucion).orElseThrow(() ->
				new NotFoundException("La devolución " + idDevolucion + " no se encuentra registrada en la base de datos"));
		// Se carga el detalle dentro de la transacción para no depender de la sesión abierta durante la serialización
		devolucion.getItems().size();
		return devolucion;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public DevolucionBodega crear(DevolucionBodegaRequest request, Integer idUsuario) {
		validarSolicitud(request);

		Sucursal sucursal = sucursalService.findById(request.getIdSucursalOrigen());
		validarSucursalActiva(sucursal);
		Bodega bodega = bodegaService.findActivaById(request.getIdBodega());
		Usuario usuario = usuarioService.findById(idUsuario);

		DevolucionBodega devolucion = DevolucionBodega.builder()
				.estado(EstadoDespachoBodegaEnum.PENDIENTE)
				.sucursalOrigen(sucursal)
				.bodega(bodega)
				.usuarioRegistra(usuario)
				.entregadoPor(request.getEntregadoPor().trim())
				.motivo(request.getMotivo().trim())
				.observaciones(request.getObservaciones() == null || request.getObservaciones().isBlank()
						? null : request.getObservaciones().trim())
				.build();

		BigDecimal total = BigDecimal.ZERO;
		// Orden por id de producto: los bloqueos de fila se toman siempre en la misma secuencia entre transacciones
		for (Map.Entry<Integer, Integer> linea : consolidarLineas(request).entrySet()) {
			Producto producto = productoService.findById(linea.getKey());
			int cantidad = linea.getValue();
			int existencia = inventarioSucursalService.obtenerParaActualizar(sucursal, producto).getStock();
			if (existencia < cantidad) {
				throw new BadRequestException("Existencias insuficientes en la sucursal para " + producto.getNombre()
						+ ". Disponible: " + existencia + ", devuelto: " + cantidad + ".", null);
			}

			BigDecimal precio = (producto.getPrecioCompra() == null ? BigDecimal.ZERO : producto.getPrecioCompra())
					.setScale(2, RoundingMode.HALF_UP);
			BigDecimal subTotal = precio.multiply(BigDecimal.valueOf(cantidad));
			devolucion.getItems().add(DevolucionBodegaDetalle.builder()
					.producto(producto)
					.cantidad(cantidad)
					.precioUnitario(precio)
					.subTotal(subTotal)
					.existenciaSucursal(existencia)
					.build());
			total = total.add(subTotal);
		}
		devolucion.setTotal(total);

		DevolucionBodega guardada;
		try {
			guardada = devolucionRepo.save(devolucion);
		} catch (DataAccessException e) {
			log.error("Error al registrar la devolución a bodega: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(
					"Ha ocurrido un error al registrar la devolución", e);
		}

		// La salida de la sucursal reserva el stock: falla (y deshace todo) si otra operación lo consumió entre tanto
		for (DevolucionBodegaDetalle item : guardada.getItems()) {
			movimientoProductoService.save(movimientoSucursal(TipoMovimientoEnum.SALIDA, guardada, item, usuario));
		}
		log.info("Devolución {} registrada desde la sucursal {} hacia la bodega {}", guardada.getIdDevolucion(),
				sucursal.getIdSucursal(), bodega.getIdBodega());
		return guardada;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public DevolucionBodega aprobar(Long idDevolucion, Integer idUsuario) {
		DevolucionBodega devolucion = obtenerPendiente(idDevolucion);
		Usuario usuario = usuarioService.findById(idUsuario);

		Bodega bodega = bodegaService.findActivaById(devolucion.getBodega().getIdBodega());
		String motivo = "Ingreso por la devolución #" + idDevolucion + " desde la sucursal "
				+ devolucion.getSucursalOrigen().getNombre();
		for (DevolucionBodegaDetalle item : devolucion.getItems()) {
			inventarioBodegaService.registrarMovimiento(bodega, item.getProducto(),
					TipoMovimientoBodegaEnum.INGRESO_DEVOLUCION, item.getCantidad(), motivo, usuario,
					DOCUMENTO_ORIGEN, idDevolucion);
		}

		resolver(devolucion, usuario);
		devolucion.setEstado(EstadoDespachoBodegaEnum.REALIZADO);
		log.info("Devolución {} aprobada por el usuario {}", idDevolucion, idUsuario);
		return devolucionRepo.save(devolucion);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public DevolucionBodega cancelar(Long idDevolucion, String motivo, Integer idUsuario) {
		if (motivo == null || motivo.isBlank()) {
			throw new BadRequestException("Debe indicar el motivo del rechazo.", null);
		}
		if (motivo.trim().length() > 300) {
			throw new BadRequestException("El motivo del rechazo no puede superar los 300 caracteres.", null);
		}
		DevolucionBodega devolucion = obtenerPendiente(idDevolucion);
		Usuario usuario = usuarioService.findById(idUsuario);

		// La sucursal vuelve a tener las unidades que se habían reservado al registrar la devolución
		for (DevolucionBodegaDetalle item : devolucion.getItems()) {
			movimientoProductoService.save(movimientoSucursal(TipoMovimientoEnum.ENTRADA, devolucion, item, usuario));
		}

		devolucion.setMotivoCancelacion(motivo.trim());
		resolver(devolucion, usuario);
		devolucion.setEstado(EstadoDespachoBodegaEnum.CANCELADO);
		log.info("Devolución {} rechazada por el usuario {}", idDevolucion, idUsuario);
		return devolucionRepo.save(devolucion);
	}

	private MovimientoProducto movimientoSucursal(TipoMovimientoEnum tipo, DevolucionBodega devolucion,
			DevolucionBodegaDetalle item, Usuario usuario) {
		return MovimientoProducto.builder()
				.tipoMovimiento(tipo)
				.tipoDocumentoOrigen(DOCUMENTO_ORIGEN)
				.idDocumentoOrigen(devolucion.getIdDevolucion())
				.usuario(usuario)
				.producto(item.getProducto())
				.sucursal(devolucion.getSucursalOrigen())
				.cantidad(item.getCantidad())
				.build();
	}

	private DevolucionBodega obtenerPendiente(Long idDevolucion) {
		DevolucionBodega devolucion = devolucionRepo.findParaResolver(idDevolucion).orElseThrow(() ->
				new NotFoundException("La devolución " + idDevolucion + " no se encuentra registrada en la base de datos"));
		if (devolucion.getEstado() != EstadoDespachoBodegaEnum.PENDIENTE) {
			throw new BadRequestException("La devolución #" + idDevolucion + " ya fue "
					+ (devolucion.getEstado() == EstadoDespachoBodegaEnum.REALIZADO ? "aprobada" : "rechazada")
					+ " y no admite más cambios.", null);
		}
		return devolucion;
	}

	private void resolver(DevolucionBodega devolucion, Usuario usuario) {
		devolucion.setFechaResolucion(LocalDateTime.now());
		devolucion.setUsuarioResuelve(usuario);
	}

	private void validarSucursalActiva(Sucursal sucursal) {
		if (sucursal.getEstado() != null && !"ACTIVO".equals(sucursal.getEstado().getEstado())) {
			throw new BadRequestException("La sucursal \"" + sucursal.getNombre() + "\" está inactiva y no puede registrar devoluciones.", null);
		}
	}

	private void validarSolicitud(DevolucionBodegaRequest request) {
		if (request == null || request.getIdSucursalOrigen() == null) {
			throw new BadRequestException("Debe indicar la sucursal que devuelve el producto.", null);
		}
		if (request.getIdBodega() == null) {
			throw new BadRequestException("Debe indicar la bodega que recibe la devolución.", null);
		}
		if (request.getEntregadoPor() == null || request.getEntregadoPor().isBlank()) {
			throw new BadRequestException("Debe indicar quién entrega la devolución.", null);
		}
		if (request.getEntregadoPor().trim().length() > 150) {
			throw new BadRequestException("El nombre de quien entrega no puede superar los 150 caracteres.", null);
		}
		if (request.getMotivo() == null || request.getMotivo().isBlank()) {
			throw new BadRequestException("Debe indicar el motivo de la devolución.", null);
		}
		if (request.getMotivo().trim().length() > 300) {
			throw new BadRequestException("El motivo de la devolución no puede superar los 300 caracteres.", null);
		}
		if (request.getObservaciones() != null && request.getObservaciones().length() > 500) {
			throw new BadRequestException("Las observaciones no pueden superar los 500 caracteres.", null);
		}
		if (request.getItems() == null || request.getItems().isEmpty()) {
			throw new BadRequestException("La devolución debe incluir al menos un producto.", null);
		}
	}

	/** Valida cada línea y suma las cantidades de un mismo producto repetido. */
	private Map<Integer, Integer> consolidarLineas(DevolucionBodegaRequest request) {
		Map<Integer, Integer> cantidades = new TreeMap<>();
		for (DevolucionBodegaRequest.Linea linea : request.getItems()) {
			if (linea == null || linea.getIdProducto() == null || linea.getCantidad() == null || linea.getCantidad() <= 0) {
				throw new BadRequestException("El detalle de la devolución contiene un producto o cantidad inválida.", null);
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
			throw new BadRequestException("El estado de devolución indicado no es válido.", e);
		}
	}

}
