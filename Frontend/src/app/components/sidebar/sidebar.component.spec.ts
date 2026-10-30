import { NavigationEnd } from '@angular/router';
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

  it('encuentra el tutorial de bodegas al buscar tutorial, manual o ayuda', () => {
    ['tutorial', 'manual', 'ayuda'].forEach(termino => {
      component.filtro = termino;

      expect(component.mostrarGrupo('bodegas')).toBeTrue();
      expect(component.mostrarGrupo('facturas')).toBeFalse();
    });
  });

  it('abre el grupo Mayoristas o Cuentas por cobrar según la ruta activa y deja cerrado el otro', () => {
    router.url = '/autorizaciones/index';
    routerEvents.next(new NavigationEnd(1, '/autorizaciones/index', '/autorizaciones/index'));
    expect(component.mayoristasAbierto).toBeTrue();
    expect(component.cobrosAbierto).toBeFalse();

    routerEvents.next(new NavigationEnd(2, '/devoluciones-cliente/index', '/devoluciones-cliente/index'));
    expect(component.mayoristasAbierto).toBeFalse();
    expect(component.cobrosAbierto).toBeTrue();

    routerEvents.next(new NavigationEnd(3, '/pedidos/index', '/pedidos/index'));
    expect(component.mayoristasAbierto).toBeFalse();
    expect(component.cobrosAbierto).toBeFalse();
  });

  it('abre y cierra los grupos nuevos con alternarMenu', () => {
    component.alternarMenu('mayoristas');
    component.alternarMenu('cobros');
    expect(component.menuAbierto('mayoristas')).toBeTrue();
    expect(component.menuAbierto('cobros')).toBeTrue();

    component.alternarMenu('mayoristas');
    expect(component.menuAbierto('mayoristas')).toBeFalse();
    expect(component.menuAbierto('cobros')).toBeTrue();
  });

  it('el buscador encuentra cada opción dentro de su grupo y no en los demás', () => {
    const casos: Array<[string, string]> = [
      ['indicadores', 'mayoristas'], ['comisiones', 'mayoristas'], ['rutas', 'mayoristas'],
      ['autorizaciones', 'mayoristas'], ['descuentos', 'mayoristas'],
      ['cobranza', 'cobros'], ['recibos', 'cobros'], ['devoluciones de clientes', 'cobros']
    ];
    casos.forEach(([termino, grupo]) => {
      component.filtro = termino;
      expect(component.mostrarGrupo(grupo as any)).withContext(termino).toBeTrue();
      expect(component.menuAbierto(grupo as any)).withContext(termino).toBeTrue();
      expect(component.mostrarGrupo(grupo === 'cobros' ? 'mayoristas' : 'cobros')).withContext(termino).toBeFalse();
    });
  });

  it('buscar por el nombre del grupo muestra el grupo completo', () => {
    component.filtro = 'mayoristas';
    expect(component.mostrarGrupo('mayoristas')).toBeTrue();

    component.filtro = 'cuentas por cobrar';
    expect(component.mostrarGrupo('cobros')).toBeTrue();
    expect(component.mostrarGrupo('mayoristas')).toBeFalse();
  });

  it('cierra la sesión mediante el servicio y navega al login', () => {
    component.cerrarSesion();

    expect(authService.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
