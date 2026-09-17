import { Component, EventEmitter, HostListener, OnInit, Output } from '@angular/core';

import { MarcaProductoService } from 'src/app/services/marca-producto.service';
import { TipoProductoService } from 'src/app/services/tipo-producto.service';

import { MarcaProducto } from 'src/app/models/marca-producto';
import { Producto } from 'src/app/models/producto';
import { TipoProducto } from 'src/app/models/tipo-producto';

import swal from 'sweetalert2';

@Component({
  selector: 'app-modal-crear-producto',
  templateUrl: './modal-crear-producto.component.html',
  styleUrls: [
    '../../../movimientos-producto/create-movimiento/modal-buscar-producto-movimiento/modal-buscar-producto-movimiento.component.css',
    './modal-crear-producto.component.css'
  ]
})
export class ModalCrearProductoComponent implements OnInit {

  @Output() producto = new EventEmitter<Producto>();
  @Output() cerrar = new EventEmitter<void>();

  title: string;
  nuevoProducto: Producto;

  tipos: TipoProducto[];
  marcas: MarcaProducto[];

  constructor(
    private serviceMarca: MarcaProductoService,
    private serviceTipo: TipoProductoService
  ) {
    this.title = 'Registrar producto nuevo';
    this.nuevoProducto = new Producto();
  }

  ngOnInit(): void {
    this.serviceMarca.getMarcas().subscribe(marcas => this.marcas = marcas);
    this.serviceTipo.getTiposProducto().subscribe(tipos => this.tipos = tipos);
  }

  compararMarca(o1: MarcaProducto, o2: MarcaProducto): boolean {
    if (o1 === undefined && o2 === undefined) {
      return true;
    }
    return o1 === null || o2 === null || o1 === undefined || o2 === undefined ? false : o1.idMarcaProducto === o2.idMarcaProducto;
  }

  compararTipo(o1: TipoProducto, o2: TipoProducto): boolean {
    if (o1 === undefined && o2 === undefined) {
      return true;
    }
    return o1 == null || o2 == null || o1 === undefined || o2 === undefined ? false : o1.idTipoProducto === o2.idTipoProducto;
  }

  calcularPrecioVenta(): void {
    const compra = this.nuevoProducto.precioCompra;
    const porcentaje = this.nuevoProducto.porcentajeGanancia;

    if (!compra || !porcentaje) {
      return;
    }
    this.nuevoProducto.precioVenta = compra + ((porcentaje / 100) * compra);
  }

  confirmar(): void {
    if (!this.nuevoProducto.nombre || !this.nuevoProducto.precioCompra || !this.nuevoProducto.marcaProducto
        || !this.nuevoProducto.tipoProducto) {
      swal.fire('Datos incompletos', 'Completa nombre, precio de compra, marca y tipo de producto.', 'warning');
      return;
    }

    if (!this.nuevoProducto.codProducto) {
      this.nuevoProducto.codProducto = this.nuevoProducto.generarCodigo();
    }

    this.producto.emit(this.nuevoProducto);
  }

  cerrarModal(): void {
    this.cerrar.emit();
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.cerrarModal();
  }

}
