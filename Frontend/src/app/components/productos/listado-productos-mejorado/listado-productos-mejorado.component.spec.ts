import { ListadoProductosMejoradoComponent } from './listado-productos-mejorado.component';
import { ProductoDto } from 'src/app/dtos/productoDto';

describe('ListadoProductosMejoradoComponent - edicion en linea de stock', () => {
  let component: ListadoProductosMejoradoComponent;
  let producto: ProductoDto;

  beforeEach(() => {
    component = new ListadoProductosMejoradoComponent(null, null, null, null, null);
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
