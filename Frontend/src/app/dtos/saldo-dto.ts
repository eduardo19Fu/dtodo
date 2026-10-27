export type CondicionPago = 'CONTADO' | 'CREDITO';

/** Cómo va el cobro de un pedido mayorista. */
export class PedidoSaldoDto {
  idProforma: number;
  noProforma: string;
  condicionPago: CondicionPago;
  diasCredito?: number | null;
  total: number;
  /** Valor de lo entregado hasta hoy. */
  entregado: number;
  /** Lo cobrado con recibos ligados al pedido. */
  cobrado: number;
  /** Lo que se aplicó de los abonos sin pedido del cliente. */
  aplicadoDeAbonos: number;
  /** Lo entregado que todavía no se paga. */
  pendiente: number;
  /** yyyy-MM-dd */
  primeraEntrega?: string | null;
  /** yyyy-MM-dd: solo en pedidos a crédito ya entregados. */
  vencimiento?: string | null;
}

/** Estado de cuenta de un cliente mayorista. Un saldo negativo es dinero a favor del cliente. */
export class SaldoClienteDto {
  idCliente: number;
  cliente: string;
  limiteCredito?: number | null;
  diasCredito?: number | null;
  entregado: number;
  abonado: number;
  saldo: number;
  /** Lo que queda del límite de crédito; nulo si el cliente no tiene límite. */
  disponible?: number | null;
  pedidos: PedidoSaldoDto[] = [];
}
