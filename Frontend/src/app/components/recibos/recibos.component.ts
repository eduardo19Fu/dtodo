import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { ClienteDto } from '../../dtos/cliente-dto';
import { CrearReciboCajaDto, FormaPagoRecibo, ReciboCajaDto } from '../../dtos/recibo-caja-dto';
import { Sucursal } from '../../models/sucursal';
import { AuthService } from '../../services/auth.service';
import { ClienteService } from '../../services/cliente.service';
import { ReciboCajaService } from '../../services/recibo-caja.service';
import { SaldoService } from '../../services/saldo.service';
import { SucursalService } from '../../services/sucursal.service';
import { SaldoClienteDto } from '../../dtos/saldo-dto';

import Swal from 'sweetalert2';

/**
 * Recibos de caja: abonos y depósitos de los clientes. Los emiten el administrador y el cobrador desde su sucursal (el
 * administrador puede elegir otra); solo el administrador anula, con motivo. La lista muestra los de los últimos 30 días.
 */
@Component({
  selector: 'app-recibos',
  templateUrl: './recibos.component.html',
  styleUrls: ['./recibos.component.css']
})
export class RecibosComponent implements OnInit, OnDestroy {

  title = 'Recibos de caja';
  readonly formas: { valor: FormaPagoRecibo; etiqueta: string }[] = [
    { valor: 'EFECTIVO', etiqueta: 'Efectivo' },
    { valor: 'DEPOSITO', etiqueta: 'Depósito' },
    { valor: 'TRANSFERENCIA', etiqueta: 'Transferencia' }
  ];

  // ---- formulario de emisión
  formularioAbierto = false;
  busquedaCliente = '';
  clientesEncontrados: ClienteDto[] = [];
  clienteElegido: ClienteDto = null;
  /** Estado de cuenta del cliente elegido, para ver su saldo y escoger el pedido al que paga. */
  saldo: SaldoClienteDto = null;
  idProformaElegida: number = null;
  monto: number = null;
  formaPago: FormaPagoRecibo = 'EFECTIVO';
  referencia = '';
  observaciones = '';
  fechaPago = '';
  idSucursalEmision: number = null;
  emitiendo = false;

  // ---- lista
  recibos: ReciboCajaDto[] = [];
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
  incluirAnulados = false;
  idSucursalFiltro: number = null;
  idClienteFiltro: number = null;

  private clienteSubject = new Subject<string>();
  private clienteSubscription: Subscription;

  constructor(
    private reciboService: ReciboCajaService,
    private clienteService: ClienteService,
    private sucursalService: SucursalService,
    public auth: AuthService,
    private saldoService: SaldoService
  ) { }

  ngOnInit(): void {
    this.fechaPago = this.hoy();
    this.clienteSubscription = this.clienteSubject.pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(texto => this.buscarClientes(texto));
    if (this.esAdmin) {
      this.sucursalService.getSucursales().subscribe(lista => this.sucursales = lista, () => this.sucursales = []);
    }
    this.cargar(0);
  }

  ngOnDestroy(): void {
    if (this.clienteSubscription) {
      this.clienteSubscription.unsubscribe();
    }
  }

  get esAdmin(): boolean {
    return this.auth.hasRole('ROLE_ADMIN');
  }

  /** Hoy en formato yyyy-MM-dd, el máximo de la fecha de pago. */
  get hoyIso(): string {
    return this.hoy();
  }

  private hoy(): string {
    const ahora = new Date();
    const mes = String(ahora.getMonth() + 1).padStart(2, '0');
    const dia = String(ahora.getDate()).padStart(2, '0');
    return `${ahora.getFullYear()}-${mes}-${dia}`;
  }

  // ---- lista

  cargar(page: number): void {
    this.cargando = true;
    this.errorCarga = false;
    this.reciboService.getListado(page, this.pageSize, {
      idSucursal: this.idSucursalFiltro, idCliente: this.idClienteFiltro, desde: this.desde, hasta: this.hasta,
      incluirAnulados: this.incluirAnulados
    }).subscribe(respuesta => {
      this.recibos = respuesta.content;
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

  irPaginaAnterior(): void { if (!this.isFirst) { this.cargar(this.paginaActual - 1); } }
  irPaginaSiguiente(): void { if (!this.isLast) { this.cargar(this.paginaActual + 1); } }

  // ---- emisión

  abrirFormulario(): void {
    this.formularioAbierto = true;
  }

  cancelarFormulario(): void {
    this.formularioAbierto = false;
    this.limpiarFormulario();
  }

  private limpiarFormulario(): void {
    this.busquedaCliente = '';
    this.clientesEncontrados = [];
    this.clienteElegido = null;
    this.saldo = null;
    this.idProformaElegida = null;
    this.monto = null;
    this.formaPago = 'EFECTIVO';
    this.referencia = '';
    this.observaciones = '';
    this.fechaPago = this.hoy();
    this.idSucursalEmision = null;
  }

  onBuscarCliente(texto: string): void {
    this.busquedaCliente = texto;
    this.clienteElegido = null;
    this.saldo = null;
    this.idProformaElegida = null;
    this.clienteSubject.next((texto || '').trim());
  }

  private buscarClientes(texto: string): void {
    if (texto.length < 2) {
      this.clientesEncontrados = [];
      return;
    }
    this.clienteService.getListado(0, 8, texto, 'nombre', 'asc').subscribe(
      respuesta => this.clientesEncontrados = respuesta.content,
      () => this.clientesEncontrados = []
    );
  }

  elegirCliente(cliente: ClienteDto): void {
    this.clienteElegido = cliente;
    this.busquedaCliente = cliente.nombre;
    this.clientesEncontrados = [];
    this.saldo = null;
    this.idProformaElegida = null;
    this.saldoService.cliente(cliente.idCliente).subscribe(saldo => {
      if (this.clienteElegido && this.clienteElegido.idCliente === cliente.idCliente) {
        this.saldo = saldo;
      }
    }, () => this.saldo = null);
  }

  /** Lo que falta cobrar de un pedido, para ofrecerlo como monto: lo entregado sin pagar o, si no se ha entregado, su total. */
  sugerirMonto(idProforma: number): void {
    const pedido = this.saldo && this.saldo.pedidos.find(p => p.idProforma === idProforma);
    if (!pedido) {
      return;
    }
    const porCobrar = pedido.condicionPago === 'CONTADO' && pedido.cobrado < pedido.total
      ? Number(pedido.total) - Number(pedido.cobrado) : Number(pedido.pendiente);
    this.monto = Math.max(0, Math.round(porCobrar * 100) / 100) || this.monto;
  }

  get pideReferencia(): boolean {
    return this.formaPago !== 'EFECTIVO';
  }

  /** Por qué todavía no se puede emitir, o texto vacío si el formulario está completo. */
  get faltante(): string {
    if (!this.clienteElegido) {
      return 'Elige el cliente.';
    }
    if (!(Number(this.monto) > 0)) {
      return 'Indica el monto.';
    }
    if (this.pideReferencia && !this.referencia.trim()) {
      return 'Indica el número de referencia.';
    }
    return '';
  }

  emitir(): void {
    if (this.faltante || this.emitiendo) {
      return;
    }
    const solicitud: CrearReciboCajaDto = {
      idCliente: this.clienteElegido.idCliente,
      idProforma: this.idProformaElegida || null,
      monto: Number(this.monto),
      formaPago: this.formaPago,
      referencia: this.referencia.trim() || null,
      observaciones: this.observaciones.trim() || null,
      fechaPago: this.fechaPago || null,
      idSucursal: this.esAdmin ? this.idSucursalEmision : null
    };
    this.emitiendo = true;
    this.reciboService.emitir(solicitud).subscribe(recibo => {
      this.emitiendo = false;
      this.formularioAbierto = false;
      this.limpiarFormulario();
      Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: `Recibo ${recibo.numeroVisible} emitido`,
        showConfirmButton: false, timer: 2500 });
      this.cargar(0);
      this.imprimir(recibo);
    }, error => {
      this.emitiendo = false;
      Swal.fire('No se pudo emitir el recibo', this.mensaje(error), 'error');
    });
  }

  // ---- impresión y anulación

  imprimir(recibo: ReciboCajaDto): void {
    this.reciboService.imprimir(recibo.idRecibo).subscribe(respuesta => {
      const url = window.URL.createObjectURL(respuesta.body);
      window.open(url, '_blank');
      window.setTimeout(() => window.URL.revokeObjectURL(url), 60000);
    }, error => Swal.fire('No se pudo imprimir', this.mensaje(error), 'error'));
  }

  anular(recibo: ReciboCajaDto): void {
    Swal.fire({
      title: `Anular el recibo ${recibo.numeroVisible}`,
      text: 'El recibo deja de contar para el saldo del cliente.',
      input: 'textarea',
      inputLabel: 'Motivo (obligatorio, queda registrado)',
      inputAttributes: { maxlength: '500' },
      showCancelButton: true,
      confirmButtonText: 'Anular',
      cancelButtonText: 'Cancelar',
      inputValidator: valor => !valor || !valor.trim() ? 'Indica el motivo.' : null
    }).then(resultado => {
      if (!resultado.isConfirmed) {
        return;
      }
      this.reciboService.anular(recibo.idRecibo, String(resultado.value).trim()).subscribe(() => {
        Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: 'Recibo anulado', showConfirmButton: false, timer: 2500 });
        this.cargar(this.paginaActual);
      }, error => Swal.fire('No se pudo anular', this.mensaje(error), 'error'));
    });
  }

  etiquetaForma(forma: FormaPagoRecibo): string {
    const encontrada = this.formas.find(f => f.valor === forma);
    return encontrada ? encontrada.etiqueta : forma;
  }

  trackRecibo(_: number, recibo: ReciboCajaDto): number {
    return recibo.idRecibo;
  }

  private mensaje(error: any): string {
    return error?.error?.message || error?.error?.mensaje || 'Ha ocurrido un error inesperado';
  }
}
