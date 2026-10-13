import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { Bodega } from '../models/bodega';
import { ImportacionInventarioDto, MovimientoBodegaRequest, OrigenInventarioBodega } from '../dtos/movimiento-bodega-request';
import { TipoMovimientoBodega } from '../dtos/movimiento-bodega-dto';

import { global } from './global';
import swal from 'sweetalert2';

export interface FiltroMovimientosBodega {
  fechaIni?: string;
  fechaFin?: string;
  tipo?: TipoMovimientoBodega;
  filtro?: string;
}

@Injectable({
  providedIn: 'root'
})
export class BodegaService {

  private url: string;

  constructor(private http: HttpClient) {
    this.url = global.url;
  }

  getBodegas(soloActivas = false): Observable<Bodega[]> {
    const params = new HttpParams().set('soloActivas', String(soloActivas));
    return this.http.get<Bodega[]>(`${this.url}/bodegas`, { params });
  }

  getListado(page: number, size: number, filtro: string, orden: string, direccion: string): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('filtro', filtro || '')
      .set('orden', orden)
      .set('direccion', direccion);
    return this.http.get(`${this.url}/bodegas/listado`, { params });
  }

  /** Total de bodegas registradas, tomado de la paginación del listado. */
  getTotalBodegas(): Observable<number> {
    return this.getListado(0, 1, '', 'nombre', 'asc').pipe(map(respuesta => respuesta.totalElements));
  }

  getBodega(id: number): Observable<Bodega> {
    return this.http.get<Bodega>(`${this.url}/bodegas/${id}`).pipe(
      catchError(e => this.mostrarError('Error al consultar la bodega', e))
    );
  }

  create(bodega: Bodega): Observable<Bodega> {
    return this.http.post<Bodega>(`${this.url}/bodegas`, bodega).pipe(
      catchError(e => this.mostrarError('No se pudo registrar la bodega', e))
    );
  }

  update(bodega: Bodega): Observable<Bodega> {
    return this.http.put<Bodega>(`${this.url}/bodegas`, bodega).pipe(
      catchError(e => this.mostrarError('No se pudo actualizar la bodega', e))
    );
  }

  /*********** INVENTARIO ***********/

  getInventario(idBodega: number, page: number, size: number, filtro: string): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('filtro', filtro || '');
    return this.http.get(`${this.url}/bodegas/${idBodega}/inventario`, { params });
  }

  getInventarioPorCodigo(idBodega: number, codigo: string): Observable<any> {
    return this.http.get(`${this.url}/bodegas/${idBodega}/inventario/codigo/${encodeURIComponent(codigo)}`);
  }

  agregarProducto(idBodega: number, request: MovimientoBodegaRequest): Observable<any> {
    return this.http.post<any>(`${this.url}/bodegas/${idBodega}/movimientos/agregar`, request).pipe(
      catchError(e => this.mostrarError('No se pudo agregar el producto', e))
    );
  }

  reducirExistencias(idBodega: number, request: MovimientoBodegaRequest): Observable<any> {
    return this.http.post<any>(`${this.url}/bodegas/${idBodega}/movimientos/reducir`, request).pipe(
      catchError(e => this.mostrarError('No se pudieron reducir las existencias', e))
    );
  }

  eliminarProducto(idBodega: number, idProducto: number, motivo: string): Observable<any> {
    const params = new HttpParams().set('motivo', motivo || '');
    return this.http.delete<any>(`${this.url}/bodegas/${idBodega}/inventario/${idProducto}`, { params }).pipe(
      catchError(e => this.mostrarError('No se pudo eliminar el producto', e))
    );
  }

  getMovimientos(idBodega: number, page: number, size: number, filtros: FiltroMovimientosBodega = {}): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('filtro', filtros.filtro || '');
    if (filtros.fechaIni && filtros.fechaFin) {
      params = params.set('fechaIni', filtros.fechaIni).set('fechaFin', filtros.fechaFin);
    }
    if (filtros.tipo) {
      params = params.set('tipo', filtros.tipo);
    }
    return this.http.get(`${this.url}/bodegas/${idBodega}/movimientos`, { params });
  }

  /*********** IMPORTACIÓN DE INVENTARIO ***********/

  clonarInventario(idBodegaDestino: number, origen: OrigenInventarioBodega, idOrigen: number): Observable<any> {
    const params = new HttpParams().set('origen', origen).set('idOrigen', idOrigen.toString());
    return this.http.post<any>(`${this.url}/bodegas/${idBodegaDestino}/clonar-inventario`, {}, { params }).pipe(
      catchError(e => this.mostrarError('No se pudo copiar el inventario', e))
    );
  }

  importarExcel(idBodega: number, archivo: File): Observable<ImportacionInventarioDto> {
    const formData = new FormData();
    formData.append('archivo', archivo, archivo.name);
    return this.http.post<ImportacionInventarioDto>(`${this.url}/bodegas/${idBodega}/importar-excel`, formData).pipe(
      catchError(e => this.mostrarError('No se pudo importar el archivo', e))
    );
  }

  descargarPlantillaImportacion(): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.url}/bodegas/plantilla-importacion`, { observe: 'response', responseType: 'blob' });
  }

  private mostrarError(titulo: string, e: any): Observable<never> {
    const mensaje = (e && e.error && (e.error.message || e.error.detail)) || 'Ha ocurrido un error inesperado';
    swal.fire(titulo, mensaje, 'error');
    return throwError(e);
  }
}
