import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';

import { ExportarProformasComponent } from './exportar-proformas.component';

describe('ExportarProformasComponent', () => {
  let component: ExportarProformasComponent;
  let fixture: ComponentFixture<ExportarProformasComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ExportarProformasComponent],
      imports: [FormsModule]
    }).compileComponents();

    fixture = TestBed.createComponent(ExportarProformasComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should reject an inverted date range', () => {
    component.fechaInicio = '2026-08-20';
    component.fechaFin = '2026-08-19';

    expect(component.rangoValido).toBeFalse();
    expect(component.rangoInvertido).toBeTrue();
  });
});
