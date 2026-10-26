package xyz.pangosoft.dtodo.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.pangosoft.dtodo.dto.ClienteDto;
import xyz.pangosoft.dtodo.dto.RutaDto;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.Ruta;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.repository.IClienteRepository;
import xyz.pangosoft.dtodo.repository.IRutaRepository;
import xyz.pangosoft.dtodo.service.IEstadoService;
import xyz.pangosoft.dtodo.service.IRutaService;
import xyz.pangosoft.dtodo.service.ISucursalService;
import xyz.pangosoft.dtodo.service.IUsuarioService;

@Service
@RequiredArgsConstructor
@Slf4j
public class RutaServiceImpl implements IRutaService {

    static final String ESTADO_ACTIVO = "ACTIVO";
    static final String ESTADO_INACTIVO = "INACTIVO";
    static final String ROLE_VENDEDOR = "ROLE_VENDEDOR";

    private final IRutaRepository rutaRepository;
    private final IClienteRepository clienteRepository;
    private final ISucursalService sucursalService;
    private final IUsuarioService usuarioService;
    private final IEstadoService estadoService;

    @Transactional(readOnly = true)
    @Override
    public Page<RutaDto> findListado(String filtro, Pageable pageable) {
        try {
            String criterio = filtro == null ? "" : filtro.trim();
            return rutaRepository.findListado(criterio, pageable);
        } catch (DataAccessException e) {
            log.error("Error al consultar el listado de rutas: {}", e.getMessage());
            throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(
                    "Ha ocurrido un error al consultar las rutas", e);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<RutaDto> findActivas() {
        return rutaRepository.findActivasDto();
    }

    @Transactional(readOnly = true)
    @Override
    public List<RutaDto> findActivasDeVendedor(Integer idUsuario) {
        return rutaRepository.findActivasDtoByVendedor(idUsuario);
    }

    @Transactional(readOnly = true)
    @Override
    public Ruta findById(Integer idRuta) {
        return rutaRepository.findById(idRuta)
                .orElseThrow(() -> new NotFoundException("La ruta con ID " + idRuta + " no se encuentra registrada."));
    }

    @Transactional(readOnly = true)
    @Override
    public RutaDto findDtoById(Integer idRuta) {
        return rutaRepository.findDtoById(idRuta)
                .orElseThrow(() -> new NotFoundException("La ruta con ID " + idRuta + " no se encuentra registrada."));
    }

    @Transactional
    @Override
    public RutaDto save(Ruta ruta) {
        try {
            String nombre = ruta.getNombre() == null ? "" : ruta.getNombre().trim();
            if (nombre.isEmpty()) {
                throw new ReglaNegocioException("El nombre de la ruta no puede estar vacío.");
            }
            Sucursal sucursal = resolverSucursal(ruta);
            Usuario vendedor = resolverVendedor(ruta);

            Ruta guardada;
            if (ruta.getIdRuta() == null) {
                if (rutaRepository.existsByNombreIgnoreCaseAndSucursal_IdSucursal(nombre, sucursal.getIdSucursal())) {
                    throw new ReglaNegocioException("Ya existe una ruta llamada \"" + nombre
                            + "\" en la sucursal " + sucursal.getNombre() + ".");
                }
                log.info("Registrando ruta nueva: {}", nombre);
                ruta.setNombre(nombre);
                ruta.setSucursal(sucursal);
                ruta.setVendedor(vendedor);
                ruta.setEstado(resolverEstado(ruta.getEstado()));
                guardada = rutaRepository.save(ruta);
            } else {
                guardada = findById(ruta.getIdRuta());
                if (rutaRepository.existsByNombreIgnoreCaseAndSucursal_IdSucursalAndIdRutaNot(
                        nombre, sucursal.getIdSucursal(), guardada.getIdRuta())) {
                    throw new ReglaNegocioException("Ya existe otra ruta llamada \"" + nombre
                            + "\" en la sucursal " + sucursal.getNombre() + ".");
                }
                log.info("Actualizando ruta: {}", guardada.getIdRuta());
                guardada.setNombre(nombre);
                guardada.setZona(ruta.getZona());
                guardada.setDiasVisita(ruta.getDiasVisita());
                guardada.setSucursal(sucursal);
                guardada.setVendedor(vendedor);
                if (ruta.getEstado() != null && ruta.getEstado().getIdEstado() != null) {
                    guardada.setEstado(resolverEstado(ruta.getEstado()));
                }
                guardada = rutaRepository.save(guardada);
            }
            return findDtoById(guardada.getIdRuta());
        } catch (DataAccessException e) {
            log.error("Error al guardar la ruta: {}", e.getMessage());
            throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(
                    "Ha ocurrido un error al guardar la ruta", e);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<ClienteDto> findClientes(Integer idRuta) {
        findById(idRuta);
        return clienteRepository.findByRuta_IdRutaOrderByNombre(idRuta).stream()
                .map(RutaServiceImpl::aDto)
                .collect(Collectors.toList());
    }

    private static ClienteDto aDto(Cliente cliente) {
        return new ClienteDto(cliente.getIdCliente(), cliente.getNombre(), cliente.getNit(), cliente.getDireccion(),
                cliente.getFechaRegistro(), cliente.getTelefono(), cliente.getCodigoCliente(), cliente.getNombreNegocio(),
                cliente.getTipoCliente(), cliente.getCategoriaDescuento(),
                cliente.getRuta() == null ? null : cliente.getRuta().getIdRuta(),
                cliente.getRuta() == null ? null : cliente.getRuta().getNombre(),
                cliente.getContacto(), cliente.getTelefonoContacto(), cliente.getReferenciaUbicacion(),
                cliente.getLimiteCredito(), cliente.getDiasCredito());
    }

    private Sucursal resolverSucursal(Ruta ruta) {
        if (ruta.getSucursal() == null || ruta.getSucursal().getIdSucursal() == null) {
            throw new ReglaNegocioException("Seleccione la sucursal de la ruta.");
        }
        return sucursalService.findById(ruta.getSucursal().getIdSucursal());
    }

    /** El vendedor es opcional, pero si se indica debe estar habilitado y tener el rol de vendedor. */
    private Usuario resolverVendedor(Ruta ruta) {
        if (ruta.getVendedor() == null || ruta.getVendedor().getIdUsuario() == null) {
            return null;
        }
        Usuario vendedor = usuarioService.findById(ruta.getVendedor().getIdUsuario());
        boolean esVendedor = vendedor.getRoles() != null
                && vendedor.getRoles().stream().anyMatch(role -> ROLE_VENDEDOR.equals(role.getRole()));
        if (!esVendedor) {
            throw new ReglaNegocioException("El usuario " + vendedor.getUsuario() + " no tiene el rol de vendedor.");
        }
        if (!vendedor.isEnabled()) {
            throw new ReglaNegocioException("El usuario " + vendedor.getUsuario() + " está deshabilitado.");
        }
        return vendedor;
    }

    /** Una ruta solo puede quedar ACTIVA o INACTIVA; por defecto nace activa. */
    private Estado resolverEstado(Estado solicitado) {
        if (solicitado == null || solicitado.getIdEstado() == null) {
            return estadoService.findByEstado(ESTADO_ACTIVO);
        }
        Estado estado = estadoService.findById(solicitado.getIdEstado());
        if (estado == null
                || (!ESTADO_ACTIVO.equals(estado.getEstado()) && !ESTADO_INACTIVO.equals(estado.getEstado()))) {
            throw new ReglaNegocioException("El estado indicado no es válido para una ruta.");
        }
        return estado;
    }
}
