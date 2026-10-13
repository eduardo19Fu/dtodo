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
});
