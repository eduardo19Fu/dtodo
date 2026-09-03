import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { Sucursal } from 'src/app/models/sucursal';
import { SucursalDto } from 'src/app/dtos/sucursal-dto';
import { AuthService } from 'src/app/services/auth.service';
import { SucursalService } from 'src/app/services/sucursal.service';
import { DetailSucursalService } from 'src/app/services/sucursales/detail-sucursal.service';

import Swal from 'sweetalert2';

@Component({
  selector: 'app-sucursales',
  templateUrl: './sucursales.component.html',
  styleUrls: ['./sucursales.component.css']
})
export class SucursalesComponent implements OnInit, OnDestroy {

  title = 'Sucursales';
  sucursales: SucursalDto[] = [];
  sucursalSeleccionada: Sucursal;
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
    private sucursalService: SucursalService,
    private detailSucursalService: DetailSucursalService,
    public auth: AuthService
  ) { }

  ngOnInit(): void {
    this.cargarSucursales(0);
    this.busquedaSubscription = this.busquedaSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(filtro => {
      this.filtro = filtro;
      this.cargarSucursales(0);
    });
  }

  ngOnDestroy(): void {
    if (this.busquedaSubscription) {
      this.busquedaSubscription.unsubscribe();
    }
  }

  cargarSucursales(page: number): void {
    this.cargando = true;
    this.sucursalService.getListado(page, this.pageSize, this.filtro, this.orden, this.direccion).subscribe(
      response => {
        this.sucursales = response.content;
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
        Swal.fire('Error al cargar sucursales',
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
    this.cargarSucursales(0);
  }

  iconoOrden(campo: string): string {
    if (this.orden !== campo) {
      return 'fas fa-sort';
    }
    return this.direccion === 'asc' ? 'fas fa-sort-up' : 'fas fa-sort-down';
  }

  cambiarPageSize(size: number): void { this.pageSize = size; this.cargarSucursales(0); }
  irPrimeraPagina(): void { this.cargarSucursales(0); }
  irUltimaPagina(): void { this.cargarSucursales(this.totalPaginas - 1); }
  irPaginaAnterior(): void { if (!this.isFirst) { this.cargarSucursales(this.paginaActual - 1); } }
  irPaginaSiguiente(): void { if (!this.isLast) { this.cargarSucursales(this.paginaActual + 1); } }
  irAPagina(pagina: number): void { this.cargarSucursales(pagina); }

  get paginasVisibles(): number[] {
    const paginas: number[] = [];
    const inicio = Math.max(0, Math.min(this.paginaActual - 2, this.totalPaginas - 5));
    const fin = Math.min(this.totalPaginas - 1, inicio + 4);
    for (let pagina = inicio; pagina <= fin; pagina++) {
      paginas.push(pagina);
    }
    return paginas;
  }

  abrirDetalle(sucursal: SucursalDto): void {
    if (this.detalleCargandoId !== null) {
      return;
    }
    this.detalleCargandoId = sucursal.idSucursal;
    this.sucursalService.getSucursal(sucursal.idSucursal).subscribe(
      detalle => {
        this.sucursalSeleccionada = detalle;
        this.detalleCargandoId = null;
        this.detailSucursalService.abrirModal();
      },
      () => {
        this.detalleCargandoId = null;
      }
    );
  }
}
