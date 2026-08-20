import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { MarcaProductoService } from 'src/app/services/marca-producto.service';
import { ProductoService } from 'src/app/services/producto.service';
import { TipoProductoService } from 'src/app/services/tipo-producto.service';

import { MarcaProducto } from 'src/app/models/marca-producto';
import { Producto } from 'src/app/models/producto';
import { TipoProducto } from 'src/app/models/tipo-producto';

import swal from 'sweetalert2';

@Component({
  selector: 'app-create-producto',
  templateUrl: './create-producto.component.html',
  styleUrls: ['./create-producto.component.css']
})
export class CreateProductoComponent implements OnInit {

  title: string;
  producto: Producto;

  tipos: TipoProducto[];
  marcas: MarcaProducto[];

  constructor(
    private serviceMarca: MarcaProductoService,
    private serviceTipo: TipoProductoService,
    private serviceProducto: ProductoService,
    private router: Router,
    private activatedRoute: ActivatedRoute
  ) {
    this.title = 'Registro de Productos';
    this.producto = new Producto();
  }

  ngOnInit(): void {
    // tslint:disable-next-line: deprecation
    this.activatedRoute.params.subscribe(params => {
      // tslint:disable-next-line: no-string-literal
      const id = params['id'];

      if (id) {
        // tslint:disable-next-line: deprecation
        this.serviceProducto.getProducto(id).subscribe(
          producto => this.producto = producto
        );
      }
    });
    this.cargarMarcas();
    this.cargarTipos();
  }

  cargarProducto(): void {
    // tslint:disable-next-line: deprecation
    this.activatedRoute.params.subscribe(params => {
      // tslint:disable-next-line: no-string-literal
      const id = params['id'];

      if (id) {
        // tslint:disable-next-line: deprecation
        this.serviceProducto.getProducto(id).subscribe(
          producto => this.producto = producto
        );
      }
    });
  }

  cargarMarcas(): void {
    // tslint:disable-next-line: deprecation
    this.serviceMarca.getMarcas().subscribe(marcas => this.marcas = marcas);
  }

  cargarTipos(): void {
    // tslint:disable-next-line: deprecation
    this.serviceTipo.getTiposProducto().subscribe(tipos => this.tipos = tipos);
  }

  create(): void {
    // this.producto.porcentajeGanancia = Number.parseFloat((document.getElementById('porcentaje-ganancia') as HTMLInputElement).value);
    this.producto.precioVenta = Number.parseFloat((document.getElementById('precio-venta') as HTMLInputElement).value);
    if (this.producto.codProducto) {
      this.serviceProducto.create(this.producto).subscribe(
        producto => {
          this.router.navigate(['/productos/index']);
          swal.fire('Producto Guardado', `El producto ${producto.nombre} ha sido registrado con éxito`, 'success');
        }
      );
    } else {
      this.producto.codProducto = this.producto.generarCodigo();
      this.serviceProducto.create(this.producto).subscribe(
        producto => {
          this.router.navigate(['/productos/index']);
          swal.fire('Producto Guardado', `El producto ${producto.nombre} ha sido registrado con éxito`, 'success');
        }
      );
    }
  }

  update(): void {
    // this.producto.porcentajeGanancia = Number.parseFloat((document.getElementById('porcentaje-ganancia') as HTMLInputElement).value);
    this.producto.precioVenta = Number.parseFloat((document.getElementById('precio-venta') as HTMLInputElement).value);
    this.serviceProducto.update(this.producto).subscribe(
      producto => {
        this.router.navigate(['/productos/index']);
        swal.fire('Producto Actualizado', `El producto ${producto.nombre} ha sido actualizado con éxito`, 'success');
      }
    );
  }

  // Comparar para reemplazar el valor en el select del formulario en caso de existir
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

  calcularPorcentajeGanancia(): void {
    const pcompra = ((document.getElementById('precio-compra') as HTMLInputElement).value);
    const pventa = (document.getElementById('precio-venta') as HTMLInputElement).value;
    let porcentaje = 0;

    if (!pcompra || !pventa) {
      console.log('valores incorrectos');
    } else {
      porcentaje = ((Number.parseFloat(pventa) - Number.parseFloat(pcompra)) / Number.parseFloat(pcompra)) * 100;
    }

    (document.getElementById('porcentaje-ganancia') as HTMLInputElement).value = porcentaje.toString();
  }

  calcularPrecioVenta(): void {
    const pcompra = ((document.getElementById('precio-compra') as HTMLInputElement).value);
    const pporcentaje = ((document.getElementById('porcentaje-ganancia') as HTMLInputElement).value);

    let precioVenta = 0;

    if (!pcompra || !pporcentaje) {
      console.log('valores incorrectos');
    } else {
      precioVenta = ((Number.parseFloat(pcompra) + ((Number.parseFloat(pporcentaje) / 100) * Number.parseFloat(pcompra))));
      console.log(precioVenta);
    }

    (document.getElementById('precio-venta') as HTMLInputElement).value = precioVenta.toString();
  }

}
