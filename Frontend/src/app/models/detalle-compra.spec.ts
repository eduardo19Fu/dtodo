import { DetalleCompra } from './detalle-compra';
import { Producto } from './producto';

describe('DetalleCompra - calcularSubTotal', () => {
  it('multiplica cantidad por precio unitario', () => {
    const detalle = new DetalleCompra();
    detalle.producto = new Producto();
    detalle.cantidad = 4;
    detalle.precioUnitario = 12.5;

    expect(detalle.calcularSubTotal()).toBe(50);
  });

  it('devuelve 0 si falta el precio unitario', () => {
    const detalle = new DetalleCompra();
    detalle.producto = new Producto();
    detalle.cantidad = 4;
    detalle.precioUnitario = null;

    expect(detalle.calcularSubTotal()).toBe(0);
  });
});
