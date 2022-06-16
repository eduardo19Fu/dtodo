import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Usuario } from '../models/usuario';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  // tslint:disable-next-line: variable-name
  private _usuario: Usuario;
  // tslint:disable-next-line: variable-name
  private _token: string;

  url: string;

  constructor(
    private http: HttpClient
  ) {
    // this.url = 'http://localhost:8282';
    this.url = 'https://dtodojalapa.xyz:8282';
  }

  public get usuario(): Usuario {
    if (this._usuario != null) {
      return this._usuario;
    } else if (this._usuario == null && sessionStorage.getItem('usuario')) {
      this._usuario = JSON.parse(sessionStorage.getItem('usuario')) as Usuario;
      return this._usuario;
    }
    return new Usuario();
  }

  public get token(): string {
    if (this._token != null) {
      return this._token;
    } else if (this._token == null && sessionStorage.getItem('token')) {
      this._token = sessionStorage.getItem('token');
      return this._token;
    }
    return null;
  }

  login(usuario: Usuario): Observable<any> {
    const urlEndpoint = this.url + '/oauth/token';
    // const credenciales = btoa('angularapp' + ':' + '12345');
    const credenciales = btoa('angularapp' + ':' + 'pangosoftpuntodeventastore2021');
    // tslint:disable-next-line: object-literal-key-quotes
    const httpHeaders = new HttpHeaders({ 'Content-Type': 'application/x-www-form-urlencoded', 'Authorization': 'Basic ' + credenciales });

    const params = new URLSearchParams();
    params.set('grant_type', 'password');
    params.set('username', usuario.usuario);
    params.set('password', usuario.password);

    return this.http.post<any>(urlEndpoint, params.toString(), { headers: httpHeaders });
  }

  guardarToken(accessToken: string): void {
    this._token = accessToken;
    sessionStorage.setItem('token', this._token);
  }

  guardarUsuario(accessToken: string): void {

    const payload = this.obtenerDatos(accessToken);
    this._usuario = new Usuario();
    this._usuario.idUsuario = payload.id_usuario;
    this._usuario.primerNombre = payload.primerNombre;
    this._usuario.segundoNombre = payload.segundoNombre;
    this._usuario.apellido = payload.apellido;
    this._usuario.usuario = payload.user_name;
    this._usuario.roles = payload.authorities;

    sessionStorage.setItem('usuario', JSON.stringify(this._usuario));
  }

  obtenerDatos(accessToken: string): any {
    if (accessToken != null) {
      return JSON.parse(atob(accessToken.split('.')[1]));
    }

    return null;
  }

  isAuthenticated(): boolean {
    const payload = this.obtenerDatos(this.token);
    if (payload != null && payload.user_name && payload.user_name.length > 0) {
      return true;
    }
    return false;
  }

  hasRole(role: string): boolean {
    if (this.usuario.roles.includes(role)) {
      return true;
    }
    return false;
  }

  logout(): void {
    this._token = null;
    this._usuario = null;
    sessionStorage.clear();
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('usuario');
  }
}
