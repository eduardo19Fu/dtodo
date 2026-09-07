import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../../../services/auth.service';
import { MovimientosProductoService } from '../../../services/movimientos/movimientos-producto.service';
import { ProductoService } from '../../../services/producto.service';
import { UsuarioService } from '../../../services/usuarios/usuario.service';

import { Producto } from 'src/app/models/producto';
import { MovimientoProducto } from '../../../models/movimiento-producto';
import { UsuarioAuxiliar } from 'src/app/models/auxiliar/usuario-auxiliar';

import Swal from 'sweetalert2';

@Component({
  selector: 'app-create-movimiento',
  templateUrl: './create-movimiento.component.html',
  styleUrls: [
    '../../productos/create-producto/create-producto.component.css',
    './create-movimiento.component.css'
  ]
})
export class CreateMovimientoComponent implements OnInit {

  @ViewChild('cantidadInput') cantidadInput: ElementRef<HTMLInputElement>;

  title: string;

  usuario: UsuarioAuxiliar;
  movimientoProducto: MovimientoProducto;
  producto: Producto;
  movimientos: string[] = ['ENTRADA', 'SALIDA'];
  modalProductoVisible: boolean = false;

  constructor(
    private movimientoProductoService: MovimientosProductoService,
    private productoService: ProductoService,
    private usuarioService: UsuarioService,
    private authService: AuthService,
    private router: Router
  ) {
    this.title = 'Ingresar Nuevo Movimiento';
    this.movimientoProducto = new MovimientoProducto();
    this.producto = new Producto();
  }

  ngOnInit(): void {
    this.usuarioService.getUsuario(this.authService.usuario.idUsuario).subscribe(
      usuario => {
        this.usuario = usuario;
        this.movimientoProducto.usuario = this.usuario;
      },
      error => {
        Swal.fire(`Error: ${error.status}`, '', 'error');
      }
    );
  }

  create(): void {
    this.movimientoProducto.producto = this.producto;
    this.movimientoProducto.usuario = this.usuario;
    if (this.movimientoProducto.producto) {
      // tslint:disable-next-line: max-line-length
      if (this.movimientoProducto.producto.stock >= this.movimientoProducto.cantidad || this.movimientoProducto.tipoMovimiento === 'ENTRADA') {

        this.movimientoProductoService.create(this.movimientoProducto).subscribe(
          response => {
            this.router.navigate(['/productos/inventario/index']);
            Swal.fire('Movimiento creado con éxito', `El movimiento ${response.idMovimiento} ha sido creada con éxito!`, 'success');
          },
          error => {
            Swal.fire('Error', error.error.message, 'error');
          }
        );

      } else {
        Swal.fire('Existencias Insuficientes', 'El stock disponible es insuficiente para surtir la salida', 'warning');
      }
    }
  }

  buscarProducto(): void {
    const codigo = ((document.getElementById('cod-producto') as HTMLInputElement)).value;

    if (codigo) {
      this.productoService.getProductoByCode(codigo).subscribe(
        producto => {
          this.producto = producto;
          if (this.cantidadInput) {
            this.cantidadInput.nativeElement.focus();
          }
        },
        error => {
          if (error.status === 400) {
            Swal.fire(`Error: ${error.status}`, 'Petición no se puede llevar a cabo.', 'error');
          }

          if (error.status === 404) {
            Swal.fire(`Error: ${error.status}`, error.error.mensaje, 'error');
          }
        }
      );
    } else {
      Swal.fire('Código Inválido', 'Ingrese un código de producto válido para realizar la búsqueda.', 'warning');
    }
  }

  abrirModalProducto(): void {
    this.modalProductoVisible = true;
  }

  cerrarModalProducto(): void {
    this.modalProductoVisible = false;
  }

  loadProducto(producto: Producto): void {
    this.producto.codProducto = producto.codProducto;
    this.cerrarModalProducto();
    this.buscarProducto();
  }
}
