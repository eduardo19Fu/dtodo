import { Bodega } from './bodega';
import { DetalleDevolucionBodega } from './detalle-devolucion-bodega';
import { EstadoDespachoBodega } from './despacho-bodega';
import { Sucursal } from './sucursal';
import { UsuarioAuxiliar } from './auxiliar/usuario-auxiliar';

/** Una devolución comparte el ciclo de estados del despacho: PENDIENTE, REALIZADO (aprobada) y CANCELADO (rechazada). */
export type EstadoDevolucionBodega = EstadoDespachoBodega;

export const ETIQUETAS_ESTADO_DEVOLUCION: Record<EstadoDevolucionBodega, string> = {
    PENDIENTE: 'Pendiente de aprobación',
    REALIZADO: 'Aprobada',
    CANCELADO: 'Rechazada'
};

export class DevolucionBodega {
    idDevolucion: number;
    fechaRegistro: Date;
    fechaResolucion: Date;
    estado: EstadoDevolucionBodega;
    total: number;
    entregadoPor: string;
    motivo: string;
    observaciones: string;
    motivoCancelacion: string;

    sucursalOrigen: Sucursal;
    bodega: Bodega;
    usuarioRegistra: UsuarioAuxiliar;
    usuarioResuelve: UsuarioAuxiliar;
    items: DetalleDevolucionBodega[] = [];
}
