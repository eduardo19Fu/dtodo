import { DetalleCompra } from './detalle-compra';
import { Proveedor } from './proveedor';
import { Sucursal } from './sucursal';
import { UsuarioAuxiliar } from './auxiliar/usuario-auxiliar';

export type TipoComprobanteCompra = 'FACTURA' | 'RECIBO' | 'NOTA_ENVIO' | 'TICKET' | 'OTRO';
export type EstadoCompra = 'ACTIVA' | 'ANULADA';

export class Compra {
    idCompra: number;
    fechaCompra: Date;
    fechaRegistro: Date;
    noComprobante: string;
    tipoComprobante: TipoComprobanteCompra;
    total: number;
    costoEnvio = 0;
    observaciones: string;
    estado: EstadoCompra;

    proveedor: Proveedor;
    usuario: UsuarioAuxiliar;
    sucursal: Sucursal;
    items: DetalleCompra[] = [];
}
