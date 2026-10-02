import { of, throwError } from 'rxjs';

import { InventarioBodegaComponent } from './inventario-bodega.component';
import swal from 'sweetalert2';

describe('InventarioBodegaComponent', () => {
  let component: InventarioBodegaComponent;
  let bodegaService: any;
  let params$: any;

  const pagina = (contenido: any[], extra: any = {}) => ({
    content: contenido, number: 0, totalPages: 1, totalElements: contenido.length, size: 10, first: true, last: true, ...extra
  });

  beforeEach(() => {
    bodegaService = {
      getBodega: jasmine.createSpy('getBodega').and.returnValue(of({ idBodega: 2, nombre: 'Principal', estado: { estado: 'ACTIVO' } })),
      getInventario: jasmine.createSpy('getInventario').and.returnValue(of(pagina([{ idProducto: 1, stock: 5 }]))),
      getMovimientos: jasmine.createSpy('getMovimientos').and.returnValue(of(pagina([{ idMovimiento: 1 }])))
    };
    params$ = of({ id: '2' });
    component = new InventarioBodegaComponent(bodegaService, { params: params$ } as any);
  });

  afterEach(() => component.ngOnDestroy());

  it('al iniciar carga la bodega y su inventario según el id de la ruta', () => {
    component.ngOnInit();

    expect(component.idBodega).toBe(2);
    expect(bodegaService.getBodega).toHaveBeenCalledWith(2);
    expect(bodegaService.getInventario).toHaveBeenCalledWith(2, 0, 10, '');
    expect(component.items.length).toBe(1);
    expect(component.activa).toBeTrue();
  });

  it('una bodega inactiva no está activa', () => {
    bodegaService.getBodega.and.returnValue(of({ idBodega: 2, estado: { estado: 'INACTIVO' } }));

    component.ngOnInit();

    expect(component.activa).toBeFalse();
  });

  it('informa el error cuando no se puede cargar el inventario', () => {
    const alerta = spyOn(swal, 'fire');
    bodegaService.getInventario.and.returnValue(throwError({ error: { message: 'Sin conexión' } }));
    component.idBodega = 2;

    component.cargarExistencias(0);

    expect(component.cargando).toBeFalse();
    expect(alerta).toHaveBeenCalledWith('Error al cargar el inventario', 'Sin conexión', 'error');
  });

  it('marca como bajo mínimo solo los productos con mínimo configurado y existencia igual o menor', () => {
    expect(component.bajoMinimo({ stock: 3, stockMinimo: 5 } as any)).toBeTrue();
    expect(component.bajoMinimo({ stock: 5, stockMinimo: 5 } as any)).toBeTrue();
    expect(component.bajoMinimo({ stock: 6, stockMinimo: 5 } as any)).toBeFalse();
    expect(component.bajoMinimo({ stock: 0, stockMinimo: null } as any)).toBeFalse();
    expect(component.bajoMinimo({ stock: 0, stockMinimo: 0 } as any)).toBeFalse();
  });

  it('los movimientos solo se cargan al abrir la pestaña por primera vez', () => {
    component.idBodega = 2;

    component.cambiarVista('movimientos');
    expect(bodegaService.getMovimientos).toHaveBeenCalledTimes(1);

    component.cambiarVista('existencias');
    component.cambiarVista('movimientos');
    expect(bodegaService.getMovimientos).toHaveBeenCalledTimes(1);
  });

  it('muestra la cantidad con signo según el tipo de movimiento', () => {
    expect(component.cantidadConSigno({ tipoMovimiento: 'INGRESO', cantidad: 5 } as any)).toBe('+5');
    expect(component.cantidadConSigno({ tipoMovimiento: 'ANULACION_DESPACHO', cantidad: 2 } as any)).toBe('+2');
    expect(component.cantidadConSigno({ tipoMovimiento: 'DESPACHO', cantidad: 4 } as any)).toBe('-4');
    expect(component.cantidadConSigno({ tipoMovimiento: 'ELIMINACION', cantidad: 0 } as any)).toBe('0');
  });

  it('no consulta movimientos con un rango de fechas incompleto', () => {
    component.idBodega = 2;
    component.fechaInicio = '2026-10-01';
    component.fechaFin = '';

    component.aplicarFiltrosMovimientos();

    expect(bodegaService.getMovimientos).not.toHaveBeenCalled();
  });

  it('rechaza un rango de fechas invertido', () => {
    const alerta = spyOn(swal, 'fire');
    component.idBodega = 2;
    component.fechaInicio = '2026-10-05';
    component.fechaFin = '2026-10-01';

    component.aplicarFiltrosMovimientos();

    expect(alerta).toHaveBeenCalled();
    expect(bodegaService.getMovimientos).not.toHaveBeenCalled();
  });

  it('consulta los movimientos con tipo y rango completos', () => {
    component.idBodega = 2;
    component.fechaInicio = '2026-10-01';
    component.fechaFin = '2026-10-02';
    component.tipoMovimiento = 'DESPACHO';

    component.aplicarFiltrosMovimientos();

    expect(bodegaService.getMovimientos).toHaveBeenCalledWith(2, 0, 10, {
      fechaIni: '2026-10-01', fechaFin: '2026-10-02', tipo: 'DESPACHO', filtro: ''
    });
  });

  it('limpiar filtros de movimientos los reinicia y recarga', () => {
    component.idBodega = 2;
    component.fechaInicio = '2026-10-01';
    component.fechaFin = '2026-10-02';
    component.tipoMovimiento = 'INGRESO';
    component.filtroMovimientos = 'lapiz';
    expect(component.hayFiltrosMovimientos).toBeTrue();

    component.limpiarFiltrosMovimientos();

    expect(component.hayFiltrosMovimientos).toBeFalse();
    expect(bodegaService.getMovimientos).toHaveBeenCalled();
  });

  it('abre y cierra el modal de movimientos con el producto elegido', () => {
    const item: any = { idProducto: 7 };

    component.abrirMovimiento('REDUCIR', item);
    expect(component.modoMovimiento).toBe('REDUCIR');
    expect(component.itemMovimiento).toBe(item);

    component.cerrarMovimiento();
    expect(component.modoMovimiento).toBeNull();
    expect(component.itemMovimiento).toBeNull();
  });

  it('al completar un movimiento cierra el modal y recarga existencias y movimientos', () => {
    component.idBodega = 2;
    component.vista = 'movimientos';
    component.abrirMovimiento('AGREGAR');

    component.movimientoCompletado();

    expect(component.modoMovimiento).toBeNull();
    expect(bodegaService.getInventario).toHaveBeenCalled();
    expect(bodegaService.getMovimientos).toHaveBeenCalled();
  });

  it('si se quita el único producto de la última página regresa a la página anterior', () => {
    component.idBodega = 2;
    component.items = [{ idProducto: 1 } as any];
    component.paginacion.paginaActual = 3;
    component.paginacion.isFirst = false;

    component.movimientoCompletado();

    expect(bodegaService.getInventario).toHaveBeenCalledWith(2, 2, 10, '');
  });

  it('abre y cierra la importación de inventario', () => {
    component.abrirImportar();
    expect(component.importarVisible).toBeTrue();

    component.importacionCompletada();
    expect(component.importarVisible).toBeFalse();
  });
});
