import { Producto } from './producto';

export class DetalleDevolucionBodega {
    idDetalle: number;
    cantidad = 1;
    precioUnitario: number;
    subTotal: number;
    existenciaSucursal: number;

    producto: Producto;

    public calcularSubTotal(): number {
        return (this.precioUnitario || 0) * (this.cantidad || 0);
    }
}
