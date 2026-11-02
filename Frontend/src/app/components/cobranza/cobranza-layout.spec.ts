import { CommonModule } from '@angular/common';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { of } from 'rxjs';

import { CobranzaService } from '../../services/cobranza.service';
import { CobranzaComponent } from './cobranza.component';

describe('Distribución de filtros de cobranza', () => {
  let fixture: ComponentFixture<CobranzaComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CommonModule, FormsModule],
      declarations: [CobranzaComponent],
      providers: [{ provide: CobranzaService, useValue: {
        cuentasPorCobrar: () => of({ clientes: [], clientesConVencido: 0, vencido: 0 })
      } }],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
    fixture = TestBed.createComponent(CobranzaComponent);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => fixture.destroy());

  it('aplica el diseño del resumen y destaca el total con los estilos cargados en el navegador', () => {
    const root: HTMLElement = fixture.nativeElement;
    const summary: HTMLElement = root.querySelector('.cobranza-summary');
    const total: HTMLElement = root.querySelector('.balance-total');
    const amount: HTMLElement = total.querySelector('strong');
    const periods = Array.from(root.querySelectorAll('.balance-period > strong')) as HTMLElement[];

    expect(getComputedStyle(summary).display).toBe('grid');
    expect(getComputedStyle(summary).backgroundColor).toBe('rgb(255, 255, 255)');
    expect(getComputedStyle(summary).borderTopLeftRadius).toBe('10px');
    expect(getComputedStyle(total).backgroundColor).toBe('rgb(237, 246, 242)');
    expect(getComputedStyle(amount).display).toBe('block');
    expect(getComputedStyle(amount).fontSize).toBe('30px');
    expect(periods.length).toBe(4);
    periods.forEach(period => expect(getComputedStyle(period).display).toBe('block'));
  });

  [960, 640, 320].forEach(width => {
    it(`mantiene campos uniformes y exportaciones separadas a ${width}px`, () => {
      const root: HTMLElement = fixture.nativeElement;
      const fields: HTMLElement = root.querySelector('.cobranza-fields');
      fields.style.width = `${width}px`;
      const controls = Array.from(fields.querySelectorAll(
        '.form-control, .cobranza-balance-toggle'
      )) as HTMLElement[];
      expect(controls.length).toBe(3);
      controls.forEach(control => expect(control.getBoundingClientRect().height).toBe(46));
      expect(fields.scrollWidth).toBeLessThanOrEqual(fields.clientWidth);

      if (width === 960) {
        const top = controls[0].getBoundingClientRect().top;
        controls.forEach(control => expect(control.getBoundingClientRect().top).toBe(top));
      } else if (width === 320) {
        expect(controls[1].getBoundingClientRect().top).toBeGreaterThan(controls[0].getBoundingClientRect().bottom);
        expect(controls[2].getBoundingClientRect().top).toBeGreaterThan(controls[1].getBoundingClientRect().bottom);
      }

      const exports = root.querySelector('.cobranza-exports').getBoundingClientRect();
      expect(exports.top).toBeGreaterThan(fields.getBoundingClientRect().bottom);
    });
  });
});
