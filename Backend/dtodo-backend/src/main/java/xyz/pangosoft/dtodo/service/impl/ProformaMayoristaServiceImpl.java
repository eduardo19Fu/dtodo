package xyz.pangosoft.dtodo.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.DetalleProforma;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.enums.CondicionPagoEnum;
import xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum;
import xyz.pangosoft.dtodo.model.enums.TipoClienteEnum;
import xyz.pangosoft.dtodo.model.enums.TipoDescuentoEnum;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;
import xyz.pangosoft.dtodo.repository.IClienteRepository;
import xyz.pangosoft.dtodo.service.IAmbitoVendedorService;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.service.IProformaMayoristaService;
import xyz.pangosoft.dtodo.service.ISaldoClienteService;

@Service
@RequiredArgsConstructor
public class ProformaMayoristaServiceImpl implements IProformaMayoristaService {

    static final int MAX_OBSERVACION_LINEA = 255;
    static final int MAX_OBSERVACION_GENERAL = 500;

    private final IClienteRepository clienteRepository;
    private final IAmbitoVendedorService ambito;
    private final ISaldoClienteService saldoService;
    private final IProformaRepository proformaRepository;

    /** Sobre este porcentaje la proforma mayorista pide autorización de un administrador. */
    @Value("${dtodo.mayorista.descuento-maximo:15}")
    private double descuentoMaximo = 15;

    @Override
    public double descuentoMaximo() {
        return descuentoMaximo;
    }

    @Override
    public void evaluarAutorizacion(Proforma proforma) {
        boolean requiere = proforma.getTipoProforma() == TipoProformaEnum.MAYORISTA
                && proforma.getItemsProforma() != null
                && proforma.getItemsProforma().stream().anyMatch(linea -> linea != null
                        && linea.getTipoDescuento() != TipoDescuentoEnum.PRECIO_MAYORISTA
                        && linea.getDescuento() != null && linea.getDescuento() > descuentoMaximo);
        proforma.setEstadoAutorizacion(requiere ? EstadoAutorizacionEnum.PENDIENTE : null);
    }

    @Transactional(readOnly = true)
    @Override
    public void evaluarCredito(Proforma proforma) {
        proforma.setCreditoAutorizacion(null);
        proforma.setExcesoCredito(null);
        if (proforma.getTipoProforma() != TipoProformaEnum.MAYORISTA || proforma.getCondicionPago() != CondicionPagoEnum.CREDITO
                || proforma.getCliente() == null || proforma.getCliente().getIdCliente() == null) {
            return;
        }
        Cliente cliente = clienteRepository.findById(proforma.getCliente().getIdCliente()).orElse(null);
        if (cliente == null || cliente.getLimiteCredito() == null) {
            return;
        }
        BigDecimal exposicion = saldoService.exposicionDeCredito(cliente.getIdCliente(), proforma.getIdProforma())
                .add(totalDe(proforma));
        BigDecimal exceso = exposicion.subtract(cliente.getLimiteCredito()).setScale(2, RoundingMode.HALF_UP);
        if (exceso.signum() <= 0) {
            return;
        }
        Proforma previa = proforma.getIdProforma() == null ? null : proformaRepository.findById(proforma.getIdProforma()).orElse(null);
        if (previa != null && previa.getCreditoAutorizacion() == EstadoAutorizacionEnum.APROBADA
                && previa.getExcesoCredito() != null && exceso.compareTo(previa.getExcesoCredito()) <= 0) {
            proforma.setCreditoAutorizacion(EstadoAutorizacionEnum.APROBADA);
            proforma.setExcesoCredito(previa.getExcesoCredito());
            return;
        }
        proforma.setCreditoAutorizacion(EstadoAutorizacionEnum.PENDIENTE);
        proforma.setExcesoCredito(exceso);
    }

    /** El total del pedido según sus líneas (con descuento); si alguna no lo trae, el total de la proforma. */
    static BigDecimal totalDe(Proforma proforma) {
        BigDecimal suma = BigDecimal.ZERO;
        if (proforma.getItemsProforma() != null && !proforma.getItemsProforma().isEmpty()) {
            for (DetalleProforma linea : proforma.getItemsProforma()) {
                if (linea == null || linea.getSubTotalDescuento() == null) {
                    return proforma.getTotal() == null ? BigDecimal.ZERO : proforma.getTotal();
                }
                suma = suma.add(linea.getSubTotalDescuento());
            }
            return suma;
        }
        return proforma.getTotal() == null ? BigDecimal.ZERO : proforma.getTotal();
    }

    @Transactional(readOnly = true)
    @Override
    public void preparar(Proforma proforma) {
        TipoProformaEnum tipo = proforma.getTipoProforma() == null ? TipoProformaEnum.MINORISTA : proforma.getTipoProforma();
        if (ambito.esVendedor()) {
            tipo = TipoProformaEnum.MAYORISTA;
        }

        if (tipo == TipoProformaEnum.MAYORISTA) {
            Cliente cliente = proforma.getCliente() == null || proforma.getCliente().getIdCliente() == null ? null
                    : clienteRepository.findById(proforma.getCliente().getIdCliente()).orElse(null);
            if (cliente == null || cliente.getTipoCliente() != TipoClienteEnum.MAYORISTA || cliente.getRuta() == null) {
                throw new ReglaNegocioException("Una proforma mayorista solo puede hacerse a un cliente mayorista con ruta. "
                        + "Para otro cliente use una proforma normal.");
            }
            proforma.setRuta(cliente.getRuta());
            definirCondicionDePago(proforma, cliente);
        } else {
            proforma.setRuta(null);
            proforma.setCondicionPago(CondicionPagoEnum.CONTADO);
            proforma.setDiasCredito(null);
        }
        proforma.setTipoProforma(tipo);
        // La etapa del pedido la fija el servidor; nunca viene en la petición.
        proforma.setEstadoPedido(null);
        // La autorización del vencimiento la da un administrador; nunca viene en la petición.
        proforma.setVencimientoAutorizado(false);

        proforma.setObservaciones(limpiar(proforma.getObservaciones(), MAX_OBSERVACION_GENERAL, "La observación general"));
        if (proforma.getItemsProforma() != null) {
            for (DetalleProforma linea : proforma.getItemsProforma()) {
                if (linea != null) {
                    // Lo entregado y lo descontado lo fija el servidor: una edición nunca lo trae de la petición.
                    linea.setCantidadEntregada(0);
                    linea.setCantidadDescontada(0);
                    String producto = linea.getProducto() == null || linea.getProducto().getNombre() == null ? "una línea"
                            : linea.getProducto().getNombre();
                    linea.setObservacion(limpiar(linea.getObservacion(), MAX_OBSERVACION_LINEA,
                            "La observación de " + producto));
                }
            }
        }
    }

    /**
     * Contado por defecto. A crédito solo si el cliente tiene límite y días de crédito; los días del cliente quedan en el
     * pedido para que un cambio posterior no altere los plazos ya pactados.
     */
    static void definirCondicionDePago(Proforma proforma, Cliente cliente) {
        CondicionPagoEnum condicion = proforma.getCondicionPago() == null ? CondicionPagoEnum.CONTADO : proforma.getCondicionPago();
        if (condicion == CondicionPagoEnum.CREDITO) {
            boolean conCredito = cliente.getLimiteCredito() != null && cliente.getLimiteCredito().signum() > 0
                    && cliente.getDiasCredito() != null && cliente.getDiasCredito() > 0;
            if (!conCredito) {
                throw new ReglaNegocioException("El cliente " + cliente.getNombre()
                        + " no tiene límite ni días de crédito: el pedido debe ser de contado.");
            }
            proforma.setDiasCredito(cliente.getDiasCredito());
        } else {
            proforma.setDiasCredito(null);
        }
        proforma.setCondicionPago(condicion);
    }

    /** Texto sin espacios sobrantes; vacío es nulo; más largo que el máximo se rechaza en lugar de cortarse. */
    static String limpiar(String texto, int maximo, String que) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        String limpio = texto.trim();
        if (limpio.length() > maximo) {
            throw new ReglaNegocioException(que + " admite hasta " + maximo + " caracteres y trae " + limpio.length() + ".");
        }
        return limpio;
    }
}
