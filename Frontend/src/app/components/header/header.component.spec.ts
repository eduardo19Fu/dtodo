import { HeaderComponent } from './header.component';

describe('HeaderComponent', () => {
  it('sincroniza el icono con el estado de pantalla completa', () => {
    const component = new HeaderComponent();
    spyOnProperty(document, 'fullscreenElement').and.returnValue(document.documentElement);

    component.sincronizarPantallaCompleta();

    expect(component.pantallaCompleta).toBeTrue();
  });

  it('emite la solicitud para alternar el sidebar', () => {
    const component = new HeaderComponent();
    const toggle = spyOn(component.toggleSidebar, 'emit');

    component.toggleSidebar.emit();

    expect(toggle).toHaveBeenCalled();
  });
});
