import { Component, EventEmitter, OnDestroy, OnInit, Output } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import Swal from 'sweetalert2';

import { Cliente } from '../../../../models/cliente';
import { ClienteService } from '../../../../services/cliente.service';

@Component({
  selector: 'app-modal-buscar-cliente',
  templateUrl: './modal-buscar-cliente.component.html',
  styleUrls: [
    '../../../movimientos-producto/create-movimiento/modal-buscar-producto-movimiento/modal-buscar-producto-movimiento.component.css',
    './modal-buscar-cliente.component.css'
  ]
})
export class ModalBuscarClienteComponent implements OnInit, OnDestroy {

  @Output() cliente = new EventEmitter<Cliente>();

  title = 'Búsqueda de clientes';
  clientes: Cliente[] = [];

  paginaActual = 0;
  totalPaginas = 0;
  totalElementos = 0;
  pageSize = 5;
  pageSizeOptions: number[] = [5, 10, 15, 25];
  isFirst = true;
  isLast = false;

  filtro = '';
  orden = 'nombre';
  direccion = 'asc';
  cargando = false;

  private busquedaSubject = new Subject<string>();
  private busquedaSubscription: Subscription;

  constructor(private clienteService: ClienteService) { }

  ngOnInit(): void {
    this.busquedaSubscription = this.busquedaSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(filtro => {
      this.filtro = filtro.trim();
      this.cargarClientes(0);
    });

    this.cargarClientes(0);
  }

  ngOnDestroy(): void {
    if (this.busquedaSubscription) {
      this.busquedaSubscription.unsubscribe();
    }
  }

  onBuscar(valor: string): void {
    this.busquedaSubject.next(valor);
  }

  cargarClientes(page: number): void {
    this.cargando = true;
    this.clienteService.getListado(page, this.pageSize, this.filtro, this.orden, this.direccion).subscribe(
      response => {
        this.clientes = response.content || [];
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
        Swal.fire('Error al cargar clientes', error.error?.message || 'Ha ocurrido un error inesperado', 'error');
      }
    );
  }

  cambiarPageSize(nuevoSize: number): void {
    this.pageSize = nuevoSize;
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

  irPrimeraPagina(): void {
    if (!this.isFirst) {
      this.cargarClientes(0);
    }
  }

  irUltimaPagina(): void {
    if (!this.isLast) {
      this.cargarClientes(this.totalPaginas - 1);
    }
  }

  irPaginaAnterior(): void {
    if (!this.isFirst) {
      this.cargarClientes(this.paginaActual - 1);
    }
  }

  irPaginaSiguiente(): void {
    if (!this.isLast) {
      this.cargarClientes(this.paginaActual + 1);
    }
  }

  irAPagina(pagina: number): void {
    if (pagina !== this.paginaActual) {
      this.cargarClientes(pagina);
    }
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

  chooseCliente(cliente: Cliente): void {
    this.cliente.emit(cliente);
  }
}
