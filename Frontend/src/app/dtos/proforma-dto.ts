export class ProformaDto {
  idProforma: number;
  noProforma: string;
  fechaEmision: Date;
  total: number;
  idEstado: number;
  estado: string;
  usuario: string;
  vendedor: string;
  cliente: string;
  tipoProforma?: 'MINORISTA' | 'MAYORISTA';
  ruta?: string | null;
  estadoAutorizacion?: 'PENDIENTE' | 'APROBADA' | 'RECHAZADA' | null;
  nitCliente: string;
  /** Factura generada desde la proforma; null/undefined si todavía no se factura. */
  idFactura?: number | null;
  serieFactura?: string | null;
  noFactura?: number | null;
  estadoFactura?: string | null;
  /** Etapa del pedido mayorista; nula en una proforma minorista. */
  estadoPedido?: string | null;
  /** Solo una cotización (precios): no es un pedido. */
  esCotizacion?: boolean;
  modalidadEntrega?: string | null;
  facturadoPor?: string | null;
}
