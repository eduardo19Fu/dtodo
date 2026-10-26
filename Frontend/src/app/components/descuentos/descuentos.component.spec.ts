import { of, throwError } from 'rxjs';

import { CategoriaNiveles, ProductoDescuentos } from 'src/app/dtos/descuento-dto';
import { DescuentosComponent } from './descuentos.component';
import swal from 'sweetalert2';

describe('DescuentosComponent', () => {
  let component: DescuentosComponent;
  let servicio: any;

  function categoria(nombre: any, porcentajes: number[]): CategoriaNiveles {
    return Object.assign(new CategoriaNiveles(), { categoria: nombre, porcentajes });
  }

  function producto(extra: Partial<ProductoDescuentos> = {}): ProductoDescuentos {
    return Object.assign(new ProductoDescuentos(), {
      idProducto: 1, codProducto: 'R1', nombre: 'RESMA', precioVenta: 50, precioMayorista: null, descuentos: []
    }, extra);
  }

  beforeEach(() => {
    servicio = {
      getCategorias: jasmine.createSpy('getCategorias').and.returnValue(of([categoria('VOLUMEN', [12, 15, 20])])),
      guardarCategoria: jasmine.createSpy('guardarCategoria'),
      getProductos: jasmine.createSpy('getProductos').and.returnValue(of({
        content: [producto()], number: 0, totalPages: 1, totalElements: 1, first: true, last: true
      })),
      guardarProducto: jasmine.createSpy('guardarProducto')
    };
    component = new DescuentosComponent(servicio);
    spyOn(swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: true } as any));
  });

  it('al iniciar carga las categorías y los productos con descuentos', () => {
    component.ngOnInit();

    expect(component.categorias.length).toBe(1);
    expect(servicio.getProductos).toHaveBeenCalledWith(0, 10, '', true);
    expect(component.productos.length).toBe(1);
    component.ngOnDestroy();
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
    component.ngOnDestroy();
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
    component.ngOnDestroy();
  });

  it('al editar un producto arma las filas de ciento y fardo con lo que ya tiene', () => {
    component.editar(producto({
      precioMayorista: 40,
      descuentos: [{ tipo: 'FARDO', cantidadMinima: 24, porcentaje: 15, activo: true }]
    }));

    expect(component.edicion.precioMayorista).toBe(40);
    expect(component.edicion.fardo.activo).toBeTrue();
    expect(component.edicion.fardo.cantidadMinima).toBe(24);
    expect(component.edicion.ciento.activo).toBeFalse();
    expect(component.edicion.ciento.cantidadMinima).toBe(100);
  });

  it('guarda solo los descuentos activos y manda el precio mayorista vacío como nulo', () => {
    servicio.guardarProducto.and.returnValue(of(producto()));
    component.editar(producto());
    component.edicion.precioMayorista = null;
    component.edicion.ciento.activo = true;
    component.edicion.ciento.cantidadMinima = 100;
    component.edicion.ciento.porcentaje = 12;

    component.guardarProducto();

    const solicitud = servicio.guardarProducto.calls.mostRecent().args[1];
    expect(solicitud.precioMayorista).toBeNull();
    expect(solicitud.descuentos.length).toBe(1);
    expect(solicitud.descuentos[0]).toEqual(jasmine.objectContaining({ tipo: 'CIENTO', cantidadMinima: 100, porcentaje: 12 }));
    expect(component.edicion).toBeNull();
  });

  it('muestra el mensaje del servidor si una regla de negocio rechaza el producto', () => {
    servicio.guardarProducto.and.returnValue(throwError({
      status: 422, error: { message: 'El precio mayorista debe ser menor al precio de venta' }
    }));
    component.editar(producto());
    component.edicion.precioMayorista = 60;

    component.guardarProducto();

    const args = (swal.fire as jasmine.Spy).calls.mostRecent().args;
    expect(args[1]).toContain('menor al precio de venta');
    expect(args[2]).toBe('warning');
    expect(component.guardandoProducto).toBeFalse();
    expect(component.edicion).not.toBeNull();
  });
});
