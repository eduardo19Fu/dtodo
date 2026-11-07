import { Component, OnInit } from '@angular/core';

import { AutorizacionProformaDto } from '../../dtos/autorizacion-dto';
import { AutorizacionProformaService } from '../../services/autorizacion-proforma.service';

import { dialogo, escaparHtml, TonoDialogo } from '../../shared/dialogo-swal';
import Swal from 'sweetalert2';

type Resolucion = 'aprobar' | 'rechazar' | 'vencida' | 'aprobarCredito' | 'rechazarCredito';

/**
 * Bandeja de autorizaciones (solo administrador): los descuentos de proformas mayoristas que pasan del tope, los pedidos a
 * crédito que pasan del límite del cliente y las proformas vencidas que alguien quiere facturar. Toda decisión pide un motivo y queda en el historial.
 */
@Component({
  selector: 'app-autorizaciones',
  templateUrl: './autorizaciones.component.html',
  styleUrls: ['./autorizaciones.component.css', '../shared/management-page.css']
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

  private textos(accion: Resolucion, proforma: AutorizacionProformaDto):
    { titulo: string; boton: string; exito: string; icono: string; tono: TonoDialogo; detalle: string } {
    switch (accion) {
      case 'aprobar':
        return { titulo: 'Aprobar el descuento', boton: 'Aprobar', exito: 'Descuento aprobado', icono: 'fa-check-circle',
          tono: 'verde', detalle: 'Proforma' };
      case 'rechazar':
        return { titulo: 'Rechazar el descuento', boton: 'Rechazar', exito: 'Descuento rechazado', icono: 'fa-times-circle',
          tono: 'rojo', detalle: 'Proforma' };
      case 'aprobarCredito':
        return { titulo: 'Autorizar el crédito', boton: 'Autorizar', exito: 'Crédito autorizado', icono: 'fa-hand-holding-usd',
          tono: 'verde', detalle: 'Pedido' };
      case 'rechazarCredito':
        return { titulo: 'Rechazar el crédito', boton: 'Rechazar', exito: 'Crédito rechazado', icono: 'fa-ban',
          tono: 'rojo', detalle: 'Pedido' };
      default:
        return { titulo: 'Autorizar facturar la proforma vencida', boton: 'Autorizar', exito: 'Vencimiento autorizado',
          icono: 'fa-hourglass-end', tono: 'azul', detalle: 'Proforma' };
    }
  }

  /** Tarjeta con los datos que el administrador necesita para decidir. */
  private resumen(proforma: AutorizacionProformaDto, etiqueta: string): string {
    const moneda = (valor: number) => 'Q' + Number(valor || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    const filas: string[] = [
      `<div class="ancho"><span>Cliente</span><strong>${escaparHtml(proforma.cliente)}</strong></div>`,
      `<div><span>Total</span><strong>${moneda(proforma.total)}</strong></div>`,
      `<div><span>Vendedor</span><strong>${escaparHtml(proforma.vendedor)}</strong></div>`
    ];
    if (proforma.tipo === 'CREDITO') {
      filas.push(`<div><span>Límite de crédito</span><strong>${moneda(proforma.limiteCredito)}</strong></div>`);
      filas.push(`<div><span>Pasa del límite por</span><strong>${moneda(proforma.excesoCredito)}</strong></div>`);
    } else if (proforma.tipo === 'DESCUENTO' && proforma.descuentoMaximo !== null && proforma.descuentoMaximo !== undefined) {
      filas.push(`<div><span>Descuento máximo</span><strong>${proforma.descuentoMaximo} %</strong></div>`);
    } else if (proforma.tipo === 'VENCIDA' && proforma.fechaVencimiento) {
      filas.push(`<div><span>Venció el</span><strong>${escaparHtml(proforma.fechaVencimiento)}</strong></div>`);
    }
    return `<p class="mb-2">${etiqueta} <strong>${escaparHtml(proforma.noProforma)}</strong></p><div class="app-dialog-resumen">${filas.join('')}</div>`;
  }

  private resolver(proforma: AutorizacionProformaDto, accion: Resolucion): void {
    const textos = this.textos(accion, proforma);
    dialogo({
      iconoFa: textos.icono,
      tono: textos.tono,
      title: textos.titulo,
      html: this.resumen(proforma, textos.detalle),
      input: 'textarea',
      inputLabel: 'Motivo (obligatorio, queda en el historial)',
      inputPlaceholder: 'Explica por qué se toma esta decisión',
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
