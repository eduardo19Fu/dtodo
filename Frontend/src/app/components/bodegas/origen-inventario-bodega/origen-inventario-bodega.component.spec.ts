import { of, throwError } from 'rxjs';

import { OrigenInventarioBodegaComponent, OrigenInventarioSeleccion } from './origen-inventario-bodega.component';
import swal from 'sweetalert2';

describe('OrigenInventarioBodegaComponent', () => {
  let component: OrigenInventarioBodegaComponent;
  let bodegaService: any;
  let emitido: OrigenInventarioSeleccion;

  function archivoSeleccionado(nombre: string): Event {
    const input: any = { files: [new File(['x'], nombre)], value: 'ruta' };
    return { target: input } as unknown as Event;
  }

  beforeEach(() => {
    bodegaService = { descargarPlantillaImportacion: jasmine.createSpy('descargarPlantillaImportacion') };
    component = new OrigenInventarioBodegaComponent(bodegaService);
    component.cambio.subscribe(seleccion => emitido = seleccion);
  });

  it('por defecto no carga inventario y está completo', () => {
    expect(component.tipo).toBe('NINGUNO');
    expect(component.completa).toBeTrue();
  });

  it('copiar de sucursal o bodega exige elegir el origen', () => {
    component.seleccionarTipo('SUCURSAL');
    expect(component.completa).toBeFalse();
    expect(emitido.completa).toBeFalse();

    component.seleccionarOrigen(3);
    expect(component.completa).toBeTrue();
    expect(emitido.tipo).toBe('SUCURSAL');
    expect(emitido.idOrigen).toBe(3);
  });

  it('importar desde Excel exige elegir un archivo', () => {
    component.seleccionarTipo('EXCEL');

    expect(component.completa).toBeFalse();

    component.alElegirArchivo(archivoSeleccionado('inventario.xlsx'));
    expect(component.completa).toBeTrue();
    expect(emitido.archivo.name).toBe('inventario.xlsx');
  });

  it('rechaza archivos que no son .xlsx', () => {
    const alerta = spyOn(swal, 'fire');
    component.seleccionarTipo('EXCEL');

    component.alElegirArchivo(archivoSeleccionado('inventario.csv'));

    expect(component.archivo).toBeNull();
    expect(component.completa).toBeFalse();
    expect(alerta).toHaveBeenCalled();
  });

  it('acepta la extensión .xlsx sin importar mayúsculas', () => {
    component.seleccionarTipo('EXCEL');

    component.alElegirArchivo(archivoSeleccionado('INVENTARIO.XLSX'));

    expect(component.archivo).not.toBeNull();
  });

  it('cambiar el tipo reinicia el origen y el archivo elegidos', () => {
    component.seleccionarTipo('SUCURSAL');
    component.seleccionarOrigen(3);

    component.seleccionarTipo('EXCEL');

    expect(component.idOrigen).toBeNull();
    expect(component.archivo).toBeNull();
  });

  it('permite quitar el archivo elegido', () => {
    const input: any = { value: 'ruta' };
    component.seleccionarTipo('EXCEL');
    component.alElegirArchivo(archivoSeleccionado('a.xlsx'));

    component.quitarArchivo(input);

    expect(component.archivo).toBeNull();
    expect(input.value).toBe('');
    expect(emitido.completa).toBeFalse();
  });

  it('muestra el tamaño del archivo en KB o MB', () => {
    component.archivo = { size: 2048 } as File;
    expect(component.tamanioArchivo).toBe('2 KB');

    component.archivo = { size: 3 * 1024 * 1024 } as File;
    expect(component.tamanioArchivo).toBe('3.0 MB');

    component.archivo = null;
    expect(component.tamanioArchivo).toBe('');
  });

  it('el tipo inicial se puede definir desde el contenedor', () => {
    component.tipoInicial = 'EXCEL';

    expect(component.tipo).toBe('EXCEL');
  });

  it('descarga la plantilla y la entrega como archivo', () => {
    spyOn(window.URL, 'createObjectURL').and.returnValue('blob:plantilla');
    spyOn(window.URL, 'revokeObjectURL');
    const enlace = document.createElement('a');
    const clic = spyOn(enlace, 'click');
    spyOn(document, 'createElement').and.returnValue(enlace);
    bodegaService.descargarPlantillaImportacion.and.returnValue(of({ body: new Blob(['x']) }));

    component.descargarPlantilla();

    expect(clic).toHaveBeenCalled();
    expect(enlace.download).toBe('plantilla_inventario_bodega.xlsx');
    expect(component.descargandoPlantilla).toBeFalse();
  });

  it('informa si no se pudo descargar la plantilla', () => {
    const alerta = spyOn(swal, 'fire');
    bodegaService.descargarPlantillaImportacion.and.returnValue(throwError({ status: 500 }));

    component.descargarPlantilla();

    expect(component.descargandoPlantilla).toBeFalse();
    expect(alerta).toHaveBeenCalledWith('No se pudo descargar la plantilla', 'Intenta nuevamente.', 'error');
  });

  it('ilustra el formato con las tres columnas esperadas', () => {
    expect(component.columnasEjemplo).toEqual(['A', 'B', 'C']);
    expect(component.filasEjemplo.length).toBeGreaterThan(0);
  });
});
