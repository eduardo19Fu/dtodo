/** Un vendedor en las ventas del mes: lo entregado menos lo devuelto y sus pedidos entregados a tiempo. */
export class VentaVendedorDto {
  idVendedor: number;
  vendedor: string;
  entregado: number;
  devuelto: number;
  neto: number;
  /** Por ciento de las ventas netas del mes. */
  participacion: number;
  pedidosConFecha: number;
  pedidosATiempo: number;
  /** Nulo si no tuvo pedidos con fecha estimada. */
  porcentajeATiempo?: number | null;
}

/** Los indicadores de un mes (las cuentas por cobrar son las de hoy). */
export class IndicadoresMesDto {
  anio: number;
  mes: number;
  desde: string;
  hasta: string;
  vendedores: VentaVendedorDto[] = [];
  totalEntregado: number;
  totalDevuelto: number;
  totalNeto: number;
  netoMesAnterior: number;
  pedidosConFecha: number;
  pedidosATiempo: number;
  porcentajeATiempo?: number | null;
  cuentasPorCobrar: {
    total: number;
    porVencer: number;
    vencido: number;
    clientesConDeuda: number;
    clientesConVencido: number;
  };
}
