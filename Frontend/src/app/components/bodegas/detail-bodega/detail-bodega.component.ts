import { DOCUMENT } from '@angular/common';
import {
  AfterViewInit, Component, ElementRef, EventEmitter, HostListener, Inject, Input, OnDestroy, Output
} from '@angular/core';

import { Bodega } from 'src/app/models/bodega';

@Component({
  selector: 'app-detail-bodega',
  templateUrl: './detail-bodega.component.html',
  styleUrls: ['./detail-bodega.component.css']
})
export class DetailBodegaComponent implements AfterViewInit, OnDestroy {

  title = 'Detalle de Bodega';

  @Input() bodega: Bodega;
  @Output() cerrar = new EventEmitter<void>();

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

  get activa(): boolean {
    return this.bodega?.estado?.estado === 'ACTIVO';
  }

  get nombreRegistro(): string {
    const usuario = this.bodega?.usuario;
    if (!usuario) {
      return 'No registrado';
    }
    const nombre = [usuario.primerNombre, usuario.apellido].filter(parte => !!parte).join(' ').trim();
    return nombre || usuario.usuario || 'No registrado';
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
