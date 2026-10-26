import { fakeAsync, tick } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { DetalleFactura } from '../../../models/detalle-factura';
import { Cliente } from '../../../models/cliente';
import { DetalleProforma } from '../../../models/detalle-proforma';
import { Producto } from '../../../models/producto';
import { Proforma } from '../../../models/proforma';
import { CreateFacturaComponent } from './create-factura.component';
import swal from 'sweetalert2';

describe('CreateFacturaComponent - edición de detalle', () => {
  let component: CreateFacturaComponent;
  let item: DetalleFactura;

  beforeEach(() => {
    component = new CreateFacturaComponent(null, null, null, null, null, null, null, null, null);
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

  it('registra la proforma de origen al preparar una factura', () => {
    const proforma = new Proforma();
    proforma.idProforma = 42;
    proforma.cliente = new Cliente();
    proforma.itemsProforma = [];
    const proformaService: any = { getProforma: () => of(proforma) };
    component = new CreateFacturaComponent(null, proformaService, null, null, null, null, null, null, null);

    component.buscarProformaPorId(42);

    expect(component.factura.idProformaOrigen).toBe(42);
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

  it('muestra todos los renglones y no envía la venta si se elige revisar', async () => {
    const segundo = new DetalleFactura();
    segundo.producto = new Producto();
    segundo.producto.idProducto = 2;
    segundo.producto.codProducto = 'SIL-2';
    segundo.producto.nombre = 'Silicón';
    segundo.producto.precioVenta = 20;
    segundo.cantidad = 1;
    segundo.descuento = 0;
    component.factura.itemsFactura.push(segundo);
    component.calcularCambio();
    component.efectivo = 300;
    const alerta = spyOn(swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: false } as any));

    await component.createFactura();

    const opciones = alerta.calls.mostRecent().args[0] as any;
    expect(opciones.html).toContain('2 renglones');
    expect(opciones.html).toContain('Silicón');
    expect(component.isSaving).toBeFalse();
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

  it('identifica los productos que no tienen stock suficiente en la sucursal', () => {
    item.cantidad = 21;

    expect(component.cantidadesValidas()).toBeFalse();
    expect(component.productosSinStock()).toEqual([item]);
  });

  it('considera la cantidad acumulada cuando el producto aparece más de una vez', () => {
    const segundoItem = new DetalleFactura();
    segundoItem.producto = item.producto;
    segundoItem.cantidad = 11;
    item.cantidad = 10;
    component.factura.itemsFactura = [item, segundoItem];

    expect(component.stockInsuficiente(item)).toBeTrue();
    expect(component.stockInsuficiente(segundoItem)).toBeTrue();
    expect(component.cantidadesValidas()).toBeFalse();
  });

  it('muestra una alerta con los códigos que no tienen stock suficiente', () => {
    item.producto.codProducto = 'COD-123';
    item.cantidad = 21;
    spyOn(swal, 'fire');

    (component as any).mostrarAlertaStockInsuficiente();

    const configuracion = (swal.fire as jasmine.Spy).calls.mostRecent().args[0];
    expect(configuracion.title).toBe('Revisa las existencias');
    expect(configuracion.html).toContain('COD-123');
    expect(configuracion.html).toContain('Solicitadas</small><strong>21');
    expect(configuracion.html).toContain('Disponibles</small><strong>20');
    expect(configuracion.customClass.popup).toBe('stock-alert-popup');
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

  it('abre el directorio de clientes al cambiar de cliente', () => {
    component.nitIngresado = '12345';

    component.cambiarCliente();

    expect(component.modalClienteVisible).toBeTrue();
    expect(component.nitIngresado).toBeNull();
  });

  it('avisa cuando la factura va a un cliente distinto del de la proforma', () => {
    const proforma = new Proforma();
    proforma.idProforma = 42;
    const clienteProforma = new Cliente();
    clienteProforma.idCliente = 1;
    clienteProforma.nombre = 'Cliente de la proforma';
    proforma.cliente = clienteProforma;
    proforma.itemsProforma = [];
    const proformaService: any = { getProforma: () => of(proforma) };
    component = new CreateFacturaComponent(null, proformaService, null, null, null, null, null, null, null);

    component.buscarProformaPorId(42);
    expect(component.clienteDifiereDeLaProforma).toBeFalse();

    const otro = new Cliente();
    otro.idCliente = 2;
    component.cargarCliente(otro);

    expect(component.clienteDifiereDeLaProforma).toBeTrue();
  });

  it('muestra en línea una regla de negocio (422) sin recargar la pantalla', async () => {
    const facturaService: any = {
      createV2: () => throwError({ status: 422, error: { message: 'El NIT "55551234" no es válido' } })
    };
    (component as any).facturaService = facturaService;
    component.efectivo = 180;
    component.calcularCambio();
    const alerta = spyOn(swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: true } as any));

    await component.createFactura();

    const ultima = alerta.calls.mostRecent().args;
    expect(ultima[0]).toBe('No se pudo facturar');
    expect(ultima[1]).toContain('55551234');
    expect(component.isSaving).toBeFalse();
  });

  describe('búsqueda de cliente', () => {
    let clienteService: any;
    let clienteCreateService: any;

    beforeEach(() => {
      clienteService = {
        getCliente: jasmine.createSpy('getCliente'),
        getClienteByNit: jasmine.createSpy('getClienteByNit'),
        getClienteByCodigo: jasmine.createSpy('getClienteByCodigo')
      };
      clienteCreateService = { abrirModal: jasmine.createSpy('abrirModal') };
      component = new CreateFacturaComponent(null, null, null, clienteService, null, clienteCreateService, null, null, null);
    });

    it('elige del directorio al cliente completo por su id aunque no tenga NIT', () => {
      const completo = new Cliente();
      completo.idCliente = 3;
      completo.nombre = 'Cliente sin NIT';
      completo.nit = null;
      clienteService.getCliente.and.returnValue(of(completo));
      const fila = new Cliente();
      fila.idCliente = 3;
      component.abrirModalCliente();

      component.loadCliente(fila);

      expect(clienteService.getCliente).toHaveBeenCalledWith(3);
      expect(clienteService.getClienteByNit).not.toHaveBeenCalled();
      expect(component.cliente).toBe(completo);
      expect(component.modalClienteVisible).toBeFalse();
    });

    it('si el valor no es un NIT registrado lo busca como código de cliente', () => {
      const porCodigo = new Cliente();
      porCodigo.idCliente = 8;
      porCodigo.codigoCliente = 'C-0008';
      clienteService.getClienteByNit.and.returnValue(throwError({ status: 404 }));
      clienteService.getClienteByCodigo.and.returnValue(of(porCodigo));
      component.nitBusqueda = ' C-0008 ';

      component.buscarCliente();

      expect(clienteService.getClienteByNit).toHaveBeenCalledWith('C-0008');
      expect(clienteService.getClienteByCodigo).toHaveBeenCalledWith('C-0008');
      expect(component.cliente).toBe(porCodigo);
      expect(clienteCreateService.abrirModal).not.toHaveBeenCalled();
    });

    it('ofrece registrar al cliente solo si ni el NIT ni el código existen', () => {
      clienteService.getClienteByNit.and.returnValue(throwError({ status: 404 }));
      clienteService.getClienteByCodigo.and.returnValue(throwError({ status: 404 }));
      component.nitBusqueda = '999';

      component.buscarCliente();

      expect(clienteCreateService.abrirModal).toHaveBeenCalled();
      expect(component.nitIngresado).toBe('999');
    });
  });
});
