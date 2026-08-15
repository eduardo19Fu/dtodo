import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { CorrelativoDto } from '../../dtos/correlativo-dto';
import { CorrelativoService } from '../../services/correlativos/correlativo.service';
import { AuthService } from '../../services/auth.service';

import Swal from 'sweetalert2';

@Component({
  selector: 'app-correlativos',
  templateUrl: './correlativos.component.html',
  styleUrls: ['./correlativos.component.css']
})
export class CorrelativosComponent implements OnInit, OnDestroy {

  title = 'Listado de Correlativos';
  correlativos: CorrelativoDto[] = [];
  paginaActual = 0;
  totalPaginas = 0;
  totalElementos = 0;
  pageSize = 5;
  pageSizeOptions: number[] = [5, 10, 15, 25, 50];
  isFirst = true;
  isLast = false;
  filtro = '';
  cargando = false;
  orden = 'id';
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
    private correlativoService: CorrelativoService,
    public authService: AuthService
  ) { }

  ngOnInit(): void {
    this.cargarCorrelativos(0);
    this.busquedaSubscription = this.busquedaSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(filtro => {
      this.filtro = filtro;
      this.cargarCorrelativos(0);
    });
  }

  ngOnDestroy(): void {
    if (this.busquedaSubscription) {
      this.busquedaSubscription.unsubscribe();
    }
  }

  cargarCorrelativos(page: number): void {
    this.cargando = true;
    this.correlativoService.getListado(
      page, this.pageSize, this.filtro, this.orden, this.direccion
    ).subscribe(
      response => {
        this.correlativos = response.content;
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
        Swal.fire(
          'Error al cargar correlativos',
          error.error?.message || error.error?.mensaje || 'Ha ocurrido un error inesperado',
          'error'
        );
      }
    );
  }

  onBuscar(valor: string): void {
    this.busquedaSubject.next(valor);
  }

  ordenarPor(campo: string): void {
    if (this.orden === campo) {
      this.direccion = this.direccion === 'asc' ? 'desc' : 'asc';
    } else {
      this.orden = campo;
      this.direccion = 'asc';
    }
    this.cargarCorrelativos(0);
  }

  iconoOrden(campo: string): string {
    if (this.orden !== campo) {
      return 'fas fa-sort';
    }
    return this.direccion === 'asc' ? 'fas fa-sort-up' : 'fas fa-sort-down';
  }

  cambiarPageSize(size: number): void {
    this.pageSize = size;
    this.cargarCorrelativos(0);
  }

  irPrimeraPagina(): void { this.cargarCorrelativos(0); }
  irUltimaPagina(): void { this.cargarCorrelativos(this.totalPaginas - 1); }
  irPaginaAnterior(): void { if (!this.isFirst) { this.cargarCorrelativos(this.paginaActual - 1); } }
  irPaginaSiguiente(): void { if (!this.isLast) { this.cargarCorrelativos(this.paginaActual + 1); } }
  irAPagina(pagina: number): void { this.cargarCorrelativos(pagina); }

  get paginasVisibles(): number[] {
    const paginas: number[] = [];
    const inicio = Math.max(0, Math.min(this.paginaActual - 2, this.totalPaginas - 5));
    const fin = Math.min(this.totalPaginas - 1, inicio + 4);
    for (let pagina = inicio; pagina <= fin; pagina++) {
      paginas.push(pagina);
    }
    return paginas;
  }

  anularCorrelativo(correlativo: CorrelativoDto): void {
    this.swalWithBootstrapButtons.fire({
      title: '¿Está seguro?',
      text: `¿Seguro que desea anular el correlativo ${correlativo.idCorrelativo} del cajero ${correlativo.usuario}?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: '¡Sí, anular!',
      cancelButtonText: '¡No, cancelar!',
      reverseButtons: true
    }).then(result => {
      if (!result.isConfirmed) {
        return;
      }
      this.correlativoService.delete(correlativo.idCorrelativo).subscribe(
        () => {
          this.swalWithBootstrapButtons.fire(
            '¡Anulado!',
            `El correlativo No. ${correlativo.idCorrelativo} ha sido anulado con éxito`,
            'success'
          );
          this.cargarCorrelativos(this.paginaActual);
        },
        error => Swal.fire(
          'Error al anular el correlativo',
          error.error?.message || error.error?.mensaje || 'Ha ocurrido un error inesperado',
          'error'
        )
      );
    });
  }
}
