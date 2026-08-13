import { Component, OnInit, OnDestroy, Output, EventEmitter } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { ProductoService } from '../../../../services/producto.service';
import { Producto } from '../../../../models/producto';
import { ProductoDto } from '../../../../dtos/productoDto';

import Swal from 'sweetalert2';

@Component({
  selector: 'app-modal-buscar-producto',
  templateUrl: './modal-buscar-producto.component.html',
  styles: [
  ]
})
export class ModalBuscarProductoComponent implements OnInit, OnDestroy {

  @Output() producto = new EventEmitter<Producto>();

  title: string;
  productosDto: ProductoDto[] = [];

  // Paginación
  paginaActual: number = 0;
  totalPaginas: number = 0;
  totalElementos: number = 0;
  pageSize: number = 5;
  pageSizeOptions: number[] = [5, 10, 15, 25];
  isFirst: boolean = true;
  isLast: boolean = false;

  // Búsqueda
  filtro: string = '';
  private busquedaSubject = new Subject<string>();
  private busquedaSubscription: Subscription;

  cargando: boolean = false;

  constructor(
    private productoService: ProductoService
  ) {
    this.title = 'Búsqueda de Productos';
  }

  ngOnInit(): void {
    this.cargarProductos(0);

    this.busquedaSubscription = this.busquedaSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(filtro => {
      this.filtro = filtro;
      this.cargarProductos(0);
    });
  }

  ngOnDestroy(): void {
    if (this.busquedaSubscription) {
      this.busquedaSubscription.unsubscribe();
    }
  }

  onBuscar(valor: string): void {
    this.busquedaSubject.next(valor);
  }

  cargarProductos(page: number): void {
    this.cargando = true;
    const request = this.filtro
      ? this.productoService.buscarProductosDto(page, this.filtro, this.pageSize)
      : this.productoService.getProductosDtoPaginados(page, this.pageSize);

    request.subscribe(
      response => {
        this.productosDto = response.content;
        this.paginaActual = response.number;
        this.totalPaginas = response.totalPages;
        this.totalElementos = response.totalElements;
        this.pageSize = response.size;
        this.isFirst = response.first;
        this.isLast = response.last;
        this.cargando = false;
      },
      error => {
        console.error(error);
        this.cargando = false;
        Swal.fire('Error al cargar productos', error.error?.message || 'Ha ocurrido un error inesperado', 'error');
      }
    );
  }

  cambiarPageSize(nuevoSize: number): void {
    this.pageSize = nuevoSize;
    this.cargarProductos(0);
  }

  irPrimeraPagina(): void {
    this.cargarProductos(0);
  }

  irUltimaPagina(): void {
    this.cargarProductos(this.totalPaginas - 1);
  }

  irPaginaAnterior(): void {
    if (!this.isFirst) {
      this.cargarProductos(this.paginaActual - 1);
    }
  }

  irPaginaSiguiente(): void {
    if (!this.isLast) {
      this.cargarProductos(this.paginaActual + 1);
    }
  }

  irAPagina(pagina: number): void {
    this.cargarProductos(pagina);
  }

  get paginasVisibles(): number[] {
    const paginas: number[] = [];
    const rango = 2;
    let inicio = Math.max(0, this.paginaActual - rango);
    let fin = Math.min(this.totalPaginas - 1, this.paginaActual + rango);

    if (this.paginaActual - rango < 0) {
      fin = Math.min(this.totalPaginas - 1, fin + (rango - this.paginaActual));
    }
    if (this.paginaActual + rango > this.totalPaginas - 1) {
      inicio = Math.max(0, inicio - (this.paginaActual + rango - (this.totalPaginas - 1)));
    }

    for (let i = inicio; i <= fin; i++) {
      paginas.push(i);
    }
    return paginas;
  }

  chooseProducto(producto: ProductoDto): void {
    this.producto.emit(producto as any);
  }

}
