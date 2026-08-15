import { Injectable } from '@angular/core';
import { Correlativo } from 'src/app/models/correlativo';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';

import { global } from '../global';
import swal from 'sweetalert2';
import { map, catchError } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class CorrelativoService {

  url: string;

  constructor(
    private http: HttpClient
  ) {
    this.url = global.url;
  }

  getCorrelativos(): Observable<Correlativo[]>{
    return this.http.get<Correlativo[]>(`${this.url}/correlativos`);
  }

  getCorrelativosPage(page: number): Observable<any>{
    return this.http.get<any>(`${this.url}/correlativos/page/${page}`).pipe(
      map((response: any) => {
        (response.content as Correlativo[]).map(correlativo => {
          return correlativo;
        });
        return response;
      })
    );
  }

  getListado(page: number, size: number, filtro: string,
             orden: string, direccion: 'asc' | 'desc'): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('filtro', filtro || '')
      .set('orden', orden)
      .set('direccion', direccion);
    return this.http.get<any>(`${this.url}/correlativos/listado`, {params});
  }

  getCorrelativo(id: number): Observable<Correlativo>{
    return this.http.get<Correlativo>(`${this.url}/correlativos/${id}`).pipe(
      catchError(e => {
        swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }

  getCorrelativoPorUsuario(idusuario: number): Observable<Correlativo>{
    return this.http.get<Correlativo>(`${this.url}/correlativos/usuario/${idusuario}`).pipe(
      catchError(e => {
        swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }

  create(correlativo: Correlativo): Observable<any>{
    return this.http.post<any>(`${this.url}/correlativos`, correlativo).pipe(
      catchError(e => {
        swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }

  update(correlativo: Correlativo): Observable<any>{
    return this.http.put<any>(`${this.url}/correlativos`, correlativo).pipe(
      catchError(e => {
        swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }

  delete(id: number): Observable<any>{
    return this.http.delete<any>(`${this.url}/correlativos/${id}`).pipe(
      catchError(e => {
        swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }
}
