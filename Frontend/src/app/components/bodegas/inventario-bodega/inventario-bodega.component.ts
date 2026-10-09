import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { Bodega } from 'src/app/models/bodega';
import { PaginacionTabla } from 'src/app/models/auxiliar/paginacion-tabla';
import { InventarioBodegaDto } from 'src/app/dtos/inventario-bodega-dto';
import {
  MovimientoBodegaDto, TIPOS_MOVIMIENTO_BODEGA, TipoMovimientoBodega, TipoMovimientoBodegaInfo
} from 'src/app/dtos/movimiento-bodega-dto';
import { AuthService } from 'src/app/services/auth.service';
import { BodegaService } from 'src/app/services/bodega.service';
import { ModoMovimientoBodega } from '../modal-movimiento-bodega/modal-movimiento-bodega.component';

import Swal from 'sweetalert2';

type VistaInventario = 'existencias' | 'movimientos';

@Component({
  selector: 'app-inventario-bodega',
  templateUrl: './inventario-bodega.component.html',
  styleUrls: ['../bodegas.component.css', './inventario-bodega.component.css']
})
export class InventarioBodegaComponent implements OnInit, OnDestroy {

  bodega: Bodega;
  idBodega: number;
  vista: VistaInventario = 'existencias';

  // Existencias
  items: InventarioBodegaDto[] = [];
  paginacion = new PaginacionTabla(10, [10, 15, 25, 50]);
  filtro = '';
  cargando = false;

  // Movimientos
  movimientos: MovimientoBodegaDto[] = [];
  paginacionMovimientos = new PaginacionTabla(10, [10, 15, 25, 50]);
  filtroMovimientos = '';
  fechaInicio = '';
  fechaFin = '';
  tipoMovimiento: TipoMovimientoBodega | '' = '';
  cargandoMovimientos = false;
  readonly tiposMovimiento = TIPOS_MOVIMIENTO_BODEGA;

  // Modales
  modoMovimiento: ModoMovimientoBodega = null;
  itemMovimiento: InventarioBodegaDto = null;
  importarVisible = false;

  private busquedaSubject = new Subject<string>();
  private busquedaMovimientosSubject = new Subject<string>();
  private suscripciones = new Subscription();

  constructor(
    private bodegaService: BodegaService,
    private activatedRoute: ActivatedRoute,
    private auth: AuthService
  ) { }

  ngOnInit(): void {
    this.suscripciones.add(this.activatedRoute.params.subscribe(params => {
      this.idBodega = +params.id;
      this.cargarBodega();
      this.cargarExistencias(0);
    }));
    this.suscripciones.add(this.busquedaSubject.pipe(debounceTime(300), distinctUntilChanged()).subscribe(filtro => {
      this.filtro = filtro;
      this.cargarExistencias(0);
    }));
    this.suscripciones.add(this.busquedaMovimientosSubject.pipe(debounceTime(300), distinctUntilChanged()).subscribe(filtro => {
      this.filtroMovimientos = filtro;
      this.cargarMovimientos(0);
    }));
  }

  ngOnDestroy(): void {
    this.suscripciones.unsubscribe();
  }

  /** Reducir existencias y eliminar productos es una decisión del administrador: el rol Bodega solo suma y despacha. */
  get puedeAjustar(): boolean {
    return this.auth.hasRole('ROLE_ADMIN');
  }

  get activa(): boolean {
    return !!this.bodega && this.bodega.estado?.estado === 'ACTIVO';
  }

  private cargarBodega(): void {
    this.bodegaService.getBodega(this.idBodega).subscribe(bodega => this.bodega = bodega);
  }

  cambiarVista(vista: VistaInventario): void {
    this.vista = vista;
    if (vista === 'movimientos' && this.movimientos.length === 0) {
      this.cargarMovimientos(0);
    }
  }

  /*********** EXISTENCIAS ***********/

  cargarExistencias(page: number): void {
    this.cargando = true;
    this.bodegaService.getInventario(this.idBodega, page, this.paginacion.pageSize, this.filtro).subscribe(
      response => {
        this.items = response.content;
        this.paginacion.actualizar(response);
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
  cambiarPageSize(size: number): void { this.paginacion.pageSize = size; this.cargarExistencias(0); }

  bajoMinimo(item: InventarioBodegaDto): boolean {
    return item.stockMinimo != null && item.stockMinimo > 0 && item.stock <= item.stockMinimo;
  }

  /*********** MOVIMIENTOS ***********/

  cargarMovimientos(page: number): void {
    this.cargandoMovimientos = true;
    this.bodegaService.getMovimientos(this.idBodega, page, this.paginacionMovimientos.pageSize, {
      fechaIni: this.fechaInicio,
      fechaFin: this.fechaFin,
      tipo: this.tipoMovimiento || undefined,
      filtro: this.filtroMovimientos
    }).subscribe(
      response => {
        this.movimientos = response.content;
        this.paginacionMovimientos.actualizar(response);
        this.cargandoMovimientos = false;
      },
      error => {
        this.cargandoMovimientos = false;
        Swal.fire('Error al cargar los movimientos',
          error.error?.message || error.error?.mensaje || 'Ha ocurrido un error inesperado', 'error');
      }
    );
  }

  onBuscarMovimientos(valor: string): void { this.busquedaMovimientosSubject.next(valor); }
  cambiarPageSizeMovimientos(size: number): void { this.paginacionMovimientos.pageSize = size; this.cargarMovimientos(0); }

  /** Aplica el rango de fechas solo cuando están ambas (el backend exige el rango completo). */
  aplicarFiltrosMovimientos(): void {
    if ((this.fechaInicio && !this.fechaFin) || (!this.fechaInicio && this.fechaFin)) {
      return;
    }
    if (this.fechaInicio && this.fechaFin && this.fechaFin < this.fechaInicio) {
      Swal.fire('Rango inválido', 'La fecha final no puede ser anterior a la fecha inicial.', 'warning');
      return;
    }
    this.cargarMovimientos(0);
  }

  limpiarFiltrosMovimientos(): void {
    this.fechaInicio = '';
    this.fechaFin = '';
    this.tipoMovimiento = '';
    this.filtroMovimientos = '';
    this.cargarMovimientos(0);
  }

  get hayFiltrosMovimientos(): boolean {
    return !!(this.fechaInicio || this.fechaFin || this.tipoMovimiento || this.filtroMovimientos);
  }

  infoTipo(tipo: TipoMovimientoBodega): TipoMovimientoBodegaInfo {
    return this.tiposMovimiento.find(info => info.codigo === tipo);
  }

  /** Cantidad con signo para mostrarla: positiva si suma existencias, negativa si las resta. */
  cantidadConSigno(movimiento: MovimientoBodegaDto): string {
    const info = this.infoTipo(movimiento.tipoMovimiento);
    if (movimiento.cantidad === 0) {
      return '0';
    }
    return `${info && info.entrada ? '+' : '-'}${movimiento.cantidad}`;
  }

  /*********** MODALES ***********/

  abrirMovimiento(modo: ModoMovimientoBodega, item: InventarioBodegaDto = null): void {
    this.itemMovimiento = item;
    this.modoMovimiento = modo;
  }

  cerrarMovimiento(): void {
    this.modoMovimiento = null;
    this.itemMovimiento = null;
  }

  movimientoCompletado(): void {
    this.cerrarMovimiento();
    this.recargarTodo();
  }

  abrirImportar(): void {
    this.importarVisible = true;
  }

  cerrarImportar(): void {
    this.importarVisible = false;
  }

  importacionCompletada(): void {
    this.importarVisible = false;
    this.recargarTodo();
  }

  private recargarTodo(): void {
    // Si se quitó el único producto de la última página, se regresa a la página anterior para no mostrarla vacía
    const quedaVacia = this.items.length === 1 && !this.paginacion.isFirst;
    this.cargarExistencias(quedaVacia ? this.paginacion.paginaActual - 1 : this.paginacion.paginaActual);
    if (this.movimientos.length > 0 || this.vista === 'movimientos') {
      this.cargarMovimientos(0);
    }
  }
}
