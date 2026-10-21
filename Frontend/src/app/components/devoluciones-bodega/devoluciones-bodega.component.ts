import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { Bodega } from 'src/app/models/bodega';
import { PaginacionTabla } from 'src/app/models/auxiliar/paginacion-tabla';
import { DevolucionBodega, ETIQUETAS_ESTADO_DEVOLUCION, EstadoDevolucionBodega } from 'src/app/models/devolucion-bodega';
import { DevolucionBodegaDto } from 'src/app/dtos/devolucion-bodega-dto';
import { AuthService } from 'src/app/services/auth.service';
import { BodegaService } from 'src/app/services/bodega.service';
import { DevolucionBodegaService } from 'src/app/services/devolucion-bodega.service';

import Swal from 'sweetalert2';

@Component({
  selector: 'app-devoluciones-bodega',
  templateUrl: './devoluciones-bodega.component.html',
  styleUrls: [
    '../bodegas/bodegas.component.css',
    '../despachos-bodega/despachos-bodega.component.css',
    './devoluciones-bodega.component.css'
  ]
})
export class DevolucionesBodegaComponent implements OnInit, OnDestroy {

  title = 'Devoluciones a bodega';
  devoluciones: DevolucionBodegaDto[] = [];
  paginacion = new PaginacionTabla(5);
  filtro = '';
  estadoFiltro: EstadoDevolucionBodega | '' = '';
  idBodegaFiltro: number = null;
  bodegas: Bodega[] = [];
  cargando = false;

  devolucionSeleccionada: DevolucionBodega;
  detalleCargandoId: number = null;
  procesandoId: number = null;
  /** Devolución cuyo comprobante se está generando: mientras tanto no se pide otro. */
  imprimiendoId: number = null;

  readonly estados: Array<{ codigo: EstadoDevolucionBodega; etiqueta: string }> = [
    { codigo: 'PENDIENTE', etiqueta: ETIQUETAS_ESTADO_DEVOLUCION.PENDIENTE },
    { codigo: 'REALIZADO', etiqueta: ETIQUETAS_ESTADO_DEVOLUCION.REALIZADO },
    { codigo: 'CANCELADO', etiqueta: ETIQUETAS_ESTADO_DEVOLUCION.CANCELADO }
  ];

  private busquedaSubject = new Subject<string>();
  private suscripciones = new Subscription();

  constructor(
    private devolucionService: DevolucionBodegaService,
    private bodegaService: BodegaService,
    private activatedRoute: ActivatedRoute,
    public auth: AuthService
  ) { }

  ngOnInit(): void {
    const idBodega = this.activatedRoute.snapshot.queryParamMap.get('bodega');
    this.idBodegaFiltro = idBodega ? +idBodega : null;
    this.bodegaService.getBodegas().subscribe(bodegas => this.bodegas = bodegas);
    this.cargarDevoluciones(0);
    this.suscripciones.add(this.busquedaSubject.pipe(debounceTime(300), distinctUntilChanged()).subscribe(filtro => {
      this.filtro = filtro;
      this.cargarDevoluciones(0);
    }));
  }

  ngOnDestroy(): void {
    this.suscripciones.unsubscribe();
  }

  /** El costo y el valor de lo devuelto solo los ve un administrador. */
  get verCostos(): boolean {
    return this.auth.hasRole('ROLE_ADMIN');
  }

  /** Solo un administrador aprueba una devolución. */
  get puedeAprobar(): boolean {
    return this.auth.hasRole('ROLE_ADMIN');
  }

  etiquetaEstado(estado: EstadoDevolucionBodega): string {
    return ETIQUETAS_ESTADO_DEVOLUCION[estado] || estado;
  }

  claseEstado(estado: EstadoDevolucionBodega): string {
    switch (estado) {
      case 'REALIZADO': return 'listing-status-success';
      case 'PENDIENTE': return 'listing-status-warning';
      default: return 'listing-status-danger';
    }
  }

  numeroDevolucion(id: number): string {
    return String(id).padStart(6, '0');
  }

  cargarDevoluciones(page: number): void {
    this.cargando = true;
    this.devolucionService.getListado(page, this.paginacion.pageSize, this.filtro, this.idBodegaFiltro,
      this.estadoFiltro || undefined).subscribe(
      response => {
        this.devoluciones = response.content;
        this.paginacion.actualizar(response);
        this.cargando = false;
      },
      error => {
        this.cargando = false;
        Swal.fire('Error al cargar devoluciones',
          error.error?.message || error.error?.mensaje || 'Ha ocurrido un error inesperado', 'error');
      }
    );
  }

  onBuscar(valor: string): void { this.busquedaSubject.next(valor); }
  cambiarPageSize(size: number): void { this.paginacion.pageSize = size; this.cargarDevoluciones(0); }
  aplicarFiltros(): void { this.cargarDevoluciones(0); }

  get hayFiltros(): boolean {
    return !!(this.filtro || this.estadoFiltro || this.idBodegaFiltro);
  }

  /*********** DETALLE ***********/

  abrirDetalle(devolucion: DevolucionBodegaDto): void {
    if (this.detalleCargandoId !== null) {
      return;
    }
    this.detalleCargandoId = devolucion.idDevolucion;
    Swal.fire({
      toast: true,
      position: 'top-end',
      icon: 'info',
      title: 'Cargando detalle',
      text: `Preparando la devolución ${this.numeroDevolucion(devolucion.idDevolucion)}...`,
      showConfirmButton: false,
      customClass: { popup: 'app-loading-toast' },
      didOpen: () => Swal.showLoading()
    });
    this.devolucionService.getDevolucion(devolucion.idDevolucion).subscribe(
      detalle => {
        this.devolucionSeleccionada = detalle;
        this.detalleCargandoId = null;
        Swal.close();
      },
      () => {
        this.detalleCargandoId = null;
        Swal.close();
      }
    );
  }

  cerrarDetalle(): void {
    this.devolucionSeleccionada = null;
  }

  imprimir(idDevolucion: number): void {
    if (this.imprimiendoId !== null) {
      return;
    }
    this.imprimiendoId = idDevolucion;
    Swal.fire({
      toast: true,
      position: 'top-end',
      icon: 'info',
      title: 'Generando comprobante',
      text: `Preparando la devolución ${this.numeroDevolucion(idDevolucion)}...`,
      showConfirmButton: false,
      customClass: { popup: 'app-loading-toast' },
      didOpen: () => Swal.showLoading()
    });
    this.devolucionService.imprimirComprobante(idDevolucion).subscribe(
      () => this.terminarImpresion(),
      // El servicio ya muestra el motivo del error: aquí solo se quita el aviso de carga
      () => this.terminarImpresion()
    );
  }

  private terminarImpresion(): void {
    this.imprimiendoId = null;
    Swal.close();
  }

  /*********** APROBAR / RECHAZAR ***********/

  aprobar(idDevolucion: number, bodega: string): void {
    if (this.procesandoId !== null) {
      return;
    }
    Swal.fire({
      title: '¿Aprobar esta devolución?',
      html: `Las existencias de la devolución <strong>${this.numeroDevolucion(idDevolucion)}</strong> ingresarán al inventario de ` +
        `<strong>${bodega}</strong>. Esta acción no se puede deshacer.`,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Sí, aprobar',
      cancelButtonText: 'Cancelar'
    }).then(resultado => {
      if (!resultado.isConfirmed) {
        return;
      }
      this.procesandoId = idDevolucion;
      this.devolucionService.aprobar(idDevolucion).subscribe(
        () => this.finalizarAccion('Devolución aprobada',
          `La devolución ${this.numeroDevolucion(idDevolucion)} fue aprobada y el inventario de ${bodega} se actualizó.`),
        () => this.procesandoId = null
      );
    });
  }

  rechazar(idDevolucion: number, sucursal: string): void {
    if (this.procesandoId !== null) {
      return;
    }
    const numero = this.numeroDevolucion(idDevolucion);
    Swal.fire({
      title: this.puedeAprobar ? '¿Rechazar esta devolución?' : '¿Cancelar esta devolución?',
      html: `Las existencias reservadas de la devolución <strong>${numero}</strong> regresarán al inventario de ` +
        `<strong>${sucursal}</strong>.`,
      icon: 'warning',
      input: 'textarea',
      inputLabel: this.puedeAprobar ? 'Motivo del rechazo' : 'Motivo de la cancelación',
      inputPlaceholder: 'Ej. El producto no cumple las condiciones para ingresar a bodega...',
      inputAttributes: { maxlength: '300' },
      inputValidator: valor => !valor || !valor.trim() ? 'Debes indicar el motivo.' : null,
      showCancelButton: true,
      confirmButtonText: this.puedeAprobar ? 'Sí, rechazar devolución' : 'Sí, cancelar devolución',
      cancelButtonText: 'Volver'
    }).then(resultado => {
      if (!resultado.isConfirmed) {
        return;
      }
      this.procesandoId = idDevolucion;
      this.devolucionService.cancelar(idDevolucion, resultado.value).subscribe(
        () => this.finalizarAccion(this.puedeAprobar ? 'Devolución rechazada' : 'Devolución cancelada',
          `La devolución ${numero} fue ${this.puedeAprobar ? 'rechazada' : 'cancelada'} y las existencias regresaron a ${sucursal}.`),
        () => this.procesandoId = null
      );
    });
  }

  private finalizarAccion(titulo: string, mensaje: string): void {
    this.procesandoId = null;
    this.devolucionSeleccionada = null;
    this.cargarDevoluciones(this.paginacion.paginaActual);
    Swal.fire(titulo, mensaje, 'success');
  }
}
