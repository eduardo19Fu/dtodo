import { UsuarioAuxiliar } from './auxiliar/usuario-auxiliar';
import { Cliente } from './cliente';
import { Estado } from './estado';
import { TipoFactura } from './tipo-factura';
import { DetalleProforma } from './detalle-proforma';

export type TipoProforma = 'MINORISTA' | 'MAYORISTA';
export type EstadoAutorizacion = 'PENDIENTE' | 'APROBADA' | 'RECHAZADA';

export class Proforma {

    idProforma: number;
    noProforma: string;
    total = 0;
    fechaEmision: Date;

    estado: Estado;
    usuario: UsuarioAuxiliar;
    cliente: Cliente;
    tipoFactura: TipoFactura;
    itemsProforma: DetalleProforma[] = [];

    /** MAYORISTA solo para un cliente mayorista con ruta; el servidor lo confirma y toma la ruta del cliente. */
    tipoProforma?: TipoProforma;
    /** Observación general del pedido (hasta 500 caracteres); solo se imprime en la copia de la tienda. */
    observaciones?: string | null;
    /** Fecha de entrega estimada, yyyy-MM-dd. */
    fechaEntregaEstimada?: string | null;
    /** Contado o crédito. A crédito solo si el cliente tiene límite y días de crédito; el servidor lo valida. */
    condicionPago?: 'CONTADO' | 'CREDITO' = 'CONTADO';
    /** PENDIENTE si el pedido a crédito pasa del límite del cliente (lo calcula el servidor); nulo si cabe. */
    creditoAutorizacion?: EstadoAutorizacion | null;
    /** Cuánto pasa del límite de crédito. */
    excesoCredito?: number | null;
    /** Los días de crédito del cliente cuando se hizo el pedido a crédito. */
    diasCredito?: number | null;
    /** Lo calcula el servidor: PENDIENTE si una línea pasa del tope de descuento; nulo si no necesita autorización. */
    estadoAutorizacion?: EstadoAutorizacion | null;
    /** Etapa del pedido (solo la mayorista): la fija el servidor. */
    estadoPedido?: 'CONFIRMADA' | 'EN_PREPARACION' | 'EMPACADA' | 'EN_RUTA' | 'ENTREGADA_PARCIAL' | 'ENTREGADA' | null;
    /** Un administrador autorizó facturarla aunque ya venció. */
    vencimientoAutorizado?: boolean;
    /** Vence a los 15 días de la emisión en una proforma mayorista; la fija el servidor. */
    fechaVencimiento?: string | null;

    calcularTotal(): number{
        this.total = 0;
        this.itemsProforma.forEach((item: DetalleProforma) => {
            this.total += item.calcularImporteDescuento();
        });

        return this.total;
    }

    generarNoProforma(): string {
        const min = 0;
        const max = 10000000000;
        const noProforma = Math.floor(Math.random() * (max - min + 1) + min) + 'P';
        return noProforma;
    }
}
