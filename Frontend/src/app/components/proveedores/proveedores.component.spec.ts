import { ProveedoresComponent } from './proveedores.component';

describe('ProveedoresComponent - paginacion y orden', () => {
  let component: ProveedoresComponent;

  beforeEach(() => {
    component = new ProveedoresComponent(null, { hasRole: () => true } as any);
  });

  it('usa el icono neutro cuando la columna no es la ordenada actualmente', () => {
    component.orden = 'nombre';

    expect(component.iconoOrden('id')).toBe('fas fa-sort');
  });

  it('usa la flecha hacia arriba cuando la columna esta ordenada ascendente', () => {
    component.orden = 'nombre';
    component.direccion = 'asc';

    expect(component.iconoOrden('nombre')).toBe('fas fa-sort-up');
  });

  it('calcula las paginas visibles centradas en la pagina actual', () => {
    component.totalPaginas = 10;
    component.paginaActual = 5;

    expect(component.paginasVisibles).toEqual([3, 4, 5, 6, 7]);
  });
});
