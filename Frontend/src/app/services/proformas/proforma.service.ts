import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { global } from '../global';
import Swal from 'sweetalert2';
import { Observable, throwError } from 'rxjs';
import { Proforma } from 'src/app/models/proforma';
import { catchError } from 'rxjs/operators';

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

  getProformasSP(fechaIni: Date, fechaFin: Date): Observable<any> {
    return this.httpClient.get<any>(`${this.url}/proformas/get-listado-sp/get?fechaIni=${fechaIni.toString()}&fechaFin=${fechaFin.toString()}`).pipe(
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

  delete(id: number): Observable<any> {
    return this.httpClient.delete<any>(`${this.url}/proformas/${id}`).pipe(
      catchError(e => {
        Swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }
}
