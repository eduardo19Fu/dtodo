import { CommonModule } from '@angular/common';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { ProductoDescuentos } from 'src/app/dtos/descuento-dto';
import { Producto } from 'src/app/models/producto';
import { AuthService } from 'src/app/services/auth.service';
import { DescuentoService } from 'src/app/services/descuento.service';
import { MarcaProductoService } from 'src/app/services/marca-producto.service';
import { ProductoService } from 'src/app/services/producto.service';
import { SucursalService } from 'src/app/services/sucursal.service';
import { TipoProductoService } from 'src/app/services/tipo-producto.service';
import { CreateProductoComponent } from './create-producto.component';
import swal from 'sweetalert2';

describe('CreateProductoComponent: precio y descuentos mayoristas', () => {
  let fixture: ComponentFixture<CreateProductoComponent>;
  let component: CreateProductoComponent;
  let roles: string[];
  let rutaParams: any;
  let productos: any;
  let descuentos: any;
  let router: any;
  let llamadas: string[];

  function guardado(valores: Partial<ProductoDescuentos> = {}): ProductoDescuentos {
    return Object.assign(new ProductoDescuentos(), {
      idProducto: 5, codProducto: '00065', nombre: 'CODIGO TRIBUTARIO', precioVenta: 100, precioMayorista: 90,
      descuentos: [{ tipo: 'CIENTO', cantidadMinima: 100, porcentaje: 12, activo: true }]
    }, valores);
  }

  async function crear(): Promise<void> {
    fixture = TestBed.createComponent(CreateProductoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function raiz(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  /** Deja el precio de venta en el formulario, como lo calcularía la ganancia, para que el guardado lo lea. */
  async function conPrecioVenta(precio: number): Promise<void> {
    component.producto.precioVenta = precio;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  beforeEach(async () => {
    roles = ['ROLE_ADMIN'];
    rutaParams = {};
    llamadas = [];
    productos = {
      getProducto: jasmine.createSpy('getProducto').and.callFake(() => of(Object.assign(new Producto(), {
        idProducto: 5, nombre: 'CODIGO TRIBUTARIO', precioCompra: 60, porcentajeGanancia: 66.67, precioVenta: 100
      }))),
      create: jasmine.createSpy('create').and.callFake((p: Producto) => {
        llamadas.push('producto.create');
        return of(Object.assign(new Producto(), p, { idProducto: 99, nombre: p.nombre || 'NUEVO' }));
      }),
      update: jasmine.createSpy('update').and.callFake((p: Producto) => {
        llamadas.push('producto.update');
        return of(p);
      })
    };
    descuentos = {
      getProducto: jasmine.createSpy('getProducto').and.callFake(() => of(guardado())),
      guardarProducto: jasmine.createSpy('guardarProducto').and.callFake(() => {
        llamadas.push('descuentos.guardar');
        return of(guardado());
      })
    };
    router = { navigate: jasmine.createSpy('navigate') };
    await TestBed.configureTestingModule({
      imports: [CommonModule, FormsModule],
      declarations: [CreateProductoComponent],
      providers: [
        { provide: MarcaProductoService, useValue: { getMarcas: () => of([]) } },
        { provide: TipoProductoService, useValue: { getTiposProducto: () => of([]) } },
        { provide: ProductoService, useValue: productos },
        { provide: SucursalService, useValue: { getSucursales: () => of([{ idSucursal: 1, nombre: 'Central', esPrincipal: true }]) } },
        { provide: AuthService, useValue: { hasRole: (rol: string) => roles.includes(rol), usuario: null } },
        { provide: DescuentoService, useValue: descuentos },
        { provide: Router, useValue: router },
        { provide: ActivatedRoute, useValue: { params: { subscribe: (fn: any) => fn(rutaParams) } } }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
    spyOn(swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: true } as any));
  });

  afterEach(() => fixture && fixture.destroy());

  it('el administrador ve la sección de precio mayorista y las demás secciones se renumeran', async () => {
    await crear();

    expect(raiz().querySelector('#mayorista-title').textContent).toContain('Precio mayorista y descuentos');
    expect(raiz().querySelector('#precio-mayorista')).not.toBeNull();
    const numeros = Array.from(raiz().querySelectorAll('.section-number')).map(n => n.textContent.trim());
    expect(numeros).toContain('03');
    expect(numeros).toContain('04');
    expect(numeros).toContain('05');
  });

  it('quien no es administrador no ve la sección, no la consulta y conserva la numeración de siempre', async () => {
    roles = ['ROLE_INVENTARIO'];
    rutaParams = { id: '5' };
    await crear();

    expect(raiz().querySelector('#mayorista-title')).toBeNull();
    expect(descuentos.getProducto).not.toHaveBeenCalled();
    const numeros = Array.from(raiz().querySelectorAll('.section-number')).map(n => n.textContent.trim());
    expect(numeros).not.toContain('05');
    expect(numeros).toContain('04');
  });

  it('al editar carga el precio mayorista y los descuentos que ya tiene el producto', async () => {
    rutaParams = { id: '5' };
    await crear();

    expect(descuentos.getProducto).toHaveBeenCalledWith(5);
    expect(component.precioMayorista).toBe(90);
    expect(component.ciento.activo).toBeTrue();
    expect(component.ciento.cantidadMinima).toBe(100);
    expect(component.ciento.porcentaje).toBe(12);
    expect(component.fardo.activo).toBeFalse();
    expect((raiz().querySelector('#regla-ciento') as HTMLInputElement).checked).toBeTrue();
    expect((raiz().querySelector('#ciento-porcentaje') as HTMLInputElement).disabled).toBeFalse();
    expect((raiz().querySelector('#fardo-porcentaje') as HTMLInputElement).disabled).toBeTrue();
    expect(component.hayCambiosMayorista).toBeFalse();
  });

  it('las tres tarjetas van en una fila y los campos de una regla se habilitan solo con su interruptor', async () => {
    await crear();
    const tarjetas = raiz().querySelectorAll('.mayorista-fila > .mayorista-tarjeta');
    expect(tarjetas.length).toBe(3);
    expect(tarjetas[0].querySelector('#precio-mayorista')).not.toBeNull();
    expect(tarjetas[1].querySelector('#regla-ciento')).not.toBeNull();
    expect(tarjetas[2].querySelector('#regla-fardo')).not.toBeNull();

    const fardo = raiz().querySelector('#regla-fardo') as HTMLInputElement;
    const minimo = raiz().querySelector('#fardo-minimo') as HTMLInputElement;
    expect(minimo.disabled).toBeTrue();
    expect(raiz().querySelector('.mayorista-regla.is-on')).toBeNull();

    fardo.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(minimo.disabled).toBeFalse();
    expect(raiz().querySelectorAll('.mayorista-regla.is-on').length).toBe(1);

    fardo.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(minimo.disabled).toBeTrue();
    expect(raiz().querySelector('.mayorista-regla.is-on')).toBeNull();
  });

  it('las leyendas son breves', async () => {
    await crear();

    const leyendas = Array.from(raiz().querySelectorAll('.mayorista-tarjeta-leyenda')).map(l => l.textContent.trim());
    expect(leyendas.length).toBe(3);
    leyendas.forEach(leyenda => expect(leyenda.length).toBeLessThanOrEqual(24));
  });

  it('el diseño usa tres columnas en una fila y el interruptor lleva su propio tamaño', async () => {
    await crear();
    const fila = raiz().querySelector('.mayorista-fila') as HTMLElement;
    raiz().style.display = 'block';
    raiz().style.width = '1180px';

    expect(getComputedStyle(fila).display).toBe('grid');
    // En pantallas anchas (desde 1100 px) las tres tarjetas comparten la fila; en las angostas se apilan.
    expect(getComputedStyle(fila).gridTemplateColumns.split(' ').length).toBe(window.innerWidth >= 1100 ? 3 : 1);
    const interruptor = raiz().querySelector('#regla-ciento') as HTMLElement;
    expect(getComputedStyle(interruptor).width).toBe('46px');
    expect(getComputedStyle(interruptor).appearance).toBe('none');
  });

  it('muestra qué descuento equivale al precio mayorista y avisa si no es menor al de venta', async () => {
    await crear();
    await conPrecioVenta(100);

    component.precioMayorista = 90;
    fixture.detectChanges();
    expect(component.equivalenciaMayorista).toBe(10);
    expect(raiz().querySelector('.mayorista-equivalencia').textContent).toContain('10 %');

    component.precioMayorista = 100;
    fixture.detectChanges();
    expect(component.precioMayoristaInvalido).toBeTrue();
    expect(component.equivalenciaMayorista).toBeNull();
    expect(raiz().querySelector('.mayorista-section .validation-message').textContent).toContain('menor al precio de venta');
  });

  it('valida el precio, la cantidad mínima y el porcentaje antes de guardar', async () => {
    await crear();
    await conPrecioVenta(100);

    component.precioMayorista = 120;
    expect(component.errorMayoristaAlGuardar()).toContain('debe ser menor al precio de venta');
    component.precioMayorista = 0;
    expect(component.errorMayoristaAlGuardar()).toContain('mayor a cero');
    component.precioMayorista = 90;
    expect(component.errorMayoristaAlGuardar()).toBe('');

    component.ciento = { activo: true, cantidadMinima: 1, porcentaje: 10 };
    expect(component.errorMayoristaAlGuardar()).toContain('por ciento');
    expect(component.errorMayoristaAlGuardar()).toContain('2 o más');
    component.ciento = { activo: true, cantidadMinima: 100, porcentaje: 0 };
    expect(component.errorMayoristaAlGuardar()).toContain('entre 0.01 y 100');
    component.ciento = { activo: true, cantidadMinima: 100, porcentaje: 12 };
    component.fardo = { activo: true, cantidadMinima: 2.5, porcentaje: 10 };
    expect(component.errorMayoristaAlGuardar()).toContain('fardo cerrado');
    component.fardo = { activo: true, cantidadMinima: 12, porcentaje: 8 };
    expect(component.errorMayoristaAlGuardar()).toBe('');
  });

  it('un dato mayorista inválido detiene el guardado del producto y lo avisa', async () => {
    await crear();
    await conPrecioVenta(100);
    component.producto.nombre = 'NUEVO';
    component.precioMayorista = 150;

    component.create();

    expect(productos.create).not.toHaveBeenCalled();
    expect(swal.fire).toHaveBeenCalledWith('Revisa el precio y los descuentos mayoristas', jasmine.stringMatching('menor al precio de venta'), 'warning');
  });

  it('al crear guarda primero el producto y después su precio y descuentos', async () => {
    await crear();
    await conPrecioVenta(100);
    component.producto.nombre = 'NUEVO';
    component.precioMayorista = 90;
    component.fardo = { activo: true, cantidadMinima: 12, porcentaje: 8 };

    component.create();

    expect(llamadas).toEqual(['producto.create', 'descuentos.guardar']);
    const [id, solicitud] = descuentos.guardarProducto.calls.mostRecent().args;
    expect(id).toBe(99);
    expect(solicitud.precioMayorista).toBe(90);
    expect(solicitud.descuentos).toEqual([jasmine.objectContaining({ tipo: 'FARDO', cantidadMinima: 12, porcentaje: 8 })]);
    expect(router.navigate).toHaveBeenCalledWith(['/productos/index']);
    expect(swal.fire).toHaveBeenCalledWith('Producto Guardado', jasmine.stringMatching('descuentos mayoristas'), 'success');
  });

  it('al crear sin precio ni descuentos mayoristas no los manda', async () => {
    await crear();
    await conPrecioVenta(100);
    component.producto.nombre = 'NUEVO';

    component.create();

    expect(llamadas).toEqual(['producto.create']);
    expect(descuentos.guardarProducto).not.toHaveBeenCalled();
  });

  it('al actualizar guarda los descuentos solo si cambiaron, y después del producto', async () => {
    rutaParams = { id: '5' };
    await crear();
    await conPrecioVenta(100);

    component.update();
    expect(llamadas).toEqual(['producto.update']);

    llamadas.length = 0;
    component.precioMayorista = 85;
    component.update();
    expect(llamadas).toEqual(['producto.update', 'descuentos.guardar']);
    expect(descuentos.guardarProducto).toHaveBeenCalledWith(5, jasmine.objectContaining({ precioMayorista: 85 }));
  });

  it('quitar el precio mayorista manda nulo para borrarlo', async () => {
    rutaParams = { id: '5' };
    await crear();
    await conPrecioVenta(100);

    component.precioMayorista = null;
    component.ciento.activo = false;
    component.update();

    expect(descuentos.guardarProducto).toHaveBeenCalledWith(5, { precioMayorista: null, descuentos: [] });
  });

  it('si no se pudieron cargar los descuentos, avisa y no los modifica al guardar', async () => {
    descuentos.getProducto.and.returnValue(throwError({ status: 500 }));
    rutaParams = { id: '5' };
    await crear();
    await conPrecioVenta(100);

    expect(component.errorMayorista).toBeTrue();
    expect(raiz().querySelector('.mayorista-aviso')).not.toBeNull();
    expect(raiz().querySelector('#precio-mayorista')).toBeNull();

    component.update();
    expect(llamadas).toEqual(['producto.update']);
    expect(descuentos.guardarProducto).not.toHaveBeenCalled();

    descuentos.getProducto.and.returnValue(of(guardado()));
    (raiz().querySelector('.mayorista-reintentar') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(component.errorMayorista).toBeFalse();
    expect(component.mayoristaDisponible).toBeTrue();
    expect(raiz().querySelector('#precio-mayorista')).not.toBeNull();
  });

  it('si el servidor rechaza los descuentos, el producto queda guardado y se avisa cómo corregirlos', async () => {
    descuentos.guardarProducto.and.returnValue(throwError({ error: { message: 'El precio mayorista debe ser menor.' } }));
    await crear();
    await conPrecioVenta(100);
    component.producto.nombre = 'NUEVO';
    component.precioMayorista = 90;

    component.create();

    expect(productos.create).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/productos/index']);
    expect(swal.fire).toHaveBeenCalledWith('Producto Guardado', jasmine.stringMatching('Mayoristas → Descuentos'), 'warning');
  });
});
