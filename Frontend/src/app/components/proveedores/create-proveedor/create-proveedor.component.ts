import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { Pais } from 'src/app/models/pais';
import { Proveedor } from 'src/app/models/proveedor';
import { AuthService } from 'src/app/services/auth.service';
import { PaisService } from 'src/app/services/pais.service';
import { ProveedorService } from 'src/app/services/proveedor.service';
import { UsuarioService } from 'src/app/services/usuarios/usuario.service';

import swal from 'sweetalert2';

@Component({
  selector: 'app-create-proveedor',
  templateUrl: './create-proveedor.component.html',
  styleUrls: ['../../productos/create-producto/create-producto.component.css']
})
export class CreateProveedorComponent implements OnInit {

  title: string;
  proveedor: Proveedor;
  paises: Pais[] = [];
  guardando = false;

  constructor(
    private proveedorService: ProveedorService,
    private paisService: PaisService,
    private usuarioService: UsuarioService,
    private authService: AuthService,
    private router: Router,
    private activatedRoute: ActivatedRoute
  ) {
    this.title = 'Registrar nuevo proveedor';
    this.proveedor = new Proveedor();
  }

  ngOnInit(): void {
    this.cargarPaises();
    this.cargarProveedor();
  }

  get esNuevo(): boolean {
    return !this.proveedor.idProveedor;
  }

  private cargarPaises(): void {
    this.paisService.getPaises().subscribe(paises => this.paises = paises);
  }

  private cargarProveedor(): void {
    this.activatedRoute.params.subscribe(params => {
      const id = params.id;
      if (id) {
        this.title = 'Editar proveedor';
        this.proveedorService.getProveedor(id).subscribe(
          proveedor => this.proveedor = proveedor
        );
      }
    });
  }

  compararPais(o1: Pais, o2: Pais): boolean {
    if (o1 === undefined && o2 === undefined) {
      return true;
    }
    return o1 == null || o2 == null || o1 === undefined || o2 === undefined ? false : o1.idPais === o2.idPais;
  }

  create(): void {
    this.guardando = true;
    this.usuarioService.getUsuario(this.authService.usuario.idUsuario).subscribe(
      usuario => {
        this.proveedor.usuario = usuario;
        this.proveedorService.create(this.proveedor).subscribe(
          nuevoProveedor => {
            this.guardando = false;
            this.router.navigate(['/proveedores/index']);
            swal.fire('Proveedor Guardado', `El proveedor ${nuevoProveedor.nombre} fue registrado con &eacute;xito`, 'success');
          },
          () => this.guardando = false
        );
      },
      () => this.guardando = false
    );
  }

  update(): void {
    this.guardando = true;
    this.proveedorService.update(this.proveedor).subscribe(
      proveedorActualizado => {
        this.guardando = false;
        this.router.navigate(['/proveedores/index']);
        swal.fire('Proveedor Actualizado', `${proveedorActualizado.nombre} fu&eacute; actualizado con &eacute;xito!`, 'success');
      },
      () => this.guardando = false
    );
  }

}
