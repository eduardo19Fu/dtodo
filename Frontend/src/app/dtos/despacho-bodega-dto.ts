import { EstadoDespachoBodega } from '../models/despacho-bodega';

export class DespachoBodegaDto {
  idDespacho: number;
  fechaRegistro: Date;
  fechaResolucion: Date;
  estado: EstadoDespachoBodega;
  total: number;
  recibidoPor: string;
  observaciones: string;
  idBodega: number;
  bodega: string;
  idSucursalDestino: number;
  sucursalDestino: string;
  usuarioDespacha: string;
  usuarioResuelve: string;
  totalLineas: number;
}
