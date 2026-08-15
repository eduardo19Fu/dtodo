import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { UsuarioDto } from '../../dtos/usuario-dto';
import { UsuarioAuxiliar } from '../../models/auxiliar/usuario-auxiliar';
import { AuthService } from '../../services/auth.service';
import { DetailUsuarioService } from '../../services/usuarios/detail-usuario.service';
import { UsuarioService } from '../../services/usuarios/usuario.service';

import Swal from 'sweetalert2';

@Component({
  selector: 'app-usuarios',
  templateUrl: './usuarios.component.html',
  styleUrls: ['./usuarios.component.css']
})
export class UsuariosComponent implements OnInit, OnDestroy {

  title = 'Listado de Usuarios';
  usuarios: UsuarioDto[] = [];
  usuarioSeleccionado: UsuarioAuxiliar;
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
    private detailUsuarioService: DetailUsuarioService,
    private usuarioService: UsuarioService,
    private authService: AuthService
  ) { }

  ngOnInit(): void {
    this.cargarUsuarios(0);
    this.busquedaSubscription = this.busquedaSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(filtro => {
      this.filtro = filtro;
      this.cargarUsuarios(0);
    });
  }

  ngOnDestroy(): void {
    if (this.busquedaSubscription) {
      this.busquedaSubscription.unsubscribe();
    }
  }

  cargarUsuarios(page: number): void {
    this.cargando = true;
    this.usuarioService.getListado(
      page, this.pageSize, this.filtro, this.orden, this.direccion
    ).subscribe(
      response => {
        this.usuarios = response.content;
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
          'Error al cargar usuarios',
          error.error?.message || error.error?.mensaje || 'Ha ocurrido un error inesperado',
          'error'
        );
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
    this.cargarUsuarios(0);
  }

  iconoOrden(campo: string): string {
    if (this.orden !== campo) {
      return 'fas fa-sort';
    }
    return this.direccion === 'asc' ? 'fas fa-sort-up' : 'fas fa-sort-down';
  }

  cambiarPageSize(size: number): void { this.pageSize = size; this.cargarUsuarios(0); }
  irPrimeraPagina(): void { this.cargarUsuarios(0); }
  irUltimaPagina(): void { this.cargarUsuarios(this.totalPaginas - 1); }
  irPaginaAnterior(): void { if (!this.isFirst) { this.cargarUsuarios(this.paginaActual - 1); } }
  irPaginaSiguiente(): void { if (!this.isLast) { this.cargarUsuarios(this.paginaActual + 1); } }
  irAPagina(pagina: number): void { this.cargarUsuarios(pagina); }

  get paginasVisibles(): number[] {
    const paginas: number[] = [];
    const inicio = Math.max(0, Math.min(this.paginaActual - 2, this.totalPaginas - 5));
    const fin = Math.min(this.totalPaginas - 1, inicio + 4);
    for (let pagina = inicio; pagina <= fin; pagina++) {
      paginas.push(pagina);
    }
    return paginas;
  }

  delete(usuario: UsuarioDto): void {
    this.swalWithBootstrapButtons.fire({
      title: '¿Está seguro?',
      text: `¿Seguro que desea eliminar el usuario ${usuario.usuario}?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: '¡Sí, eliminar!',
      cancelButtonText: '¡No, cancelar!',
      reverseButtons: true
    }).then(result => {
      if (!result.isConfirmed) {
        return;
      }
      this.usuarioService.delete(usuario.idUsuario).subscribe(
        () => {
          if (this.authService.usuario.idUsuario === usuario.idUsuario) {
            this.authService.logout();
            return;
          }
          this.swalWithBootstrapButtons.fire(
            '¡Usuario eliminado!',
            'El registro ha sido eliminado con éxito.',
            'success'
          );
          const pagina = this.usuarios.length === 1 && this.paginaActual > 0
            ? this.paginaActual - 1 : this.paginaActual;
          this.cargarUsuarios(pagina);
        },
        error => Swal.fire(
          'Error al eliminar el usuario',
          error.error?.message || error.error?.mensaje || 'Ha ocurrido un error inesperado',
          'error'
        )
      );
    });
  }

  abrirDetalle(usuario: UsuarioDto): void {
    if (this.detalleCargandoId !== null) {
      return;
    }
    this.detalleCargandoId = usuario.idUsuario;
    this.usuarioService.getUsuario(usuario.idUsuario).subscribe(
      detalle => {
        this.usuarioSeleccionado = detalle;
        this.detalleCargandoId = null;
        this.detailUsuarioService.abrirModal();
      },
      error => {
        this.detalleCargandoId = null;
        Swal.fire(
          'Error al cargar el detalle',
          error.error?.message || error.error?.mensaje || 'Ha ocurrido un error inesperado',
          'error'
        );
      }
    );
  }
}
