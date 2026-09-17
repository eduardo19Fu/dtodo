import { EstadoCompra, TipoComprobanteCompra } from '../models/compra';

export class CompraDto {
  idCompra: number;
  fechaCompra: Date;
  fechaRegistro: Date;
  noComprobante: string;
  tipoComprobante: TipoComprobanteCompra;
  total: number;
  costoEnvio: number;
  estado: EstadoCompra;
  proveedor: string;
  sucursal: string;
  usuario: string;
}
