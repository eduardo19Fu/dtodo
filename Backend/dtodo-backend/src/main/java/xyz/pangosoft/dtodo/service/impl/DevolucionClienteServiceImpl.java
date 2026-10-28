package xyz.pangosoft.dtodo.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.pangosoft.dtodo.dto.CrearDevolucionClienteDto;
import xyz.pangosoft.dtodo.dto.DevolucionClienteDto;
import xyz.pangosoft.dtodo.dto.PedidoDevolubleDto;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.DetalleProforma;
import xyz.pangosoft.dtodo.model.DevolucionCliente;
import xyz.pangosoft.dtodo.model.DevolucionClienteDetalle;
import xyz.pangosoft.dtodo.model.Factura;
import xyz.pangosoft.dtodo.model.MovimientoProducto;
import xyz.pangosoft.dtodo.model.NotaCredito;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.AccionAuditoriaEnum;
import xyz.pangosoft.dtodo.model.enums.DestinoDevolucionEnum;
import xyz.pangosoft.dtodo.model.enums.EstadoNotaCreditoEnum;
import xyz.pangosoft.dtodo.model.enums.TipoDocumentoOrigenEnum;
import xyz.pangosoft.dtodo.model.enums.TipoMovimientoEnum;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;
import xyz.pangosoft.dtodo.repository.IDevolucionClienteRepository;
import xyz.pangosoft.dtodo.repository.IFacturaRepository;
import xyz.pangosoft.dtodo.repository.INotaCreditoRepository;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.repository.ISucursalRepository;
import xyz.pangosoft.dtodo.service.IAmbitoVendedorService;
import xyz.pangosoft.dtodo.service.IDevolucionClienteService;
import xyz.pangosoft.dtodo.service.IMovimientoProductoService;
import xyz.pangosoft.dtodo.service.IProformaAuditoriaService;
import xyz.pangosoft.dtodo.service.IUsuarioService;

@Service
@RequiredArgsConstructor
@Slf4j
public class DevolucionClienteServiceImpl implements IDevolucionClienteService {

    static final String ORIGEN = "DEVOLUCION_CLIENTE";
    static final int DIAS_POR_DEFECTO = 90;

    private final IDevolucionClienteRepository devolucionRepository;
    private final IProformaRepository proformaRepository;
    private final IFacturaRepository facturaRepository;
    private final INotaCreditoRepository notaCreditoRepository;
    private final ISucursalRepository sucursalRepository;
    private final IMovimientoProductoService movimientoService;
    private final IAmbitoVendedorService ambito;
    private final IUsuarioService usuarioService;
    private final IProformaAuditoriaService auditoria;

    @Transactional
    @Override
    public DevolucionClienteDto registrar(CrearDevolucionClienteDto solicitud) {
        if (solicitud == null || solicitud.getIdProforma() == null) {
            throw new ReglaNegocioException("Indique el pedido del que se devuelve.");
        }
        // El pedido se bloquea: dos devoluciones simultáneas de lo mismo se serializan y la segunda ve la primera.
        Proforma pedido = facturaRepository.bloquearProforma(solicitud.getIdProforma())
                .orElseThrow(() -> new NotFoundException("El pedido solicitado no se encuentra registrado."));
        exigirPedidoDevoluble(pedido);
        String motivo = auditoria.motivoObligatorio(solicitud.getMotivo(), "registrar una devolución");
        LocalDate fecha = solicitud.getFecha() == null ? LocalDate.now() : solicitud.getFecha();
        if (fecha.isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("La fecha de la devolución no puede ser futura.");
        }
        Usuario usuario = usuarioDeLaSesion();

        Map<Long, DetalleProforma> lineasDelPedido = new HashMap<>();
        pedido.getItemsProforma().forEach(linea -> lineasDelPedido.put(linea.getIdDetalle(), linea));
        List<DevolucionClienteDetalle> detalles = validarLineas(solicitud, lineasDelPedido);

        boolean reingresa = detalles.stream().anyMatch(d -> d.getDestino() == DestinoDevolucionEnum.REINGRESO);
        Sucursal sucursal = reingresa ? sucursalDeReingreso(solicitud.getIdSucursal(), pedido) : null;
        Long idNota = notaDeCredito(solicitud.getIdNotaCredito(), pedido);

        BigDecimal total = BigDecimal.ZERO;
        for (DevolucionClienteDetalle detalle : detalles) {
            DetalleProforma linea = lineasDelPedido.get(detalle.getIdDetalle());
            detalle.setValor(valorDeLinea(linea, detalle.getCantidad()));
            total = total.add(detalle.getValor());
        }

        DevolucionCliente devolucion = DevolucionCliente.builder()
                .idProforma(pedido.getIdProforma())
                .cliente(pedido.getCliente())
                .sucursal(sucursal)
                .idNotaCredito(idNota)
                .fecha(fecha)
                .motivo(motivo)
                .total(total)
                .usuario(usuario)
                .fechaRegistro(LocalDateTime.now())
                .build();
        detalles.forEach(detalle -> detalle.setDevolucion(devolucion));
        devolucion.getDetalles().addAll(detalles);
        DevolucionCliente guardada = devolucionRepository.save(devolucion);

        for (DevolucionClienteDetalle detalle : guardada.getDetalles()) {
            DetalleProforma linea = lineasDelPedido.get(detalle.getIdDetalle());
            linea.setCantidadDevuelta(valor(linea.getCantidadDevuelta()) + detalle.getCantidad());
            if (detalle.getDestino() == DestinoDevolucionEnum.REINGRESO) {
                movimientoService.save(MovimientoProducto.builder()
                        .cantidad(detalle.getCantidad())
                        .tipoDocumentoOrigen(ORIGEN)
                        .idDocumentoOrigen(guardada.getIdDevolucion())
                        .producto(linea.getProducto())
                        .usuario(usuario)
                        .sucursal(sucursal)
                        .tipoMovimiento(TipoMovimientoEnum.DEVOLUCION_CLIENTE)
                        .build());
            }
        }
        proformaRepository.save(pedido);
        auditoria.registrar(pedido.getIdProforma(), AccionAuditoriaEnum.DEVOLUCION_CLIENTE, motivo,
                "Devolución #" + guardada.getIdDevolucion() + " por Q" + total + ": " + resumenDeLineas(guardada, lineasDelPedido));
        log.info("Devolución {} registrada del pedido {} por Q{}", guardada.getIdDevolucion(), pedido.getNoProforma(), total);
        return aDto(guardada, pedido, true);
    }

    private static void exigirPedidoDevoluble(Proforma pedido) {
        boolean anulado = pedido.getEstado() != null && "ANULADO".equals(pedido.getEstado().getEstado());
        if (pedido.getTipoProforma() != TipoProformaEnum.MAYORISTA || pedido.getEstadoPedido() == null || anulado) {
            throw new NotFoundException("El pedido solicitado no se encuentra registrado.");
        }
        boolean algoEntregado = pedido.getItemsProforma().stream().anyMatch(l -> valor(l.getCantidadEntregada()) > 0);
        if (!algoEntregado) {
            throw new ReglaNegocioException("El pedido " + pedido.getNoProforma() + " no tiene nada entregado que devolver.");
        }
    }

    /** Cada línea debe ser del pedido, una sola vez, con unidades enteras y sin pasar de lo entregado menos lo ya devuelto. */
    static List<DevolucionClienteDetalle> validarLineas(CrearDevolucionClienteDto solicitud, Map<Long, DetalleProforma> lineasDelPedido) {
        if (solicitud.getLineas() == null || solicitud.getLineas().isEmpty()) {
            throw new ReglaNegocioException("Indique al menos una línea que se devuelve.");
        }
        List<DevolucionClienteDetalle> detalles = new ArrayList<>();
        Set<Long> vistas = new HashSet<>();
        for (CrearDevolucionClienteDto.Linea pedida : solicitud.getLineas()) {
            if (pedida == null || pedida.getIdDetalle() == null) {
                throw new ReglaNegocioException("Una línea de la devolución no indica el producto.");
            }
            DetalleProforma linea = lineasDelPedido.get(pedida.getIdDetalle());
            if (linea == null) {
                throw new ReglaNegocioException("Una línea no pertenece al pedido.");
            }
            if (!vistas.add(pedida.getIdDetalle())) {
                throw new ReglaNegocioException("La línea de " + nombre(linea) + " está repetida.");
            }
            int cantidad = pedida.getCantidad() == null ? 0 : pedida.getCantidad();
            if (cantidad <= 0) {
                throw new ReglaNegocioException("La cantidad de " + nombre(linea) + " debe ser mayor que cero.");
            }
            int devolvible = valor(linea.getCantidadEntregada()) - valor(linea.getCantidadDevuelta());
            if (cantidad > devolvible) {
                throw new ReglaNegocioException("De " + nombre(linea) + " solo se pueden devolver " + Math.max(devolvible, 0)
                        + " (entregado menos lo ya devuelto).");
            }
            detalles.add(DevolucionClienteDetalle.builder().idDetalle(pedida.getIdDetalle()).cantidad(cantidad)
                    .destino(destino(pedida.getDestino(), linea)).build());
        }
        return detalles;
    }

    private static DestinoDevolucionEnum destino(String texto, DetalleProforma linea) {
        try {
            return DestinoDevolucionEnum.valueOf(texto == null ? "" : texto.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ReglaNegocioException("Indique si " + nombre(linea) + " reingresa al inventario o se da de baja.");
        }
    }

    private Sucursal sucursalDeReingreso(Integer idSolicitada, Proforma pedido) {
        if (idSolicitada != null) {
            return sucursalRepository.findById(idSolicitada)
                    .orElseThrow(() -> new ReglaNegocioException("La sucursal indicada no existe."));
        }
        if (pedido.getSucursal() == null) {
            throw new ReglaNegocioException("Elija la sucursal que recibe lo que reingresa al inventario.");
        }
        return pedido.getSucursal();
    }

    /** Con factura vigente la devolución debe ligarse a una nota de crédito vigente del cliente que corresponda al pedido. */
    private Long notaDeCredito(Long idNota, Proforma pedido) {
        Factura factura = facturaRepository.findVigenteByProformaOrigen(pedido.getIdProforma()).orElse(null);
        if (factura == null) {
            return null;
        }
        if (idNota == null) {
            throw new ReglaNegocioException("El pedido " + pedido.getNoProforma() + " tiene una factura vigente: ligue la devolución "
                    + "a una nota de crédito.");
        }
        NotaCredito nota = notaCreditoRepository.findById(idNota)
                .orElseThrow(() -> new ReglaNegocioException("La nota de crédito indicada no existe."));
        if (!notaCorresponde(nota, pedido, factura)) {
            throw new ReglaNegocioException("La nota de crédito " + idNota + " no corresponde a este pedido ni a su factura.");
        }
        return nota.getIdNotaCredito();
    }

    /** Vigente, del mismo cliente y emitida sobre la factura vigente del pedido o sobre el propio pedido. */
    static boolean notaCorresponde(NotaCredito nota, Proforma pedido, Factura factura) {
        if (nota.getEstado() == EstadoNotaCreditoEnum.ANULADO || nota.getCliente() == null || pedido.getCliente() == null
                || !nota.getCliente().getIdCliente().equals(pedido.getCliente().getIdCliente())) {
            return false;
        }
        if (nota.getTipoDocumentoOrigen() == TipoDocumentoOrigenEnum.PROFORMA) {
            return pedido.getNoProforma() != null && pedido.getNoProforma().equals(nota.getNoProforma());
        }
        return factura != null && factura.getCorrelativoSat() != null && factura.getCorrelativoSat().equals(nota.getCorrelativoFacturaSat())
                && (factura.getSerieSat() == null || factura.getSerieSat().equals(nota.getSerieFacturaSat()));
    }

    /** Unidades x importe de la línea con descuento / unidades de la línea, a dos decimales. */
    static BigDecimal valorDeLinea(DetalleProforma linea, int unidades) {
        if (linea.getCantidad() == null || linea.getCantidad() <= 0 || linea.getSubTotalDescuento() == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        return linea.getSubTotalDescuento().multiply(new BigDecimal(unidades))
                .divide(new BigDecimal(linea.getCantidad()), 2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------------------------------ consultas

    @Transactional(readOnly = true)
    @Override
    public DevolucionClienteDto buscar(Long idDevolucion) {
        DevolucionCliente devolucion = devolucionRepository.findById(idDevolucion)
                .orElseThrow(() -> new NotFoundException("La devolución solicitada no se encuentra registrada."));
        return aDto(devolucion, proformaRepository.findById(devolucion.getIdProforma()).orElse(null), true);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<DevolucionClienteDto> listar(Integer idCliente, LocalDate desde, LocalDate hasta, Pageable pageable) {
        LocalDate fin = hasta == null ? LocalDate.now() : hasta;
        LocalDate inicio = desde == null ? fin.minusDays(DIAS_POR_DEFECTO) : desde;
        if (inicio.isAfter(fin)) {
            throw new ReglaNegocioException("La fecha inicial no puede ser posterior a la final.");
        }
        return devolucionRepository.buscar(idCliente, inicio, fin, pageable)
                .map(d -> aDto(d, proformaRepository.findById(d.getIdProforma()).orElse(null), false));
    }

    @Transactional(readOnly = true)
    @Override
    public List<PedidoDevolubleDto> pedidosDevolubles(Integer idCliente) {
        List<NotaCredito> notas = notaCreditoRepository.findByCliente_IdClienteAndEstadoNot(idCliente, EstadoNotaCreditoEnum.ANULADO);
        List<PedidoDevolubleDto> resultado = new ArrayList<>();
        for (Proforma pedido : proformaRepository.pedidosVigentesDelCliente(idCliente)) {
            if (pedido.getItemsProforma().stream().noneMatch(l -> valor(l.getCantidadEntregada()) > 0)) {
                continue;
            }
            PedidoDevolubleDto dto = new PedidoDevolubleDto();
            dto.setIdProforma(pedido.getIdProforma());
            dto.setNoProforma(pedido.getNoProforma());
            dto.setEstadoPedido(pedido.getEstadoPedido());
            if (pedido.getSucursal() != null) {
                dto.setIdSucursal(pedido.getSucursal().getIdSucursal());
                dto.setSucursal(pedido.getSucursal().getNombre());
            }
            Factura factura = facturaRepository.findVigenteByProformaOrigen(pedido.getIdProforma()).orElse(null);
            dto.setFacturado(factura != null);
            dto.setNoFactura(factura == null ? null : factura.getNoFactura());
            for (DetalleProforma linea : pedido.getItemsProforma()) {
                int entregada = valor(linea.getCantidadEntregada());
                if (entregada <= 0) {
                    continue;
                }
                PedidoDevolubleDto.Linea l = new PedidoDevolubleDto.Linea();
                l.setIdDetalle(linea.getIdDetalle());
                l.setCodigo(linea.getProducto() == null ? null : linea.getProducto().getCodProducto());
                l.setProducto(nombre(linea));
                l.setEntregada(entregada);
                l.setDevuelta(valor(linea.getCantidadDevuelta()));
                l.setDevolvible(Math.max(0, entregada - l.getDevuelta()));
                l.setPrecioUnitario(valorDeLinea(linea, 1));
                dto.getLineas().add(l);
            }
            if (factura != null) {
                for (NotaCredito nota : notas) {
                    if (notaCorresponde(nota, pedido, factura)) {
                        PedidoDevolubleDto.NotaCreditoOpcion opcion = new PedidoDevolubleDto.NotaCreditoOpcion();
                        opcion.setIdNotaCredito(nota.getIdNotaCredito());
                        opcion.setDescripcion("Nota de crédito #" + nota.getIdNotaCredito());
                        opcion.setTotal(nota.getTotal());
                        dto.getNotasCredito().add(opcion);
                    }
                }
            }
            resultado.add(dto);
        }
        return resultado;
    }

    // ------------------------------------------------------------------------------------------ apoyo

    private DevolucionClienteDto aDto(DevolucionCliente devolucion, Proforma pedido, boolean conLineas) {
        DevolucionClienteDto dto = new DevolucionClienteDto();
        dto.setIdDevolucion(devolucion.getIdDevolucion());
        dto.setIdProforma(devolucion.getIdProforma());
        dto.setNoProforma(pedido == null ? null : pedido.getNoProforma());
        dto.setIdCliente(devolucion.getCliente().getIdCliente());
        dto.setCliente(devolucion.getCliente().getNombre());
        if (devolucion.getSucursal() != null) {
            dto.setIdSucursal(devolucion.getSucursal().getIdSucursal());
            dto.setSucursal(devolucion.getSucursal().getNombre());
        }
        dto.setIdNotaCredito(devolucion.getIdNotaCredito());
        dto.setFecha(devolucion.getFecha());
        dto.setMotivo(devolucion.getMotivo());
        dto.setTotal(devolucion.getTotal());
        dto.setUsuario(nombreUsuario(devolucion.getUsuario()));
        dto.setFechaRegistro(devolucion.getFechaRegistro());
        if (conLineas) {
            Map<Long, DetalleProforma> lineas = new HashMap<>();
            if (pedido != null) {
                pedido.getItemsProforma().forEach(l -> lineas.put(l.getIdDetalle(), l));
            }
            for (DevolucionClienteDetalle detalle : devolucion.getDetalles()) {
                DevolucionClienteDto.Linea l = new DevolucionClienteDto.Linea();
                DetalleProforma linea = lineas.get(detalle.getIdDetalle());
                l.setIdDetalle(detalle.getIdDetalle());
                l.setCodigo(linea == null || linea.getProducto() == null ? null : linea.getProducto().getCodProducto());
                l.setProducto(linea == null ? "Línea " + detalle.getIdDetalle() : nombre(linea));
                l.setCantidad(detalle.getCantidad());
                l.setDestino(detalle.getDestino().name());
                l.setValor(detalle.getValor());
                dto.getLineas().add(l);
            }
        }
        return dto;
    }

    private static String resumenDeLineas(DevolucionCliente devolucion, Map<Long, DetalleProforma> lineas) {
        return devolucion.getDetalles().stream()
                .map(d -> d.getCantidad() + " x " + nombre(lineas.get(d.getIdDetalle())) + " (" + d.getDestino().name().toLowerCase() + ")")
                .collect(Collectors.joining("; "));
    }

    private Usuario usuarioDeLaSesion() {
        Integer idUsuario = ambito.idUsuarioActual();
        if (idUsuario == null) {
            throw new ReglaNegocioException("No se pudo identificar al usuario de la sesión.");
        }
        return usuarioService.findById(idUsuario);
    }

    private static String nombre(DetalleProforma linea) {
        return linea == null || linea.getProducto() == null || linea.getProducto().getNombre() == null ? "la línea"
                : linea.getProducto().getNombre();
    }

    private static String nombreUsuario(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return ((usuario.getPrimerNombre() == null ? "" : usuario.getPrimerNombre()) + " "
                + (usuario.getApellido() == null ? "" : usuario.getApellido())).trim();
    }

    private static int valor(Integer numero) {
        return numero == null ? 0 : numero;
    }
}
