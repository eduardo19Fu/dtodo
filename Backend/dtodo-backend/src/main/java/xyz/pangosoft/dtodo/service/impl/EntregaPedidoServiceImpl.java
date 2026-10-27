package xyz.pangosoft.dtodo.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import xyz.pangosoft.dtodo.dto.EntregaSolicitudDto;
import xyz.pangosoft.dtodo.dto.PedidoDetalleDto;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.DetalleProforma;
import xyz.pangosoft.dtodo.model.InventarioSucursal;
import xyz.pangosoft.dtodo.model.MovimientoProducto;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.ProformaEntrega;
import xyz.pangosoft.dtodo.model.ProformaEntregaDetalle;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.CondicionPagoEnum;
import xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum;
import xyz.pangosoft.dtodo.model.enums.EstadoPedidoEnum;
import xyz.pangosoft.dtodo.model.enums.TipoMovimientoEnum;
import xyz.pangosoft.dtodo.repository.IFacturaRepository;
import xyz.pangosoft.dtodo.repository.IProformaEntregaRepository;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.repository.ISucursalRepository;
import xyz.pangosoft.dtodo.service.IAmbitoVendedorService;
import xyz.pangosoft.dtodo.service.IEntregaPedidoService;
import xyz.pangosoft.dtodo.service.IInventarioSucursalService;
import xyz.pangosoft.dtodo.service.IMovimientoProductoService;
import xyz.pangosoft.dtodo.service.IPedidoService;
import xyz.pangosoft.dtodo.service.IProformaAuditoriaService;
import xyz.pangosoft.dtodo.service.ISaldoClienteService;
import xyz.pangosoft.dtodo.service.IUsuarioService;
import xyz.pangosoft.dtodo.util.Utils;

@Service
@RequiredArgsConstructor
public class EntregaPedidoServiceImpl implements IEntregaPedidoService {

    static final String ORIGEN = "ENTREGA_PROFORMA";
    static final int MAX_DIAS_ATRAS = 7;
    static final int MAX_ENTREGADO_POR = 150;
    static final int MAX_OBSERVACIONES = 255;

    private final IPedidoService pedidoService;
    private final IProformaRepository proformaRepository;
    private final IProformaEntregaRepository entregaRepository;
    private final ISucursalRepository sucursalRepository;
    private final IInventarioSucursalService inventarioService;
    private final IMovimientoProductoService movimientoService;
    private final IFacturaRepository facturaRepository;
    private final IAmbitoVendedorService ambito;
    private final IUsuarioService usuarioService;
    private final IProformaAuditoriaService auditoria;
    private final ISaldoClienteService saldoService;

    @Transactional
    @Override
    public PedidoDetalleDto registrar(Long idProforma, EntregaSolicitudDto solicitud) {
        Proforma pedido = pedidoService.obtenerPedido(idProforma);
        ambito.exigirProformaPropia(pedido);
        ambito.exigirPedidoAsignado(pedido);
        exigirEntregable(pedido);
        if (solicitud == null) {
            throw new ReglaNegocioException("Indique lo que se entregó.");
        }

        // Un repartidor no decide sobre el inventario: se descuenta de la sucursal del pedido y nunca se marca a mano.
        boolean repartidor = ambito.esRepartidor();
        boolean descontadoAMano = !repartidor && solicitud.isDescontadoAMano();
        Sucursal sucursal = repartidor ? sucursalDelPedido(pedido) : cargarSucursal(solicitud.getIdSucursal());
        LocalDate fechaEntrega = validarFecha(solicitud.getFechaEntrega(), pedido);
        String entregadoPor = repartidor ? null : limpiar(solicitud.getEntregadoPor(), MAX_ENTREGADO_POR, "El nombre de quien entregó");
        String observaciones = limpiar(solicitud.getObservaciones(), MAX_OBSERVACIONES, "Las observaciones");
        Map<Long, Integer> cantidades = validarLineas(pedido, solicitud);
        String notaDeCobro = exigirCobro(pedido, cantidades, solicitud, repartidor);

        // Lo que hay que sacar del inventario por línea: lo entregado que todavía no estaba descontado.
        Map<Long, Integer> cubiertas = new LinkedHashMap<>();
        Map<Integer, Integer> porProducto = new LinkedHashMap<>();
        Map<Integer, Producto> productos = new HashMap<>();
        for (DetalleProforma linea : pedido.getItemsProforma()) {
            Integer cantidad = cantidades.get(linea.getIdDetalle());
            if (cantidad == null) {
                continue;
            }
            int entregadaNueva = valor(linea.getCantidadEntregada()) + cantidad;
            int cubierta = Math.max(0, entregadaNueva - valor(linea.getCantidadDescontada()));
            cubiertas.put(linea.getIdDetalle(), cubierta);
            if (!descontadoAMano && cubierta > 0) {
                Producto producto = linea.getProducto();
                porProducto.merge(producto.getIdProducto(), cubierta, Integer::sum);
                productos.putIfAbsent(producto.getIdProducto(), producto);
            }
        }
        validarExistencias(sucursal, porProducto, productos);

        Usuario usuario = usuarioSesion();
        ProformaEntrega entrega = ProformaEntrega.builder()
                .idProforma(idProforma)
                .sucursal(sucursal)
                .usuario(usuario)
                .entregadoPor(entregadoPor)
                .fechaEntrega(fechaEntrega)
                .fechaRegistro(LocalDateTime.now())
                .descontadoAMano(descontadoAMano)
                .observaciones(observaciones)
                .build();
        for (DetalleProforma linea : pedido.getItemsProforma()) {
            Integer cantidad = cantidades.get(linea.getIdDetalle());
            if (cantidad == null) {
                continue;
            }
            int cubierta = cubiertas.get(linea.getIdDetalle());
            int descontadaPorElSistema = descontadoAMano ? 0 : cubierta;
            entrega.getDetalles().add(ProformaEntregaDetalle.builder()
                    .entrega(entrega)
                    .idDetalle(linea.getIdDetalle())
                    .cantidad(cantidad)
                    .cantidadCubierta(cubierta)
                    .cantidadDescontada(descontadaPorElSistema)
                    .build());
            linea.setCantidadEntregada(valor(linea.getCantidadEntregada()) + cantidad);
            linea.setCantidadDescontada(valor(linea.getCantidadDescontada()) + cubierta);
        }
        ProformaEntrega guardada = entregaRepository.save(entrega);

        for (ProformaEntregaDetalle detalle : guardada.getDetalles()) {
            if (detalle.getCantidadDescontada() > 0) {
                DetalleProforma linea = lineaPorId(pedido, detalle.getIdDetalle());
                movimientoService.save(movimiento(TipoMovimientoEnum.ENTREGA_PROFORMA, linea.getProducto(),
                        detalle.getCantidadDescontada(), sucursal, usuario, guardada.getIdEntrega()));
            }
        }

        proformaRepository.save(pedido);
        String nota = observaciones == null ? null : "Entrega: " + observaciones;
        if (notaDeCobro != null) {
            nota = nota == null ? notaDeCobro : nota + ". " + notaDeCobro;
        }
        pedidoService.cambiarEstado(pedido, estadoSegunLoEntregado(pedido), entregadoPor, nota);
        return pedidoService.detalle(idProforma);
    }

    /**
     * Un pedido de contado no se entrega sin cobrar: lo cobrado con recibos ligados al pedido debe cubrir lo ya entregado más
     * lo de esta entrega. Solo el administrador puede entregar sin cobro y queda anotado en las etapas. Un pedido a crédito
     * no se condiciona al pago (su límite se controla al confirmarlo).
     *
     * @return la nota para las etapas cuando se entregó sin cobro, o {@code null}
     */
    private String exigirCobro(Proforma pedido, Map<Long, Integer> cantidades, EntregaSolicitudDto solicitud, boolean repartidor) {
        if (pedido.getCondicionPago() == CondicionPagoEnum.CREDITO) {
            return null;
        }
        BigDecimal estaEntrega = BigDecimal.ZERO;
        for (DetalleProforma linea : pedido.getItemsProforma()) {
            Integer cantidad = cantidades.get(linea.getIdDetalle());
            if (cantidad != null && linea.getCantidad() != null && linea.getCantidad() > 0 && linea.getSubTotalDescuento() != null) {
                estaEntrega = estaEntrega.add(linea.getSubTotalDescuento().multiply(new BigDecimal(cantidad))
                        .divide(new BigDecimal(linea.getCantidad()), 6, RoundingMode.HALF_UP));
            }
        }
        BigDecimal necesario = saldoService.valorEntregado(pedido.getIdProforma()).add(estaEntrega).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cobrado = saldoService.cobrado(pedido.getIdProforma());
        if (cobrado.compareTo(necesario) >= 0) {
            return null;
        }
        BigDecimal falta = necesario.subtract(cobrado);
        if (solicitud.isEntregarSinPago() && !repartidor && Utils.esAdministrador()) {
            return "Entregado sin cobrar Q" + falta + " (autorizado por el administrador)";
        }
        throw new ReglaNegocioException("El pedido " + pedido.getNoProforma() + " es de contado y falta cobrar Q" + falta
                + ". Emita un recibo de caja ligado al pedido antes de entregar.");
    }

    @Transactional
    @Override
    public PedidoDetalleDto anular(Long idProforma, Long idEntrega, String motivo) {
        String motivoLimpio = auditoria.motivoObligatorio(motivo, "anular la entrega");
        Proforma pedido = pedidoService.obtenerPedido(idProforma);
        ProformaEntrega entrega = entregaRepository.findById(idEntrega).orElse(null);
        if (entrega == null || !idProforma.equals(entrega.getIdProforma())) {
            throw new NotFoundException("La entrega solicitada no se encuentra registrada en este pedido.");
        }
        if (entrega.isAnulada()) {
            throw new ReglaNegocioException("La entrega ya estaba anulada.");
        }
        if (facturaRepository.findVigenteByProformaOrigen(idProforma).isPresent()) {
            throw new ReglaNegocioException("El pedido " + pedido.getNoProforma() + " tiene una factura vigente. "
                    + "Anule primero la factura para poder anular la entrega.");
        }

        Usuario usuario = usuarioSesion();
        for (ProformaEntregaDetalle detalle : entrega.getDetalles()) {
            DetalleProforma linea = lineaPorId(pedido, detalle.getIdDetalle());
            if (linea == null) {
                continue;
            }
            linea.setCantidadEntregada(Math.max(0, valor(linea.getCantidadEntregada()) - detalle.getCantidad()));
            linea.setCantidadDescontada(Math.max(0, valor(linea.getCantidadDescontada()) - detalle.getCantidadCubierta()));
            if (detalle.getCantidadDescontada() > 0) {
                movimientoService.save(movimiento(TipoMovimientoEnum.ANULACION_ENTREGA_PROFORMA, linea.getProducto(),
                        detalle.getCantidadDescontada(), entrega.getSucursal(), usuario, entrega.getIdEntrega()));
            }
        }
        entrega.setAnulada(true);
        entrega.setFechaAnulacion(LocalDateTime.now());
        entrega.setUsuarioAnulacion(usuario);
        entrega.setMotivoAnulacion(motivoLimpio);
        entregaRepository.save(entrega);
        proformaRepository.save(pedido);

        boolean quedaAlgoEntregado = pedido.getItemsProforma().stream().anyMatch(linea -> valor(linea.getCantidadEntregada()) > 0);
        EstadoPedidoEnum nuevo = quedaAlgoEntregado ? estadoSegunLoEntregado(pedido) : EstadoPedidoEnum.EN_RUTA;
        pedidoService.cambiarEstado(pedido, nuevo, null, "Entrega #" + idEntrega + " anulada: " + motivoLimpio);
        return pedidoService.detalle(idProforma);
    }

    @Transactional
    @Override
    public PedidoDetalleDto cerrarConLoEntregado(Long idProforma, String motivo) {
        String motivoLimpio = auditoria.motivoObligatorio(motivo, "cerrar el pedido con lo entregado");
        Proforma pedido = pedidoService.obtenerPedido(idProforma);
        if (pedido.getEstadoPedido() != EstadoPedidoEnum.ENTREGADA_PARCIAL) {
            throw new ReglaNegocioException("Solo se cierra un pedido con entrega parcial.");
        }
        pedidoService.cambiarEstado(pedido, EstadoPedidoEnum.ENTREGADA, null, "Cerrado con lo entregado: " + motivoLimpio);
        return pedidoService.detalle(idProforma);
    }

    // ------------------------------------------------------------------------------------------ validaciones

    private void exigirEntregable(Proforma pedido) {
        EstadoPedidoEnum estado = pedido.getEstadoPedido();
        if (estado != EstadoPedidoEnum.EN_RUTA && estado != EstadoPedidoEnum.ENTREGADA_PARCIAL) {
            throw new ReglaNegocioException(estado == EstadoPedidoEnum.ENTREGADA
                    ? "El pedido " + pedido.getNoProforma() + " ya se entregó completo."
                    : "El pedido " + pedido.getNoProforma() + " debe estar en ruta para registrar su entrega.");
        }
        if (pedido.getEstadoAutorizacion() == EstadoAutorizacionEnum.PENDIENTE
                || pedido.getEstadoAutorizacion() == EstadoAutorizacionEnum.RECHAZADA) {
            throw new ReglaNegocioException("El descuento del pedido " + pedido.getNoProforma()
                    + " no está autorizado; no se puede entregar.");
        }
    }

    private Sucursal sucursalDelPedido(Proforma pedido) {
        if (pedido.getSucursal() == null) {
            throw new ReglaNegocioException("El pedido " + pedido.getNoProforma() + " no tiene sucursal; pida a bodega o al administrador que registre la entrega.");
        }
        return pedido.getSucursal();
    }

    private Sucursal cargarSucursal(Integer idSucursal) {
        if (idSucursal == null) {
            throw new ReglaNegocioException("Elija la sucursal de la que se descuenta el inventario.");
        }
        return sucursalRepository.findById(idSucursal)
                .orElseThrow(() -> new ReglaNegocioException("La sucursal elegida no existe."));
    }

    private LocalDate validarFecha(LocalDate fecha, Proforma pedido) {
        LocalDate hoy = LocalDate.now();
        LocalDate elegida = fecha == null ? hoy : fecha;
        if (elegida.isAfter(hoy)) {
            throw new ReglaNegocioException("La fecha de entrega no puede ser futura.");
        }
        if (elegida.isBefore(hoy.minusDays(MAX_DIAS_ATRAS))) {
            throw new ReglaNegocioException("La fecha de entrega no puede ser de hace más de " + MAX_DIAS_ATRAS + " días.");
        }
        if (pedido.getFechaEmision() != null) {
            LocalDate emision = new java.sql.Date(pedido.getFechaEmision().getTime()).toLocalDate();
            if (elegida.isBefore(emision)) {
                throw new ReglaNegocioException("La fecha de entrega no puede ser anterior a la emisión del pedido ("
                        + emision + ").");
            }
        }
        return elegida;
    }

    private String limpiar(String texto, int maximo, String que) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        String limpio = texto.trim();
        if (limpio.length() > maximo) {
            throw new ReglaNegocioException(que + " admite hasta " + maximo + " caracteres y trae " + limpio.length() + ".");
        }
        return limpio;
    }

    /** Cantidad entregada por línea: positiva, de una línea del pedido, sin repetir y sin pasar de lo pendiente. */
    private Map<Long, Integer> validarLineas(Proforma pedido, EntregaSolicitudDto solicitud) {
        if (solicitud.getLineas() == null || solicitud.getLineas().isEmpty()) {
            throw new ReglaNegocioException("Indique al menos un producto entregado.");
        }
        Map<Long, DetalleProforma> lineas = new HashMap<>();
        for (DetalleProforma linea : pedido.getItemsProforma()) {
            lineas.put(linea.getIdDetalle(), linea);
        }
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        Set<Long> vistas = new HashSet<>();
        for (EntregaSolicitudDto.Linea pedida : solicitud.getLineas()) {
            if (pedida == null || pedida.getIdDetalle() == null || !lineas.containsKey(pedida.getIdDetalle())) {
                throw new ReglaNegocioException("Una de las líneas no pertenece al pedido.");
            }
            if (!vistas.add(pedida.getIdDetalle())) {
                throw new ReglaNegocioException("Una línea del pedido viene repetida en la entrega.");
            }
            DetalleProforma linea = lineas.get(pedida.getIdDetalle());
            String nombre = linea.getProducto() == null || linea.getProducto().getNombre() == null ? "una línea"
                    : linea.getProducto().getNombre();
            int pendiente = linea.getCantidad() - valor(linea.getCantidadEntregada());
            if (pedida.getCantidad() == null || pedida.getCantidad() < 0) {
                throw new ReglaNegocioException("La cantidad entregada de " + nombre + " no es válida.");
            }
            if (pedida.getCantidad() == 0) {
                continue;
            }
            if (pedida.getCantidad() > pendiente) {
                throw new ReglaNegocioException("De " + nombre + " solo quedan " + pendiente + " por entregar y se intentó entregar "
                        + pedida.getCantidad() + ".");
            }
            cantidades.put(pedida.getIdDetalle(), pedida.getCantidad());
        }
        if (cantidades.isEmpty()) {
            throw new ReglaNegocioException("Indique al menos un producto con cantidad entregada.");
        }
        return cantidades;
    }

    private void validarExistencias(Sucursal sucursal, Map<Integer, Integer> porProducto, Map<Integer, Producto> productos) {
        for (Map.Entry<Integer, Integer> necesidad : porProducto.entrySet()) {
            Producto producto = productos.get(necesidad.getKey());
            InventarioSucursal inventario = inventarioService.obtenerParaActualizar(sucursal, producto);
            int disponible = inventario.getStock();
            if (disponible < necesidad.getValue()) {
                throw new ReglaNegocioException("No hay existencias suficientes de " + producto.getNombre() + " en "
                        + sucursal.getNombre() + ". Disponible: " + disponible + ", por descontar: " + necesidad.getValue()
                        + ". Elija otra sucursal o entregue menos.");
            }
        }
    }

    // ------------------------------------------------------------------------------------------ apoyo

    private EstadoPedidoEnum estadoSegunLoEntregado(Proforma pedido) {
        boolean completo = pedido.getItemsProforma().stream()
                .allMatch(linea -> valor(linea.getCantidadEntregada()) >= linea.getCantidad());
        return completo ? EstadoPedidoEnum.ENTREGADA : EstadoPedidoEnum.ENTREGADA_PARCIAL;
    }

    private DetalleProforma lineaPorId(Proforma pedido, Long idDetalle) {
        return pedido.getItemsProforma().stream().filter(linea -> linea.getIdDetalle().equals(idDetalle)).findFirst().orElse(null);
    }

    private Usuario usuarioSesion() {
        Integer idUsuario = ambito.idUsuarioActual();
        if (idUsuario == null) {
            throw new ReglaNegocioException("No se pudo identificar al usuario de la sesión.");
        }
        return usuarioService.findById(idUsuario);
    }

    private MovimientoProducto movimiento(TipoMovimientoEnum tipo, Producto producto, int cantidad, Sucursal sucursal,
                                          Usuario usuario, Long idEntrega) {
        return MovimientoProducto.builder()
                .cantidad(cantidad)
                .tipoDocumentoOrigen(ORIGEN)
                .idDocumentoOrigen(idEntrega)
                .producto(producto)
                .usuario(usuario)
                .sucursal(sucursal)
                .tipoMovimiento(tipo)
                .build();
    }

    private static int valor(Integer numero) {
        return numero == null ? 0 : numero;
    }
}
