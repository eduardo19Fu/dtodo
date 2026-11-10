export type FormaPagoRecibo = 'EFECTIVO' | 'DEPOSITO' | 'TRANSFERENCIA' | 'CHEQUE';

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
  /** Solo en recibos pagados con cheque. */
  cheque?: ChequeDto | null;
}

export type EstadoCheque = 'RECIBIDO' | 'DEPOSITADO' | 'COBRADO' | 'RECHAZADO';

/** Un cheque en el control de cheques y en el detalle de su recibo. */
export class ChequeDto {
  idCheque: number;
  idRecibo: number;
  numeroRecibo: string;
  idSucursal: number;
  sucursal: string;
  idCliente: number;
  cliente: string;
  monto: number;
  /** yyyy-MM-dd */
  fechaPago: string;
  banco: string;
  numeroCheque: string;
  girador?: string | null;
  /** Fecha escrita en el cheque, yyyy-MM-dd. */
  fechaCheque: string;
  /** La fecha del cheque aún no llega: todavía no se puede depositar. */
  postfechado: boolean;
  notas?: string | null;
  estado: EstadoCheque;
  fechaEstado: string;
  usuarioEstado?: string | null;
  motivoRechazo?: string | null;
}

/** Los datos del cheque que se mandan al emitir un recibo con forma de pago CHEQUE. */
export class DatosChequeDto {
  banco: string;
  numeroCheque: string;
  girador?: string | null;
  fechaCheque: string;
  notas?: string | null;
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
  /** Obligatorio si la forma de pago es CHEQUE. */
  cheque?: DatosChequeDto | null;
}
