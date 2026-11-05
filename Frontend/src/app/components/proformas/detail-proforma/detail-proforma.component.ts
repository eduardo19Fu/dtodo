import { DOCUMENT } from '@angular/common';
import {
  AfterViewInit, Component, ElementRef, EventEmitter, HostListener, Inject, Input, OnChanges, OnDestroy, Output
} from '@angular/core';

import { DetalleDocumentoDto } from '../../../dtos/detalleDocumentoDto';
import { ETIQUETAS_ACCION_AUDITORIA, ProformaAuditoriaDto } from '../../../dtos/autorizacion-dto';
import { ProformaDto } from '../../../dtos/proforma-dto';
import { DetailService } from '../../../services/facturas/detail.service';
import { ProformaService } from '../../../services/proformas/proforma.service';

import Swal from 'sweetalert2';

@Component({
  selector: 'app-detail-proforma',
  templateUrl: './detail-proforma.component.html',
  styleUrls: ['./detail-proforma.component.css']
})
export class DetailProformaComponent implements OnChanges, AfterViewInit, OnDestroy {

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
  historial: ProformaAuditoriaDto[] = [];
  historialNoDisponible = false;
  /** El historial arranca plegado: solo se ve su barra, para que no le quite espacio al detalle. */
  historialAbierto = false;
  private cargaInicialPendiente = false;
  constructor(
    public detailService: DetailService,
    private proformaService: ProformaService,
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
    if (this.proformadto) {
      this.historialAbierto = false;
      this.cargaInicialPendiente = true;
      this.cargarDetalle(0);
      this.cargarHistorial();
    }
  }

  /** El historial no es imprescindible: si falla se avisa en su sección y el detalle sigue funcionando. */
  cargarHistorial(): void {
    this.historial = [];
    this.historialNoDisponible = false;
    this.proformaService.getAuditoria(this.proformadto.idProforma).subscribe(
      historial => this.historial = historial,
      () => this.historialNoDisponible = true
    );
  }

  etiquetaAccion(accion: string): string {
    return ETIQUETAS_ACCION_AUDITORIA[accion] || accion;
  }

  alternarHistorial(): void {
    this.historialAbierto = !this.historialAbierto;
  }

  /** El movimiento más reciente: el servidor entrega el historial del más nuevo al más antiguo. */
  get ultimoMovimiento(): ProformaAuditoriaDto | null {
    return this.historial.length > 0 ? this.historial[0] : null;
  }

  /** Icono de cada movimiento en la línea de tiempo. */
  iconoAccion(accion: string): string {
    switch (accion) {
      case 'CREACION': return 'fa-plus';
      case 'MODIFICACION': return 'fa-pen';
      case 'ANULACION': return 'fa-ban';
      case 'AUTORIZACION':
      case 'AUTORIZACION_CREDITO':
      case 'AUTORIZACION_VENCIDA': return 'fa-check';
      case 'RECHAZO':
      case 'RECHAZO_CREDITO': return 'fa-times';
      case 'DEVOLUCION_CLIENTE': return 'fa-undo-alt';
      case 'ANULACION_DEVOLUCION_CLIENTE': return 'fa-ban';
      case 'CAMBIO_CLIENTE_FACTURA': return 'fa-exchange-alt';
      default: return 'fa-circle';
    }
  }

  /** Tono del punto: verde si se aprobó o creó, rojo si se rechazó o anuló, neutro en lo demás. */
  tonoAccion(accion: string): 'ok' | 'bad' | 'neutral' {
    if (['CREACION', 'AUTORIZACION', 'AUTORIZACION_CREDITO', 'AUTORIZACION_VENCIDA'].includes(accion)) {
      return 'ok';
    }
    if (['ANULACION', 'RECHAZO', 'RECHAZO_CREDITO', 'ANULACION_DEVOLUCION_CLIENTE'].includes(accion)) {
      return 'bad';
    }
    return 'neutral';
  }

  etiquetaAutorizacion(estado: string): string {
    return estado === 'PENDIENTE' ? 'Pendiente de autorización' : estado === 'APROBADA' ? 'Descuento aprobado'
      : estado === 'RECHAZADA' ? 'Descuento rechazado' : '';
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
