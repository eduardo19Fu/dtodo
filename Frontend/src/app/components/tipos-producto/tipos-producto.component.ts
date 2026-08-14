import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { TipoProductoDto } from 'src/app/dtos/tipo-producto-dto';
import { AuthService } from 'src/app/services/auth.service';
import { TipoProductoService } from 'src/app/services/tipo-producto.service';

import Swal from 'sweetalert2';

@Component({
  selector: 'app-tipos-producto',
  templateUrl: './tipos-producto.component.html',
  styleUrls: ['./tipos-producto.component.css']
})
export class TiposProductoComponent implements OnInit, OnDestroy {

  title = 'Listado de Categorías';
  tipos: TipoProductoDto[] = [];
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

  swalWithBootstrapButtons = Swal.mixin({
    customClass: {
      confirmButton: 'btn btn-success',
      cancelButton: 'btn btn-danger'
    },
    buttonsStyling: true
  });

  constructor(
    private tipoService: TipoProductoService,
    public auth: AuthService
  ) { }

  ngOnInit(): void {
    this.cargarTipos(0);
    this.busquedaSubscription = this.busquedaSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(filtro => {
      this.filtro = filtro;
      this.cargarTipos(0);
    });
  }

  ngOnDestroy(): void {
    if (this.busquedaSubscription) {
      this.busquedaSubscription.unsubscribe();
    }
  }

  cargarTipos(page: number): void {
    this.cargando = true;
    this.tipoService.getListado(page, this.pageSize, this.filtro, this.orden, this.direccion).subscribe(
      response => {
        this.tipos = response.content;
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
        Swal.fire('Error al cargar categorías',
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
    this.cargarTipos(0);
  }

  iconoOrden(campo: string): string {
    if (this.orden !== campo) {
      return 'fas fa-sort';
    }
    return this.direccion === 'asc' ? 'fas fa-sort-up' : 'fas fa-sort-down';
  }

  cambiarPageSize(size: number): void { this.pageSize = size; this.cargarTipos(0); }
  irPrimeraPagina(): void { this.cargarTipos(0); }
  irUltimaPagina(): void { this.cargarTipos(this.totalPaginas - 1); }
  irPaginaAnterior(): void { if (!this.isFirst) { this.cargarTipos(this.paginaActual - 1); } }
  irPaginaSiguiente(): void { if (!this.isLast) { this.cargarTipos(this.paginaActual + 1); } }
  irAPagina(pagina: number): void { this.cargarTipos(pagina); }

  get paginasVisibles(): number[] {
    const paginas: number[] = [];
    const inicio = Math.max(0, Math.min(this.paginaActual - 2, this.totalPaginas - 5));
    const fin = Math.min(this.totalPaginas - 1, inicio + 4);
    for (let pagina = inicio; pagina <= fin; pagina++) {
      paginas.push(pagina);
    }
    return paginas;
  }

  delete(tipo: TipoProductoDto): void {
    this.swalWithBootstrapButtons.fire({
      title: '¿Está seguro?',
      text: `¿Seguro que desea eliminar ${tipo.tipoProducto}?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: '¡Sí, eliminar!',
      cancelButtonText: '¡No, cancelar!',
      reverseButtons: true
    }).then(result => {
      if (!result.isConfirmed) {
        return;
      }
      this.tipoService.delete(tipo.idTipoProducto).subscribe(
        () => {
          this.swalWithBootstrapButtons.fire('¡Categoría eliminada!',
            'El registro ha sido eliminado con éxito.', 'success');
          const pagina = this.tipos.length === 1 && this.paginaActual > 0
            ? this.paginaActual - 1 : this.paginaActual;
          this.cargarTipos(pagina);
        },
        error => Swal.fire('Error al eliminar la categoría',
          error.error?.message || error.error?.mensaje || 'Ha ocurrido un error inesperado', 'error')
      );
    });
  }
}
