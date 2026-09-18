import { DOCUMENT } from '@angular/common';
import {
  AfterViewInit, Component, ElementRef, HostListener, Inject, Input, OnChanges, OnDestroy
} from '@angular/core';

import { CompraDetalleDocumentoDto } from 'src/app/dtos/compra-detalle-documento-dto';
import { CompraDto } from 'src/app/dtos/compra-dto';
import { CompraService } from 'src/app/services/compra.service';
import { DetailCompraService } from 'src/app/services/compras/detail-compra.service';

import Swal from 'sweetalert2';

@Component({
  selector: 'app-detail-compra',
  templateUrl: './detail-compra.component.html',
  styleUrls: ['./detail-compra.component.css']
})
export class DetailCompraComponent implements OnChanges, AfterViewInit, OnDestroy {

  title = 'Detalle de Compra';

  @Input() compra: CompraDto;

  items: CompraDetalleDocumentoDto[] = [];
  paginaActual = 0;
  totalPaginas = 0;
  totalElementos = 0;
  pageSize = 5;
  pageSizeOptions: number[] = [5, 10, 15, 25];
  isFirst = true;
  isLast = false;
  cargando = false;
  private cargaInicialPendiente = false;

  constructor(
    public detailCompraService: DetailCompraService,
    private compraService: CompraService,
    private elementRef: ElementRef<HTMLElement>,
    @Inject(DOCUMENT) private document: Document
  ) {}

  ngOnChanges(): void {
    if (this.compra) {
      this.cargaInicialPendiente = true;
      this.cargarDetalle(0);
    }
  }

  ngAfterViewInit(): void {
    this.document.body.appendChild(this.elementRef.nativeElement);
  }

  ngOnDestroy(): void {
    const hostElement = this.elementRef.nativeElement;
    if (hostElement.parentNode === this.document.body) {
      this.document.body.removeChild(hostElement);
    }
  }

  cargarDetalle(page: number): void {
    this.cargando = true;
    this.compraService.getDetalleCompraDto(this.compra.idCompra, page, this.pageSize).subscribe(
      response => {
        this.items = response.content;
        this.paginaActual = response.number;
        this.totalPaginas = response.totalPages;
        this.totalElementos = response.totalElements;
        this.pageSize = response.size;
        this.isFirst = response.first;
        this.isLast = response.last;
        this.cargando = false;
        if (this.cargaInicialPendiente) {
          this.cargaInicialPendiente = false;
          Swal.close();
          this.detailCompraService.abrirModal();
        }
      },
      error => {
        this.cargando = false;
        this.cargaInicialPendiente = false;
        Swal.close();
        Swal.fire('Error al cargar el detalle de la compra',
          error.error?.message || error.error?.mensaje || 'Ha ocurrido un error inesperado', 'error');
      }
    );
  }

  get nombreRegistro(): string {
    if (!this.compra) {
      return 'No registrado';
    }
    return this.compra.registradoPor?.trim() || this.compra.usuario || 'No registrado';
  }

  cerrarModal(): void {
    this.detailCompraService.cerrarModal();
  }

  @HostListener('document:keydown.escape')
  cerrarConEscape(): void {
    if (this.detailCompraService.modal) {
      this.cerrarModal();
    }
  }

  cerrarDesdeBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.cerrarModal();
    }
  }

  irPaginaAnterior(): void { if (!this.isFirst) { this.cargarDetalle(this.paginaActual - 1); } }
  irPaginaSiguiente(): void { if (!this.isLast) { this.cargarDetalle(this.paginaActual + 1); } }
  irAPagina(pagina: number): void { this.cargarDetalle(pagina); }
  cambiarPageSize(size: number): void { this.pageSize = size; this.cargarDetalle(0); }

  get paginasVisibles(): number[] {
    const paginas: number[] = [];
    const inicio = Math.max(0, Math.min(this.paginaActual - 2, this.totalPaginas - 5));
    const fin = Math.min(this.totalPaginas - 1, inicio + 4);
    for (let pagina = inicio; pagina <= fin; pagina++) {
      paginas.push(pagina);
    }
    return paginas;
  }
}
