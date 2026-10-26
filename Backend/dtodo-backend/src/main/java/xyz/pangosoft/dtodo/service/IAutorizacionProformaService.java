package xyz.pangosoft.dtodo.service;

import java.util.List;

import xyz.pangosoft.dtodo.dto.AutorizacionProformaDto;
import xyz.pangosoft.dtodo.model.Proforma;

/**
 * Bandeja de autorizaciones de las proformas mayoristas. Un administrador aprueba o rechaza las que pasan del tope
 * de descuento y autoriza facturar las vencidas; todo queda en el historial de la proforma. Las reglas responden
 * 422 (401 y 403 los reserva el interceptor del frontend).
 */
public interface IAutorizacionProformaService {

    /** Proformas con el descuento pendiente de aprobar o rechazar. */
    List<AutorizacionProformaDto> pendientes();

    /** Proformas mayoristas vencidas, sin facturar y sin vencimiento autorizado. */
    List<AutorizacionProformaDto> vencidas();

    /** Aprueba el descuento de una proforma {@code PENDIENTE}; el motivo es obligatorio. */
    void aprobar(Long idProforma, String motivo);

    /** Rechaza el descuento de una proforma {@code PENDIENTE}; el motivo es obligatorio. */
    void rechazar(Long idProforma, String motivo);

    /** Autoriza facturar una proforma mayorista vencida; el motivo es obligatorio. */
    void autorizarVencida(Long idProforma, String motivo);

    /**
     * Valida que la proforma pueda facturarse: no puede estar pendiente ni rechazada, ni vencida sin autorización.
     * Sin proforma de origen (venta directa) no hay nada que validar.
     */
    void exigirFacturable(Long idProforma);

    /** 422 si la proforma ya tiene factura: una proforma mayorista facturada no se modifica ni se anula. */
    void exigirSinFactura(Proforma proforma);
}
