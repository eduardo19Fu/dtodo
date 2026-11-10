import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ChequeDto, EstadoCheque } from '../dtos/recibo-caja-dto';
import { global } from './global';

/**
 * Control de cheques (administrador y cobrador). No muestra alertas: las reglas de negocio llegan como 422 con el mensaje en
 * {@code error.error.message} y las muestra la pantalla.
 */
@Injectable({
  providedIn: 'root'
})
export class ChequeService {

  private url: string;

  constructor(private http: HttpClient) {
    this.url = `${global.url}/cheques`;
  }

  getListado(page: number, size: number, filtros: {
    idSucursal?: number; estado?: string; desde?: string; hasta?: string;
  }): Observable<any> {
    let params = new HttpParams().set('size', size.toString());
    if (filtros.idSucursal) {
      params = params.set('idSucursal', filtros.idSucursal.toString());
    }
    if (filtros.estado) {
      params = params.set('estado', filtros.estado);
    }
    if (filtros.desde) {
      params = params.set('desde', filtros.desde);
    }
    if (filtros.hasta) {
      params = params.set('hasta', filtros.hasta);
    }
    return this.http.get(`${this.url}/page/${page}`, { params });
  }

  cambiarEstado(idCheque: number, estado: EstadoCheque, motivo?: string): Observable<ChequeDto> {
    return this.http.post<ChequeDto>(`${this.url}/${idCheque}/estado`, { estado, motivo: motivo || null });
  }

  /** El control de cheques del período en PDF o Excel; el cobrador solo obtiene los de su sucursal. */
  reporte(formato: 'PDF' | 'XLSX', filtros: { idSucursal?: number; estado?: string; desde: string; hasta: string }):
      Observable<HttpResponse<Blob>> {
    let params = new HttpParams().set('fechaInicio', filtros.desde).set('fechaFin', filtros.hasta).set('formato', formato);
    if (filtros.idSucursal) {
      params = params.set('idSucursal', filtros.idSucursal.toString());
    }
    if (filtros.estado) {
      params = params.set('estado', filtros.estado);
    }
    return this.http.get(`${global.url}/reportes/controles/cheques`, { params, observe: 'response', responseType: 'blob' });
  }
}
