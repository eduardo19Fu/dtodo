import { Producto } from './producto';

export class DetalleDespachoBodega {
    idDetalle: number;
    cantidad = 1;
    precioUnitario: number;
    subTotal: number;
    existenciaBodega: number;

    producto: Producto;

    public calcularSubTotal(): number {
        return (this.precioUnitario || 0) * (this.cantidad || 0);
    }
}
