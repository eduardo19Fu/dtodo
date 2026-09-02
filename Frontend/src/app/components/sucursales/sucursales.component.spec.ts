import { SucursalesComponent } from './sucursales.component';

describe('SucursalesComponent - orden y paginacion', () => {
  let component: SucursalesComponent;

  beforeEach(() => {
    component = new SucursalesComponent(null, null);
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
