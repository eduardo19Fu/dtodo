import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import Swal from 'sweetalert2';

import { UsuarioDto } from '../../dtos/usuario-dto';
import { ReporteFiltroDto } from '../../dtos/reporte-filtro-dto';
import { CategoriaReporte, ReporteDefinicion } from '../../models/reporte-definicion';
import { Proveedor } from '../../models/proveedor';
import { Sucursal } from '../../models/sucursal';
import { Usuario } from '../../models/usuario';
import { AuthService } from '../../services/auth.service';
import { ReporteService } from '../../services/reporte.service';
import { ProveedorService } from '../../services/proveedor.service';
import { SucursalService } from '../../services/sucursal.service';
import { UsuarioService } from '../../services/usuarios/usuario.service';

interface CategoriaDefinicion {
  codigo: CategoriaReporte;
  titulo: string;
  icono: string;
}

@Component({
  selector: 'app-reportes',
  templateUrl: './reportes.component.html',
  styleUrls: ['./reportes.component.css']
})
export class ReportesComponent implements OnInit, OnDestroy {
  readonly categorias: CategoriaDefinicion[] = [
    { codigo: 'VENTAS', titulo: 'Ventas', icono: 'fa-chart-line' },
    { codigo: 'INVENTARIO', titulo: 'Inventario', icono: 'fa-boxes' },
    { codigo: 'PROFORMAS', titulo: 'Proformas', icono: 'fa-file-alt' },
    { codigo: 'NOTAS_CREDITO', titulo: 'Notas de crédito', icono: 'fa-credit-card' },
    { codigo: 'COMPRAS', titulo: 'Compras', icono: 'fa-shopping-basket' }
  ];

  readonly reportes: ReporteDefinicion[] = [
    this.reporte('POLIZA_INDIVIDUAL', 'VENTAS', 'Póliza individual',
      'Ventas, costos y ganancia de un usuario durante el período seleccionado.', 'fa-user-tie', ['PDF'],
      ['FECHAS', 'SUCURSAL', 'USUARIO'], ['ROLE_ADMIN'], true),
    this.reporte('POLIZA_GENERAL', 'VENTAS', 'Póliza general',
      'Resumen de ventas de la sucursal agrupado por vendedor.', 'fa-users', ['PDF'],
      ['FECHAS', 'SUCURSAL'], ['ROLE_ADMIN'], true),
    this.reporte('VENTAS_PRODUCTO', 'VENTAS', 'Ventas por producto o categoría',
      'Unidades e importes vendidos, agrupados por producto y categoría.', 'fa-tags', ['PDF', 'XLSX'],
      ['FECHAS', 'SUCURSAL', 'CATEGORIA'], ['ROLE_ADMIN'], false),
    this.reporte('VENTAS_CLIENTE', 'VENTAS', 'Ventas por cliente',
      'Detalle comercial acumulado por cliente.', 'fa-user-friends', ['XLSX'],
      ['FECHAS', 'SUCURSAL', 'CLIENTE'], ['ROLE_ADMIN'], false),
    this.reporte('RENTABILIDAD_PRODUCTO', 'VENTAS', 'Rentabilidad por producto',
      'Venta neta, costo y margen por producto.', 'fa-percentage', ['PDF', 'XLSX'],
      ['FECHAS', 'SUCURSAL', 'CATEGORIA'], ['ROLE_ADMIN'], false),
    this.reporte('MOVIMIENTOS_INVENTARIO', 'INVENTARIO', 'Movimientos y saldo',
      'Entradas, salidas y saldo de inventario durante un período.', 'fa-exchange-alt', ['PDF'],
      ['FECHAS', 'SUCURSAL'], ['ROLE_ADMIN', 'ROLE_INVENTARIO'], true),
    this.reporte('EXISTENCIAS', 'INVENTARIO', 'Existencias actuales',
      'Productos, precios y stock actual de una sucursal.', 'fa-clipboard-list', ['XLSX'],
      ['SUCURSAL'], ['ROLE_ADMIN', 'ROLE_INVENTARIO'], true),
    this.reporte('BAJO_STOCK', 'INVENTARIO', 'Productos bajo stock mínimo',
      'Productos que requieren reabastecimiento.', 'fa-exclamation-triangle', ['PDF', 'XLSX'],
      ['SUCURSAL', 'CATEGORIA'], ['ROLE_ADMIN', 'ROLE_INVENTARIO'], false),
    this.reporte('KARDEX', 'INVENTARIO', 'Kardex de producto',
      'Historial cronológico de entradas, salidas y saldo de un producto.', 'fa-stream', ['PDF', 'XLSX'],
      ['FECHAS', 'SUCURSAL', 'PRODUCTO'], ['ROLE_ADMIN', 'ROLE_INVENTARIO'], false),
    this.reporte('VALORIZACION', 'INVENTARIO', 'Valorización de inventario',
      'Valor del inventario a una fecha de corte.', 'fa-coins', ['PDF', 'XLSX'],
      ['FECHA_CORTE', 'SUCURSAL'], ['ROLE_ADMIN'], false),
    this.reporte('PROFORMAS_EMITIDAS', 'PROFORMAS', 'Proformas emitidas',
      'Listado de proformas por período y usuario.', 'fa-file-excel', ['XLSX'],
      ['FECHAS', 'USUARIO'], ['ROLE_ADMIN'], true),
    this.reporte('CONVERSION_PROFORMAS', 'PROFORMAS', 'Conversión a ventas',
      'Proporción y valor de proformas convertidas en ventas.', 'fa-random', ['PDF', 'XLSX'],
      ['FECHAS', 'SUCURSAL', 'USUARIO'], ['ROLE_ADMIN'], false),
    this.reporte('RESUMEN_NOTAS', 'NOTAS_CREDITO', 'Resumen de notas de crédito',
      'Notas emitidas, importes y estados durante el período.', 'fa-file-invoice', ['PDF', 'XLSX'],
      ['FECHAS', 'SUCURSAL', 'ESTADO'], ['ROLE_ADMIN'], true),
    this.reporte('PENDIENTES_DESPACHO', 'NOTAS_CREDITO', 'Pendientes de despacho',
      'Productos de notas de crédito aún pendientes de entregar.', 'fa-truck-loading', ['PDF', 'XLSX'],
      ['FECHAS', 'SUCURSAL', 'CLIENTE'], ['ROLE_ADMIN', 'ROLE_INVENTARIO'], false),
    this.reporte('COMPRAS_PERIODO', 'COMPRAS', 'Compras por período',
      'Compras registradas por sucursal, proveedor y estado.', 'fa-shopping-cart', ['PDF', 'XLSX'],
      ['FECHAS', 'SUCURSAL', 'PROVEEDOR', 'ESTADO'], ['ROLE_ADMIN'], true),
    this.reporte('COMPRAS_PROVEEDOR', 'COMPRAS', 'Compras por proveedor o producto',
      'Detalle de abastecimiento agrupado por proveedor y producto.', 'fa-dolly-flatbed', ['XLSX'],
      ['FECHAS', 'SUCURSAL', 'PROVEEDOR'], ['ROLE_ADMIN'], false)
  ];

  reporteSeleccionado: ReporteDefinicion;
  sucursales: Sucursal[] = [];
  usuarios: Array<Usuario | UsuarioDto> = [];
  proveedores: Proveedor[] = [];
  fechaInicio: string;
  fechaFin: string;
  idSucursal: number;
  idUsuario: number;
  idProveedor: number;
  estado: string;
  formatoSeleccionado: 'PDF' | 'XLSX' = 'PDF';
  generando = false;
  readonly estadosNotaCredito = [
    { codigo: '', nombre: 'Todos los estados' },
    { codigo: 'ENTREGA_PENDIENTE', nombre: 'Entrega pendiente' },
    { codigo: 'ENTREGADO', nombre: 'Entregado' },
    { codigo: 'PAGADO', nombre: 'Pagado' },
    { codigo: 'ANULADO', nombre: 'Anulado' }
  ];
  readonly estadosCompra = [
    { codigo: '', nombre: 'Todos los estados' },
    { codigo: 'ACTIVA', nombre: 'Activas' },
    { codigo: 'ANULADA', nombre: 'Anuladas' }
  ];
  guiaLado: 'top' | 'right' | 'bottom' | 'left' = 'top';
  estilosGuia: { [propiedad: string]: string } = { '--guide-offset': '50%' };

  private cardOrigen: HTMLElement;
  private guiaPendiente = false;
  private readonly escucharScroll = () => this.programarGuia();

  constructor(
    public auth: AuthService,
    private reporteService: ReporteService,
    private sucursalService: SucursalService,
    private usuarioService: UsuarioService,
    private proveedorService: ProveedorService
  ) {
    this.seleccionarMesActual();
    window.addEventListener('scroll', this.escucharScroll, true);
  }

  ngOnInit(): void {
    this.idSucursal = this.auth.usuario && this.auth.usuario.sucursal
      ? this.auth.usuario.sucursal.idSucursal : null;
    this.cargarSucursales();
  }

  ngOnDestroy(): void {
    window.removeEventListener('scroll', this.escucharScroll, true);
  }

  @HostListener('window:resize')
  reposicionarGuia(): void {
    this.programarGuia();
  }

  reportesPorCategoria(categoria: CategoriaReporte): ReporteDefinicion[] {
    return this.reportes.filter(reporte => reporte.categoria === categoria && this.puedeVer(reporte));
  }

  seleccionarReporte(reporte: ReporteDefinicion, evento?: MouseEvent): void {
    if (!reporte.disponible) {
      return;
    }
    this.cardOrigen = evento ? evento.currentTarget as HTMLElement : null;
    this.reporteSeleccionado = reporte;
    this.idUsuario = null;
    this.idProveedor = null;
    this.estado = '';
    this.formatoSeleccionado = reporte.formatos[0];
    if (reporte.filtros.includes('USUARIO')) {
      this.cargarUsuarios(reporte.codigo);
    }
    if (reporte.filtros.includes('PROVEEDOR')) {
      this.cargarProveedores();
    }
    this.programarGuia();
  }

  cerrarConfiguracion(): void {
    this.reporteSeleccionado = null;
    this.cardOrigen = null;
  }

  get categoriaSeleccionada(): string {
    if (!this.reporteSeleccionado) {
      return '';
    }
    const categoria = this.categorias.find(item => item.codigo === this.reporteSeleccionado.categoria);
    return categoria ? categoria.titulo : '';
  }

  requiereFiltro(filtro: string): boolean {
    return !!this.reporteSeleccionado && this.reporteSeleccionado.filtros.indexOf(filtro as any) >= 0;
  }

  get estadosDisponibles(): Array<{ codigo: string; nombre: string }> {
    return this.reporteSeleccionado && this.reporteSeleccionado.codigo === 'COMPRAS_PERIODO'
      ? this.estadosCompra : this.estadosNotaCredito;
  }

  puedeGenerar(): boolean {
    if (!this.reporteSeleccionado || this.generando) {
      return false;
    }
    if (this.requiereFiltro('SUCURSAL') && !this.idSucursal) {
      return false;
    }
    if (this.requiereFiltro('USUARIO') && !this.idUsuario) {
      return false;
    }
    return !this.requiereFiltro('FECHAS') || (!!this.fechaInicio && !!this.fechaFin && this.fechaFin >= this.fechaInicio);
  }

  generar(): void {
    if (!this.puedeGenerar()) {
      return;
    }
    const filtros: ReporteFiltroDto = {
      fechaInicio: this.fechaInicio,
      fechaFin: this.fechaFin,
      idSucursal: this.idSucursal,
      idUsuario: this.idUsuario,
      idProveedor: this.idProveedor,
      estado: this.estado,
      formato: this.formatoSeleccionado
    };
    this.generando = true;
    this.reporteService.generar(this.reporteSeleccionado.codigo, filtros).subscribe(
      response => {
        this.entregarArchivo(response);
        this.generando = false;
      },
      error => {
        this.generando = false;
        Swal.fire('No se pudo generar el reporte',
          error.error && error.error.message ? error.error.message : 'Intenta nuevamente.', 'error');
      }
    );
  }

  onSucursalChange(): void {
    if (this.reporteSeleccionado && this.reporteSeleccionado.codigo === 'POLIZA_INDIVIDUAL') {
      this.idUsuario = null;
      this.cargarUsuarios(this.reporteSeleccionado.codigo);
    }
  }

  private puedeVer(reporte: ReporteDefinicion): boolean {
    return reporte.roles.some(role => this.auth.hasRole(role));
  }

  private cargarSucursales(): void {
    if (this.auth.hasRole('ROLE_ADMIN')) {
      this.sucursalService.getSucursales().subscribe(sucursales => this.sucursales = sucursales);
      return;
    }
    if (this.auth.usuario && this.auth.usuario.sucursal) {
      this.sucursales = [this.auth.usuario.sucursal];
    }
  }

  private cargarUsuarios(codigo: string): void {
    this.usuarios = [];
    if (codigo === 'PROFORMAS_EMITIDAS') {
      this.reporteService.listarUsuariosProformas().subscribe(usuarios => this.usuarios = usuarios);
      return;
    }
    this.usuarioService.getCajeros(this.idSucursal).subscribe(usuarios => this.usuarios = usuarios);
  }

  private cargarProveedores(): void {
    this.proveedores = [];
    this.proveedorService.getProveedores().subscribe(proveedores => this.proveedores = proveedores);
  }

  private entregarArchivo(response: HttpResponse<Blob>): void {
    const extension = this.formatoSeleccionado === 'PDF' ? 'pdf' : 'xlsx';
    const respaldo = `${this.reporteSeleccionado.codigo.toLowerCase()}.${extension}`;
    const nombre = this.reporteService.obtenerNombreArchivo(response, respaldo);
    const url = window.URL.createObjectURL(response.body);
    if (extension === 'pdf') {
      window.open(url, '_blank');
      window.setTimeout(() => window.URL.revokeObjectURL(url), 60000);
      return;
    }
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = nombre;
    document.body.appendChild(enlace);
    enlace.click();
    enlace.remove();
    window.URL.revokeObjectURL(url);
  }

  private seleccionarMesActual(): void {
    const hoy = new Date();
    this.fechaInicio = this.fechaIso(new Date(hoy.getFullYear(), hoy.getMonth(), 1));
    this.fechaFin = this.fechaIso(hoy);
  }

  private fechaIso(fecha: Date): string {
    const anio = fecha.getFullYear();
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');
    return `${anio}-${mes}-${dia}`;
  }

  private programarGuia(): void {
    if (!this.reporteSeleccionado || !this.cardOrigen || this.guiaPendiente) {
      return;
    }
    this.guiaPendiente = true;
    window.requestAnimationFrame(() => {
      this.guiaPendiente = false;
      this.actualizarGuia();
    });
  }

  private actualizarGuia(): void {
    if (!this.cardOrigen) {
      return;
    }
    const panel = document.getElementById('report-config-panel');
    if (!panel) {
      return;
    }

    if (window.innerWidth <= 767) {
      this.estilosGuia = { '--guide-offset': '50%' };
      return;
    }

    const card = this.cardOrigen.getBoundingClientRect();
    const modal = panel.getBoundingClientRect();
    const margen = 16;
    const separacion = 26;
    const anchoDeseado = 650;
    const anchoMinimoLateral = 360;
    const altoMinimoVertical = 260;
    const espacioIzquierda = card.left - separacion - margen;
    const espacioDerecha = window.innerWidth - card.right - separacion - margen;
    const espacioArriba = card.top - separacion - margen;
    const espacioAbajo = window.innerHeight - card.bottom - separacion - margen;
    const centroCardX = card.left + card.width / 2;
    const centroCardY = card.top + card.height / 2;
    const ladosHorizontales: Array<'left' | 'right'> = centroCardX <= window.innerWidth / 2
      ? ['right', 'left'] : ['left', 'right'];
    const ladoHorizontal = ladosHorizontales.find(lado =>
      (lado === 'left' ? espacioIzquierda : espacioDerecha) >= anchoMinimoLateral);

    let izquierda: number;
    let arriba: number;
    let ancho = Math.min(anchoDeseado, window.innerWidth - margen * 2);
    let altoMaximo = window.innerHeight - margen * 2;

    if (ladoHorizontal) {
      const espacio = ladoHorizontal === 'left' ? espacioIzquierda : espacioDerecha;
      ancho = Math.min(anchoDeseado, espacio);
      izquierda = ladoHorizontal === 'left' ? card.left - separacion - ancho : card.right + separacion;
      arriba = this.limitar(centroCardY - modal.height / 2, margen,
        Math.max(margen, window.innerHeight - modal.height - margen));
      this.guiaLado = ladoHorizontal === 'left' ? 'right' : 'left';
    } else {
      const colocarAbajo = espacioAbajo >= altoMinimoVertical || espacioAbajo >= espacioArriba;
      izquierda = this.limitar(centroCardX - ancho / 2, margen, window.innerWidth - ancho - margen);

      if (colocarAbajo) {
        arriba = card.bottom + separacion;
        altoMaximo = Math.max(1, espacioAbajo);
        this.guiaLado = 'top';
      } else {
        altoMaximo = Math.max(1, espacioArriba);
        arriba = Math.max(margen, card.top - separacion - Math.min(modal.height, altoMaximo));
        this.guiaLado = 'bottom';
      }
    }

    this.estilosGuia = {
      '--guide-offset': '50%',
      'bottom': 'auto',
      'left': `${Math.round(izquierda)}px`,
      'max-height': `${Math.round(altoMaximo)}px`,
      'right': 'auto',
      'top': `${Math.round(arriba)}px`,
      'width': `${Math.round(ancho)}px`
    };

    window.requestAnimationFrame(() => this.actualizarPuntero(panel));
  }

  private actualizarPuntero(panel: HTMLElement): void {
    if (!this.cardOrigen || window.innerWidth <= 767) {
      return;
    }

    const card = this.cardOrigen.getBoundingClientRect();
    const modal = panel.getBoundingClientRect();
    const centroX = card.left + card.width / 2;
    const centroY = card.top + card.height / 2;
    const desplazamiento = this.guiaLado === 'top' || this.guiaLado === 'bottom'
      ? this.limitar(centroX - modal.left, 54, modal.width - 54)
      : this.limitar(centroY - modal.top, 54, modal.height - 54);
    this.estilosGuia = Object.assign({}, this.estilosGuia,
      { '--guide-offset': `${Math.round(desplazamiento)}px` });
  }

  private limitar(valor: number, minimo: number, maximo: number): number {
    return Math.max(minimo, Math.min(valor, maximo));
  }

  private reporte(
    codigo: string,
    categoria: CategoriaReporte,
    titulo: string,
    descripcion: string,
    icono: string,
    formatos: any[],
    filtros: any[],
    roles: string[],
    disponible: boolean
  ): ReporteDefinicion {
    return { codigo, categoria, titulo, descripcion, icono, formatos, filtros, roles, disponible };
  }
}
