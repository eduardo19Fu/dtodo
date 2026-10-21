import { DOCUMENT } from '@angular/common';
import {
  AfterViewInit, Component, ElementRef, EventEmitter, HostListener, Inject, Input, OnDestroy, Output
} from '@angular/core';

import { DevolucionBodega, ETIQUETAS_ESTADO_DEVOLUCION } from 'src/app/models/devolucion-bodega';
import { UsuarioAuxiliar } from 'src/app/models/auxiliar/usuario-auxiliar';

@Component({
  selector: 'app-detail-devolucion-bodega',
  templateUrl: './detail-devolucion-bodega.component.html',
  styleUrls: [
    '../../bodegas/detail-bodega/detail-bodega.component.css',
    './detail-devolucion-bodega.component.css'
  ]
})
export class DetailDevolucionBodegaComponent implements AfterViewInit, OnDestroy {

  title = 'Devolución a bodega';

  @Input() devolucion: DevolucionBodega;
  @Input() puedeAprobar = false;
  /** El costo y el valor de la devolución solo los ve un administrador. */
  @Input() verCostos = false;
  @Input() procesando = false;
  /** Se está generando un comprobante: el botón de imprimir queda bloqueado con un indicador de carga. */
  @Input() imprimiendo = false;

  @Output() cerrar = new EventEmitter<void>();
  @Output() imprimir = new EventEmitter<number>();
  @Output() aprobar = new EventEmitter<{ idDevolucion: number; bodega: string }>();
  @Output() rechazar = new EventEmitter<{ idDevolucion: number; sucursal: string }>();

  constructor(
    private elementRef: ElementRef<HTMLElement>,
    @Inject(DOCUMENT) private document: Document
  ) {}

  ngAfterViewInit(): void {
    // El modal se mueve al body para que su overlay cubra toda la pantalla, igual que el resto de detalles
    this.document.body.appendChild(this.elementRef.nativeElement);
  }

  ngOnDestroy(): void {
    const hostElement = this.elementRef.nativeElement;
    if (hostElement.parentNode === this.document.body) {
      this.document.body.removeChild(hostElement);
    }
  }

  get numero(): string {
    return this.devolucion ? String(this.devolucion.idDevolucion).padStart(6, '0') : '';
  }

  get etiquetaEstado(): string {
    return this.devolucion ? ETIQUETAS_ESTADO_DEVOLUCION[this.devolucion.estado] : '';
  }

  get claseEstado(): string {
    switch (this.devolucion?.estado) {
      case 'REALIZADO': return 'status-done';
      case 'PENDIENTE': return 'status-pending';
      default: return 'status-cancelled';
    }
  }

  get iconoEstado(): string {
    switch (this.devolucion?.estado) {
      case 'REALIZADO': return 'fa-check-circle';
      case 'PENDIENTE': return 'fa-hourglass-half';
      default: return 'fa-ban';
    }
  }

  get totalUnidades(): number {
    return (this.devolucion?.items || []).reduce((total, item) => total + (item.cantidad || 0), 0);
  }

  get pendiente(): boolean {
    return this.devolucion?.estado === 'PENDIENTE';
  }

  get rechazada(): boolean {
    return this.devolucion?.estado === 'CANCELADO';
  }

  nombreCompleto(usuario: UsuarioAuxiliar): string {
    if (!usuario) {
      return 'No registrado';
    }
    const nombre = [usuario.primerNombre, usuario.apellido].filter(parte => !!parte).join(' ').trim();
    return nombre || usuario.usuario || 'No registrado';
  }

  solicitarAprobacion(): void {
    this.aprobar.emit({ idDevolucion: this.devolucion.idDevolucion, bodega: this.devolucion.bodega?.nombre });
  }

  solicitarRechazo(): void {
    this.rechazar.emit({ idDevolucion: this.devolucion.idDevolucion, sucursal: this.devolucion.sucursalOrigen?.nombre });
  }

  cerrarModal(): void {
    this.cerrar.emit();
  }

  @HostListener('document:keydown.escape')
  cerrarConEscape(): void {
    this.cerrarModal();
  }

  cerrarDesdeBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.cerrarModal();
    }
  }
}
