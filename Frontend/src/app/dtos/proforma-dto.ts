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
  nitCliente: string;
  /** Factura generada desde la proforma; null/undefined si todavía no se factura. */
  idFactura?: number | null;
  serieFactura?: string | null;
  noFactura?: number | null;
  estadoFactura?: string | null;
  facturadoPor?: string | null;
}
