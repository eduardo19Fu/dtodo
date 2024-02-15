import { Component, OnInit } from '@angular/core';

import { UsuarioAuxiliar } from 'src/app/models/auxiliar/usuario-auxiliar';
import { Cliente } from 'src/app/models/cliente';
import { Correlativo } from 'src/app/models/correlativo';
import { DetalleFactura } from 'src/app/models/detalle-factura';
import { Factura } from 'src/app/models/factura';
import { Producto } from 'src/app/models/producto';

import { AuthService } from 'src/app/services/auth.service';
import { ClienteService } from 'src/app/services/cliente.service';
import { CorrelativoService } from 'src/app/services/correlativos/correlativo.service';
import { ClienteCreateService } from 'src/app/services/facturas/cliente-create.service';
import { FacturaService } from 'src/app/services/facturas/factura.service';
import { ProductoService } from 'src/app/services/producto.service';
import { UsuarioService } from 'src/app/services/usuarios/usuario.service';

import swal from 'sweetalert2';
import { ModalCambioService } from '../../../services/facturas/modal-cambio.service';
import { ProformaService } from '../../../services/proformas/proforma.service';
import { Proforma } from '../../../models/proforma';
import { DetalleProforma } from '../../../models/detalle-proforma';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-create-factura',
  templateUrl: './create-factura.component.html',
  styleUrls: ['./create-factura.component.css']
})
export class CreateFacturaComponent implements OnInit {

  title: string;
  nitIngresado: string;
  pagar = false;

  producto: Producto;
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
          this.correlativo = correlativo;
        },
        error => {
          swal.fire('Error al cargar correlativo', error.error.mensaje, 'error');
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
            swal.fire(`Error: ${error.status}`, 'Petición no se puede llevar a cabo.', 'error');
          }

          if (error.status === 404) {
            swal.fire(`Error: ${error.status}`, error.error.mensaje, 'error');
          }
        }
      );
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

        item.cantidad = +((document.getElementById('cantidad') as HTMLInputElement)).value; // valor obtenido del formulario de cantidad
        item.descuento = 0; // valor obtenido del input de descuento

        if (item.cantidad > this.producto.stock) {
          swal.fire('Stock Insuficiente', 'No existen las suficientes existencias de este producto.', 'warning');
          return;
        } else {
          if (item.cantidad && item.cantidad !== 0) {
            if (this.existeItem(this.producto.idProducto)) {
              this.incrementaCantidad(this.producto.idProducto, item.cantidad);
              this.producto = new Producto();
              (document.getElementById('cantidad') as HTMLInputElement).value = '';
            } else {
                item.producto = this.producto;
                item.subTotalDescuento = item.calcularImporte();
                item.subTotal = item.calcularImporte();

                this.factura.itemsFactura.push(item);
                this.producto = new Producto();

                (document.getElementById('cantidad') as HTMLInputElement).value = '';
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

  actualizarCantidad(idProducto: number, event: any): void {
    const cantidad = event.target.value as number;

    this.factura.itemsFactura = this.factura.itemsFactura.map((item: DetalleFactura) => {
      if (idProducto === item.producto.idProducto) {
        if (cantidad > item.producto.stock) {
          swal.fire('Stock Insuficiente', 'No existen las suficientes existencias de este producto.', 'warning');
        } else {
          item.cantidad = cantidad;
          item.subTotal = item.calcularImporte();
          item.subTotalDescuento = item.calcularImporteDescuento();
        }
      }

      return item;
    });

    this.proforma.itemsProforma = this.proforma.itemsProforma.map((item: DetalleProforma) => {

      if (idProducto === item.producto.idProducto) {
        item.cantidad = cantidad;
        item.subTotal = item.calcularImporte();
        item.subTotalDescuento = item.calcularImporteDescuento();
      }

      return item;
    });
  }

  actualizarCantidadDescuento(idProducto: number, event: any): void {
    const descuento = event.target.value as number;

    this.factura.itemsFactura = this.factura.itemsFactura.map((item: DetalleFactura) => {
      if (idProducto === item.producto.idProducto) {
        item.descuento = descuento;
        item.subTotal = item.calcularImporte();
        item.subTotalDescuento = item.calcularImporteDescuento();
      }

      return item;
    });
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

  incrementaCantidad(idProducto: number, cantidad: number): void {
    this.factura.itemsFactura = this.factura.itemsFactura.map((item: DetalleFactura) => {
      if (idProducto === item.producto.idProducto) {
        item.cantidad = item.cantidad + cantidad;
        item.subTotal = item.calcularImporte();
        item.subTotalDescuento = item.calcularImporteDescuento();
      }

      return item;
    });
  }

  eliminarItem(index: number): void {
    this.factura.itemsFactura.splice(index, 1);
    this.proforma.itemsProforma.splice(index, 1);
  }

  createFactura(): void {
    this.factura.noFactura = this.correlativo.correlativoActual;
    this.factura.serie = this.correlativo.serie;
    this.factura.cliente = this.cliente;
    this.factura.usuario = this.usuario;
    this.factura.total = this.factura.calcularTotal();

    this.facturaService.create(this.factura).subscribe(
      response => {
        this.cliente = new Cliente();
        this.factura = new Factura();
        this.cargarCorrelativo();
        (document.getElementById('buscar') as HTMLInputElement).value = '';
        swal.fire('Venta Realizada', `Factura No. ${response.factura.noFactura} creada con éxito!`, 'success');
        (document.getElementById('buscar') as HTMLInputElement).focus();
        this.cambio = 0;
        (document.getElementById('efectivo') as HTMLInputElement).value = '';

        const url = 'https://report.feel.com.gt/ingfacereport/ingfacereport_documento?uuid=' + response.factura.certificacionSat;

        const a = document.createElement('a');
        window.open(url, '_blank').focus();

        // this.facturaService.getBillPDF(response.factura.idFactura).subscribe(res => {
        //   const url = window.URL.createObjectURL(res.data);
        //   const a = document.createElement('a');
        //   document.body.appendChild(a);
        //   a.setAttribute('style', 'display: none');
        //   a.setAttribute('target', 'blank');
        //   a.href = url;
        //   /*
        //     opcion para pedir descarga de la respuesta obtenida
        //     a.download = response.filename;
        //   */
        //   window.open(a.toString(), '_blank');
        //   window.URL.revokeObjectURL(url);
        //   a.remove();
        // },
        //   error => {
        //     console.log(error);
        //   });
      }
    );
  }

  cargarProforma(): void {
    this.activatedRoute.params.subscribe(params => {
      const id = params['proformaId'];

      if (id) {
        this.buscarProformaPorId(id);
      }
    })
  }

  buscarProformaPorId(id: number): void {
    this.proformaService.getProforma(id).subscribe(
      proforma => {
        this.proforma = proforma;

        this.cliente = proforma.cliente;
        this.factura.total = this.proforma.total;

        this.proforma.itemsProforma.forEach((itemProforma) => {
          let item: DetalleFactura = new DetalleFactura();

          item.cantidad = itemProforma.cantidad;
          item.subTotal = itemProforma.subTotal;
          item.subTotalDescuento = itemProforma.subTotalDescuento;
          item.producto = itemProforma.producto;
          item.descuento = itemProforma.descuento;
          
          this.factura.itemsFactura.push(item);
          console.log(this.factura.itemsFactura);
        });
      }
    );
  }

  calcularCambio(event): void {
    if (this.efectivo) {
      this.cambio = this.efectivo - this.factura.calcularTotal();
    } else {
      this.cambio = 0.00;
    }
  }

  loadProducto(event): void {
    (document.getElementById('codigo') as HTMLInputElement).value = event.codProducto;
    (document.getElementById('button-x')).click();
    this.buscarProducto();
    (document.getElementById('cantidad') as HTMLInputElement).focus();
  }

  loadCliente(event): void {
    console.log(event);
    (document.getElementById('buscar') as HTMLInputElement).value = event.nit;
    (document.getElementById('button-2x')).click();
    this.buscarCliente();
  }

}
