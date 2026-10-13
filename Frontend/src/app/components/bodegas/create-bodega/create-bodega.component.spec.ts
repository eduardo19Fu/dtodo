import { of, throwError } from 'rxjs';

import { CreateBodegaComponent } from './create-bodega.component';
import { Sucursal } from 'src/app/models/sucursal';
import swal from 'sweetalert2';

describe('CreateBodegaComponent', () => {
  let component: CreateBodegaComponent;
  let bodegaService: any;
  let router: any;

  beforeEach(() => {
    bodegaService = {
      create: jasmine.createSpy('create').and.returnValue(of({ idBodega: 10, nombre: 'Nueva' })),
      update: jasmine.createSpy('update').and.returnValue(of({ idBodega: 10, nombre: 'Nueva' })),
      clonarInventario: jasmine.createSpy('clonarInventario').and.returnValue(of({ productosCopiados: 40 })),
      importarExcel: jasmine.createSpy('importarExcel').and.returnValue(of({
        filasLeidas: 2, productosImportados: 2, unidadesImportadas: 9, errores: []
      }))
    };
    router = { navigate: jasmine.createSpy('navigate') };
    component = new CreateBodegaComponent(bodegaService, null, router, null);
    spyOn(swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: true } as any));
  });

  it('inicia como bodega nueva', () => {
    expect(component.esNueva).toBeTrue();
    expect(component.title).toBe('Registrar nueva bodega');
  });

  it('es edición cuando la bodega ya tiene id', () => {
    component.bodega.idBodega = 3;

    expect(component.esNueva).toBeFalse();
  });

  it('compara sucursales por id y trata null y undefined como iguales', () => {
    const a = new Sucursal();
    a.idSucursal = 1;
    const b = new Sucursal();
    b.idSucursal = 1;

    expect(component.compararSucursal(a, b)).toBeTrue();
    expect(component.compararSucursal(null, undefined)).toBeTrue();
    expect(component.compararSucursal(a, null)).toBeFalse();
  });

  it('el estado seleccionado se traduce a uno de los estados admitidos por una bodega', () => {
    component.idEstadoSeleccionado = 2;

    expect(component.bodega.estado.estado).toBe('INACTIVO');
    expect(component.idEstadoSeleccionado).toBe(2);
  });

  it('sin inventario inicial solo registra la bodega y vuelve al listado', () => {
    component.alCambiarOrigen({ tipo: 'NINGUNO', idOrigen: null, archivo: null, completa: true });

    component.create();

    expect(bodegaService.create).toHaveBeenCalled();
    expect(bodegaService.clonarInventario).not.toHaveBeenCalled();
    expect(bodegaService.importarExcel).not.toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/bodegas/index']);
    expect(component.guardando).toBeFalse();
  });

  it('no registra la bodega si el origen del inventario inicial está incompleto', () => {
    component.alCambiarOrigen({ tipo: 'SUCURSAL', idOrigen: null, archivo: null, completa: false });

    component.create();

    expect(bodegaService.create).not.toHaveBeenCalled();
  });

  it('copia el inventario desde la sucursal elegida después de registrar la bodega', () => {
    component.alCambiarOrigen({ tipo: 'SUCURSAL', idOrigen: 1, archivo: null, completa: true });

    component.create();

    expect(bodegaService.clonarInventario).toHaveBeenCalledWith(10, 'SUCURSAL', 1);
  });

  it('copia el inventario desde otra bodega', () => {
    component.alCambiarOrigen({ tipo: 'BODEGA', idOrigen: 4, archivo: null, completa: true });

    component.create();

    expect(bodegaService.clonarInventario).toHaveBeenCalledWith(10, 'BODEGA', 4);
  });

  it('importa el Excel después de registrar la bodega', () => {
    const archivo = new File(['x'], 'inventario.xlsx');
    component.alCambiarOrigen({ tipo: 'EXCEL', idOrigen: null, archivo, completa: true });

    component.create();

    expect(bodegaService.importarExcel).toHaveBeenCalledWith(10, archivo);
    expect(router.navigate).toHaveBeenCalledWith(['/bodegas/index']);
  });

  it('la bodega queda registrada aunque falle la carga del inventario inicial', () => {
    bodegaService.clonarInventario.and.returnValue(throwError({ status: 400 }));
    component.alCambiarOrigen({ tipo: 'SUCURSAL', idOrigen: 1, archivo: null, completa: true });

    component.create();

    expect(router.navigate).toHaveBeenCalledWith(['/bodegas/index']);
    expect(component.guardando).toBeFalse();
  });

  it('avisa cuando el Excel tiene errores pero igual conserva la bodega', () => {
    bodegaService.importarExcel.and.returnValue(of({
      filasLeidas: 3, productosImportados: 0, unidadesImportadas: 0, errores: ['Fila 2: sin código']
    }));
    component.alCambiarOrigen({ tipo: 'EXCEL', idOrigen: null, archivo: new File(['x'], 'a.xlsx'), completa: true });

    component.create();

    expect(router.navigate).toHaveBeenCalledWith(['/bodegas/index']);
    const llamada = (swal.fire as jasmine.Spy).calls.mostRecent().args;
    expect(llamada[1]).toContain('1 fila(s) con errores');
  });

  it('un error al registrar libera el botón de guardar y no navega', () => {
    bodegaService.create.and.returnValue(throwError({ status: 400 }));

    component.create();

    expect(component.guardando).toBeFalse();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('actualiza una bodega existente', () => {
    component.bodega.idBodega = 10;

    component.update();

    expect(bodegaService.update).toHaveBeenCalledWith(component.bodega);
    expect(router.navigate).toHaveBeenCalledWith(['/bodegas/index']);
  });
});
