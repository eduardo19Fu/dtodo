import { AfterViewInit, Component, OnInit } from '@angular/core';

import { Producto } from 'src/app/models/producto';

import { AuthService } from 'src/app/services/auth.service';
import { ProductoService } from 'src/app/services/producto.service';
import { ModalService } from 'src/app/services/productos/modal.service';
import { JqueryConfigs } from '../../utils/jquery/jquery-utils';
import { ProductoDto } from 'src/app/dtos/productoDto';

@Component({
  selector: 'app-productos',
  templateUrl: './productos.component.html',
  styles: [
  ]
})
export class ProductosComponent implements OnInit, AfterViewInit {

  title: string;
  // productos: Producto[];
  productos: ProductoDto[];

  public productoSeleccionado: Producto;

  paginador: any;

  jQueryConfigs: JqueryConfigs;

  constructor(
    public modalService: ModalService,
    private productoService: ProductoService,
    public auth: AuthService
  ) {
    this.title = 'Listado de Productos';
    this.jQueryConfigs = new JqueryConfigs();
   }

  ngOnInit(): void {
    //this.getProductos();
    this.getProductosDto();
    this.modalService.notificarUpload.subscribe(producto => {
      this.productos = this.productos.map(productoOriginal => {
        if (producto.idProducto === productoOriginal.idProducto){
          productoOriginal.imagen = producto.imagen;
        }
        return productoOriginal;
      });
    });
  }

  ngAfterViewInit(): void{
  }

  // getProductos(): void{
  //   this.productoService.getProductos().subscribe(
  //     productos => {
  //       this.productos = productos;
  //       this.jQueryConfigs.configDataTable('productos');
  //       this.jQueryConfigs.configToolTip();
  //     },
  //     error => { }
  //   );
  // }

  getProductosDto(): void {
    this.productoService.getProductosDto().subscribe(
      productos => {
        this.productos = productos;
        this.jQueryConfigs.configDataTable('productos');
        this.jQueryConfigs.configToolTip();
      }
    );
  }

  abrirModal(producto: Producto): void{
    this.productoSeleccionado = producto;
    this.modalService.abrirModal();
  }

}
