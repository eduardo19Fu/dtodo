import { of, throwError } from 'rxjs';

import { Usuario } from 'src/app/models/usuario';
import { CreateRutaComponent } from './create-ruta.component';
import swal from 'sweetalert2';

describe('CreateRutaComponent', () => {
  let rutaService: any;
  let router: any;
  let component: CreateRutaComponent;

  beforeEach(() => {
    rutaService = {
      create: jasmine.createSpy('create').and.returnValue(of({ idRuta: 1, nombre: 'RUTA 1' })),
      update: jasmine.createSpy('update').and.returnValue(of({ idRuta: 5, nombre: 'RUTA 5' })),
      getRuta: jasmine.createSpy('getRuta')
    };
    router = { navigate: jasmine.createSpy('navigate') };
    component = new CreateRutaComponent(rutaService, null, null, router, null);
    spyOn(swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: true } as any));
  });

  function usuario(roles: any[], enabled = true): Usuario {
    const u = new Usuario();
    u.roles = roles;
    u.enabled = enabled;
    return u;
  }

  it('solo ofrece como vendedores a los usuarios habilitados con el rol de vendedor', () => {
    expect(CreateRutaComponent.esVendedorHabilitado(usuario([{ role: 'ROLE_VENDEDOR' }]))).toBeTrue();
    expect(CreateRutaComponent.esVendedorHabilitado(usuario(['ROLE_VENDEDOR'] as any))).toBeTrue();
    expect(CreateRutaComponent.esVendedorHabilitado(usuario([{ role: 'ROLE_COBRADOR' }]))).toBeFalse();
    expect(CreateRutaComponent.esVendedorHabilitado(usuario([{ role: 'ROLE_VENDEDOR' }], false))).toBeFalse();
    expect(CreateRutaComponent.esVendedorHabilitado(usuario(undefined))).toBeFalse();
  });

  it('guarda las referencias de sucursal, vendedor y estado solo con su id', () => {
    component.idSucursalSeleccionada = 3;
    component.idVendedorSeleccionado = 9;
    component.idEstadoSeleccionado = 2;

    expect(component.ruta.sucursal).toEqual({ idSucursal: 3 });
    expect(component.ruta.vendedor).toEqual({ idUsuario: 9 });
    expect(component.ruta.estado.estado).toBe('INACTIVO');

    component.idVendedorSeleccionado = null;
    expect(component.ruta.vendedor).toBeNull();
  });

  it('crea la ruta nueva, vuelve al listado y avisa', () => {
    component.ruta.nombre = 'RUTA 1';

    component.guardar();

    expect(rutaService.create).toHaveBeenCalledWith(component.ruta);
    expect(router.navigate).toHaveBeenCalledWith(['/rutas/index']);
    expect(component.guardando).toBeFalse();
    expect((swal.fire as jasmine.Spy).calls.mostRecent().args[0]).toBe('Ruta Guardada');
  });

  it('actualiza una ruta existente por su id', () => {
    component.idRuta = 5;

    component.guardar();

    expect(rutaService.update).toHaveBeenCalledWith(5, component.ruta);
    expect(rutaService.create).not.toHaveBeenCalled();
    expect((swal.fire as jasmine.Spy).calls.mostRecent().args[0]).toBe('Ruta Actualizada');
  });

  it('muestra el mensaje del servidor cuando la regla de negocio rechaza la ruta', () => {
    rutaService.create.and.returnValue(throwError({ status: 422, error: { message: 'Ya existe una ruta llamada "RUTA 1"' } }));

    component.guardar();

    const args = (swal.fire as jasmine.Spy).calls.mostRecent().args;
    expect(args[1]).toContain('RUTA 1');
    expect(args[2]).toBe('warning');
    expect(component.guardando).toBeFalse();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('no envía dos veces la misma ruta mientras guarda', () => {
    component.guardando = true;

    component.guardar();

    expect(rutaService.create).not.toHaveBeenCalled();
  });
});
