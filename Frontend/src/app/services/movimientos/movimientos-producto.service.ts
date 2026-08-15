import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

import { MovimientoProducto } from 'src/app/models/movimiento-producto';

import { global } from '../global';
import Swal from 'sweetalert2';

@Injectable({
  providedIn: 'root'
})
export class MovimientosProductoService {

  url: string;

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    this.url = global.url;
  }

  getMovimientosProducto(): Observable<MovimientoProducto[]> {
    return this.http.get<MovimientoProducto[]>(`${this.url}/movimientos`);
  }

  getMovimientosDtoPaginados(page: number, size: number = 5): Observable<any> {
    return this.http.get<any>(`${this.url}/movimientos-dto/page/${page}?size=${size}`).pipe(
      catchError(e => {
        console.error(e);
        return throwError(e);
      })
    );
  }

  buscarMovimientosDto(page: number, filtro: string, size: number = 5): Observable<any> {
    return this.http.get<any>(`${this.url}/movimientos-dto/search/${page}?filtro=${filtro}&size=${size}`).pipe(
      catchError(e => {
        console.error(e);
        return throwError(e);
      })
    );
  }

  getListado(page: number, size: number, filtro: string,
             orden: string, direccion: 'asc' | 'desc',
             fechaIni?: string, fechaFin?: string): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('filtro', filtro || '')
      .set('orden', orden)
      .set('direccion', direccion);
    if (fechaIni && fechaFin) {
      params = params.set('fechaIni', fechaIni).set('fechaFin', fechaFin);
    }
    return this.http.get<any>(`${this.url}/movimientos/listado`, {params});
  }

  getMovimientosProductoPage(idproducto: number, page: number): Observable<any> {
    return this.http.get<any>(`${this.url}/movimientos/${idproducto}/${page}`).pipe(
      map((response: any) => {
        (response.content as MovimientoProducto[]).map(movimientoProducto => {
          return movimientoProducto;
        });
        return response;
      })
    );
  }

  create(movimientoProducto: MovimientoProducto): Observable<any> {
    return this.http.post<any>(`${this.url}/movimientos`, movimientoProducto).pipe(
      catchError(e => {
        Swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }

  /********* INVENTARIO FORMATO PDF ***********/
  getInventoryPDF(fechaIni: Date, fechaFin: Date): Observable<any> {
    const headers = new HttpHeaders();
    headers.append('Accept', 'application/pdf');
    const requestOptions: any = { headers, responseType: 'blob' };

    return this.http.post(`${this.url}/movimientos/inventario?fechaIni=${fechaIni.toString()}&fechaFin=${fechaFin.toString()}`,
      '', requestOptions).pipe(

      map((response: any) => {
        return {
          filename: 'inventario.pdf',
          data: new Blob([response], { type: 'application/pdf' })
        };
      })
    );
  }
}
