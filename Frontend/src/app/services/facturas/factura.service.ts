import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { Factura } from 'src/app/models/factura';
import { DocumentoOrigenNotaDto } from 'src/app/dtos/documento-origen-nota-dto';

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

  getFacturasDtoPaginadas(page: number, fechaIni: string, fechaFin: string, size: number = 5,
                          orden: string = 'fecha', direccion: string = 'desc'): Observable<any> {
    const params = new HttpParams()
      .set('fechaIni', fechaIni)
      .set('fechaFin', fechaFin)
      .set('size', size.toString())
      .set('orden', orden)
      .set('direccion', direccion);
    return this.http.get<any>(`${this.url}/facturas-dto/page/${page}`, { params }).pipe(
      catchError(e => throwError(e))
    );
  }

  buscarFacturasDto(page: number, fechaIni: string, fechaFin: string,
                    filtro: string, size: number = 5,
                    orden: string = 'fecha', direccion: string = 'desc'): Observable<any> {
    const params = new HttpParams()
      .set('fechaIni', fechaIni)
      .set('fechaFin', fechaFin)
      .set('filtro', filtro)
      .set('size', size.toString())
      .set('orden', orden)
      .set('direccion', direccion);
    return this.http.get<any>(`${this.url}/facturas-dto/search/${page}`, { params }).pipe(
      catchError(e => throwError(e))
    );
  }

  getUltimasFacturasDto(page: number, filtro: string = '', size: number = 5,
                        orden: string = 'fecha', direccion: string = 'desc'): Observable<any> {
    const params = new HttpParams()
      .set('filtro', filtro)
      .set('size', size.toString())
      .set('orden', orden)
      .set('direccion', direccion);
    return this.http.get<any>(`${this.url}/facturas-dto/ultimas/${page}`, { params }).pipe(
      catchError(e => throwError(e))
    );
  }

  getDetalleFacturaDto(idFactura: number, page: number, size: number = 5): Observable<any> {
    const params = new HttpParams().set('size', size.toString());
    return this.http.get<any>(`${this.url}/facturas-dto/${idFactura}/detalle/${page}`, { params }).pipe(
      catchError(e => throwError(e))
    );
  }

  getOrigenNotaDto(correlativo: string, serie: string): Observable<DocumentoOrigenNotaDto> {
    const params = new HttpParams()
      .set('correlativo', correlativo)
      .set('serie', serie);
    return this.http.get<DocumentoOrigenNotaDto>(`${this.url}/facturas-dto/nota-credito/origen`, { params })
      .pipe(catchError(e => throwError(e)));
  }

  getFactura(id: number): Observable<Factura> {
    return this.http.get<Factura>(`${this.url}/facturas/factura/${id}`).pipe(
      catchError(e => {
        swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }

  getFacturaByCorrelativoSat(correlativo: string, serie: string): Observable<Factura> {
    return this.http.get<Factura>(`${this.url}/facturas/get-by-correlativo-sat?correlativo=${correlativo}&serie=${serie}`).pipe(
      catchError(e => {
        swal.fire('Factura no encontrada', 'No se encontró una factura con el correlativo y serie ingresados.', 'warning');
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
        swal.fire(e.message, e.code, 'error');
        return throwError(e);
      })
    );
  }

  cancelV2(idusuario: number, factura: Factura): Observable<any> {
    return this.http.put<any>(`${this.url}/facturas/cancelV2/${idusuario}`, factura).pipe(
      catchError(e => {
        swal.fire(e.message, e.code, 'error');
        console.log(e);
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

  createV2(factura: Factura): Observable<any> {
    return this.http.post<any>(`${this.url}/facturas/createV2`, factura).pipe(
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
      }),
      catchError(e => {
        console.log(e);
        return throwError(e);
      })
    );
  }
}
