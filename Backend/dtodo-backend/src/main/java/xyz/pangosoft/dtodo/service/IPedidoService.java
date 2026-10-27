package xyz.pangosoft.dtodo.service;

import java.time.LocalDate;
import java.util.List;

import xyz.pangosoft.dtodo.dto.PedidoDetalleDto;
import xyz.pangosoft.dtodo.dto.ReporteSelectorDto;
import xyz.pangosoft.dtodo.dto.PedidoDto;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.enums.EstadoPedidoEnum;

/**
 * Pedidos mayoristas: una proforma mayorista pasa por {@code CONFIRMADA → EN_PREPARACION → EMPACADA → EN_RUTA}.
 * Bodega avanza cada etapa y queda quién y cuándo; la entrega, que sí descuenta inventario, llega en la fase 2B.
 * Las reglas responden 422 (401 y 403 los reserva el interceptor del frontend).
 */
public interface IPedidoService {

    /**
     * Pedidos de la lista. Sin {@code estado} trae los que todavía están por entregar. El vendedor solo ve los
     * suyos (el servidor lo fuerza). Las fechas, opcionales, filtran por emisión.
     */
    List<PedidoDto> listar(Integer idRuta, EstadoPedidoEnum estado, LocalDate desde, LocalDate hasta);

    /** El pedido con sus líneas y etapas; 404 si la proforma no es un pedido o, para el vendedor, no es suya. */
    PedidoDetalleDto detalle(Long idProforma);

    /**
     * Pasa el pedido a la etapa siguiente. No avanza si el descuento está pendiente o rechazado. {@code responsable}
     * es opcional: el nombre de quien lo hizo cuando no tiene usuario. Al cargarlo a ruta es obligatorio el piloto
     * ({@code idRepartidor}, un usuario activo con el rol repartidor).
     */
    PedidoDetalleDto avanzar(Long idProforma, String responsable, Integer idRepartidor);

    /** Cambia el piloto de un pedido que ya salió a ruta (por ejemplo, si el camión falla). */
    PedidoDetalleDto asignarRepartidor(Long idProforma, Integer idRepartidor);

    /** Los repartidores activos, para elegir el piloto. */
    List<ReporteSelectorDto> repartidores();

    /** Vuelve el pedido a la etapa anterior; el motivo es obligatorio. No aplica a uno ya entregado. */
    PedidoDetalleDto retroceder(Long idProforma, String motivo);

    /** Deja registrada la etapa inicial de una proforma mayorista recién creada. */
    void registrarConfirmacion(Proforma proforma);

    /**
     * Una proforma mayorista editada después de empezar la preparación vuelve a {@code CONFIRMADA}: la hoja de picking
     * que ya se imprimió quedó desactualizada. Deja constancia en las etapas.
     */
    void reiniciarSiEditado(Proforma proforma, String motivoDeLaEdicion);

    /** La proforma como pedido (entidad); 404 si no existe o no es un pedido mayorista. */
    Proforma obtenerPedido(Long idProforma);

    /**
     * Pone el pedido en la etapa indicada y deja constancia en las etapas, con el responsable y el motivo opcionales.
     * Lo usa la entrega, que decide a qué etapa pasa el pedido.
     */
    void cambiarEstado(Proforma pedido, EstadoPedidoEnum nuevo, String responsable, String motivo);

    /** 422 si el pedido ya se entregó (completo o en parte): ya no se modifica. */
    void exigirEditable(Proforma proforma);
}
