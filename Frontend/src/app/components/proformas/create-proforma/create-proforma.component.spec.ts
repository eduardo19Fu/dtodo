import { fakeAsync, tick } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { Cliente } from '../../../models/cliente';
import { OpcionesDescuento } from '../../../dtos/descuento-dto';
import { DetalleProforma } from '../../../models/detalle-proforma';
import { Producto } from '../../../models/producto';
import { CreateProformaComponent } from './create-proforma.component';
import swal from 'sweetalert2';

describe('CreateProformaComponent - edición de detalle', () => {
  let component: CreateProformaComponent;
  let item: DetalleProforma;

  beforeEach(() => {
    component = new CreateProformaComponent(null, null, null, null, null, null, null, null, null);
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
      component = new CreateProformaComponent(null, null, clienteService, clienteCreateService, null, null, null, null, null);
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

describe('CreateProformaComponent - descuentos del cliente mayorista', () => {
  let component: CreateProformaComponent;
  let descuentoService: any;

  function opciones(extra: Partial<OpcionesDescuento> = {}): OpcionesDescuento {
    return Object.assign(new OpcionesDescuento(), {
      idProducto: 1, precioVenta: 50, precioMayorista: null, categoria: 'VOLUMEN', cantidad: 1,
      opciones: [
        { tipo: 'CATEGORIA', etiqueta: 'Categoría VOLUMEN: elegir nivel', porcentaje: null, niveles: [12, 15, 20],
          cantidadMinima: null, alcanzaMinimo: true, precio: null },
        { tipo: 'CIENTO', etiqueta: 'Por ciento: 12 % desde 100 unidades', porcentaje: 12, niveles: [],
          cantidadMinima: 100, alcanzaMinimo: false, precio: null },
        { tipo: 'MANUAL', etiqueta: 'Descuento manual', porcentaje: null, niveles: [], cantidadMinima: null,
          alcanzaMinimo: true, precio: null }
      ],
      sugerida: null
    }, extra);
  }

  function mayorista(): Cliente {
    const cliente = new Cliente();
    cliente.idCliente = 5;
    cliente.tipoCliente = 'MAYORISTA';
    return cliente;
  }

  beforeEach(() => {
    descuentoService = { getOpciones: jasmine.createSpy('getOpciones').and.returnValue(of(opciones())) };
    component = new CreateProformaComponent(null, null, null, null, null, null, null, null, descuentoService);
    component.producto = Object.assign(new Producto(), { idProducto: 1, nombre: 'RESMA', precioVenta: 50, stock: 500 });
    component.cantidadProducto = 10;
    spyOn(swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: true } as any));
  });

  it('con un cliente minorista no consulta descuentos y la línea queda como descuento manual', () => {
    component.cliente = new Cliente();
    component.descuentoProducto = 5;

    component.cargarOpciones();
    component.agregarLinea();

    expect(descuentoService.getOpciones).not.toHaveBeenCalled();
    expect(component.proforma.itemsProforma[0].tipoDescuento).toBe('MANUAL');
    expect(component.proforma.itemsProforma[0].descuento).toBe(5);
    expect(component.descuentoEditable).toBeTrue();
  });

  it('con un cliente mayorista pide al servidor las opciones del producto, cliente y cantidad', () => {
    component.cliente = mayorista();

    component.cargarOpciones();

    expect(descuentoService.getOpciones).toHaveBeenCalledWith(1, 5, 10);
    expect(component.opciones.opciones.length).toBe(3);
    expect(component.tipoDescuento).toBe('MANUAL');
  });

  it('aplica la sugerencia del servidor mientras el usuario no elija otro tipo', () => {
    component.cliente = mayorista();
    descuentoService.getOpciones.and.returnValue(of(opciones({
      sugerida: { tipo: 'CIENTO', etiqueta: 'x', porcentaje: 12, niveles: [], cantidadMinima: 100, alcanzaMinimo: true, precio: null }
    })));

    component.cargarOpciones();

    expect(component.tipoDescuento).toBe('CIENTO');
    expect(component.descuentoProducto).toBe(12);
    expect(component.descuentoEditable).toBeFalse();
  });

  it('respeta el tipo que el usuario eligió aunque cambie la cantidad', () => {
    component.cliente = mayorista();
    component.cargarOpciones();

    component.cambiarTipoDescuento('CIENTO');
    descuentoService.getOpciones.and.returnValue(of(opciones({
      sugerida: { tipo: 'CATEGORIA', etiqueta: 'x', porcentaje: 8, niveles: [], cantidadMinima: null, alcanzaMinimo: true, precio: null }
    })));
    component.cargarOpciones();

    expect(component.tipoDescuento).toBe('CIENTO');
    expect(component.descuentoProducto).toBe(12);
  });

  it('la nota de línea se abre para editar y al cerrar se limpia', () => {
    const item = new DetalleProforma();
    item.observacion = '  10 rojos  ';
    expect(component.notaAbierta(item)).toBeFalse();

    component.abrirNota(item, 0);
    expect(component.notaAbierta(item)).toBeTrue();

    component.cerrarNota(item);
    expect(component.notaAbierta(item)).toBeFalse();
    expect(item.observacion).toBe('10 rojos');

    item.observacion = '   ';
    component.cerrarNota(item);
    expect(item.observacion).toBeNull();
  });

  it('el selector flotante se cierra al elegir un tipo ya definido y con Escape', () => {
    component.cliente = mayorista();
    component.cargarOpciones();
    component.alternarSelectorDescuento();
    expect(component.selectorDescuentoAbierto).toBeTrue();

    component.elegirOpcionDescuento(component.opciones.opciones.find(o => o.tipo === 'CIENTO'));
    expect(component.tipoDescuento).toBe('CIENTO');
    expect(component.selectorDescuentoAbierto).toBeFalse();

    component.alternarSelectorDescuento();
    component.alPresionarEscape();
    expect(component.selectorDescuentoAbierto).toBeFalse();
  });

  it('el selector queda abierto al elegir Manual para escribir el porcentaje', () => {
    component.cliente = mayorista();
    component.cargarOpciones();
    component.alternarSelectorDescuento();

    component.elegirOpcionDescuento(component.opciones.opciones.find(o => o.tipo === 'MANUAL'));

    expect(component.selectorDescuentoAbierto).toBeTrue();
  });

  it('la categoría con varios niveles exige elegir uno antes de agregar la línea', () => {
    component.cliente = mayorista();
    component.cargarOpciones();
    component.cambiarTipoDescuento('CATEGORIA');

    expect(component.nivelesDisponibles).toEqual([12, 15, 20]);

    component.agregarLinea();
    expect(component.proforma.itemsProforma.length).toBe(0);

    component.elegirNivel(15);
    component.agregarLinea();

    expect(component.proforma.itemsProforma.length).toBe(1);
    expect(component.proforma.itemsProforma[0].descuento).toBe(15);
    expect(component.proforma.itemsProforma[0].tipoDescuento).toBe('CATEGORIA');
  });

  it('avisa, sin bloquear, cuando la cantidad no llega al mínimo del descuento elegido', () => {
    component.cliente = mayorista();
    component.cargarOpciones();

    component.cambiarTipoDescuento('CIENTO');

    expect(component.avisoMinimo).toContain('100 unidades');
    component.agregarLinea();
    expect(component.proforma.itemsProforma.length).toBe(1);
    expect(component.proforma.itemsProforma[0].tipoDescuento).toBe('CIENTO');
  });

  it('al agregar una línea se restablece el selector de descuento', () => {
    component.cliente = mayorista();
    component.cargarOpciones();
    component.cambiarTipoDescuento('CIENTO');

    component.agregarLinea();

    expect(component.opciones).toBeNull();
    expect(component.tipoDescuento).toBe('MANUAL');
    expect(component.descuentoProducto).toBe(0);
  });

  it('corregir el porcentaje de una línea a mano la deja como descuento manual', () => {
    component.cliente = mayorista();
    component.cargarOpciones();
    component.cambiarTipoDescuento('CIENTO');
    component.agregarLinea();

    component.abrirEdicionDetalle(0, 'descuento', document.createElement('button'));
    component.valorDetalleNuevo = 7;
    component.confirmarEdicionDetalle();

    expect(component.proforma.itemsProforma[0].descuento).toBe(7);
    expect(component.proforma.itemsProforma[0].tipoDescuento).toBe('MANUAL');
  });

  it('al cambiar de cliente las líneas con descuento tipado pasan a manual', () => {
    component.cliente = mayorista();
    component.cargarCliente(component.cliente);
    component.cargarOpciones();
    component.cambiarTipoDescuento('CIENTO');
    component.agregarLinea();
    expect(component.proforma.itemsProforma[0].tipoDescuento).toBe('CIENTO');

    const otro = new Cliente();
    otro.idCliente = 9;
    component.cargarCliente(otro);

    expect(component.proforma.itemsProforma[0].tipoDescuento).toBe('MANUAL');
    expect(component.proforma.itemsProforma[0].descuento).toBe(12);
  });

  it('si el servidor no responde las opciones, se sigue con descuento manual', () => {
    component.cliente = mayorista();
    descuentoService.getOpciones.and.returnValue(throwError({ status: 500 }));

    component.cargarOpciones();

    expect(component.opciones).toBeNull();
    expect(component.tipoDescuento).toBe('MANUAL');
    expect(component.descuentoEditable).toBeTrue();
  });
});

describe('CreateProformaComponent - proforma mayorista', () => {
  let component: CreateProformaComponent;
  let proformaService: any;

  function clienteConRuta(conRuta: boolean): Cliente {
    const cliente = new Cliente();
    cliente.idCliente = 5;
    cliente.tipoCliente = 'MAYORISTA';
    cliente.ruta = conRuta ? { idRuta: 1, nombre: 'RUTA 1' } : null;
    return cliente;
  }

  function conLinea(): DetalleProforma {
    const item = new DetalleProforma();
    item.producto = Object.assign(new Producto(), { idProducto: 1, nombre: 'CUADERNO', precioVenta: 10, stock: 50 });
    item.cantidad = 2;
    item.observacion = '10 rojos';
    component.proforma.itemsProforma = [item];
    return item;
  }

  beforeEach(() => {
    proformaService = {
      create: jasmine.createSpy('create').and.returnValue(of({ idProforma: 9 })),
      update: jasmine.createSpy('update'),
      getProformaPdf: jasmine.createSpy('getProformaPdf').and.returnValue(of({ data: new Blob(['x']) }))
    };
    component = new CreateProformaComponent(proformaService, null, null, null, null, null, null, null, null);
    component.usuario = Object.assign(component.usuario, { idUsuario: 1 });
    spyOn(component, 'generarProformaPdf');
  });

  it('es mayorista solo con un cliente mayorista que tiene ruta', () => {
    component.cliente = clienteConRuta(true);
    expect(component.esProformaMayorista).toBeTrue();

    component.cliente = clienteConRuta(false);
    expect(component.esProformaMayorista).toBeFalse();

    component.cliente = new Cliente();
    expect(component.esProformaMayorista).toBeFalse();
  });

  it('al guardar una mayorista envía tipo, observaciones y notas de línea, y pide las dos copias', () => {
    component.cliente = clienteConRuta(true);
    const item = conLinea();
    component.proforma.observaciones = '  empacar aparte  ';
    component.proforma.fechaEntregaEstimada = '2026-11-02';

    component.createProforma();

    const enviada = proformaService.create.calls.mostRecent().args[0];
    expect(enviada.tipoProforma).toBe('MAYORISTA');
    expect(enviada.observaciones).toBe('empacar aparte');
    expect(enviada.fechaEntregaEstimada).toBe('2026-11-02');
    expect(item.observacion).toBe('10 rojos');
    expect(component.generarProformaPdf).toHaveBeenCalledWith(9, true);
  });

  it('al guardar una minorista descarta observaciones y fecha de entrega, y pide una sola copia', () => {
    component.cliente = new Cliente();
    component.cliente.idCliente = 7;
    const item = conLinea();
    component.proforma.observaciones = 'no debe viajar';
    component.proforma.fechaEntregaEstimada = '2026-11-02';

    component.createProforma();

    const enviada = proformaService.create.calls.mostRecent().args[0];
    expect(enviada.tipoProforma).toBe('MINORISTA');
    expect(enviada.observaciones).toBeNull();
    expect(enviada.fechaEntregaEstimada).toBeNull();
    expect(item.observacion).toBeNull();
    expect(component.generarProformaPdf).toHaveBeenCalledWith(9, false);
  });

  it('una observación general en blanco se envía como nula', () => {
    component.cliente = clienteConRuta(true);
    conLinea();
    component.proforma.observaciones = '   ';

    component.createProforma();

    expect(proformaService.create.calls.mostRecent().args[0].observaciones).toBeNull();
  });

  it('el mínimo de la entrega estimada es hoy', () => {
    expect(component.hoy).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });
});

describe('CreateProformaComponent - autorización y motivo de cambios', () => {
  let component: CreateProformaComponent;
  let proformaService: any;

  function mayoristaConRuta(): Cliente {
    const cliente = new Cliente();
    cliente.idCliente = 5;
    cliente.tipoCliente = 'MAYORISTA';
    cliente.ruta = { idRuta: 1, nombre: 'RUTA 1' };
    return cliente;
  }

  function linea(descuento: number, tipo: any = 'MANUAL'): DetalleProforma {
    const item = new DetalleProforma();
    item.producto = Object.assign(new Producto(), { idProducto: 1, nombre: 'CUADERNO', precioVenta: 10, stock: 50 });
    item.cantidad = 2;
    item.descuento = descuento;
    item.tipoDescuento = tipo;
    return item;
  }

  beforeEach(() => {
    proformaService = {
      create: jasmine.createSpy('create'),
      update: jasmine.createSpy('update').and.returnValue(of({ idProforma: 9, noProforma: '9P', mensaje: 'ok' })),
      getProformaPdf: jasmine.createSpy('getProformaPdf')
    };
    const router: any = { navigate: jasmine.createSpy('navigate') };
    component = new CreateProformaComponent(proformaService, null, null, null, null, null, null, router, null);
    component.proforma.idProforma = 9;
    spyOn(component, 'generarProformaPdf');
  });

  it('avisa que quedará pendiente solo cuando una línea mayorista pasa del tope', () => {
    component.cliente = mayoristaConRuta();
    component.proforma.itemsProforma = [linea(10), linea(15)];
    expect(component.requiereAutorizacion).toBeFalse();

    component.proforma.itemsProforma = [linea(10), linea(15.5)];
    expect(component.requiereAutorizacion).toBeTrue();
  });

  it('el precio mayorista fijo y la proforma minorista no avisan', () => {
    component.cliente = mayoristaConRuta();
    component.proforma.itemsProforma = [linea(30, 'PRECIO_MAYORISTA')];
    expect(component.requiereAutorizacion).toBeFalse();

    component.cliente = new Cliente();
    component.proforma.itemsProforma = [linea(40)];
    expect(component.requiereAutorizacion).toBeFalse();
  });

  it('editar una mayorista pide el motivo y lo envía junto con los cambios', async () => {
    component.cliente = mayoristaConRuta();
    component.proforma.itemsProforma = [linea(5)];
    spyOn(swal, 'fire').and.callFake(((opciones: any) => opciones && opciones.input
      ? Promise.resolve({ isConfirmed: true, value: '  cambio de cantidad ' }) : Promise.resolve({ isConfirmed: true })) as any);

    component.update();
    await Promise.resolve();
    await Promise.resolve();

    expect(proformaService.update).toHaveBeenCalledWith(component.proforma, 'cambio de cantidad');
    expect(component.generarProformaPdf).toHaveBeenCalledWith(9, true);
  });

  it('si se cancela el motivo no se guarda nada', async () => {
    component.cliente = mayoristaConRuta();
    component.proforma.itemsProforma = [linea(5)];
    spyOn(swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: false } as any));

    component.update();
    await Promise.resolve();
    await Promise.resolve();

    expect(proformaService.update).not.toHaveBeenCalled();
  });

  it('editar una minorista no pide motivo', () => {
    component.cliente = new Cliente();
    component.cliente.idCliente = 7;
    component.proforma.itemsProforma = [linea(5)];
    const fire = spyOn(swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: true } as any));

    component.update();

    // Solo el aviso final de "actualizada": ningún cuadro pide el motivo.
    expect(fire.calls.allArgs().some(args => !!(args[0] as any).input)).toBeFalse();
    expect(proformaService.update).toHaveBeenCalledWith(component.proforma, undefined);
  });

  it('al crear una proforma pendiente avisa antes de abrir los PDF', async () => {
    component.cliente = mayoristaConRuta();
    component.cliente.idCliente = 5;
    component.proforma.itemsProforma = [linea(20)];
    component.usuario = Object.assign(component.usuario, { idUsuario: 1 });
    proformaService.create.and.returnValue(of({ idProforma: 9, noProforma: '9P', estadoAutorizacion: 'PENDIENTE' }));
    const fire = spyOn(swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: true } as any));

    component.createProforma();
    await Promise.resolve();
    await Promise.resolve();

    expect(fire).toHaveBeenCalledWith('Pendiente de autorización', jasmine.stringMatching('9P'), 'info');
    expect(component.generarProformaPdf).toHaveBeenCalledWith(9, true);
  });

  it('avisa que editar un pedido en preparación lo devuelve a confirmado', () => {
    component.cliente = mayoristaConRuta();
    component.proforma.estadoPedido = 'EMPACADA';
    expect(component.pedidoEnPreparacion).toBeTrue();

    component.proforma.estadoPedido = 'CONFIRMADA';
    expect(component.pedidoEnPreparacion).toBeFalse();

    component.proforma.estadoPedido = 'EN_RUTA';
    component.cliente = new Cliente();
    expect(component.pedidoEnPreparacion).toBeFalse();
  });
});
