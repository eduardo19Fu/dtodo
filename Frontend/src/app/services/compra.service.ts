import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { Compra } from '../models/compra';

import { global } from './global';
import swal from 'sweetalert2';

@Injectable({
  providedIn: 'root'
})
export class CompraService {

  private url: string;

  constructor(private http: HttpClient) {
    this.url = global.url;
  }

  getListado(page: number, size: number, filtro: string, idSucursal: number): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('filtro', filtro || '');
    if (idSucursal) {
      params = params.set('idSucursal', idSucursal.toString());
    }
    return this.http.get(`${this.url}/compras/listado`, { params });
  }

  getCompra(id: number): Observable<Compra> {
    return this.http.get<Compra>(`${this.url}/compras/${id}`).pipe(
      catchError(e => {
        swal.fire('Error al consultar la compra', e.error.message, 'error');
        return throwError(e);
      })
    );
  }

  create(compra: Compra): Observable<Compra> {
    return this.http.post<Compra>(`${this.url}/compras`, compra).pipe(
      catchError(e => {
        swal.fire(e.error.status, e.error.message, 'error');
        return throwError(e);
      })
    );
  }

  anular(idCompra: number, idUsuario: number): Observable<Compra> {
    return this.http.put<Compra>(`${this.url}/compras/anular/${idCompra}/${idUsuario}`, {}).pipe(
      catchError(e => {
        swal.fire(e.error.status, e.error.message, 'error');
        return throwError(e);
      })
    );
  }
}
