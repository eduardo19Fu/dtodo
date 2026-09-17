import { fakeAsync, tick } from '@angular/core/testing';

import { CreateCompraComponent } from './create-compra.component';
import { DetalleCompra } from 'src/app/models/detalle-compra';
import { Producto } from 'src/app/models/producto';
import swal from 'sweetalert2';

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

  it('elimina la linea unicamente al confirmar', fakeAsync(() => {
    spyOn(swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: true }) as ReturnType<typeof swal.fire>);
    const item = new DetalleCompra();
    item.producto = new Producto();
    item.producto.nombre = 'Producto de prueba';
    component.compra.items = [item];

    component.eliminarLinea(item);
    expect(component.compra.items.length).toBe(1);
    tick();

    expect(component.compra.items.length).toBe(0);
  }));

  it('conserva la linea si se cancela la eliminacion', fakeAsync(() => {
    spyOn(swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: false }) as ReturnType<typeof swal.fire>);
    const item = new DetalleCompra();
    item.producto = new Producto();
    item.producto.nombre = 'Producto de prueba';
    component.compra.items = [item];

    component.eliminarLinea(item);
    tick();

    expect(component.compra.items.length).toBe(1);
  }));
});

describe('CreateCompraComponent - edicion de detalle', () => {
  let component: CreateCompraComponent;
  let item: DetalleCompra;

  beforeEach(() => {
    component = new CreateCompraComponent(null, null, null, null, null, null, null);
    const producto = new Producto();
    producto.idProducto = 1;
    producto.nombre = 'Producto de prueba';

    item = new DetalleCompra();
    item.producto = producto;
    item.cantidad = 5;
    item.precioUnitario = 25;
    item.subTotal = item.calcularSubTotal();
    component.compra.items = [item];
  });

  it('actualiza la cantidad y el subtotal al confirmar', fakeAsync(() => {
    const origen = document.createElement('button');
    spyOn(origen, 'focus');

    component.abrirEdicionDetalle(0, 'cantidad', origen);
    expect(component.valorDetalleNuevo).toBeNull();
    component.valorDetalleNuevo = 8;

    component.confirmarEdicionDetalle();
    tick();

    expect(item.cantidad).toBe(8);
    expect(item.subTotal).toBe(200);
    expect(component.edicionDetalleAbierta).toBeFalse();
    expect(origen.focus).toHaveBeenCalled();
  }));

  it('actualiza el precio unitario y el subtotal al confirmar', fakeAsync(() => {
    component.abrirEdicionDetalle(0, 'precioUnitario', document.createElement('button'));
    component.valorDetalleNuevo = 30;

    component.confirmarEdicionDetalle();
    tick();

    expect(item.precioUnitario).toBe(30);
    expect(item.subTotal).toBe(150);
    expect(component.edicionDetalleAbierta).toBeFalse();
  }));

  it('rechaza una cantidad no entera o menor o igual a 0', () => {
    component.abrirEdicionDetalle(0, 'cantidad', document.createElement('button'));
    component.valorDetalleNuevo = 0;

    component.confirmarEdicionDetalle();

    expect(item.cantidad).toBe(5);
    expect(component.edicionDetalleAbierta).toBeTrue();
    expect(component.errorEdicionDetalle).toContain('entero mayor a 0');
  });

  it('rechaza un precio unitario menor o igual a 0', () => {
    component.abrirEdicionDetalle(0, 'precioUnitario', document.createElement('button'));
    component.valorDetalleNuevo = 0;

    component.confirmarEdicionDetalle();

    expect(item.precioUnitario).toBe(25);
    expect(component.edicionDetalleAbierta).toBeTrue();
    expect(component.errorEdicionDetalle).toBe('El precio unitario debe ser mayor a 0.');
  });

  it('conserva el valor anterior cuando el nuevo valor esta vacio', () => {
    component.abrirEdicionDetalle(0, 'cantidad', document.createElement('button'));

    component.confirmarEdicionDetalle();

    expect(item.cantidad).toBe(5);
    expect(component.edicionDetalleAbierta).toBeTrue();
    expect(component.errorEdicionDetalle).toBe('Ingresa un valor nuevo.');
  });

  it('conserva el valor anterior al cancelar', fakeAsync(() => {
    const origen = document.createElement('button');
    spyOn(origen, 'focus');
    component.abrirEdicionDetalle(0, 'cantidad', origen);
    component.valorDetalleNuevo = 99;

    component.cancelarEdicionDetalle();
    tick();

    expect(item.cantidad).toBe(5);
    expect(component.edicionDetalleAbierta).toBeFalse();
    expect(origen.focus).toHaveBeenCalled();
  }));
});
