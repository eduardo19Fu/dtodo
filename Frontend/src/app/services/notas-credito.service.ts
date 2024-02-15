import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { global } from './global';
import { NotaCredito } from '../models/nota-credito';

import Swal from 'sweetalert2';

@Injectable({
  providedIn: 'root'
})
export class NotasCreditoService {

  url: string;

  constructor(private httpClient: HttpClient) {
    this.url = global.url;
  }

  getNotasCredito(): Observable<NotaCredito[]> {
    return this.httpClient.get<NotaCredito[]>(`${this.url}/notas-credito`).pipe(
      catchError(e => {
        Swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }

  getNotasCreditoActivas(): Observable<any> {
    return this.httpClient.get<any>(`${this.url}/notas-credito/activas`).pipe(
      catchError(e => {
        Swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }

  getNotaCredito(id: number): Observable<any> {
    return this.httpClient.get<any>(`${this.url}/notas-credito/${id}`).pipe(
      catchError(e => {
        Swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }

  create(notaCredito: NotaCredito): Observable<any> {
    return this.httpClient.post<any>(`${this.url}/notas-credito`, notaCredito).pipe(
      catchError(e => {
        Swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }
}
