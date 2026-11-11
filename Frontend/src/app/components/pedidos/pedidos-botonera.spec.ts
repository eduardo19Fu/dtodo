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
    // restos de otros specs (avisos, tooltips o diálogos) pueden tapar los botones al medir su centro
    document.querySelectorAll('.swal2-container, .app-tooltip, .pedido-overlay').forEach(elemento => elemento.remove());
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
    [select, fecha, entrego].forEach(control => expect(control.getBoundingClientRect().height).withContext(control.outerHTML.slice(0, 80)).toBe(50));
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

  function pedidoDeRuta(id: number, idRuta: number, ruta: string, estado: string, unidades = 10): PedidoDto {
    return Object.assign(new PedidoDto(), {
      idProforma: id, noProforma: id + 'P', cliente: 'CLIENTE ' + id, vendedor: 'Ana', idRuta, ruta, estadoPedido: estado, unidades
    });
  }

  it('con varias rutas solo se abre la primera y las demás se pueden abrir, plegar o expandir', () => {
    const componente = fixture.componentInstance;
    componente.pedidos = [
      pedidoDeRuta(1, 1, 'RUTA A', 'EMPACADA'), pedidoDeRuta(2, 1, 'RUTA A', 'EN_RUTA'),
      pedidoDeRuta(3, 2, 'RUTA B', 'CONFIRMADA'), pedidoDeRuta(4, 3, 'RUTA C', 'EN_PREPARACION'),
      pedidoDeRuta(5, 4, 'RUTA D', 'EMPACADA')
    ];
    fixture.detectChanges();

    const tarjetas = () => Array.from(fixture.nativeElement.querySelectorAll('.grupo-encabezado')).map((e: HTMLElement) => e.closest('.listing-card') as HTMLElement);
    const abiertas = () => tarjetas().filter(t => t.querySelector('.pedidos-table')).length;
    expect(tarjetas().length).toBe(4);
    expect(abiertas()).toBe(1);
    expect(tarjetas()[0].querySelector('.pedidos-table')).not.toBeNull();   // solo la primera

    (tarjetas()[1].querySelector('.grupo-encabezado') as HTMLElement).click();   // abrir la segunda a mano
    fixture.detectChanges();
    expect(abiertas()).toBe(2);

    (fixture.nativeElement.querySelectorAll('.pedidos-toolbar .barra-boton')[1] as HTMLElement).click();   // plegar todo
    fixture.detectChanges();
    expect(abiertas()).toBe(0);

    (fixture.nativeElement.querySelectorAll('.pedidos-toolbar .barra-boton')[0] as HTMLElement).click();   // expandir todo
    fixture.detectChanges();
    expect(abiertas()).toBe(4);
  });

  it('el encabezado de la ruta resume sus etapas y la nota de envío solo aparece donde hay algo por cargar', () => {
    const componente = fixture.componentInstance;
    componente.pedidos = [
      pedidoDeRuta(1, 1, 'RUTA A', 'EMPACADA'), pedidoDeRuta(2, 1, 'RUTA A', 'EMPACADA', 5), pedidoDeRuta(3, 1, 'RUTA A', 'EN_RUTA'),
      pedidoDeRuta(4, 2, 'RUTA B', 'CONFIRMADA')
    ];
    fixture.detectChanges();

    const [a, b] = Array.from(fixture.nativeElement.querySelectorAll('.grupo-encabezado')).map((e: HTMLElement) => e.closest('.listing-card') as HTMLElement);
    const chipsA = Array.from(a.querySelectorAll('.grupo-chip')).map(c => c.textContent.trim());
    expect(chipsA).toEqual(['2 empacado', '1 en ruta']);
    expect(a.querySelector('.nota-envio')).not.toBeNull();
    expect(b.querySelector('.nota-envio')).toBeNull();
    // solo una ruta con nota: no hace falta el botón general
    expect(fixture.nativeElement.querySelector('.barra-boton-principal')).toBeNull();

    componente.pedidos.push(pedidoDeRuta(5, 3, 'RUTA C', 'EMPACADA'));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.barra-boton-principal').textContent).toContain('(2)');
  });

  it('un pedido con el crédito por autorizar no ofrece avanzar y lo explica junto a la etapa', () => {
    const componente = fixture.componentInstance;
    componente.pedidos = [
      pedidoDeRuta(1, 1, 'RUTA A', 'CONFIRMADA'),
      Object.assign(pedidoDeRuta(2, 1, 'RUTA A', 'CONFIRMADA'), { creditoAutorizacion: 'PENDIENTE' } as any),
      Object.assign(pedidoDeRuta(3, 1, 'RUTA A', 'CONFIRMADA'), { creditoAutorizacion: 'RECHAZADA' } as any)
    ];
    fixture.detectChanges();

    expect(componente.puedeAvanzar(componente.pedidos[0])).toBeTrue();
    expect(componente.puedeAvanzar(componente.pedidos[1])).toBeFalse();
    expect(componente.puedeAvanzar(componente.pedidos[2])).toBeFalse();
    expect(componente.bloqueos(componente.pedidos[1]).map(b => b.texto)).toEqual(['Crédito por autorizar']);
    expect(componente.bloqueos(componente.pedidos[2]).map(b => b.texto)).toEqual(['Crédito rechazado']);

    const filas = Array.from(fixture.nativeElement.querySelectorAll('.pedidos-table tbody tr')) as HTMLElement[];
    expect(filas.length).toBe(3);
    expect(filas[0].querySelector('.accion-avance')).not.toBeNull();
    expect(filas[1].querySelector('.accion-avance')).toBeNull();
    expect(filas[1].querySelector('.etapa-bloqueada').textContent).toContain('Crédito por autorizar');
    expect(filas[2].querySelector('.accion-avance')).toBeNull();
  });

  it('si el descuento y el crédito esperan autorización se muestran las dos etiquetas', () => {
    const pedido = Object.assign(pedidoDeRuta(9, 1, 'RUTA A', 'CONFIRMADA'),
      { estadoAutorizacion: 'PENDIENTE', creditoAutorizacion: 'PENDIENTE' } as any);

    expect(fixture.componentInstance.bloqueos(pedido).map(b => b.texto)).toEqual(['Por autorizar', 'Crédito por autorizar']);
  });
});
