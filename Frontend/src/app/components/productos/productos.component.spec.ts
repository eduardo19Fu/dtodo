import { ProductosComponent } from './productos.component';
import { ProductoDto } from 'src/app/dtos/productoDto';
import { Sucursal } from 'src/app/models/sucursal';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';
import { ProductoService } from 'src/app/services/producto.service';
import { InventarioSucursalService } from 'src/app/services/inventario-sucursal.service';
import { SucursalService } from 'src/app/services/sucursal.service';
import { AuthService } from 'src/app/services/auth.service';
import { global } from 'src/app/services/global';

describe('ProductosComponent - edicion en linea de stock', () => {
  let component: ProductosComponent;
  let producto: ProductoDto;

  beforeEach(() => {
    component = new ProductosComponent(null, null, null, null);
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

describe('ProductosComponent - lista y galería', () => {
  let fixture: ComponentFixture<ProductosComponent>;
  let component: ProductosComponent;
  let producto: ProductoDto;
  let esAdmin: boolean;
  let inventario: { ajustarStock: jasmine.Spy };

  beforeEach(async () => {
    esAdmin = true;
    inventario = { ajustarStock: jasmine.createSpy('ajustarStock').and.returnValue(of({})) };
    await TestBed.configureTestingModule({
      imports: [CommonModule, FormsModule, RouterTestingModule],
      declarations: [ProductosComponent],
      providers: [
        { provide: ProductoService, useValue: {
          getProductosDtoPaginados: () => of({
            content: [producto], number: 0, totalPages: 1, totalElements: 1, size: 5, first: true, last: true
          })
        } },
        { provide: InventarioSucursalService, useValue: inventario },
        { provide: SucursalService, useValue: { getSucursales: () => of([]) } },
        { provide: AuthService, useValue: {
          hasRole: () => esAdmin, usuario: { sucursal: { idSucursal: 2, nombre: 'Central' } }
        } }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
    fixture = TestBed.createComponent(ProductosComponent);
    component = fixture.componentInstance;
    producto = Object.assign(new ProductoDto(), {
      idProducto: 8, nombre: 'Ábaco', codProducto: 'ABC-001', imagen: 'foto #1.jpeg',
      stock: 18, precioCompra: 33, precioVenta: 54.45, tipoProducto: 'Educativos', marcaProducto: 'Beifa', estado: 'ACTIVO'
    });
    component.productosDto = [producto];
    component.totalElementos = 1;
    fixture.detectChanges();
  });

  it('cambia a galería conservando búsqueda, página y orden sin consultar otra vez', () => {
    const cargar = spyOn(component, 'cargarProductos');
    component.filtro = 'ábaco';
    component.paginaActual = 3;
    component.orden = 'stock';
    const buttons = fixture.nativeElement.querySelectorAll('.view-switch button');
    buttons[1].click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.gallery-product')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.productos-table')).toBeNull();
    expect(component.filtro).toBe('ábaco');
    expect(component.paginaActual).toBe(3);
    expect(component.orden).toBe('stock');
    expect(cargar).not.toHaveBeenCalled();
  });

  it('usa la foto del entorno y permite ampliarla desde la miniatura', () => {
    const show = spyOn(component.visorFoto.nativeElement, 'showModal');
    const image = fixture.nativeElement.querySelector('.product-photo img');
    expect(image.getAttribute('src')).toBe(`${global.url}/uploads/img/foto%20%231.jpeg`);
    fixture.nativeElement.querySelector('button.product-photo').click();

    expect(component.productoFoto).toBe(producto);
    expect(show).toHaveBeenCalled();
  });

  it('sustituye una foto rota en ambas vistas y vuelve a intentar cuando cambia el archivo', () => {
    fixture.nativeElement.querySelector('.product-photo img').dispatchEvent(new Event('error'));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.photo-placeholder').textContent).toContain('Sin foto');
    component.cambiarVista('galeria');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.photo-placeholder')).not.toBeNull();
    producto.imagen = 'foto-nueva.jpeg';
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.product-photo img')).not.toBeNull();
  });

  it('mantiene el ajuste de stock y su valor al cambiar de vista', () => {
    component.idSucursalActiva = 2;
    component.editarStock(producto);
    component.stockEdicion = 21;
    component.cambiarVista('galeria');
    fixture.detectChanges();
    fixture.nativeElement.querySelector('[aria-label="Guardar stock"]').click();
    fixture.detectChanges();

    expect(inventario.ajustarStock).toHaveBeenCalledWith(2, 8, 21, null);
    expect(producto.stock).toBe(21);
    expect(component.idProductoEditandoStock).toBeNull();
  });

  it('oculta costo y acciones de administración en ambas vistas para otros roles', () => {
    esAdmin = false;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('.col-precio').length).toBe(2);
    expect(fixture.nativeElement.querySelector('[aria-label="Editar producto"]')).toBeNull();
    component.cambiarVista('galeria');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.gallery-cost')).toBeNull();
    expect(fixture.nativeElement.querySelector('[aria-label="Ajustar stock"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[aria-label="Ver detalle del producto"]')).not.toBeNull();
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
    component = new ProductosComponent(null, null, null, null);
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

describe('ProductosComponent - detalle de producto', () => {
  let component: ProductosComponent;

  beforeEach(() => {
    component = new ProductosComponent(null, null, null, null);
  });

  it('abre y cierra el detalle mediante estado Angular', () => {
    const producto = new ProductoDto();
    producto.idProducto = 8;

    component.abrirModal(producto);
    expect(component.productoSeleccionado).toBe(producto as any);

    component.cerrarDetalleProducto();
    expect(component.productoSeleccionado).toBeNull();
  });
});
