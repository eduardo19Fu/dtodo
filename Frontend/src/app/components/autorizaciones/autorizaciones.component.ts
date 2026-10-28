import { Component, OnInit } from '@angular/core';

import { AutorizacionProformaDto } from '../../dtos/autorizacion-dto';
import { AutorizacionProformaService } from '../../services/autorizacion-proforma.service';

import Swal from 'sweetalert2';

type Resolucion = 'aprobar' | 'rechazar' | 'vencida' | 'aprobarCredito' | 'rechazarCredito';

/**
 * Bandeja de autorizaciones (solo administrador): los descuentos de proformas mayoristas que pasan del tope, los pedidos a
 * crédito que pasan del límite del cliente y las proformas vencidas que alguien quiere facturar. Toda decisión pide un motivo y queda en el historial.
 */
@Component({
  selector: 'app-autorizaciones',
  templateUrl: './autorizaciones.component.html',
  styleUrls: ['./autorizaciones.component.css']
})
export class AutorizacionesComponent implements OnInit {

  title = 'Autorizaciones';
  pendientes: AutorizacionProformaDto[] = [];
  vencidas: AutorizacionProformaDto[] = [];
  credito: AutorizacionProformaDto[] = [];
  cargando = false;
  errorCarga = false;
  /** Proforma que se está resolviendo, para bloquear su botón y evitar un doble clic. */
  resolviendo: number = null;

  constructor(private autorizacionService: AutorizacionProformaService) { }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.errorCarga = false;
    let faltan = 3;
    const terminar = () => {
      faltan--;
      if (faltan === 0) {
        this.cargando = false;
      }
    };
    const fallar = () => {
      this.errorCarga = true;
      terminar();
    };
    this.autorizacionService.pendientes().subscribe(lista => { this.pendientes = lista; terminar(); }, fallar);
    this.autorizacionService.vencidas().subscribe(lista => { this.vencidas = lista; terminar(); }, fallar);
    this.autorizacionService.credito().subscribe(lista => { this.credito = lista; terminar(); }, fallar);
  }

  get total(): number {
    return this.pendientes.length + this.vencidas.length + this.credito.length;
  }

  /** Días que lleva vencida (0 si vence hoy o no tiene fecha). */
  diasVencida(proforma: AutorizacionProformaDto): number {
    if (!proforma.fechaVencimiento) {
      return 0;
    }
    const vencimiento = new Date(proforma.fechaVencimiento + 'T00:00:00');
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    return Math.max(0, Math.round((hoy.getTime() - vencimiento.getTime()) / 86400000));
  }

  aprobar(proforma: AutorizacionProformaDto): void {
    this.resolver(proforma, 'aprobar');
  }

  rechazar(proforma: AutorizacionProformaDto): void {
    this.resolver(proforma, 'rechazar');
  }

  aprobarCredito(proforma: AutorizacionProformaDto): void {
    this.resolver(proforma, 'aprobarCredito');
  }

  rechazarCredito(proforma: AutorizacionProformaDto): void {
    this.resolver(proforma, 'rechazarCredito');
  }

  autorizarVencida(proforma: AutorizacionProformaDto): void {
    this.resolver(proforma, 'vencida');
  }

  private textos(accion: Resolucion, proforma: AutorizacionProformaDto): { titulo: string; boton: string; exito: string } {
    switch (accion) {
      case 'aprobar':
        return { titulo: `Aprobar el descuento de la proforma ${proforma.noProforma}`, boton: 'Aprobar',
          exito: 'Descuento aprobado' };
      case 'rechazar':
        return { titulo: `Rechazar el descuento de la proforma ${proforma.noProforma}`, boton: 'Rechazar',
          exito: 'Descuento rechazado' };
      case 'aprobarCredito':
        return { titulo: `Autorizar el crédito del pedido ${proforma.noProforma}`, boton: 'Autorizar',
          exito: 'Crédito autorizado' };
      case 'rechazarCredito':
        return { titulo: `Rechazar el crédito del pedido ${proforma.noProforma}`, boton: 'Rechazar',
          exito: 'Crédito rechazado' };
      default:
        return { titulo: `Autorizar facturar la proforma vencida ${proforma.noProforma}`, boton: 'Autorizar',
          exito: 'Vencimiento autorizado' };
    }
  }

  private resolver(proforma: AutorizacionProformaDto, accion: Resolucion): void {
    const textos = this.textos(accion, proforma);
    Swal.fire({
      title: textos.titulo,
      input: 'textarea',
      inputLabel: 'Motivo (obligatorio, queda en el historial)',
      inputAttributes: { maxlength: '500' },
      showCancelButton: true,
      confirmButtonText: textos.boton,
      cancelButtonText: 'Cancelar',
      inputValidator: valor => !valor || !valor.trim() ? 'Indica el motivo.' : null
    }).then(resultado => {
      if (!resultado.isConfirmed) {
        return;
      }
      const motivo = String(resultado.value).trim();
      this.resolviendo = proforma.idProforma;
      const llamada = accion === 'aprobar' ? this.autorizacionService.aprobar(proforma.idProforma, motivo)
        : accion === 'rechazar' ? this.autorizacionService.rechazar(proforma.idProforma, motivo)
          : accion === 'aprobarCredito' ? this.autorizacionService.aprobarCredito(proforma.idProforma, motivo)
            : accion === 'rechazarCredito' ? this.autorizacionService.rechazarCredito(proforma.idProforma, motivo)
              : this.autorizacionService.autorizarVencida(proforma.idProforma, motivo);
      llamada.subscribe(() => {
        this.resolviendo = null;
        Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: textos.exito,
          showConfirmButton: false, timer: 2500 });
        this.cargar();
      }, error => {
        this.resolviendo = null;
        Swal.fire('No se pudo resolver', error.error?.message || error.error?.mensaje || 'Ha ocurrido un error inesperado',
          'error');
        this.cargar();
      });
    });
  }
}
