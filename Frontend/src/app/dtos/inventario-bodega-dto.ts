export class InventarioBodegaDto {
  idInventarioBodega: number;
  idProducto: number;
  codProducto: string;
  nombreProducto: string;
  stock: number;
  stockMinimo: number;
  precioCompra: number;
  fechaActualizacion: Date;
}
