import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { MarcaProductoService } from 'src/app/services/marca-producto.service';
import { ProductoService } from 'src/app/services/producto.service';
import { TipoProductoService } from 'src/app/services/tipo-producto.service';
import { SucursalService } from 'src/app/services/sucursal.service';
import { AuthService } from 'src/app/services/auth.service';

import { MarcaProducto } from 'src/app/models/marca-producto';
import { Producto } from 'src/app/models/producto';
import { TipoProducto } from 'src/app/models/tipo-producto';
import { Sucursal } from 'src/app/models/sucursal';
import { SucursalStock } from 'src/app/models/sucursal-stock';

import swal from 'sweetalert2';

/** Fila del selector de sucursales al registrar un producto nuevo. */
interface SeleccionSucursal {
  sucursal: Sucursal;
  seleccionada: boolean;
  stock: number;
}

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

  // Selección de sucursales donde sembrar el producto nuevo (solo aplica al registrar, no al editar)
  seleccionSucursales: SeleccionSucursal[] = [];

  constructor(
    private serviceMarca: MarcaProductoService,
    private serviceTipo: TipoProductoService,
    private serviceProducto: ProductoService,
    private serviceSucursal: SucursalService,
    private auth: AuthService,
    private router: Router,
    private activatedRoute: ActivatedRoute
  ) {
    this.title = 'Registro de Productos';
    this.producto = new Producto();
  }

  ngOnInit(): void {
    // eslint-disable-next-line import/no-deprecated
    this.activatedRoute.params.subscribe(params => {
      // eslint-disable-next-line @typescript-eslint/dot-notation
      const id = params['id'];

      if (id) {
        // eslint-disable-next-line import/no-deprecated
        this.serviceProducto.getProducto(id).subscribe(
          producto => this.producto = producto
        );
      } else {
        this.cargarSucursales();
      }
    });
    this.cargarMarcas();
    this.cargarTipos();
  }

  private cargarSucursales(): void {
    this.serviceSucursal.getSucursales().subscribe(sucursales => {
      const idSucursalActiva = this.auth.usuario?.sucursal?.idSucursal;
      this.seleccionSucursales = sucursales.map(sucursal => ({
        sucursal,
        seleccionada: sucursal.idSucursal === idSucursalActiva,
        stock: null
      }));

      // Si el usuario no tiene sucursal asignada (ej. sin sesión con sucursal), preselecciona la principal.
      if (!this.seleccionSucursales.some(s => s.seleccionada)) {
        const principal = this.seleccionSucursales.find(s => s.sucursal.esPrincipal);
        if (principal) {
          principal.seleccionada = true;
        }
      }
    });
  }

  cargarProducto(): void {
    // eslint-disable-next-line import/no-deprecated
    this.activatedRoute.params.subscribe(params => {
      // eslint-disable-next-line @typescript-eslint/dot-notation
      const id = params['id'];

      if (id) {
        // eslint-disable-next-line import/no-deprecated
        this.serviceProducto.getProducto(id).subscribe(
          producto => this.producto = producto
        );
      }
    });
  }

  cargarMarcas(): void {
    // eslint-disable-next-line import/no-deprecated
    this.serviceMarca.getMarcas().subscribe(marcas => this.marcas = marcas);
  }

  cargarTipos(): void {
    // eslint-disable-next-line import/no-deprecated
    this.serviceTipo.getTiposProducto().subscribe(tipos => this.tipos = tipos);
  }

  create(): void {
    // this.producto.porcentajeGanancia = Number.parseFloat((document.getElementById('porcentaje-ganancia') as HTMLInputElement).value);
    this.producto.precioVenta = Number.parseFloat((document.getElementById('precio-venta') as HTMLInputElement).value);

    const sucursales = this.sucursalesSeleccionadasParaEnvio();
    if (sucursales.length === 0) {
      swal.fire('Selecciona al menos una sucursal', 'Elige en qué sucursal(es) va a estar disponible el producto.', 'warning');
      return;
    }

    if (!this.producto.codProducto) {
      this.producto.codProducto = this.producto.generarCodigo();
    }

    this.serviceProducto.create(this.producto, sucursales).subscribe(
      producto => {
        this.router.navigate(['/productos/index']);
        swal.fire('Producto Guardado', `El producto ${producto.nombre} ha sido registrado con éxito`, 'success');
      }
    );
  }

  private sucursalesSeleccionadasParaEnvio(): SucursalStock[] {
    return this.seleccionSucursales
      .filter(s => s.seleccionada)
      .map(s => ({ idSucursal: s.sucursal.idSucursal, stock: s.stock || 0 }));
  }

  get algunaSucursalSeleccionada(): boolean {
    return this.seleccionSucursales.some(s => s.seleccionada);
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
