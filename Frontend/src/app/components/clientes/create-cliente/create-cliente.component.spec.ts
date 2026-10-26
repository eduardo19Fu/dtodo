import { of, throwError } from 'rxjs';

import { Cliente } from 'src/app/models/cliente';
import { CreateClienteComponent } from './create-cliente.component';
import swal from 'sweetalert2';

describe('CreateClienteComponent - cliente mayorista', () => {
  let component: CreateClienteComponent;
  let clienteService: any;
  let router: any;
  let roles: string[];

  beforeEach(() => {
    roles = ['ROLE_ADMIN'];
    clienteService = {
      create: jasmine.createSpy('create').and.returnValue(of({ nombre: 'Libreria Uno' })),
      update: jasmine.createSpy('update').and.returnValue(of({ nombre: 'Libreria Uno' }))
    };
    router = { navigate: jasmine.createSpy('navigate') };
    const auth: any = { hasRole: (rol: string) => roles.includes(rol) };
    component = new CreateClienteComponent(clienteService, null, router, null, auth);
    spyOn(swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: true } as any));
  });

  it('un cliente nuevo es minorista y sin ruta', () => {
    expect(component.cliente.tipoCliente).toBe('MINORISTA');
    expect(component.esMayorista).toBeFalse();
    expect(component.faltaRuta).toBeFalse();
  });

  it('al marcarlo mayorista exige la ruta', () => {
    component.esMayorista = true;

    expect(component.cliente.tipoCliente).toBe('MAYORISTA');
    expect(component.faltaRuta).toBeTrue();

    component.rutas = [{ idRuta: 1, nombre: 'RUTA 1' } as any];
    component.idRutaSeleccionada = 1;

    expect(component.faltaRuta).toBeFalse();
    expect(component.cliente.ruta).toEqual({ idRuta: 1, nombre: 'RUTA 1' });
  });

  it('al volver a minorista se limpian la ruta y la categoría', () => {
    component.esMayorista = true;
    component.cliente.ruta = { idRuta: 1 };
    component.cliente.categoriaDescuento = 'VOLUMEN';

    component.esMayorista = false;

    expect(component.cliente.tipoCliente).toBe('MINORISTA');
    expect(component.cliente.ruta).toBeNull();
    expect(component.cliente.categoriaDescuento).toBeNull();
  });

  it('mantiene la ruta actual del cliente en las opciones aunque esté inactiva', () => {
    component.rutas = [{ idRuta: 1, nombre: 'RUTA 1' } as any];
    component.cliente = Object.assign(new Cliente(), { tipoCliente: 'MAYORISTA', ruta: { idRuta: 7, nombre: 'RUTA 7' } });

    const opciones = component.rutasDisponibles;

    expect(opciones.map(o => o.idRuta)).toEqual([1, 7]);
    expect(opciones[1].nombre).toContain('inactiva');
  });

  it('propone el teléfono como código del cliente', () => {
    component.cliente.telefono = ' 5555-1234 ';

    component.usarTelefonoComoCodigo();

    expect(component.cliente.codigoCliente).toBe('5555-1234');
  });

  it('solo el administrador ve la sección de mayoristas', () => {
    expect(component.puedeAdministrarMayoristas).toBeTrue();

    roles = ['ROLE_COBRADOR'];
    expect(component.puedeAdministrarMayoristas).toBeFalse();
  });

  it('muestra el mensaje del servidor ante una regla de negocio (422)', () => {
    clienteService.create.and.returnValue(throwError({ status: 422, error: { message: 'El código "1" ya pertenece' } }));

    component.create();

    expect((swal.fire as jasmine.Spy).calls.mostRecent().args[1]).toContain('ya pertenece');
    expect(component.guardando).toBeFalse();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('guarda y vuelve al listado', () => {
    component.cliente.nombre = 'Libreria Uno';

    component.create();

    expect(clienteService.create).toHaveBeenCalledWith(component.cliente);
    expect(router.navigate).toHaveBeenCalledWith(['/clientes/index']);
  });
});
