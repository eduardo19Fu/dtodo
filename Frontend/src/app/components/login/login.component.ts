import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import { Usuario } from 'src/app/models/usuario';

import { AuthService } from 'src/app/services/auth.service';

import swal from 'sweetalert2';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {

  title: string;
  usuario: Usuario;
  usuarioTitle: string;
  passwordTitle: string;
  headerTitle: string;
  passwordVisible: boolean = false;

  constructor(
    private router: Router,
    private authService: AuthService
  ) {
    this.title = 'Iniciar Sesión';
    this.headerTitle = 'Sistema de Gestión de Inventario y Ventas'
    this.usuarioTitle = 'Usuario';
    this.passwordTitle = 'Password';
    this.usuario = new Usuario();
  }

  ngOnInit(): void {
    this.authService.logout();
  }

  togglePasswordVisible(): void {
    this.passwordVisible = !this.passwordVisible;
  }

  login(): void {

    if (this.usuario.usuario == null || this.usuario.password == null) {
      swal.fire('Error en login', 'Usuario y/o contraseña estan vacíos', 'error');
      return;
    }

    this.authService.login(this.usuario).subscribe(
      response => {
        this.authService.guardarSesion(response.access_token, response.refresh_token);

        window.location.href = '/home';
      },
      error => {
        if (error.status === 400) {
          swal.fire('Error de Autenticación', 'Usuario y/o contraseña incorrectos', 'error');
        }
      }
    );
  }

}
