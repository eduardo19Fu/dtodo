import { of } from 'rxjs';

import { CategoriaNiveles } from 'src/app/dtos/descuento-dto';
import { DescuentosComponent } from './descuentos.component';
import swal from 'sweetalert2';

describe('DescuentosComponent', () => {
  let component: DescuentosComponent;
  let servicio: any;

  function categoria(nombre: any, porcentajes: number[]): CategoriaNiveles {
    return Object.assign(new CategoriaNiveles(), { categoria: nombre, porcentajes });
  }

  beforeEach(() => {
    servicio = {
      getCategorias: jasmine.createSpy('getCategorias').and.returnValue(of([categoria('VOLUMEN', [12, 15, 20])])),
      guardarCategoria: jasmine.createSpy('guardarCategoria')
    };
    component = new DescuentosComponent(servicio);
    spyOn(swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: true } as any));
  });

  it('al iniciar carga las categorías', () => {
    component.ngOnInit();

    expect(component.categorias.length).toBe(1);
  });

  it('agrega un nivel ordenado y rechaza repetidos o fuera de rango', () => {
    component.ngOnInit();
    const volumen = component.categorias[0];

    component.nuevoPorcentaje.VOLUMEN = 18;
    component.agregarNivel(volumen);
    expect(volumen.porcentajes).toEqual([12, 15, 18, 20]);

    component.nuevoPorcentaje.VOLUMEN = 15;
    component.agregarNivel(volumen);
    component.nuevoPorcentaje.VOLUMEN = 0;
    component.agregarNivel(volumen);
    component.nuevoPorcentaje.VOLUMEN = 101;
    component.agregarNivel(volumen);

    expect(volumen.porcentajes).toEqual([12, 15, 18, 20]);
  });

  it('quita un nivel y guarda la categoría', () => {
    servicio.guardarCategoria.and.returnValue(of(categoria('VOLUMEN', [12, 20])));
    component.ngOnInit();
    const volumen = component.categorias[0];

    component.quitarNivel(volumen, 15);
    component.guardarCategoria(volumen);

    expect(servicio.guardarCategoria).toHaveBeenCalledWith('VOLUMEN', [12, 20]);
    expect(volumen.porcentajes).toEqual([12, 20]);
    expect(component.guardandoCategoria).toBeNull();
  });
});
