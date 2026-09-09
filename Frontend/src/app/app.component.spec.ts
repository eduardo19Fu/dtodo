import { fakeAsync, tick } from '@angular/core/testing';

import { AppComponent } from './app.component';

describe('AppComponent - shell principal', () => {
  let component: AppComponent;

  beforeEach(() => {
    component = new AppComponent({ url: '/home' } as any, null);
  });

  it('colapsa y expande el sidebar en escritorio', () => {
    spyOnProperty(window, 'innerWidth').and.returnValue(1280);

    component.alternarSidebar();
    expect(component.sidebarColapsado).toBeTrue();

    component.alternarSidebar();
    expect(component.sidebarColapsado).toBeFalse();
  });

  it('abre el panel lateral sin colapsar su contenido en móvil', () => {
    spyOnProperty(window, 'innerWidth').and.returnValue(760);

    component.alternarSidebar();

    expect(component.sidebarMovilAbierto).toBeTrue();
    expect(component.sidebarColapsado).toBeFalse();
  });

  it('mantiene visible el preloader antes de retirarlo con una transición', fakeAsync(() => {
    component.ngAfterViewInit();

    tick(1199);
    expect(component.preloaderCerrando).toBeFalse();
    expect(component.preloaderVisible).toBeTrue();

    tick(1);
    expect(component.preloaderCerrando).toBeTrue();
    expect(component.preloaderVisible).toBeTrue();

    tick(320);
    expect(component.preloaderVisible).toBeFalse();
  }));
});
