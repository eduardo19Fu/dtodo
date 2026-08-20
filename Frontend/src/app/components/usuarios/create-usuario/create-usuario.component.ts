import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { Role } from 'src/app/models/role';
import { Usuario } from 'src/app/models/usuario';
import { UsuarioAuxiliar } from 'src/app/models/auxiliar/usuario-auxiliar';

import { UsuarioService } from 'src/app/services/usuarios/usuario.service';

import swal from 'sweetalert2';

@Component({
  selector: 'app-create-usuario',
  templateUrl: './create-usuario.component.html',
  styleUrls: ['./create-usuario.component.css']
})
export class CreateUsuarioComponent implements OnInit {

  title: string;
  usuario: Usuario;
  role: Role;
  roles: Role[];
  filas: Role[] = [];
  roleSeleccionado: number = null;

  public usuarioAuxiliar: UsuarioAuxiliar;

  constructor(
    private router: Router,
    private usuarioService: UsuarioService,
    private activatedRoute: ActivatedRoute
  ) {
    this.title = 'Crear Usuario';
    this.usuario = new Usuario();
    this.usuarioAuxiliar = new UsuarioAuxiliar();
  }

  ngOnInit(): void {
    this.cargarUsuario();
    this.cargarRoles();
  }

  cargarUsuario(): void {
    this.activatedRoute.params.subscribe(params => {
      // tslint:disable-next-line: no-string-literal
      const id = params['id'];
      if (id) {
        this.usuarioService.getUsuario(id).subscribe(usuario => {
          this.usuarioAuxiliar = usuario;
          this.filas = this.usuarioAuxiliar.roles || [];
        });
      }
    });
  }

  create(): void {
    this.usuarioAuxiliar.roles = this.filas;

    this.usuarioService.create(this.usuarioAuxiliar).subscribe(
      response => {
        this.router.navigate(['/usuarios/index']);
        swal.fire('Usuario creado', `El usuario ${this.usuarioAuxiliar.usuario} fue creado con éxito`, 'success');
      }
    );
  }

  update(): void {
    this.usuarioAuxiliar.roles = this.filas;
    this.usuarioService.update(this.usuarioAuxiliar).subscribe(
      response => {
        this.router.navigate(['/usuarios/index']);
        swal.fire('Usuario Actualizado', `El usuario ${this.usuarioAuxiliar.usuario} fue actualizado con éxito`, 'success');
      }
    );
  }

  cargarRoles(): void {
    this.usuarioService.getRoles().subscribe(roles => this.roles = roles);
  }

  agregarRole(): void {
    if (!this.roleSeleccionado) {
      return;
    }

    const role = this.roles.find(item => item.idRole === +this.roleSeleccionado);
    const yaAsignado = this.filas.some(item => item.idRole === +this.roleSeleccionado);

    if (role && !yaAsignado) {
      this.filas.push(role);
    }

    this.roleSeleccionado = null;
  }

  eliminarFila(index: number): void{
    this.filas.splice(index, 1);
  }

}
