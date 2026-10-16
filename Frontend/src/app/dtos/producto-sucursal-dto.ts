/** Producto con la existencia que tiene una sucursal; no incluye costos. */
export class ProductoSucursalDto {
  idInventarioSucursal: number;
  idProducto: number;
  codProducto: string;
  nombreProducto: string;
  stock: number;
  stockMinimo: number;
  fechaActualizacion: Date;
}
