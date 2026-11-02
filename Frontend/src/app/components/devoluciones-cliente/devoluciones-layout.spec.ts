import { CommonModule } from '@angular/common';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { of } from 'rxjs';

import { ClienteDto } from '../../dtos/cliente-dto';
import { PedidoDevolubleDto } from '../../dtos/devolucion-cliente-dto';
import { DevolucionClienteService } from '../../services/devolucion-cliente.service';
import { SucursalService } from '../../services/sucursal.service';
import { DevolucionesClienteComponent } from './devoluciones-cliente.component';

describe('Formulario de devoluciones de clientes', () => {
  let fixture: ComponentFixture<DevolucionesClienteComponent>;
  let root: HTMLElement;

  function pedido(facturado = false): PedidoDevolubleDto {
    return Object.assign(new PedidoDevolubleDto(), {
      idProforma: 7, noProforma: 'P-7', estadoPedido: 'ENTREGADA', idSucursal: 1, sucursal: 'Central', facturado,
      noFactura: facturado ? 1234 : null, notasCredito: [],
      lineas: [{ idDetalle: 1, producto: 'CUADERNO', entregada: 8, devuelta: 2, devolvible: 6, precioUnitario: 10 }]
    });
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CommonModule, FormsModule],
      declarations: [DevolucionesClienteComponent],
      providers: [
        { provide: SucursalService, useValue: { getSucursales: () => of([{ idSucursal: 1, nombre: 'Central' }]) } },
        { provide: DevolucionClienteService, useValue: {
          getListado: () => of({ content: [], number: 0, totalPages: 0, totalElements: 0, first: true, last: true }),
          pedidosDevolubles: () => of([pedido()])
        } }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
    fixture = TestBed.createComponent(DevolucionesClienteComponent);
    fixture.detectChanges();
    root = fixture.nativeElement;
  });

  afterEach(() => fixture.destroy());

  it('al crear una devolución oculta la lista de registradas y la muestra de nuevo al cerrar', () => {
    expect(root.textContent).toContain('Devoluciones registradas');
    expect(root.querySelector('.return-form-page')).toBeNull();

    fixture.componentInstance.abrirFormulario();
    fixture.detectChanges();
    expect(root.querySelector('.return-form-page')).not.toBeNull();
    expect(root.textContent).not.toContain('Devoluciones registradas');
    expect(root.querySelector('.filtros-devoluciones')).toBeNull();

    fixture.componentInstance.cancelarFormulario();
    fixture.detectChanges();
    expect(root.querySelector('.return-form-page')).toBeNull();
    expect(root.textContent).toContain('Devoluciones registradas');
  });

  it('sigue la estructura de los demás formularios de creación: encabezado, banner y secciones numeradas', () => {
    fixture.componentInstance.abrirFormulario();
    fixture.detectChanges();

    expect(root.querySelector('.page-heading h1').textContent).toContain('Registrar nueva devolución');
    expect(root.querySelector('form.product-card .card-hero')).not.toBeNull();
    expect(root.querySelector('.status-pill').textContent).toContain('Nuevo registro');
    const numeros = Array.from(root.querySelectorAll('.section-number')).map(n => n.textContent.trim());
    expect(numeros).toEqual(['01']);
    expect(root.querySelector('.form-section').classList).toContain('last-section');
    expect(root.querySelector('.form-actions .primary-action')).not.toBeNull();
  });

  it('el cliente se busca con la lupa en el directorio y no se escribe en el campo', () => {
    fixture.componentInstance.abrirFormulario();
    fixture.detectChanges();

    const campo = root.querySelector('#clienteDevolucion') as HTMLInputElement;
    expect(campo.readOnly).toBeTrue();
    expect(root.querySelector('.return-search-client .fa-search')).not.toBeNull();
    expect(root.querySelector('.return-remove-client')).toBeNull();
    expect(root.querySelector('app-modal-buscar-cliente')).toBeNull();

    (root.querySelector('.return-search-client') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(root.querySelector('app-modal-buscar-cliente').getAttribute('tipoCliente')).toBe('MAYORISTA');
  });

  it('al elegir cliente y pedido aparecen las secciones de productos y datos', () => {
    fixture.componentInstance.abrirFormulario();
    fixture.componentInstance.elegirCliente(Object.assign(new ClienteDto(), { idCliente: 5, nombre: 'Libreria Uno', nit: '4514627-6' }));
    fixture.componentInstance.elegirPedido(7);
    fixture.detectChanges();

    const numeros = Array.from(root.querySelectorAll('.section-number')).map(n => n.textContent.trim());
    expect(numeros).toEqual(['01', '02', '03']);
    expect((root.querySelector('#clienteDevolucion') as HTMLInputElement).value).toBe('Libreria Uno');
    expect(root.querySelector('.return-remove-client')).not.toBeNull();
    expect(root.querySelectorAll('.form-section.last-section').length).toBe(1);
    expect(root.querySelectorAll('.listing-data-table tbody tr').length).toBe(1);
    expect(root.querySelector('#motivoDevolucion')).not.toBeNull();
    expect(root.querySelector('.form-actions p').textContent).toContain('Indica la cantidad');
  });

  it('un pedido facturado avisa y pide la nota de crédito', () => {
    const componente = fixture.componentInstance;
    componente.abrirFormulario();
    componente.pedidos = [pedido(true)];
    componente.clienteElegido = Object.assign(new ClienteDto(), { idCliente: 5, nombre: 'Libreria Uno' });
    componente.elegirPedido(7);
    fixture.detectChanges();

    expect(root.querySelector('.return-invoice-note').textContent).toContain('1234');
    expect(root.querySelector('#notaDevolucion')).not.toBeNull();
  });
});
