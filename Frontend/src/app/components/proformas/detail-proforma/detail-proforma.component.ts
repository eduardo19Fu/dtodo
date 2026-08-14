import { Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';

import { DetalleDocumentoDto } from '../../../dtos/detalleDocumentoDto';
import { ProformaDto } from '../../../dtos/proforma-dto';
import { DetailService } from '../../../services/facturas/detail.service';
import { ProformaService } from '../../../services/proformas/proforma.service';

import Swal from 'sweetalert2';

@Component({
  selector: 'app-detail-proforma',
  templateUrl: './detail-proforma.component.html',
  styleUrls: ['./detail-proforma.component.css']
})
export class DetailProformaComponent implements OnChanges {

  @Input() proformadto: ProformaDto;
  @Output() closeModal = new EventEmitter<void>();

  title = 'Detalle de Proforma';
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
    private proformaService: ProformaService
  ) {}

  ngOnChanges(): void {
    if (this.proformadto) {
      this.cargaInicialPendiente = true;
      this.cargarDetalle(0);
    }
  }

  cargarDetalle(page: number): void {
    this.cargando = true;
    this.proformaService.getDetalleProformaDto(this.proformadto.idProforma, page, this.pageSize).subscribe(
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
        Swal.fire('Error al cargar el detalle de la proforma',
          error.error?.message || error.error?.mensaje || 'Ha ocurrido un error inesperado', 'error');
      }
    );
  }

  cerrarModal(): void {
    this.detailService.cerrarModal();
    this.closeModal.emit();
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
