import { of, throwError } from 'rxjs';

import { ModalMovimientoBodegaComponent } from './modal-movimiento-bodega.component';
import swal from 'sweetalert2';

describe('ModalMovimientoBodegaComponent', () => {
  let component: ModalMovimientoBodegaComponent;
  let bodegaService: any;
  let productoService: any;

  const item: any = { idProducto: 5, codProducto: 'P-5', nombreProducto: 'Cuaderno', stock: 20, stockMinimo: 4 };

  beforeEach(() => {
    spyOn(swal, 'fire');
    bodegaService = {
      agregarProducto: jasmine.createSpy('agregarProducto').and.returnValue(of({ mensaje: 'ok', stockInicial: 0, stockFinal: 5 })),
      reducirExistencias: jasmine.createSpy('reducirExistencias').and.returnValue(of({ mensaje: 'ok', stockInicial: 20, stockFinal: 15 })),
      eliminarProducto: jasmine.createSpy('eliminarProducto').and.returnValue(of({ mensaje: 'ok', stockInicial: 20, stockFinal: 0 })),
      getInventarioPorCodigo: jasmine.createSpy('getInventarioPorCodigo')
    };
    productoService = { buscarProductosDto: jasmine.createSpy('buscarProductosDto') };
    component = new ModalMovimientoBodegaComponent(bodegaService, productoService);
    component.bodega = { idBodega: 1, nombre: 'Principal' } as any;
  });

  function abrir(modo: 'AGREGAR' | 'REDUCIR' | 'ELIMINAR', fila: any = item): void {
    component.modo = modo;
    component.item = fila;
    component.ngOnInit();
  }

  it('al reducir o eliminar parte del producto y la existencia de la fila elegida', () => {
    abrir('REDUCIR');

    expect(component.producto.nombre).toBe('Cuaderno');
    expect(component.existenciaActual).toBe(20);
  });

  it('muestra títulos y textos según el modo', () => {
    abrir('AGREGAR', null);
    expect(component.titulo).toBe('Agregar producto a la bodega');
    expect(component.textoConfirmar).toBe('Agregar a la bodega');

    abrir('REDUCIR');
    expect(component.titulo).toBe('Reducir existencias');

    abrir('ELIMINAR');
    expect(component.titulo).toBe('Eliminar producto de la bodega');
    expect(component.icono).toBe('fa-trash-alt');
  });

  it('el motivo es obligatorio salvo al agregar', () => {
    abrir('AGREGAR', null);
    expect(component.motivoObligatorio).toBeFalse();

    abrir('REDUCIR');
    expect(component.motivoObligatorio).toBeTrue();
  });

  it('calcula la existencia resultante según el modo', () => {
    abrir('AGREGAR');
    component.cantidad = 5;
    expect(component.existenciaResultante).toBe(25);

    abrir('REDUCIR');
    component.cantidad = 8;
    expect(component.existenciaResultante).toBe(12);

    abrir('ELIMINAR');
    expect(component.existenciaResultante).toBe(0);
  });

  it('la existencia resultante es nula mientras no se conoce la existencia actual', () => {
    component.modo = 'AGREGAR';
    component.existenciaActual = null;

    expect(component.existenciaResultante).toBeNull();
  });

  it('no permite reducir más de lo que hay en la bodega', () => {
    abrir('REDUCIR');
    component.cantidad = 21;

    expect(component.cantidadValida).toBeFalse();
    expect(component.errorCantidad).toContain('20 unidades');

    component.cantidad = 20;
    expect(component.cantidadValida).toBeTrue();
    expect(component.errorCantidad).toBe('');
  });

  it('rechaza cantidades no enteras, cero o negativas', () => {
    abrir('AGREGAR');

    [0, -1, 2.5].forEach(valor => {
      component.cantidad = valor;
      expect(component.cantidadValida).toBeFalse();
    });
    component.cantidad = 3;
    expect(component.cantidadValida).toBeTrue();
  });

  it('no muestra error de cantidad mientras el campo está vacío', () => {
    abrir('AGREGAR');
    component.cantidad = null;

    expect(component.errorCantidad).toBe('');
  });

  it('para guardar al reducir exige cantidad válida y motivo', () => {
    abrir('REDUCIR');
    component.cantidad = 5;
    expect(component.puedeGuardar).toBeFalse();

    component.motivo = '  ';
    expect(component.puedeGuardar).toBeFalse();

    component.motivo = 'Merma';
    expect(component.puedeGuardar).toBeTrue();
  });

  it('para guardar al agregar no exige motivo pero sí producto y cantidad', () => {
    abrir('AGREGAR', null);
    component.cantidad = 5;
    expect(component.puedeGuardar).toBeFalse();

    component.producto = { idProducto: 5, codProducto: 'P-5', nombre: 'Cuaderno' };
    expect(component.puedeGuardar).toBeTrue();

    component.stockMinimo = -3;
    expect(component.puedeGuardar).toBeFalse();
  });

  it('para eliminar solo exige el motivo', () => {
    abrir('ELIMINAR');
    expect(component.puedeGuardar).toBeFalse();

    component.motivo = 'Ya no se maneja';
    expect(component.puedeGuardar).toBeTrue();
  });

  it('agrega el producto enviando cantidad, stock mínimo y motivo, y avisa que se completó', () => {
    let completado = false;
    component.completado.subscribe(() => completado = true);
    abrir('AGREGAR');
    component.cantidad = 5;
    component.stockMinimo = 3;
    component.motivo = ' Compra ';

    component.guardar();

    expect(bodegaService.agregarProducto).toHaveBeenCalledWith(1, {
      idProducto: 5, cantidad: 5, stockMinimo: 3, motivo: 'Compra'
    });
    expect(completado).toBeTrue();
    expect(component.guardando).toBeFalse();
  });

  it('al agregar sin stock mínimo no lo envía', () => {
    abrir('AGREGAR');
    component.cantidad = 1;
    component.stockMinimo = null;

    component.guardar();

    expect(bodegaService.agregarProducto.calls.mostRecent().args[1].stockMinimo).toBeUndefined();
  });

  it('reduce existencias con su motivo', () => {
    abrir('REDUCIR');
    component.cantidad = 5;
    component.motivo = 'Merma';

    component.guardar();

    expect(bodegaService.reducirExistencias).toHaveBeenCalledWith(1, jasmine.objectContaining({
      idProducto: 5, cantidad: 5, motivo: 'Merma'
    }));
  });

  it('elimina el producto con su motivo', () => {
    abrir('ELIMINAR');
    component.motivo = 'Ya no se maneja';

    component.guardar();

    expect(bodegaService.eliminarProducto).toHaveBeenCalledWith(1, 5, 'Ya no se maneja');
  });

  it('no guarda si el formulario no es válido', () => {
    abrir('REDUCIR');
    component.cantidad = 5;
    component.motivo = '';

    component.guardar();

    expect(bodegaService.reducirExistencias).not.toHaveBeenCalled();
  });

  it('si el servidor rechaza el movimiento libera el botón y no cierra el modal', () => {
    let completado = false;
    component.completado.subscribe(() => completado = true);
    bodegaService.reducirExistencias.and.returnValue(throwError({ status: 400 }));
    abrir('REDUCIR');
    component.cantidad = 5;
    component.motivo = 'Merma';

    component.guardar();

    expect(component.guardando).toBeFalse();
    expect(completado).toBeFalse();
  });

  it('busca por código exacto y selecciona el producto, consultando su existencia en la bodega', () => {
    productoService.buscarProductosDto.and.returnValue(of({
      content: [
        { idProducto: 5, codProducto: 'P-5', nombre: 'Cuaderno' },
        { idProducto: 6, codProducto: 'P-55', nombre: 'Otro' }
      ]
    }));
    bodegaService.getInventarioPorCodigo.and.returnValue(of({ stock: 12, stockMinimo: 2 }));
    abrir('AGREGAR', null);
    component.codigoBusqueda = ' p-5 ';

    component.buscarPorCodigo();

    expect(component.producto.idProducto).toBe(5);
    expect(component.existenciaActual).toBe(12);
    expect(component.stockMinimo).toBe(2);
  });

  it('si el producto aún no está en la bodega su existencia actual es cero', () => {
    productoService.buscarProductosDto.and.returnValue(of({ content: [{ idProducto: 5, codProducto: 'P-5', nombre: 'Cuaderno' }] }));
    bodegaService.getInventarioPorCodigo.and.returnValue(throwError({ status: 404 }));
    abrir('AGREGAR', null);
    component.codigoBusqueda = 'P-5';

    component.buscarPorCodigo();

    expect(component.existenciaActual).toBe(0);
  });

  it('avisa cuando el código no existe y no selecciona nada', () => {
    productoService.buscarProductosDto.and.returnValue(of({ content: [{ idProducto: 6, codProducto: 'P-55' }] }));
    abrir('AGREGAR', null);
    component.codigoBusqueda = 'P-5';

    component.buscarPorCodigo();

    expect(component.producto).toBeNull();
    expect((swal.fire as jasmine.Spy).calls.mostRecent().args[0]).toBe('Producto no encontrado');
  });

  it('avisa cuando varios productos comparten el código exacto', () => {
    productoService.buscarProductosDto.and.returnValue(of({
      content: [{ idProducto: 5, codProducto: 'DUP' }, { idProducto: 6, codProducto: 'DUP' }]
    }));
    abrir('AGREGAR', null);
    component.codigoBusqueda = 'DUP';

    component.buscarPorCodigo();

    expect(component.producto).toBeNull();
    expect((swal.fire as jasmine.Spy).calls.mostRecent().args[0]).toBe('Código repetido');
  });

  it('exige un código antes de buscar', () => {
    abrir('AGREGAR', null);
    component.codigoBusqueda = '  ';

    component.buscarPorCodigo();

    expect(productoService.buscarProductosDto).not.toHaveBeenCalled();
  });

  it('permite cambiar el producto elegido', () => {
    abrir('AGREGAR');

    component.quitarProducto();

    expect(component.producto).toBeNull();
    expect(component.existenciaActual).toBeNull();
  });

  it('elegir del catálogo cierra el buscador y selecciona el producto', () => {
    bodegaService.getInventarioPorCodigo.and.returnValue(of({ stock: 1, stockMinimo: null }));
    abrir('AGREGAR', null);
    component.abrirCatalogo();

    component.elegirDelCatalogo({ idProducto: 8, codProducto: 'X-8', nombre: 'Regla' });

    expect(component.catalogoVisible).toBeFalse();
    expect(component.producto.idProducto).toBe(8);
  });

  it('no se puede cerrar mientras se guarda', () => {
    let cerrado = false;
    component.cerrar.subscribe(() => cerrado = true);
    component.guardando = true;

    component.cerrarModal();
    expect(cerrado).toBeFalse();

    component.guardando = false;
    component.cerrarModal();
    expect(cerrado).toBeTrue();
  });
});
