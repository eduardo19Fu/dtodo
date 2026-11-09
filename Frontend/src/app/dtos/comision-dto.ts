/** Una meta de comisión: desde este monto entregado en el año se paga el porcentaje sobre todo lo entregado. */
export class MetaComisionDto {
  montoMinimo: number;
  porcentaje: number;
}

/** Un cambio de las metas de un año: quién, cuándo, por qué y cómo eran las metas antes y después. */
export class HistorialMetaComisionDto {
  /** CREAR, CAMBIAR o COPIAR. */
  accion: 'CREAR' | 'CAMBIAR' | 'COPIAR';
  usuario: string;
  fecha: string;
  motivo?: string | null;
  antes: MetaComisionDto[];
  despues: MetaComisionDto[];
}

/** Un vendedor en el avance o la liquidación de comisiones de un año. */
export class FilaComisionDto {
  idVendedor: number;
  vendedor: string;
  /** Cuándo se creó su usuario. */
  fechaAlta?: string | null;
  activo: boolean;
  /** Lo entregado en el período del vendedor, sin restar devoluciones. */
  montoEntregado: number;
  /** Lo que los clientes devolvieron en ese período. */
  montoDevuelto: number;
  /** Entregado menos devuelto, nunca menos de cero: la base de la meta y de la comisión. */
  montoNeto: number;
  metaAlcanzada?: number | null;
  porcentaje: number;
  comision: number;
  siguienteMeta?: number | null;
  faltaParaSiguiente?: number | null;
  /** NORMAL, BLOQUE (ingresó el año anterior: se paga desde su ingreso) o PENDIENTE (ingresó este año: se paga el año siguiente). */
  situacion?: 'NORMAL' | 'BLOQUE' | 'PENDIENTE';
  /** yyyy-MM-dd: desde cuándo cuenta lo entregado. */
  periodoDesde?: string | null;
  /** Solo en un año liquidado. */
  completoAnio?: boolean | null;
}

/** Comisiones de un año: el avance en vivo o, si ya se liquidó, lo guardado. */
export class AvanceComisionDto {
  anio: number;
  anioTerminado: boolean;
  liquidado: boolean;
  fechaLiquidacion?: string | null;
  liquidadoPor?: string | null;
  metas: MetaComisionDto[];
  filas: FilaComisionDto[];
  totalEntregado: number;
  totalDevuelto: number;
  totalNeto: number;
  totalComision: number;
}

export class DecisionComisionDto {
  idVendedor: number;
  completoAnio: boolean;
}
