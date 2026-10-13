import { of, throwError } from 'rxjs';

import { BodegasComponent } from './bodegas.component';
import swal from 'sweetalert2';

describe('BodegasComponent', () => {
  let component: BodegasComponent;
  let bodegaService: any;

  const respuesta = (contenido: any[], extra: any = {}) => ({
    content: contenido, number: 0, totalPages: 3, totalElements: 12, size: 5, first: true, last: false, ...extra
  });

  beforeEach(() => {
    bodegaService = {
      getListado: jasmine.createSpy('getListado').and.returnValue(of(respuesta([{ idBodega: 1, nombre: 'Principal' }]))),
      getBodega: jasmine.createSpy('getBodega')
    };
    component = new BodegasComponent(bodegaService, { usuario: {}, hasRole: () => true } as any);
  });

  it('carga la primera página con el orden por defecto', () => {
    component.cargarBodegas(0);

    expect(bodegaService.getListado).toHaveBeenCalledWith(0, 5, '', 'nombre', 'asc');
    expect(component.bodegas.length).toBe(1);
    expect(component.totalElementos).toBe(12);
    expect(component.totalPaginas).toBe(3);
    expect(component.isFirst).toBeTrue();
    expect(component.cargando).toBeFalse();
  });

  it('informa el error cuando no se pueden cargar las bodegas', () => {
    const alerta = spyOn(swal, 'fire');
    bodegaService.getListado.and.returnValue(throwError({ error: { message: 'Sin conexión' } }));

    component.cargarBodegas(0);

    expect(component.cargando).toBeFalse();
    expect(alerta).toHaveBeenCalledWith('Error al cargar bodegas', 'Sin conexión', 'error');
  });

  it('al ordenar por la misma columna invierte el sentido y vuelve a la primera página', () => {
    component.ordenarPor('nombre');
    expect(component.direccion).toBe('desc');
    expect(bodegaService.getListado).toHaveBeenCalledWith(0, 5, '', 'nombre', 'desc');

    component.ordenarPor('ubicacion');
    expect(component.orden).toBe('ubicacion');
    expect(component.direccion).toBe('asc');
  });

  it('indica el icono de orden de cada columna', () => {
    expect(component.iconoOrden('nombre')).toBe('fas fa-sort-up');
    expect(component.iconoOrden('id')).toBe('fas fa-sort');
    component.direccion = 'desc';
    expect(component.iconoOrden('nombre')).toBe('fas fa-sort-down');
  });

  it('cambia el tamaño de página reiniciando en la primera', () => {
    component.cambiarPageSize(25);

    expect(bodegaService.getListado).toHaveBeenCalledWith(0, 25, '', 'nombre', 'asc');
  });

  it('calcula las páginas visibles centradas en la actual', () => {
    component.totalPaginas = 10;
    component.paginaActual = 5;

    expect(component.paginasVisibles).toEqual([3, 4, 5, 6, 7]);
  });

  it('no avanza más allá de la última página ni retrocede antes de la primera', () => {
    component.isLast = true;
    component.isFirst = true;
    component.paginaActual = 0;
    bodegaService.getListado.calls.reset();

    component.irPaginaSiguiente();
    component.irPaginaAnterior();

    expect(bodegaService.getListado).not.toHaveBeenCalled();
  });

  it('una bodega está activa solo con estado ACTIVO', () => {
    expect(component.estaActiva({ estado: 'ACTIVO' } as any)).toBeTrue();
    expect(component.estaActiva({ estado: 'INACTIVO' } as any)).toBeFalse();
  });

  it('abre el detalle cargando la bodega completa y bloquea otra apertura mientras tanto', () => {
    spyOn(swal, 'fire');
    spyOn(swal, 'close');
    bodegaService.getBodega.and.returnValue(of({ idBodega: 7, nombre: 'Norte' }));

    component.abrirDetalle({ idBodega: 7, nombre: 'Norte' } as any);

    expect(bodegaService.getBodega).toHaveBeenCalledWith(7);
    expect(component.bodegaSeleccionada.nombre).toBe('Norte');
    expect(component.detalleCargandoId).toBeNull();

    component.detalleCargandoId = 9;
    bodegaService.getBodega.calls.reset();
    component.abrirDetalle({ idBodega: 8 } as any);
    expect(bodegaService.getBodega).not.toHaveBeenCalled();
  });

  it('cierra el detalle', () => {
    component.bodegaSeleccionada = { idBodega: 1 } as any;

    component.cerrarDetalle();

    expect(component.bodegaSeleccionada).toBeNull();
  });
});
