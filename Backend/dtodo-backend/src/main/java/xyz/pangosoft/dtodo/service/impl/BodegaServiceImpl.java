package xyz.pangosoft.dtodo.service.impl;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import xyz.pangosoft.dtodo.dto.BodegaDto;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.model.Bodega;
import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.repository.IBodegaRepository;
import xyz.pangosoft.dtodo.service.IBodegaService;
import xyz.pangosoft.dtodo.service.IEstadoService;
import xyz.pangosoft.dtodo.service.ISucursalService;
import xyz.pangosoft.dtodo.service.IUsuarioService;

@Service
@RequiredArgsConstructor
@Slf4j
public class BodegaServiceImpl implements IBodegaService {

	static final String ESTADO_ACTIVO = "ACTIVO";
	static final String ESTADO_INACTIVO = "INACTIVO";

	private final IBodegaRepository bodegaRepo;

	private final IEstadoService estadoService;

	private final ISucursalService sucursalService;

	private final IUsuarioService usuarioService;

	@Transactional(readOnly = true)
	@Override
	public List<Bodega> findAll(boolean soloActivas) {
		try {
			List<Bodega> bodegas = bodegaRepo.findAll(Sort.by(Sort.Direction.ASC, "nombre"));
			if (soloActivas) {
				bodegas.removeIf(bodega -> !esActiva(bodega));
			}
			return bodegas;
		} catch (DataAccessException e) {
			log.error("Error al consultar las bodegas: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(
					"Ha ocurrido un error al consultar las bodegas", e);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public Page<BodegaDto> findListado(String filtro, Pageable pageable) {
		try {
			return bodegaRepo.findListado(filtro == null ? "" : filtro.trim(), pageable);
		} catch (DataAccessException e) {
			log.error("Error al consultar el listado paginado de bodegas: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(
					"Ha ocurrido un error al consultar las bodegas", e);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public Bodega findById(Integer idBodega) {
		return bodegaRepo.findById(idBodega).orElseThrow(() -> {
			log.warn("La bodega {} no se encuentra registrada", idBodega);
			return new NotFoundException("La bodega " + idBodega + " no se encuentra registrada en la base de datos");
		});
	}

	@Transactional(readOnly = true)
	@Override
	public Bodega findActivaById(Integer idBodega) {
		Bodega bodega = findById(idBodega);
		if (!esActiva(bodega)) {
			throw new BadRequestException("La bodega \"" + bodega.getNombre() + "\" está inactiva y no admite operaciones.", null);
		}
		return bodega;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public Bodega save(Bodega bodega, Integer idUsuario) {
		String nombre = bodega.getNombre() == null ? "" : bodega.getNombre().trim();
		if (nombre.isEmpty()) {
			throw new BadRequestException("El nombre de la bodega no puede estar vacío.", null);
		}
		Sucursal sucursal = resolverSucursal(bodega);

		try {
			if (bodega.getIdBodega() == null) {
				return registrar(bodega, nombre, sucursal, idUsuario);
			}
			return actualizar(bodega, nombre, sucursal);
		} catch (DataAccessException e) {
			log.error("Error al guardar la bodega: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(
					"Ha ocurrido un error al guardar la bodega", e);
		}
	}

	private Bodega registrar(Bodega bodega, String nombre, Sucursal sucursal, Integer idUsuario) {
		if (bodegaRepo.existsByNombreIgnoreCase(nombre)) {
			throw new BadRequestException("Ya existe una bodega con el nombre \"" + nombre + "\".", null);
		}
		log.info("Registrando bodega nueva: {}", nombre);
		bodega.setNombre(nombre);
		bodega.setSucursal(sucursal);
		bodega.setEstado(estadoService.findByEstado(ESTADO_ACTIVO));
		bodega.setUsuario(usuarioService.findById(idUsuario));
		return bodegaRepo.save(bodega);
	}

	/**
	 * Copia sobre la bodega existente solo los campos editables: el creador y la fecha de registro nunca
	 * se toman del cliente.
	 */
	private Bodega actualizar(Bodega datos, String nombre, Sucursal sucursal) {
		Bodega existente = findById(datos.getIdBodega());
		if (bodegaRepo.existsByNombreIgnoreCaseAndIdBodegaNot(nombre, existente.getIdBodega())) {
			throw new BadRequestException("Ya existe otra bodega con el nombre \"" + nombre + "\".", null);
		}
		log.info("Actualizando bodega: {}", existente.getIdBodega());

		existente.setNombre(nombre);
		existente.setUbicacion(datos.getUbicacion());
		existente.setDescripcion(datos.getDescripcion());
		existente.setEncargado(datos.getEncargado());
		existente.setTelefono(datos.getTelefono());
		existente.setSucursal(sucursal);
		if (datos.getEstado() != null && datos.getEstado().getIdEstado() != null) {
			existente.setEstado(resolverEstado(datos.getEstado().getIdEstado()));
		}
		return bodegaRepo.save(existente);
	}

	private Sucursal resolverSucursal(Bodega bodega) {
		if (bodega.getSucursal() == null || bodega.getSucursal().getIdSucursal() == null) {
			return null;
		}
		return sucursalService.findById(bodega.getSucursal().getIdSucursal());
	}

	/** Una bodega solo puede quedar ACTIVA o INACTIVA; cualquier otro estado del catálogo compartido se rechaza. */
	private Estado resolverEstado(Integer idEstado) {
		Estado estado = estadoService.findById(idEstado);
		if (estado == null || (!ESTADO_ACTIVO.equals(estado.getEstado()) && !ESTADO_INACTIVO.equals(estado.getEstado()))) {
			throw new BadRequestException("El estado indicado no es válido para una bodega.", null);
		}
		return estado;
	}

	private boolean esActiva(Bodega bodega) {
		return bodega.getEstado() != null && ESTADO_ACTIVO.equals(bodega.getEstado().getEstado());
	}

}
