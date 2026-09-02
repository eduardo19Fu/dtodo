import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { InventarioSucursal } from 'src/app/models/inventario-sucursal';
import { AuthService } from 'src/app/services/auth.service';
import { SucursalService } from 'src/app/services/sucursal.service';
import { InventarioSucursalService } from 'src/app/services/inventario-sucursal.service';

import Swal from 'sweetalert2';

@Component({
  selector: 'app-inventario-sucursal',
  templateUrl: './inventario-sucursal.component.html',
  styleUrls: ['./inventario-sucursal.component.css']
})
export class InventarioSucursalComponent implements OnInit, OnDestroy {

  title = 'Inventario por Sucursal';
  nombreSucursalActiva = '';
  idSucursalActiva: number = null;

  inventario: InventarioSucursal[] = [];
  paginaActual = 0;
  totalPaginas = 0;
  totalElementos = 0;
  pageSize = 10;
  pageSizeOptions: number[] = [10, 25, 50, 100];
  isFirst = true;
  isLast = false;
  filtro = '';
  cargando = false;

  idProductoEditando: number = null;
  stockEdicion: number = null;
  stockMinimoEdicion: number = null;

  private busquedaSubject = new Subject<string>();
  private busquedaSubscription: Subscription;

  constructor(
    private inventarioService: InventarioSucursalService,
    private sucursalService: SucursalService,
    public auth: AuthService
  ) { }

  ngOnInit(): void {
    this.busquedaSubscription = this.busquedaSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(filtro => {
      this.filtro = filtro;
      this.cargarInventario(0);
    });

    this.resolverSucursalActiva();
  }

  ngOnDestroy(): void {
    if (this.busquedaSubscription) {
      this.busquedaSubscription.unsubscribe();
    }
  }

  private resolverSucursalActiva(): void {
    const sucursal = this.auth.usuario?.sucursal;
    if (sucursal) {
      this.idSucursalActiva = sucursal.idSucursal;
      this.nombreSucursalActiva = sucursal.nombre;
      this.cargarInventario(0);
      return;
    }

    this.sucursalService.getPrincipal().subscribe(principal => {
      this.idSucursalActiva = principal.idSucursal;
      this.nombreSucursalActiva = principal.nombre;
      this.cargarInventario(0);
    });
  }

  cargarInventario(page: number): void {
    if (!this.idSucursalActiva) {
      return;
    }
    this.cargando = true;
    this.inventarioService.getListado(this.idSucursalActiva, page, this.pageSize, this.filtro).subscribe(
      response => {
        this.inventario = response.content;
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
        Swal.fire('Error al cargar el inventario',
          error.error?.message || error.error?.mensaje || 'Ha ocurrido un error inesperado', 'error');
      }
    );
  }

  onBuscar(valor: string): void { this.busquedaSubject.next(valor); }

  cambiarPageSize(size: number): void { this.pageSize = size; this.cargarInventario(0); }
  irPrimeraPagina(): void { this.cargarInventario(0); }
  irUltimaPagina(): void { this.cargarInventario(this.totalPaginas - 1); }
  irPaginaAnterior(): void { if (!this.isFirst) { this.cargarInventario(this.paginaActual - 1); } }
  irPaginaSiguiente(): void { if (!this.isLast) { this.cargarInventario(this.paginaActual + 1); } }
  irAPagina(pagina: number): void { this.cargarInventario(pagina); }

  get paginasVisibles(): number[] {
    const paginas: number[] = [];
    const inicio = Math.max(0, Math.min(this.paginaActual - 2, this.totalPaginas - 5));
    const fin = Math.min(this.totalPaginas - 1, inicio + 4);
    for (let pagina = inicio; pagina <= fin; pagina++) {
      paginas.push(pagina);
    }
    return paginas;
  }

  editar(fila: InventarioSucursal): void {
    this.idProductoEditando = fila.idProducto;
    this.stockEdicion = fila.stock;
    this.stockMinimoEdicion = fila.stockMinimo;
  }

  cancelarEdicion(): void {
    this.idProductoEditando = null;
  }

  guardar(fila: InventarioSucursal): void {
    if (this.stockEdicion === null || this.stockEdicion < 0) {
      Swal.fire('Stock inv&aacute;lido', 'Ingresa una cantidad v&aacute;lida.', 'warning');
      return;
    }

    this.inventarioService.ajustarStock(
      this.idSucursalActiva, fila.idProducto, this.stockEdicion, this.stockMinimoEdicion
    ).subscribe(
      () => {
        fila.stock = this.stockEdicion;
        fila.stockMinimo = this.stockMinimoEdicion;
        this.idProductoEditando = null;
        Swal.fire('Stock actualizado', `${fila.nombreProducto} qued&oacute; en ${fila.stock} unidades.`, 'success');
      }
    );
  }

}
