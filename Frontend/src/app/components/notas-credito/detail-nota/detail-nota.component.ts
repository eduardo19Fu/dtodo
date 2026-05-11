import { Component, Input, OnInit, OnChanges, SimpleChanges } from '@angular/core';

import { NotaCredito } from '../../../models/nota-credito';
import { DespachoNotaDto } from '../../../dtos/despacho-nota-dto';
import { DetailService } from '../../../services/facturas/detail.service';
import { NotasCreditoService } from '../../../services/notas-credito.service';

@Component({
  selector: 'app-detail-nota',
  templateUrl: './detail-nota.component.html',
  styleUrls: ['./detail-nota.component.css']
})
export class DetailNotaComponent implements OnInit, OnChanges {

  title: string;

  @Input() notaCredito: NotaCredito;

  despachos: DespachoNotaDto[] = [];
  despachadoPorProducto: { [idProducto: number]: number } = {};

  // Acordeón
  seccionProductos: boolean = true;
  seccionDespachos: boolean = false;

  // Paginación productos
  paginaItems: number = 0;
  pageSizeItems: number = 5;

  // Paginación despachos
  paginaDespachos: number = 0;
  pageSizeDespachos: number = 5;

  constructor(
    public detailService: DetailService,
    private notasService: NotasCreditoService
  ) {
    this.title = 'Detalle de Nota de Credito ';
  }

  ngOnInit(): void {
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.notaCredito && this.notaCredito?.idNotaCredito) {
      this.cargarDespachos();
    }
  }

  cargarDespachos(): void {
    this.notasService.getDespachos(this.notaCredito.idNotaCredito).subscribe(
      despachos => {
        this.despachos = despachos;
        this.despachadoPorProducto = {};
        despachos.forEach(d => {
          const id = d.idProducto;
          this.despachadoPorProducto[id] = (this.despachadoPorProducto[id] || 0) + d.cantidad;
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

  get itemsPaginados() {
    const inicio = this.paginaItems * this.pageSizeItems;
    return this.notaCredito.items.slice(inicio, inicio + this.pageSizeItems);
  }

  get totalPaginasItems(): number {
    return Math.ceil(this.notaCredito.items.length / this.pageSizeItems);
  }

  // --- Paginación despachos ---

  get despachosPaginados() {
    const inicio = this.paginaDespachos * this.pageSizeDespachos;
    return this.despachos.slice(inicio, inicio + this.pageSizeDespachos);
  }

  get totalPaginasDespachos(): number {
    return Math.ceil(this.despachos.length / this.pageSizeDespachos);
  }

  esPrimeraFilaDelEvento(despacho: DespachoNotaDto, index: number): boolean {
    if (index === 0) {
      return true;
    }
    return this.despachosPaginados[index - 1].idEvento !== despacho.idEvento;
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

}
