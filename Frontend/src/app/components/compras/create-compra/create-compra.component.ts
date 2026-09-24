import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from 'src/app/services/auth.service';
import { CompraService } from 'src/app/services/compra.service';
import { ProductoService } from 'src/app/services/producto.service';
import { ProveedorService } from 'src/app/services/proveedor.service';
import { SucursalService } from 'src/app/services/sucursal.service';
import { UsuarioService } from 'src/app/services/usuarios/usuario.service';

import { Compra, TipoComprobanteCompra } from 'src/app/models/compra';
import { DetalleCompra } from 'src/app/models/detalle-compra';
import { Producto } from 'src/app/models/producto';
import { Proveedor } from 'src/app/models/proveedor';
import { Sucursal } from 'src/app/models/sucursal';

import swal from 'sweetalert2';

type CampoDetalleEditableCompra = 'cantidad' | 'precioUnitario';

@Component({
  selector: 'app-create-compra',
  templateUrl: './create-compra.component.html',
  styleUrls: [
    '../../productos/create-producto/create-producto.component.css',
    '../../proformas/create-proforma/create-proforma.component.css',
    './create-compra.component.css'
  ]
})
export class CreateCompraComponent implements OnInit {

  title = 'Registrar nueva compra';
  compra: Compra;

  proveedores: Proveedor[] = [];
  sucursales: Sucursal[] = [];

  tiposComprobante: TipoComprobanteCompra[] = ['FACTURA', 'RECIBO', 'NOTA_ENVIO', 'TICKET', 'OTRO'];

  productoActual: Producto = null;
  codigoBusqueda = '';
  cantidadActual: number = null;
  precioUnitarioActual: number = null;

  modalProductoVisible = false;
  modalNuevoProductoVisible = false;

  edicionDetalleAbierta = false;
  campoDetalleEdicion: CampoDetalleEditableCompra = null;
  indiceDetalleEdicion = -1;
  valorDetalleAnterior: number = null;
  valorDetalleNuevo: number = null;
  errorEdicionDetalle = '';

  private elementoOrigenEdicion: HTMLElement;

  guardando = false;

  constructor(
    private compraService: CompraService,
    private proveedorService: ProveedorService,
    private sucursalService: SucursalService,
    private productoService: ProductoService,
    private usuarioService: UsuarioService,
    private authService: AuthService,
    private router: Router
  ) {
    this.compra = new Compra();
    this.compra.fechaCompra = new Date().toISOString().substring(0, 10) as unknown as Date;
  }

  ngOnInit(): void {
    this.proveedorService.getProveedores().subscribe(proveedores => this.proveedores = proveedores);
    this.sucursalService.getSucursales().subscribe(sucursales => {
      this.sucursales = sucursales;
      const idSucursalActiva = this.authService.usuario?.sucursal?.idSucursal;
      this.compra.sucursal = sucursales.find(s => s.idSucursal === idSucursalActiva) || sucursales.find(s => s.esPrincipal);
    });
  }

  compararProveedor(o1: Proveedor, o2: Proveedor): boolean {
    return o1 == null || o2 == null ? false : o1.idProveedor === o2.idProveedor;
  }

  compararSucursal(o1: Sucursal, o2: Sucursal): boolean {
    return o1 == null || o2 == null ? false : o1.idSucursal === o2.idSucursal;
  }

  buscarProductoPorCodigo(): void {
    if (!this.codigoBusqueda) {
      swal.fire('C&oacute;digo inv&aacute;lido', 'Ingresa un c&oacute;digo de producto', 'warning');
      return;
    }
    this.productoService.getProductoByCode(this.codigoBusqueda).subscribe(
      producto => this.seleccionarProducto(producto),
      error => {
        if (error.status === 404) {
          swal.fire('Producto no encontrado', error.error?.message || 'No existe un producto con ese c&oacute;digo', 'error');
        }
      }
    );
  }

  buscarProductoConEnter(evento: KeyboardEvent): void {
    evento.preventDefault();
    this.buscarProductoPorCodigo();
  }

  private seleccionarProducto(producto: Producto): void {
    this.productoActual = producto;
    this.cantidadActual = null;
    this.precioUnitarioActual = producto.precioCompra || null;
  }

  abrirModalProducto(): void {
    this.modalProductoVisible = true;
  }

  cerrarModalProducto(): void {
    this.modalProductoVisible = false;
  }

  loadProducto(producto: Producto): void {
    this.cerrarModalProducto();
    // El modal de catálogo emite un ProductoDto liviano, no la entidad completa
    // (mismo patrón que create-factura): se vuelve a consultar por código.
    this.productoService.getProductoByCode(producto.codProducto).subscribe(
      productoCompleto => this.seleccionarProducto(productoCompleto)
    );
  }

  abrirModalNuevoProducto(): void {
    this.modalNuevoProductoVisible = true;
  }

  cerrarModalNuevoProducto(): void {
    this.modalNuevoProductoVisible = false;
  }

  productoNuevoCreado(producto: Producto): void {
    this.cerrarModalNuevoProducto();
    this.seleccionarProducto(producto);
  }

  agregarLinea(): void {
    if (!this.productoActual) {
      swal.fire('Selecciona un producto', 'Busca un producto existente o registra uno nuevo.', 'warning');
      return;
    }
    if (!this.cantidadActual || this.cantidadActual <= 0) {
      swal.fire('Cantidad inv&aacute;lida', 'La cantidad debe ser mayor a 0.', 'warning');
      return;
    }
    if (!this.precioUnitarioActual || this.precioUnitarioActual <= 0) {
      swal.fire('Precio inv&aacute;lido', 'El precio unitario debe ser mayor a 0.', 'warning');
      return;
    }

    const detalle = new DetalleCompra();
    detalle.producto = this.productoActual;
    detalle.cantidad = this.cantidadActual;
    detalle.precioUnitario = this.precioUnitarioActual;
    detalle.subTotal = detalle.calcularSubTotal();
    this.compra.items.push(detalle);

    this.productoActual = null;
    this.codigoBusqueda = '';
    this.cantidadActual = null;
    this.precioUnitarioActual = null;
  }

  agregarLineaConEnter(evento: KeyboardEvent): void {
    evento.preventDefault();
    this.agregarLinea();
  }

  eliminarLinea(detalle: DetalleCompra): void {
    swal.fire({
      title: '&iquest;Eliminar producto?',
      text: `Se eliminar&aacute; ${detalle.producto.nombre} del detalle de la compra.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'S&iacute;, eliminar',
      cancelButtonText: 'Cancelar',
      reverseButtons: true,
      focusCancel: true
    }).then(resultado => {
      if (resultado.isConfirmed) {
        this.compra.items = this.compra.items.filter(item => item !== detalle);
      }
    });
  }

  abrirEdicionDetalle(index: number, campo: CampoDetalleEditableCompra, origen: EventTarget): void {
    const item = this.compra.items[index];
    if (!item) {
      return;
    }

    this.indiceDetalleEdicion = index;
    this.campoDetalleEdicion = campo;
    this.valorDetalleAnterior = campo === 'cantidad' ? Number(item.cantidad) : Number(item.precioUnitario);
    this.valorDetalleNuevo = null;
    this.errorEdicionDetalle = '';
    this.elementoOrigenEdicion = origen as HTMLElement;
    this.edicionDetalleAbierta = true;

    setTimeout(() => (document.getElementById('compra-nuevo-valor-detalle') as HTMLInputElement)?.focus());
  }

  confirmarEdicionDetalle(): void {
    const item = this.compra.items[this.indiceDetalleEdicion];
    const valorVacio = this.valorDetalleNuevo === null || this.valorDetalleNuevo === undefined
      || String(this.valorDetalleNuevo).trim() === '';

    if (!item || valorVacio) {
      this.errorEdicionDetalle = 'Ingresa un valor nuevo.';
      return;
    }

    const nuevoValor = Number(this.valorDetalleNuevo);
    if (!Number.isFinite(nuevoValor)) {
      this.errorEdicionDetalle = 'Ingresa un valor num&eacute;rico v&aacute;lido.';
      return;
    }

    if (this.campoDetalleEdicion === 'cantidad') {
      if (!Number.isInteger(nuevoValor) || nuevoValor <= 0) {
        this.errorEdicionDetalle = 'La cantidad debe ser un n&uacute;mero entero mayor a 0.';
        return;
      }
      item.cantidad = nuevoValor;
    } else {
      if (nuevoValor <= 0) {
        this.errorEdicionDetalle = 'El precio unitario debe ser mayor a 0.';
        return;
      }
      item.precioUnitario = nuevoValor;
    }

    item.subTotal = item.calcularSubTotal();
    this.compra.items = [...this.compra.items];
    this.cerrarEdicionDetalle();
  }

  cancelarEdicionDetalle(): void {
    this.cerrarEdicionDetalle();
  }

  evitarCambioConRueda(event: WheelEvent): void {
    (event.target as HTMLInputElement).blur();
  }

  get tituloEdicionDetalle(): string {
    return this.campoDetalleEdicion === 'cantidad' ? 'Editar cantidad' : 'Editar precio unitario';
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

  get subTotalDetalle(): number {
    return this.compra.items.reduce((acumulado, item) => acumulado + item.calcularSubTotal(), 0);
  }

  get totalCompra(): number {
    return this.subTotalDetalle + (this.compra.costoEnvio || 0);
  }

  crear(): void {
    if (this.compra.items.length === 0) {
      swal.fire('Sin productos', 'Agrega al menos un producto al detalle de la compra.', 'warning');
      return;
    }
    if (!this.compra.proveedor || !this.compra.sucursal) {
      swal.fire('Datos incompletos', 'Selecciona el proveedor y la sucursal de la compra.', 'warning');
      return;
    }

    this.guardando = true;
    this.usuarioService.getUsuario(this.authService.usuario.idUsuario).subscribe(
      usuario => {
        this.compra.usuario = usuario;
        this.compraService.create(this.compra).subscribe(
          () => {
            this.guardando = false;
            this.router.navigate(['/compras/index']);
            swal.fire('Compra registrada', 'La compra fue registrada con &eacute;xito y el inventario fue actualizado.', 'success');
          },
          () => this.guardando = false
        );
      },
      () => this.guardando = false
    );
  }

}
