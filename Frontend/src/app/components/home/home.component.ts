import { Component, OnInit } from '@angular/core';

import { ClienteService } from 'src/app/services/cliente.service';
import { CompraService } from 'src/app/services/compra.service';
import { FacturaService } from 'src/app/services/facturas/factura.service';
import { MarcaProductoService } from 'src/app/services/marca-producto.service';
import { NotasCreditoService } from 'src/app/services/notas-credito.service';
import { ProductoService } from 'src/app/services/producto.service';
import { ProformaService } from 'src/app/services/proformas/proforma.service';
import { SucursalService } from 'src/app/services/sucursal.service';
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
  public totalCompras: number;
  public totalProformas: number;
  public totalNotasCredito: number;
  public totalSucursales: number;

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
    private serviceCompra: CompraService,
    private serviceProforma: ProformaService,
    private serviceNotasCredito: NotasCreditoService,
    private serviceSucursal: SucursalService,
    public auth: AuthService
  ) {
    this.title = 'Inicio';
  }

  ngOnInit(): void {
    this.getProductos();
    this.getClientes();

    if (this.auth.hasRole('ROLE_ADMIN')) {
      this.getUsuarios();
      this.getCompras();
      this.getSucursales();
    }

    if (this.auth.hasRole('ROLE_ADMIN') || this.auth.hasRole('ROLE_COBRADOR')) {
      this.getFacturas();
      this.getProformas();
      this.getNotasCredito();
    }
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
    const idUsuario = this.auth.esSoloCobrador() ? this.auth.usuario.idUsuario : null;
    this.serviceFactura.getTotalVentas(idUsuario).subscribe(
      total => this.totalFacturas = total
    );
  }

  getCompras(): void {
    this.serviceCompra.getTotalCompras().subscribe(
      total => this.totalCompras = total
    );
  }

  getProformas(): void {
    const idUsuario = this.auth.esSoloCobrador() ? this.auth.usuario.idUsuario : null;
    this.serviceProforma.getTotalProformas(idUsuario).subscribe(
      total => this.totalProformas = total
    );
  }

  getNotasCredito(): void {
    const idUsuario = this.auth.esSoloCobrador() ? this.auth.usuario.idUsuario : null;
    this.serviceNotasCredito.getTotalNotasCredito(idUsuario).subscribe(
      total => this.totalNotasCredito = total
    );
  }

  getSucursales(): void {
    this.serviceSucursal.getTotalSucursales().subscribe(
      total => this.totalSucursales = total
    );
  }
}
