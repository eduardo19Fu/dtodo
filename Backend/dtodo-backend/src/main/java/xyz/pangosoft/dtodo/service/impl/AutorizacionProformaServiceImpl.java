package xyz.pangosoft.dtodo.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import xyz.pangosoft.dtodo.dto.AutorizacionProformaDto;
import xyz.pangosoft.dtodo.dto.LineaDescuentoDto;
import xyz.pangosoft.dtodo.model.DetalleProforma;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.enums.TipoDescuentoEnum;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.enums.AccionAuditoriaEnum;
import xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;
import xyz.pangosoft.dtodo.repository.IFacturaRepository;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.service.IAutorizacionProformaService;
import xyz.pangosoft.dtodo.service.IProformaAuditoriaService;
import xyz.pangosoft.dtodo.service.IProformaMayoristaService;

@Service
@RequiredArgsConstructor
public class AutorizacionProformaServiceImpl implements IAutorizacionProformaService {

    private final IProformaRepository proformaRepository;
    private final IFacturaRepository facturaRepository;
    private final IProformaAuditoriaService auditoria;
    private final IProformaMayoristaService mayoristaService;

    @Transactional(readOnly = true)
    @Override
    public List<AutorizacionProformaDto> pendientes() {
        List<AutorizacionProformaDto> pendientes = proformaRepository.findPorAutorizar(EstadoAutorizacionEnum.PENDIENTE);
        completarLineas(pendientes);
        return pendientes;
    }

    /** Agrega a cada proforma las líneas que pasan del tope, para que se vea a qué producto se le aplica el descuento. */
    private void completarLineas(List<AutorizacionProformaDto> pendientes) {
        if (pendientes == null || pendientes.isEmpty()) {
            return;
        }
        double tope = mayoristaService.descuentoMaximo();
        Map<Long, Proforma> proformas = new HashMap<>();
        proformaRepository.findAllById(pendientes.stream().map(AutorizacionProformaDto::getIdProforma).collect(Collectors.toList()))
                .forEach(proforma -> proformas.put(proforma.getIdProforma(), proforma));
        for (AutorizacionProformaDto dto : pendientes) {
            dto.setTopeDescuento(tope);
            Proforma proforma = proformas.get(dto.getIdProforma());
            if (proforma == null || proforma.getItemsProforma() == null) {
                continue;
            }
            dto.setLineas(proforma.getItemsProforma().stream()
                    .filter(linea -> linea != null && linea.getTipoDescuento() != TipoDescuentoEnum.PRECIO_MAYORISTA
                            && linea.getDescuento() != null && linea.getDescuento() > tope)
                    .map(AutorizacionProformaServiceImpl::aLinea).collect(Collectors.toList()));
        }
    }

    static LineaDescuentoDto aLinea(DetalleProforma linea) {
        LineaDescuentoDto dto = new LineaDescuentoDto();
        Producto producto = linea.getProducto();
        dto.setCodigo(producto == null ? null : producto.getCodProducto());
        dto.setProducto(producto == null ? null : producto.getNombre());
        dto.setCantidad(linea.getCantidad());
        BigDecimal lista = producto == null || producto.getPrecioVenta() == null ? null : producto.getPrecioVenta();
        dto.setPrecioUnitario(lista);
        dto.setDescuento(linea.getDescuento());
        dto.setTipoDescuento(linea.getTipoDescuento());
        if (lista != null && linea.getCantidad() != null && linea.getSubTotalDescuento() != null) {
            dto.setImporteDescuento(lista.multiply(BigDecimal.valueOf(linea.getCantidad())).subtract(linea.getSubTotalDescuento())
                    .setScale(2, RoundingMode.HALF_UP));
        }
        return dto;
    }

    @Transactional(readOnly = true)
    @Override
    public List<AutorizacionProformaDto> vencidas() {
        return proformaRepository.findVencidasSinAutorizar(TipoProformaEnum.MAYORISTA, LocalDate.now());
    }

    @Transactional
    @Override
    public void aprobar(Long idProforma, String motivo) {
        resolver(idProforma, motivo, EstadoAutorizacionEnum.APROBADA, AccionAuditoriaEnum.AUTORIZACION, "aprobar el descuento");
    }

    @Transactional
    @Override
    public void rechazar(Long idProforma, String motivo) {
        resolver(idProforma, motivo, EstadoAutorizacionEnum.RECHAZADA, AccionAuditoriaEnum.RECHAZO, "rechazar el descuento");
    }

    private void resolver(Long idProforma, String motivo, EstadoAutorizacionEnum nuevoEstado, AccionAuditoriaEnum accion,
                          String queHace) {
        String motivoLimpio = auditoria.motivoObligatorio(motivo, queHace);
        Proforma proforma = cargar(idProforma);
        exigirSinFactura(proforma);
        if (proforma.getEstadoAutorizacion() != EstadoAutorizacionEnum.PENDIENTE) {
            throw new ReglaNegocioException("La proforma " + proforma.getNoProforma()
                    + " no está pendiente de autorización.");
        }
        proforma.setEstadoAutorizacion(nuevoEstado);
        proformaRepository.save(proforma);
        auditoria.registrar(idProforma, accion, motivoLimpio, auditoria.resumen(proforma));
    }

    @Transactional(readOnly = true)
    @Override
    public List<AutorizacionProformaDto> credito() {
        return proformaRepository.findCreditoPorAutorizar(EstadoAutorizacionEnum.PENDIENTE);
    }

    @Transactional
    @Override
    public void aprobarCredito(Long idProforma, String motivo) {
        resolverCredito(idProforma, motivo, EstadoAutorizacionEnum.APROBADA, AccionAuditoriaEnum.AUTORIZACION_CREDITO,
                "autorizar el crédito del pedido");
    }

    @Transactional
    @Override
    public void rechazarCredito(Long idProforma, String motivo) {
        resolverCredito(idProforma, motivo, EstadoAutorizacionEnum.RECHAZADA, AccionAuditoriaEnum.RECHAZO_CREDITO,
                "rechazar el crédito del pedido");
    }

    private void resolverCredito(Long idProforma, String motivo, EstadoAutorizacionEnum nuevoEstado, AccionAuditoriaEnum accion,
                                 String queHace) {
        String motivoLimpio = auditoria.motivoObligatorio(motivo, queHace);
        Proforma proforma = cargar(idProforma);
        exigirSinFactura(proforma);
        if (proforma.getCreditoAutorizacion() != EstadoAutorizacionEnum.PENDIENTE) {
            throw new ReglaNegocioException("El pedido " + proforma.getNoProforma()
                    + " no está pendiente de autorización de crédito.");
        }
        proforma.setCreditoAutorizacion(nuevoEstado);
        proformaRepository.save(proforma);
        auditoria.registrar(idProforma, accion, motivoLimpio, "Excedía el límite por Q" + proforma.getExcesoCredito()
                + ". " + auditoria.resumen(proforma));
    }

    @Transactional
    @Override
    public void autorizarVencida(Long idProforma, String motivo) {
        String motivoLimpio = auditoria.motivoObligatorio(motivo, "autorizar facturar una proforma vencida");
        Proforma proforma = cargar(idProforma);
        exigirSinFactura(proforma);
        if (!estaVencida(proforma)) {
            throw new ReglaNegocioException("La proforma " + proforma.getNoProforma() + " no está vencida.");
        }
        if (proforma.isVencimientoAutorizado()) {
            throw new ReglaNegocioException("El vencimiento de la proforma " + proforma.getNoProforma()
                    + " ya estaba autorizado.");
        }
        proforma.setVencimientoAutorizado(true);
        proformaRepository.save(proforma);
        auditoria.registrar(idProforma, AccionAuditoriaEnum.AUTORIZACION_VENCIDA, motivoLimpio,
                "Venció el " + proforma.getFechaVencimiento() + ". " + auditoria.resumen(proforma));
    }

    @Transactional(readOnly = true)
    @Override
    public void exigirFacturable(Long idProforma) {
        if (idProforma == null) {
            return;
        }
        Proforma proforma = proformaRepository.findById(idProforma).orElse(null);
        if (proforma == null) {
            return;
        }
        if (proforma.getEstadoAutorizacion() == EstadoAutorizacionEnum.PENDIENTE) {
            throw new ReglaNegocioException("La proforma " + proforma.getNoProforma() + " tiene un descuento mayor a "
                    + "lo permitido y está pendiente de autorización. Un administrador debe aprobarla antes de facturar.");
        }
        if (proforma.getEstadoAutorizacion() == EstadoAutorizacionEnum.RECHAZADA) {
            throw new ReglaNegocioException("El descuento de la proforma " + proforma.getNoProforma()
                    + " fue rechazado y no se puede facturar. Corrija la proforma para volver a pedir autorización.");
        }
        if (proforma.getCreditoAutorizacion() == EstadoAutorizacionEnum.PENDIENTE
                || proforma.getCreditoAutorizacion() == EstadoAutorizacionEnum.RECHAZADA) {
            throw new ReglaNegocioException("El crédito de la proforma " + proforma.getNoProforma()
                    + " no está autorizado y no se puede facturar.");
        }
        if (estaVencida(proforma) && !proforma.isVencimientoAutorizado()) {
            throw new ReglaNegocioException("La proforma " + proforma.getNoProforma() + " venció el "
                    + proforma.getFechaVencimiento() + ". Un administrador debe autorizar facturarla.");
        }
    }

    @Transactional(readOnly = true)
    @Override
    public void exigirSinFactura(Proforma proforma) {
        if (proforma != null && proforma.getIdProforma() != null
                && facturaRepository.findVigenteByProformaOrigen(proforma.getIdProforma()).isPresent()) {
            throw new ReglaNegocioException("La proforma " + proforma.getNoProforma()
                    + " ya fue facturada y no se puede modificar.");
        }
    }

    private Proforma cargar(Long idProforma) {
        return proformaRepository.findById(idProforma)
                .orElseThrow(() -> new NotFoundException("La proforma solicitada no se encuentra registrada."));
    }

    static boolean estaVencida(Proforma proforma) {
        return proforma.getTipoProforma() == TipoProformaEnum.MAYORISTA && proforma.getFechaVencimiento() != null
                && proforma.getFechaVencimiento().isBefore(LocalDate.now());
    }
}
