import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { UsuarioAuxiliar } from 'src/app/models/auxiliar/usuario-auxiliar';
import { Cliente } from 'src/app/models/cliente';
import { DetalleFactura } from 'src/app/models/detalle-factura';
import { DetalleProforma } from 'src/app/models/detalle-proforma';
import { Factura } from 'src/app/models/factura';
import { NotaCredito, TipoDocumentoOrigen } from 'src/app/models/nota-credito';
import { NotaCreditoDetalle } from 'src/app/models/nota-credito-detalle';
import { Proforma } from 'src/app/models/proforma';
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
  codProducto: string;
  nombreProducto: string;
  cantidadOriginal: number;
  precioVenta: number;
  descuento: number;
  subTotalOriginal: number;
  cantidadSeleccionada: number;
  seleccionado: boolean;
  // Referencias opcionales al item origen
  refDetalleFactura?: DetalleFactura;
  refDetalleProforma?: DetalleProforma;
}

@Component({
  selector: 'app-create-nota',
  templateUrl: './create-nota.component.html',
  styleUrls: ['./create-nota.component.css']
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
  facturaCargada: Factura;
  proformaCargada: Proforma;
  itemsSeleccionables: ItemSeleccionable[] = [];
  mostrarModalProductos: boolean = false;

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
      this.proformaCargada = null;
    } else {
      this.notaCredito.correlativoFacturaSat = null;
      this.notaCredito.serieFacturaSat = null;
      this.facturaCargada = null;
    }
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

    this.facturaService.getFacturaByCorrelativoSat(correlativo, serie).subscribe(
      factura => {
        if (factura.cliente.idCliente !== this.cliente.idCliente) {
          swal.fire('Cliente No Coincide',
            `La factura con correlativo ${correlativo} y serie ${serie} pertenece al cliente "${factura.cliente.nombre}", pero el cliente seleccionado es "${this.cliente.nombre}".`,
            'error');
          return;
        }

        this.facturaCargada = factura;
        this.proformaCargada = null;
        this.itemsSeleccionables = factura.itemsFactura.map((item: DetalleFactura) => ({
          codProducto: item.producto.codProducto,
          nombreProducto: item.producto.nombre,
          cantidadOriginal: item.cantidad,
          // Unitario HISTÓRICO antes de descuento, no el precio de catálogo vigente.
          precioVenta: this.unitarioHistorico(item),
          descuento: item.descuento,
          subTotalOriginal: item.subTotalDescuento,
          cantidadSeleccionada: item.cantidad,
          seleccionado: false,
          refDetalleFactura: item
        }));
        this.resetEstadoModal();
        this.mostrarModalProductos = true;
      },
      () => {
        // FacturaService ya muestra el Swal de error
      }
    );
  }

  private cargarProductosDesdeProforma(): void {
    const noProforma = this.notaCredito.noProforma;

    if (!noProforma) {
      swal.fire('No. Proforma Requerido', 'Por favor, ingrese el número de la proforma asociada.', 'warning');
      return;
    }

    this.proformaService.getProformaByNoProforma(noProforma).subscribe(
      proforma => {
        if (!proforma.cliente || proforma.cliente.idCliente !== this.cliente.idCliente) {
          const nombreCte = proforma.cliente ? proforma.cliente.nombre : 'N/A';
          swal.fire('Cliente No Coincide',
            `La proforma ${noProforma} pertenece al cliente "${nombreCte}", pero el cliente seleccionado es "${this.cliente.nombre}".`,
            'error');
          return;
        }

        this.proformaCargada = proforma;
        this.facturaCargada = null;
        this.itemsSeleccionables = (proforma.itemsProforma || []).map((item: DetalleProforma) => ({
          codProducto: item.producto.codProducto,
          nombreProducto: item.producto.nombre,
          cantidadOriginal: item.cantidad,
          // Unitario HISTÓRICO antes de descuento, no el precio de catálogo vigente.
          precioVenta: this.unitarioHistorico(item),
          descuento: item.descuento,
          subTotalOriginal: item.subTotalDescuento,
          cantidadSeleccionada: item.cantidad,
          seleccionado: false,
          refDetalleProforma: item
        }));
        this.resetEstadoModal();
        this.mostrarModalProductos = true;
      },
      () => {
        // ProformaService ya muestra el Swal de error
      }
    );
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
  private unitarioHistorico(detalle: DetalleFactura | DetalleProforma): number {
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
      // Resolver el detalle origen (Factura o Proforma) como única fuente de
      // verdad: conserva el descuento y el precio HISTÓRICO facturado, no el
      // precio de catálogo vigente (que puede cambiar con el tiempo).
      const detalleOrigen = sel.refDetalleFactura ?? sel.refDetalleProforma;
      const cantidadOrigen = detalleOrigen.cantidad || 1;
      // Unitarios históricos derivados del detalle guardado. El "sin descuento"
      // se despeja (no se lee de subTotal, que en BD ya viene descontado).
      const unitarioSinDescuento = this.unitarioHistorico(detalleOrigen);
      const unitarioConDescuento = detalleOrigen.subTotalDescuento / cantidadOrigen;

      item.producto = detalleOrigen.producto;
      item.cantidad = sel.cantidadSeleccionada;
      item.descuento = detalleOrigen.descuento;
      item.subTotal = unitarioSinDescuento * sel.cantidadSeleccionada;
      item.subTotalDescuento = unitarioConDescuento * sel.cantidadSeleccionada;
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
    if (this.tipoOrigen === 'FACTURA' && this.facturaCargada) {
      return `Productos de Factura No. ${this.facturaCargada.noFactura}`;
    }
    if (this.tipoOrigen === 'PROFORMA' && this.proformaCargada) {
      return `Productos de Proforma No. ${this.proformaCargada.noProforma}`;
    }
    return 'Productos del Documento Origen';
  }

}
