import { CreateCompraComponent } from './create-compra.component';
import { DetalleCompra } from 'src/app/models/detalle-compra';
import { Producto } from 'src/app/models/producto';

describe('CreateCompraComponent - totales', () => {
  let component: CreateCompraComponent;

  beforeEach(() => {
    component = new CreateCompraComponent(null, null, null, null, null, null, null);
  });

  function detalle(cantidad: number, precioUnitario: number): DetalleCompra {
    const item = new DetalleCompra();
    item.producto = new Producto();
    item.cantidad = cantidad;
    item.precioUnitario = precioUnitario;
    return item;
  }

  it('el subtotal del detalle es la suma de cantidad por precio unitario de cada linea', () => {
    component.compra.items = [detalle(5, 25), detalle(2, 10)];

    expect(component.subTotalDetalle).toBe(145);
  });

  it('el total de la compra suma el subtotal del detalle y el costo de envio', () => {
    component.compra.items = [detalle(5, 25)];
    component.compra.costoEnvio = 15;

    expect(component.totalCompra).toBe(140);
  });

  it('el total de la compra es solo el subtotal cuando no hay costo de envio', () => {
    component.compra.items = [detalle(1, 100)];
    component.compra.costoEnvio = 0;

    expect(component.totalCompra).toBe(100);
  });
});

describe('CreateCompraComponent - agregarLinea', () => {
  let component: CreateCompraComponent;

  beforeEach(() => {
    component = new CreateCompraComponent(null, null, null, null, null, null, null);
  });

  it('no agrega una linea si no hay producto seleccionado', () => {
    component.productoActual = null;
    component.cantidadActual = 1;
    component.precioUnitarioActual = 10;

    component.agregarLinea();

    expect(component.compra.items.length).toBe(0);
  });

  it('no agrega una linea con cantidad invalida', () => {
    component.productoActual = new Producto();
    component.cantidadActual = 0;
    component.precioUnitarioActual = 10;

    component.agregarLinea();

    expect(component.compra.items.length).toBe(0);
  });

  it('no agrega una linea con precio unitario invalido', () => {
    component.productoActual = new Producto();
    component.cantidadActual = 1;
    component.precioUnitarioActual = 0;

    component.agregarLinea();

    expect(component.compra.items.length).toBe(0);
  });

  it('agrega la linea y limpia la seleccion actual cuando los datos son validos', () => {
    const producto = new Producto();
    producto.nombre = 'Producto de prueba';
    component.productoActual = producto;
    component.cantidadActual = 3;
    component.precioUnitarioActual = 20;

    component.agregarLinea();

    expect(component.compra.items.length).toBe(1);
    expect(component.compra.items[0].producto).toBe(producto);
    expect(component.compra.items[0].subTotal).toBe(60);
    expect(component.productoActual).toBeNull();
    expect(component.cantidadActual).toBe(1);
  });

  it('elimina una linea del detalle', () => {
    const item = new DetalleCompra();
    component.compra.items = [item];

    component.eliminarLinea(item);

    expect(component.compra.items.length).toBe(0);
  });
});
