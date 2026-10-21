export interface LineaDevolucionBodegaRequest {
  idProducto: number;
  cantidad: number;
}

/** Datos que se envían al registrar una devolución; el costo, la existencia y el estado los define el servidor. */
export class DevolucionBodegaRequest {
  idSucursalOrigen: number;
  idBodega: number;
  entregadoPor: string;
  motivo: string;
  observaciones: string;
  items: LineaDevolucionBodegaRequest[] = [];
}
