import { of, throwError } from 'rxjs';

import { CreateDespachoBodegaComponent } from './create-despacho-bodega.component';
import { DetalleDespachoBodega } from 'src/app/models/detalle-despacho-bodega';
import { Producto } from 'src/app/models/producto';
import swal from 'sweetalert2';

describe('CreateDespachoBodegaComponent', () => {
  let component: CreateDespachoBodegaComponent;
  let bodegaService: any;
  let despachoService: any;
  let sucursalService: any;
  let router: any;

  const bodega: any = { idBodega: 1, nombre: 'Principal', ubicacion: 'Zona 1', sucursal: { idSucursal: 2, nombre: 'Norte' } };
  const item = (idProducto: number, stock: number, precio = 10): any => ({
    idProducto, codProducto: 'C-' + idProducto, nombreProducto: 'Producto ' + idProducto, stock, precioCompra: precio
  });

  function detalle(cantidad: number, precio: number, existencia = 100, idProducto = 1): DetalleDespachoBodega {
    const linea = new DetalleDespachoBodega();
    linea.producto = Object.assign(new Producto(), { idProducto, nombre: 'Producto ' + idProducto });
    linea.cantidad = cantidad;
    linea.precioUnitario = precio;
    linea.existenciaBodega = existencia;
    return linea;
  }

  beforeEach(() => {
    bodegaService = {
      getBodegas: jasmine.createSpy('getBodegas').and.returnValue(of([bodega])),
      getInventarioPorCodigo: jasmine.createSpy('getInventarioPorCodigo')
    };
    despachoService = {
      create: jasmine.createSpy('create').and.returnValue(of({ idDespacho: 12 })),
      imprimirComprobante: jasmine.createSpy('imprimirComprobante').and.returnValue(of(undefined))
    };
    sucursalService = {
      getSucursales: jasmine.createSpy('getSucursales').and.returnValue(of([
        { idSucursal: 2, nombre: 'Norte', estado: { estado: 'ACTIVO' } },
        { idSucursal: 3, nombre: 'Sur', estado: { estado: 'ACTIVO' } },
        { idSucursal: 4, nombre: 'Cerrada', estado: { estado: 'INACTIVO' } }
      ]))
    };
    router = { navigate: jasmine.createSpy('navigate') };
    const ruta: any = { snapshot: { queryParamMap: { get: () => null } } };
    component = new CreateDespachoBodegaComponent(bodegaService, despachoService, sucursalService, ruta, router);
    spyOn(swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: false } as any));
  });

  describe('carga inicial', () => {
    it('ofrece solo sucursales activas y bodegas activas', () => {
      component.ngOnInit();

      expect(bodegaService.getBodegas).toHaveBeenCalledWith(true);
      expect(component.sucursales.map(sucursal => sucursal.idSucursal)).toEqual([2, 3]);
    });

    it('preselecciona la bodega recibida por parámetro y su sucursal por defecto', () => {
      const ruta: any = { snapshot: { queryParamMap: { get: () => '1' } } };
      component = new CreateDespachoBodegaComponent(bodegaService, despachoService, sucursalService, ruta, router);

      component.ngOnInit();

      expect(component.despacho.idBodega).toBe(1);
      expect(component.despacho.idSucursalDestino).toBe(2);
    });

    it('ignora una bodega por parámetro que no está activa', () => {
      const ruta: any = { snapshot: { queryParamMap: { get: () => '99' } } };
      component = new CreateDespachoBodegaComponent(bodegaService, despachoService, sucursalService, ruta, router);

      component.ngOnInit();

      expect(component.despacho.idBodega).toBeUndefined();
    });
  });

  describe('bodega y destino', () => {
    beforeEach(() => component.ngOnInit());

    it('al cambiar de bodega el destino por defecto es su sucursal y se limpia el producto elegido', () => {
      component.itemActual = item(1, 5);
      component.cantidadActual = 3;

      component.alCambiarBodega(1);

      expect(component.despacho.idSucursalDestino).toBe(2);
      expect(component.itemActual).toBeNull();
      expect(component.cantidadActual).toBeNull();
    });

    it('una bodega sin sucursal deja el destino sin elegir', () => {
      component.bodegas = [{ idBodega: 7, nombre: 'Suelta', sucursal: null } as any];

      component.alCambiarBodega(7);

      expect(component.despacho.idSucursalDestino).toBeNull();
    });
  });

  describe('búsqueda de producto', () => {
    beforeEach(() => {
      component.ngOnInit();
      component.alCambiarBodega(1);
    });

    it('exige elegir primero la bodega', () => {
      component.despacho.idBodega = null;
      component.codigoBusqueda = 'C-1';

      component.buscarProductoPorCodigo();

      expect(bodegaService.getInventarioPorCodigo).not.toHaveBeenCalled();
    });

    it('selecciona el producto de la bodega encontrado por código', () => {
      bodegaService.getInventarioPorCodigo.and.returnValue(of(item(1, 30)));
      component.codigoBusqueda = ' C-1 ';

      component.buscarProductoPorCodigo();

      expect(bodegaService.getInventarioPorCodigo).toHaveBeenCalledWith(1, 'C-1');
      expect(component.itemActual.idProducto).toBe(1);
      expect(component.disponibleItemActual).toBe(30);
    });

    it('avisa cuando el código no está en el inventario de la bodega', () => {
      bodegaService.getInventarioPorCodigo.and.returnValue(throwError({ status: 404 }));
      component.codigoBusqueda = 'NO';

      component.buscarProductoPorCodigo();

      expect(component.itemActual).toBeNull();
      expect((swal.fire as jasmine.Spy).calls.mostRecent().args[0]).toBe('Producto no encontrado');
      expect(component.buscando).toBeFalse();
    });

    it('exige un código antes de buscar', () => {
      component.codigoBusqueda = ' ';

      component.buscarProductoPorCodigo();

      expect(bodegaService.getInventarioPorCodigo).not.toHaveBeenCalled();
    });

    it('elegir del modal selecciona el producto y lo cierra', () => {
      component.modalProductoVisible = true;

      component.elegirDelModal(item(2, 8));

      expect(component.modalProductoVisible).toBeFalse();
      expect(component.itemActual.idProducto).toBe(2);
    });

    it('no abre el buscador sin bodega', () => {
      component.despacho.idBodega = null;

      component.abrirModalProducto();

      expect(component.modalProductoVisible).toBeFalse();
    });
  });

  describe('agregarLinea', () => {
    beforeEach(() => {
      component.ngOnInit();
      component.alCambiarBodega(1);
    });

    it('no agrega sin producto seleccionado', () => {
      component.cantidadActual = 2;

      component.agregarLinea();

      expect(component.lineas.length).toBe(0);
    });

    it('no agrega cantidades inválidas', () => {
      component.itemActual = item(1, 10);

      [0, -2, 1.5, null].forEach(valor => {
        component.cantidadActual = valor;
        component.agregarLinea();
      });

      expect(component.lineas.length).toBe(0);
    });

    it('no agrega más que la existencia de la bodega', () => {
      component.itemActual = item(1, 10);
      component.cantidadActual = 11;

      component.agregarLinea();

      expect(component.lineas.length).toBe(0);
    });

    it('agrega la línea con costo y existencia de la bodega y limpia la selección', () => {
      component.itemActual = item(1, 10, 12.5);
      component.cantidadActual = 4;

      component.agregarLinea();

      expect(component.lineas.length).toBe(1);
      expect(component.lineas[0].cantidad).toBe(4);
      expect(component.lineas[0].precioUnitario).toBe(12.5);
      expect(component.lineas[0].existenciaBodega).toBe(10);
      expect(component.lineas[0].subTotal).toBe(50);
      expect(component.itemActual).toBeNull();
      expect(component.codigoBusqueda).toBe('');
      expect(component.cantidadActual).toBeNull();
    });

    it('suma a la línea existente del mismo producto sin superar la existencia', () => {
      component.itemActual = item(1, 10);
      component.cantidadActual = 6;
      component.agregarLinea();

      component.itemActual = item(1, 10);
      expect(component.disponibleItemActual).toBe(4);
      component.cantidadActual = 5;
      component.agregarLinea();
      expect(component.lineas[0].cantidad).toBe(6);

      component.cantidadActual = 4;
      component.agregarLinea();
      expect(component.lineas.length).toBe(1);
      expect(component.lineas[0].cantidad).toBe(10);
    });
  });

  describe('totales', () => {
    it('suma unidades y valor de todas las líneas', () => {
      component.lineas = [detalle(5, 25), detalle(2, 10, 100, 2)];

      expect(component.totalUnidades).toBe(7);
      expect(component.totalDespacho).toBe(145);
    });

    it('sin líneas los totales son cero', () => {
      expect(component.totalUnidades).toBe(0);
      expect(component.totalDespacho).toBe(0);
    });
  });

  describe('edición de la cantidad', () => {
    beforeEach(() => {
      component.lineas = [detalle(5, 10, 8)];
      component.abrirEdicionDetalle(0, document.createElement('button'));
    });

    it('abre el diálogo con la cantidad anterior', () => {
      expect(component.edicionDetalleAbierta).toBeTrue();
      expect(component.valorDetalleAnterior).toBe(5);
    });

    it('exige un valor nuevo', () => {
      component.confirmarEdicionDetalle();

      expect(component.errorEdicionDetalle).toBe('Ingresa un valor nuevo.');
      expect(component.edicionDetalleAbierta).toBeTrue();
    });

    it('rechaza cantidades no enteras o menores a uno', () => {
      [0, -1, 2.5].forEach(valor => {
        component.valorDetalleNuevo = valor;
        component.confirmarEdicionDetalle();
        expect(component.edicionDetalleAbierta).toBeTrue();
      });
    });

    it('no permite superar la existencia que tenía la bodega', () => {
      component.valorDetalleNuevo = 9;

      component.confirmarEdicionDetalle();

      expect(component.errorEdicionDetalle).toContain('8 unidades');
      expect(component.lineas[0].cantidad).toBe(5);
    });

    it('aplica la nueva cantidad, recalcula el subtotal y cierra el diálogo', () => {
      component.valorDetalleNuevo = 8;

      component.confirmarEdicionDetalle();

      expect(component.lineas[0].cantidad).toBe(8);
      expect(component.lineas[0].subTotal).toBe(80);
      expect(component.edicionDetalleAbierta).toBeFalse();
    });

    it('cancelar no cambia la línea', () => {
      component.valorDetalleNuevo = 3;

      component.cancelarEdicionDetalle();

      expect(component.lineas[0].cantidad).toBe(5);
      expect(component.edicionDetalleAbierta).toBeFalse();
    });
  });

  describe('quitar líneas', () => {
    it('quita la línea solo si el usuario confirma', async () => {
      const linea = detalle(2, 10);
      component.lineas = [linea];
      (swal.fire as jasmine.Spy).and.returnValue(Promise.resolve({ isConfirmed: true } as any));

      component.eliminarLinea(linea);
      await Promise.resolve();

      expect(component.lineas.length).toBe(0);
    });

    it('conserva la línea si el usuario cancela', async () => {
      const linea = detalle(2, 10);
      component.lineas = [linea];

      component.eliminarLinea(linea);
      await Promise.resolve();

      expect(component.lineas.length).toBe(1);
    });
  });

  describe('crear', () => {
    beforeEach(() => {
      component.despacho.idBodega = 1;
      component.despacho.idSucursalDestino = 2;
      component.despacho.recibidoPor = '  Maria Lopez ';
      component.despacho.observaciones = ' Reposición ';
      component.lineas = [detalle(4, 10, 20, 7), detalle(2, 5, 9, 8)];
    });

    it('envía solo identificadores y cantidades, sin precio ni existencia', () => {
      component.crear();

      expect(despachoService.create).toHaveBeenCalledWith({
        idBodega: 1,
        idSucursalDestino: 2,
        recibidoPor: 'Maria Lopez',
        observaciones: 'Reposición',
        items: [{ idProducto: 7, cantidad: 4 }, { idProducto: 8, cantidad: 2 }]
      });
      expect(router.navigate).toHaveBeenCalledWith(['/despachos-bodega/index']);
      expect(component.guardando).toBeFalse();
    });

    it('ofrece imprimir el comprobante del despacho recién registrado', async () => {
      (swal.fire as jasmine.Spy).and.returnValue(Promise.resolve({ isConfirmed: true } as any));

      component.crear();
      await Promise.resolve();

      expect(despachoService.imprimirComprobante).toHaveBeenCalledWith(12);
    });

    it('no registra un despacho sin productos', () => {
      component.lineas = [];

      component.crear();

      expect(despachoService.create).not.toHaveBeenCalled();
    });

    it('exige bodega, sucursal destino y quién recibe', () => {
      component.despacho.idSucursalDestino = null;
      component.crear();
      component.despacho.idSucursalDestino = 2;
      component.despacho.recibidoPor = '   ';
      component.crear();

      expect(despachoService.create).not.toHaveBeenCalled();
    });

    it('si el servidor rechaza el despacho libera el botón y no navega', () => {
      despachoService.create.and.returnValue(throwError({ status: 400 }));

      component.crear();

      expect(component.guardando).toBeFalse();
      expect(router.navigate).not.toHaveBeenCalled();
    });
  });
});
