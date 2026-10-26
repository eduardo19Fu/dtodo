/** Una proforma en la bandeja de autorizaciones: por descuento sobre el tope o vencida. */
export class AutorizacionProformaDto {
  idProforma: number;
  noProforma: string;
  fechaEmision: Date;
  total: number;
  cliente: string;
  ruta?: string | null;
  vendedor: string;
  /** yyyy-MM-dd */
  fechaVencimiento?: string | null;
  /** Mayor porcentaje de descuento de la proforma. */
  descuentoMaximo?: number | null;
  tipo: 'DESCUENTO' | 'VENCIDA';
}

export type AccionAuditoria = 'CREACION' | 'MODIFICACION' | 'ANULACION' | 'AUTORIZACION' | 'RECHAZO'
  | 'AUTORIZACION_VENCIDA' | 'CAMBIO_CLIENTE_FACTURA';

/** Un renglón del historial de una proforma. */
export class ProformaAuditoriaDto {
  idAuditoria: number;
  accion: AccionAuditoria;
  usuario: string;
  motivo?: string | null;
  detalle?: string | null;
  /** ISO local, sin zona. */
  fecha: string;
}

export const ETIQUETAS_ACCION_AUDITORIA: { [accion: string]: string } = {
  CREACION: 'Creación',
  MODIFICACION: 'Modificación',
  ANULACION: 'Anulación',
  AUTORIZACION: 'Descuento aprobado',
  RECHAZO: 'Descuento rechazado',
  AUTORIZACION_VENCIDA: 'Vencimiento autorizado',
  CAMBIO_CLIENTE_FACTURA: 'Cambio de cliente al facturar'
};
