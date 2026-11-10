import { CommonModule } from '@angular/common';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { of } from 'rxjs';

import { AuthService } from '../../services/auth.service';
import { ClienteService } from '../../services/cliente.service';
import { ReciboCajaService } from '../../services/recibo-caja.service';
import { SaldoService } from '../../services/saldo.service';
import { SucursalService } from '../../services/sucursal.service';
import { DateRangePickerComponent } from '../shared/date-range-picker/date-range-picker.component';
import { ModalBuscarClienteComponent } from '../facturas/create-factura/modal-buscar-cliente/modal-buscar-cliente.component';
import { RecibosComponent } from './recibos.component';

describe('Selector de sucursal de recibos', () => {
  let fixture: ComponentFixture<RecibosComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CommonModule, FormsModule],
      declarations: [RecibosComponent, DateRangePickerComponent, ModalBuscarClienteComponent],
      providers: [
        { provide: AuthService, useValue: { hasRole: () => true, usuario: { primerNombre: 'Ramiro', apellido: 'Fuentes' } } },
        { provide: ClienteService, useValue: { getListado: () => of({
          content: [{ idCliente: 5, nombre: 'Librería Uno', nit: '4514627-6' }],
          number: 0, totalPages: 1, totalElements: 1, size: 5, first: true, last: true
        }) } },
        { provide: SaldoService, useValue: { cliente: jasmine.createSpy('saldoCliente').and.returnValue(of({
          entregado: 100, devuelto: 0, abonado: 60, saldo: 40, pedidos: []
        })) } },
        { provide: SucursalService, useValue: { getSucursales: () => of([{ idSucursal: 1, nombre: 'Sucursal Central' }]) } },
        { provide: ReciboCajaService, useValue: { getListado: () => of({
          content: [], number: 0, totalPages: 0, totalElements: 0, first: true, last: true
        }) } }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
    fixture = TestBed.createComponent(RecibosComponent);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => fixture.destroy());

  it('al crear un recibo oculta la lista de recibos emitidos y la vuelve a mostrar al cerrar', () => {
    const root: HTMLElement = fixture.nativeElement;
    expect(root.querySelector('.recibos-filters')).not.toBeNull();

    fixture.componentInstance.abrirFormulario();
    fixture.detectChanges();
    expect(root.querySelector('#clienteRecibo')).not.toBeNull();
    expect(root.querySelector('.recibos-filters')).toBeNull();
    expect(root.textContent).not.toContain('Recibos emitidos');

    fixture.componentInstance.cancelarFormulario();
    fixture.detectChanges();
    expect(root.querySelector('.recibos-filters')).not.toBeNull();
  });

  it('el campo de cliente se presenta como búsqueda y la forma de pago ofrece Cheque', () => {
    const root: HTMLElement = fixture.nativeElement;
    fixture.componentInstance.abrirFormulario();
    fixture.detectChanges();

    expect(root.querySelector('.cliente-buscador .fa-search')).not.toBeNull();
    expect(root.querySelector('label[for="clienteRecibo"]').textContent).toContain('Buscar cliente');
    const formas = Array.from(root.querySelectorAll('#formaRecibo option')).map(o => o.textContent.trim());
    expect(formas).toEqual(['Efectivo', 'Depósito', 'Transferencia', 'Cheque']);

    fixture.componentInstance.formaPago = 'CHEQUE';
    fixture.detectChanges();
    // El cheque no usa la referencia: pide banco, número y fecha del cheque.
    expect(root.querySelector('#referenciaRecibo')).toBeNull();
    expect(root.querySelector('#bancoCheque')).not.toBeNull();
    expect(root.querySelector('#numeroCheque')).not.toBeNull();
    expect(root.querySelector('#giradorCheque')).not.toBeNull();
  });

  it('presenta el usuario y la búsqueda como Proformas y Enter abre el directorio sin emitir', () => {
    const root: HTMLElement = fixture.nativeElement;
    fixture.componentInstance.abrirFormulario();
    fixture.detectChanges();
    const user: HTMLInputElement = root.querySelector('#usuarioRecibo');
    expect(user.value).toBe('Ramiro Fuentes');
    expect(user.disabled).toBeTrue();
    expect(getComputedStyle(user).backgroundColor).toBe('rgb(255, 255, 255)');
    const button: HTMLButtonElement = root.querySelector('.receipt-search-client');
    expect(button.getBoundingClientRect().width).toBe(46);
    expect(button.getBoundingClientRect().height).toBe(46);
    expect(button.getAttribute('aria-label')).toBe('Buscar cliente');
    spyOn(fixture.componentInstance, 'emitir');
    root.querySelector('#clienteRecibo').dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', cancelable: true }));
    fixture.detectChanges();
    expect(root.querySelector('app-modal-buscar-cliente')).not.toBeNull();
    expect(fixture.componentInstance.emitir).not.toHaveBeenCalled();
  });

  it('reutiliza el modal de clientes y carga el saldo al seleccionar un cliente', () => {
    const root: HTMLElement = fixture.nativeElement;
    fixture.componentInstance.abrirFormulario();
    fixture.detectChanges();
    (root.querySelector('.receipt-search-client') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(root.querySelector('app-modal-buscar-cliente [role="dialog"]')).not.toBeNull();
    expect(root.querySelector('app-modal-buscar-cliente').textContent).toContain('Librería Uno');
    (root.querySelector('app-modal-buscar-cliente .choose-product-button') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(root.querySelector('app-modal-buscar-cliente')).toBeNull();
    expect((root.querySelector('#clienteRecibo') as HTMLInputElement).value).toBe('Librería Uno');
    expect(TestBed.inject(SaldoService).cliente).toHaveBeenCalledWith(5);
    expect(root.querySelector('#pedidoRecibo')).not.toBeNull();
    expect(root.querySelector('.receipt-balance').textContent).toContain('Saldo pendiente');
    expect(document.activeElement).toBe(root.querySelector('.receipt-search-client'));
  });

  it('cerrar la búsqueda no modifica el cliente ni el pago del recibo', () => {
    const root: HTMLElement = fixture.nativeElement;
    fixture.componentInstance.abrirFormulario();
    fixture.componentInstance.monto = 125;
    fixture.componentInstance.formaPago = 'CHEQUE';
    fixture.componentInstance.referencia = 'CH-25';
    fixture.detectChanges();
    (root.querySelector('.receipt-search-client') as HTMLButtonElement).click();
    fixture.detectChanges();
    (root.querySelector('app-modal-buscar-cliente .close-modal') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(root.querySelector('app-modal-buscar-cliente')).toBeNull();
    expect(fixture.componentInstance.clienteElegido).toBeNull();
    expect(fixture.componentInstance.monto).toBe(125);
    expect(fixture.componentInstance.formaPago).toBe('CHEQUE');
    expect(fixture.componentInstance.referencia).toBe('CH-25');
  });

  [1040, 640, 320].forEach(width => {
    it(`mantiene los campos y el botón de cliente dentro del formulario a ${width}px`, () => {
      const root: HTMLElement = fixture.nativeElement;
      fixture.componentInstance.abrirFormulario();
      fixture.detectChanges();
      const page: HTMLElement = root.querySelector('.receipt-form-page');
      page.style.width = `${width}px`;
      const picker: HTMLElement = page.querySelector('.receipt-client-picker');
      const input: HTMLInputElement = picker.querySelector('input');
      const button: HTMLButtonElement = picker.querySelector('.receipt-search-client');

      expect(picker.scrollWidth).toBeLessThanOrEqual(picker.clientWidth);
      expect(input.getBoundingClientRect().height).toBe(46);
      expect(button.getBoundingClientRect().right).toBeLessThanOrEqual(picker.getBoundingClientRect().right);
      expect(getComputedStyle(page.querySelector('.card-hero')).backgroundImage).toContain('linear-gradient');
      expect(page.querySelectorAll('.section-number').length).toBe(2);
    });
  });

  it('ocupa toda la caja del campo y permite mostrar Todas las sucursales sin recortarlo', () => {
    const root: HTMLElement = fixture.nativeElement;
    const filters: HTMLElement = root.querySelector('.recibos-filters');
    filters.style.width = '680px';
    const select: HTMLSelectElement = root.querySelector('#sucursalFiltro');
    const field = select.parentElement.getBoundingClientRect();
    const bounds = select.getBoundingClientRect();
    const style = getComputedStyle(select);
    const canvas = document.createElement('canvas').getContext('2d');
    canvas.font = style.font;
    const requiredWidth = canvas.measureText(select.selectedOptions[0].text).width
      + parseFloat(style.paddingLeft) + parseFloat(style.paddingRight);

    expect(bounds.left).toBe(field.left);
    expect(bounds.width).toBe(field.width);
    expect(bounds.height).toBe(46);
    expect(select.clientWidth).toBeGreaterThan(requiredWidth);
    select.style.transition = 'none';
    select.focus();
    expect(document.activeElement).toBe(select);
    expect(getComputedStyle(select).borderColor).toBe('rgb(47, 125, 107)');
  });

  [1240, 760, 360].forEach(width => {
    it(`ajusta los filtros del listado al panel de ${width}px sin recortes`, () => {
      const root: HTMLElement = fixture.nativeElement;
      const body: HTMLElement = root.querySelector('.receipt-list > .card-body');
      body.style.width = `${width}px`;
      const filters: HTMLElement = body.querySelector('.recibos-filters');
      const dates: HTMLElement = filters.querySelector('.receipt-date-filters');
      const branch: HTMLSelectElement = filters.querySelector('#sucursalFiltro');
      const status: HTMLElement = filters.querySelector('.receipt-canceled-toggle');
      const actions: HTMLElement = filters.querySelector('.receipt-filter-actions');

      expect(filters.scrollWidth).toBeLessThanOrEqual(filters.clientWidth);
      expect(status.textContent).toContain('Incluir anulados');
      expect(branch.getBoundingClientRect().height).toBe(46);
      expect(status.getBoundingClientRect().height).toBe(46);
      expect(root.querySelector('.receipt-list.listing-card .card-header')).toBeNull();
      expect(root.querySelector('.filter-card .filter-card-header')).not.toBeNull();
      if (width === 1240) {
        expect(branch.getBoundingClientRect().top).toBe(dates.querySelector('button').getBoundingClientRect().top);
        expect(actions.getBoundingClientRect().top).toBe(branch.getBoundingClientRect().top);
      } else if (width === 360) {
        expect(status.getBoundingClientRect().top).toBeGreaterThan(branch.getBoundingClientRect().bottom);
        const dateButtons = Array.from(dates.querySelectorAll('button')) as HTMLElement[];
        expect(dateButtons[1].getBoundingClientRect().top).toBeGreaterThan(dateButtons[0].getBoundingClientRect().bottom);
      }
    });
  });
});
