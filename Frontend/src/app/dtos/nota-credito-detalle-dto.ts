import { TipoDocumentoOrigen } from '../models/nota-credito';

export class NotaCreditoDetalleItemDto {
  idNotaDetalle: number;
  idProducto: number;
  codProducto: string;
  producto: string;
  subTotal: number;
  cantidad: number;
  descuento: number;
  subTotalDescuento: number;
}

export class NotaCreditoDetalleDto {
  idNotaCredito: number;
  correlativoFacturaSat: string;
  serieFacturaSat: string;
  tipoDocumentoOrigen: TipoDocumentoOrigen;
  noProforma: string;
  total: number;
  observaciones: string;
  fechaCreacion: Date;
  fechaEntregaEstimada: Date;
  estado: string;
  cliente: string;
  vendedor: string;
  items: NotaCreditoDetalleItemDto[] = [];
}
