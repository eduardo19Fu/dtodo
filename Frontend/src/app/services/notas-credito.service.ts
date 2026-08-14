import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { global } from './global';
import { NotaCredito } from '../models/nota-credito';
import { DespachoNota } from '../models/despacho-nota';
import { DespachoNotaDto } from '../dtos/despacho-nota-dto';
import { DespachoRequest } from '../dtos/despacho-request';
import { NotaCreditoDto } from '../dtos/nota-credito-dto';

import Swal from 'sweetalert2';

@Injectable({
  providedIn: 'root'
})
export class NotasCreditoService {

  url: string;

  constructor(private httpClient: HttpClient) {
    this.url = global.url;
  }

  getNotasCredito(): Observable<NotaCreditoDto[]> {
    return this.httpClient.get<NotaCreditoDto[]>(`${this.url}/notas-credito`).pipe(
      catchError(e => {
        if (e.status === 204 || e.status === 404) {
          return of([]);
        }
        const mensaje = e.error?.mensaje || e.error?.message || 'Error al cargar notas de crédito';
        const detalle = e.error?.error || e.error?.status || '';
        Swal.fire(mensaje, `${detalle}`, 'error');
        return throwError(e);
      })
    );
  }

  getUltimasNotasCredito(page: number, filtro: string = '', size: number = 5): Observable<any> {
    const params = new HttpParams()
      .set('filtro', filtro)
      .set('size', size.toString());
    return this.httpClient.get<any>(`${this.url}/notas-credito-dto/ultimas/${page}`, { params }).pipe(
      catchError(e => throwError(e))
    );
  }

  getNotasCreditoPorFechas(page: number, fechaIni: string, fechaFin: string,
                           filtro: string = '', size: number = 5): Observable<any> {
    const params = new HttpParams()
      .set('fechaIni', fechaIni)
      .set('fechaFin', fechaFin)
      .set('filtro', filtro)
      .set('size', size.toString());
    return this.httpClient.get<any>(`${this.url}/notas-credito-dto/fechas/${page}`, { params }).pipe(
      catchError(e => throwError(e))
    );
  }

  getNotasCreditoActivas(): Observable<NotaCreditoDto[]> {
    return this.httpClient.get<NotaCreditoDto[]>(`${this.url}/notas-credito/activas`).pipe(
      catchError(e => {
        if (e.status === 204 || e.status === 404) {
          return of([]);
        }
        const mensaje = e.error?.mensaje || e.error?.message || 'Error al cargar notas activas';
        const detalle = e.error?.error || e.error?.status || '';
        Swal.fire(mensaje, `${detalle}`, 'error');
        return throwError(e);
      })
    );
  }

  getNotaCredito(id: number): Observable<any> {
    return this.httpClient.get<any>(`${this.url}/notas-credito/${id}`).pipe(
      catchError(e => {
        const mensaje = e.error?.mensaje || e.error?.message || 'Error al cargar nota de crédito';
        const detalle = e.error?.error || e.error?.status || '';
        Swal.fire(mensaje, `${detalle}`, 'error');
        return throwError(e);
      })
    );
  }

  create(notaCredito: NotaCredito): Observable<any> {
    return this.httpClient.post<any>(`${this.url}/notas-credito`, notaCredito).pipe(
      catchError(e => {
        // 409 Conflict: ya existe una nota de credito para el mismo documento origen
        if (e.status === 409) {
          const mensajeDuplicado = e.error?.message || e.error?.mensaje
            || 'Ya existe una Nota de Crédito para el documento indicado.';
          Swal.fire('Nota de Crédito Duplicada', mensajeDuplicado, 'warning');
          return throwError(e);
        }
        const mensaje = e.error?.mensaje || e.error?.message || 'Error al crear nota de crédito';
        const detalle = e.error?.error || e.error?.status || '';
        Swal.fire(mensaje, `${detalle}`, 'error');
        return throwError(e);
      })
    );
  }

  anular(idNota: number): Observable<NotaCredito> {
    return this.httpClient.put<NotaCredito>(`${this.url}/notas-credito/anular/${idNota}`, {}).pipe(
      catchError(e => {
        const mensaje = e.error?.mensaje || e.error?.message || 'Error al anular nota de crédito';
        const detalle = e.error?.error || e.error?.status || '';
        Swal.fire(mensaje, `${detalle}`, 'error');
        return throwError(e);
      })
    );
  }

  entregar(idNota: number): Observable<NotaCredito> {
    return this.httpClient.put<NotaCredito>(`${this.url}/notas-credito/entregar/${idNota}`, {}).pipe(
      catchError(e => {
        const mensaje = e.error?.mensaje || e.error?.message || 'Error al entregar nota de crédito';
        const detalle = e.error?.error || e.error?.status || '';
        Swal.fire(mensaje, `${detalle}`, 'error');
        return throwError(e);
      })
    );
  }

  // --- Despachos ---

  getDespachos(idNota: number): Observable<DespachoNotaDto[]> {
    return this.httpClient.get<DespachoNotaDto[]>(`${this.url}/despachos-nota/${idNota}`).pipe(
      catchError(e => {
        if (e.status === 204 || e.status === 404) {
          return of([]);
        }
        const mensaje = e.error?.mensaje || e.error?.message || 'Error al cargar despachos';
        const detalle = e.error?.error || e.error?.status || '';
        Swal.fire(mensaje, `${detalle}`, 'error');
        return throwError(e);
      })
    );
  }

  /**
   * Registra un despacho enviando las líneas y la contraseña de autorización
   * en un único payload.
   *
   * El 422 (contraseña incorrecta) se propaga sin Swal: lo maneja el modal de
   * autorización mostrando el error en línea, sin cerrarse ni perder la
   * selección de productos.
   */
  registrarDespacho(idNota: number, despachos: DespachoNota[], password: string): Observable<DespachoNotaDto[]> {
    const request = new DespachoRequest(password, despachos);

    return this.httpClient.post<DespachoNotaDto[]>(`${this.url}/despachos-nota/${idNota}`, request).pipe(
      catchError(e => {
        if (e.status === 422) {
          return throwError(e);
        }
        const mensaje = e.error?.mensaje || e.error?.message || 'Error al registrar despacho';
        const detalle = e.error?.error || e.error?.status || '';
        Swal.fire(mensaje, `${detalle}`, 'error');
        return throwError(e);
      })
    );
  }

  getComprobanteDespachoPDF(idEvento: string): Observable<any> {
    const headers = new HttpHeaders();
    headers.append('Accept', 'application/pdf');
    const requestOptions: any = { headers, responseType: 'blob' };

    return this.httpClient.get<any>(`${this.url}/despachos-nota/comprobante/${idEvento}`, requestOptions).pipe(
      map((response: any) => {
        return {
          filename: `comprobante_despacho_${idEvento}.pdf`,
          data: new Blob([response], { type: 'application/pdf' })
        };
      }),
      catchError(e => {
        Swal.fire('Error', 'No se pudo generar el comprobante del despacho.', 'error');
        return throwError(e);
      })
    );
  }

  getNotaCreditoPDF(idNota: number): Observable<any> {
    const headers = new HttpHeaders();
    headers.append('Accept', 'application/pdf');
    const requestOptions: any = { headers, responseType: 'blob' };

    return this.httpClient.get<any>(`${this.url}/notas-credito/generate/${idNota}`, requestOptions).pipe(
      map((response: any) => {
        return {
          filename: 'nota_credito.pdf',
          data: new Blob([response], { type: 'application/pdf' })
        };
      }),
      catchError(e => {
        Swal.fire('Error', 'No se pudo generar el reporte de la nota de crédito.', 'error');
        return throwError(e);
      })
    );
  }
}
