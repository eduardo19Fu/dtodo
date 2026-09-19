import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { ProveedorDto } from 'src/app/dtos/proveedor-dto';
import { AuthService } from 'src/app/services/auth.service';
import { ProveedorService } from 'src/app/services/proveedor.service';
import { DetailProveedorService } from 'src/app/services/proveedores/detail-proveedor.service';

import Swal from 'sweetalert2';

@Component({
  selector: 'app-proveedores',
  templateUrl: './proveedores.component.html',
  styleUrls: ['./proveedores.component.css']
})
export class ProveedoresComponent implements OnInit, OnDestroy {

  title = 'Proveedores';
  proveedores: ProveedorDto[] = [];
  proveedorSeleccionado: ProveedorDto;
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
    private proveedorService: ProveedorService,
    public detailProveedorService: DetailProveedorService,
    public auth: AuthService
  ) { }

  ngOnInit(): void {
    this.cargarProveedores(0);
    this.busquedaSubscription = this.busquedaSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(filtro => {
      this.filtro = filtro;
      this.cargarProveedores(0);
    });
  }

  ngOnDestroy(): void {
    if (this.busquedaSubscription) {
      this.busquedaSubscription.unsubscribe();
    }
  }

  cargarProveedores(page: number): void {
    this.cargando = true;
    this.proveedorService.getListado(page, this.pageSize, this.filtro, this.orden, this.direccion).subscribe(
      response => {
        this.proveedores = response.content;
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
        Swal.fire('Error al cargar proveedores',
          error.error?.message || error.error?.mensaje || 'Ha ocurrido un error inesperado', 'error');
      }
    );
  }

  abrirDetalle(proveedor: ProveedorDto): void {
    this.proveedorSeleccionado = proveedor;
    this.detailProveedorService.abrirModal();
  }

  onBuscar(valor: string): void { this.busquedaSubject.next(valor); }
  ordenarPor(campo: string): void {
    if (this.orden === campo) {
      this.direccion = this.direccion === 'asc' ? 'desc' : 'asc';
    } else {
      this.orden = campo;
      this.direccion = 'asc';
    }
    this.cargarProveedores(0);
  }

  iconoOrden(campo: string): string {
    if (this.orden !== campo) {
      return 'fas fa-sort';
    }
    return this.direccion === 'asc' ? 'fas fa-sort-up' : 'fas fa-sort-down';
  }

  cambiarPageSize(size: number): void { this.pageSize = size; this.cargarProveedores(0); }
  irPrimeraPagina(): void { this.cargarProveedores(0); }
  irUltimaPagina(): void { this.cargarProveedores(this.totalPaginas - 1); }
  irPaginaAnterior(): void { if (!this.isFirst) { this.cargarProveedores(this.paginaActual - 1); } }
  irPaginaSiguiente(): void { if (!this.isLast) { this.cargarProveedores(this.paginaActual + 1); } }
  irAPagina(pagina: number): void { this.cargarProveedores(pagina); }

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
