import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { Bodega } from 'src/app/models/bodega';
import { PaginacionTabla } from 'src/app/models/auxiliar/paginacion-tabla';
import { DespachoBodega, ETIQUETAS_ESTADO_DESPACHO, EstadoDespachoBodega } from 'src/app/models/despacho-bodega';
import { DespachoBodegaDto } from 'src/app/dtos/despacho-bodega-dto';
import { AuthService } from 'src/app/services/auth.service';
import { BodegaService } from 'src/app/services/bodega.service';
import { DespachoBodegaService } from 'src/app/services/despacho-bodega.service';

import Swal from 'sweetalert2';

@Component({
  selector: 'app-despachos-bodega',
  templateUrl: './despachos-bodega.component.html',
  styleUrls: ['../bodegas/bodegas.component.css', './despachos-bodega.component.css']
})
export class DespachosBodegaComponent implements OnInit, OnDestroy {

  title = 'Despachos de bodega';
  despachos: DespachoBodegaDto[] = [];
  paginacion = new PaginacionTabla(5);
  filtro = '';
  estadoFiltro: EstadoDespachoBodega | '' = '';
  idBodegaFiltro: number = null;
  bodegas: Bodega[] = [];
  cargando = false;

  despachoSeleccionado: DespachoBodega;
  detalleCargandoId: number = null;
  procesandoId: number = null;

  readonly estados: Array<{ codigo: EstadoDespachoBodega; etiqueta: string }> = [
    { codigo: 'PENDIENTE', etiqueta: ETIQUETAS_ESTADO_DESPACHO.PENDIENTE },
    { codigo: 'REALIZADO', etiqueta: ETIQUETAS_ESTADO_DESPACHO.REALIZADO },
    { codigo: 'CANCELADO', etiqueta: ETIQUETAS_ESTADO_DESPACHO.CANCELADO }
  ];

  private busquedaSubject = new Subject<string>();
  private suscripciones = new Subscription();

  constructor(
    private despachoService: DespachoBodegaService,
    private bodegaService: BodegaService,
    private activatedRoute: ActivatedRoute,
    public auth: AuthService
  ) { }

  ngOnInit(): void {
    const idBodega = this.activatedRoute.snapshot.queryParamMap.get('bodega');
    this.idBodegaFiltro = idBodega ? +idBodega : null;
    this.bodegaService.getBodegas().subscribe(bodegas => this.bodegas = bodegas);
    this.cargarDespachos(0);
    this.suscripciones.add(this.busquedaSubject.pipe(debounceTime(300), distinctUntilChanged()).subscribe(filtro => {
      this.filtro = filtro;
      this.cargarDespachos(0);
    }));
  }

  ngOnDestroy(): void {
    this.suscripciones.unsubscribe();
  }

  get puedeAprobar(): boolean {
    return this.auth.hasRole('ROLE_ADMIN');
  }

  etiquetaEstado(estado: EstadoDespachoBodega): string {
    return ETIQUETAS_ESTADO_DESPACHO[estado] || estado;
  }

  claseEstado(estado: EstadoDespachoBodega): string {
    switch (estado) {
      case 'REALIZADO': return 'listing-status-success';
      case 'PENDIENTE': return 'listing-status-warning';
      default: return 'listing-status-danger';
    }
  }

  numeroDespacho(id: number): string {
    return String(id).padStart(6, '0');
  }

  cargarDespachos(page: number): void {
    this.cargando = true;
    this.despachoService.getListado(page, this.paginacion.pageSize, this.filtro, this.idBodegaFiltro,
      this.estadoFiltro || undefined).subscribe(
      response => {
        this.despachos = response.content;
        this.paginacion.actualizar(response);
        this.cargando = false;
      },
      error => {
        this.cargando = false;
        Swal.fire('Error al cargar despachos',
          error.error?.message || error.error?.mensaje || 'Ha ocurrido un error inesperado', 'error');
      }
    );
  }

  onBuscar(valor: string): void { this.busquedaSubject.next(valor); }
  cambiarPageSize(size: number): void { this.paginacion.pageSize = size; this.cargarDespachos(0); }
  aplicarFiltros(): void { this.cargarDespachos(0); }

  get hayFiltros(): boolean {
    return !!(this.filtro || this.estadoFiltro || this.idBodegaFiltro);
  }

  /*********** DETALLE Y COMPROBANTE ***********/

  abrirDetalle(despacho: DespachoBodegaDto): void {
    if (this.detalleCargandoId !== null) {
      return;
    }
    this.detalleCargandoId = despacho.idDespacho;
    Swal.fire({
      toast: true,
      position: 'top-end',
      icon: 'info',
      title: 'Cargando detalle',
      text: `Preparando el despacho ${this.numeroDespacho(despacho.idDespacho)}...`,
      showConfirmButton: false,
      customClass: { popup: 'app-loading-toast' },
      didOpen: () => Swal.showLoading()
    });
    this.despachoService.getDespacho(despacho.idDespacho).subscribe(
      detalle => {
        this.despachoSeleccionado = detalle;
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
    this.despachoSeleccionado = null;
  }

  imprimir(idDespacho: number): void {
    this.despachoService.imprimirComprobante(idDespacho).subscribe();
  }

  /*********** APROBAR / CANCELAR ***********/

  aprobar(idDespacho: number, sucursalDestino: string): void {
    if (this.procesandoId !== null) {
      return;
    }
    Swal.fire({
      title: '¿Aprobar este despacho?',
      html: `Las existencias del despacho <strong>${this.numeroDespacho(idDespacho)}</strong> ingresarán al inventario de ` +
        `<strong>${sucursalDestino}</strong>. Esta acción no se puede deshacer.`,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Sí, aprobar',
      cancelButtonText: 'Cancelar'
    }).then(resultado => {
      if (!resultado.isConfirmed) {
        return;
      }
      this.procesandoId = idDespacho;
      this.despachoService.aprobar(idDespacho).subscribe(
        () => {
          this.finalizarAccion('Despacho aprobado',
            `El despacho ${this.numeroDespacho(idDespacho)} fue aprobado y el inventario de ${sucursalDestino} se actualizó.`);
        },
        () => this.procesandoId = null
      );
    });
  }

  cancelar(idDespacho: number): void {
    if (this.procesandoId !== null) {
      return;
    }
    Swal.fire({
      title: '¿Cancelar este despacho?',
      html: `Las existencias reservadas del despacho <strong>${this.numeroDespacho(idDespacho)}</strong> regresarán a la bodega.`,
      icon: 'warning',
      input: 'textarea',
      inputLabel: 'Motivo de la cancelación',
      inputPlaceholder: 'Ej. Error en las cantidades, la sucursal ya no lo necesita...',
      inputAttributes: { maxlength: '300' },
      inputValidator: valor => !valor || !valor.trim() ? 'Debes indicar el motivo de la cancelación.' : null,
      showCancelButton: true,
      confirmButtonText: 'Sí, cancelar despacho',
      cancelButtonText: 'Volver'
    }).then(resultado => {
      if (!resultado.isConfirmed) {
        return;
      }
      this.procesandoId = idDespacho;
      this.despachoService.cancelar(idDespacho, resultado.value).subscribe(
        () => {
          this.finalizarAccion('Despacho cancelado',
            `El despacho ${this.numeroDespacho(idDespacho)} fue cancelado y las existencias regresaron a la bodega.`);
        },
        () => this.procesandoId = null
      );
    });
  }

  private finalizarAccion(titulo: string, mensaje: string): void {
    this.procesandoId = null;
    this.despachoSeleccionado = null;
    this.cargarDespachos(this.paginacion.paginaActual);
    Swal.fire(titulo, mensaje, 'success');
  }
}
