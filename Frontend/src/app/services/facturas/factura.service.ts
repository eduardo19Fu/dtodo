import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { Factura } from 'src/app/models/factura';

import { global } from '../global';
import swal from 'sweetalert2';

@Injectable({
  providedIn: 'root'
})
export class FacturaService {

  private url: string;

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    this.url = global.url;
  }

  getFacturas(): Observable<Factura[]> {
    return this.http.get<Factura[]>(`${this.url}/facturas`);
  }

  getFacturasSP(date1: Date, date2: Date): Observable<any> {
    return this.http.get<any>(`${this.url}/facturas/get-listado-sp/get?fechaIni=${date1.toString()}&fechaFin=${date2.toString()}`).pipe(
      catchError(e => {
        swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }

  getFacturasPage(page: number): Observable<any> {
    return this.http.get<any>(`${this.url}/facturas/page/${page}`).pipe(
      map((response: any) => {
        (response.content as Factura[]).map(factura => {
          return factura;
        });
        return response;
      })
    );
  }

  getFactura(id: number): Observable<Factura> {
    return this.http.get<Factura>(`${this.url}/facturas/factura/${id}`).pipe(
      catchError(e => {
        swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }

  getTotalVentas(): Observable<any> {
    return this.http.get<any>(`${this.url}/facturas/cantidad-ventas`).pipe(
      catchError(e => {
        swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }

  cancel(id: number, idusuario: number): Observable<any> {
    return this.http.delete<any>(`${this.url}/facturas/cancel/${id}/${idusuario}`).pipe(
      catchError(e => {
        swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }

  create(factura: Factura): Observable<any> {
    return this.http.post<any>(`${this.url}/facturas`, factura).pipe(
      catchError(e => {
        swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }

  /*********** FACTURA PDF **************/
  getBillPDF(idfactura: number): Observable<any> {
    const headers = new HttpHeaders();
    headers.append('Accept', 'application/pdf');
    const requestOptions: any = { headers, responseType: 'blob' };

    return this.http.get<any>(`${this.url}/facturas/generate/${idfactura}`, requestOptions).pipe(
      map((response: any) => {
        return {
          filename: 'factura.pdf',
          data: new Blob([response], { type: 'application/pdf' })
        };
      })
    );
  }

  getSellsDaillyReportPDF(cajero: number, fecha: Date): Observable<any> {
    const headers = new HttpHeaders();
    headers.append('Accept', 'application/pdf');
    const requestOptions: any = { headers, responseType: 'blob' };

    return this.http.get<any>(`${this.url}/facturas/daily-sales?usuario=${cajero}&fecha=${fecha.toString()}`, requestOptions).pipe(
      map((response: any) => {
        return {
          filename: 'poliza.pdf',
          data: new Blob([response], { type: 'application/pdf' })
        };
      })
    );
  }
}
