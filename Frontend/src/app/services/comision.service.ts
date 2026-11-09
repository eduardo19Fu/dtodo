import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { AvanceComisionDto, HistorialMetaComisionDto, DecisionComisionDto, MetaComisionDto } from '../dtos/comision-dto';
import { global } from './global';

/**
 * Comisiones de los vendedores (solo administrador). No muestra alertas: las reglas de negocio llegan como 422 con el
 * mensaje en {@code error.error.message} y las muestra la pantalla.
 */
@Injectable({
  providedIn: 'root'
})
export class ComisionService {

  private url: string;

  constructor(private http: HttpClient) {
    this.url = `${global.url}/comisiones`;
  }

  metas(anio: number): Observable<MetaComisionDto[]> {
    return this.http.get<MetaComisionDto[]>(`${this.url}/metas`, { params: new HttpParams().set('anio', String(anio)) });
  }

  /** Guarda las metas; el motivo es obligatorio si el año ya terminó y aún no se liquida. */
  guardarMetas(anio: number, metas: MetaComisionDto[], motivo?: string): Observable<MetaComisionDto[]> {
    let params = new HttpParams().set('anio', String(anio));
    if (motivo) {
      params = params.set('motivo', motivo);
    }
    return this.http.put<MetaComisionDto[]>(`${this.url}/metas`, metas, { params });
  }

  /** Los cambios de las metas del año, del más reciente al más antiguo. */
  historialMetas(anio: number): Observable<HistorialMetaComisionDto[]> {
    return this.http.get<HistorialMetaComisionDto[]>(`${this.url}/metas/historial`,
      { params: new HttpParams().set('anio', String(anio)) });
  }

  copiarMetas(desde: number, hacia: number): Observable<MetaComisionDto[]> {
    const params = new HttpParams().set('desde', String(desde)).set('hacia', String(hacia));
    return this.http.post<MetaComisionDto[]>(`${this.url}/metas/copiar`, null, { params });
  }

  avance(anio: number): Observable<AvanceComisionDto> {
    return this.http.get<AvanceComisionDto>(`${this.url}/avance`, { params: new HttpParams().set('anio', String(anio)) });
  }

  liquidar(anio: number, decisiones: DecisionComisionDto[]): Observable<AvanceComisionDto> {
    return this.http.post<AvanceComisionDto>(`${this.url}/liquidar`, { decisiones },
      { params: new HttpParams().set('anio', String(anio)) });
  }

  deshacer(anio: number, motivo: string): Observable<AvanceComisionDto> {
    return this.http.post<AvanceComisionDto>(`${this.url}/deshacer`, { motivo },
      { params: new HttpParams().set('anio', String(anio)) });
  }

  reporte(anio: number, formato: 'PDF' | 'XLSX'): Observable<HttpResponse<Blob>> {
    const params = new HttpParams().set('anio', String(anio)).set('formato', formato);
    return this.http.get(`${this.url}/reporte`, { params, observe: 'response', responseType: 'blob' });
  }
}
