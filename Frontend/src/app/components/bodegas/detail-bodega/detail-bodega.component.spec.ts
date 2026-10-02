import { DetailBodegaComponent } from './detail-bodega.component';

describe('DetailBodegaComponent', () => {
  let component: DetailBodegaComponent;

  beforeEach(() => {
    component = new DetailBodegaComponent({ nativeElement: document.createElement('div') } as any, document);
  });

  it('una bodega con estado ACTIVO se muestra como activa', () => {
    component.bodega = { estado: { estado: 'ACTIVO' } } as any;
    expect(component.activa).toBeTrue();

    component.bodega = { estado: { estado: 'INACTIVO' } } as any;
    expect(component.activa).toBeFalse();
  });

  it('arma el nombre de quien registró la bodega', () => {
    component.bodega = { usuario: { primerNombre: 'Ana', apellido: 'Gómez', usuario: 'agomez' } } as any;
    expect(component.nombreRegistro).toBe('Ana Gómez');

    component.bodega = { usuario: { usuario: 'agomez' } } as any;
    expect(component.nombreRegistro).toBe('agomez');

    component.bodega = {} as any;
    expect(component.nombreRegistro).toBe('No registrado');
  });

  it('se cierra con Escape', () => {
    let cierres = 0;
    component.cerrar.subscribe(() => cierres++);

    component.cerrarConEscape();

    expect(cierres).toBe(1);
  });
});
