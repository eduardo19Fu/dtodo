import { DetalleDespachoBodega } from './detalle-despacho-bodega';

describe('DetalleDespachoBodega', () => {
  it('el subtotal es cantidad por costo unitario', () => {
    const detalle = new DetalleDespachoBodega();
    detalle.cantidad = 4;
    detalle.precioUnitario = 12.5;

    expect(detalle.calcularSubTotal()).toBe(50);
  });

  it('inicia con una unidad y subtotal cero si no hay precio', () => {
    const detalle = new DetalleDespachoBodega();

    expect(detalle.cantidad).toBe(1);
    expect(detalle.calcularSubTotal()).toBe(0);
  });

  it('trata cantidad o precio ausentes como cero', () => {
    const detalle = new DetalleDespachoBodega();
    detalle.cantidad = null;
    detalle.precioUnitario = 10;

    expect(detalle.calcularSubTotal()).toBe(0);
  });
});
