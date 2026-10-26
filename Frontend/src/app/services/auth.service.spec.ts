import { AuthService } from './auth.service';

describe('AuthService - roles', () => {
  let service: AuthService;

  beforeEach(() => {
    sessionStorage.clear();
    service = new AuthService(null);
  });

  afterEach(() => sessionStorage.clear());

  function iniciarSesionConRoles(roles: string[]): void {
    sessionStorage.setItem('usuario', JSON.stringify({ idUsuario: 1, usuario: 'prueba', roles }));
  }

  it('reconoce a un usuario que tiene únicamente el rol de bodega', () => {
    iniciarSesionConRoles(['ROLE_BODEGA']);

    expect(service.esSoloBodega()).toBeTrue();
    expect(service.hasRole('ROLE_BODEGA')).toBeTrue();
    expect(service.hasRole('ROLE_ADMIN')).toBeFalse();
  });

  it('un administrador con rol de bodega no es un usuario solo de bodega', () => {
    iniciarSesionConRoles(['ROLE_ADMIN', 'ROLE_BODEGA']);

    expect(service.esSoloBodega()).toBeFalse();
  });

  it('otros roles únicos tampoco son solo de bodega', () => {
    iniciarSesionConRoles(['ROLE_COBRADOR']);

    expect(service.esSoloBodega()).toBeFalse();
    expect(service.esSoloCobrador()).toBeTrue();
  });

  it('un vendedor sin otro rol de alcance general está acotado a sus rutas', () => {
    iniciarSesionConRoles(['ROLE_VENDEDOR']);

    expect(service.esVendedor()).toBeTrue();
  });

  it('combinar el rol de vendedor con otro de alcance general lo deja de acotar', () => {
    for (const otro of ['ROLE_ADMIN', 'ROLE_COBRADOR', 'ROLE_INVENTARIO']) {
      service = new AuthService(null);
      sessionStorage.clear();
      iniciarSesionConRoles(['ROLE_VENDEDOR', otro]);

      expect(service.esVendedor()).toBeFalse();
    }
  });

  it('quien no tiene el rol de vendedor no es vendedor', () => {
    iniciarSesionConRoles(['ROLE_COBRADOR']);

    expect(service.esVendedor()).toBeFalse();
  });
});
