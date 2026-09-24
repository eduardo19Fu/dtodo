import { Producto } from './producto';
import { Venta } from './venta';

export class DetalleVenta {
    private idDetalle: number;
    private subTotal: number;

    private producto: Producto;
    private venta: Venta;

    constructor(){}

    public getIdDetalle(): number{
        return this.idDetalle;
    }

    public setIdDetalle(idDetalle: number){
        this.idDetalle = idDetalle;
    }

    public getSubTotal(): number{
        return this.subTotal;
    }

    public setSubTotal(subTotal: number){
        this.subTotal = subTotal;
    }

    public getProducto(): Producto{
        return this.producto;
    }

    public setProducto(producto: Producto){
        this.producto = producto;
    }

    public getVenta(): Venta{
        return this.venta;
    }

    public setVenta(venta: Venta){
        this.venta = venta;
    }
}
