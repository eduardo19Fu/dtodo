/** Una meta de comisión: desde este monto entregado en el año se paga el porcentaje sobre todo lo entregado. */
export class MetaComisionDto {
  montoMinimo: number;
  porcentaje: number;
}

/** Un vendedor en el avance o la liquidación de comisiones de un año. */
export class FilaComisionDto {
  idVendedor: number;
  vendedor: string;
  /** Cuándo se creó su usuario. */
  fechaAlta?: string | null;
  activo: boolean;
  montoEntregado: number;
  metaAlcanzada?: number | null;
  porcentaje: number;
  comision: number;
  siguienteMeta?: number | null;
  faltaParaSiguiente?: number | null;
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
  totalComision: number;
}

export class DecisionComisionDto {
  idVendedor: number;
  completoAnio: boolean;
}
