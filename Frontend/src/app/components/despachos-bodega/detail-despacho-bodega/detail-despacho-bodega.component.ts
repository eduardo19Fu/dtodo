import { DOCUMENT } from '@angular/common';
import {
  AfterViewInit, Component, ElementRef, EventEmitter, HostListener, Inject, Input, OnDestroy, Output
} from '@angular/core';

import { DespachoBodega, ETIQUETAS_ESTADO_DESPACHO, EstadoDespachoBodega } from 'src/app/models/despacho-bodega';
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
  @Output() aprobar = new EventEmitter<{ idDespacho: number; destino: string }>();
  @Output() cancelar = new EventEmitter<{ idDespacho: number; estado: EstadoDespachoBodega }>();

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

  get esTraslado(): boolean {
    return !!this.despacho?.bodegaDestino;
  }

  /** Nombre de la sucursal destino o, en un traslado, de la bodega destino. */
  get nombreDestino(): string {
    return (this.esTraslado ? this.despacho.bodegaDestino.nombre : this.despacho?.sucursalDestino?.nombre) || '';
  }

  get totalUnidades(): number {
    return (this.despacho?.items || []).reduce((total, item) => total + (item.cantidad || 0), 0);
  }

  get pendiente(): boolean {
    return this.despacho?.estado === 'PENDIENTE';
  }

  /** Un pendiente se cancela; uno aprobado solo lo revierte un administrador (el mismo permiso que aprobar). */
  get puedeCancelar(): boolean {
    return this.pendiente || (this.despacho?.estado === 'REALIZADO' && this.puedeAprobar);
  }

  solicitarCancelacion(): void {
    this.cancelar.emit({ idDespacho: this.despacho.idDespacho, estado: this.despacho.estado });
  }

  nombreCompleto(usuario: UsuarioAuxiliar): string {
    if (!usuario) {
      return 'No registrado';
    }
    const nombre = [usuario.primerNombre, usuario.apellido].filter(parte => !!parte).join(' ').trim();
    return nombre || usuario.usuario || 'No registrado';
  }

  solicitarAprobacion(): void {
    this.aprobar.emit({ idDespacho: this.despacho.idDespacho, destino: this.nombreDestino });
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
