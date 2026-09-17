import { ModalCrearProductoComponent } from './modal-crear-producto.component';
import { MarcaProducto } from 'src/app/models/marca-producto';
import { TipoProducto } from 'src/app/models/tipo-producto';

describe('ModalCrearProductoComponent - calculo de precio de venta', () => {
  let component: ModalCrearProductoComponent;

  beforeEach(() => {
    component = new ModalCrearProductoComponent(null, null);
  });

  it('calcula el precio de venta a partir del precio de compra y el porcentaje de ganancia', () => {
    component.nuevoProducto.precioCompra = 15;
    component.nuevoProducto.porcentajeGanancia = 20;

    component.calcularPrecioVenta();

    expect(component.nuevoProducto.precioVenta).toBe(18);
  });

  it('no calcula nada si falta el precio de compra o el porcentaje', () => {
    component.nuevoProducto.precioCompra = null;
    component.nuevoProducto.porcentajeGanancia = 20;

    component.calcularPrecioVenta();

    expect(component.nuevoProducto.precioVenta).toBeUndefined();
  });
});

describe('ModalCrearProductoComponent - confirmar', () => {
  let component: ModalCrearProductoComponent;

  beforeEach(() => {
    component = new ModalCrearProductoComponent(null, null);
    spyOn(component.producto, 'emit');
  });

  it('no emite el producto si faltan datos obligatorios', () => {
    component.nuevoProducto.nombre = 'Producto sin marca';

    component.confirmar();

    expect(component.producto.emit).not.toHaveBeenCalled();
  });

  it('emite el producto con un codigo autogenerado cuando los datos son validos', () => {
    component.nuevoProducto.nombre = 'Producto valido';
    component.nuevoProducto.precioCompra = 10;
    component.nuevoProducto.marcaProducto = new MarcaProducto();
    component.nuevoProducto.tipoProducto = new TipoProducto();

    component.confirmar();

    expect(component.nuevoProducto.codProducto).toBeTruthy();
    expect(component.producto.emit).toHaveBeenCalledWith(component.nuevoProducto);
  });

  it('respeta el codigo si ya fue ingresado manualmente', () => {
    component.nuevoProducto.nombre = 'Producto valido';
    component.nuevoProducto.precioCompra = 10;
    component.nuevoProducto.marcaProducto = new MarcaProducto();
    component.nuevoProducto.tipoProducto = new TipoProducto();
    component.nuevoProducto.codProducto = 'COD-MANUAL';

    component.confirmar();

    expect(component.nuevoProducto.codProducto).toBe('COD-MANUAL');
  });
});
