import { Bodega } from './bodega';
import { DetalleDespachoBodega } from './detalle-despacho-bodega';
import { Sucursal } from './sucursal';
import { UsuarioAuxiliar } from './auxiliar/usuario-auxiliar';

export type EstadoDespachoBodega = 'PENDIENTE' | 'REALIZADO' | 'CANCELADO';

export const ETIQUETAS_ESTADO_DESPACHO: Record<EstadoDespachoBodega, string> = {
    PENDIENTE: 'Pendiente de aprobación',
    REALIZADO: 'Realizado',
    CANCELADO: 'Cancelado'
};

export class DespachoBodega {
    idDespacho: number;
    fechaRegistro: Date;
    fechaResolucion: Date;
    estado: EstadoDespachoBodega;
    total: number;
    recibidoPor: string;
    observaciones: string;
    motivoCancelacion: string;

    bodega: Bodega;
    sucursalDestino: Sucursal;
    usuarioDespacha: UsuarioAuxiliar;
    usuarioResuelve: UsuarioAuxiliar;
    items: DetalleDespachoBodega[] = [];
}
