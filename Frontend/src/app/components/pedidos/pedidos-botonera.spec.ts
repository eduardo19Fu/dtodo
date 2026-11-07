import { CommonModule } from '@angular/common';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { of, Subject } from 'rxjs';
import Swal from 'sweetalert2';

import { PedidoDto } from '../../dtos/pedido-dto';
import { TooltipDirective } from '../../directives/tooltip.directive';
import { AuthService } from '../../services/auth.service';
import { PedidoService } from '../../services/pedido.service';
import { SaldoService } from '../../services/saldo.service';
import { DateRangePickerComponent } from '../shared/date-range-picker/date-range-picker.component';
import { PedidosComponent } from './pedidos.component';

describe('Botonera del listado de pedidos', () => {
  let fixture: ComponentFixture<PedidosComponent>;
  let detalleRespuesta: Subject<any>;

  beforeEach(async () => {
    detalleRespuesta = new Subject<any>();
    const pedido = Object.assign(new PedidoDto(), {
      idProforma: 7, noProforma: '123P', cliente: 'ANA PEREZ', negocio: 'LIBRERIA', vendedor: 'Bryan', idRuta: 1,
      ruta: 'RUTA 1', estadoPedido: 'CONFIRMADA', unidades: 15
    });
    await TestBed.configureTestingModule({
      imports: [CommonModule, FormsModule],
      declarations: [PedidosComponent, DateRangePickerComponent, TooltipDirective],
      providers: [
        { provide: AuthService, useValue: { hasRole: (rol: string) => rol === 'ROLE_ADMIN', esRepartidor: () => false } },
        { provide: PedidoService, useValue: { listar: () => of([pedido]), detalle: () => detalleRespuesta } },
        { provide: SaldoService, useValue: { pedido: () => of({ condicionPago: 'CONTADO', entregado: 0, cobrado: 0 }) } }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
    fixture = TestBed.createComponent(PedidosComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.destroy();
    document.querySelectorAll('.app-tooltip').forEach(tooltip => tooltip.remove());
  });

  it('cada botón de icono muestra su tooltip al pasar el cursor', () => {
    const botones = Array.from(fixture.nativeElement.querySelectorAll('.acciones-iconos .table-action')) as HTMLElement[];
    expect(botones.length).toBeGreaterThanOrEqual(3);

    const esperados = ['Ver detalle del pedido', 'Imprimir hoja de picking', 'Imprimir lista de empaque'];
    esperados.forEach((texto, indice) => {
      botones[indice].dispatchEvent(new Event('mouseenter'));
      const tooltip = document.querySelector('.app-tooltip');
      expect(tooltip).withContext(texto).not.toBeNull();
      expect(tooltip.textContent).toBe(texto);
      botones[indice].dispatchEvent(new Event('mouseleave'));
      expect(document.querySelector('.app-tooltip')).toBeNull();
    });
  });

  it('la acción principal vive bajo la etapa, es compacta y no está en la columna de iconos', () => {
    const principal = fixture.nativeElement.querySelector('.etapa-celda .accion-principal') as HTMLElement;
    expect(principal).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.acciones .accion-principal')).toBeNull();
    expect(principal.getBoundingClientRect().height).toBeLessThan(34);
    const pildora = fixture.nativeElement.querySelector('.etapa-celda .etapa-pill') as HTMLElement;
    expect(principal.getBoundingClientRect().top).toBeGreaterThan(pildora.getBoundingClientRect().bottom - 1);
  });

  it('ningún elemento tapa los botones: reciben el clic en su centro', () => {
    const botones = Array.from(fixture.nativeElement.querySelectorAll('tbody button')) as HTMLElement[];
    expect(botones.length).toBeGreaterThan(3);
    botones.forEach(boton => {
      boton.scrollIntoView({ block: 'center' });
      const caja = boton.getBoundingClientRect();
      const encima = document.elementFromPoint(caja.left + caja.width / 2, caja.top + caja.height / 2);
      expect(boton.contains(encima)).withContext(boton.className + ' tapado por ' + (encima && encima.outerHTML.slice(0, 80))).toBeTrue();
    });
  });

  it('los botones sobreviven a la detección de cambios que provoca el hover (no se reconstruye la tabla)', () => {
    const antes = fixture.nativeElement.querySelector('.acciones .action-detail') as HTMLElement;

    antes.dispatchEvent(new Event('mouseenter'));
    fixture.detectChanges();
    fixture.detectChanges();

    const despues = fixture.nativeElement.querySelector('.acciones .action-detail') as HTMLElement;
    expect(despues).toBe(antes);
  });

  it('el detalle muestra un toast de carga y abre el modal solo cuando llegan los datos', () => {
    const fire = spyOn(Swal, 'fire');
    const close = spyOn(Swal, 'close');
    const componente = fixture.componentInstance;

    componente.verDetalle(componente.pedidos[0]);
    fixture.detectChanges();

    expect(fire).toHaveBeenCalledWith(jasmine.objectContaining({ toast: true, title: 'Cargando detalle' }));
    expect(fixture.nativeElement.querySelector('.pedido-overlay')).toBeNull();

    detalleRespuesta.next({ noProforma: '123P', lineas: [], entregas: [], etapas: [], estadoPedido: 'CONFIRMADA' });
    fixture.detectChanges();

    expect(close).toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('.pedido-overlay')).not.toBeNull();
  });

  it('las líneas del detalle se paginan: 10 por página por defecto y se puede cambiar', () => {
    const componente = fixture.componentInstance;
    componente.detalle = {
      noProforma: '123P', estadoPedido: 'CONFIRMADA', entregas: [], etapas: [],
      lineas: Array.from({ length: 23 }, (_, i) => ({ idDetalle: i + 1, codigo: 'C' + i, producto: 'P' + i, cantidad: 1, cantidadEntregada: 0, pendiente: 1 }))
    } as any;
    fixture.detectChanges();

    expect(componente.totalPaginasLineas).toBe(3);
    expect(componente.lineasPagina.length).toBe(10);
    expect(fixture.nativeElement.querySelectorAll('.pedido-section .document-lines-table tbody tr').length).toBe(10);
    expect(fixture.nativeElement.querySelector('.pagination-info').textContent).toContain('Mostrando 1 a 10 de 23');

    componente.irAPaginaLineas(2);
    fixture.detectChanges();
    expect(componente.lineasPagina.length).toBe(3);
    expect(fixture.nativeElement.querySelector('.pagination-info').textContent).toContain('Mostrando 21 a 23 de 23');

    componente.cambiarTamanoPaginaLineas(25);
    fixture.detectChanges();
    expect(componente.paginaLineas).toBe(0);
    expect(componente.totalPaginasLineas).toBe(1);
    expect(fixture.nativeElement.querySelector('.pagination')).toBeNull();
  });

  it('el formulario de entrega alinea sucursal, fecha y quien entregó, y dibuja las casillas como tarjetas', async () => {
    const componente = fixture.componentInstance;
    componente.detalle = {
      noProforma: '123P', estadoPedido: 'EN_RUTA', condicionPago: 'CONTADO', entregas: [], etapas: [],
      lineas: [{ idDetalle: 1, codigo: 'C1', producto: 'P1', cantidad: 5, cantidadEntregada: 0, pendiente: 5 }]
    } as any;
    componente.registrandoEntrega = true;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const select = fixture.nativeElement.querySelector('.entrega-campos select') as HTMLElement;
    const fecha = fixture.nativeElement.querySelector('.entrega-campos .date-trigger') as HTMLElement;
    const entrego = fixture.nativeElement.querySelector('.entrega-campos input[name="entregaPor"]') as HTMLElement;
    [select, fecha, entrego].forEach(control => expect(control.getBoundingClientRect().height).withContext(control.outerHTML.slice(0, 80)).toBe(46));
    if (window.innerWidth >= 1100) {
      expect(Math.round(fecha.getBoundingClientRect().top)).toBe(Math.round(select.getBoundingClientRect().top));
      expect(Math.round(entrego.getBoundingClientRect().top)).toBe(Math.round(select.getBoundingClientRect().top));
    }

    const tarjetas = fixture.nativeElement.querySelectorAll('.entrega-casilla') as NodeListOf<HTMLElement>;
    expect(tarjetas.length).toBe(2);
    const entrada = tarjetas[1].querySelector('input') as HTMLInputElement;
    expect(getComputedStyle(entrada).opacity).toBe('0');
    expect(tarjetas[1].classList).not.toContain('marcada');
    entrada.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(tarjetas[1].classList).toContain('marcada');
    expect(entrada.checked).toBeTrue();
  });

  it('el filtro de etapa incluye los pedidos con entrega parcial y los entregados', () => {
    const codigos = fixture.componentInstance.opcionesEstado.map(opcion => opcion.codigo);
    expect(codigos).toContain('ENTREGADA_PARCIAL');
    expect(codigos).toContain('ENTREGADA');
  });
});
