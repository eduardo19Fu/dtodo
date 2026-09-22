import { Component, ElementRef, EventEmitter, HostListener, Input, OnDestroy, Output } from '@angular/core';

import { ReporteSelectorOpcionDto } from '../../../dtos/reporte-selector-opcion-dto';

@Component({
  selector: 'app-reporte-selector',
  templateUrl: './reporte-selector.component.html',
  styleUrls: ['./reporte-selector.component.css']
})
export class ReporteSelectorComponent implements OnDestroy {
  private static siguienteId = 0;

  @Input() etiqueta: string;
  @Input() icono = 'fa-list';
  @Input() opciones: ReporteSelectorOpcionDto[] = [];
  @Input() valor: any;
  @Input() valorVacio: any = null;
  @Input() textoVacio = 'Todos';
  @Input() detalleVacio = 'Sin filtro';
  @Input() textoSinCoincidencia = 'Cargando selección...';
  @Input() permitirVacio = true;
  @Input() requerido = false;
  @Input() deshabilitado = false;
  @Input() buscable = true;
  @Input() placeholderBusqueda = 'Buscar';
  @Output() valorChange = new EventEmitter<any>();
  @Output() despliegueChange = new EventEmitter<boolean>();

  readonly id = `reporte-selector-${ReporteSelectorComponent.siguienteId++}`;
  abierto = false;
  busqueda = '';
  estilosMenu: { [propiedad: string]: string } = {};

  private posicionPendiente = false;
  private readonly escucharReposicion = () => this.programarPosicionMenu();

  constructor(private elementRef: ElementRef) {}

  ngOnDestroy(): void {
    this.desactivarReposicion();
  }

  get textoSeleccionado(): string {
    if (this.esValorVacio()) {
      return this.textoVacio;
    }
    const opcion = this.opciones.find(item => item.valor === this.valor);
    return opcion ? opcion.etiqueta : this.textoSinCoincidencia;
  }

  get opcionesCoincidentes(): ReporteSelectorOpcionDto[] {
    const termino = this.normalizar(this.busqueda);
    return this.opciones.filter(opcion => !termino || this.normalizar(
      `${opcion.etiqueta || ''} ${opcion.detalle || ''}`
    ).includes(termino));
  }

  get opcionesVisibles(): ReporteSelectorOpcionDto[] {
    return this.opcionesCoincidentes.slice(0, 50);
  }

  alternar(): void {
    if (this.deshabilitado) {
      return;
    }
    this.abierto = !this.abierto;
    this.busqueda = '';
    this.despliegueChange.emit(this.abierto);
    if (this.abierto) {
      this.activarReposicion();
      window.setTimeout(() => {
        this.posicionarMenu();
        const buscador = this.elementRef.nativeElement.querySelector('.report-select-search input');
        if (buscador && this.buscable) {
          buscador.focus();
        }
      });
    } else {
      this.desactivarReposicion();
    }
  }

  seleccionar(opcion: ReporteSelectorOpcionDto): void {
    this.valor = opcion.valor;
    this.valorChange.emit(this.valor);
    this.cerrar();
  }

  seleccionarVacio(): void {
    this.valor = this.valorVacio;
    this.valorChange.emit(this.valor);
    this.cerrar();
  }

  estaSeleccionada(opcion: ReporteSelectorOpcionDto): boolean {
    return !this.esValorVacio() && opcion.valor === this.valor;
  }

  @HostListener('document:keydown.escape')
  cerrarConEscape(): void {
    this.cerrar();
  }

  @HostListener('document:click', ['$event'])
  cerrarAlHacerClickFuera(evento: MouseEvent): void {
    if (this.abierto && !this.elementRef.nativeElement.contains(evento.target)) {
      this.cerrar();
    }
  }

  @HostListener('window:resize')
  alCambiarVentana(): void {
    this.programarPosicionMenu();
  }

  private cerrar(): void {
    if (!this.abierto) {
      return;
    }
    this.abierto = false;
    this.busqueda = '';
    this.estilosMenu = {};
    this.desactivarReposicion();
    this.despliegueChange.emit(false);
  }

  esValorVacio(): boolean {
    return this.valor === this.valorVacio || this.valor === null || this.valor === undefined;
  }

  private normalizar(valor: string): string {
    return (valor || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
  }

  private activarReposicion(): void {
    window.addEventListener('scroll', this.escucharReposicion, true);
  }

  private desactivarReposicion(): void {
    window.removeEventListener('scroll', this.escucharReposicion, true);
  }

  private programarPosicionMenu(): void {
    if (!this.abierto || this.posicionPendiente) {
      return;
    }
    this.posicionPendiente = true;
    window.requestAnimationFrame(() => {
      this.posicionPendiente = false;
      this.posicionarMenu();
    });
  }

  private posicionarMenu(): void {
    if (!this.abierto) {
      return;
    }
    const trigger = this.elementRef.nativeElement.querySelector('.report-select-trigger') as HTMLElement;
    const menu = this.elementRef.nativeElement.querySelector('.report-select-menu') as HTMLElement;
    if (!trigger || !menu) {
      return;
    }
    const margen = 10;
    const separacion = 7;
    const triggerRect = trigger.getBoundingClientRect();
    const ancho = Math.min(triggerRect.width, window.innerWidth - margen * 2);
    const izquierda = Math.max(margen, Math.min(triggerRect.left, window.innerWidth - ancho - margen));
    const espacioAbajo = window.innerHeight - triggerRect.bottom - margen - separacion;
    const espacioArriba = triggerRect.top - margen - separacion;
    const altoDeseado = Math.min(menu.scrollHeight || 300, 300);
    const abrirArriba = espacioAbajo < Math.min(altoDeseado, 220) && espacioArriba > espacioAbajo;
    const espacioDisponible = Math.max(120, abrirArriba ? espacioArriba : espacioAbajo);
    const altoMaximo = Math.min(300, espacioDisponible);
    const altoMenu = Math.min(menu.scrollHeight || altoDeseado, altoMaximo);
    const arriba = abrirArriba
      ? Math.max(margen, triggerRect.top - separacion - altoMenu)
      : Math.min(window.innerHeight - margen - altoMenu, triggerRect.bottom + separacion);
    this.estilosMenu = {
      top: `${arriba}px`,
      left: `${izquierda}px`,
      width: `${ancho}px`,
      maxHeight: `${altoMaximo}px`
    };
  }
}
