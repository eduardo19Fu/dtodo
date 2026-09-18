import { DOCUMENT } from '@angular/common';
import { AfterViewInit, Component, ElementRef, HostListener, Inject, Input, OnDestroy } from '@angular/core';

import { ProveedorDto } from 'src/app/dtos/proveedor-dto';
import { DetailProveedorService } from 'src/app/services/proveedores/detail-proveedor.service';

@Component({
  selector: 'app-detail-proveedor',
  templateUrl: './detail-proveedor.component.html',
  styleUrls: ['./detail-proveedor.component.css']
})
export class DetailProveedorComponent implements AfterViewInit, OnDestroy {

  title = 'Detalle de Proveedor';

  @Input() proveedor: ProveedorDto;

  constructor(
    public detailProveedorService: DetailProveedorService,
    private elementRef: ElementRef<HTMLElement>,
    @Inject(DOCUMENT) private document: Document
  ) {}

  ngAfterViewInit(): void {
    this.document.body.appendChild(this.elementRef.nativeElement);
  }

  ngOnDestroy(): void {
    const hostElement = this.elementRef.nativeElement;
    if (hostElement.parentNode === this.document.body) {
      this.document.body.removeChild(hostElement);
    }
  }

  get nombreRegistro(): string {
    if (!this.proveedor) {
      return 'No registrado';
    }
    return this.proveedor.registradoPor?.trim() || this.proveedor.usuario || 'No registrado';
  }

  cerrarModal(): void {
    this.detailProveedorService.cerrarModal();
  }

  @HostListener('document:keydown.escape')
  cerrarConEscape(): void {
    if (this.detailProveedorService.modal) {
      this.cerrarModal();
    }
  }

  cerrarDesdeBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.cerrarModal();
    }
  }

}
