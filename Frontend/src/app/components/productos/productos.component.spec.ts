import { ProductosComponent } from './productos.component';
import { ProductoDto } from 'src/app/dtos/productoDto';
import { Sucursal } from 'src/app/models/sucursal';

describe('ProductosComponent - edicion en linea de stock', () => {
  let component: ProductosComponent;
  let producto: ProductoDto;

  beforeEach(() => {
    component = new ProductosComponent(null, null, null, null, null);
    producto = new ProductoDto();
    producto.idProducto = 4;
    producto.stock = 15;
  });

  it('al editar precarga el stock actual del producto', () => {
    component.editarStock(producto);

    expect(component.idProductoEditandoStock).toBe(4);
    expect(component.stockEdicion).toBe(15);
  });

  it('al cancelar cierra la edicion sin modificar el producto', () => {
    component.editarStock(producto);

    component.cancelarEdicionStock();

    expect(component.idProductoEditandoStock).toBeNull();
    expect(producto.stock).toBe(15);
  });
});

describe('ProductosComponent - importar inventario a una sucursal vacia', () => {
  let component: ProductosComponent;

  const sucursal = (id: number, nombre: string): Sucursal => {
    const s = new Sucursal();
    s.idSucursal = id;
    s.nombre = nombre;
    return s;
  };

  beforeEach(() => {
    component = new ProductosComponent(null, null, null, null, null);
    component.idSucursalActiva = 2;
    component.sucursalesAdmin = [sucursal(1, 'Sucursal Central'), sucursal(2, 'Sucursal Norte')];
  });

  it('excluye la propia sucursal activa de las opciones de origen', () => {
    expect(component.sucursalesOrigenDisponibles.map(s => s.idSucursal)).toEqual([1]);
  });

  it('no intenta importar si no se selecciono una sucursal de origen', () => {
    component.idSucursalImportar = null;

    component.importarProductos();

    expect(component.importando).toBeFalse();
  });
});
