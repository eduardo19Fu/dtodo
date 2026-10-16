import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { DevolucionBodega, EstadoDevolucionBodega } from '../models/devolucion-bodega';
import { DevolucionBodegaRequest } from '../dtos/devolucion-bodega-request';

import { global } from './global';
import swal from 'sweetalert2';

@Injectable({
  providedIn: 'root'
})
export class DevolucionBodegaService {

  private url: string;

  constructor(private http: HttpClient) {
    this.url = global.url;
  }

  getListado(page: number, size: number, filtro: string, idBodega?: number, estado?: EstadoDevolucionBodega): Observable<any> {
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
    return this.http.get(`${this.url}/devoluciones-bodega/listado`, { params });
  }

  /** Cantidad de devoluciones pendientes de aprobación, tomada de la paginación del listado. */
  getTotalPendientes(): Observable<number> {
    return this.getListado(0, 1, '', null, 'PENDIENTE').pipe(map(respuesta => respuesta.totalElements));
  }

  /** Productos de la sucursal que devuelve, con su existencia actual. */
  getProductosSucursal(idSucursal: number, page: number, size: number, filtro: string): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('filtro', filtro || '');
    return this.http.get(`${this.url}/devoluciones-bodega/sucursales/${idSucursal}/productos`, { params });
  }

  getDevolucion(id: number): Observable<DevolucionBodega> {
    return this.http.get<DevolucionBodega>(`${this.url}/devoluciones-bodega/${id}`).pipe(
      catchError(e => this.mostrarError('Error al consultar la devolución', e))
    );
  }

  create(request: DevolucionBodegaRequest): Observable<DevolucionBodega> {
    return this.http.post<DevolucionBodega>(`${this.url}/devoluciones-bodega`, request).pipe(
      catchError(e => this.mostrarError('No se pudo registrar la devolución', e))
    );
  }

  aprobar(idDevolucion: number): Observable<DevolucionBodega> {
    return this.http.put<DevolucionBodega>(`${this.url}/devoluciones-bodega/${idDevolucion}/aprobar`, {}).pipe(
      catchError(e => this.mostrarError('No se pudo aprobar la devolución', e))
    );
  }

  /** Rechaza (o cancela) una devolución pendiente; las existencias reservadas regresan a la sucursal. */
  cancelar(idDevolucion: number, motivo: string): Observable<DevolucionBodega> {
    return this.http.put<DevolucionBodega>(`${this.url}/devoluciones-bodega/${idDevolucion}/cancelar`, { motivo }).pipe(
      catchError(e => this.mostrarError('No se pudo rechazar la devolución', e))
    );
  }

  private mostrarError(titulo: string, e: any): Observable<never> {
    const mensaje = (e && e.error && (e.error.message || e.error.detail)) || 'Ha ocurrido un error inesperado';
    swal.fire(titulo, mensaje, 'error');
    return throwError(e);
  }
}
