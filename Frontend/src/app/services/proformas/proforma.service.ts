import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';

import { global } from '../global';
import Swal from 'sweetalert2';
import { Observable, throwError } from 'rxjs';
import { Proforma } from 'src/app/models/proforma';
import { catchError, map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class ProformaService {

  url: string;

  constructor(
    private httpClient: HttpClient
  ) {
    this.url = global.url;
  }

  getProformas(): Observable<Proforma[]> {
    return this.httpClient.get<Proforma[]>(`${this.url}/proformas`);
  }

  getProforma(id: number): Observable<any> {
    return this.httpClient.get<any>(`${this.url}/proformas/${id}`).pipe(
      catchError(e => {
        Swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }

  /**
   * Busca una proforma por su número único.
   * Usado por el flujo de creación de Notas de Crédito basadas en Proforma.
   *
   * @param noProforma número único de la proforma
   */
  getProformaByNoProforma(noProforma: string): Observable<Proforma> {
    return this.httpClient.get<Proforma>(`${this.url}/proformas/by-no-proforma/${encodeURIComponent(noProforma)}`).pipe(
      catchError(e => {
        if (e.status === 404) {
          Swal.fire('Proforma no encontrada',
            `No existe una proforma con el número "${noProforma}".`,
            'warning');
        } else {
          const mensaje = e.error?.message || e.error?.mensaje || 'Error al cargar proforma';
          Swal.fire(mensaje, e.error?.error || '', 'error');
        }
        return throwError(e);
      })
    );
  }

  getProformasSP(date1: Date, date2: Date): Observable<any> {
    return this.httpClient.get<any>(`${this.url}/proformas/get-listado-sp/get?date1=${date1.toString()}&date2=${date2.toString()}`).pipe(
      catchError(e => {
        Swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }

  getProformasDto(date1: Date, date2: Date): Observable<any> {
    return this.httpClient.get<any>(`${this.url}/proformas/get-listado-dto/get?date1=${date1.toString()}&date2=${date2.toString()}`).pipe(
      catchError(e => {
        Swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }

  create(proforma: Proforma): Observable<any> {
    return this.httpClient.post<any>(`${this.url}/proformas`, proforma).pipe(
      catchError(e => {
        Swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }

  update(profroma: Proforma): Observable<any> {
    return this.httpClient.put<any>(`${this.url}/proformas/${profroma.idProforma}`, profroma).pipe(
      catchError(e => {
        Swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }

  delete(id: number): Observable<any> {
    return this.httpClient.delete<any>(`${this.url}/proformas/${id}`).pipe(
      catchError(e => {
        Swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }

  getProformaPdf(id: number): Observable<any> {
    const headers = new HttpHeaders();
    headers.append('Accept', 'application/pdf');
    const requestOptions: any = { headers, responseType: 'blob' };

    return this.httpClient.get<any>(`${this.url}/proformas/generate/${id}`, requestOptions).pipe(
      map((response: any) => {
        return {
          filename: 'factura.pdf',
          data: new Blob([response], { type: 'application/pdf' })
        };
      })
    );
  }
}
