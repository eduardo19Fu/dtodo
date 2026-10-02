import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { DespachoBodega, EstadoDespachoBodega } from '../models/despacho-bodega';
import { DespachoBodegaRequest } from '../dtos/despacho-bodega-request';

import { global } from './global';
import swal from 'sweetalert2';

@Injectable({
  providedIn: 'root'
})
export class DespachoBodegaService {

  private url: string;

  constructor(private http: HttpClient) {
    this.url = global.url;
  }

  getListado(page: number, size: number, filtro: string, idBodega?: number, estado?: EstadoDespachoBodega): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('filtro', filtro || '');
    if (idBodega) {
      params = params.set('idBodega', idBodega.toString());
    }
    if (estado) {
      params = params.set('estado', estado);
    }
    return this.http.get(`${this.url}/despachos-bodega/listado`, { params });
  }

  /** Cantidad de despachos pendientes de aprobación, tomada de la paginación del listado. */
  getTotalPendientes(): Observable<number> {
    return this.getListado(0, 1, '', null, 'PENDIENTE').pipe(map(respuesta => respuesta.totalElements));
  }

  getDespacho(id: number): Observable<DespachoBodega> {
    return this.http.get<DespachoBodega>(`${this.url}/despachos-bodega/${id}`).pipe(
      catchError(e => this.mostrarError('Error al consultar el despacho', e))
    );
  }

  create(request: DespachoBodegaRequest): Observable<DespachoBodega> {
    return this.http.post<DespachoBodega>(`${this.url}/despachos-bodega`, request).pipe(
      catchError(e => this.mostrarError('No se pudo registrar el despacho', e))
    );
  }

  aprobar(idDespacho: number): Observable<DespachoBodega> {
    return this.http.put<DespachoBodega>(`${this.url}/despachos-bodega/${idDespacho}/aprobar`, {}).pipe(
      catchError(e => this.mostrarError('No se pudo aprobar el despacho', e))
    );
  }

  cancelar(idDespacho: number, motivo: string): Observable<DespachoBodega> {
    return this.http.put<DespachoBodega>(`${this.url}/despachos-bodega/${idDespacho}/cancelar`, { motivo }).pipe(
      catchError(e => this.mostrarError('No se pudo cancelar el despacho', e))
    );
  }

  /** Descarga el comprobante en PDF y lo abre en una pestaña nueva para imprimirlo. */
  imprimirComprobante(idDespacho: number): Observable<void> {
    return this.http.get(`${this.url}/despachos-bodega/${idDespacho}/comprobante`, { responseType: 'blob' }).pipe(
      map(pdf => {
        const urlPdf = window.URL.createObjectURL(pdf);
        window.open(urlPdf, '_blank');
        window.setTimeout(() => window.URL.revokeObjectURL(urlPdf), 60000);
      }),
      catchError(e => this.mostrarError('No se pudo generar el comprobante', e))
    );
  }

  private mostrarError(titulo: string, e: any): Observable<never> {
    // Una respuesta de error de un blob llega como Blob: no hay mensaje legible del servidor
    const mensaje = (e && e.error && (e.error.message || e.error.detail)) || 'Ha ocurrido un error inesperado';
    swal.fire(titulo, mensaje, 'error');
    return throwError(e);
  }
}
