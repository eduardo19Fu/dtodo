import { fakeAsync, tick } from '@angular/core/testing';

import { DetalleFactura } from '../../../models/detalle-factura';
import { DetalleProforma } from '../../../models/detalle-proforma';
import { Producto } from '../../../models/producto';
import { Proforma } from '../../../models/proforma';
import { CreateFacturaComponent } from './create-factura.component';
import swal from 'sweetalert2';

describe('CreateFacturaComponent - edición de detalle', () => {
  let component: CreateFacturaComponent;
  let item: DetalleFactura;

  beforeEach(() => {
    component = new CreateFacturaComponent(null, null, null, null, null, null, null, null, null, null);
    const producto = new Producto();
    producto.idProducto = 1;
    producto.nombre = 'Producto de prueba';
    producto.precioVenta = 100;
    producto.stock = 20;

    item = new DetalleFactura();
    item.producto = producto;
    item.cantidad = 2;
    item.descuento = 10;
    item.subTotal = item.calcularImporte();
    item.subTotalDescuento = item.calcularImporteDescuento();
    component.factura.itemsFactura = [item];
    component.calcularCambio();
  });

  it('controla la visibilidad de los selectores con estado Angular', () => {
    component.abrirModalCliente();
    component.abrirModalProducto();

    expect(component.modalClienteVisible).toBeTrue();
    expect(component.modalProductoVisible).toBeTrue();

    component.cerrarModalCliente();
    component.cerrarModalProducto();

    expect(component.modalClienteVisible).toBeFalse();
    expect(component.modalProductoVisible).toBeFalse();
  });

  it('muestra el total completo como pendiente antes de ingresar el efectivo', () => {
    expect(component.factura.total).toBe(180);
    expect(component.pagoSuficiente()).toBeFalse();
    expect(component.saldoPendiente()).toBe(180);
  });

  it('habilita el pago y deja de mostrar saldo pendiente al recibir el total', () => {
    component.efectivo = 180;
    component.calcularCambio();

    expect(component.pagoSuficiente()).toBeTrue();
    expect(component.saldoPendiente()).toBe(0);
    expect(component.cambio).toBe(0);
  });

  it('actualiza cantidad, total y cambio al confirmar', fakeAsync(() => {
    const origen = document.createElement('button');
    spyOn(origen, 'focus');
    component.efectivo = 500;
    component.abrirEdicionDetalle(0, 'cantidad', origen);
    expect(component.valorDetalleNuevo).toBeNull();
    component.valorDetalleNuevo = 3;

    component.confirmarEdicionDetalle();
    tick();

    expect(item.cantidad).toBe(3);
    expect(item.subTotal).toBe(300);
    expect(item.subTotalDescuento).toBe(270);
    expect(component.factura.total).toBe(270);
    expect(component.cambio).toBe(230);
    expect(component.edicionDetalleAbierta).toBeFalse();
    expect(origen.focus).toHaveBeenCalled();
  }));

  it('rechaza una cantidad mayor al stock disponible', () => {
    component.abrirEdicionDetalle(0, 'cantidad', document.createElement('button'));
    component.valorDetalleNuevo = 21;

    component.confirmarEdicionDetalle();

    expect(item.cantidad).toBe(2);
    expect(component.edicionDetalleAbierta).toBeTrue();
    expect(component.errorEdicionDetalle).toContain('stock disponible');
  });

  it('conserva la cantidad y sus cálculos cuando el nuevo valor está vacío', () => {
    component.abrirEdicionDetalle(0, 'cantidad', document.createElement('button'));

    component.confirmarEdicionDetalle();

    expect(item.cantidad).toBe(2);
    expect(item.subTotal).toBe(200);
    expect(item.subTotalDescuento).toBe(180);
    expect(component.factura.total).toBe(180);
    expect(component.edicionDetalleAbierta).toBeTrue();
    expect(component.errorEdicionDetalle).toBe('Ingresa un valor nuevo.');
  });

  it('actualiza el descuento y sincroniza la proforma de origen', fakeAsync(() => {
    const itemProforma = new DetalleProforma();
    itemProforma.producto = item.producto;
    itemProforma.cantidad = item.cantidad;
    itemProforma.descuento = item.descuento;
    component.proforma = new Proforma();
    component.proforma.itemsProforma = [itemProforma];

    component.abrirEdicionDetalle(0, 'descuento', document.createElement('button'));
    component.valorDetalleNuevo = 25;
    component.confirmarEdicionDetalle();
    tick();

    expect(item.descuento).toBe(25);
    expect(item.subTotalDescuento).toBe(150);
    expect(component.factura.total).toBe(150);
    expect(itemProforma.descuento).toBe(25);
    expect(itemProforma.subTotalDescuento).toBe(150);
    expect(component.proforma.total).toBe(150);
  }));

  it('conserva el descuento y sus cálculos cuando el nuevo valor está vacío', () => {
    component.abrirEdicionDetalle(0, 'descuento', document.createElement('button'));
    (component.valorDetalleNuevo as any) = '';

    component.confirmarEdicionDetalle();

    expect(item.descuento).toBe(10);
    expect(item.subTotal).toBe(200);
    expect(item.subTotalDescuento).toBe(180);
    expect(component.factura.total).toBe(180);
    expect(component.edicionDetalleAbierta).toBeTrue();
    expect(component.errorEdicionDetalle).toBe('Ingresa un valor nuevo.');
  });

  it('conserva el valor anterior al cancelar', fakeAsync(() => {
    const origen = document.createElement('button');
    spyOn(origen, 'focus');
    component.abrirEdicionDetalle(0, 'descuento', origen);
    component.valorDetalleNuevo = 50;

    component.cancelarEdicionDetalle();
    tick();

    expect(item.descuento).toBe(10);
    expect(component.factura.total).toBe(180);
    expect(component.edicionDetalleAbierta).toBeFalse();
    expect(origen.focus).toHaveBeenCalled();
  }));

  it('elimina la línea y recalcula el total únicamente al confirmar', fakeAsync(() => {
    spyOn(swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: true }) as ReturnType<typeof swal.fire>);

    component.eliminarItem(0);
    expect(component.factura.itemsFactura.length).toBe(1);
    tick();

    expect(component.factura.itemsFactura.length).toBe(0);
    expect(component.factura.total).toBe(0);
  }));

  it('conserva la línea y el total al cancelar la eliminación', fakeAsync(() => {
    spyOn(swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: false }) as ReturnType<typeof swal.fire>);

    component.eliminarItem(0);
    tick();

    expect(component.factura.itemsFactura.length).toBe(1);
    expect(component.factura.total).toBe(180);
  }));
});
