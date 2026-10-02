import { of, throwError } from 'rxjs';

import { ModalImportarInventarioComponent } from './modal-importar-inventario.component';
import swal from 'sweetalert2';

describe('ModalImportarInventarioComponent', () => {
  let component: ModalImportarInventarioComponent;
  let bodegaService: any;
  let sucursalService: any;
  let completado: boolean;

  beforeEach(() => {
    spyOn(swal, 'fire');
    bodegaService = {
      getBodegas: jasmine.createSpy('getBodegas').and.returnValue(of([{ idBodega: 1 }, { idBodega: 2 }])),
      clonarInventario: jasmine.createSpy('clonarInventario').and.returnValue(of({ productosCopiados: 30 })),
      importarExcel: jasmine.createSpy('importarExcel').and.returnValue(of({
        filasLeidas: 2, productosImportados: 2, unidadesImportadas: 9, errores: []
      }))
    };
    sucursalService = { getSucursales: jasmine.createSpy('getSucursales').and.returnValue(of([{ idSucursal: 1 }])) };
    component = new ModalImportarInventarioComponent(bodegaService, sucursalService);
    component.bodega = { idBodega: 1, nombre: 'Principal' } as any;
    completado = false;
    component.completado.subscribe(() => completado = true);
  });

  it('en una bodega vacía ofrece copiar y carga sucursales y las otras bodegas', () => {
    component.inventarioVacio = true;

    component.ngOnInit();

    expect(component.tipoInicial).toBe('SUCURSAL');
    expect(component.sucursales.length).toBe(1);
    expect(component.bodegasOrigen.map(bodega => bodega.idBodega)).toEqual([2]);
  });

  it('en una bodega con inventario solo ofrece el Excel y no consulta orígenes', () => {
    component.inventarioVacio = false;

    component.ngOnInit();

    expect(component.tipoInicial).toBe('EXCEL');
    expect(sucursalService.getSucursales).not.toHaveBeenCalled();
    expect(bodegaService.getBodegas).not.toHaveBeenCalled();
  });

  it('no importa mientras la selección esté incompleta', () => {
    component.alCambiarOrigen({ tipo: 'EXCEL', idOrigen: null, archivo: null, completa: false });

    component.importar();

    expect(bodegaService.importarExcel).not.toHaveBeenCalled();
    expect(bodegaService.clonarInventario).not.toHaveBeenCalled();
  });

  it('importa el Excel elegido y avisa que se completó cuando no hay errores', () => {
    const archivo = new File(['x'], 'inventario.xlsx');
    component.alCambiarOrigen({ tipo: 'EXCEL', idOrigen: null, archivo, completa: true });

    component.importar();

    expect(bodegaService.importarExcel).toHaveBeenCalledWith(1, archivo);
    expect(completado).toBeTrue();
    expect(component.importando).toBeFalse();
    expect(component.errores).toEqual([]);
  });

  it('si el Excel tiene errores los muestra y no se completa la importación', () => {
    bodegaService.importarExcel.and.returnValue(of({
      filasLeidas: 5, productosImportados: 0, unidadesImportadas: 0, errores: ['Fila 3: no existe', 'Fila 4: repetido']
    }));
    component.alCambiarOrigen({ tipo: 'EXCEL', idOrigen: null, archivo: new File(['x'], 'a.xlsx'), completa: true });

    component.importar();

    expect(component.errores.length).toBe(2);
    expect(component.filasLeidas).toBe(5);
    expect(completado).toBeFalse();
    expect(component.importando).toBeFalse();
  });

  it('cambiar el origen limpia los errores anteriores', () => {
    component.errores = ['Fila 3: no existe'];

    component.alCambiarOrigen({ tipo: 'EXCEL', idOrigen: null, archivo: null, completa: false });

    expect(component.errores).toEqual([]);
  });

  it('copia el inventario desde la sucursal elegida', () => {
    component.alCambiarOrigen({ tipo: 'SUCURSAL', idOrigen: 3, archivo: null, completa: true });

    component.importar();

    expect(bodegaService.clonarInventario).toHaveBeenCalledWith(1, 'SUCURSAL', 3);
    expect(completado).toBeTrue();
  });

  it('un error del servidor libera el botón sin completar', () => {
    bodegaService.clonarInventario.and.returnValue(throwError({ status: 400 }));
    component.alCambiarOrigen({ tipo: 'BODEGA', idOrigen: 2, archivo: null, completa: true });

    component.importar();

    expect(component.importando).toBeFalse();
    expect(completado).toBeFalse();
  });

  it('no se puede cerrar mientras importa', () => {
    let cerrado = false;
    component.cerrar.subscribe(() => cerrado = true);
    component.importando = true;

    component.cerrarModal();
    expect(cerrado).toBeFalse();

    component.importando = false;
    component.cerrarModal();
    expect(cerrado).toBeTrue();
  });
});
