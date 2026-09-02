import { InventarioSucursalComponent } from './inventario-sucursal.component';
import { InventarioSucursal } from 'src/app/models/inventario-sucursal';

describe('InventarioSucursalComponent - edicion en linea', () => {
  let component: InventarioSucursalComponent;
  let fila: InventarioSucursal;

  beforeEach(() => {
    component = new InventarioSucursalComponent(null, null, null);
    fila = new InventarioSucursal();
    fila.idProducto = 7;
    fila.stock = 12;
    fila.stockMinimo = 5;
  });

  it('al editar precarga el stock actual en los campos de edicion', () => {
    component.editar(fila);

    expect(component.idProductoEditando).toBe(7);
    expect(component.stockEdicion).toBe(12);
    expect(component.stockMinimoEdicion).toBe(5);
  });

  it('al cancelar la edicion cierra el formulario sin modificar la fila', () => {
    component.editar(fila);

    component.cancelarEdicion();

    expect(component.idProductoEditando).toBeNull();
    expect(fila.stock).toBe(12);
  });

  it('calcula las paginas visibles centradas en la pagina actual', () => {
    component.totalPaginas = 8;
    component.paginaActual = 4;

    expect(component.paginasVisibles).toEqual([2, 3, 4, 5, 6]);
  });
});
