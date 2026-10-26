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
  codigo: string;
  producto: string;
  cantidad: number;
  observacion?: string | null;
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
  observaciones?: string | null;
  lineas: PedidoLineaDto[] = [];
  etapas: PedidoEtapaDto[] = [];
}
