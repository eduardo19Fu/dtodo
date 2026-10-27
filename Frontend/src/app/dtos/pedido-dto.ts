export type EstadoPedido = 'CONFIRMADA' | 'EN_PREPARACION' | 'EMPACADA' | 'EN_RUTA' | 'ENTREGADA_PARCIAL' | 'ENTREGADA';

export const ETIQUETAS_ESTADO_PEDIDO: { [estado: string]: string } = {
  CONFIRMADA: 'Confirmado',
  EN_PREPARACION: 'En preparación',
  EMPACADA: 'Empacado',
  EN_RUTA: 'En ruta',
  ENTREGADA_PARCIAL: 'Entrega parcial',
  ENTREGADA: 'Entregado'
};

/** Una fila de la lista de pedidos. Sin importes: la lista es operativa. */
export class PedidoDto {
  idProforma: number;
  noProforma: string;
  fechaEmision: Date;
  /** yyyy-MM-dd */
  fechaEntregaEstimada?: string | null;
  cliente: string;
  negocio?: string | null;
  idRuta?: number | null;
  ruta?: string | null;
  vendedor: string;
  estadoPedido: EstadoPedido;
  unidades: number;
  estadoAutorizacion?: 'PENDIENTE' | 'APROBADA' | 'RECHAZADA' | null;
  /** Piloto asignado al cargar el pedido a ruta. */
  idRepartidor?: number | null;
  repartidor?: string | null;
}

export class PedidoEtapaDto {
  idEtapa: number;
  etapa: EstadoPedido;
  usuario: string;
  responsable?: string | null;
  motivo?: string | null;
  /** ISO local, sin zona. */
  fecha: string;
}

export class PedidoLineaDto {
  idDetalle: number;
  codigo: string;
  producto: string;
  cantidad: number;
  observacion?: string | null;
  cantidadEntregada: number;
  /** Lo que falta por entregar. */
  pendiente: number;
}

export class EntregaLineaDto {
  idDetalle: number;
  producto: string;
  cantidad: number;
}

/** Una entrega del pedido; la anulada se conserva. */
export class EntregaDto {
  idEntrega: number;
  /** yyyy-MM-dd: la fecha real de la entrega. */
  fechaEntrega: string;
  /** ISO local, sin zona: cuándo se registró. */
  fechaRegistro: string;
  usuario: string;
  entregadoPor?: string | null;
  idSucursal: number;
  sucursal: string;
  descontadoAMano: boolean;
  observaciones?: string | null;
  anulada: boolean;
  fechaAnulacion?: string | null;
  motivoAnulacion?: string | null;
  lineas: EntregaLineaDto[] = [];
}

/** Lo que se envía para registrar una entrega. */
export class EntregaSolicitudDto {
  idSucursal: number;
  fechaEntrega?: string | null;
  entregadoPor?: string | null;
  descontadoAMano = false;
  observaciones?: string | null;
  lineas: Array<{ idDetalle: number; cantidad: number }> = [];
}

export class PedidoDetalleDto {
  idProforma: number;
  noProforma: string;
  fechaEmision: Date;
  fechaEntregaEstimada?: string | null;
  cliente: string;
  negocio?: string | null;
  direccion?: string | null;
  contacto?: string | null;
  telefonoContacto?: string | null;
  ruta?: string | null;
  vendedor: string;
  estadoPedido: EstadoPedido;
  estadoAutorizacion?: 'PENDIENTE' | 'APROBADA' | 'RECHAZADA' | null;
  idRepartidor?: number | null;
  repartidor?: string | null;
  observaciones?: string | null;
  /** Sucursal de la proforma: la que se propone para descontar el inventario. */
  idSucursal?: number | null;
  sucursal?: string | null;
  lineas: PedidoLineaDto[] = [];
  etapas: PedidoEtapaDto[] = [];
  entregas: EntregaDto[] = [];
}
