export interface LineaDespachoBodegaRequest {
  idProducto: number;
  cantidad: number;
}

/** Datos que se envían al registrar un despacho; el precio, la existencia y el estado los define el servidor. */
export class DespachoBodegaRequest {
  idBodega: number;
  idSucursalDestino: number;
  recibidoPor: string;
  observaciones: string;
  items: LineaDespachoBodegaRequest[] = [];
}
