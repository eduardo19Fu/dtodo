import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { Bodega } from 'src/app/models/bodega';
import { BodegaDto } from 'src/app/dtos/bodega-dto';
import { AuthService } from 'src/app/services/auth.service';
import { BodegaService } from 'src/app/services/bodega.service';

import Swal from 'sweetalert2';

@Component({
  selector: 'app-bodegas',
  templateUrl: './bodegas.component.html',
  styleUrls: ['./bodegas.component.css']
})
export class BodegasComponent implements OnInit, OnDestroy {

  title = 'Bodegas';
  bodegas: BodegaDto[] = [];
  bodegaSeleccionada: Bodega;
  detalleCargandoId: number = null;
  paginaActual = 0;
  totalPaginas = 0;
  totalElementos = 0;
  pageSize = 5;
  pageSizeOptions: number[] = [5, 10, 15, 25, 50];
  isFirst = true;
  isLast = false;
  filtro = '';
  cargando = false;
  orden = 'nombre';
  direccion: 'asc' | 'desc' = 'asc';

  private busquedaSubject = new Subject<string>();
  private busquedaSubscription: Subscription;

  constructor(
    private bodegaService: BodegaService,
    public auth: AuthService
  ) { }

  ngOnInit(): void {
    this.cargarBodegas(0);
    this.busquedaSubscription = this.busquedaSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(filtro => {
      this.filtro = filtro;
      this.cargarBodegas(0);
    });
  }

  ngOnDestroy(): void {
    if (this.busquedaSubscription) {
      this.busquedaSubscription.unsubscribe();
    }
  }

  cargarBodegas(page: number): void {
    this.cargando = true;
    this.bodegaService.getListado(page, this.pageSize, this.filtro, this.orden, this.direccion).subscribe(
      response => {
        this.bodegas = response.content;
        this.paginaActual = response.number;
        this.totalPaginas = response.totalPages;
        this.totalElementos = response.totalElements;
        this.pageSize = response.size;
        this.isFirst = response.first;
        this.isLast = response.last;
        this.cargando = false;
      },
      error => {
        this.cargando = false;
        Swal.fire('Error al cargar bodegas',
          error.error?.message || error.error?.mensaje || 'Ha ocurrido un error inesperado', 'error');
      }
    );
  }

  onBuscar(valor: string): void { this.busquedaSubject.next(valor); }

  ordenarPor(campo: string): void {
    if (this.orden === campo) {
      this.direccion = this.direccion === 'asc' ? 'desc' : 'asc';
    } else {
      this.orden = campo;
      this.direccion = 'asc';
    }
    this.cargarBodegas(0);
  }

  iconoOrden(campo: string): string {
    if (this.orden !== campo) {
      return 'fas fa-sort';
    }
    return this.direccion === 'asc' ? 'fas fa-sort-up' : 'fas fa-sort-down';
  }

  cambiarPageSize(size: number): void { this.pageSize = size; this.cargarBodegas(0); }
  irPrimeraPagina(): void { this.cargarBodegas(0); }
  irUltimaPagina(): void { this.cargarBodegas(this.totalPaginas - 1); }
  irPaginaAnterior(): void { if (!this.isFirst) { this.cargarBodegas(this.paginaActual - 1); } }
  irPaginaSiguiente(): void { if (!this.isLast) { this.cargarBodegas(this.paginaActual + 1); } }
  irAPagina(pagina: number): void { this.cargarBodegas(pagina); }

  get paginasVisibles(): number[] {
    const paginas: number[] = [];
    const inicio = Math.max(0, Math.min(this.paginaActual - 2, this.totalPaginas - 5));
    const fin = Math.min(this.totalPaginas - 1, inicio + 4);
    for (let pagina = inicio; pagina <= fin; pagina++) {
      paginas.push(pagina);
    }
    return paginas;
  }

  estaActiva(bodega: BodegaDto): boolean {
    return bodega.estado === 'ACTIVO';
  }

  abrirDetalle(bodega: BodegaDto): void {
    if (this.detalleCargandoId !== null) {
      return;
    }
    this.detalleCargandoId = bodega.idBodega;
    Swal.fire({
      toast: true,
      position: 'top-end',
      icon: 'info',
      title: 'Cargando detalle',
      text: `Preparando la bodega ${bodega.nombre}...`,
      showConfirmButton: false,
      customClass: { popup: 'app-loading-toast' },
      didOpen: () => Swal.showLoading()
    });
    this.bodegaService.getBodega(bodega.idBodega).subscribe(
      detalle => {
        this.bodegaSeleccionada = detalle;
        this.detalleCargandoId = null;
        Swal.close();
      },
      () => {
        this.detalleCargandoId = null;
        Swal.close();
      }
    );
  }

  cerrarDetalle(): void {
    this.bodegaSeleccionada = null;
  }
}
