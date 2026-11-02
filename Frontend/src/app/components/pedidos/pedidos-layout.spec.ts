import { CommonModule } from '@angular/common';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { of } from 'rxjs';

import { AuthService } from '../../services/auth.service';
import { PedidoService } from '../../services/pedido.service';
import { SaldoService } from '../../services/saldo.service';
import { DateRangePickerComponent } from '../shared/date-range-picker/date-range-picker.component';
import { PedidosComponent } from './pedidos.component';

describe('Filtros de pedidos: integración visual', () => {
  let fixture: ComponentFixture<PedidosComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CommonModule, FormsModule],
      declarations: [PedidosComponent, DateRangePickerComponent],
      providers: [
        { provide: AuthService, useValue: { hasRole: () => false, esRepartidor: () => false } },
        { provide: PedidoService, useValue: { listar: () => of([]) } },
        { provide: SaldoService, useValue: {} }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
    fixture = TestBed.createComponent(PedidosComponent);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => fixture.destroy());

  it('mantiene la misma altura real en etapa, ruta, fechas y actualizar', () => {
    const controls = Array.from(fixture.nativeElement.querySelectorAll(
      '.filtros select, .filtros .date-trigger, .filtros .filtro-boton'
    )) as HTMLElement[];

    expect(controls.length).toBe(5);
    controls.forEach(control => {
      expect(control.getBoundingClientRect().height).withContext(control.outerHTML).toBe(46);
    });

    const labels = Array.from(fixture.nativeElement.querySelectorAll(
      '.filtros .filtro > span, .filtros .filter-label'
    )) as HTMLElement[];
    expect(labels.length).toBe(4);
    labels.forEach(label => {
      expect(getComputedStyle(label).fontSize).toBe('13px');
      expect(getComputedStyle(label).marginBottom).toBe('6px');
    });
  });

  it('muestra una sola línea por fecha y conserva la apertura del calendario', () => {
    const trigger: HTMLButtonElement = fixture.nativeElement.querySelector('.date-trigger');
    const redundantLabel: HTMLElement = trigger.querySelector('small');
    expect(getComputedStyle(redundantLabel).display).toBe('none');

    trigger.click();
    fixture.detectChanges();
    expect(trigger.getAttribute('aria-expanded')).toBe('true');
    expect(fixture.nativeElement.querySelector('[role="dialog"]')).not.toBeNull();
  });
});
