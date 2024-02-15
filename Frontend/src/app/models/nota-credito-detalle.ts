import { Producto } from "./producto";

export class NotaCreditoDetalle {
    idNotaDetalle: number;
    subTotal: number;
    cantidad: number;
    descuento: number;
    subTotalDescuento: number;

    producto: Producto;

    public calcularImporte(): number{
        return this.producto.precioVenta * this.cantidad;

        // return this.producto.precioVenta * this.cantidad;
    }

    public calcularPrecioDescuento(): number{
        return this.descuento <= 0 ? this.producto.precioVenta 
        : (Math.round(((this.producto.precioVenta - (this.producto.precioVenta * (this.descuento / 100))) + Number.EPSILON) * 100) / 100);
    }

    public calcularImporteDescuento(): number{
        return this.descuento <= 0 ? this.producto.precioVenta * this.cantidad
        // : (this.producto.precioVenta * this.cantidad) - ((this.descuento / 100) * (this.producto.precioVenta * this.cantidad));
        // : (Number((Math.abs((this.producto.precioVenta - (this.producto.precioVenta * (this.descuento / 100)))).toPrecision(15))) * this.cantidad);
        : (Math.round(((this.producto.precioVenta - (this.producto.precioVenta * (this.descuento / 100))) + Number.EPSILON) * 100) / 100) * this.cantidad;
    }
}
