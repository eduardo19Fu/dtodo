import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { RutaDto } from 'src/app/dtos/ruta-dto';
import { ClienteDto } from 'src/app/dtos/cliente-dto';
import { AuthService } from 'src/app/services/auth.service';
import { ClienteService } from 'src/app/services/cliente.service';
import { RutaService } from 'src/app/services/ruta.service';

import Swal from 'sweetalert2';

@Component({
  selector: 'app-clientes',
  templateUrl: './clientes.component.html',
  styleUrls: ['./clientes.component.css']
})
export class ClientesComponent implements OnInit, OnDestroy {

  title = 'Clientes';
  clientes: ClienteDto[] = [];
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
  tipoFiltro = '';
  soloNitInvalido = false;
  totalNitInvalidos: number = null;
  idRutaFiltro: number = null;
  rutas: RutaDto[] = [];

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
    private clienteService: ClienteService,
    private rutaService: RutaService,
    private activatedRoute: ActivatedRoute,
    public auth: AuthService
  ) { }

  ngOnInit(): void {
    // Desde el listado de rutas se llega con ?ruta=ID para ver solo la cartera de esa ruta.
    const ruta = Number(this.activatedRoute.snapshot.queryParamMap.get('ruta'));
    if (ruta) {
      this.idRutaFiltro = ruta;
      this.tipoFiltro = 'MAYORISTA';
    }
    // El vendedor solo conoce sus rutas; pedirle todas respondería 403 y lo sacaría de la pantalla.
    const rutas$ = this.auth.esVendedor() ? this.rutaService.getMias() : this.rutaService.getActivas();
    rutas$.subscribe(rutas => this.rutas = rutas, () => this.rutas = []);
    if (this.auth.hasRole('ROLE_ADMIN')) {
      this.clienteService.getCantidadNitInvalidos().subscribe(total => this.totalNitInvalidos = total, () => { });
    }
    this.cargarClientes(0);
    this.busquedaSubscription = this.busquedaSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(filtro => {
      this.filtro = filtro;
      this.cargarClientes(0);
    });
  }

  ngOnDestroy(): void {
    if (this.busquedaSubscription) {
      this.busquedaSubscription.unsubscribe();
    }
  }

  cargarClientes(page: number): void {
    this.cargando = true;
    this.clienteService.getListado(page, this.pageSize, this.filtro, this.orden, this.direccion,
      this.tipoFiltro, this.idRutaFiltro, this.soloNitInvalido).subscribe(
      response => {
        this.clientes = response.content;
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
        Swal.fire('Error al cargar clientes',
          error.error?.message || error.error?.mensaje || 'Ha ocurrido un error inesperado', 'error');
      }
    );
  }

  onBuscar(valor: string): void { this.busquedaSubject.next(valor); }

  /** Cambia el tipo o la ruta del filtro; una ruta solo aplica a clientes mayoristas. */
  cambiarFiltros(): void {
    if (this.idRutaFiltro) {
      this.tipoFiltro = 'MAYORISTA';
    }
    this.cargarClientes(0);
  }

  /** Cliente mayorista cuyo detalle está abierto. */
  clienteDetalle: ClienteDto = null;

  abrirDetalle(cliente: ClienteDto): void {
    this.clienteDetalle = cliente;
  }

  cerrarDetalle(): void {
    this.clienteDetalle = null;
  }

  /** Cuántos de los filtros (tipo, ruta y NIT inválido) están puestos. */
  get cantidadFiltrosActivos(): number {
    return (this.tipoFiltro ? 1 : 0) + (this.idRutaFiltro ? 1 : 0) + (this.soloNitInvalido ? 1 : 0);
  }

  get hayFiltrosActivos(): boolean {
    return this.cantidadFiltrosActivos > 0;
  }

  limpiarFiltros(): void {
    this.tipoFiltro = '';
    this.idRutaFiltro = null;
    this.soloNitInvalido = false;
    this.cargarClientes(0);
  }

  ordenarPor(campo: string): void {
    if (this.orden === campo) {
      this.direccion = this.direccion === 'asc' ? 'desc' : 'asc';
    } else {
      this.orden = campo;
      this.direccion = 'asc';
    }
    this.cargarClientes(0);
  }

  iconoOrden(campo: string): string {
    if (this.orden !== campo) {
      return 'fas fa-sort';
    }
    return this.direccion === 'asc' ? 'fas fa-sort-up' : 'fas fa-sort-down';
  }

  cambiarPageSize(size: number): void { this.pageSize = size; this.cargarClientes(0); }
  irPrimeraPagina(): void { this.cargarClientes(0); }
  irUltimaPagina(): void { this.cargarClientes(this.totalPaginas - 1); }
  irPaginaAnterior(): void { if (!this.isFirst) { this.cargarClientes(this.paginaActual - 1); } }
  irPaginaSiguiente(): void { if (!this.isLast) { this.cargarClientes(this.paginaActual + 1); } }
  irAPagina(pagina: number): void { this.cargarClientes(pagina); }

  get paginasVisibles(): number[] {
    const paginas: number[] = [];
    const inicio = Math.max(0, Math.min(this.paginaActual - 2, this.totalPaginas - 5));
    const fin = Math.min(this.totalPaginas - 1, inicio + 4);
    for (let pagina = inicio; pagina <= fin; pagina++) {
      paginas.push(pagina);
    }
    return paginas;
  }

  delete(cliente: ClienteDto): void {
    this.swalWithBootstrapButtons.fire({
      title: '¿Está seguro?',
      text: `¿Seguro que desea eliminar al cliente ${cliente.nombre}?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: '¡Sí, eliminar!',
      cancelButtonText: '¡No, cancelar!',
      reverseButtons: true
    }).then(result => {
      if (!result.isConfirmed) {
        return;
      }
      this.clienteService.delete(cliente.idCliente).subscribe(
        () => {
          this.swalWithBootstrapButtons.fire(
            '¡Cliente eliminado!',
            'El cliente ha sido eliminado con éxito.',
            'success'
          );
          const pagina = this.clientes.length === 1 && this.paginaActual > 0
            ? this.paginaActual - 1 : this.paginaActual;
          this.cargarClientes(pagina);
        },
        error => Swal.fire('Error al eliminar el cliente',
          error.error?.message || error.error?.mensaje || 'Ha ocurrido un error inesperado', 'error')
      );
    });
  }
}
