import { DOCUMENT } from '@angular/common';
import {
  AfterViewInit, Component, ElementRef, HostListener, Inject, Input, OnChanges, OnDestroy, OnInit, SimpleChanges
} from '@angular/core';

import { NotaCreditoDetalleDto, NotaCreditoDetalleItemDto } from '../../../dtos/nota-credito-detalle-dto';
import { DespachoNotaDto } from '../../../dtos/despacho-nota-dto';
import { DetailService } from '../../../services/facturas/detail.service';
import { NotasCreditoService } from '../../../services/notas-credito.service';

@Component({
  selector: 'app-detail-nota',
  templateUrl: './detail-nota.component.html',
  styleUrls: ['./detail-nota.component.css']
})
export class DetailNotaComponent implements OnInit, OnChanges, AfterViewInit, OnDestroy {

  title: string;

  @Input() notaCredito: NotaCreditoDetalleDto;

  despachos: DespachoNotaDto[] = [];
  despachadoPorProducto: { [idProducto: number]: number } = {};
  numeroDespachoPorEvento: { [idEvento: string]: number } = {};

  // Acordeón
  seccionProductos = true;
  seccionDespachos = false;

  // Paginación productos
  paginaItems = 0;
  pageSizeItems = 5;

  // Paginación despachos
  paginaDespachos = 0;
  pageSizeDespachos = 5;

  constructor(
    public detailService: DetailService,
    private notasService: NotasCreditoService,
    private elementRef: ElementRef<HTMLElement>,
    @Inject(DOCUMENT) private document: Document
  ) {
    this.title = 'Detalle de Nota de Crédito';
  }

  ngOnInit(): void {
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

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.notaCredito && this.notaCredito?.idNotaCredito) {
      this.paginaItems = 0;
      this.paginaDespachos = 0;
      this.cargarDespachos();
    }
  }

  get estadoLegible(): string {
    return this.notaCredito?.estado
      ? this.notaCredito.estado.toLowerCase().replace(/_/g, ' ')
      : 'Sin estado';
  }

  cargarDespachos(): void {
    this.notasService.getDespachos(this.notaCredito.idNotaCredito).subscribe(
      despachos => {
        this.despachos = despachos;
        this.despachadoPorProducto = {};
        this.numeroDespachoPorEvento = {};
        despachos.forEach(d => {
          const id = d.idProducto;
          const claveEvento = this.getClaveEvento(d);
          const numeroActual = this.numeroDespachoPorEvento[claveEvento];
          this.despachadoPorProducto[id] = (this.despachadoPorProducto[id] || 0) + d.cantidad;
          if (!numeroActual || d.idDespacho < numeroActual) {
            this.numeroDespachoPorEvento[claveEvento] = d.idDespacho;
          }
        });
      }
    );
  }

  getDespachado(idProducto: number): number {
    return this.despachadoPorProducto[idProducto] || 0;
  }

  getPendiente(idProducto: number, cantidadTotal: number): number {
    return cantidadTotal - this.getDespachado(idProducto);
  }

  // --- Paginación productos ---

  get itemsPaginados(): NotaCreditoDetalleItemDto[] {
    const items = this.notaCredito?.items || [];
    const inicio = this.paginaItems * this.pageSizeItems;
    return items.slice(inicio, inicio + this.pageSizeItems);
  }

  get totalPaginasItems(): number {
    return Math.ceil((this.notaCredito?.items?.length || 0) / this.pageSizeItems);
  }

  // --- Paginación despachos ---

  get despachosPaginados(): DespachoNotaDto[] {
    const inicio = this.paginaDespachos * this.pageSizeDespachos;
    return this.despachos.slice(inicio, inicio + this.pageSizeDespachos);
  }

  get totalPaginasDespachos(): number {
    return Math.ceil(this.despachos.length / this.pageSizeDespachos);
  }

  get totalEventosDespacho(): number {
    return Object.keys(this.numeroDespachoPorEvento).length;
  }

  getNumeroDespacho(despacho: DespachoNotaDto): number {
    return this.numeroDespachoPorEvento[this.getClaveEvento(despacho)] || despacho.idDespacho;
  }

  private getClaveEvento(despacho: DespachoNotaDto): string {
    return despacho.idEvento || `despacho-${despacho.idDespacho}`;
  }

  esPrimeraFilaDelEvento(despacho: DespachoNotaDto, index: number): boolean {
    if (index === 0) {
      return true;
    }
    return this.getClaveEvento(this.despachosPaginados[index - 1]) !== this.getClaveEvento(despacho);
  }

  countItemsEvento(idEvento: string): number {
    return this.despachos.filter(d => d.idEvento === idEvento).length;
  }

  imprimirComprobante(idEvento: string): void {
    if (!idEvento) {
      return;
    }
    this.notasService.getComprobanteDespachoPDF(idEvento).subscribe(
      response => {
        const file = new Blob([response.data], { type: 'application/pdf' });
        const fileURL = URL.createObjectURL(file);
        window.open(fileURL);
      }
    );
  }

  toggleSeccion(seccion: string): void {
    if (seccion === 'productos') {
      this.seccionProductos = !this.seccionProductos;
    } else if (seccion === 'despachos') {
      this.seccionDespachos = !this.seccionDespachos;
    }
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

}
