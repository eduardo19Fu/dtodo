import { CommonModule } from '@angular/common';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';

import { AuthService } from 'src/app/services/auth.service';
import { ClienteService } from 'src/app/services/cliente.service';
import { RutaService } from 'src/app/services/ruta.service';
import { CreateClienteComponent } from './create-cliente.component';

describe('Formulario de cliente: interruptor de cliente mayorista', () => {
  let fixture: ComponentFixture<CreateClienteComponent>;
  let root: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CommonModule, FormsModule],
      declarations: [CreateClienteComponent],
      providers: [
        { provide: ClienteService, useValue: {} },
        { provide: RutaService, useValue: { getActivas: () => of([]), getMias: () => of([]) } },
        { provide: Router, useValue: { navigate: () => Promise.resolve(true) } },
        { provide: ActivatedRoute, useValue: { params: of({}), snapshot: { params: {} } } },
        { provide: AuthService, useValue: { hasRole: (rol: string) => rol === 'ROLE_ADMIN', esVendedor: () => false } }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
    fixture = TestBed.createComponent(CreateClienteComponent);
    fixture.detectChanges();
    root = fixture.nativeElement;
    // El componente se pinta fuera de la página real: se le pone ancho para medir la ficha como en pantalla.
    root.style.display = 'block';
    root.style.width = '900px';
    document.body.appendChild(root);
  });

  afterEach(() => {
    fixture.destroy();
    if (root.parentElement === document.body) {
      document.body.removeChild(root);
    }
  });

  it('se presenta como una ficha con título, explicación e interruptor, no como texto suelto', () => {
    const ficha = root.querySelector('.mayorista-toggle') as HTMLElement;
    const estiloFicha = getComputedStyle(ficha);

    expect(estiloFicha.display).toBe('flex');
    expect(estiloFicha.borderTopWidth).toBe('1px');
    expect(ficha.getBoundingClientRect().width).toBeGreaterThan(800);

    const copia = root.querySelector('.mayorista-toggle-copy') as HTMLElement;
    expect(getComputedStyle(copia).flexDirection).toBe('column');
    const titulo = root.querySelector('.mayorista-toggle-title') as HTMLElement;
    const descripcion = root.querySelector('.mayorista-toggle-description') as HTMLElement;
    expect(descripcion.getBoundingClientRect().top).toBeGreaterThan(titulo.getBoundingClientRect().bottom - 2);

    const interruptor = root.querySelector('#esMayorista') as HTMLInputElement;
    const cajaFicha = ficha.getBoundingClientRect();
    expect(interruptor.getBoundingClientRect().right).toBeGreaterThan(cajaFicha.right - 40);
  });

  it('los selectores de ruta y categoría usan una flecha propia en lugar de la del navegador', () => {
    (root.querySelector('#esMayorista') as HTMLInputElement).click();
    fixture.detectChanges();

    ['ruta', 'categoriaDescuento'].forEach(id => {
      const selector = root.querySelector('#' + id) as HTMLSelectElement;
      const estilo = getComputedStyle(selector);

      expect(selector).withContext(id).not.toBeNull();
      expect(estilo.appearance).withContext(id).toBe('none');
      expect(estilo.backgroundImage).withContext(id).toContain('svg');
      expect(estilo.backgroundRepeat).withContext(id).toBe('no-repeat');
      expect(parseFloat(estilo.paddingRight)).withContext(id).toBeGreaterThanOrEqual(40);
    });
  });

  it('al activarlo la ficha cambia de estado y aparecen los datos mayoristas', () => {
    const interruptor = root.querySelector('#esMayorista') as HTMLInputElement;
    expect(root.querySelector('.mayorista-toggle').classList).not.toContain('is-on');
    expect(root.textContent).toContain('Actívalo si compra por volumen');

    interruptor.click();
    fixture.detectChanges();

    expect(root.querySelector('.mayorista-toggle').classList).toContain('is-on');
    expect(root.textContent).toContain('Completa abajo su ruta');
    expect(root.querySelector('#ruta')).not.toBeNull();
  });
});
