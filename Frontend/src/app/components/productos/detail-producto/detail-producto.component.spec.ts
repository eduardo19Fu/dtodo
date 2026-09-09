import { fakeAsync, tick } from '@angular/core/testing';

import { DetailProductoComponent } from './detail-producto.component';

describe('DetailProductoComponent', () => {
  it('emite el cierre después de completar la transición', fakeAsync(() => {
    const component = new DetailProductoComponent(null, null);
    const cerrar = spyOn(component.cerrar, 'emit');
    spyOn(window, 'matchMedia').and.returnValue({ matches: false } as MediaQueryList);

    component.cerrarModal();

    expect(component.cerrando).toBeTrue();
    expect(cerrar).not.toHaveBeenCalled();

    tick(180);
    expect(cerrar).toHaveBeenCalled();
  }));

  it('evita programar más de un cierre', fakeAsync(() => {
    const component = new DetailProductoComponent(null, null);
    const cerrar = spyOn(component.cerrar, 'emit');
    spyOn(window, 'matchMedia').and.returnValue({ matches: false } as MediaQueryList);

    component.cerrarModal();
    component.cerrarModal();
    tick(180);

    expect(cerrar).toHaveBeenCalledTimes(1);
  }));
});
