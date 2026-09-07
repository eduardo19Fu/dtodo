import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { Router } from '@angular/router';
import { AuthService } from './auth.service';

import { Sucursal } from '../models/sucursal';

import { global } from './global';
import swal from 'sweetalert2';

@Injectable({
  providedIn: 'root'
})
export class SucursalService {

  private url: string;

  constructor(
    private http: HttpClient,
    private router: Router,
    private authService: AuthService
  ) {
    this.url = global.url;
  }

  getSucursales(): Observable<Sucursal[]> {
    return this.http.get<Sucursal[]>(`${this.url}/sucursales`);
  }

  getSucursalesPage(page: number): Observable<any> {
    return this.http.get(`${this.url}/sucursales/page/${page}`);
  }

  getListado(page: number, size: number, filtro: string, orden: string, direccion: string): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('filtro', filtro || '')
      .set('orden', orden)
      .set('direccion', direccion);
    return this.http.get(`${this.url}/sucursales/listado`, { params });
  }

  getSucursal(id: number): Observable<Sucursal> {
    return this.http.get<Sucursal>(`${this.url}/sucursales/${id}`).pipe(
      catchError(e => {
        swal.fire('Error al consultar la sucursal', e.error.message, 'error');
        return throwError(e);
      })
    );
  }

  getPrincipal(): Observable<Sucursal> {
    return this.http.get<Sucursal>(`${this.url}/sucursales/principal`);
  }

  create(sucursal: Sucursal): Observable<Sucursal> {
    return this.http.post<Sucursal>(`${this.url}/sucursales`, sucursal).pipe(
      catchError(e => {
        swal.fire(e.error.status, e.error.message, 'error');
        return throwError(e);
      })
    );
  }

  update(sucursal: Sucursal): Observable<Sucursal> {
    return this.http.put<Sucursal>(`${this.url}/sucursales`, sucursal).pipe(
      catchError(e => {
        swal.fire(e.error.status, e.error.message, 'error');
        return throwError(e);
      })
    );
  }

  clonarInventario(idSucursalDestino: number, idSucursalOrigen: number): Observable<any> {
    return this.http.post<any>(
      `${this.url}/sucursales/${idSucursalDestino}/clonar-inventario/${idSucursalOrigen}`, {}
    ).pipe(
      catchError(e => {
        swal.fire(e.error.status, e.error.message, 'error');
        return throwError(e);
      })
    );
  }
}
