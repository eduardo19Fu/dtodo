import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';

import {
  CategoriaHistorial, EntregaDto, EntregaSolicitudDto, EstadoPedido, ETIQUETAS_ESTADO_PEDIDO, HistorialEventoDto, PedidoDetalleDto,
  PedidoDto
} from '../../dtos/pedido-dto';
import { ReporteSelectorOpcionDto } from '../../dtos/reporte-selector-opcion-dto';
import { AuthService } from '../../services/auth.service';
import { PedidoService } from '../../services/pedido.service';
import { SaldoService } from '../../services/saldo.service';
import { PedidoSaldoDto } from '../../dtos/saldo-dto';

import Swal from 'sweetalert2';

interface GrupoRuta {
  idRuta: number | null;
  ruta: string;
  pedidos: PedidoDto[];
  unidades: number;
}

/** Texto del botón que avanza cada etapa, y a cuál pasa. */
const ACCION_AVANCE: { [estado: string]: string } = {
  CONFIRMADA: 'Iniciar picking',
  EN_PREPARACION: 'Marcar empacado',
  EMPACADA: 'Cargar y salir a ruta'
};

/**
 * Pedidos mayoristas por entregar, agrupados por ruta. Bodega y el administrador avanzan las etapas e imprimen los
 * documentos; el vendedor ve solo los suyos. La entrega que descuenta inventario no está en esta pantalla todavía.
 */
@Component({
  selector: 'app-pedidos',
  templateUrl: './pedidos.component.html',
  styleUrls: ['./pedidos.component.css']
})
export class PedidosComponent implements OnInit {

  title = 'Pedidos';
  pedidos: PedidoDto[] = [];
  cargando = false;
  errorCarga = false;
  estado: EstadoPedido | '' = '';
  idRuta: number | '' = '';
  fechaInicio = '';
  fechaFin = '';
  /** Pedido con una acción en curso, para bloquear sus botones y evitar un doble clic. */
  procesando: number = null;
  detalle: PedidoDetalleDto = null;
  cargandoDetalle = false;

  /** Formulario de entrega abierto sobre el detalle. */
  registrandoEntrega = false;
  guardandoEntrega = false;
  errorEntrega = '';
  sucursales: ReporteSelectorOpcionDto[] = [];
  entrega = { idSucursal: null as number, fechaEntrega: '', entregadoPor: '', descontadoAMano: false, observaciones: '',
    entregarSinPago: false };
  /** Cantidad a entregar por línea, por idDetalle. */
  cantidades: { [idDetalle: number]: number } = {};
  private abrirEntregaAlCargar = false;
  private sucursalesCargadas = false;

  readonly estados: Array<{ codigo: EstadoPedido | ''; nombre: string }> = [
    { codigo: '', nombre: 'Por entregar' },
    { codigo: 'CONFIRMADA', nombre: ETIQUETAS_ESTADO_PEDIDO.CONFIRMADA },
    { codigo: 'EN_PREPARACION', nombre: ETIQUETAS_ESTADO_PEDIDO.EN_PREPARACION },
    { codigo: 'EMPACADA', nombre: ETIQUETAS_ESTADO_PEDIDO.EMPACADA },
    { codigo: 'EN_RUTA', nombre: ETIQUETAS_ESTADO_PEDIDO.EN_RUTA }
  ];

  /** Línea de tiempo del pedido abierto; se carga al abrirla. */
  historial: HistorialEventoDto[] = null;
  historialAbierto = false;
  cargandoHistorial = false;
  errorHistorial = false;

  /** Cómo va el cobro del pedido abierto; solo lo consultan quienes pueden ver dinero. */
  cobro: PedidoSaldoDto = null;

  constructor(private pedidoService: PedidoService, public auth: AuthService, private saldoService: SaldoService) { }

  /** Administrador, cobrador y vendedor ven el cobro; bodega y el repartidor no ven dinero. */
  get puedeVerCobro(): boolean {
    return this.auth.hasRole('ROLE_ADMIN') || this.auth.hasRole('ROLE_COBRADOR') || this.auth.hasRole('ROLE_VENDEDOR');
  }

  /** Lo que falta cobrar para poder entregar (solo en un pedido de contado). */
  get faltaCobrar(): number {
    if (!this.cobro || this.cobro.condicionPago !== 'CONTADO') {
      return 0;
    }
    return Math.max(0, Math.round((Number(this.cobro.entregado) - Number(this.cobro.cobrado)) * 100) / 100);
  }

  private cargarCobro(detalle: PedidoDetalleDto): void {
    this.cobro = null;
    if (!this.puedeVerCobro) {
      return;
    }
    this.saldoService.pedido(detalle.idProforma).subscribe(cobro => this.cobro = cobro, () => this.cobro = null);
  }

  ngOnInit(): void {
    this.cargar();
  }

  /** Bodega y el administrador operan las etapas; el vendedor solo consulta. */
  get puedeOperar(): boolean {
    return this.auth.hasRole('ROLE_ADMIN') || this.auth.hasRole('ROLE_BODEGA');
  }

  /** Un repartidor ve solo sus pedidos y registra la entrega con lo mínimo: no decide sobre el inventario. */
  get esRepartidor(): boolean {
    return this.auth.esRepartidor();
  }

  get esAdmin(): boolean {
    return this.auth.hasRole('ROLE_ADMIN');
  }

  cargar(): void {
    this.cargando = true;
    this.errorCarga = false;
    this.pedidoService.listar(this.estado, this.fechaInicio, this.fechaFin).subscribe(pedidos => {
      this.pedidos = pedidos;
      this.cargando = false;
      if (this.idRuta !== '' && !this.rutas.some(ruta => ruta.idRuta === this.idRuta)) {
        this.idRuta = '';
      }
    }, () => {
      this.cargando = false;
      this.errorCarga = true;
    });
  }

  /** Rutas de los pedidos cargados, para el filtro. */
  get rutas(): Array<{ idRuta: number; nombre: string }> {
    const vistas = new Map<number, string>();
    this.pedidos.filter(pedido => pedido.idRuta).forEach(pedido => vistas.set(pedido.idRuta, pedido.ruta));
    return Array.from(vistas, ([idRuta, nombre]) => ({ idRuta, nombre })).sort((a, b) => a.nombre.localeCompare(b.nombre));
  }

  /** Los pedidos de la ruta elegida, agrupados por ruta con su total de unidades. */
  get grupos(): GrupoRuta[] {
    const porRuta = new Map<string, GrupoRuta>();
    this.pedidos
      .filter(pedido => this.idRuta === '' || pedido.idRuta === this.idRuta)
      .forEach(pedido => {
        const clave = String(pedido.idRuta);
        if (!porRuta.has(clave)) {
          porRuta.set(clave, { idRuta: pedido.idRuta || null, ruta: pedido.ruta || 'Sin ruta', pedidos: [], unidades: 0 });
        }
        const grupo = porRuta.get(clave);
        grupo.pedidos.push(pedido);
        grupo.unidades += Number(pedido.unidades) || 0;
      });
    return Array.from(porRuta.values());
  }

  etiquetaEstado(estado: string): string {
    return ETIQUETAS_ESTADO_PEDIDO[estado] || estado;
  }

  textoAvance(pedido: PedidoDto): string {
    return ACCION_AVANCE[pedido.estadoPedido] || '';
  }

  /** Puede avanzar si bodega lo opera, tiene una etapa siguiente y su descuento no está pendiente ni rechazado. */
  puedeAvanzar(pedido: PedidoDto): boolean {
    return this.puedeOperar && !!ACCION_AVANCE[pedido.estadoPedido] && !this.bloqueadoPorAutorizacion(pedido);
  }

  bloqueadoPorAutorizacion(pedido: PedidoDto): boolean {
    return pedido.estadoAutorizacion === 'PENDIENTE' || pedido.estadoAutorizacion === 'RECHAZADA';
  }

  puedeRetroceder(pedido: PedidoDto): boolean {
    return this.esAdmin && pedido.estadoPedido !== 'CONFIRMADA' && pedido.estadoPedido !== 'ENTREGADA'
      && pedido.estadoPedido !== 'ENTREGADA_PARCIAL';
  }

  avanzar(pedido: PedidoDto): void {
    if (pedido.estadoPedido === 'EMPACADA') {
      this.elegirPiloto(`${this.textoAvance(pedido)}: pedido ${pedido.noProforma}`, this.textoAvance(pedido)).then(idRepartidor => {
        if (idRepartidor) {
          this.ejecutar(pedido, this.pedidoService.avanzar(pedido.idProforma, undefined, idRepartidor));
        }
      });
      return;
    }
    Swal.fire({
      title: `${this.textoAvance(pedido)}: pedido ${pedido.noProforma}`,
      input: 'text',
      inputLabel: '¿Quién lo hizo? (opcional, si no tiene usuario)',
      inputAttributes: { maxlength: '150' },
      showCancelButton: true,
      confirmButtonText: this.textoAvance(pedido),
      cancelButtonText: 'Cancelar'
    }).then(resultado => {
      if (resultado.isConfirmed) {
        this.ejecutar(pedido, this.pedidoService.avanzar(pedido.idProforma, String(resultado.value || '').trim()));
      }
    });
  }

  /** Puede cambiar de piloto un pedido que ya salió a ruta (por ejemplo, si el camión falla). */
  puedeCambiarPiloto(pedido: PedidoDto): boolean {
    return this.puedeOperar && (pedido.estadoPedido === 'EN_RUTA' || pedido.estadoPedido === 'ENTREGADA_PARCIAL');
  }

  cambiarPiloto(pedido: PedidoDto): void {
    this.elegirPiloto(`Cambiar el piloto del pedido ${pedido.noProforma}`, 'Cambiar piloto').then(idRepartidor => {
      if (idRepartidor) {
        this.ejecutar(pedido, this.pedidoService.asignarRepartidor(pedido.idProforma, idRepartidor));
      }
    });
  }

  /** Pide elegir un repartidor activo; devuelve su id o {@code null} si se cancela o no hay repartidores. */
  private elegirPiloto(titulo: string, boton: string): Promise<number> {
    return new Promise<number>(resolver => {
      this.pedidoService.repartidores().subscribe(repartidores => {
        if (!repartidores.length) {
          Swal.fire('No hay repartidores', 'Pide al administrador que cree el usuario del piloto con el rol Repartidor.', 'info');
          resolver(null);
          return;
        }
        const opciones: { [id: string]: string } = {};
        repartidores.forEach(repartidor => opciones[String(repartidor.valor)] = repartidor.etiqueta);
        Swal.fire({
          title: titulo,
          input: 'select',
          inputOptions: opciones,
          inputPlaceholder: 'Elige el piloto',
          inputLabel: 'Piloto que lleva el pedido (obligatorio)',
          showCancelButton: true,
          confirmButtonText: boton,
          cancelButtonText: 'Cancelar',
          inputValidator: valor => valor ? null : 'Elige el piloto.'
        }).then(resultado => resolver(resultado.isConfirmed ? Number(resultado.value) : null));
      }, error => {
        Swal.fire('No se pudieron cargar los pilotos', error.error?.message || 'Ha ocurrido un error inesperado', 'error');
        resolver(null);
      });
    });
  }

  retroceder(pedido: PedidoDto): void {
    Swal.fire({
      title: `Regresar el pedido ${pedido.noProforma} a la etapa anterior`,
      input: 'textarea',
      inputLabel: 'Motivo (obligatorio, queda en el historial)',
      inputAttributes: { maxlength: '500' },
      showCancelButton: true,
      confirmButtonText: 'Regresar',
      cancelButtonText: 'Cancelar',
      inputValidator: valor => !valor || !valor.trim() ? 'Indica el motivo.' : null
    }).then(resultado => {
      if (resultado.isConfirmed) {
        this.ejecutar(pedido, this.pedidoService.retroceder(pedido.idProforma, String(resultado.value).trim()));
      }
    });
  }

  private ejecutar(pedido: PedidoDto, llamada: Observable<PedidoDetalleDto>): void {
    this.procesando = pedido.idProforma;
    llamada.subscribe(() => {
      this.procesando = null;
      this.cargar();
    }, error => {
      this.procesando = null;
      Swal.fire('No se pudo actualizar el pedido', error.error?.message || error.error?.mensaje || 'Ha ocurrido un error inesperado',
        'error');
      this.cargar();
    });
  }

  verDetalle(pedido: PedidoDto, abrirEntrega = false): void {
    this.cargandoDetalle = true;
    this.detalle = null;
    this.registrandoEntrega = false;
    this.abrirEntregaAlCargar = abrirEntrega;
    this.pedidoService.detalle(pedido.idProforma).subscribe(detalle => {
      this.detalle = detalle;
      this.reiniciarHistorial();
      this.cargarCobro(detalle);
      this.cargandoDetalle = false;
      if (this.abrirEntregaAlCargar && this.puedeEntregarDetalle(detalle)) {
        this.abrirEntrega();
      }
      this.abrirEntregaAlCargar = false;
    }, error => {
      this.cargandoDetalle = false;
      Swal.fire('No se pudo cargar el pedido', error.error?.message || 'Ha ocurrido un error inesperado', 'error');
    });
  }

  cerrarDetalle(): void {
    this.detalle = null;
    this.registrandoEntrega = false;
    this.reiniciarHistorial();
  }

  private reiniciarHistorial(): void {
    this.historial = null;
    this.historialAbierto = false;
    this.cargandoHistorial = false;
    this.errorHistorial = false;
  }

  /** El repartidor no ve el historial: trae el de auditoría. */
  get puedeVerHistorial(): boolean {
    return !this.esRepartidor;
  }

  /** Abre o cierra la línea de tiempo; la trae del servidor la primera vez. */
  alternarHistorial(): void {
    if (this.historialAbierto) {
      this.historialAbierto = false;
      return;
    }
    this.historialAbierto = true;
    if (this.historial || !this.detalle) {
      return;
    }
    this.cargandoHistorial = true;
    this.errorHistorial = false;
    this.pedidoService.historial(this.detalle.idProforma).subscribe(eventos => {
      this.historial = eventos;
      this.cargandoHistorial = false;
    }, () => {
      this.cargandoHistorial = false;
      this.errorHistorial = true;
    });
  }

  /** Icono de Font Awesome según la clase de hecho. */
  iconoHistorial(categoria: CategoriaHistorial): string {
    const iconos: { [clave: string]: string } = {
      CREACION: 'fa-file-signature', EDICION: 'fa-pen', AUTORIZACION: 'fa-user-check', ETAPA: 'fa-flag-checkered',
      ENTREGA: 'fa-truck', DEVOLUCION: 'fa-undo-alt', FACTURA: 'fa-file-invoice', RECIBO: 'fa-receipt', ANULACION: 'fa-ban'
    };
    return iconos[categoria] || 'fa-circle';
  }

  // ------------------------------------------------------------------ entrega

  /** Administrador, bodega y vendedor (los suyos) registran la entrega de un pedido en ruta o con entrega parcial. */
  puedeEntregar(pedido: PedidoDto): boolean {
    return (this.puedeOperar || this.auth.hasRole('ROLE_VENDEDOR') || this.esRepartidor)
      && (pedido.estadoPedido === 'EN_RUTA' || pedido.estadoPedido === 'ENTREGADA_PARCIAL') && !this.bloqueadoPorAutorizacion(pedido);
  }

  puedeEntregarDetalle(detalle: PedidoDetalleDto): boolean {
    return (this.puedeOperar || this.auth.hasRole('ROLE_VENDEDOR') || this.esRepartidor)
      && (detalle.estadoPedido === 'EN_RUTA' || detalle.estadoPedido === 'ENTREGADA_PARCIAL')
      && detalle.estadoAutorizacion !== 'PENDIENTE' && detalle.estadoAutorizacion !== 'RECHAZADA';
  }

  get hoy(): string {
    return this.formatoFecha(new Date());
  }

  /** La entrega puede ser de hasta 7 días atrás. */
  get fechaMinimaEntrega(): string {
    const fecha = new Date();
    fecha.setDate(fecha.getDate() - 7);
    return this.formatoFecha(fecha);
  }

  private formatoFecha(fecha: Date): string {
    const dos = (n: number) => String(n).padStart(2, '0');
    return `${fecha.getFullYear()}-${dos(fecha.getMonth() + 1)}-${dos(fecha.getDate())}`;
  }

  abrirEntrega(): void {
    if (!this.detalle) {
      return;
    }
    if (!this.esRepartidor) {
      this.cargarSucursales();
    }
    this.errorEntrega = '';
    this.entrega = { idSucursal: this.detalle.idSucursal || null, fechaEntrega: this.hoy, entregadoPor: '',
      descontadoAMano: false, observaciones: '', entregarSinPago: false };
    this.cantidades = {};
    this.detalle.lineas.forEach(linea => this.cantidades[linea.idDetalle] = linea.pendiente);
    this.registrandoEntrega = true;
  }

  cancelarEntrega(): void {
    this.registrandoEntrega = false;
    this.errorEntrega = '';
  }

  private cargarSucursales(): void {
    if (this.sucursalesCargadas) {
      return;
    }
    this.sucursalesCargadas = true;
    this.pedidoService.sucursales().subscribe(opciones => this.sucursales = opciones, () => this.sucursalesCargadas = false);
  }

  /** Lo que se va a entregar, o el primer problema que impide registrarlo. */
  validarEntrega(): string {
    if (!this.esRepartidor && !this.entrega.idSucursal) {
      return 'Elige la sucursal de la que se descuenta el inventario.';
    }
    if (!this.entrega.fechaEntrega || this.entrega.fechaEntrega > this.hoy || this.entrega.fechaEntrega < this.fechaMinimaEntrega) {
      return 'La fecha de entrega debe ser de hoy o de los últimos 7 días.';
    }
    let algunaEntregada = false;
    for (const linea of this.detalle.lineas) {
      const cantidad = Number(this.cantidades[linea.idDetalle]) || 0;
      if (!Number.isInteger(cantidad) || cantidad < 0) {
        return `La cantidad de ${linea.producto} no es válida.`;
      }
      if (cantidad > linea.pendiente) {
        return `De ${linea.producto} solo quedan ${linea.pendiente} por entregar.`;
      }
      algunaEntregada = algunaEntregada || cantidad > 0;
    }
    return algunaEntregada ? '' : 'Indica la cantidad entregada de al menos un producto.';
  }

  confirmarEntrega(): void {
    this.errorEntrega = this.validarEntrega();
    if (this.errorEntrega || this.guardandoEntrega) {
      return;
    }
    const solicitud: EntregaSolicitudDto = {
      idSucursal: this.entrega.idSucursal,
      fechaEntrega: this.entrega.fechaEntrega,
      entregadoPor: this.esRepartidor ? null : this.entrega.entregadoPor.trim() || null,
      descontadoAMano: !this.esRepartidor && this.entrega.descontadoAMano,
      entregarSinPago: this.esAdmin && this.entrega.entregarSinPago,
      observaciones: this.entrega.observaciones.trim() || null,
      lineas: this.detalle.lineas.map(linea => ({ idDetalle: linea.idDetalle, cantidad: Number(this.cantidades[linea.idDetalle]) || 0 }))
        .filter(linea => linea.cantidad > 0)
    };
    this.guardandoEntrega = true;
    this.pedidoService.registrarEntrega(this.detalle.idProforma, solicitud).subscribe(detalle => {
      this.guardandoEntrega = false;
      this.registrandoEntrega = false;
      this.detalle = detalle;
      this.cargar();
    }, error => {
      this.guardandoEntrega = false;
      this.errorEntrega = error.error?.message || error.error?.mensaje || 'No fue posible registrar la entrega.';
    });
  }

  /** Solo administrador: anula una entrega con motivo y reingresa lo que descontó. */
  anularEntrega(entrega: EntregaDto): void {
    this.pedirMotivo(`Anular la entrega #${entrega.idEntrega}`, 'Anular entrega').then(motivo => {
      if (motivo) {
        this.actualizarDetalle(this.pedidoService.anularEntrega(this.detalle.idProforma, entrega.idEntrega, motivo));
      }
    });
  }

  /** Solo administrador: da por entregado un pedido parcial aunque falte parte. */
  cerrarPedido(): void {
    this.pedirMotivo('Cerrar el pedido con lo entregado', 'Cerrar pedido').then(motivo => {
      if (motivo) {
        this.actualizarDetalle(this.pedidoService.cerrarConLoEntregado(this.detalle.idProforma, motivo));
      }
    });
  }

  private pedirMotivo(titulo: string, boton: string): Promise<string> {
    return Swal.fire({
      title: titulo,
      input: 'textarea',
      inputLabel: 'Motivo (obligatorio, queda en el historial)',
      inputAttributes: { maxlength: '500' },
      showCancelButton: true,
      confirmButtonText: boton,
      cancelButtonText: 'Cancelar',
      inputValidator: valor => !valor || !valor.trim() ? 'Indica el motivo.' : null
    }).then(resultado => resultado.isConfirmed ? String(resultado.value).trim() : null);
  }

  private actualizarDetalle(llamada: Observable<PedidoDetalleDto>): void {
    llamada.subscribe(detalle => {
      this.detalle = detalle;
      this.cargar();
    }, error => Swal.fire('No se pudo completar la operación',
      error.error?.message || error.error?.mensaje || 'Ha ocurrido un error inesperado', 'error'));
  }

  imprimirPicking(pedido: PedidoDto): void {
    this.abrirPdf(this.pedidoService.picking(pedido.idProforma));
  }

  imprimirEmpaque(pedido: PedidoDto): void {
    this.abrirPdf(this.pedidoService.empaque(pedido.idProforma));
  }

  imprimirNotaEnvio(grupo: GrupoRuta): void {
    if (!grupo.idRuta) {
      Swal.fire('Pedidos sin ruta', 'La nota de envío se imprime por ruta.', 'info');
      return;
    }
    this.abrirPdf(this.pedidoService.notaEnvio(grupo.idRuta));
  }

  private abrirPdf(origen: Observable<Blob>): void {
    origen.subscribe(blob => {
      const url = window.URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }));
      window.open(url, '_blank');
      window.URL.revokeObjectURL(url);
    }, error => this.mostrarErrorPdf(error));
  }

  private mostrarErrorPdf(error: any): void {
    const mostrar = (detalle: any): void => {
      Swal.fire('No se pudo generar el documento',
        detalle && detalle.message ? detalle.message : 'Ha ocurrido un error inesperado', 'error');
    };
    if (error && error.error instanceof Blob) {
      const lector = new FileReader();
      lector.onload = () => {
        try {
          mostrar(JSON.parse(lector.result as string));
        } catch (_) {
          mostrar({ message: lector.result as string });
        }
      };
      lector.onerror = () => mostrar(null);
      lector.readAsText(error.error);
      return;
    }
    mostrar(error && error.error);
  }
}
