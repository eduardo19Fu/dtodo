import { of } from 'rxjs';
import { SucursalesComponent } from './sucursales.component';

describe('SucursalesComponent - orden y paginacion', () => {
  let component: SucursalesComponent;

  beforeEach(() => {
    component = new SucursalesComponent(null, null, null);
  });

  it('usa el icono neutro cuando la columna no es la ordenada actualmente', () => {
    component.orden = 'nombre';

    expect(component.iconoOrden('direccion')).toBe('fas fa-sort');
  });

  it('usa la flecha hacia arriba cuando la columna esta ordenada ascendente', () => {
    component.orden = 'nombre';
    component.direccion = 'asc';

    expect(component.iconoOrden('nombre')).toBe('fas fa-sort-up');
  });

  it('usa la flecha hacia abajo cuando la columna esta ordenada descendente', () => {
    component.orden = 'nombre';
    component.direccion = 'desc';

    expect(component.iconoOrden('nombre')).toBe('fas fa-sort-down');
  });

  it('calcula las paginas visibles centradas en la pagina actual', () => {
    component.totalPaginas = 10;
    component.paginaActual = 5;

    expect(component.paginasVisibles).toEqual([3, 4, 5, 6, 7]);
  });

  it('no muestra paginas negativas cuando la pagina actual esta al inicio', () => {
    component.totalPaginas = 10;
    component.paginaActual = 0;

    expect(component.paginasVisibles).toEqual([0, 1, 2, 3, 4]);
  });
});

describe('SucursalesComponent - abrirDetalle', () => {
  let component: SucursalesComponent;
  let sucursalService: any;
  let detailSucursalService: any;

  beforeEach(() => {
    sucursalService = { getSucursal: jasmine.createSpy('getSucursal') };
    detailSucursalService = { abrirModal: jasmine.createSpy('abrirModal') };
    component = new SucursalesComponent(sucursalService, detailSucursalService, null);
  });

  it('consulta la sucursal seleccionada y abre el modal de detalle', () => {
    const sucursalDetalle: any = { idSucursal: 3, nombre: 'Sucursal de Prueba' };
    sucursalService.getSucursal.and.returnValue(of(sucursalDetalle));

    component.abrirDetalle({ idSucursal: 3 } as any);

    expect(sucursalService.getSucursal).toHaveBeenCalledWith(3);
    expect(component.sucursalSeleccionada).toBe(sucursalDetalle);
    expect(component.detalleCargandoId).toBeNull();
    expect(detailSucursalService.abrirModal).toHaveBeenCalled();
  });

  it('no dispara una nueva consulta si ya hay un detalle cargando', () => {
    component.detalleCargandoId = 1;

    component.abrirDetalle({ idSucursal: 3 } as any);

    expect(sucursalService.getSucursal).not.toHaveBeenCalled();
  });
});
