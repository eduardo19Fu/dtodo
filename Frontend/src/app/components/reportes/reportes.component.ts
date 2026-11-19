import { Component, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import Swal from 'sweetalert2';

import { ReporteFiltroDto } from '../../dtos/reporte-filtro-dto';
import { ReporteSelectorOpcionDto } from '../../dtos/reporte-selector-opcion-dto';
import { CategoriaReporte, ReporteDefinicion } from '../../models/reporte-definicion';
import { AuthService } from '../../services/auth.service';
import { ReporteService } from '../../services/reporte.service';

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
export class ReportesComponent implements OnInit {
  readonly categorias: CategoriaDefinicion[] = [
    { codigo: 'VENTAS', titulo: 'Ventas', icono: 'fa-chart-line' },
    { codigo: 'INVENTARIO', titulo: 'Inventario', icono: 'fa-boxes' },
    { codigo: 'PROFORMAS', titulo: 'Proformas', icono: 'fa-file-alt' },
    { codigo: 'NOTAS_CREDITO', titulo: 'Notas de crédito', icono: 'fa-credit-card' },
    { codigo: 'COMPRAS', titulo: 'Compras', icono: 'fa-shopping-basket' },
    { codigo: 'BODEGAS', titulo: 'Bodegas', icono: 'fa-warehouse' },
    { codigo: 'MAYORISTAS', titulo: 'Mayoristas', icono: 'fa-route' }
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
      ['FECHAS', 'SUCURSAL', 'CATEGORIA'], ['ROLE_ADMIN'], true),
    this.reporte('VENTAS_CLIENTE', 'VENTAS', 'Ventas por cliente',
      'Detalle comercial acumulado por cliente.', 'fa-user-friends', ['PDF', 'XLSX'],
      ['FECHAS', 'SUCURSAL', 'CLIENTE'], ['ROLE_ADMIN'], true),
    this.reporte('RENTABILIDAD_PRODUCTO', 'VENTAS', 'Rentabilidad por producto',
      'Venta neta, costo y margen por producto.', 'fa-percentage', ['PDF', 'XLSX'],
      ['FECHAS', 'SUCURSAL', 'CATEGORIA'], ['ROLE_ADMIN'], true),
    this.reporte('MOVIMIENTOS_INVENTARIO', 'INVENTARIO', 'Movimientos y saldo',
      'Entradas, salidas y saldo de inventario durante un período.', 'fa-exchange-alt', ['PDF', 'XLSX'],
      ['FECHAS', 'SUCURSAL'], ['ROLE_ADMIN', 'ROLE_INVENTARIO'], true),
    this.reporte('EXISTENCIAS', 'INVENTARIO', 'Existencias actuales',
      'Productos, precios y stock actual de una sucursal.', 'fa-clipboard-list', ['XLSX'],
      ['SUCURSAL'], ['ROLE_ADMIN', 'ROLE_INVENTARIO'], true),
    this.reporte('BAJO_STOCK', 'INVENTARIO', 'Productos bajo stock mínimo',
      'Productos que requieren reabastecimiento.', 'fa-exclamation-triangle', ['PDF', 'XLSX'],
      ['SUCURSAL', 'CATEGORIA'], ['ROLE_ADMIN', 'ROLE_INVENTARIO'], true),
    this.reporte('KARDEX', 'INVENTARIO', 'Kardex de producto',
      'Historial cronológico de entradas, salidas y saldo de un producto.', 'fa-stream', ['PDF', 'XLSX'],
      ['FECHAS', 'SUCURSAL', 'PRODUCTO'], ['ROLE_ADMIN', 'ROLE_INVENTARIO'], true),
    this.reporte('VALORIZACION', 'INVENTARIO', 'Valorización de inventario',
      'Valor del inventario a una fecha de corte.', 'fa-coins', ['PDF', 'XLSX'],
      ['FECHA_CORTE', 'SUCURSAL'], ['ROLE_ADMIN'], true),
    this.reporte('PROFORMAS_EMITIDAS', 'PROFORMAS', 'Proformas emitidas',
      'Listado de proformas por período y usuario.', 'fa-file-excel', ['XLSX'],
      ['FECHAS', 'USUARIO'], ['ROLE_ADMIN'], true),
    this.reporte('CONVERSION_PROFORMAS', 'PROFORMAS', 'Conversión a ventas',
      'Proporción y valor de proformas convertidas en ventas.', 'fa-random', ['PDF', 'XLSX'],
      ['FECHAS', 'SUCURSAL', 'USUARIO'], ['ROLE_ADMIN'], true),
    this.reporte('RESUMEN_NOTAS', 'NOTAS_CREDITO', 'Resumen de notas de crédito',
      'Notas emitidas, importes y estados durante el período.', 'fa-file-invoice', ['PDF', 'XLSX'],
      ['FECHAS', 'SUCURSAL', 'ESTADO'], ['ROLE_ADMIN'], true),
    this.reporte('PENDIENTES_DESPACHO', 'NOTAS_CREDITO', 'Pendientes de despacho',
      'Productos de notas de crédito aún pendientes de entregar.', 'fa-truck-loading', ['PDF', 'XLSX'],
      ['FECHAS', 'SUCURSAL', 'CLIENTE'], ['ROLE_ADMIN', 'ROLE_INVENTARIO'], true),
    this.reporte('COMPRAS_PERIODO', 'COMPRAS', 'Compras por período',
      'Compras registradas por sucursal, proveedor y estado.', 'fa-shopping-cart', ['PDF', 'XLSX'],
      ['FECHAS', 'SUCURSAL', 'PROVEEDOR', 'ESTADO'], ['ROLE_ADMIN'], true),
    this.reporte('COMPRAS_PROVEEDOR', 'COMPRAS', 'Compras por proveedor o producto',
      'Detalle de abastecimiento agrupado por proveedor y producto.', 'fa-dolly-flatbed', ['XLSX'],
      ['FECHAS', 'SUCURSAL', 'PROVEEDOR'], ['ROLE_ADMIN'], true),
    this.reporte('BODEGA_EXISTENCIAS', 'BODEGAS', 'Existencias de bodega',
      'Productos y existencias del inventario de una bodega. El valor al costo solo lo incluye para administradores.',
      'fa-boxes', ['PDF', 'XLSX'],
      ['BODEGA'], ['ROLE_ADMIN', 'ROLE_BODEGA'], true),
    this.reporte('BODEGA_MOVIMIENTOS', 'BODEGAS', 'Movimientos de bodega',
      'Ingresos, importaciones, reducciones, eliminaciones y despachos de una bodega.', 'fa-exchange-alt', ['PDF', 'XLSX'],
      ['FECHAS', 'BODEGA'], ['ROLE_ADMIN', 'ROLE_BODEGA'], true),
    this.reporte('BODEGA_DESPACHOS', 'BODEGAS', 'Despachos de bodega',
      'Despachos a sucursales y traslados por período, bodega y estado. El valor al costo solo lo incluye para administradores.',
      'fa-truck-loading', ['PDF', 'XLSX'],
      ['FECHAS', 'BODEGA_OPCIONAL', 'ESTADO'], ['ROLE_ADMIN', 'ROLE_BODEGA'], true),
    this.reporte('MAYORISTAS_CLIENTES_RUTA', 'MAYORISTAS', 'Clientes por ruta',
      'Clientes mayoristas agrupados por ruta, con su vendedor, contacto y condiciones.', 'fa-route', ['PDF', 'XLSX'],
      ['RUTA'], ['ROLE_ADMIN'], true),
    this.reporte('MAYORISTAS_SIN_COMPRAS', 'MAYORISTAS', 'Clientes sin compras',
      'Clientes mayoristas sin proformas ni facturas en el período, con su última compra.', 'fa-user-clock', ['PDF', 'XLSX'],
      ['FECHAS', 'RUTA'], ['ROLE_ADMIN'], true),
    this.reporte('MAYORISTAS_PROFORMAS_VENDEDOR', 'MAYORISTAS', 'Proformas por vendedor',
      'Proformas mayoristas generadas frente a las facturadas por cada vendedor.', 'fa-people-carry', ['PDF', 'XLSX'],
      ['FECHAS', 'RUTA'], ['ROLE_ADMIN'], true),
    this.reporte('MAYORISTAS_ENTREGADO', 'MAYORISTAS', 'Ventas entregadas',
      'Lo entregado a los clientes por ruta y vendedor, a precio de línea; una entrega parcial suma solo lo entregado.', 'fa-truck', ['PDF', 'XLSX'],
      ['FECHAS', 'RUTA', 'VENDEDOR_OPCIONAL'], ['ROLE_ADMIN'], true),
    this.reporte('MAYORISTAS_CHEQUES', 'MAYORISTAS', 'Control de cheques',
      'Cheques recibidos de los clientes por fecha de pago, con banco, número, fecha del cheque y su estado.', 'fa-money-check-alt', ['PDF', 'XLSX'],
      ['FECHAS', 'ESTADO'], ['ROLE_ADMIN'], true),
    this.reporte('MAYORISTAS_DESPACHADOS_CREDITO', 'MAYORISTAS', 'Despachados al crédito',
      'Clientes con pedidos a crédito entregados: monto, tres abonos, saldo pendiente, tipo de pago y espacio para la firma de recibido.', 'fa-file-signature', ['PDF', 'XLSX'],
      ['FECHAS', 'RUTA'], ['ROLE_ADMIN'], true),
    this.reporte('MAYORISTAS_LISTADO_PROFORMAS', 'MAYORISTAS', 'Listado de proformas',
      'Pedidos mayoristas del período con columnas en blanco para anotar cajas, paquetes o rollos y la hora de empaque.', 'fa-clipboard-list', ['PDF', 'XLSX'],
      ['FECHAS', 'RUTA'], ['ROLE_ADMIN'], true)
  ];

  busquedaReportes = '';
  categoriaActiva: CategoriaReporte;
  reporteSeleccionado: ReporteDefinicion;
  sucursales: ReporteSelectorOpcionDto[] = [];
  usuarios: ReporteSelectorOpcionDto[] = [];
  proveedores: ReporteSelectorOpcionDto[] = [];
  categoriasProducto: ReporteSelectorOpcionDto[] = [];
  clientes: ReporteSelectorOpcionDto[] = [];
  productos: ReporteSelectorOpcionDto[] = [];
  bodegas: ReporteSelectorOpcionDto[] = [];
  rutas: ReporteSelectorOpcionDto[] = [];
  fechaInicio: string;
  fechaFin: string;
  fechaCorte: string;
  idSucursal: number;
  idBodega: number;
  idUsuario: number;
  idProveedor: number;
  idCategoria: number;
  idCliente: number;
  idProducto: number;
  idRuta: number;
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
  readonly estadosCheque = [
    { codigo: '', nombre: 'Todos los estados' },
    { codigo: 'RECIBIDO', nombre: 'Recibidos' },
    { codigo: 'DEPOSITADO', nombre: 'Depositados' },
    { codigo: 'COBRADO', nombre: 'Cobrados' },
    { codigo: 'RECHAZADO', nombre: 'Rechazados' }
  ];
  readonly estadosDespachoBodega = [
    { codigo: '', nombre: 'Todos los estados' },
    { codigo: 'PENDIENTE', nombre: 'Pendientes de aprobación' },
    { codigo: 'REALIZADO', nombre: 'Realizados' },
    { codigo: 'CANCELADO', nombre: 'Cancelados' }
  ];
  private sucursalesCargadas = false;
  private proveedoresCargados = false;
  private bodegasCargadas = false;
  private rutasCargadas = false;
  private categoriasCargadas = false;
  private clientesCargados = false;
  private readonly productosCache = new Map<number, ReporteSelectorOpcionDto[]>();
  private productosCargando = new Set<number>();
  private readonly usuariosCache = new Map<string, ReporteSelectorOpcionDto[]>();
  private readonly usuariosCargando = new Set<string>();
  private claveUsuariosActual: string;

  constructor(
    public auth: AuthService,
    private reporteService: ReporteService
  ) {
    this.seleccionarMesActual();
  }

  ngOnInit(): void {
    this.idSucursal = this.auth.usuario && this.auth.usuario.sucursal
      ? this.auth.usuario.sucursal.idSucursal : null;
    this.cargarSucursales();
    this.categoriaActiva = this.categoriasVisibles.length ? this.categoriasVisibles[0].codigo : null;
  }

  get categoriasVisibles(): CategoriaDefinicion[] {
    return this.categorias.filter(categoria => this.reportes.some(reporte =>
      reporte.categoria === categoria.codigo && this.puedeVer(reporte)));
  }

  get buscando(): boolean {
    return !!this.busquedaReportes.trim();
  }

  get categoriasMostradas(): CategoriaDefinicion[] {
    return this.categoriasVisibles.filter(categoria =>
      (this.buscando || categoria.codigo === this.categoriaActiva) && this.reportesPorCategoria(categoria.codigo).length > 0);
  }

  seleccionarCategoria(categoria: CategoriaReporte): void {
    this.categoriaActiva = categoria;
    this.busquedaReportes = '';
    this.cerrarConfiguracion();
  }

  reportesPorCategoria(categoria: CategoriaReporte): ReporteDefinicion[] {
    return this.reportes.filter(reporte => reporte.categoria === categoria
      && this.puedeVer(reporte) && this.coincideBusqueda(reporte));
  }

  get hayReportesEncontrados(): boolean {
    return this.reportes.some(reporte => this.puedeVer(reporte) && this.coincideBusqueda(reporte));
  }

  actualizarBusqueda(valor: string): void {
    this.busquedaReportes = valor;
    if (this.reporteSeleccionado && !this.coincideBusqueda(this.reporteSeleccionado)) {
      this.cerrarConfiguracion();
    }
  }

  limpiarBusqueda(): void {
    this.actualizarBusqueda('');
  }

  seleccionarReporte(reporte: ReporteDefinicion): void {
    if (!reporte.disponible) {
      return;
    }
    this.categoriaActiva = reporte.categoria;
    this.reporteSeleccionado = reporte;
    this.idUsuario = null;
    this.idProveedor = null;
    this.idCategoria = null;
    this.idCliente = null;
    this.idProducto = null;
    this.idRuta = null;
    this.idBodega = null;
    this.estado = '';
    this.formatoSeleccionado = reporte.formatos[0];
    if (reporte.filtros.includes('BODEGA') || reporte.filtros.includes('BODEGA_OPCIONAL')) {
      this.cargarBodegas();
    }
    if (reporte.filtros.includes('USUARIO') || reporte.filtros.includes('VENDEDOR_OPCIONAL')) {
      this.cargarUsuarios(reporte.codigo);
    }
    if (reporte.filtros.includes('PROVEEDOR')) {
      this.cargarProveedores();
    }
    if (reporte.filtros.includes('CATEGORIA')) {
      this.cargarCategoriasProducto();
    }
    if (reporte.filtros.includes('CLIENTE')) {
      this.cargarClientes();
    }
    if (reporte.filtros.includes('PRODUCTO')) {
      this.cargarProductos();
    }
    if (reporte.filtros.includes('RUTA')) {
      this.cargarRutas();
    }
    if (window.innerWidth < 1200) {
      window.requestAnimationFrame(() => {
        const titulo = document.getElementById('report-config-title');
        if (titulo) {
          titulo.focus();
        }
      });
    }
  }

  cerrarConfiguracion(): void {
    this.reporteSeleccionado = null;
  }

  limpiarFiltros(): void {
    if (!this.reporteSeleccionado || this.generando) {
      return;
    }
    this.seleccionarMesActual();
    this.idUsuario = null;
    this.idProveedor = null;
    this.idCategoria = null;
    this.idCliente = null;
    this.idProducto = null;
    this.idRuta = null;
    this.idBodega = null;
    this.estado = '';
    this.formatoSeleccionado = this.reporteSeleccionado.formatos[0];
  }

  get opcionesSucursal(): ReporteSelectorOpcionDto[] {
    return this.sucursales;
  }

  get opcionesUsuario(): ReporteSelectorOpcionDto[] {
    return this.usuarios;
  }

  get opcionesCategoria(): ReporteSelectorOpcionDto[] {
    return this.categoriasProducto;
  }

  get opcionesCliente(): ReporteSelectorOpcionDto[] {
    return this.clientes;
  }

  get opcionesProveedor(): ReporteSelectorOpcionDto[] {
    return this.proveedores;
  }

  get opcionesProducto(): ReporteSelectorOpcionDto[] {
    return this.productos;
  }

  get opcionesRuta(): ReporteSelectorOpcionDto[] {
    return this.rutas;
  }

  get opcionesBodega(): ReporteSelectorOpcionDto[] {
    return this.bodegas;
  }

  get opcionesEstado(): ReporteSelectorOpcionDto[] {
    return this.estadosDisponibles
      .filter(opcion => !!opcion.codigo)
      .map(opcion => ({ valor: opcion.codigo, etiqueta: opcion.nombre }));
  }

  seleccionarSucursal(valor: number): void {
    this.idSucursal = valor;
    this.onSucursalChange();
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
    if (this.reporteSeleccionado && this.reporteSeleccionado.codigo === 'BODEGA_DESPACHOS') {
      return this.estadosDespachoBodega;
    }
    if (this.reporteSeleccionado && this.reporteSeleccionado.codigo === 'MAYORISTAS_CHEQUES') {
      return this.estadosCheque;
    }
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
    if (this.requiereFiltro('PRODUCTO') && !this.idProducto) {
      return false;
    }
    if (this.requiereFiltro('BODEGA') && !this.idBodega) {
      return false;
    }
    if (this.requiereFiltro('FECHA_CORTE') && !this.fechaCorte) {
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
      fechaCorte: this.fechaCorte,
      idSucursal: this.idSucursal,
      idBodega: this.idBodega,
      idUsuario: this.idUsuario,
      idProveedor: this.idProveedor,
      idCategoria: this.idCategoria,
      idCliente: this.idCliente,
      idProducto: this.idProducto,
      idRuta: this.idRuta,
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
    if (this.reporteSeleccionado && this.reporteSeleccionado.codigo === 'KARDEX') {
      this.idProducto = null;
      this.cargarProductos();
    }
    if (this.reporteSeleccionado && this.reporteSeleccionado.codigo === 'CONVERSION_PROFORMAS') {
      this.idUsuario = null;
      this.cargarUsuarios(this.reporteSeleccionado.codigo);
    }
  }

  private puedeVer(reporte: ReporteDefinicion): boolean {
    return reporte.roles.some(role => this.auth.hasRole(role));
  }

  private coincideBusqueda(reporte: ReporteDefinicion): boolean {
    const terminos = this.normalizarBusqueda(this.busquedaReportes).split(/\s+/).filter(termino => !!termino);
    if (!terminos.length) {
      return true;
    }
    const categoria = this.categorias.find(item => item.codigo === reporte.categoria);
    const contenido = this.normalizarBusqueda([
      reporte.titulo, reporte.descripcion, categoria ? categoria.titulo : '', ...reporte.formatos
    ].join(' '));
    return terminos.every(termino => contenido.includes(termino));
  }

  private normalizarBusqueda(valor: string): string {
    return (valor || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
  }

  private cargarSucursales(): void {
    const sucursalSesion = this.auth.usuario && this.auth.usuario.sucursal
      ? this.auth.usuario.sucursal : null;
    if (sucursalSesion && sucursalSesion.nombre) {
      this.sucursales = [{
        valor: sucursalSesion.idSucursal,
        etiqueta: sucursalSesion.nombre,
        detalle: sucursalSesion.direccion
      }];
    }
    if (this.auth.hasRole('ROLE_ADMIN')) {
      if (this.sucursalesCargadas) {
        return;
      }
      this.sucursalesCargadas = true;
      this.reporteService.listarSucursalesSelector().subscribe(
        opciones => this.sucursales = opciones,
        () => this.sucursalesCargadas = false
      );
      return;
    }
    if (sucursalSesion) {
      const sucursal = sucursalSesion;
      this.sucursales = [{
        valor: sucursal.idSucursal,
        etiqueta: sucursal.nombre,
        detalle: sucursal.direccion
      }];
      this.sucursalesCargadas = true;
    }
  }

  private cargarUsuarios(codigo: string): void {
    const esProformas = codigo === 'PROFORMAS_EMITIDAS' || codigo === 'CONVERSION_PROFORMAS' || codigo === 'MAYORISTAS_ENTREGADO';
    const clave = codigo === 'CONVERSION_PROFORMAS'
      ? `proformas-${this.idSucursal || 'todas'}`
      : esProformas ? 'proformas' : `cajeros-${this.idSucursal || 'todas'}`;
    this.claveUsuariosActual = clave;
    if (this.usuariosCache.has(clave)) {
      this.usuarios = this.usuariosCache.get(clave);
      return;
    }
    this.usuarios = [];
    if (this.usuariosCargando.has(clave)) {
      return;
    }
    this.usuariosCargando.add(clave);
    const consulta = esProformas
      ? this.reporteService.listarUsuariosProformasSelector(
          codigo === 'CONVERSION_PROFORMAS' ? this.idSucursal : null)
      : this.reporteService.listarCajerosSelector(this.idSucursal);
    consulta.subscribe(
      opciones => {
        this.usuariosCache.set(clave, opciones);
        if (this.claveUsuariosActual === clave) {
          this.usuarios = opciones;
        }
        this.usuariosCargando.delete(clave);
      },
      () => this.usuariosCargando.delete(clave)
    );
  }

  private cargarProveedores(): void {
    if (this.proveedoresCargados) {
      return;
    }
    this.proveedoresCargados = true;
    this.reporteService.listarProveedoresSelector().subscribe(
      opciones => this.proveedores = opciones,
      () => this.proveedoresCargados = false
    );
  }

  private cargarRutas(): void {
    if (this.rutasCargadas) {
      return;
    }
    this.rutasCargadas = true;
    this.reporteService.listarRutasSelector().subscribe(
      opciones => this.rutas = opciones,
      () => this.rutasCargadas = false
    );
  }

  private cargarBodegas(): void {
    if (this.bodegasCargadas) {
      return;
    }
    this.bodegasCargadas = true;
    this.reporteService.listarBodegasSelector().subscribe(
      opciones => this.bodegas = opciones,
      () => this.bodegasCargadas = false
    );
  }

  private cargarCategoriasProducto(): void {
    if (this.categoriasCargadas) {
      return;
    }
    this.categoriasCargadas = true;
    this.reporteService.listarCategoriasSelector().subscribe(
      opciones => this.categoriasProducto = opciones,
      () => this.categoriasCargadas = false
    );
  }

  private cargarClientes(): void {
    if (this.clientesCargados) {
      return;
    }
    this.clientesCargados = true;
    this.reporteService.listarClientesSelector().subscribe(
      opciones => this.clientes = opciones,
      () => this.clientesCargados = false
    );
  }

  private cargarProductos(): void {
    if (!this.idSucursal) {
      this.productos = [];
      return;
    }
    const idSucursal = this.idSucursal;
    if (this.productosCache.has(idSucursal)) {
      this.productos = this.productosCache.get(idSucursal);
      return;
    }
    this.productos = [];
    if (this.productosCargando.has(idSucursal)) {
      return;
    }
    this.productosCargando.add(idSucursal);
    this.reporteService.listarProductosSelector(idSucursal).subscribe(
      opciones => {
        this.productosCache.set(idSucursal, opciones);
        if (this.idSucursal === idSucursal) {
          this.productos = opciones;
        }
        this.productosCargando.delete(idSucursal);
      },
      () => this.productosCargando.delete(idSucursal)
    );
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
    this.fechaCorte = this.fechaFin;
  }

  private fechaIso(fecha: Date): string {
    const anio = fecha.getFullYear();
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');
    return `${anio}-${mes}-${dia}`;
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
