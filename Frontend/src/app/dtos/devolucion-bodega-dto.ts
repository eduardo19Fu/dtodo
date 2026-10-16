import { EstadoDevolucionBodega } from '../models/devolucion-bodega';

export class DevolucionBodegaDto {
  idDevolucion: number;
  fechaRegistro: Date;
  fechaResolucion: Date;
  estado: EstadoDevolucionBodega;
  total: number;
  entregadoPor: string;
  motivo: string;
  idSucursalOrigen: number;
  sucursalOrigen: string;
  idBodega: number;
  bodega: string;
  usuarioRegistra: string;
  usuarioResuelve: string;
  totalLineas: number;
}
