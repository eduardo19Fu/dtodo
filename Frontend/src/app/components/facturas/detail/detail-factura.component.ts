import { DOCUMENT } from '@angular/common';
import {
  AfterViewInit, Component, ElementRef, HostListener, Inject, Input, OnChanges, OnDestroy
} from '@angular/core';

import { DetalleDocumentoDto } from '../../../dtos/detalleDocumentoDto';
import { FacturaDto } from '../../../dtos/factura-dto';
import { DetailService } from '../../../services/facturas/detail.service';
import { FacturaService } from '../../../services/facturas/factura.service';

import Swal from 'sweetalert2';

@Component({
  selector: 'app-detail-factura',
  templateUrl: './detail-factura.component.html',
  styleUrls: ['./detail-factura.component.css']
})
export class DetailFacturaComponent implements OnChanges, AfterViewInit, OnDestroy {

  title = 'Detalle de Factura';
  @Input() factura: FacturaDto;

  items: DetalleDocumentoDto[] = [];
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
    public detailService: DetailService,
    private facturaService: FacturaService,
    private elementRef: ElementRef<HTMLElement>,
    @Inject(DOCUMENT) private document: Document
  ) {}

  ngAfterViewInit(): void {
    const hostElement = this.elementRef.nativeElement;
    this.document.body.appendChild(hostElement);
  }

  ngOnDestroy(): void {
    const hostElement = this.elementRef.nativeElement;

    if (hostElement.parentNode === this.document.body) {
      this.document.body.removeChild(hostElement);
    }
  }

  ngOnChanges(): void {
    if (this.factura) {
      this.cargaInicialPendiente = true;
      this.cargarDetalle(0);
    }
  }

  cargarDetalle(page: number): void {
    this.cargando = true;
    this.facturaService.getDetalleFacturaDto(this.factura.idFactura, page, this.pageSize).subscribe(
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
          this.detailService.abrirModal();
        }
      },
      error => {
        this.cargando = false;
        this.cargaInicialPendiente = false;
        Swal.close();
        Swal.fire('Error al cargar el detalle de la factura',
          error.error?.message || error.error?.mensaje || 'Ha ocurrido un error inesperado', 'error');
      }
    );
  }

  cerrarModal(): void {
    this.detailService.cerrarModal();
  }

  @HostListener('document:keydown.escape')
  cerrarConEscape(): void {
    if (this.detailService.modal) {
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
