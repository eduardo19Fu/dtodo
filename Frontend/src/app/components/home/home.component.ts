import { Component, OnInit } from '@angular/core';

import { ClienteService } from 'src/app/services/cliente.service';
import { FacturaService } from 'src/app/services/facturas/factura.service';
import { MarcaProductoService } from 'src/app/services/marca-producto.service';
import { ProductoService } from 'src/app/services/producto.service';
import { UsuarioService } from 'src/app/services/usuarios/usuario.service';
import { AuthService } from '../../services/auth.service';

import { Cliente } from 'src/app/models/cliente';
import { MarcaProducto } from 'src/app/models/marca-producto';
import { Producto } from 'src/app/models/producto';
import { Usuario } from 'src/app/models/usuario';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {

  public title: string;
  public totalProductos: number;
  public totalClientes: number;
  public totalMarcas: number;
  public totalUsuarios: number;
  public totalFacturas: number;

  productos: Producto[];
  clientes: Cliente[];
  marcas: MarcaProducto[];
  usuarios: Usuario[];

  constructor(
    private serviceProducto: ProductoService,
    private serviceCliente: ClienteService,
    private serviceMarca: MarcaProductoService,
    private serviceUsuario: UsuarioService,
    private serviceFactura: FacturaService,
    public auth: AuthService
  ) {
    this.title = 'Inicio';
  }

  ngOnInit(): void {
    this.getProductos();
    this.getClientes();
    this.getUsuarios();
    this.getFacturas();
  }

  getProductos(): void {
    this.serviceProducto.getTotalProductos().subscribe(
      total => this.totalProductos = total
    );
  }

  getClientes(): void {
    this.serviceCliente.getTotalClientes().subscribe(
      total => this.totalClientes = total
    );
  }

  getUsuarios(): void {
    this.serviceUsuario.getTotalUsuarios().subscribe(
      total => this.totalUsuarios = total
    );
  }

  getFacturas(): void {
    this.serviceFactura.getTotalVentas().subscribe(
      total => this.totalFacturas = total
    );
  }
}
