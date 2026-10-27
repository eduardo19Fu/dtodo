package xyz.pangosoft.dtodo.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.Factura;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.enums.TipoClienteEnum;
import xyz.pangosoft.dtodo.repository.IClienteRepository;
import xyz.pangosoft.dtodo.repository.IFacturaRepository;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.repository.IRutaRepository;
import xyz.pangosoft.dtodo.service.IAmbitoVendedorService;
import xyz.pangosoft.dtodo.service.IUsuarioService;
import xyz.pangosoft.dtodo.util.NitValidator;
import xyz.pangosoft.dtodo.util.Utils;

@Service
@RequiredArgsConstructor
public class AmbitoVendedorServiceImpl implements IAmbitoVendedorService {

    private final IRutaRepository rutaRepository;
    private final IClienteRepository clienteRepository;
    private final IFacturaRepository facturaRepository;
    private final IProformaRepository proformaRepository;
    private final IUsuarioService usuarioService;

    @Override
    public boolean esVendedor() {
        return Utils.esVendedorAcotado();
    }

    @Override
    public boolean esRepartidor() {
        return Utils.esRepartidorAcotado();
    }

    @Override
    public Integer idRepartidorForzado() {
        return esRepartidor() ? idUsuarioActual() : null;
    }

    @Override
    public void exigirPedidoAsignado(Proforma proforma) {
        if (!esRepartidor()) {
            return;
        }
        boolean asignado = proforma != null && proforma.getRepartidor() != null
                && idUsuarioActual().equals(proforma.getRepartidor().getIdUsuario())
                && (proforma.getEstadoPedido() == xyz.pangosoft.dtodo.model.enums.EstadoPedidoEnum.EN_RUTA
                || proforma.getEstadoPedido() == xyz.pangosoft.dtodo.model.enums.EstadoPedidoEnum.ENTREGADA_PARCIAL);
        if (!asignado) {
            throw new NotFoundException("El pedido solicitado no se encuentra entre los suyos.");
        }
    }

    @Override
    public Integer idUsuarioActual() {
        return Utils.idUsuarioAutenticado();
    }

    @Transactional(readOnly = true)
    @Override
    public List<Integer> idsRutasPermitidas() {
        return esVendedor() ? rutaRepository.findIdsByVendedor(idUsuarioActual()) : null;
    }

    @Transactional(readOnly = true)
    @Override
    public boolean clienteEnAmbito(Cliente cliente) {
        if (!esVendedor()) {
            return true;
        }
        return cliente != null && cliente.getRuta() != null
                && idsRutasPermitidas().contains(cliente.getRuta().getIdRuta());
    }

    @Transactional(readOnly = true)
    @Override
    public Cliente limitarLectura(Cliente cliente) {
        if (!clienteEnAmbito(cliente)) {
            throw new NotFoundException("El cliente solicitado no se encuentra registrado en sus rutas.");
        }
        return cliente;
    }

    @Transactional(readOnly = true)
    @Override
    public void exigirClienteEnAmbito(Cliente cliente) {
        if (!esVendedor()) {
            return;
        }
        Cliente completo = cliente == null || cliente.getIdCliente() == null ? null
                : clienteRepository.findById(cliente.getIdCliente()).orElse(null);
        if (completo == null) {
            throw new ReglaNegocioException("Seleccione un cliente de sus rutas.");
        }
        if (!clienteEnAmbito(completo)) {
            throw new ReglaNegocioException("El cliente " + completo.getNombre() + " no pertenece a sus rutas.");
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Cliente prepararAlta(Cliente cliente) {
        if (!esVendedor()) {
            return cliente;
        }
        if (cliente.getIdCliente() != null) {
            throw new ReglaNegocioException("Un cliente existente se modifica con la edición, no con el alta.");
        }
        Integer idRuta = cliente.getRuta() == null ? null : cliente.getRuta().getIdRuta();
        if (idRuta == null || !idsRutasPermitidas().contains(idRuta)) {
            throw new ReglaNegocioException("Seleccione una de sus rutas para el cliente.");
        }
        String nit = cliente.getNit() == null ? "" : cliente.getNit().trim();
        if (!nit.isEmpty() && !NitValidator.esConsumidorFinal(nit) && clienteRepository.existsByNit(nit)) {
            throw new ReglaNegocioException("Ya existe un cliente con el NIT " + nit
                    + ". Pida al administrador que lo asigne a su ruta.");
        }
        cliente.setTipoCliente(TipoClienteEnum.MAYORISTA);
        cliente.setCategoriaDescuento(null);
        cliente.setLimiteCredito(null);
        cliente.setDiasCredito(null);
        return cliente;
    }

    @Transactional
    @Override
    public Cliente prepararEdicion(Cliente solicitud) {
        if (!esVendedor()) {
            return solicitud;
        }
        Cliente existente = solicitud.getIdCliente() == null ? null
                : clienteRepository.findById(solicitud.getIdCliente()).orElse(null);
        limitarLectura(existente);
        existente.setDireccion(solicitud.getDireccion());
        existente.setContacto(solicitud.getContacto());
        existente.setTelefonoContacto(solicitud.getTelefonoContacto());
        existente.setReferenciaUbicacion(solicitud.getReferenciaUbicacion());
        return existente;
    }

    @Override
    public Integer forzarUsuario(Integer idUsuarioSolicitado) {
        return esVendedor() ? idUsuarioActual() : idUsuarioSolicitado;
    }

    @Transactional(readOnly = true)
    @Override
    public void exigirProformaPropia(Proforma proforma) {
        if (!esVendedor()) {
            return;
        }
        boolean propia = proforma != null && proforma.getUsuario() != null
                && idUsuarioActual().equals(proforma.getUsuario().getIdUsuario());
        if (!propia) {
            throw new NotFoundException("La proforma solicitada no se encuentra registrada entre las suyas.");
        }
    }

    @Transactional(readOnly = true)
    @Override
    public void exigirProformaSinFactura(Proforma proforma) {
        if (esVendedor() && proforma != null && facturaRepository.findVigenteByProformaOrigen(proforma.getIdProforma()).isPresent()) {
            throw new ReglaNegocioException("La proforma " + proforma.getNoProforma()
                    + " ya fue facturada y no se puede modificar.");
        }
    }

    @Transactional(readOnly = true)
    @Override
    public void prepararProforma(Proforma proforma) {
        if (!esVendedor()) {
            return;
        }
        exigirClienteEnAmbito(proforma.getCliente());
        proforma.setUsuario(usuarioService.findById(idUsuarioActual()));
    }

    @Transactional(readOnly = true)
    @Override
    public void prepararFactura(Factura factura) {
        if (!esVendedor()) {
            return;
        }
        if (factura.getIdProformaOrigen() == null) {
            throw new ReglaNegocioException("El vendedor solo factura desde una de sus proformas.");
        }
        Proforma origen = proformaRepository.findById(factura.getIdProformaOrigen()).orElse(null);
        exigirProformaPropia(origen);
        exigirClienteEnAmbito(factura.getCliente());
        factura.setUsuario(usuarioService.findById(idUsuarioActual()));
    }

    @Override
    public void exigirUsuarioPropio(Integer idUsuario) {
        if (esVendedor() && !idUsuarioActual().equals(idUsuario)) {
            throw new NotFoundException("El usuario solicitado no se encuentra registrado.");
        }
    }
}
