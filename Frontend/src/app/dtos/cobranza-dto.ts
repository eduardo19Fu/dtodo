/** Un cliente con deuda en la cobranza, repartida según su antigüedad. */
export class CobranzaClienteDto {
  idCliente: number;
  cliente: string;
  negocio?: string | null;
  idRuta?: number | null;
  ruta?: string | null;
  limiteCredito?: number | null;
  porVencer: number;
  vencido1a30: number;
  vencido31a60: number;
  vencidoMasDe60: number;
  total: number;
  vencido: number;
  /** Días de atraso del pedido más vencido; 0 si no tiene nada vencido. */
  diasMaxAtraso: number;
}

/** Cuentas por cobrar de los clientes mayoristas al día de hoy. */
export class CobranzaDto {
  /** yyyy-MM-dd */
  fecha: string;
  clientes: CobranzaClienteDto[] = [];
  porVencer: number;
  vencido1a30: number;
  vencido31a60: number;
  vencidoMasDe60: number;
  total: number;
  vencido: number;
  clientesConVencido: number;
}
