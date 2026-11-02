import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { RutaDto } from 'src/app/dtos/ruta-dto';
import { AuthService } from 'src/app/services/auth.service';
import { RutaService } from 'src/app/services/ruta.service';

import Swal from 'sweetalert2';

@Component({
  selector: 'app-rutas',
  templateUrl: './rutas.component.html',
  styleUrls: ['./rutas.component.css', '../shared/management-page.css']
})
export class RutasComponent implements OnInit, OnDestroy {

  title = 'Rutas';
  rutas: RutaDto[] = [];
  paginaActual = 0;
  totalPaginas = 0;
  totalElementos = 0;
  pageSize = 10;
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
    private rutaService: RutaService,
    public auth: AuthService
  ) { }

  ngOnInit(): void {
    this.cargarRutas(0);
    this.busquedaSubscription = this.busquedaSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(filtro => {
      this.filtro = filtro;
      this.cargarRutas(0);
    });
  }

  ngOnDestroy(): void {
    if (this.busquedaSubscription) {
      this.busquedaSubscription.unsubscribe();
    }
  }

  cargarRutas(page: number): void {
    this.cargando = true;
    this.rutaService.getListado(page, this.pageSize, this.filtro, this.orden, this.direccion).subscribe(
      response => {
        this.rutas = response.content;
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
        Swal.fire('Error al cargar rutas',
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
    this.cargarRutas(0);
  }

  iconoOrden(campo: string): string {
    if (this.orden !== campo) {
      return 'fas fa-sort';
    }
    return this.direccion === 'asc' ? 'fas fa-sort-up' : 'fas fa-sort-down';
  }

  cambiarPageSize(size: number): void { this.pageSize = size; this.cargarRutas(0); }
  irPrimeraPagina(): void { this.cargarRutas(0); }
  irUltimaPagina(): void { this.cargarRutas(this.totalPaginas - 1); }
  irPaginaAnterior(): void { if (!this.isFirst) { this.cargarRutas(this.paginaActual - 1); } }
  irPaginaSiguiente(): void { if (!this.isLast) { this.cargarRutas(this.paginaActual + 1); } }
  irAPagina(pagina: number): void { this.cargarRutas(pagina); }

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
