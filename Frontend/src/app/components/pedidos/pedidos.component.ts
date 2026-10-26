import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';

import {
  EstadoPedido, ETIQUETAS_ESTADO_PEDIDO, PedidoDetalleDto, PedidoDto
} from '../../dtos/pedido-dto';
import { AuthService } from '../../services/auth.service';
import { PedidoService } from '../../services/pedido.service';

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

  readonly estados: Array<{ codigo: EstadoPedido | ''; nombre: string }> = [
    { codigo: '', nombre: 'Por entregar' },
    { codigo: 'CONFIRMADA', nombre: ETIQUETAS_ESTADO_PEDIDO.CONFIRMADA },
    { codigo: 'EN_PREPARACION', nombre: ETIQUETAS_ESTADO_PEDIDO.EN_PREPARACION },
    { codigo: 'EMPACADA', nombre: ETIQUETAS_ESTADO_PEDIDO.EMPACADA },
    { codigo: 'EN_RUTA', nombre: ETIQUETAS_ESTADO_PEDIDO.EN_RUTA }
  ];

  constructor(private pedidoService: PedidoService, public auth: AuthService) { }

  ngOnInit(): void {
    this.cargar();
  }

  /** Bodega y el administrador operan las etapas; el vendedor solo consulta. */
  get puedeOperar(): boolean {
    return this.auth.hasRole('ROLE_ADMIN') || this.auth.hasRole('ROLE_BODEGA');
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

  verDetalle(pedido: PedidoDto): void {
    this.cargandoDetalle = true;
    this.detalle = null;
    this.pedidoService.detalle(pedido.idProforma).subscribe(detalle => {
      this.detalle = detalle;
      this.cargandoDetalle = false;
    }, error => {
      this.cargandoDetalle = false;
      Swal.fire('No se pudo cargar el pedido', error.error?.message || 'Ha ocurrido un error inesperado', 'error');
    });
  }

  cerrarDetalle(): void {
    this.detalle = null;
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
