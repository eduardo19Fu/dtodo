package xyz.pangosoft.dtodo.service.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import xyz.pangosoft.dtodo.dto.HistorialEventoDto;
import xyz.pangosoft.dtodo.dto.HistorialEventoDto.Categoria;
import xyz.pangosoft.dtodo.dto.PedidoEtapaDto;
import xyz.pangosoft.dtodo.dto.ProformaAuditoriaDto;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.model.Factura;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.ProformaEntrega;
import xyz.pangosoft.dtodo.model.ProformaEntregaDetalle;
import xyz.pangosoft.dtodo.model.ReciboCaja;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.AccionAuditoriaEnum;
import xyz.pangosoft.dtodo.model.enums.EstadoPedidoEnum;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;
import xyz.pangosoft.dtodo.repository.IFacturaRepository;
import xyz.pangosoft.dtodo.repository.IProformaAuditoriaRepository;
import xyz.pangosoft.dtodo.repository.IProformaEntregaRepository;
import xyz.pangosoft.dtodo.repository.IProformaEtapaRepository;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.repository.IReciboCajaRepository;
import xyz.pangosoft.dtodo.service.IAmbitoVendedorService;
import xyz.pangosoft.dtodo.service.IHistorialPedidoService;

@Service
@RequiredArgsConstructor
public class HistorialPedidoServiceImpl implements IHistorialPedidoService {

    private static final java.time.format.DateTimeFormatter FECHA = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final IProformaRepository proformaRepository;
    private final IProformaAuditoriaRepository auditoriaRepository;
    private final IProformaEtapaRepository etapaRepository;
    private final IProformaEntregaRepository entregaRepository;
    private final IFacturaRepository facturaRepository;
    private final IReciboCajaRepository reciboRepository;
    private final IAmbitoVendedorService ambito;

    @Transactional(readOnly = true)
    @Override
    public List<HistorialEventoDto> historial(Long idProforma) {
        Proforma pedido = proformaRepository.findById(idProforma).orElse(null);
        if (pedido == null || pedido.getTipoProforma() != TipoProformaEnum.MAYORISTA || pedido.getEstadoPedido() == null) {
            throw new NotFoundException("El pedido solicitado no se encuentra registrado.");
        }
        ambito.exigirProformaPropia(pedido);

        List<HistorialEventoDto> eventos = new ArrayList<>();
        for (ProformaAuditoriaDto a : auditoriaRepository.findHistorial(idProforma)) {
            eventos.add(deAuditoria(a));
        }
        for (PedidoEtapaDto e : etapaRepository.findHistorial(idProforma)) {
            eventos.add(new HistorialEventoDto(e.getFecha(), Categoria.ETAPA, "Etapa: " + etiquetaEtapa(e.getEtapa()), e.getUsuario(),
                    e.getResponsable(), e.getMotivo(), null));
        }
        for (ProformaEntrega entrega : entregaRepository.findByIdProformaOrderByFechaRegistroAscIdEntregaAsc(idProforma)) {
            eventos.addAll(deEntrega(entrega));
        }
        for (Factura factura : facturaRepository.findByIdProformaOrigenOrderByIdFactura(idProforma)) {
            eventos.add(deFactura(factura));
        }
        for (ReciboCaja recibo : reciboRepository.findByProforma_IdProformaOrderByFechaRegistroAscIdReciboAsc(idProforma)) {
            eventos.addAll(deRecibo(recibo));
        }
        // Estable: lo que ocurrió a la misma hora conserva el orden en que se agregó (auditoría, etapas, entregas...).
        eventos.sort(Comparator.comparing(HistorialEventoDto::getFecha, Comparator.nullsFirst(Comparator.naturalOrder())));
        return eventos;
    }

    static HistorialEventoDto deAuditoria(ProformaAuditoriaDto a) {
        AccionAuditoriaEnum accion = a.getAccion();
        Categoria categoria;
        String titulo;
        switch (accion) {
            case CREACION:
                categoria = Categoria.CREACION;
                titulo = "Pedido creado";
                break;
            case MODIFICACION:
                categoria = Categoria.EDICION;
                titulo = "Pedido editado";
                break;
            case ANULACION:
                categoria = Categoria.ANULACION;
                titulo = "Proforma anulada";
                break;
            case AUTORIZACION:
                categoria = Categoria.AUTORIZACION;
                titulo = "Descuento autorizado";
                break;
            case RECHAZO:
                categoria = Categoria.AUTORIZACION;
                titulo = "Descuento rechazado";
                break;
            case AUTORIZACION_VENCIDA:
                categoria = Categoria.AUTORIZACION;
                titulo = "Vencimiento autorizado";
                break;
            case AUTORIZACION_CREDITO:
                categoria = Categoria.AUTORIZACION;
                titulo = "Crédito autorizado";
                break;
            case RECHAZO_CREDITO:
                categoria = Categoria.AUTORIZACION;
                titulo = "Crédito rechazado";
                break;
            case DEVOLUCION_CLIENTE:
                categoria = Categoria.DEVOLUCION;
                titulo = "Devolución de cliente";
                break;
            case CAMBIO_CLIENTE_FACTURA:
                categoria = Categoria.FACTURA;
                titulo = "Cliente cambiado al facturar";
                break;
            default:
                categoria = Categoria.EDICION;
                titulo = accion.name();
                break;
        }
        return new HistorialEventoDto(a.getFecha(), categoria, titulo, a.getUsuario(), null, a.getMotivo(), a.getDetalle());
    }

    static List<HistorialEventoDto> deEntrega(ProformaEntrega entrega) {
        List<HistorialEventoDto> eventos = new ArrayList<>();
        int unidades = entrega.getDetalles().stream().mapToInt(ProformaEntregaDetalle::getCantidad).sum();
        StringBuilder detalle = new StringBuilder(unidades + (unidades == 1 ? " unidad" : " unidades") + " en "
                + entrega.getDetalles().size() + (entrega.getDetalles().size() == 1 ? " línea" : " líneas"));
        if (entrega.getFechaEntrega() != null) {
            detalle.append(" · entregado el ").append(FECHA.format(entrega.getFechaEntrega()));
        }
        if (entrega.getEntregadoPor() != null) {
            detalle.append(" · entregó ").append(entrega.getEntregadoPor());
        }
        if (entrega.isDescontadoAMano()) {
            detalle.append(" · inventario descontado a mano");
        }
        eventos.add(new HistorialEventoDto(entrega.getFechaRegistro(), Categoria.ENTREGA, "Entrega registrada",
                nombre(entrega.getUsuario()), entrega.getEntregadoPor(), entrega.getObservaciones(), detalle.toString()));
        if (entrega.isAnulada()) {
            eventos.add(new HistorialEventoDto(entrega.getFechaAnulacion(), Categoria.ANULACION, "Entrega anulada",
                    nombre(entrega.getUsuarioAnulacion()), null, entrega.getMotivoAnulacion(), "Entrega #" + entrega.getIdEntrega()));
        }
        return eventos;
    }

    static HistorialEventoDto deFactura(Factura factura) {
        String numero = (factura.getSerie() == null || factura.getSerie().isEmpty() ? "" : factura.getSerie() + "-")
                + (factura.getNoFactura() == null ? factura.getIdFactura() : factura.getNoFactura());
        boolean anulada = factura.getEstado() != null && "ANULADO".equals(factura.getEstado().getEstado());
        LocalDateTime fecha = factura.getFecha() == null ? null
                : LocalDateTime.ofInstant(((Date) factura.getFecha()).toInstant(), ZoneId.systemDefault());
        return new HistorialEventoDto(fecha, Categoria.FACTURA, "Factura " + numero + " emitida", nombre(factura.getUsuario()), null, null,
                anulada ? "La factura está anulada." : null);
    }

    static List<HistorialEventoDto> deRecibo(ReciboCaja recibo) {
        List<HistorialEventoDto> eventos = new ArrayList<>();
        String numero = ReciboCajaServiceImpl.numeroVisible(recibo);
        eventos.add(new HistorialEventoDto(recibo.getFechaRegistro(), Categoria.RECIBO, "Recibo " + numero + " emitido",
                nombre(recibo.getUsuario()), null, null, null));
        if (recibo.isAnulado()) {
            eventos.add(new HistorialEventoDto(recibo.getFechaAnulacion(), Categoria.ANULACION, "Recibo " + numero + " anulado",
                    nombre(recibo.getUsuarioAnulacion()), null, recibo.getMotivoAnulacion(), null));
        }
        return eventos;
    }

    static String etiquetaEtapa(EstadoPedidoEnum etapa) {
        if (etapa == null) {
            return "";
        }
        switch (etapa) {
            case CONFIRMADA:
                return "Confirmado";
            case EN_PREPARACION:
                return "En preparación";
            case EMPACADA:
                return "Empacado";
            case EN_RUTA:
                return "En ruta";
            case ENTREGADA_PARCIAL:
                return "Entrega parcial";
            case ENTREGADA:
                return "Entregado";
            default:
                return etapa.name();
        }
    }

    private static String nombre(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return ((usuario.getPrimerNombre() == null ? "" : usuario.getPrimerNombre()) + " "
                + (usuario.getApellido() == null ? "" : usuario.getApellido())).trim();
    }
}
