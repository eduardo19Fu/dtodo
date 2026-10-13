import { PaginacionTabla } from './paginacion-tabla';

describe('PaginacionTabla', () => {
  let paginacion: PaginacionTabla;

  beforeEach(() => {
    paginacion = new PaginacionTabla(10, [10, 20]);
  });

  it('inicia en la primera página sin registros', () => {
    expect(paginacion.paginaActual).toBe(0);
    expect(paginacion.totalElementos).toBe(0);
    expect(paginacion.isFirst).toBeTrue();
    expect(paginacion.pageSize).toBe(10);
    expect(paginacion.pageSizeOptions).toEqual([10, 20]);
  });

  it('copia los datos de una respuesta paginada del backend', () => {
    paginacion.actualizar({ number: 2, totalPages: 7, totalElements: 68, size: 10, first: false, last: false });

    expect(paginacion.paginaActual).toBe(2);
    expect(paginacion.totalPaginas).toBe(7);
    expect(paginacion.totalElementos).toBe(68);
    expect(paginacion.isFirst).toBeFalse();
    expect(paginacion.isLast).toBeFalse();
  });

  it('calcula las páginas visibles centradas en la actual', () => {
    paginacion.totalPaginas = 10;
    paginacion.paginaActual = 5;

    expect(paginacion.paginasVisibles).toEqual([3, 4, 5, 6, 7]);
  });

  it('no muestra páginas negativas ni más allá de la última', () => {
    paginacion.totalPaginas = 10;
    paginacion.paginaActual = 0;
    expect(paginacion.paginasVisibles).toEqual([0, 1, 2, 3, 4]);

    paginacion.paginaActual = 9;
    expect(paginacion.paginasVisibles).toEqual([5, 6, 7, 8, 9]);
  });

  it('muestra todas las páginas cuando hay menos de cinco', () => {
    paginacion.totalPaginas = 3;
    paginacion.paginaActual = 1;

    expect(paginacion.paginasVisibles).toEqual([0, 1, 2]);
  });

  it('calcula el rango de registros mostrados', () => {
    paginacion.actualizar({ number: 2, totalPages: 5, totalElements: 47, size: 10, first: false, last: false });

    expect(paginacion.desde()).toBe(21);
    expect(paginacion.hasta(10)).toBe(30);
  });

  it('el primer registro mostrado es 0 cuando no hay registros', () => {
    expect(paginacion.desde()).toBe(0);
  });
});
