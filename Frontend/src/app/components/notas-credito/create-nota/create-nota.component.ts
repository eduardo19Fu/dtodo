import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { UsuarioAuxiliar } from 'src/app/models/auxiliar/usuario-auxiliar';
import { Cliente } from 'src/app/models/cliente';
import { DetalleDocumentoDto } from 'src/app/dtos/detalleDocumentoDto';
import { DocumentoOrigenNotaDto } from 'src/app/dtos/documento-origen-nota-dto';
import { NotaCredito, TipoDocumentoOrigen } from 'src/app/models/nota-credito';
import { NotaCreditoDetalle } from 'src/app/models/nota-credito-detalle';
import { Producto } from 'src/app/models/producto';
import { AuthService } from 'src/app/services/auth.service';
import { ClienteService } from 'src/app/services/cliente.service';
import { ClienteCreateService } from 'src/app/services/facturas/cliente-create.service';
import { FacturaService } from 'src/app/services/facturas/factura.service';
import { NotasCreditoService } from 'src/app/services/notas-credito.service';
import { ProformaService } from 'src/app/services/proformas/proforma.service';

import swal from 'sweetalert2';

declare var $: any;

/**
 * Item genérico utilizado por el modal de selección, independiente del
 * documento origen (Factura o Proforma).
 */
interface ItemSeleccionable {
  idProducto: number;
  codProducto: string;
  nombreProducto: string;
  cantidadOriginal: number;
  precioVenta: number;
  precioUnitarioConDescuento: number;
  descuento: number;
  subTotalOriginal: number;
  cantidadSeleccionada: number;
  seleccionado: boolean;
}

@Component({
  selector: 'app-create-nota',
  templateUrl: './create-nota.component.html',
  styleUrls: [
    '../../productos/create-producto/create-producto.component.css',
    '../../proformas/create-proforma/create-proforma.component.css',
    './create-nota.component.css'
  ]
})
export class CreateNotaComponent implements OnInit {

  @ViewChild('mybuscar') myBuscarTexto: ElementRef;
  @ViewChild('myCorrelativoFactura') myCorrelativo: ElementRef;

  nitIngresado: string;
  title: string;

  notaCredito: NotaCredito;
  cliente: Cliente;
  usuario: UsuarioAuxiliar;

  // Origen del documento: Factura o Proforma
  tipoOrigen: TipoDocumentoOrigen = 'FACTURA';

  // Modal de selección de productos
  documentoOrigenCargado: DocumentoOrigenNotaDto;
  itemsSeleccionables: ItemSeleccionable[] = [];
  mostrarModalProductos: boolean = false;
  cargandoProductosOrigen: boolean = false;

  // Paginación del modal
  paginaActual: number = 0;
  pageSize: number = 5;
  pageSizeOptions: number[] = [5, 10, 15, 25];
  filtroModal: string = '';

  constructor(
    private notaService: NotasCreditoService,
    private facturaService: FacturaService,
    private proformaService: ProformaService,
    private clienteService: ClienteService,
    private clienteCreateService: ClienteCreateService,
    private authService: AuthService,
    private router: Router
  ) {
    this.title = 'Creación de Notas de Crédito';
    this.cliente = new Cliente();
    this.usuario = new UsuarioAuxiliar();
    this.notaCredito = new NotaCredito();
    this.notaCredito.tipoDocumentoOrigen = 'FACTURA';
  }

  ngOnInit(): void {
    this.cargarVendedor();
  }

  cargarVendedor(): void {
    this.usuario.idUsuario = this.authService.usuario.idUsuario;
    this.usuario.primerNombre = this.authService.usuario.primerNombre;
    this.usuario.apellido = this.authService.usuario.apellido;
    this.usuario.usuario = this.authService.usuario.usuario;
  }

  /**
   * Cambia el documento origen (FACTURA / PROFORMA) limpiando los campos
   * incompatibles para evitar inconsistencias.
   */
  cambiarTipoOrigen(nuevoTipo: TipoDocumentoOrigen): void {
    if (this.tipoOrigen === nuevoTipo) {
      return;
    }
    this.tipoOrigen = nuevoTipo;
    this.notaCredito.tipoDocumentoOrigen = nuevoTipo;

    if (nuevoTipo === 'FACTURA') {
      this.notaCredito.noProforma = null;
    } else {
      this.notaCredito.correlativoFacturaSat = null;
      this.notaCredito.serieFacturaSat = null;
    }
    this.documentoOrigenCargado = null;
  }

  cargarCliente(event): void {
    this.cliente = event;
  }

  loadCliente(event): void {
    this.myBuscarTexto.nativeElement.value = event.nit;
    (document.getElementById('button-2x')).click();
    this.buscarCliente();
  }

  buscarCliente(): void {
    const nit = this.myBuscarTexto.nativeElement.value;
    if (nit) {
      this.clienteService.getClienteByNit(nit).subscribe(
        cliente => {
          this.cliente = cliente;
          if (this.myCorrelativo) {
            this.myCorrelativo.nativeElement.focus();
          }
        },
        error => {
          if (error.status === 400) {
            swal.fire(`Error: ${error.status}`, 'Petición Equivocada', 'error');
          }
          if (error.status === 404) {
            this.nitIngresado = nit;
            this.clienteCreateService.abrirModal();
          }
        }
      );
    } else {
      swal.fire('NIT Vacío', 'Ingrese un valor valido para realizar la búsqueda.', 'warning');
    }
  }

  // --- Flujo de selección de productos ---

  abrirSeleccionProductos(): void {
    if (!this.cliente || !this.cliente.idCliente) {
      swal.fire('Cliente Requerido', 'Por favor, seleccione un cliente antes de continuar.', 'warning');
      return;
    }

    if (this.tipoOrigen === 'FACTURA') {
      this.cargarProductosDesdeFactura();
    } else {
      this.cargarProductosDesdeProforma();
    }
  }

  private cargarProductosDesdeFactura(): void {
    const correlativo = this.notaCredito.correlativoFacturaSat;
    const serie = this.notaCredito.serieFacturaSat;

    if (!correlativo) {
      swal.fire('Correlativo Requerido', 'Por favor, ingrese el correlativo de la factura asociada.', 'warning');
      return;
    }
    if (!serie) {
      swal.fire('Serie Requerida', 'Por favor, ingrese la serie de la factura asociada.', 'warning');
      return;
    }

    this.iniciarCargaProductos('factura');
    this.facturaService.getOrigenNotaDto(correlativo, serie).subscribe(
      documento => {
        if (documento.idCliente !== this.cliente.idCliente) {
          this.finalizarCargaProductos();
          swal.fire('Cliente No Coincide',
            `La factura con correlativo ${correlativo} y serie ${serie} pertenece al cliente "${documento.cliente}", pero el cliente seleccionado es "${this.cliente.nombre}".`,
            'error');
          return;
        }

        this.cargarDetalleDocumento(documento, true);
      },
      () => {
        this.finalizarCargaProductos();
        swal.fire('Factura no encontrada',
          'No se encontró una factura con el correlativo y serie ingresados.', 'warning');
      }
    );
  }

  private cargarProductosDesdeProforma(): void {
    const noProforma = this.notaCredito.noProforma;

    if (!noProforma) {
      swal.fire('No. Proforma Requerido', 'Por favor, ingrese el número de la proforma asociada.', 'warning');
      return;
    }

    this.iniciarCargaProductos('proforma');
    this.proformaService.getOrigenNotaDto(noProforma).subscribe(
      documento => {
        if (documento.idCliente !== this.cliente.idCliente) {
          this.finalizarCargaProductos();
          swal.fire('Cliente No Coincide',
            `La proforma ${noProforma} pertenece al cliente "${documento.cliente}", pero el cliente seleccionado es "${this.cliente.nombre}".`,
            'error');
          return;
        }

        this.cargarDetalleDocumento(documento, false);
      },
      () => {
        this.finalizarCargaProductos();
        swal.fire('Proforma no encontrada', `No existe una proforma con el número "${noProforma}".`, 'warning');
      }
    );
  }

  private cargarDetalleDocumento(documento: DocumentoOrigenNotaDto, esFactura: boolean): void {
    const detalle$ = esFactura
      ? this.facturaService.getDetalleFacturaDto(documento.idDocumento, 0, 10000)
      : this.proformaService.getDetalleProformaDto(documento.idDocumento, 0, 10000);

    detalle$.subscribe(
      response => {
        const detalles = (response.content || []) as DetalleDocumentoDto[];
        this.documentoOrigenCargado = documento;
        this.itemsSeleccionables = detalles.map(detalle => this.crearItemSeleccionable(detalle));
        this.resetEstadoModal();
        this.mostrarModalProductos = true;
        this.finalizarCargaProductos();
      },
      () => {
        this.finalizarCargaProductos();
        swal.fire('No fue posible cargar el detalle',
          'Ocurrió un problema al consultar los productos del documento.', 'error');
      }
    );
  }

  private crearItemSeleccionable(detalle: DetalleDocumentoDto): ItemSeleccionable {
    const cantidad = detalle.cantidad || 1;
    return {
      idProducto: detalle.idProducto,
      codProducto: detalle.codigoProducto,
      nombreProducto: detalle.producto,
      cantidadOriginal: detalle.cantidad,
      precioVenta: this.unitarioHistorico(detalle),
      precioUnitarioConDescuento: (detalle.subTotalDescuento || 0) / cantidad,
      descuento: detalle.descuento,
      subTotalOriginal: detalle.subTotalDescuento,
      cantidadSeleccionada: detalle.cantidad,
      seleccionado: false
    };
  }

  private iniciarCargaProductos(tipoDocumento: string): void {
    this.cargandoProductosOrigen = true;
    swal.fire({
      toast: true,
      position: 'top-end',
      title: 'Buscando productos...',
      text: `Consultando el detalle de la ${tipoDocumento}.`,
      showConfirmButton: false,
      allowOutsideClick: false,
      allowEscapeKey: false,
      didOpen: () => swal.showLoading()
    });
  }

  private finalizarCargaProductos(): void {
    this.cargandoProductosOrigen = false;
    swal.close();
  }

  /**
   * Recupera el precio unitario HISTÓRICO (antes de descuento) de una línea
   * origen. Las dos tablas de detalle usan convenciones distintas para
   * `subTotal`, así que hay que decidir cuál aplica línea por línea:
   *
   * - `proformas_detalle` guarda el bruto (100% de las líneas con descuento).
   * - `facturas_detalle` guarda el neto en el 94% de las líneas, es decir
   *   `subTotal == subTotalDescuento`; ahí hay que despejar el bruto.
   *
   * Usar el bruto cuando existe no es solo más preciso (evita el redondeo del
   * despeje): es lo único que funciona con descuento del 100%, donde el
   * despeje dividiría entre cero.
   */
  private unitarioHistorico(detalle: DetalleDocumentoDto): number {
    const cantidad = detalle.cantidad || 1;
    const subTotal = detalle.subTotal || 0;
    const subTotalDescuento = detalle.subTotalDescuento || 0;

    if (subTotal > subTotalDescuento) {
      return subTotal / cantidad;
    }

    const factor = 1 - (detalle.descuento || 0) / 100;
    const unitarioConDescuento = subTotalDescuento / cantidad;
    return factor > 0 ? unitarioConDescuento / factor : unitarioConDescuento;
  }

  private resetEstadoModal(): void {
    this.paginaActual = 0;
    this.filtroModal = '';
  }

  cerrarModalProductos(): void {
    this.mostrarModalProductos = false;
  }

  toggleProducto(index: number): void {
    this.itemsSeleccionables[index].seleccionado = !this.itemsSeleccionables[index].seleccionado;
    if (!this.itemsSeleccionables[index].seleccionado) {
      this.itemsSeleccionables[index].cantidadSeleccionada = 0;
    } else {
      this.itemsSeleccionables[index].cantidadSeleccionada = this.itemsSeleccionables[index].cantidadOriginal;
    }
  }

  actualizarCantidadSeleccion(index: number, event): void {
    const cantidad = +event.target.value;
    const maxCantidad = this.itemsSeleccionables[index].cantidadOriginal;

    if (cantidad < 0) {
      event.target.value = 0;
      this.itemsSeleccionables[index].cantidadSeleccionada = 0;
      return;
    }

    if (cantidad > maxCantidad) {
      swal.fire('Cantidad Excedida', `La cantidad máxima para este producto es ${maxCantidad}.`, 'warning');
      event.target.value = maxCantidad;
      this.itemsSeleccionables[index].cantidadSeleccionada = maxCantidad;
      return;
    }

    this.itemsSeleccionables[index].cantidadSeleccionada = cantidad;
  }

  hayProductosSeleccionados(): boolean {
    return this.itemsSeleccionables.some(i => i.seleccionado && i.cantidadSeleccionada > 0);
  }

  get cantidadProductosSeleccionados(): number {
    return this.itemsSeleccionables.filter(i => i.seleccionado && i.cantidadSeleccionada > 0).length;
  }

  // --- Paginación y filtro del modal ---

  get itemsFiltrados(): ItemSeleccionable[] {
    if (!this.filtroModal) {
      return this.itemsSeleccionables;
    }
    const filtro = this.filtroModal.toLowerCase();
    return this.itemsSeleccionables.filter(reg =>
      reg.nombreProducto.toLowerCase().includes(filtro) ||
      (reg.codProducto && reg.codProducto.toLowerCase().includes(filtro))
    );
  }

  get itemsPaginados(): ItemSeleccionable[] {
    const inicio = this.paginaActual * this.pageSize;
    return this.itemsFiltrados.slice(inicio, inicio + this.pageSize);
  }

  get totalPaginasModal(): number {
    return Math.ceil(this.itemsFiltrados.length / this.pageSize);
  }

  get paginasVisiblesModal(): number[] {
    const paginas: number[] = [];
    const rango = 2;
    let inicio = Math.max(0, this.paginaActual - rango);
    let fin = Math.min(this.totalPaginasModal - 1, this.paginaActual + rango);

    if (this.paginaActual - rango < 0) {
      fin = Math.min(this.totalPaginasModal - 1, fin + (rango - this.paginaActual));
    }
    if (this.paginaActual + rango > this.totalPaginasModal - 1) {
      inicio = Math.max(0, inicio - (this.paginaActual + rango - (this.totalPaginasModal - 1)));
    }

    for (let i = inicio; i <= fin; i++) {
      paginas.push(i);
    }
    return paginas;
  }

  onFiltrarModal(valor: string): void {
    this.filtroModal = valor;
    this.paginaActual = 0;
  }

  cambiarPageSizeModal(nuevoSize: number): void {
    this.pageSize = nuevoSize;
    this.paginaActual = 0;
  }

  irPaginaModal(pagina: number): void {
    this.paginaActual = pagina;
  }

  getIndiceReal(indicePaginado: number): number {
    const item = this.itemsPaginados[indicePaginado];
    return this.itemsSeleccionables.indexOf(item);
  }

  // --- Confirmación y creación ---

  confirmarSeleccion(): void {
    const seleccionados = this.itemsSeleccionables.filter(i => i.seleccionado && i.cantidadSeleccionada > 0);

    if (seleccionados.length === 0) {
      swal.fire('Sin Productos', 'Seleccione al menos un producto pendiente de entrega.', 'warning');
      return;
    }

    this.notaCredito.items = seleccionados.map(sel => {
      const item = new NotaCreditoDetalle();
      const producto = new Producto();
      producto.idProducto = sel.idProducto;
      item.producto = producto;
      item.cantidad = sel.cantidadSeleccionada;
      item.descuento = sel.descuento;
      item.subTotal = sel.precioVenta * sel.cantidadSeleccionada;
      item.subTotalDescuento = sel.precioUnitarioConDescuento * sel.cantidadSeleccionada;
      return item;
    });

    this.cerrarModalProductos();
    this.crearNota();
  }

  crearNota(): void {
    this.notaCredito.cliente = this.cliente;
    this.notaCredito.usuario = this.usuario;
    this.notaCredito.tipoDocumentoOrigen = this.tipoOrigen;
    this.notaCredito.total = this.notaCredito.calcularTotal();

    this.notaService.create(this.notaCredito).subscribe(
      response => {
        this.cliente = new Cliente();
        this.notaCredito = new NotaCredito();
        this.notaCredito.tipoDocumentoOrigen = this.tipoOrigen;
        if (this.myBuscarTexto) {
          this.myBuscarTexto.nativeElement.value = '';
        }
        this.router.navigate(['/notas-credito/index']);
        swal.fire('Nota Creada', `Nota de Crédito No. ${response.idNotaCredito} creada satisfactoriamente.`, 'success');
        this.printNote(response.idNotaCredito);
      }
    );
  }

  printNote(idNota: number): void {
    this.notaService.getNotaCreditoPDF(idNota).subscribe(
      response => {
        const file = new Blob([response.data], { type: 'application/pdf' });
        const fileURL = URL.createObjectURL(file);
        window.open(fileURL);
      }
    );
  }

  // --- Helpers de UI ---

  /**
   * Indica si el botón "Guardar" puede habilitarse según el tipo de origen
   * y el cliente seleccionado.
   */
  get puedeGuardar(): boolean {
    if (!this.cliente?.idCliente) {
      return false;
    }
    if (this.tipoOrigen === 'FACTURA') {
      return !!(this.notaCredito.correlativoFacturaSat && this.notaCredito.serieFacturaSat);
    }
    return !!this.notaCredito.noProforma;
  }

  /** Etiqueta a mostrar en el header del modal según el origen. */
  get tituloModal(): string {
    if (this.documentoOrigenCargado) {
      const documento = this.tipoOrigen === 'FACTURA' ? 'Factura' : 'Proforma';
      return `Productos de ${documento} No. ${this.documentoOrigenCargado.numero}`;
    }
    return 'Productos del Documento Origen';
  }

}
