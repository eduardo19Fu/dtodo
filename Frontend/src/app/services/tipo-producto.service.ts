import { Injectable } from '@angular/core';
import { global } from '../services/global';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { TipoProducto } from '../models/tipo-producto';
import { catchError, map } from 'rxjs/operators';

import swal from 'sweetalert2';

@Injectable({
  providedIn: 'root'
})
export class TipoProductoService {

  private url: string;

  constructor(
    private http: HttpClient
  ) {
    this.url = global.url;
  }

  getTiposProducto(): Observable<TipoProducto[]> {
    return this.http.get<TipoProducto[]>(`${this.url}/tipos-producto`).pipe(
      map((response: any) => {
        (response as TipoProducto[]).map(tipo => {
          tipo.tipoProducto = tipo.tipoProducto.toUpperCase();
          return tipo;
        });
        return response;
      })
    );
  }

  getTiposPaginados(page: number): Observable<TipoProducto[]> {
    return this.http.get(`${this.url}/tipos-producto/page/${page}`).pipe(
      map((response: any) => {
        (response.content as TipoProducto[]).map(tipo => {
          tipo.tipoProducto = tipo.tipoProducto.toUpperCase();
          return tipo;
        });
        return response;
      })
    );
  }

  getTipoProducto(id: number): Observable<TipoProducto> {
    return this.http.get<TipoProducto>(`${this.url}/tipos-producto/${id}`).pipe(
      catchError(e => {
        swal.fire('Error al consultar el tipo deseado', e.error.message, 'error');
        return throwError(e);
      })
    );
  }

  create(tipoProducto: TipoProducto): Observable<any> {
    return this.http.post<any>(`${this.url}/tipos-producto`, tipoProducto).pipe(
      catchError(e => {
        swal.fire(e.error.status, e.error.message, 'error');
        return throwError(e);
      })
    );
  }

  update(tipoProducto: TipoProducto): Observable<any> {
    return this.http.put<any>(`${this.url}/tipos-producto`, tipoProducto).pipe(
      catchError(e => {
        swal.fire(e.error.status, e.error.message, 'error');
        return throwError(e);
      })
    );
  }

  delete(id: number): Observable<TipoProducto> {
    return this.http.delete<TipoProducto>(`${this.url}/tipos-producto/${id}`).pipe(
      catchError(e => {
        swal.fire(e.error.status, e.error.message, 'error');
        return throwError(e);
      })
    );
  }
}
