import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { ClienteDto } from '../../dtos/cliente-dto';
import {
  CrearDevolucionClienteDto, DestinoDevolucion, DevolucionClienteDto, PedidoDevolubleDto
} from '../../dtos/devolucion-cliente-dto';
import { Sucursal } from '../../models/sucursal';
import { ClienteService } from '../../services/cliente.service';
import { DevolucionClienteService } from '../../services/devolucion-cliente.service';
import { SucursalService } from '../../services/sucursal.service';

import Swal from 'sweetalert2';

/**
 * Devoluciones de clientes (solo administrador): lo que un cliente devuelve de lo ya entregado de un pedido mayorista. Se
 * registra y queda aprobada en un solo paso. Cada línea reingresa al inventario de una sucursal o se da de baja; el valor
 * devuelto baja lo que debe el cliente y, si el pedido está facturado, la devolución se liga a una nota de crédito.
 */
@Component({
  selector: 'app-devoluciones-cliente',
  templateUrl: './devoluciones-cliente.component.html',
  styleUrls: ['./devoluciones-cliente.component.css']
})
export class DevolucionesClienteComponent implements OnInit, OnDestroy {

  title = 'Devoluciones de clientes';
  readonly destinos: Array<{ valor: DestinoDevolucion; etiqueta: string }> = [
    { valor: 'REINGRESO', etiqueta: 'Reingresa al inventario' },
    { valor: 'BAJA', etiqueta: 'Baja (merma)' }
  ];

  // ---- formulario
  formularioAbierto = false;
  busquedaCliente = '';
  clientesEncontrados: ClienteDto[] = [];
  clienteElegido: ClienteDto = null;
  pedidos: PedidoDevolubleDto[] = [];
  cargandoPedidos = false;
  idProformaElegida: number = null;
  cantidades: { [idDetalle: number]: number } = {};
  destinoDe: { [idDetalle: number]: DestinoDevolucion } = {};
  motivo = '';
  fecha = '';
  idSucursal: number = null;
  idNotaCredito: number = null;
  sucursales: Sucursal[] = [];
  registrando = false;

  // ---- lista
  devoluciones: DevolucionClienteDto[] = [];
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
  /** Devolución con el detalle abierto. */
  abierta: DevolucionClienteDto = null;

  private clienteSubject = new Subject<string>();
  private clienteSubscription: Subscription;

  constructor(
    private devolucionService: DevolucionClienteService,
    private clienteService: ClienteService,
    private sucursalService: SucursalService
  ) { }

  ngOnInit(): void {
    this.fecha = this.hoy();
    this.clienteSubscription = this.clienteSubject.pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(texto => this.buscarClientes(texto));
    this.sucursalService.getSucursales().subscribe(lista => this.sucursales = lista, () => this.sucursales = []);
    this.cargar(0);
  }

  ngOnDestroy(): void {
    if (this.clienteSubscription) {
      this.clienteSubscription.unsubscribe();
    }
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
    this.devolucionService.getListado(page, this.pageSize, { desde: this.desde, hasta: this.hasta }).subscribe(respuesta => {
      this.devoluciones = respuesta.content;
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
    this.cargar(0);
  }

  irPaginaAnterior(): void { if (!this.isFirst) { this.cargar(this.paginaActual - 1); } }
  irPaginaSiguiente(): void { if (!this.isLast) { this.cargar(this.paginaActual + 1); } }

  /** Abre o cierra el detalle de una devolución (trae sus líneas la primera vez). */
  alternarDetalle(devolucion: DevolucionClienteDto): void {
    if (this.abierta && this.abierta.idDevolucion === devolucion.idDevolucion) {
      this.abierta = null;
      return;
    }
    this.devolucionService.buscar(devolucion.idDevolucion).subscribe(
      detalle => this.abierta = detalle,
      error => Swal.fire('No se pudo cargar la devolución', this.mensaje(error), 'error')
    );
  }

  // ---- formulario

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
    this.pedidos = [];
    this.limpiarPedido();
    this.motivo = '';
    this.fecha = this.hoy();
  }

  private limpiarPedido(): void {
    this.idProformaElegida = null;
    this.cantidades = {};
    this.destinoDe = {};
    this.idSucursal = null;
    this.idNotaCredito = null;
  }

  onBuscarCliente(texto: string): void {
    this.busquedaCliente = texto;
    this.clienteElegido = null;
    this.pedidos = [];
    this.limpiarPedido();
    this.clienteSubject.next((texto || '').trim());
  }

  private buscarClientes(texto: string): void {
    if (texto.length < 2) {
      this.clientesEncontrados = [];
      return;
    }
    this.clienteService.getListado(0, 8, texto, 'nombre', 'asc', 'MAYORISTA').subscribe(
      respuesta => this.clientesEncontrados = respuesta.content,
      () => this.clientesEncontrados = []
    );
  }

  elegirCliente(cliente: ClienteDto): void {
    this.clienteElegido = cliente;
    this.busquedaCliente = cliente.nombre;
    this.clientesEncontrados = [];
    this.pedidos = [];
    this.limpiarPedido();
    this.cargandoPedidos = true;
    this.devolucionService.pedidosDevolubles(cliente.idCliente).subscribe(pedidos => {
      this.pedidos = pedidos;
      this.cargandoPedidos = false;
    }, error => {
      this.cargandoPedidos = false;
      Swal.fire('No se pudieron cargar los pedidos', this.mensaje(error), 'error');
    });
  }

  get pedido(): PedidoDevolubleDto {
    return this.pedidos.find(p => p.idProforma === this.idProformaElegida) || null;
  }

  /** Al elegir un pedido se parte de cero: sin cantidades, todo reingresa y con la sucursal del pedido. */
  elegirPedido(idProforma: number): void {
    this.idProformaElegida = idProforma;
    this.cantidades = {};
    this.destinoDe = {};
    this.idNotaCredito = null;
    const pedido = this.pedido;
    this.idSucursal = pedido ? pedido.idSucursal || null : null;
    if (pedido) {
      pedido.lineas.forEach(linea => this.destinoDe[linea.idDetalle] = 'REINGRESO');
      if (pedido.facturado && pedido.notasCredito.length === 1) {
        this.idNotaCredito = pedido.notasCredito[0].idNotaCredito;
      }
    }
  }

  cantidadDe(idDetalle: number): number {
    return Number(this.cantidades[idDetalle]) || 0;
  }

  valorDeLinea(linea: { idDetalle: number; precioUnitario: number }): number {
    return Math.round(this.cantidadDe(linea.idDetalle) * Number(linea.precioUnitario) * 100) / 100;
  }

  get total(): number {
    const pedido = this.pedido;
    return pedido ? pedido.lineas.reduce((suma, linea) => suma + this.valorDeLinea(linea), 0) : 0;
  }

  get algunaReingresa(): boolean {
    const pedido = this.pedido;
    return !!pedido && pedido.lineas.some(l => this.cantidadDe(l.idDetalle) > 0 && this.destinoDe[l.idDetalle] === 'REINGRESO');
  }

  /** Por qué todavía no se puede registrar, o texto vacío si el formulario está completo. */
  get faltante(): string {
    const pedido = this.pedido;
    if (!this.clienteElegido) {
      return 'Elige el cliente.';
    }
    if (!pedido) {
      return 'Elige el pedido del que devuelve.';
    }
    for (const linea of pedido.lineas) {
      const cantidad = this.cantidadDe(linea.idDetalle);
      if (!Number.isInteger(cantidad) || cantidad < 0) {
        return `La cantidad de ${linea.producto} no es válida.`;
      }
      if (cantidad > linea.devolvible) {
        return `De ${linea.producto} solo se pueden devolver ${linea.devolvible}.`;
      }
    }
    if (!pedido.lineas.some(l => this.cantidadDe(l.idDetalle) > 0)) {
      return 'Indica la cantidad que devuelve de al menos un producto.';
    }
    if (this.algunaReingresa && !this.idSucursal) {
      return 'Elige la sucursal que recibe lo que reingresa.';
    }
    if (pedido.facturado && !this.idNotaCredito) {
      return pedido.notasCredito.length === 0
        ? 'El pedido está facturado y no tiene una nota de crédito: emítela primero.'
        : 'Elige la nota de crédito a la que se liga la devolución.';
    }
    if (!this.motivo.trim()) {
      return 'Escribe el motivo de la devolución.';
    }
    return '';
  }

  registrar(): void {
    const pedido = this.pedido;
    if (this.faltante || this.registrando || !pedido) {
      return;
    }
    const solicitud: CrearDevolucionClienteDto = {
      idProforma: pedido.idProforma,
      fecha: this.fecha || null,
      motivo: this.motivo.trim(),
      idSucursal: this.algunaReingresa ? this.idSucursal : null,
      idNotaCredito: pedido.facturado ? this.idNotaCredito : null,
      lineas: pedido.lineas.filter(l => this.cantidadDe(l.idDetalle) > 0)
        .map(l => ({ idDetalle: l.idDetalle, cantidad: this.cantidadDe(l.idDetalle), destino: this.destinoDe[l.idDetalle] || 'REINGRESO' }))
    };
    this.registrando = true;
    this.devolucionService.registrar(solicitud).subscribe(devolucion => {
      this.registrando = false;
      this.formularioAbierto = false;
      this.limpiarFormulario();
      Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: `Devolución #${devolucion.idDevolucion} registrada`,
        showConfirmButton: false, timer: 2500 });
      this.cargar(0);
    }, error => {
      this.registrando = false;
      Swal.fire('No se pudo registrar la devolución', this.mensaje(error), 'error');
    });
  }

  /** Anula la devolución con motivo; lo que reingresó sale de nuevo y vuelve a subir lo que debe el cliente. */
  anular(devolucion: DevolucionClienteDto): void {
    Swal.fire({
      title: `Anular la devolución #${devolucion.idDevolucion}`,
      text: 'Lo que reingresó al inventario sale de nuevo y el valor vuelve a sumarse a lo que debe el cliente.',
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
      this.devolucionService.anular(devolucion.idDevolucion, String(resultado.value).trim()).subscribe(() => {
        Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: 'Devolución anulada', showConfirmButton: false, timer: 2500 });
        this.abierta = null;
        this.cargar(this.paginaActual);
      }, error => Swal.fire('No se pudo anular', this.mensaje(error), 'error'));
    });
  }

  etiquetaDestino(destino: DestinoDevolucion): string {
    return destino === 'REINGRESO' ? 'Reingreso' : 'Baja';
  }

  trackDevolucion(_: number, devolucion: DevolucionClienteDto): number {
    return devolucion.idDevolucion;
  }

  private mensaje(error: any): string {
    return error?.error?.message || error?.error?.mensaje || 'Ha ocurrido un error inesperado';
  }
}
