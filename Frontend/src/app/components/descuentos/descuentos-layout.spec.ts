import { CommonModule } from '@angular/common';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { of } from 'rxjs';

import { DescuentoService } from '../../services/descuento.service';
import { DescuentosComponent } from './descuentos.component';

describe('Distribución adaptable de filtros de descuentos', () => {
  let fixture: ComponentFixture<DescuentosComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CommonModule, FormsModule],
      declarations: [DescuentosComponent],
      providers: [{ provide: DescuentoService, useValue: {
        getCategorias: () => of([]),
        getProductos: () => of({ content: [], number: 0, totalPages: 0, totalElements: 0, first: true, last: true })
      } }],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
    fixture = TestBed.createComponent(DescuentosComponent);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => fixture.destroy());

  [960, 640, 320].forEach(width => {
    it(`conserva el texto separado y el interruptor dentro de su campo a ${width}px`, () => {
      const root: HTMLElement = fixture.nativeElement;
      const filters: HTMLElement = root.querySelector('.filtros-productos');
      filters.style.width = `${width}px`;
      const search = root.querySelector('.filtros-productos > .listing-field').getBoundingClientRect();
      const option = root.querySelector('.discount-filter-toggle').getBoundingClientRect();
      const title = root.querySelector('.discount-filter-title').getBoundingClientRect();
      const description = root.querySelector('.discount-filter-description').getBoundingClientRect();
      const toggle = root.querySelector('.discount-filter-toggle input').getBoundingClientRect();

      expect(description.top).toBeGreaterThanOrEqual(title.bottom);
      expect(toggle.right).toBeLessThanOrEqual(option.right);
      expect(description.right).toBeLessThan(toggle.left);
      expect(filters.scrollWidth).toBeLessThanOrEqual(filters.clientWidth);
      if (width === 960) {
        expect(option.left).toBeGreaterThan(search.right);
      } else {
        expect(option.top).toBeGreaterThan(search.bottom);
      }
    });
  });
});
