import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { forkJoin } from 'rxjs';

import { UsuarioAuxiliar } from '../../../models/auxiliar/usuario-auxiliar';
import { Cliente } from '../../../models/cliente';
import { Correlativo } from '../../../models/correlativo';
import { DetalleFactura } from '../../../models/detalle-factura';
import { Factura } from '../../../models/factura';
import { Producto } from '../../../models/producto';

import { AuthService } from '../../../services/auth.service';
import { ClienteService } from '../../../services/cliente.service';
import { CorrelativoService } from '../../../services/correlativos/correlativo.service';
import { ClienteCreateService } from '../../../services/facturas/cliente-create.service';
import { FacturaService } from '../../../services/facturas/factura.service';
import { ProductoService } from '../../../services/producto.service';
import { UsuarioService } from '../../../services/usuarios/usuario.service';
import { ProformaService } from '../../../services/proformas/proforma.service';
import { Proforma } from '../../../models/proforma';
import { DetalleProforma } from '../../../models/detalle-proforma';

import swal from 'sweetalert2';

type CampoDetalleEditable = 'cantidad' | 'descuento';

@Component({
  selector: 'app-create-factura',
  templateUrl: './create-factura.component.html',
  styleUrls: [
    '../../productos/create-producto/create-producto.component.css',
    '../../proformas/create-proforma/create-proforma.component.css',
    './create-factura.component.css'
  ]
})
export class CreateFacturaComponent implements OnInit {

  title: string;
  nitIngresado: string;
  nitBusqueda = '';
  isSaving = false;
  modalProductoVisible = false;
  modalClienteVisible = false;
  stockProformaCargando = false;

  edicionDetalleAbierta = false;
  campoDetalleEdicion: CampoDetalleEditable = null;
  indiceDetalleEdicion = -1;
  valorDetalleAnterior: number = null;
  valorDetalleNuevo: number = null;
  errorEdicionDetalle = '';

  private elementoOrigenEdicion: HTMLElement;

  producto: Producto;
  cantidadProducto: number = null;
  descuentoProducto = 0;
  cliente: Cliente;
  usuario: UsuarioAuxiliar;
  factura: Factura;
  proforma: Proforma;
  correlativo: Correlativo;

  efectivo: number;
  cambio = 0.00;

  constructor(
    private facturaService: FacturaService,
    private proformaService: ProformaService,
    private productoService: ProductoService,
    private clienteService: ClienteService,
    private usuarioService: UsuarioService,
    private clienteCreateService: ClienteCreateService,
    private correlativoService: CorrelativoService,
    public authService: AuthService,
    private activatedRoute: ActivatedRoute
  ) {
    this.title = 'Crear Factura';
    this.cliente = new Cliente();
    this.usuario = new UsuarioAuxiliar();
    this.factura = new Factura();
    this.correlativo = new Correlativo();
    this.producto = new Producto();
    this.proforma = null;
  }

  ngOnInit(): void {
    this.usuarioService.getUsuario(this.authService.usuario.idUsuario).subscribe(
      usuario => {
        this.usuario = usuario;
        this.cargarCorrelativo();
      }
    );

    this.cargarProforma();
  }

  buscarCliente(): void {
    const nit = (this.nitBusqueda || '').trim();

    if (nit) {
      this.clienteService.getClienteByNit(nit).subscribe(
        cliente => {
          this.cliente = cliente;
          (document.getElementById('codigo')).focus();
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

  cargarCliente(cliente: Cliente): void {
    this.cliente = cliente;
    this.nitBusqueda = cliente.nit;
    this.nitIngresado = null;
  }

  cargarCorrelativo(): void {
    if (this.usuario) {
      this.correlativoService.getCorrelativoPorUsuario(this.usuario.idUsuario).subscribe(
        correlativo => {
          if (correlativo) {
            this.correlativo = correlativo;
          } else {
            swal.fire('¡Error al Cargar Correlativo!', 'El usuario no cuenta con un correlativo activo', 'error');
          }
        },
        error => {
          swal.fire('Error al cargar correlativo', error.error.message, 'error');
        }
      );
    }
  }

  buscarProducto(): void {
    const codigo = this.producto.codProducto;

    if (codigo) {
      this.productoService.getProductoByCode(codigo).subscribe(
        producto => {
          this.producto = producto;
          this.enfocarCantidad();
        },
        error => {
          if (error.status === 400) {
            swal.fire(`Error: ${error.error.status}`, 'Petición no se puede llevar a cabo.', 'error');
          }

          if (error.status === 404) {
            swal.fire(`Error: ${error.error.status}`, error.error.message, 'error');
          }
        });
    } else {
      swal.fire('Código Inválido', 'Ingrese un código de producto válido para realizar la búsqueda.', 'warning');
    }
  }

  agregarLinea(): void {
    if (!this.cliente) { // Comprueba que el cliente exista
      swal.fire('Ha ocurrido un Problema', 'Por favor, elija un cliente antes de llevar a cabo la venta.', 'error');
    } else {
      if (this.producto) { // comprueba que el producto exista
        const item = new DetalleFactura();

        item.cantidad = Number(this.cantidadProducto);
        item.descuento = Number(this.descuentoProducto || 0);

        if (!Number.isInteger(item.cantidad) || item.cantidad <= 0) {
          swal.fire('Cantidad Inválida', 'La cantidad debe ser un número entero mayor a 0.', 'warning');
          return;
        }

        if (!Number.isFinite(item.descuento) || item.descuento < 0 || item.descuento > 100) {
          swal.fire('Descuento Inválido', 'El descuento debe estar entre 0% y 100%.', 'warning');
          return;
        }

        if (item.cantidad > this.producto.stock) {
          swal.fire('Stock Insuficiente', 'No existen las suficientes existencias de este producto.', 'warning');
          return;
        } else {
          if (item.cantidad && item.cantidad !== 0) {
            if (this.existeItem(this.producto.idProducto)) {
              this.incrementaCantidad(this.producto.idProducto, item.cantidad, item.descuento);
              this.producto = new Producto();
              this.cantidadProducto = null;
              this.descuentoProducto = 0;
            } else {
              item.producto = this.producto;
              item.subTotalDescuento = item.calcularImporteDescuento();
              item.subTotal = item.calcularImporte();

              this.factura.itemsFactura = [...this.factura.itemsFactura, item];
              this.producto = new Producto();
              this.cantidadProducto = null;
              this.descuentoProducto = 0;
              this.calcularCambio();
            }

          } else if (item.cantidad === 0) {
            swal.fire('Cantidad Erronéa', 'La cantidad a agregar debe ser mayor a 0.', 'warning');
          } else if (!item.cantidad) {
            swal.fire('Valor Inválido', 'La cantidad no puede estar vacía.  Ingrese un valor válido.', 'warning');
          }
        }
      }
    }
  }

  existeItem(id: number): boolean {
    let existe = false;
    this.factura.itemsFactura.forEach((item: DetalleFactura) => {
      if (id === item.producto.idProducto) {
        existe = true;
      }
    });
    return existe;
  }

  incrementaCantidad(idProducto: number, cantidad: number, descuento: number): void {
    this.factura.itemsFactura = this.factura.itemsFactura.map((item: DetalleFactura) => {
      if (idProducto === item.producto.idProducto) {
        item.cantidad = item.cantidad + cantidad;
        item.descuento = descuento;
        item.subTotal = item.calcularImporte();
        item.subTotalDescuento = item.calcularImporteDescuento();
      }

      return item;
    });

    if (this.proforma) {
      this.proforma.itemsProforma = this.proforma.itemsProforma.map((item: DetalleProforma) => {
        if (idProducto === item.producto.idProducto) {
          item.cantidad = item.cantidad + cantidad;
          item.descuento = descuento;
          item.subTotal = item.calcularImporte();
          item.subTotalDescuento = item.calcularImporteDescuento();
        }
        return item;
      });
    }

    this.calcularCambio();
  }

  eliminarItem(index: number): void {
    const item = this.factura.itemsFactura[index];
    if (!item) {
      return;
    }

    swal.fire({
      title: '¿Eliminar producto?',
      text: `Se eliminará ${item.producto.nombre} del detalle de la factura.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar',
      reverseButtons: true,
      focusCancel: true
    }).then(result => {
      if (!result.isConfirmed) {
        return;
      }

      this.factura.itemsFactura = this.factura.itemsFactura.filter((detalle, itemIndex) => itemIndex !== index);
      if (this.proforma) {
        this.proforma.itemsProforma = this.proforma.itemsProforma.filter((detalle, itemIndex) => itemIndex !== index);
        this.proforma.total = this.proforma.calcularTotal();
      }
      this.calcularCambio();
    });
  }

  abrirEdicionDetalle(index: number, campo: CampoDetalleEditable, origen: EventTarget): void {
    const item = this.factura.itemsFactura[index];
    if (!item) {
      return;
    }

    this.indiceDetalleEdicion = index;
    this.campoDetalleEdicion = campo;
    this.valorDetalleAnterior = campo === 'cantidad' ? Number(item.cantidad) : Number(item.descuento || 0);
    this.valorDetalleNuevo = null;
    this.errorEdicionDetalle = '';
    this.elementoOrigenEdicion = origen as HTMLElement;
    this.edicionDetalleAbierta = true;

    setTimeout(() => (document.getElementById('factura-nuevo-valor-detalle') as HTMLInputElement)?.focus());
  }

  confirmarEdicionDetalle(): void {
    const item = this.factura.itemsFactura[this.indiceDetalleEdicion];
    const valorVacio = this.valorDetalleNuevo === null || this.valorDetalleNuevo === undefined
      || String(this.valorDetalleNuevo).trim() === '';

    if (!item || valorVacio) {
      this.errorEdicionDetalle = 'Ingresa un valor nuevo.';
      return;
    }

    const nuevoValor = Number(this.valorDetalleNuevo);
    if (!Number.isFinite(nuevoValor)) {
      this.errorEdicionDetalle = 'Ingresa un valor numérico válido.';
      return;
    }

    if (this.campoDetalleEdicion === 'cantidad') {
      if (!Number.isInteger(nuevoValor) || nuevoValor <= 0) {
        this.errorEdicionDetalle = 'La cantidad debe ser un número entero mayor a 0.';
        return;
      }
      if (nuevoValor > Number(item.producto.stock)) {
        this.errorEdicionDetalle = `La cantidad no puede superar el stock disponible (${item.producto.stock}).`;
        return;
      }
      item.cantidad = nuevoValor;
    } else {
      if (nuevoValor < 0 || nuevoValor > 100) {
        this.errorEdicionDetalle = 'El descuento debe estar entre 0% y 100%.';
        return;
      }
      item.descuento = nuevoValor;
    }

    item.subTotal = item.calcularImporte();
    item.subTotalDescuento = item.calcularImporteDescuento();
    this.factura.itemsFactura = [...this.factura.itemsFactura];
    this.sincronizarDetalleProforma(item);
    this.calcularCambio();
    this.cerrarEdicionDetalle();
  }

  cancelarEdicionDetalle(): void {
    this.cerrarEdicionDetalle();
  }

  evitarCambioConRueda(event: WheelEvent): void {
    (event.target as HTMLInputElement).blur();
  }

  get tituloEdicionDetalle(): string {
    return this.campoDetalleEdicion === 'cantidad' ? 'Editar cantidad' : 'Editar descuento';
  }

  get stockDetalleEdicion(): number {
    const item = this.factura.itemsFactura[this.indiceDetalleEdicion];
    return item ? Number(item.producto.stock) : 0;
  }

  private sincronizarDetalleProforma(itemFactura: DetalleFactura): void {
    if (!this.proforma) {
      return;
    }

    this.proforma.itemsProforma = this.proforma.itemsProforma.map((item: DetalleProforma) => {
      if (item.producto.idProducto === itemFactura.producto.idProducto) {
        item.cantidad = itemFactura.cantidad;
        item.descuento = itemFactura.descuento;
        item.subTotal = item.calcularImporte();
        item.subTotalDescuento = item.calcularImporteDescuento();
        item.nuevoPrecioVenta = item.calcularNuevoPrecioVenta();
      }
      return item;
    });
    this.proforma.total = this.proforma.calcularTotal();
  }

  private cerrarEdicionDetalle(): void {
    const elementoOrigen = this.elementoOrigenEdicion;
    this.edicionDetalleAbierta = false;
    this.campoDetalleEdicion = null;
    this.indiceDetalleEdicion = -1;
    this.valorDetalleAnterior = null;
    this.valorDetalleNuevo = null;
    this.errorEdicionDetalle = '';
    this.elementoOrigenEdicion = null;
    setTimeout(() => elementoOrigen?.focus());
  }

  createFactura(): void {
    if (!this.pagoSuficiente() || this.isSaving) {
      return;
    }

    this.isSaving = true;
    this.factura.noFactura = this.correlativo.correlativoActual;
    this.factura.serie = this.correlativo.serie;
    this.factura.cliente = this.cliente;
    this.factura.usuario = this.usuario;
    this.factura.total = this.factura.calcularTotal();

    // this.facturaService.create(this.factura).subscribe(
    //   response => {
    //     this.cliente = new Cliente();
    //     this.factura = new Factura();
    //     this.cargarCorrelativo();
    //     (document.getElementById('buscar') as HTMLInputElement).value = '';
    //     swal.fire('Venta Realizada', `Factura No. ${response.factura.noFactura} creada con éxito!`, 'success');
    //     (document.getElementById('buscar') as HTMLInputElement).focus();
    //     this.cambio = 0;
    //     (document.getElementById('efectivo') as HTMLInputElement).value = '';

    //     const url = 'https://report.feel.com.gt/ingfacereport/ingfacereport_documento?uuid=' + response.factura.certificacionSat;

    //     const a = document.createElement('a');
    //     window.open(url, '_blank').focus();

    //     // this.facturaService.getBillPDF(response.factura.idFactura).subscribe(res => {
    //     //   const url = window.URL.createObjectURL(res.data);
    //     //   const a = document.createElement('a');
    //     //   document.body.appendChild(a);
    //     //   a.setAttribute('style', 'display: none');
    //     //   a.setAttribute('target', 'blank');
    //     //   a.href = url;
    //     //   /*
    //     //     opcion para pedir descarga de la respuesta obtenida
    //     //     a.download = response.filename;
    //     //   */
    //     //   window.open(a.toString(), '_blank');
    //     //   window.URL.revokeObjectURL(url);
    //     //   a.remove();
    //     // },
    //     //   error => {
    //     //     console.log(error);
    //     //   });
    //   }
    // );

    this.facturaService.createV2(this.factura).subscribe(
      response => {
        this.isSaving = false;
        this.cliente = new Cliente();
        this.factura = new Factura();
        this.nitBusqueda = '';
        this.cargarCorrelativo();
        swal.fire('Venta Realizada', `Factura No. ${response.noFactura} creada con éxito!`, 'success');
        (document.getElementById('buscar') as HTMLInputElement).focus();
        this.cambio = 0;
        this.efectivo = null;

        const url = 'https://report.feel.com.gt/ingfacereport/ingfacereport_documento?uuid=' + response.certificacionSat;

        const a = document.createElement('a');
        window.open(url, '_blank').focus();
      }, error => {
        this.isSaving = false;
        swal.fire(`Error: ${error.error.status} al Crear Factura`, `${error.error.message}`, 'error');
      }
    );
  }

  cargarProforma(): void {
    this.activatedRoute.params.subscribe(params => {
      const id = params.proformaId;

      if (id) {
        this.buscarProformaPorId(id);
      }
    }, error => {
      swal.fire(`Error al cargar proforma`, `${error.error.message}`, 'error');
    });
  }

  buscarProformaPorId(id: number): void {
    this.stockProformaCargando = true;
    this.proformaService.getProforma(id).subscribe(
      proforma => {
        this.proforma = proforma;
        this.factura.idProformaOrigen = proforma.idProforma;
        this.cliente = proforma.cliente;
        this.nitBusqueda = this.cliente.nit || '';
        this.cargarStockActualDeProforma();
      }, error => {
        this.stockProformaCargando = false;
        swal.fire(`Ha ocurrido un error: ${error.error.status}`, `${error.error.message}`, 'error');
      }
    );
  }

  private cargarStockActualDeProforma(): void {
    const items = this.proforma.itemsProforma || [];
    if (items.length === 0) {
      this.cargarDetalleFacturaDesdeProforma();
      return;
    }

    forkJoin(items.map(item => this.productoService.getProductoByCode(item.producto.codProducto))).subscribe(
      productos => {
        productos.forEach((producto, index) => items[index].producto.stock = producto.stock);
        this.cargarDetalleFacturaDesdeProforma();
        this.mostrarAlertaStockInsuficiente();
      }, () => {
        this.stockProformaCargando = false;
        swal.fire('No fue posible verificar existencias',
          'Actualice la página antes de intentar facturar esta proforma.', 'error');
      }
    );
  }

  private cargarDetalleFacturaDesdeProforma(): void {
    this.factura.itemsFactura = this.proforma.itemsProforma.map(itemProforma => {
      const item = new DetalleFactura();
      item.cantidad = itemProforma.cantidad;
      item.subTotal = itemProforma.subTotal;
      item.subTotalDescuento = itemProforma.subTotalDescuento;
      item.producto = itemProforma.producto;
      item.descuento = itemProforma.descuento;
      return item;
    });
    this.stockProformaCargando = false;
    this.recalcularTotal();
  }

  calcularCambio(): void {
    this.recalcularTotal();
    if (this.efectivo) {
      this.cambio = this.efectivo - this.factura.total;
    } else {
      this.cambio = 0.00;
    }
  }

  pagoSuficiente(): boolean {
    return !!this.efectivo && this.efectivo >= this.factura.total;
  }

  saldoPendiente(): number {
    const efectivoRecibido = Number(this.efectivo) || 0;
    return Math.max(this.factura.total - efectivoRecibido, 0);
  }

  cantidadesValidas(): boolean {
    const cantidadesCorrectas = this.factura.itemsFactura.every((item: DetalleFactura) =>
      Number.isInteger(Number(item.cantidad))
      && Number(item.cantidad) > 0
    );
    return cantidadesCorrectas && this.productosSinStock().length === 0;
  }

  productosSinStock(): DetalleFactura[] {
    return this.factura.itemsFactura.filter((item: DetalleFactura) =>
      this.stockInsuficiente(item)
    );
  }

  stockInsuficiente(item: DetalleFactura): boolean {
    return this.cantidadSolicitada(item) > Number(item.producto.stock);
  }

  private cantidadSolicitada(item: DetalleFactura): number {
    return this.factura.itemsFactura
      .filter(detalle => this.mismoProducto(detalle, item))
      .reduce((cantidad, detalle) => cantidad + Number(detalle.cantidad), 0);
  }

  private mismoProducto(primerItem: DetalleFactura, segundoItem: DetalleFactura): boolean {
    if (primerItem.producto.idProducto && segundoItem.producto.idProducto) {
      return primerItem.producto.idProducto === segundoItem.producto.idProducto;
    }
    return primerItem.producto.codProducto === segundoItem.producto.codProducto;
  }

  private mostrarAlertaStockInsuficiente(): void {
    const productos = this.productosSinStock().filter((item, index, items) =>
      items.findIndex(otroItem => this.mismoProducto(otroItem, item)) === index
    );
    if (productos.length === 0) {
      return;
    }

    const listado = productos.map(item => {
      const codigo = this.escaparHtml(item.producto.codProducto);
      const solicitadas = this.cantidadSolicitada(item);
      return `<article class="stock-alert-product">
        <div class="stock-alert-product-code">
          <span>Código de producto</span>
          <strong>${codigo}</strong>
        </div>
        <div class="stock-alert-quantities">
          <span><small>Solicitadas</small><strong>${solicitadas}</strong></span>
          <i class="fas fa-long-arrow-alt-right" aria-hidden="true"></i>
          <span><small>Disponibles</small><strong>${item.producto.stock}</strong></span>
        </div>
      </article>`;
    }).join('');

    swal.fire({
      title: 'Revisa las existencias',
      html: `<p class="stock-alert-intro">
          Esta proforma solicita más unidades de las disponibles en tu sucursal.
        </p>
        <div class="stock-alert-products">${listado}</div>
        <p class="stock-alert-note">
          <i class="fas fa-info-circle" aria-hidden="true"></i>
          Los renglones afectados están resaltados en rojo dentro del detalle.
        </p>`,
      icon: 'warning',
      confirmButtonText: 'Revisar productos',
      width: 560,
      buttonsStyling: false,
      customClass: {
        popup: 'stock-alert-popup',
        icon: 'stock-alert-icon',
        title: 'stock-alert-title',
        htmlContainer: 'stock-alert-content',
        actions: 'stock-alert-actions',
        confirmButton: 'stock-alert-confirm'
      }
    });
  }

  private escaparHtml(valor: string): string {
    const elemento = document.createElement('div');
    elemento.textContent = valor;
    return elemento.innerHTML;
  }

  private recalcularTotal(): void {
    this.factura.total = this.factura.calcularTotal();
  }

  abrirModalProducto(): void {
    this.modalProductoVisible = true;
  }

  cerrarModalProducto(): void {
    this.modalProductoVisible = false;
  }

  abrirModalCliente(): void {
    this.modalClienteVisible = true;
  }

  cerrarModalCliente(): void {
    this.modalClienteVisible = false;
  }

  loadProducto(producto: Producto): void {
    this.producto.codProducto = producto.codProducto;
    this.cerrarModalProducto();
    this.buscarProducto();
  }

  private enfocarCantidad(): void {
    setTimeout(() => (document.getElementById('cantidad') as HTMLInputElement)?.focus(), 350);
  }

  loadCliente(cliente: Cliente): void {
    this.nitBusqueda = cliente.nit;
    this.cerrarModalCliente();
    this.buscarCliente();
  }

}
