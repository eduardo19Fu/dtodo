export type FormaPagoRecibo = 'EFECTIVO' | 'DEPOSITO' | 'TRANSFERENCIA';

/** Un recibo de caja en la lista y el detalle. */
export class ReciboCajaDto {
  idRecibo: number;
  numero: number;
  /** Número del papel, por ejemplo "S2-000015". */
  numeroVisible: string;
  idSucursal: number;
  sucursal: string;
  idCliente: number;
  cliente: string;
  idProforma?: number | null;
  noProforma?: string | null;
  idFactura?: number | null;
  noFactura?: number | null;
  monto: number;
  formaPago: FormaPagoRecibo;
  referencia?: string | null;
  observaciones?: string | null;
  /** yyyy-MM-dd */
  fechaPago: string;
  fechaRegistro: string;
  usuario: string;
  anulado: boolean;
  fechaAnulacion?: string | null;
  usuarioAnulacion?: string | null;
  motivoAnulacion?: string | null;
}

/** Lo que se manda para emitir un recibo. */
export class CrearReciboCajaDto {
  idCliente: number;
  idProforma?: number | null;
  idFactura?: number | null;
  monto: number;
  formaPago: FormaPagoRecibo;
  referencia?: string | null;
  observaciones?: string | null;
  fechaPago?: string | null;
  /** Solo la toma en cuenta el servidor si quien emite es administrador. */
  idSucursal?: number | null;
}
