import { Component, ElementRef, EventEmitter, HostListener, Input, Output } from '@angular/core';

import { ReporteSelectorOpcionDto } from '../../../dtos/reporte-selector-opcion-dto';

@Component({
  selector: 'app-reporte-selector',
  templateUrl: './reporte-selector.component.html',
  styleUrls: ['./reporte-selector.component.css']
})
export class ReporteSelectorComponent {
  private static siguienteId = 0;

  @Input() etiqueta: string;
  @Input() icono = 'fa-list';
  @Input() opciones: ReporteSelectorOpcionDto[] = [];
  @Input() valor: any;
  @Input() valorVacio: any = null;
  @Input() textoVacio = 'Todos';
  @Input() detalleVacio = 'Sin filtro';
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

  constructor(private elementRef: ElementRef) {}

  get textoSeleccionado(): string {
    if (this.esValorVacio()) {
      return this.textoVacio;
    }
    const opcion = this.opciones.find(item => item.valor === this.valor);
    return opcion ? opcion.etiqueta : String(this.valor || this.textoVacio);
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
    if (this.abierto && this.buscable) {
      window.setTimeout(() => {
        const buscador = this.elementRef.nativeElement.querySelector('.report-select-search input');
        if (buscador) {
          buscador.focus();
        }
      });
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

  private cerrar(): void {
    if (!this.abierto) {
      return;
    }
    this.abierto = false;
    this.busqueda = '';
    this.despliegueChange.emit(false);
  }

  esValorVacio(): boolean {
    return this.valor === this.valorVacio || this.valor === null || this.valor === undefined;
  }

  private normalizar(valor: string): string {
    return (valor || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
  }
}
