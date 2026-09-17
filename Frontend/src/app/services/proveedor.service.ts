import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { Proveedor } from '../models/proveedor';

import { global } from './global';
import swal from 'sweetalert2';

@Injectable({
  providedIn: 'root'
})
export class ProveedorService {

  private url: string;

  constructor(private http: HttpClient) {
    this.url = global.url;
  }

  getProveedores(): Observable<Proveedor[]> {
    return this.http.get<Proveedor[]>(`${this.url}/proveedores`);
  }

  getListado(page: number, size: number, filtro: string, orden: string, direccion: string): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('filtro', filtro || '')
      .set('orden', orden)
      .set('direccion', direccion);
    return this.http.get(`${this.url}/proveedores/listado`, { params });
  }

  getProveedor(id: number): Observable<Proveedor> {
    return this.http.get<Proveedor>(`${this.url}/proveedores/${id}`).pipe(
      catchError(e => {
        swal.fire('Error al consultar el proveedor', e.error.message, 'error');
        return throwError(e);
      })
    );
  }

  create(proveedor: Proveedor): Observable<Proveedor> {
    return this.http.post<Proveedor>(`${this.url}/proveedores`, proveedor).pipe(
      catchError(e => {
        swal.fire(e.error.status, e.error.message, 'error');
        return throwError(e);
      })
    );
  }

  update(proveedor: Proveedor): Observable<Proveedor> {
    return this.http.put<Proveedor>(`${this.url}/proveedores`, proveedor).pipe(
      catchError(e => {
        swal.fire(e.error.status, e.error.message, 'error');
        return throwError(e);
      })
    );
  }
}
