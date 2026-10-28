export type DestinoDevolucion = 'REINGRESO' | 'BAJA';

/** Un pedido con lo entregado y lo que todavía se puede devolver de cada línea. */
export class PedidoDevolubleDto {
  idProforma: number;
  noProforma: string;
  estadoPedido: string;
  /** Sucursal del pedido: la que se propone para recibir lo que reingresa. */
  idSucursal?: number | null;
  sucursal?: string | null;
  /** Tiene una factura vigente: la devolución debe ligarse a una nota de crédito. */
  facturado: boolean;
  noFactura?: number | null;
  lineas: Array<{
    idDetalle: number;
    codigo?: string | null;
    producto: string;
    entregada: number;
    devuelta: number;
    devolvible: number;
    precioUnitario: number;
  }> = [];
  notasCredito: Array<{ idNotaCredito: number; descripcion: string; total: number }> = [];
}

/** Lo que se manda para registrar una devolución (el administrador la registra y queda aprobada). */
export class CrearDevolucionClienteDto {
  idProforma: number;
  /** yyyy-MM-dd; hoy si no se manda. */
  fecha?: string | null;
  motivo: string;
  idSucursal?: number | null;
  idNotaCredito?: number | null;
  lineas: Array<{ idDetalle: number; cantidad: number; destino: DestinoDevolucion }> = [];
}

/** Una devolución en la lista y el detalle. */
export class DevolucionClienteDto {
  idDevolucion: number;
  idProforma: number;
  noProforma?: string | null;
  idCliente: number;
  cliente: string;
  idSucursal?: number | null;
  sucursal?: string | null;
  idNotaCredito?: number | null;
  fecha: string;
  motivo: string;
  total: number;
  usuario: string;
  fechaRegistro: string;
  /** Una devolución anulada se conserva y deja de contar. */
  anulada: boolean;
  fechaAnulacion?: string | null;
  usuarioAnulacion?: string | null;
  motivoAnulacion?: string | null;
  lineas: Array<{ idDetalle: number; codigo?: string | null; producto: string; cantidad: number; destino: DestinoDevolucion; valor: number }> = [];
}
