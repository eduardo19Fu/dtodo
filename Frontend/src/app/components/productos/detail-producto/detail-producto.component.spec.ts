import { fakeAsync, tick } from '@angular/core/testing';

import { DetailProductoComponent } from './detail-producto.component';
import { Producto } from 'src/app/models/producto';
import { global } from 'src/app/services/global';

describe('DetailProductoComponent', () => {
  it('usa el backend del entorno y codifica el nombre de la foto', () => {
    const component = new DetailProductoComponent(null, null);
    component.producto = { imagen: 'foto con espacio #1.jpeg' } as Producto;

    expect(component.imagenUrl).toBe(`${global.url}/uploads/img/foto%20con%20espacio%20%231.jpeg`);
  });

  it('no construye una URL si el producto no tiene foto', () => {
    const component = new DetailProductoComponent(null, null);
    component.producto = new Producto();

    expect(component.imagenUrl).toBeNull();
  });

  it('muestra tipo y marca de la entidad devuelta después de subir', () => {
    const component = new DetailProductoComponent(null, null);
    component.producto = {
      tipoProducto: { tipoProducto: 'Calculadora' }, marcaProducto: { marca: 'Ejemplo' }
    } as Producto;

    expect(component.tipoProductoNombre).toBe('Calculadora');
    expect(component.marcaProductoNombre).toBe('Ejemplo');
  });

  it('conserva tipo y marca del DTO del listado', () => {
    const component = new DetailProductoComponent(null, null);
    component.producto = { tipoProducto: 'Calculadora', marcaProducto: 'Ejemplo' } as unknown as Producto;

    expect(component.tipoProductoNombre).toBe('Calculadora');
    expect(component.marcaProductoNombre).toBe('Ejemplo');
  });

  it('emite el cierre después de completar la transición', fakeAsync(() => {
    const component = new DetailProductoComponent(null, null);
    const cerrar = spyOn(component.cerrar, 'emit');
    spyOn(window, 'matchMedia').and.returnValue({ matches: false } as MediaQueryList);

    component.cerrarModal();

    expect(component.cerrando).toBeTrue();
    expect(cerrar).not.toHaveBeenCalled();

    tick(180);
    expect(cerrar).toHaveBeenCalled();
  }));

  it('evita programar más de un cierre', fakeAsync(() => {
    const component = new DetailProductoComponent(null, null);
    const cerrar = spyOn(component.cerrar, 'emit');
    spyOn(window, 'matchMedia').and.returnValue({ matches: false } as MediaQueryList);

    component.cerrarModal();
    component.cerrarModal();
    tick(180);

    expect(cerrar).toHaveBeenCalledTimes(1);
  }));
});
