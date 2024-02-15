import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { UsuarioAuxiliar } from 'src/app/models/auxiliar/usuario-auxiliar';
import { Cliente } from 'src/app/models/cliente';
import { NotaCredito } from 'src/app/models/nota-credito';
import { NotaCreditoDetalle } from 'src/app/models/nota-credito-detalle';
import { Producto } from 'src/app/models/producto';
import { AuthService } from 'src/app/services/auth.service';
import { ClienteService } from 'src/app/services/cliente.service';
import { ClienteCreateService } from 'src/app/services/facturas/cliente-create.service';
import { NotasCreditoService } from 'src/app/services/notas-credito.service';
import { ProductoService } from 'src/app/services/producto.service';
import { UsuarioService } from 'src/app/services/usuarios/usuario.service';

import swal from 'sweetalert2';

@Component({
  selector: 'app-create-nota',
  templateUrl: './create-nota.component.html',
  styleUrls: ['./create-nota.component.css']
})
export class CreateNotaComponent implements OnInit {

  @ViewChild('mybuscar') myBuscarTexto: ElementRef;
  @ViewChild('myAbono') myAbonoRef: ElementRef;
  @ViewChild('myCodProductoChild') myCodProdRef: ElementRef;
  @ViewChild('myCantidadChild') myCantidadRef: ElementRef;
  @ViewChild('myButtonXChild') myButtonXRef: ElementRef;
  @ViewChild('myButton2XChild') myButton2XRef: ElementRef;
  @ViewChild('mySaldoChild') mySaldoRef: ElementRef;

  nitIngresado: string;
  title: string;
  abono: number;
  saldoRestante: number;
  fechaLimite: Date;

  notaCredito: NotaCredito;
  cliente: Cliente;
  producto: Producto;
  usuario: UsuarioAuxiliar;

  constructor(
    private notaService: NotasCreditoService,
    private productoService: ProductoService,
    private clienteService: ClienteService,
    private clienteCreateService: ClienteCreateService,
    private usuarioService: UsuarioService,
    private authService: AuthService,
    private router: Router
  ) {
    this.title = 'Creación de Notas de Crédito';
    this.producto = new Producto();
    this.cliente = new Cliente();
    this.usuario = new UsuarioAuxiliar();
    this.notaCredito = new NotaCredito();
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

  cargarCliente(event): void {
    this.cliente = event;
  }

  loadProducto(event): void {
    this.myCodProdRef.nativeElement.value = event.codProducto;
    (document.getElementById ('button-x')).click(); // No se utilizó el nativeElement ya que no reconocia el botón para cerrar el modal
    this.buscarProducto();
    this.myCantidadRef.nativeElement.focus();
  }

  loadCliente(event): void {
    this.myBuscarTexto.nativeElement.value = event.nit;
    (document.getElementById('button-2x')).click(); // No se utilizó el nativeElement ya que no reconocia el botón para cerrar el modal
    this.buscarCliente();
  }

  calcularSaldo(event): void {
    if (this.abono) {
      this.saldoRestante = this.notaCredito.calcularTotal() - this.abono;
    } else {
      this.saldoRestante = 0.00
    }
  }

  buscarProducto(): void {
    const codigo = this.myCodProdRef.nativeElement.value;

    if (codigo) {
      this.productoService.getProductoByCode(codigo).subscribe(
        producto => {
          this.producto = producto;
          this.myCantidadRef.nativeElement.focus();
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
      swal.fire('Ha ocurrido un Problema', 'Por favor, elija un cliente antes de llevar a cabo el envío.', 'error');
    } else {
      if (this.producto) { // comprueba que el producto exista
        const item = new NotaCreditoDetalle();
        item.cantidad = +(this.myCantidadRef.nativeElement).value;
        item.descuento = 0;

        if (item.cantidad > this.producto.stock) {
          swal.fire('Stock Insuficiente', 'No existen las suficientes existencias de este producto.', 'warning');
          return;
        } else {
          if (item.cantidad && item.cantidad !== 0) {
            if (this.existeItem(this.producto.idProducto)) {
              this.incrementaCantidad(this.producto.idProducto, item.cantidad);
              this.producto = new Producto();
              this.myCantidadRef.nativeElement.value = '';
            } else {
              item.producto = this.producto;
              item.subTotalDescuento = item.calcularImporte();
              item.subTotal = item.calcularImporte();
              this.notaCredito.items.push(item);
              this.producto = new Producto();

              this.myCantidadRef.nativeElement.value = '';
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
    this.notaCredito.items.forEach((item: NotaCreditoDetalle) => {
      if (id === item.producto.idProducto) {
        existe = true;
      }
    });
    return existe;
  }

  incrementaCantidad(idProducto: number, cantidad: number): void {
    this.notaCredito.items = this.notaCredito.items.map((item: NotaCreditoDetalle) => {
      if (idProducto === item.producto.idProducto) {
        item.cantidad = item.cantidad + cantidad;
        item.subTotal = item.calcularImporte();
        item.subTotalDescuento = item.calcularImporteDescuento();
      }

      return item;
    });
  }

  buscarCliente(): void {
    const nit = this.myBuscarTexto.nativeElement.value;
    if (nit) {
      this.clienteService.getClienteByNit(nit).subscribe(
        cliente => {
          this.cliente = cliente;
          this.myCodProdRef.nativeElement.focus();
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

  eliminarItem(index: number): void {
    this.notaCredito.items.splice(index, 1);
  }

  create(): void {
    this.notaCredito.cliente = this.cliente;
    this.notaCredito.usuario = this.usuario;
    this.notaCredito.abono = this.abono;
    this.notaCredito.restante = this.saldoRestante;
    this.notaCredito.total = this.notaCredito.calcularTotal();

    this.notaService.create(this.notaCredito).subscribe(
      response => {
        this.cliente = new Cliente();
        this.notaCredito = new NotaCredito();
        this.myBuscarTexto.nativeElement.value = '';
        this.router.navigate(['/notas-credito/index']);
        swal.fire('Pedido Realizado', `Nota No. ${response.notaCredito.idNotaCredito} creada satisfactoriamente.`, 'success');
        this.myBuscarTexto.nativeElement.focus();

      // AQÍ VA EL CÓDIGO PARA GENERAR EL PDF
      this.print(response.notaCredito);
      }
    );
  }

  print(nota: NotaCredito): void {
    console.log("imprimiendo");
  }

}
