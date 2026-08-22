import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { Producto } from '../../../models/producto';
import { Cliente } from '../../../models/cliente';
import { Proforma } from '../../../models/proforma';
import { UsuarioAuxiliar } from '../../../models/auxiliar/usuario-auxiliar';
import { DetalleProforma } from '../../../models/detalle-proforma';

import { AuthService } from '../../../services/auth.service';
import { ProductoService } from '../../../services/producto.service';
import { ClienteService } from '../../../services/cliente.service';
import { ClienteCreateService } from '../../../services/facturas/cliente-create.service';
import { UsuarioService } from '../../../services/usuarios/usuario.service';
import { ProformaService } from '../../../services/proformas/proforma.service';

import swal from 'sweetalert2';

@Component({
  selector: 'app-create-proforma',
  templateUrl: './create-proforma.component.html',
  styleUrls: [
    '../../productos/create-producto/create-producto.component.css',
    './create-proforma.component.css'
  ]
})
export class CreateProformaComponent implements OnInit {

  title: string;
  nitIngresado: string;
  noProforma: string;
  cantidadProducto: number = null;
  descuentoProducto = 0;
  isSaving = false;

  producto: Producto;
  cliente: Cliente;
  usuario: UsuarioAuxiliar;
  proforma: Proforma;
  proformaCargada: Proforma;

  constructor(
    private proformaService: ProformaService,
    private productoService: ProductoService,
    private clienteService: ClienteService,
    private clienteCreateService: ClienteCreateService,
    private usuarioService: UsuarioService,
    public authService: AuthService,
    private activatedRoute: ActivatedRoute,
    private router: Router
  ) {
    this.title = 'Crear Proforma';
    this.producto = new Producto();
    this.cliente = new Cliente();
    this.usuario = new UsuarioAuxiliar();
    this.proforma = new Proforma();
    this.proformaCargada = new Proforma();
  }

  ngOnInit(): void {
    this.loadUsuario();
    this.cargarProforma();
  }

  loadUsuario(): void {
    this.usuarioService.getUsuario(this.authService.usuario.idUsuario).subscribe(
      usuario => {
        this.usuario = usuario;
      }, error => {
        swal.fire('Error al Cargar Usuario', `${error.error.message}`, 'error');
      }
    );
  }

  loadProducto(event): void {
    (document.getElementById('codigo') as HTMLInputElement).value = event.codProducto;
    (document.getElementById('button-x')).click();
    this.buscarProducto();
  }

  loadCliente(event): void {
    (document.getElementById('buscar') as HTMLInputElement).value = event.nit;
    (document.getElementById('button-2x')).click();
    this.buscarCliente();
  }

  buscarProducto(): void {
    const codigo = (document.getElementById('codigo') as HTMLInputElement).value;
    if (!codigo) {
      swal.fire('Código Inválido', 'Ingrese un código de producto válido para realizar la búsqueda.', 'warning');
      return;
    }

    this.productoService.getProductoByCode(codigo).subscribe(
      producto => {
        this.producto = producto;
        this.enfocarCantidad();
      },
      error => {
        const status = error.status;
        const message = status === 400
          ? 'Petición no se puede llevar a cabo.'
        : error.error?.message || 'Error desconocido';
        swal.fire(`Error: ${error.error?.status || status}`, message, 'error');
      }
    );
  }

  private enfocarCantidad(): void {
    setTimeout(() => (document.getElementById('cantidad') as HTMLInputElement)?.focus(), 350);
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
            swal.fire(`Error: ${error.error.status}`, 'Petición Equivocada', 'error');
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

        itemProforma.cantidad = Number(this.cantidadProducto);
        itemProforma.descuento = Number(this.descuentoProducto || 0);

        if (!Number.isInteger(itemProforma.cantidad) || itemProforma.cantidad <= 0) {
          swal.fire('Cantidad Inválida', 'La cantidad debe ser un número entero mayor a 0.', 'warning');
          return;
        }

        if (!Number.isFinite(itemProforma.descuento)
          || itemProforma.descuento < 0 || itemProforma.descuento > 100) {
          swal.fire('Descuento Inválido', 'El descuento debe estar entre 0% y 100%.', 'warning');
          return;
        }

        if (itemProforma.cantidad > this.producto.stock) {
          swal.fire('Stock Insuficiente', 'No existen las suficientes existencias de este producto.', 'warning');
          return;
        } else {
          if (itemProforma.cantidad && itemProforma.cantidad !== 0) {
            if (this.existeItem(this.producto.idProducto)) {
              this.incrementaCantidad(this.producto.idProducto, itemProforma.cantidad, itemProforma.descuento);
              this.producto = new Producto();
              this.cantidadProducto = null;
              this.descuentoProducto = 0;
            } else {
                itemProforma.producto = this.producto;
                itemProforma.subTotalDescuento = itemProforma.calcularImporteDescuento();
                itemProforma.subTotal = itemProforma.calcularImporte();
                itemProforma.nuevoPrecioVenta = itemProforma.calcularNuevoPrecioVenta();

                this.proforma.itemsProforma = [...this.proforma.itemsProforma, itemProforma];
                this.recalcularTotal();
                this.producto = new Producto();
                this.cantidadProducto = null;
                this.descuentoProducto = 0;
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
    this.proforma.itemsProforma = this.proforma.itemsProforma.filter((item, itemIndex) => itemIndex !== index);
    this.recalcularTotal();
  }

  incrementaCantidad(idProducto: number, cantidad: number, descuento: number): void {
    this.proforma.itemsProforma = this.proforma.itemsProforma.map((item: DetalleProforma) => {
      if (idProducto === item.producto.idProducto) {
        item.cantidad = item.cantidad + cantidad;
        item.descuento = descuento;
        item.subTotal = item.calcularImporte();
        item.subTotalDescuento = item.calcularImporteDescuento();
        item.nuevoPrecioVenta = item.calcularNuevoPrecioVenta();
      }

      return item;
    });
    this.recalcularTotal();
  }

  cantidadesValidas(): boolean {
    return this.proforma.itemsProforma.every((item: DetalleProforma) =>
      Number.isInteger(Number(item.cantidad)) && Number(item.cantidad) > 0
    );
  }

  recalcularTotal(): void {
    this.proforma.total = this.proforma.calcularTotal();
  }

  createProforma(): void {
    if (!this.cantidadesValidas()) {
      swal.fire('Cantidad Inválida', 'Todos los productos deben tener una cantidad entera mayor a 0.', 'warning');
      return;
    }

    this.isSaving = true;

    if (this.isSaving) {
      this.proforma.noProforma = this.noProforma;
      this.proforma.cliente = this.cliente;
      this.proforma.usuario = this.usuario;
      this.proforma.total = this.proforma.calcularTotal();

      this.proformaService.create(this.proforma).subscribe(response => {
        if (response) {
          this.generarProformaPdf(response.idProforma);
        }
      }, error => {
        this.isSaving = false;
        swal.fire(`Error: ${error.error.status}`, `${error.error.message}`, 'error');
      });
    }
  }

  cargarProforma(): void {
    this.activatedRoute.params.subscribe(params => {
      const id = params.proformaId;

      if (id) {
        this.buscarProformaPorId(id);
      }
    });
  }

  buscarProformaPorId(id: number): void {
    this.proformaService.getProforma(id).subscribe(
      response => {
        if (!response.mensaje) {
          this.proformaCargada = response;

          this.cliente = response.cliente;

          this.proforma.idProforma = response.idProforma;
          this.proforma.noProforma = response.noProforma;
          this.proforma.cliente = this.cliente;
          this.proforma.fechaEmision = response.fechaEmision;
          this.proforma.estado = response.estado;
          this.proforma.usuario = response.usuario;
          this.proforma.total = response.total;


          this.proforma.itemsProforma = response.itemsProforma.map((itemProforma: DetalleProforma) => {
            const item = new DetalleProforma();

            item.cantidad = itemProforma.cantidad;
            item.subTotal = itemProforma.subTotal;
            item.subTotalDescuento = itemProforma.subTotalDescuento;
            item.producto = itemProforma.producto;
            item.descuento = itemProforma.descuento;
            item.nuevoPrecioVenta = itemProforma.nuevoPrecioVenta;

            return item;
          });
          this.recalcularTotal();
        }
      }, error => {
        swal.fire(`Error: ${error.error.status}`, `${error.error.message}`, 'error');
      }
    );
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
        swal.fire(`Error: ${error.error.status}`, `${error.error.message}`, error);
      });
  }

  update(): void {
    if (!this.cantidadesValidas()) {
      swal.fire('Cantidad Inválida', 'Todos los productos deben tener una cantidad entera mayor a 0.', 'warning');
      return;
    }

    this.isSaving = true;
    if (this.isSaving) {
      this.proformaService.update(this.proforma).subscribe(response => {
        this.generarProformaPdf(response.idProforma);
        this.router.navigate(['/proformas/index']);
        swal.fire(response.mensaje, `Proforma ${response.noProforma} ha sido actualizada`, 'info');
      }, error => {
        this.isSaving = false;
        console.log(error);
        swal.fire('Error', `${error.error.message}`, 'error');
      });
    }
  }
}
