package xyz.pangosoft.dtodo.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import xyz.pangosoft.dtodo.dto.EntregaDto;
import xyz.pangosoft.dtodo.dto.PedidoDetalleDto;
import xyz.pangosoft.dtodo.dto.ReporteSelectorDto;
import xyz.pangosoft.dtodo.dto.PedidoDto;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.DetalleProforma;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.ProformaEntrega;
import xyz.pangosoft.dtodo.model.ProformaEntregaDetalle;
import xyz.pangosoft.dtodo.model.ProformaEtapa;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum;
import xyz.pangosoft.dtodo.model.enums.EstadoPedidoEnum;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;
import xyz.pangosoft.dtodo.repository.IProformaEntregaRepository;
import xyz.pangosoft.dtodo.repository.IProformaEtapaRepository;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.repository.IUsuarioRepository;
import xyz.pangosoft.dtodo.service.IAmbitoVendedorService;
import xyz.pangosoft.dtodo.service.IPedidoService;
import xyz.pangosoft.dtodo.service.IProformaAuditoriaService;
import xyz.pangosoft.dtodo.service.IUsuarioService;

@Service
@RequiredArgsConstructor
public class PedidoServiceImpl implements IPedidoService {

    static final int MAX_RESPONSABLE = 150;
    /** Tope de filas de la lista; la lista es operativa y se filtra por ruta, etapa y fecha. */
    static final int MAX_FILAS = 300;

    private static final Date SIN_LIMITE_INFERIOR = Date.from(LocalDate.of(2000, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    private static final Date SIN_LIMITE_SUPERIOR = Date.from(LocalDate.of(2100, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());

    private final IProformaRepository proformaRepository;
    private final IProformaEtapaRepository etapaRepository;
    private final IProformaEntregaRepository entregaRepository;
    private final IAmbitoVendedorService ambito;
    private final IUsuarioService usuarioService;
    private final IUsuarioRepository usuarioRepository;
    private final IProformaAuditoriaService auditoria;

    @Transactional(readOnly = true)
    @Override
    public List<PedidoDto> listar(Integer idRuta, EstadoPedidoEnum estado, LocalDate desde, LocalDate hasta) {
        Integer idRepartidor = ambito.idRepartidorForzado();
        List<EstadoPedidoEnum> estados = estado == null ? EstadoPedidoEnum.POR_ENTREGAR : java.util.Collections.singletonList(estado);
        if (idRepartidor != null) {
            // Un repartidor solo ve lo que está en ruta o con entrega parcial.
            estados = estados.stream().filter(e -> e == EstadoPedidoEnum.EN_RUTA || e == EstadoPedidoEnum.ENTREGADA_PARCIAL)
                    .collect(java.util.stream.Collectors.toList());
            if (estados.isEmpty()) {
                return java.util.Collections.emptyList();
            }
        }
        Date inicio = desde == null ? SIN_LIMITE_INFERIOR : Date.from(desde.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date fin = hasta == null ? SIN_LIMITE_SUPERIOR
                : Date.from(hasta.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        return proformaRepository.findPedidos(TipoProformaEnum.MAYORISTA, estados, idRuta, ambito.forzarUsuario(null), idRepartidor, inicio, fin,
                org.springframework.data.domain.PageRequest.of(0, MAX_FILAS));
    }

    @Transactional(readOnly = true)
    @Override
    public PedidoDetalleDto detalle(Long idProforma) {
        Proforma proforma = cargarPedido(idProforma);
        ambito.exigirProformaPropia(proforma);
        ambito.exigirPedidoAsignado(proforma);
        return armarDetalle(proforma);
    }

    @Transactional
    @Override
    public PedidoDetalleDto avanzar(Long idProforma, String responsable, Integer idRepartidor) {
        Proforma proforma = cargarPedido(idProforma);
        EstadoPedidoEnum actual = proforma.getEstadoPedido();
        EstadoPedidoEnum siguiente = actual.siguienteEnBodega();
        if (siguiente == null) {
            throw new ReglaNegocioException(actual.entregado()
                    ? "El pedido " + proforma.getNoProforma() + " ya se entregó."
                    : "El pedido " + proforma.getNoProforma() + " ya salió a ruta; la entrega se registra aparte.");
        }
        exigirAutorizado(proforma);

        String motivo = null;
        if (siguiente == EstadoPedidoEnum.EN_RUTA) {
            Usuario piloto = cargarRepartidor(idRepartidor);
            proforma.setRepartidor(piloto);
            motivo = "Piloto: " + nombre(piloto);
        }

        proforma.setEstadoPedido(siguiente);
        proformaRepository.save(proforma);
        registrarEtapa(proforma.getIdProforma(), siguiente, limpiarResponsable(responsable), motivo);
        return armarDetalle(proforma);
    }

    @Transactional
    @Override
    public PedidoDetalleDto asignarRepartidor(Long idProforma, Integer idRepartidor) {
        Proforma proforma = cargarPedido(idProforma);
        EstadoPedidoEnum estado = proforma.getEstadoPedido();
        if (estado != EstadoPedidoEnum.EN_RUTA && estado != EstadoPedidoEnum.ENTREGADA_PARCIAL) {
            throw new ReglaNegocioException("El piloto solo se cambia mientras el pedido " + proforma.getNoProforma()
                    + " está en ruta o con entrega parcial.");
        }
        Usuario piloto = cargarRepartidor(idRepartidor);
        proforma.setRepartidor(piloto);
        proformaRepository.save(proforma);
        registrarEtapa(proforma.getIdProforma(), estado, null, "Piloto asignado: " + nombre(piloto));
        return armarDetalle(proforma);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ReporteSelectorDto> repartidores() {
        return usuarioRepository.findRepartidores();
    }

    /** Un usuario activo con el rol repartidor; 422 si no se indica o no lo es. */
    private Usuario cargarRepartidor(Integer idRepartidor) {
        if (idRepartidor == null) {
            throw new ReglaNegocioException("Elija el piloto que lleva el pedido.");
        }
        Usuario piloto = usuarioService.findById(idRepartidor);
        boolean esRepartidor = piloto != null && piloto.isEnabled() && piloto.getRoles() != null
                && piloto.getRoles().stream().anyMatch(rol -> "ROLE_REPARTIDOR".equals(rol.getRole()));
        if (!esRepartidor) {
            throw new ReglaNegocioException("El usuario elegido no es un repartidor activo.");
        }
        return piloto;
    }

    private static String nombre(Usuario usuario) {
        return ((usuario.getPrimerNombre() == null ? "" : usuario.getPrimerNombre()) + " "
                + (usuario.getApellido() == null ? "" : usuario.getApellido())).trim();
    }

    @Transactional
    @Override
    public PedidoDetalleDto retroceder(Long idProforma, String motivo) {
        String motivoLimpio = auditoria.motivoObligatorio(motivo, "regresar el pedido a la etapa anterior");
        Proforma proforma = cargarPedido(idProforma);
        EstadoPedidoEnum anterior = proforma.getEstadoPedido().anterior();
        if (anterior == null) {
            throw new ReglaNegocioException(proforma.getEstadoPedido().entregado()
                    ? "El pedido " + proforma.getNoProforma() + " ya se entregó y no puede regresar de etapa."
                    : "El pedido " + proforma.getNoProforma() + " está en la primera etapa.");
        }
        proforma.setEstadoPedido(anterior);
        proformaRepository.save(proforma);
        registrarEtapa(proforma.getIdProforma(), anterior, null, "Regresó de etapa: " + motivoLimpio);
        return armarDetalle(proforma);
    }

    @Transactional
    @Override
    public void registrarConfirmacion(Proforma proforma) {
        if (proforma != null && proforma.getTipoProforma() == TipoProformaEnum.MAYORISTA
                && proforma.getEstadoPedido() == EstadoPedidoEnum.CONFIRMADA) {
            registrarEtapa(proforma.getIdProforma(), EstadoPedidoEnum.CONFIRMADA, null, null);
        }
    }

    @Transactional
    @Override
    public void reiniciarSiEditado(Proforma proforma, String motivoDeLaEdicion) {
        if (proforma == null || proforma.getEstadoPedido() == null) {
            return;
        }
        EstadoPedidoEnum actual = proforma.getEstadoPedido();
        if (actual == EstadoPedidoEnum.CONFIRMADA || actual.entregado()) {
            return;
        }
        proforma.setEstadoPedido(EstadoPedidoEnum.CONFIRMADA);
        proformaRepository.save(proforma);
        registrarEtapa(proforma.getIdProforma(), EstadoPedidoEnum.CONFIRMADA, null,
                "Reiniciado porque se editó la proforma estando " + actual
                        + (motivoDeLaEdicion == null ? "" : ": " + motivoDeLaEdicion));
    }

    @Override
    public Proforma obtenerPedido(Long idProforma) {
        return cargarPedido(idProforma);
    }

    @Transactional
    @Override
    public void cambiarEstado(Proforma pedido, EstadoPedidoEnum nuevo, String responsable, String motivo) {
        pedido.setEstadoPedido(nuevo);
        proformaRepository.save(pedido);
        registrarEtapa(pedido.getIdProforma(), nuevo, limpiarResponsable(responsable), motivo);
    }

    @Override
    public void exigirEditable(Proforma proforma) {
        if (proforma != null && proforma.getEstadoPedido() != null && proforma.getEstadoPedido().entregado()) {
            throw new ReglaNegocioException("El pedido " + proforma.getNoProforma()
                    + " ya se entregó y la proforma no se puede modificar.");
        }
    }

    private Proforma cargarPedido(Long idProforma) {
        Proforma proforma = proformaRepository.findById(idProforma).orElse(null);
        if (proforma == null || proforma.getTipoProforma() != TipoProformaEnum.MAYORISTA || proforma.getEstadoPedido() == null) {
            throw new NotFoundException("El pedido solicitado no se encuentra registrado.");
        }
        return proforma;
    }

    private void exigirAutorizado(Proforma proforma) {
        EstadoAutorizacionEnum autorizacion = proforma.getEstadoAutorizacion();
        if (autorizacion == EstadoAutorizacionEnum.PENDIENTE) {
            throw new ReglaNegocioException("El pedido " + proforma.getNoProforma() + " tiene un descuento mayor a lo "
                    + "permitido y está pendiente de autorización. Un administrador debe aprobarlo antes de prepararlo.");
        }
        if (autorizacion == EstadoAutorizacionEnum.RECHAZADA) {
            throw new ReglaNegocioException("El descuento del pedido " + proforma.getNoProforma()
                    + " fue rechazado; la proforma debe corregirse antes de prepararlo.");
        }
    }

    private String limpiarResponsable(String responsable) {
        if (responsable == null || responsable.trim().isEmpty()) {
            return null;
        }
        String limpio = responsable.trim();
        if (limpio.length() > MAX_RESPONSABLE) {
            throw new ReglaNegocioException("El nombre del responsable admite hasta " + MAX_RESPONSABLE
                    + " caracteres y trae " + limpio.length() + ".");
        }
        return limpio;
    }

    private void registrarEtapa(Long idProforma, EstadoPedidoEnum etapa, String responsable, String motivo) {
        Integer idUsuario = ambito.idUsuarioActual();
        if (idUsuario == null) {
            throw new ReglaNegocioException("No se pudo identificar al usuario de la sesión.");
        }
        etapaRepository.save(ProformaEtapa.builder()
                .idProforma(idProforma)
                .etapa(etapa)
                .usuario(usuarioService.findById(idUsuario))
                .responsable(responsable)
                .motivo(motivo)
                .fecha(LocalDateTime.now())
                .build());
    }

    private PedidoDetalleDto armarDetalle(Proforma proforma) {
        PedidoDetalleDto dto = new PedidoDetalleDto();
        dto.setIdProforma(proforma.getIdProforma());
        dto.setNoProforma(proforma.getNoProforma());
        dto.setFechaEmision(proforma.getFechaEmision());
        dto.setFechaEntregaEstimada(proforma.getFechaEntregaEstimada());
        Cliente cliente = proforma.getCliente();
        if (cliente != null) {
            dto.setCliente(cliente.getNombre());
            dto.setNegocio(cliente.getNombreNegocio());
            dto.setDireccion(cliente.getDireccion());
            dto.setContacto(cliente.getContacto());
            dto.setTelefonoContacto(cliente.getTelefonoContacto());
        }
        dto.setRuta(proforma.getRuta() == null ? null : proforma.getRuta().getNombre());
        if (proforma.getSucursal() != null) {
            dto.setIdSucursal(proforma.getSucursal().getIdSucursal());
            dto.setSucursal(proforma.getSucursal().getNombre());
        }
        if (proforma.getUsuario() != null) {
            dto.setVendedor(((proforma.getUsuario().getPrimerNombre() == null ? "" : proforma.getUsuario().getPrimerNombre()) + " "
                    + (proforma.getUsuario().getApellido() == null ? "" : proforma.getUsuario().getApellido())).trim());
        }
        dto.setEstadoPedido(proforma.getEstadoPedido());
        dto.setEstadoAutorizacion(proforma.getEstadoAutorizacion());
        if (proforma.getRepartidor() != null) {
            dto.setIdRepartidor(proforma.getRepartidor().getIdUsuario());
            dto.setRepartidor(nombre(proforma.getRepartidor()));
        }
        dto.setCondicionPago(proforma.getCondicionPago());
        dto.setDiasCredito(proforma.getDiasCredito());
        dto.setObservaciones(proforma.getObservaciones());
        if (proforma.getItemsProforma() != null) {
            proforma.getItemsProforma().stream()
                    .filter(linea -> linea != null && linea.getProducto() != null)
                    .sorted(Comparator.comparing((DetalleProforma linea) -> linea.getProducto().getNombre() == null ? ""
                            : linea.getProducto().getNombre()))
                    .forEach(linea -> {
                        PedidoDetalleDto.Linea fila = new PedidoDetalleDto.Linea();
                        fila.setIdDetalle(linea.getIdDetalle());
                        fila.setCodigo(linea.getProducto().getCodProducto());
                        fila.setProducto(linea.getProducto().getNombre());
                        fila.setCantidad(linea.getCantidad());
                        fila.setObservacion(linea.getObservacion());
                        int entregada = linea.getCantidadEntregada() == null ? 0 : linea.getCantidadEntregada();
                        fila.setCantidadEntregada(entregada);
                        fila.setPendiente(Math.max(0, (linea.getCantidad() == null ? 0 : linea.getCantidad()) - entregada));
                        dto.getLineas().add(fila);
                    });
        }
        dto.setEtapas(etapaRepository.findHistorial(proforma.getIdProforma()));
        dto.setEntregas(armarEntregas(proforma));
        return dto;
    }

    private java.util.List<EntregaDto> armarEntregas(Proforma proforma) {
        java.util.Map<Long, String> productos = new java.util.HashMap<>();
        if (proforma.getItemsProforma() != null) {
            proforma.getItemsProforma().stream().filter(linea -> linea != null && linea.getProducto() != null)
                    .forEach(linea -> productos.put(linea.getIdDetalle(), linea.getProducto().getNombre()));
        }
        java.util.List<EntregaDto> entregas = new java.util.ArrayList<>();
        for (ProformaEntrega entrega : entregaRepository.findByIdProformaOrderByFechaRegistroAscIdEntregaAsc(proforma.getIdProforma())) {
            EntregaDto dto = new EntregaDto();
            dto.setIdEntrega(entrega.getIdEntrega());
            dto.setFechaEntrega(entrega.getFechaEntrega());
            dto.setFechaRegistro(entrega.getFechaRegistro());
            dto.setUsuario(entrega.getUsuario() == null ? null : ((entrega.getUsuario().getPrimerNombre() == null ? "" : entrega.getUsuario().getPrimerNombre()) + " "
                    + (entrega.getUsuario().getApellido() == null ? "" : entrega.getUsuario().getApellido())).trim());
            dto.setEntregadoPor(entrega.getEntregadoPor());
            if (entrega.getSucursal() != null) {
                dto.setIdSucursal(entrega.getSucursal().getIdSucursal());
                dto.setSucursal(entrega.getSucursal().getNombre());
            }
            dto.setDescontadoAMano(entrega.isDescontadoAMano());
            dto.setObservaciones(entrega.getObservaciones());
            dto.setAnulada(entrega.isAnulada());
            dto.setFechaAnulacion(entrega.getFechaAnulacion());
            dto.setMotivoAnulacion(entrega.getMotivoAnulacion());
            for (ProformaEntregaDetalle detalle : entrega.getDetalles()) {
                EntregaDto.Linea fila = new EntregaDto.Linea();
                fila.setIdDetalle(detalle.getIdDetalle());
                fila.setProducto(productos.get(detalle.getIdDetalle()));
                fila.setCantidad(detalle.getCantidad());
                dto.getLineas().add(fila);
            }
            entregas.add(dto);
        }
        return entregas;
    }
}
