import { Subject } from 'rxjs';

import { Usuario } from '../../models/usuario';
import { SidebarComponent } from './sidebar.component';

describe('SidebarComponent', () => {
  let component: SidebarComponent;
  let routerEvents: Subject<any>;
  let router: any;
  let authService: any;

  beforeEach(() => {
    routerEvents = new Subject<any>();
    router = {
      url: '/productos/index',
      events: routerEvents,
      navigate: jasmine.createSpy('navigate')
    };
    const usuario = new Usuario();
    usuario.roles = ['ROLE_ADMIN'];
    authService = {
      usuario,
      hasRole: (role: string) => usuario.roles.includes(role),
      logout: jasmine.createSpy('logout')
    };
    component = new SidebarComponent(router, authService);
    component.ngOnInit();
  });

  afterEach(() => component.ngOnDestroy());

  it('abre automáticamente el grupo correspondiente a la ruta activa', () => {
    expect(component.productosAbierto).toBeTrue();
    expect(component.facturasAbierto).toBeFalse();
  });

  it('solicita expandirse antes de abrir un grupo cuando está colapsado', () => {
    component.collapsed = true;
    const expandir = spyOn(component.expandir, 'emit');

    component.alternarMenu('facturas');

    expect(expandir).toHaveBeenCalled();
    expect(component.facturasAbierto).toBeTrue();
  });

  it('filtra los módulos por su nombre y sus opciones hijas', () => {
    component.filtro = 'correlativos';

    expect(component.mostrarGrupo('facturas')).toBeTrue();
    expect(component.mostrarGrupo('productos')).toBeFalse();
  });

  it('cierra la sesión mediante el servicio y navega al login', () => {
    component.cerrarSesion();

    expect(authService.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
