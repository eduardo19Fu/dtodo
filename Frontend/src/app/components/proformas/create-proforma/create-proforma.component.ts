import { Component, OnInit } from '@angular/core';

import { Producto } from '../../../models/producto';
import { Cliente } from '../../../models/cliente';
import { Proforma } from '../../../models/proforma';
import { UsuarioAuxiliar } from '../../../models/auxiliar/usuario-auxiliar';
import { DetalleProforma } from '../../../models/detalle-proforma';

import { ProductoService } from '../../../services/producto.service';
import { ClienteService } from '../../../services/cliente.service';
import { ClienteCreateService } from '../../../services/facturas/cliente-create.service';

import swal from 'sweetalert2';
import { UsuarioService } from 'src/app/services/usuarios/usuario.service';
import { AuthService } from 'src/app/services/auth.service';
import { ProformaService } from 'src/app/services/proformas/proforma.service';

@Component({
  selector: 'app-create-proforma',
  templateUrl: './create-proforma.component.html',
  styleUrls: ['./create-proforma.component.css']
})
export class CreateProformaComponent implements OnInit {

  title: string;
  nitIngresado: string;
  noProforma: string;

  producto: Producto;
  cliente: Cliente;
  usuario: UsuarioAuxiliar;
  proforma: Proforma;

  constructor(
    private proformaService: ProformaService,
    private productoService: ProductoService,
    private clienteService: ClienteService,
    private clienteCreateService: ClienteCreateService,
    private usuarioService: UsuarioService,
    public authService: AuthService
  ) {
    this.title = 'Crear Proforma';
    this.producto = new Producto();
    this.cliente = new Cliente();
    this.usuario = new UsuarioAuxiliar();
    this.proforma = new Proforma();
    this.noProforma = this.proforma.generarNoProforma();
  }

  ngOnInit(): void {
    this.loadUsuario();
  }

  loadUsuario(): void {
    this.usuarioService.getUsuario(this.authService.usuario.idUsuario).subscribe(
      usuario => {
        this.usuario = usuario;
      }
    );
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

  agregarLinea(): void {
    if (!this.cliente) { // Comprueba que el cliente exista
      swal.fire('Ha ocurrido un Problema', 'Por favor, elija un cliente antes de llevar a cabo la venta.', 'error');
    } else {
      if (this.producto) { // comprueba que el producto exista
        const itemProforma = new DetalleProforma();

        itemProforma.cantidad = +((document.getElementById('cantidad') as HTMLInputElement)).value;
        itemProforma.descuento = 0;

        if (itemProforma.cantidad > this.producto.stock) {
          swal.fire('Stock Insuficiente', 'No existen las suficientes existencias de este producto.', 'warning');
          return;
        } else {
          if (itemProforma.cantidad && itemProforma.cantidad !== 0) {
            if (this.existeItem(this.producto.idProducto)) {
              this.incrementaCantidad(this.producto.idProducto, itemProforma.cantidad);
              this.producto = new Producto();
              (document.getElementById('cantidad') as HTMLInputElement).value = '';
            } else {
                itemProforma.producto = this.producto;
                itemProforma.subTotalDescuento = itemProforma.calcularImporte();
                itemProforma.subTotal = itemProforma.calcularImporte();

                this.proforma.itemsProforma.push(itemProforma);
                this.producto = new Producto();

                (document.getElementById('cantidad') as HTMLInputElement).value = '';
            }

          } else if (itemProforma.cantidad === 0) {
            swal.fire('Cantidad Erronéa', 'La cantidad a agregar debe ser mayor a 0.', 'warning');
          } else if (!itemProforma.cantidad) {
            swal.fire('Valor Inválido', 'La cantidad no puede estar vacía.  Ingrese un valor válido.', 'warning');
          }
        }
      }
    }
  }

  existeItem(id: number): boolean {
    let existe = false;
    this.proforma.itemsProforma.forEach((item: DetalleProforma) => {
      if (id === item.producto.idProducto) {
        existe = true;
      }
    });
    return existe;
  }

  eliminarItem(index: number): void {
    this.proforma.itemsProforma.splice(index, 1);
  }

  incrementaCantidad(idProducto: number, cantidad: number): void {
    this.proforma.itemsProforma = this.proforma.itemsProforma.map((item: DetalleProforma) => {
      if (idProducto === item.producto.idProducto) {
        item.cantidad = item.cantidad + cantidad;
        item.subTotal = item.calcularImporte();
        item.subTotalDescuento = item.calcularImporteDescuento();
      }

      return item;
    });
  }

  actualizarCantidad(idProducto: number, event: any): void {
    const cantidad = event.target.value as number;

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

    this.proforma.itemsProforma = this.proforma.itemsProforma.map((itemProforma: DetalleProforma) => {
      if (idProducto === itemProforma.producto.idProducto) {
        itemProforma.descuento = descuento;
        itemProforma.subTotal = itemProforma.calcularImporte();
        itemProforma.subTotalDescuento = itemProforma.calcularImporteDescuento();
        itemProforma.nPrecioVenta = itemProforma.calcularNuevoPrecioVenta();
      }
      
      return itemProforma;
    });
  }

  createProforma(): void {
    this.proforma.noProforma = this.noProforma;
    this.proforma.cliente = this.cliente;
    this.proforma.usuario = this.usuario;
    this.proforma.total = this.proforma.calcularTotal();

    this.proformaService.create(this.proforma).subscribe(response => {
      if (response.proforma) {
        this.generarProformaPdf(response.proforma.idProforma);
      }
    });

  }

  generarProformaPdf(id: number): void {
    this.proformaService.getProformaPdf(id).subscribe(response => {
      const url = window.URL.createObjectURL(response.data);
      const a = document.createElement('a');
      document.body.appendChild(a);
      a.setAttribute('style', 'display: none');
      a.setAttribute('target', 'blank');
      a.href = url;
      /*
        opcion para pedir descarga de la respuesta obtenida
        a.download = response.filename;
      */
      window.open(a.toString(), '_blank');
      window.URL.revokeObjectURL(url);
      a.remove();
      location.reload();
    },
      error => {
        console.log(error);
      });
  }
}
