import { Component, OnInit } from '@angular/core';

import { ChequeDto, EstadoCheque } from '../../dtos/recibo-caja-dto';
import { Sucursal } from '../../models/sucursal';
import { AuthService } from '../../services/auth.service';
import { ChequeService } from '../../services/cheque.service';
import { SucursalService } from '../../services/sucursal.service';

import Swal from 'sweetalert2';

/**
 * Control de cheques: los que los clientes entregan como pago y su seguimiento (recibido, depositado, cobrado o rechazado).
 * Un cheque rechazado anula su recibo y el saldo del cliente vuelve a subir. La lista muestra los de los últimos 30 días.
 */
@Component({
  selector: 'app-cheques',
  templateUrl: './cheques.component.html',
  styleUrls: ['../recibos/recibos.component.css', '../shared/management-page.css', './cheques.component.css']
})
export class ChequesComponent implements OnInit {

  title = 'Control de cheques';
  readonly estados: { valor: EstadoCheque | ''; etiqueta: string }[] = [
    { valor: '', etiqueta: 'Todos los estados' },
    { valor: 'RECIBIDO', etiqueta: 'Recibidos' },
    { valor: 'DEPOSITADO', etiqueta: 'Depositados' },
    { valor: 'COBRADO', etiqueta: 'Cobrados' },
    { valor: 'RECHAZADO', etiqueta: 'Rechazados' }
  ];

  cheques: ChequeDto[] = [];
  sucursales: Sucursal[] = [];
  paginaActual = 0;
  totalPaginas = 0;
  totalElementos = 0;
  pageSize = 10;
  isFirst = true;
  isLast = true;
  cargando = false;
  errorCarga = false;
  desde = '';
  hasta = '';
  estado: EstadoCheque | '' = '';
  idSucursalFiltro: number = null;
  /** Formato del reporte que se está generando, para mostrar el indicador de carga. */
  generando: 'PDF' | 'XLSX' | null = null;
  /** Cheque cuyo cambio de estado está en curso. */
  trabajando: number = null;

  constructor(
    private chequeService: ChequeService,
    private sucursalService: SucursalService,
    public auth: AuthService
  ) { }

  ngOnInit(): void {
    if (this.esAdmin) {
      this.sucursalService.getSucursales().subscribe(lista => this.sucursales = lista, () => this.sucursales = []);
    }
    this.cargar(0);
  }

  get esAdmin(): boolean {
    return this.auth.hasRole('ROLE_ADMIN');
  }

  cargar(page: number): void {
    this.cargando = true;
    this.errorCarga = false;
    this.chequeService.getListado(page, this.pageSize, {
      idSucursal: this.idSucursalFiltro, estado: this.estado, desde: this.desde, hasta: this.hasta
    }).subscribe(respuesta => {
      this.cheques = respuesta.content;
      this.paginaActual = respuesta.number;
      this.totalPaginas = respuesta.totalPages;
      this.totalElementos = respuesta.totalElements;
      this.isFirst = respuesta.first;
      this.isLast = respuesta.last;
      this.cargando = false;
    }, error => {
      this.cargando = false;
      this.errorCarga = true;
      Swal.fire('No se pudo cargar la lista', this.mensaje(error), 'error');
    });
  }

  aplicarFiltros(): void {
    this.cargar(0);
  }

  limpiarFiltros(): void {
    this.desde = '';
    this.hasta = '';
    this.estado = '';
    this.idSucursalFiltro = null;
    this.cargar(0);
  }

  irPaginaAnterior(): void { if (!this.isFirst) { this.cargar(this.paginaActual - 1); } }
  irPaginaSiguiente(): void { if (!this.isLast) { this.cargar(this.paginaActual + 1); } }

  // ---- seguimiento

  /** A qué estados puede pasar un cheque desde el suyo; los finales (cobrado, rechazado) no cambian más. */
  siguientes(cheque: ChequeDto): EstadoCheque[] {
    switch (cheque.estado) {
      case 'RECIBIDO': return ['DEPOSITADO', 'RECHAZADO'];
      case 'DEPOSITADO': return ['COBRADO', 'RECHAZADO'];
      default: return [];
    }
  }

  accion(estado: EstadoCheque): { texto: string; icono: string; clase: string } {
    switch (estado) {
      case 'DEPOSITADO': return { texto: 'Marcar depositado', icono: 'fa-university', clase: 'listing-action-view' };
      case 'COBRADO': return { texto: 'Marcar cobrado', icono: 'fa-check-circle', clase: 'listing-action-edit' };
      default: return { texto: 'Rechazar cheque', icono: 'fa-ban', clase: 'listing-action-delete' };
    }
  }

  /** Un postfechado no se puede depositar antes de su fecha. */
  puedeCambiar(cheque: ChequeDto, estado: EstadoCheque): boolean {
    return this.trabajando === null && !(estado === 'DEPOSITADO' && cheque.postfechado);
  }

  cambiarEstado(cheque: ChequeDto, estado: EstadoCheque): void {
    if (estado === 'RECHAZADO') {
      this.rechazar(cheque);
      return;
    }
    Swal.fire({
      title: estado === 'DEPOSITADO' ? 'Marcar como depositado' : 'Marcar como cobrado',
      text: `Cheque ${cheque.numeroCheque} de ${cheque.banco} por Q${Number(cheque.monto).toFixed(2)}.`,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Confirmar',
      cancelButtonText: 'Cancelar'
    }).then(resultado => {
      if (resultado.isConfirmed) {
        this.enviar(cheque, estado);
      }
    });
  }

  private rechazar(cheque: ChequeDto): void {
    Swal.fire({
      title: 'Rechazar el cheque',
      text: `El recibo ${cheque.numeroRecibo} se anula y el saldo de ${cheque.cliente} vuelve a subir en Q${Number(cheque.monto).toFixed(2)}.`,
      icon: 'warning',
      input: 'textarea',
      inputLabel: 'Motivo (obligatorio, por ejemplo: fondos insuficientes)',
      inputAttributes: { maxlength: '500' },
      showCancelButton: true,
      confirmButtonText: 'Rechazar',
      cancelButtonText: 'Cancelar',
      inputValidator: valor => !valor || !valor.trim() ? 'Indica el motivo.' : null
    }).then(resultado => {
      if (resultado.isConfirmed) {
        this.enviar(cheque, 'RECHAZADO', String(resultado.value).trim());
      }
    });
  }

  private enviar(cheque: ChequeDto, estado: EstadoCheque, motivo?: string): void {
    this.trabajando = cheque.idCheque;
    this.chequeService.cambiarEstado(cheque.idCheque, estado, motivo).subscribe(() => {
      this.trabajando = null;
      Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: 'Cheque actualizado', showConfirmButton: false, timer: 2500 });
      this.cargar(this.paginaActual);
    }, error => {
      this.trabajando = null;
      Swal.fire('No se pudo actualizar el cheque', this.mensaje(error), 'error');
    });
  }

  // ---- reporte

  descargar(formato: 'PDF' | 'XLSX'): void {
    if (this.generando) {
      return;
    }
    const hoy = this.hoy();
    const hasta = this.hasta || hoy;
    const desde = this.desde || this.haceDias(hasta, 30);
    this.generando = formato;
    this.chequeService.reporte(formato, { idSucursal: this.idSucursalFiltro, estado: this.estado, desde, hasta })
      .subscribe(respuesta => {
        this.generando = null;
        const url = window.URL.createObjectURL(respuesta.body);
        if (formato === 'PDF') {
          window.open(url, '_blank');
          window.setTimeout(() => window.URL.revokeObjectURL(url), 60000);
          return;
        }
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = `control_cheques_${desde}_${hasta}.xlsx`;
        document.body.appendChild(enlace);
        enlace.click();
        enlace.remove();
        window.URL.revokeObjectURL(url);
      }, error => {
        this.generando = null;
        Swal.fire('No se pudo generar el reporte', this.mensaje(error), 'error');
      });
  }

  etiquetaEstado(estado: string): string {
    return estado ? estado.charAt(0) + estado.slice(1).toLowerCase() : '';
  }

  claseEstado(estado: string): string {
    switch (estado) {
      case 'COBRADO': return 'listing-status-success';
      case 'RECHAZADO': return 'listing-status-danger';
      case 'DEPOSITADO': return 'listing-status-primary';
      default: return 'listing-status-warning';
    }
  }

  trackCheque(_: number, cheque: ChequeDto): number {
    return cheque.idCheque;
  }

  private hoy(): string {
    return this.iso(new Date());
  }

  private haceDias(fecha: string, dias: number): string {
    const [anio, mes, dia] = fecha.split('-').map(Number);
    return this.iso(new Date(anio, mes - 1, dia - dias));
  }

  private iso(fecha: Date): string {
    return `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}-${String(fecha.getDate()).padStart(2, '0')}`;
  }

  private mensaje(error: any): string {
    return error?.error?.message || error?.error?.mensaje || 'Ha ocurrido un error inesperado';
  }
}
