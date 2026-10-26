import { TipoDescuento } from '../dtos/descuento-dto';
import { Producto } from './producto';

export class DetalleProforma {

    idDetalle: number;
    cantidad = 1;
    subTotal: number;
    descuento: number;
    subTotalDescuento: number;
    nuevoPrecioVenta: number;

    /** Precio unitario al registrar la línea; lo completa el servidor (nulo en líneas anteriores). */
    precioUnitario?: number | null;
    /** De dónde sale el descuento: lo elige el usuario y el servidor verifica que coincida con el porcentaje. */
    tipoDescuento?: TipoDescuento | null;
    /** Porcentaje que sugería el sistema; lo completa el servidor. */
    porcentajeSugerido?: number | null;

    producto: Producto;

    public calcularImporte(): number{
        return this.producto.precioVenta * this.cantidad;
    }

    /** Metodo que calcula el nuevo precio venta con o sin descuento aplicado
     * @returns Valor del precioVenta por item
     */
    public calcularNuevoPrecioVenta(): number {
        if (this.descuento <= 0) {
            return parseFloat(this.producto.precioVenta.toFixed(2));
        } else {
            const precioConDescuento = this.producto.precioVenta - (this.descuento / 100) * this.producto.precioVenta;
            return parseFloat(precioConDescuento.toFixed(2));
        }
    }

    /**
     * Método que calcula el subTotal del item en caso de poseer o no descuento.
     * @returns subTotal con descuento aplicado
     */
    public calcularImporteDescuento(): number {
        const importeSinDescuento = this.producto.precioVenta * this.cantidad;
        if (this.descuento <= 0) {
            return parseFloat(importeSinDescuento.toFixed(2));
        } else {
            const importeConDescuento = this.calcularNuevoPrecioVenta() * this.cantidad;
            return parseFloat(importeConDescuento.toFixed(2));
        }
    }
}
