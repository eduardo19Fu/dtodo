import { of, throwError } from 'rxjs';

import { CreateDevolucionBodegaComponent } from './create-devolucion-bodega.component';
import swal from 'sweetalert2';

describe('CreateDevolucionBodegaComponent', () => {
  let component: CreateDevolucionBodegaComponent;
  let bodegaService: any;
  let devolucionService: any;
  let sucursalService: any;
  let router: any;
  let auth: any;

  const sucursales = [
    { idSucursal: 2, nombre: 'Norte', estado: { estado: 'ACTIVO' } },
    { idSucursal: 3, nombre: 'Sur', estado: { estado: 'INACTIVO' } }
  ];
  const bodegas = [
    { idBodega: 1, nombre: 'Principal', sucursal: { idSucursal: 2 } },
    { idBodega: 5, nombre: 'Secundaria' }
  ];
  const item = { idInventarioSucursal: 1, idProducto: 10, codProducto: 'C-10', nombreProducto: 'Cuaderno', stock: 20 };

  function crear(queryBodega: string = null): CreateDevolucionBodegaComponent {
    const ruta: any = { snapshot: { queryParamMap: { get: () => queryBodega } } };
    return new CreateDevolucionBodegaComponent(bodegaService, devolucionService, sucursalService, ruta, router, auth);
  }

  beforeEach(() => {
    bodegaService = { getBodegas: jasmine.createSpy('getBodegas').and.returnValue(of(bodegas)) };
    devolucionService = {
      getProductosSucursal: jasmine.createSpy('getProductosSucursal').and.returnValue(of({ content: [item] })),
      create: jasmine.createSpy('create').and.returnValue(of({ idDevolucion: 9 })),
      imprimirComprobante: jasmine.createSpy('imprimirComprobante').and.returnValue(of(undefined))
    };
    sucursalService = { getSucursales: jasmine.createSpy('getSucursales').and.returnValue(of(sucursales)) };
    router = { navigate: jasmine.createSpy('navigate') };
    auth = { usuario: { sucursal: { idSucursal: 2 } } };
    component = crear();
    spyOn(swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: true } as any));
  });

  it('propone la tienda del usuario y la bodega asignada a esa tienda, y excluye las sucursales inactivas', () => {
    component.ngOnInit();

    expect(component.sucursales.map(s => s.idSucursal)).toEqual([2]);
    expect(component.devolucion.idSucursalOrigen).toBe(2);
    expect(component.devolucion.idBodega).toBe(1);
  });

  it('toma la bodega de la ruta cuando se llega desde una bodega', () => {
    component = crear('5');

    component.ngOnInit();

    expect(component.devolucion.idBodega).toBe(5);
  });

  it('al elegir un producto el campo de cantidad recibe el foco', (hecho) => {
    const cantidad = document.createElement('input');
    cantidad.id = 'cantidadActual';
    document.body.appendChild(cantidad);
    component.devolucion.idSucursalOrigen = 2;
    component.elegirDelModal(item as any);

    setTimeout(() => {
      expect(document.activeElement).toBe(cantidad);
      document.body.removeChild(cantidad);
      hecho();
    });
  });

  it('pide elegir la tienda antes de buscar productos', () => {
    component.codigoBusqueda = 'C-10';

    component.buscarProductoPorCodigo();

    expect(devolucionService.getProductosSucursal).not.toHaveBeenCalled();
    expect(swal.fire).toHaveBeenCalledWith('Selecciona la tienda', jasmine.any(String), 'warning');
  });

  it('solo acepta la coincidencia exacta del código en el inventario de la tienda', () => {
    component.devolucion.idSucursalOrigen = 2;
    component.codigoBusqueda = 'C-1';
    devolucionService.getProductosSucursal.and.returnValue(of({ content: [item] }));

    component.buscarProductoPorCodigo();
    expect(component.itemActual).toBeNull();

    component.codigoBusqueda = 'c-10';
    component.buscarProductoPorCodigo();
    expect(component.itemActual.idProducto).toBe(10);
    expect(devolucionService.getProductosSucursal).toHaveBeenCalledWith(2, 0, 10, 'c-10');
  });

  it('agrega una línea y la suma si el producto ya estaba en el detalle', () => {
    component.devolucion.idSucursalOrigen = 2;
    component.itemActual = item as any;
    component.cantidadActual = 5;

    component.agregarLinea();
    component.itemActual = item as any;
    component.cantidadActual = 3;
    component.agregarLinea();

    expect(component.lineas.length).toBe(1);
    expect(component.lineas[0].cantidad).toBe(8);
    expect(component.lineas[0].existenciaSucursal).toBe(20);
    expect(component.totalUnidades).toBe(8);
    expect(component.itemActual).toBeNull();
  });

  it('no permite devolver más de lo que tiene la tienda, contando lo ya agregado', () => {
    component.devolucion.idSucursalOrigen = 2;
    component.itemActual = item as any;
    component.cantidadActual = 15;
    component.agregarLinea();

    component.itemActual = item as any;
    expect(component.disponibleItemActual).toBe(5);
    component.cantidadActual = 6;
    component.agregarLinea();

    expect(component.lineas[0].cantidad).toBe(15);
    expect(swal.fire).toHaveBeenCalledWith('Existencias insuficientes', jasmine.any(String), 'warning');
  });

  it('rechaza cantidades que no son enteros positivos', () => {
    component.itemActual = item as any;

    component.cantidadActual = 0;
    component.agregarLinea();
    component.cantidadActual = 1.5;
    component.agregarLinea();

    expect(component.lineas.length).toBe(0);
  });

  it('la edición de cantidad respeta la existencia de la tienda', () => {
    component.devolucion.idSucursalOrigen = 2;
    component.itemActual = item as any;
    component.cantidadActual = 5;
    component.agregarLinea();
    component.abrirEdicionDetalle(0, document.createElement('button'));

    component.valorDetalleNuevo = 25;
    component.confirmarEdicionDetalle();
    expect(component.errorEdicionDetalle).toContain('20');
    expect(component.lineas[0].cantidad).toBe(5);

    component.valorDetalleNuevo = 12;
    component.confirmarEdicionDetalle();
    expect(component.lineas[0].cantidad).toBe(12);
    expect(component.edicionDetalleAbierta).toBeFalse();
  });

  it('no registra sin productos, sin quien entrega o sin motivo', () => {
    component.devolucion.idSucursalOrigen = 2;
    component.devolucion.idBodega = 1;

    component.crear();
    expect(swal.fire).toHaveBeenCalledWith('Sin productos', jasmine.any(String), 'warning');

    component.itemActual = item as any;
    component.cantidadActual = 2;
    component.agregarLinea();
    component.devolucion.entregadoPor = '  ';
    component.devolucion.motivo = 'Sobrestock';
    component.crear();
    component.devolucion.entregadoPor = 'Maria';
    component.devolucion.motivo = ' ';
    component.crear();

    expect(devolucionService.create).not.toHaveBeenCalled();
  });

  it('registra la devolución enviando solo identificadores, cantidades y textos recortados', () => {
    component.devolucion.idSucursalOrigen = 2;
    component.devolucion.idBodega = 1;
    component.devolucion.entregadoPor = '  Maria López ';
    component.devolucion.motivo = ' Sobrestock ';
    component.devolucion.observaciones = '';
    component.itemActual = item as any;
    component.cantidadActual = 4;
    component.agregarLinea();

    component.crear();

    expect(devolucionService.create).toHaveBeenCalledWith({
      idSucursalOrigen: 2,
      idBodega: 1,
      entregadoPor: 'Maria López',
      motivo: 'Sobrestock',
      observaciones: '',
      items: [{ idProducto: 10, cantidad: 4 }]
    });
    expect(router.navigate).toHaveBeenCalledWith(['/devoluciones-bodega/index']);
    expect(component.guardando).toBeFalse();
  });

  it('al registrar ofrece imprimir el comprobante y lo imprime si el usuario acepta', async () => {
    component.devolucion.idSucursalOrigen = 2;
    component.devolucion.idBodega = 1;
    component.devolucion.entregadoPor = 'Maria';
    component.devolucion.motivo = 'Sobrestock';
    component.itemActual = item as any;
    component.cantidadActual = 4;
    component.agregarLinea();

    component.crear();
    await Promise.resolve();

    expect(devolucionService.imprimirComprobante).toHaveBeenCalledWith(9);
  });

  it('libera el botón de guardar si el servidor rechaza la devolución', () => {
    devolucionService.create.and.returnValue(throwError({ error: { message: 'Sin stock' } }));
    component.devolucion.idSucursalOrigen = 2;
    component.devolucion.idBodega = 1;
    component.devolucion.entregadoPor = 'Maria';
    component.devolucion.motivo = 'Sobrestock';
    component.itemActual = item as any;
    component.cantidadActual = 4;
    component.agregarLinea();

    component.crear();

    expect(component.guardando).toBeFalse();
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
