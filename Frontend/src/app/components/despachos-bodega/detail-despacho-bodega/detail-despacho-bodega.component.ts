import { DOCUMENT } from '@angular/common';
import {
  AfterViewInit, Component, ElementRef, EventEmitter, HostListener, Inject, Input, OnDestroy, Output
} from '@angular/core';

import { DespachoBodega, ETIQUETAS_ESTADO_DESPACHO } from 'src/app/models/despacho-bodega';
import { UsuarioAuxiliar } from 'src/app/models/auxiliar/usuario-auxiliar';

@Component({
  selector: 'app-detail-despacho-bodega',
  templateUrl: './detail-despacho-bodega.component.html',
  styleUrls: ['../../bodegas/detail-bodega/detail-bodega.component.css']
})
export class DetailDespachoBodegaComponent implements AfterViewInit, OnDestroy {

  title = 'Despacho de bodega';

  @Input() despacho: DespachoBodega;
  @Input() puedeAprobar = false;
  @Input() procesando = false;

  @Output() cerrar = new EventEmitter<void>();
  @Output() imprimir = new EventEmitter<number>();
  @Output() aprobar = new EventEmitter<{ idDespacho: number; sucursalDestino: string }>();
  @Output() cancelar = new EventEmitter<number>();

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
    return this.despacho ? String(this.despacho.idDespacho).padStart(6, '0') : '';
  }

  get etiquetaEstado(): string {
    return this.despacho ? ETIQUETAS_ESTADO_DESPACHO[this.despacho.estado] : '';
  }

  get claseEstado(): string {
    switch (this.despacho?.estado) {
      case 'REALIZADO': return 'status-done';
      case 'PENDIENTE': return 'status-pending';
      default: return 'status-cancelled';
    }
  }

  get iconoEstado(): string {
    switch (this.despacho?.estado) {
      case 'REALIZADO': return 'fa-check-circle';
      case 'PENDIENTE': return 'fa-hourglass-half';
      default: return 'fa-ban';
    }
  }

  get totalUnidades(): number {
    return (this.despacho?.items || []).reduce((total, item) => total + (item.cantidad || 0), 0);
  }

  get pendiente(): boolean {
    return this.despacho?.estado === 'PENDIENTE';
  }

  nombreCompleto(usuario: UsuarioAuxiliar): string {
    if (!usuario) {
      return 'No registrado';
    }
    const nombre = [usuario.primerNombre, usuario.apellido].filter(parte => !!parte).join(' ').trim();
    return nombre || usuario.usuario || 'No registrado';
  }

  solicitarAprobacion(): void {
    this.aprobar.emit({ idDespacho: this.despacho.idDespacho, sucursalDestino: this.despacho.sucursalDestino.nombre });
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
