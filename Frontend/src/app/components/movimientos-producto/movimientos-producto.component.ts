import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { MovimientoProductoDto } from 'src/app/dtos/movimiento-productoDto';

import { AuthService } from 'src/app/services/auth.service';
import { MovimientosProductoService } from '../../services/movimientos/movimientos-producto.service';

import Swal from 'sweetalert2';

@Component({
  selector: 'app-movimientos-producto',
  templateUrl: './movimientos-producto.component.html',
  styleUrls: ['./movimientos-producto.component.css']
})
export class MovimientosProductoComponent implements OnInit, OnDestroy {

  title: string;
  movimientosDto: MovimientoProductoDto[];

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
  orden: string = 'fecha';
  direccion: 'asc' | 'desc' = 'desc';
  private busquedaSubject = new Subject<string>();
  private busquedaSubscription: Subscription;

  cargando: boolean = false;

  constructor(
    private movimientosProductoService: MovimientosProductoService,
    public authService: AuthService
  ) {
    this.title = 'Movimientos de Productos Creados';
  }

  ngOnInit(): void {
    this.cargarMovimientos(0);

    this.busquedaSubscription = this.busquedaSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(filtro => {
      this.filtro = filtro;
      this.cargarMovimientos(0);
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

  cargarMovimientos(page: number): void {
    this.cargando = true;
    this.movimientosProductoService.getListado(
      page, this.pageSize, this.filtro, this.orden, this.direccion
    ).subscribe(
      response => {
        this.movimientosDto = response.content;
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
        Swal.fire('Error al cargar movimientos', error.error?.message || 'Ha ocurrido un error inesperado', 'error');
      }
    );
  }

  irPrimeraPagina(): void {
    this.cargarMovimientos(0);
  }

  irUltimaPagina(): void {
    this.cargarMovimientos(this.totalPaginas - 1);
  }

  irPaginaAnterior(): void {
    if (!this.isFirst) {
      this.cargarMovimientos(this.paginaActual - 1);
    }
  }

  irPaginaSiguiente(): void {
    if (!this.isLast) {
      this.cargarMovimientos(this.paginaActual + 1);
    }
  }

  irAPagina(pagina: number): void {
    this.cargarMovimientos(pagina);
  }

  cambiarPageSize(nuevoSize: number): void {
    this.pageSize = nuevoSize;
    this.cargarMovimientos(0);
  }

  ordenarPor(campo: string): void {
    if (this.orden === campo) {
      this.direccion = this.direccion === 'asc' ? 'desc' : 'asc';
    } else {
      this.orden = campo;
      this.direccion = 'asc';
    }
    this.cargarMovimientos(0);
  }

  iconoOrden(campo: string): string {
    if (this.orden !== campo) {
      return 'fas fa-sort';
    }
    return this.direccion === 'asc' ? 'fas fa-sort-up' : 'fas fa-sort-down';
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

}
