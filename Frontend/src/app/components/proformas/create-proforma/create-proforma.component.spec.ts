import { fakeAsync, tick } from '@angular/core/testing';

import { of } from 'rxjs';

import { Cliente } from '../../../models/cliente';
import { DetalleProforma } from '../../../models/detalle-proforma';
import { Producto } from '../../../models/producto';
import { Proforma } from '../../../models/proforma';
import { ProductoService } from '../../../services/producto.service';
import { ProformaService } from '../../../services/proformas/proforma.service';
import { CreateProformaComponent } from './create-proforma.component';
import swal from 'sweetalert2';

describe('CreateProformaComponent - edición de detalle', () => {
  let component: CreateProformaComponent;
  let item: DetalleProforma;

  beforeEach(() => {
    component = new CreateProformaComponent(null, null, null, null, null, null, null, null);
    const producto = new Producto();
    producto.nombre = 'Producto de prueba';
    producto.precioVenta = 100;
    producto.stock = 20;

    item = new DetalleProforma();
    item.producto = producto;
    item.cantidad = 2;
    item.descuento = 10;
    item.subTotal = item.calcularImporte();
    item.subTotalDescuento = item.calcularImporteDescuento();
    item.nuevoPrecioVenta = item.calcularNuevoPrecioVenta();
    component.proforma.itemsProforma = [item];
    component.recalcularTotal();
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

  it('actualiza cantidad e importes únicamente al confirmar', fakeAsync(() => {
    const origen = document.createElement('button');
    spyOn(origen, 'focus');

    component.abrirEdicionDetalle(0, 'cantidad', origen);
    expect(component.valorDetalleNuevo).toBeNull();
    component.valorDetalleNuevo = 3;
    component.confirmarEdicionDetalle();
    tick();

    expect(item.cantidad).toBe(3);
    expect(item.subTotal).toBe(300);
    expect(item.subTotalDescuento).toBe(270);
    expect(component.proforma.total).toBe(270);
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
    expect(component.proforma.total).toBe(180);
    expect(component.edicionDetalleAbierta).toBeTrue();
    expect(component.errorEdicionDetalle).toBe('Ingresa un valor nuevo.');
  });

  it('actualiza el descuento y recalcula el total', fakeAsync(() => {
    component.abrirEdicionDetalle(0, 'descuento', document.createElement('button'));
    component.valorDetalleNuevo = 25;

    component.confirmarEdicionDetalle();
    tick();

    expect(item.descuento).toBe(25);
    expect(item.nuevoPrecioVenta).toBe(75);
    expect(item.subTotalDescuento).toBe(150);
    expect(component.proforma.total).toBe(150);
  }));

  it('conserva el descuento y sus cálculos cuando el nuevo valor está vacío', () => {
    component.abrirEdicionDetalle(0, 'descuento', document.createElement('button'));
    (component.valorDetalleNuevo as any) = '';

    component.confirmarEdicionDetalle();

    expect(item.descuento).toBe(10);
    expect(item.nuevoPrecioVenta).toBe(90);
    expect(item.subTotalDescuento).toBe(180);
    expect(component.proforma.total).toBe(180);
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
    expect(component.proforma.total).toBe(180);
    expect(component.edicionDetalleAbierta).toBeFalse();
    expect(origen.focus).toHaveBeenCalled();
  }));

  it('elimina la línea y recalcula el total únicamente al confirmar', fakeAsync(() => {
    spyOn(swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: true }) as ReturnType<typeof swal.fire>);

    component.eliminarItem(0);
    expect(component.proforma.itemsProforma.length).toBe(1);
    tick();

    expect(component.proforma.itemsProforma.length).toBe(0);
    expect(component.proforma.total).toBe(0);
  }));

  it('conserva la línea y el total al cancelar la eliminación', fakeAsync(() => {
    spyOn(swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: false }) as ReturnType<typeof swal.fire>);

    component.eliminarItem(0);
    tick();

    expect(component.proforma.itemsProforma.length).toBe(1);
    expect(component.proforma.total).toBe(180);
  }));
});

describe('CreateProformaComponent - stock de una proforma guardada', () => {
  let component: CreateProformaComponent;
  let productoService: jasmine.SpyObj<ProductoService>;
  let proformaService: jasmine.SpyObj<ProformaService>;

  const nuevaLinea = (codigo: string, stockLegado: number, cantidad: number): DetalleProforma => {
    const producto = new Producto();
    producto.idProducto = 1;
    producto.codProducto = codigo;
    producto.nombre = 'Producto ' + codigo;
    producto.precioVenta = 10;
    producto.stock = stockLegado;

    const linea = new DetalleProforma();
    linea.producto = producto;
    linea.cantidad = cantidad;
    linea.descuento = 0;
    linea.subTotal = linea.calcularImporte();
    linea.subTotalDescuento = linea.calcularImporteDescuento();
    linea.nuevoPrecioVenta = linea.calcularNuevoPrecioVenta();
    return linea;
  };

  const productoConStock = (codigo: string, stock: number): Producto => {
    const producto = new Producto();
    producto.codProducto = codigo;
    producto.stock = stock;
    return producto;
  };

  beforeEach(() => {
    productoService = jasmine.createSpyObj<ProductoService>('ProductoService', ['getProductoByCode']);
    proformaService = jasmine.createSpyObj<ProformaService>('ProformaService', ['getProforma']);
    component = new CreateProformaComponent(proformaService, productoService, null, null, null, null, null, null);
  });

  it('permite editar por encima del stock legado cuando las existencias reales alcanzan', () => {
    component.proforma.itemsProforma = [nuevaLinea('712L', 23, 3)];
    productoService.getProductoByCode.and.returnValue(of(productoConStock('712L', 933)));

    component.actualizarStockDeLineas();
    component.abrirEdicionDetalle(0, 'cantidad', document.createElement('button'));
    component.valorDetalleNuevo = 66;
    component.confirmarEdicionDetalle();

    expect(productoService.getProductoByCode).toHaveBeenCalledWith('712L');
    expect(component.proforma.itemsProforma[0].cantidad).toBe(66);
    expect(component.errorEdicionDetalle).toBe('');
  });

  it('rechaza una cantidad mayor a las existencias reales aunque el stock legado sea más alto', () => {
    component.proforma.itemsProforma = [nuevaLinea('7891040321230', 23, 1)];
    productoService.getProductoByCode.and.returnValue(of(productoConStock('7891040321230', 6)));

    component.actualizarStockDeLineas();
    component.abrirEdicionDetalle(0, 'cantidad', document.createElement('button'));
    component.valorDetalleNuevo = 10;
    component.confirmarEdicionDetalle();

    expect(component.proforma.itemsProforma[0].cantidad).toBe(1);
    expect(component.errorEdicionDetalle).toContain('(6)');
  });

  it('actualiza el stock de cada línea al cargar una proforma guardada', () => {
    const guardada = new Proforma();
    guardada.idProforma = 7;
    guardada.cliente = new Cliente();
    guardada.itemsProforma = [nuevaLinea('A', 32, 2), nuevaLinea('B', 23, 1)];
    proformaService.getProforma.and.returnValue(of(guardada));
    productoService.getProductoByCode.and.callFake((codigo: string) =>
      of(productoConStock(codigo, codigo === 'A' ? 193 : 5)));

    component.buscarProformaPorId(7);

    expect(component.proforma.itemsProforma.map(i => i.producto.stock)).toEqual([193, 5]);
  });

  it('no consulta productos cuando la proforma no tiene líneas', () => {
    component.proforma.itemsProforma = [];

    component.actualizarStockDeLineas();

    expect(productoService.getProductoByCode).not.toHaveBeenCalled();
  });
});
