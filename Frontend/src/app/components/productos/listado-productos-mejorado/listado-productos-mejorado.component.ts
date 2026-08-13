import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { Producto } from '../../../models/producto';
import { ProductoDto } from '../../../dtos/productoDto';

import { AuthService } from '../../../services/auth.service';
import { ProductoService } from '../../../services/producto.service';
import { ModalService } from '../../../services/productos/modal.service';

import Swal from 'sweetalert2';

@Component({
  selector: 'app-listado-productos-mejorado',
  templateUrl: './listado-productos-mejorado.component.html',
  styleUrls: ['./listado-productos-mejorado.component.css']
})
export class ListadoProductosMejoradoComponent implements OnInit, OnDestroy {

  title: string;
  productosDto: ProductoDto[];

  public productoSeleccionado: Producto;

  // Paginación
  paginaActual: number = 0;
  totalPaginas: number = 0;
  totalElementos: number = 0;
  pageSize: number = 5;
  pageSizeOptions: number[] = [5, 10, 15, 25, 50];
  isFirst: boolean = true;
  isLast: boolean = false;

  // Búsqueda
  filtro: string = '';
  private busquedaSubject = new Subject<string>();
  private busquedaSubscription: Subscription;

  cargando: boolean = false;

  constructor(
    public modalService: ModalService,
    private productoService: ProductoService,
    public auth: AuthService
  ) {
    this.title = 'Listado de Productos';
  }

  ngOnInit(): void {
    this.cargarProductos(0);
    this.modalService.notificarUpload.subscribe(producto => {
      this.cargarProductos(this.paginaActual);
    });

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

  cambiarPageSize(nuevoSize: number): void {
    this.pageSize = nuevoSize;
    this.cargarProductos(0);
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

  abrirModal(producto: any): void {
    this.productoSeleccionado = producto;
    this.modalService.abrirModal();
  }

}
