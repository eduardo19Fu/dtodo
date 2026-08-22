import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

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
import { ModalCambioService } from '../../../services/facturas/modal-cambio.service';
import { ProformaService } from '../../../services/proformas/proforma.service';
import { Proforma } from '../../../models/proforma';
import { DetalleProforma } from '../../../models/detalle-proforma';

import swal from 'sweetalert2';

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
  pagar = false;
  isSaving = false;

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
    private modalCambioService: ModalCambioService,
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
    const nit = ((document.getElementById('buscar') as HTMLInputElement)).value;

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

  cargarCliente(event): void {
    this.cliente = event;
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
    const codigo = ((document.getElementById('codigo') as HTMLInputElement)).value;

    if (codigo) {
      this.productoService.getProductoByCode(codigo).subscribe(
        producto => {
          this.producto = producto;
          (document.getElementById('cantidad') as HTMLInputElement).focus();
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
    this.factura.itemsFactura = this.factura.itemsFactura.filter((item, itemIndex) => itemIndex !== index);
    if (this.proforma) {
      this.proforma.itemsProforma = this.proforma.itemsProforma.filter((item, itemIndex) => itemIndex !== index);
    }
    this.calcularCambio();
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
        this.cargarCorrelativo();
        (document.getElementById('buscar') as HTMLInputElement).value = '';
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
    this.proformaService.getProforma(id).subscribe(
      proforma => {
        this.proforma = proforma;

        this.cliente = proforma.cliente;
        this.factura.total = this.proforma.total;

        this.proforma.itemsProforma.forEach((itemProforma) => {
          const item: DetalleFactura = new DetalleFactura();

          item.cantidad = itemProforma.cantidad;
          item.subTotal = itemProforma.subTotal;
          item.subTotalDescuento = itemProforma.subTotalDescuento;
          item.producto = itemProforma.producto;
          item.descuento = itemProforma.descuento;

          this.factura.itemsFactura = [...this.factura.itemsFactura, item];
        });
        this.recalcularTotal();
      }, error => {
        swal.fire(`Ha ocurrido un error: ${error.error.status}`, `${error.error.message}`, 'error');
      }
    );
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

  cantidadesValidas(): boolean {
    return this.factura.itemsFactura.every((item: DetalleFactura) =>
      Number.isInteger(Number(item.cantidad))
      && Number(item.cantidad) > 0
      && Number(item.cantidad) <= item.producto.stock
    );
  }

  private recalcularTotal(): void {
    this.factura.total = this.factura.calcularTotal();
  }

  loadProducto(event): void {
    (document.getElementById('codigo') as HTMLInputElement).value = event.codProducto;
    (document.getElementById('button-x')).click();
    this.buscarProducto();
    (document.getElementById('cantidad') as HTMLInputElement).focus();
  }

  loadCliente(event): void {
    (document.getElementById('buscar') as HTMLInputElement).value = event.nit;
    (document.getElementById('button-2x')).click();
    this.buscarCliente();
  }

}
