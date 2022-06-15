import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { global } from './global';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { Cliente } from '../models/cliente';
import swal from 'sweetalert2';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { AppUnauthorizated } from '../app.unauthorizated';

@Injectable({
  providedIn: 'root'
})
export class ClienteService {

  private url: string;

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    this.url = global.url;
  }

  getClientes(): Observable<Cliente[]> {
    return this.http.get<Cliente[]>(`${this.url}/clientes`);
  }

  getClientesPaginados(page: number): Observable<Cliente[]> {
    return this.http.get(`${this.url}/clientes/page/${page}`).pipe(
      map((response: any) => {
        (response.content as Cliente[]).map(cliente => {
          cliente.nombre = cliente.nombre.toUpperCase();
          return cliente;
        });
        return response;
      })
    );
  }

  getCliente(id: number): Observable<Cliente> {
    // método pipe para poder tratar excepciones y errores
    return this.http.get<Cliente>(`${this.url}/clientes/${id}`).pipe(
      catchError(e => {
        this.router.navigate(['/clientes']);
        swal.fire('Error al editar', e.error.mensaje, 'error');
        return throwError(e);
      })
    );
  }

  getClienteByNit(nit: string): Observable<Cliente> {
    return this.http.get<Cliente>(`${this.url}/clientes/nit/${nit}`);
  }

  getTotalClientes(): Observable<any> {
    return this.http.get<any>(`${this.url}/clientes/cantidad-clientes`).pipe(
      catchError(e => {
        swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }

  create(cliente: Cliente): Observable<any> {
    return this.http.post<any>(`${this.url}/clientes`, cliente).pipe(
      catchError(e => {
        swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }

  update(cliente: Cliente): Observable<any> {
    return this.http.put<any>(`${this.url}/clientes`, cliente).pipe(
      catchError(e => {
        swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }

  delete(id: number): Observable<Cliente> {
    return this.http.delete<Cliente>(`${this.url}/clientes/${id}`).pipe(
      catchError(e => {
        swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }
}
