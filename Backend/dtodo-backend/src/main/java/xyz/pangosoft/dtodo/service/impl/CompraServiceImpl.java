package xyz.pangosoft.dtodo.service.impl;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import xyz.pangosoft.dtodo.dto.CompraDetalleDocumentoDto;
import xyz.pangosoft.dtodo.dto.CompraDto;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.model.Compra;
import xyz.pangosoft.dtodo.model.CompraDetalle;
import xyz.pangosoft.dtodo.model.MovimientoProducto;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.Proveedor;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.EstadoCompraEnum;
import xyz.pangosoft.dtodo.model.enums.TipoMovimientoEnum;
import xyz.pangosoft.dtodo.repository.ICompraRepository;
import xyz.pangosoft.dtodo.service.ICompraService;
import xyz.pangosoft.dtodo.service.IMovimientoProductoService;
import xyz.pangosoft.dtodo.service.IProductoService;
import xyz.pangosoft.dtodo.service.IProveedorService;
import xyz.pangosoft.dtodo.service.ISucursalService;
import xyz.pangosoft.dtodo.service.IUsuarioService;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompraServiceImpl implements ICompraService {

	private final ICompraRepository compraRepository;

	private final IProveedorService proveedorService;

	private final IUsuarioService usuarioService;

	private final ISucursalService sucursalService;

	private final IProductoService productoService;

	private final IMovimientoProductoService movimientoProductoService;

	@Transactional(readOnly = true)
	@Override
	public Page<CompraDto> findListado(String filtro, Integer idSucursal, Pageable pageable) {
		try {
			return compraRepository.findListado(filtro == null ? "" : filtro.trim(), idSucursal, pageable);
		} catch (DataAccessException e) {
			log.error("Error al consultar el listado paginado de compras: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error al consultar las compras", e);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public Page<CompraDetalleDocumentoDto> findDetalleDto(Long idCompra, Pageable pageable) {
		if (!compraRepository.existsById(idCompra)) {
			throw new NotFoundException("La compra con ID " + idCompra + " no existe");
		}
		try {
			return compraRepository.findDetalleDto(idCompra, pageable).map(this::mapDetalleDocumentoDto);
		} catch (DataAccessException e) {
			log.error("Error al consultar el detalle de la compra {}: {}", idCompra, e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(
					"Ha ocurrido un error al consultar el detalle de la compra", e);
		}
	}

	private CompraDetalleDocumentoDto mapDetalleDocumentoDto(Object[] fila) {
		return new CompraDetalleDocumentoDto(
				((Number) fila[0]).longValue(),
				((Number) fila[1]).intValue(),
				(String) fila[2],
				(String) fila[3],
				((Number) fila[4]).intValue(),
				toBigDecimal(fila[5]),
				toBigDecimal(fila[6]));
	}

	private BigDecimal toBigDecimal(Object valor) {
		return valor instanceof BigDecimal ? (BigDecimal) valor : new BigDecimal(valor.toString());
	}

	@Transactional(readOnly = true)
	@Override
	public Compra findById(Long idCompra) {
		Optional<Compra> compra = compraRepository.findById(idCompra);
		if (compra.isPresent()) {
			return compra.get();
		}
		throw new NotFoundException("La compra: " + idCompra + ", no se encuentra registrada en la base de datos");
	}

	@Transactional(rollbackFor = { Exception.class, DataAccessException.class })
	@Override
	public Compra crear(Compra compra) {
		log.info("Registrando nueva compra, proveedor: {}", compra.getProveedor());

		if (compra.getItems() == null || compra.getItems().isEmpty()) {
			throw new BadRequestException("La compra debe incluir al menos un producto en el detalle.", null);
		}
		if (compra.getUsuario() == null || compra.getUsuario().getIdUsuario() == null) {
			throw new BadRequestException("La compra debe indicar el usuario que la registra.", null);
		}
		if (compra.getSucursal() == null || compra.getSucursal().getIdSucursal() == null) {
			throw new BadRequestException("La compra debe indicar la sucursal donde ingresa la mercadería.", null);
		}
		if (compra.getProveedor() == null || compra.getProveedor().getIdProveedor() == null) {
			throw new BadRequestException("La compra debe indicar el proveedor.", null);
		}

		Usuario usuario = usuarioService.findById(compra.getUsuario().getIdUsuario());
		Sucursal sucursal = sucursalService.findById(compra.getSucursal().getIdSucursal());
		Proveedor proveedor = proveedorService.findById(compra.getProveedor().getIdProveedor());

		compra.setUsuario(usuario);
		compra.setSucursal(sucursal);
		compra.setProveedor(proveedor);
		compra.setEstado(EstadoCompraEnum.ACTIVA);
		if (compra.getCostoEnvio() == null) {
			compra.setCostoEnvio(BigDecimal.ZERO);
		}

		BigDecimal totalCompra = BigDecimal.ZERO;
		for (CompraDetalle item : compra.getItems()) {
			if (item.getProducto() == null || item.getCantidad() == null || item.getCantidad() <= 0
					|| item.getPrecioUnitario() == null || item.getPrecioUnitario().compareTo(BigDecimal.ZERO) <= 0) {
				throw new BadRequestException(
						"El detalle de la compra contiene un producto, cantidad o precio unitario inválido.", null);
			}

			Producto producto = item.getProducto().getIdProducto() == null
					? productoService.save(item.getProducto())
					: productoService.findById(item.getProducto().getIdProducto());

			item.setProducto(producto);
			item.setSubTotal(item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad())));
			totalCompra = totalCompra.add(item.getSubTotal());
		}
		compra.setTotal(totalCompra.add(compra.getCostoEnvio()));

		Compra compraGuardada = compraRepository.save(compra);

		compraGuardada.getItems().forEach(item -> movimientoProductoService.save(
				MovimientoProducto.builder()
						.tipoMovimiento(TipoMovimientoEnum.COMPRA)
						.usuario(usuario)
						.producto(item.getProducto())
						.sucursal(sucursal)
						.cantidad(item.getCantidad())
						.build()));

		return compraGuardada;
	}

	@Transactional(rollbackFor = { Exception.class, DataAccessException.class })
	@Override
	public Compra anular(Long idCompra, Integer idUsuario) {
		Compra compra = findById(idCompra);

		if (EstadoCompraEnum.ANULADA.equals(compra.getEstado())) {
			throw new BadRequestException("La compra ya se encuentra anulada.", null);
		}

		Usuario usuarioAnula = usuarioService.findById(idUsuario);

		compra.setEstado(EstadoCompraEnum.ANULADA);
		Compra compraAnulada = compraRepository.save(compra);

		compraAnulada.getItems().forEach(item -> movimientoProductoService.save(
				MovimientoProducto.builder()
						.tipoMovimiento(TipoMovimientoEnum.ELIMINAR_COMPRA)
						.usuario(usuarioAnula)
						.producto(item.getProducto())
						// Se revierte el stock a la sucursal donde ingresó la compra original, no a la del usuario que anula
						.sucursal(compraAnulada.getSucursal())
						.cantidad(item.getCantidad())
						.build()));

		return compraAnulada;
	}

}
