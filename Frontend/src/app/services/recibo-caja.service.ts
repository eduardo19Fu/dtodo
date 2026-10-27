import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { CrearReciboCajaDto, ReciboCajaDto } from '../dtos/recibo-caja-dto';
import { global } from './global';

/**
 * Recibos de caja (administrador y cobrador). No muestra alertas: las reglas de negocio llegan como 422 con el mensaje en
 * {@code error.error.message} y las muestra la pantalla.
 */
@Injectable({
  providedIn: 'root'
})
export class ReciboCajaService {

  private url: string;

  constructor(private http: HttpClient) {
    this.url = `${global.url}/recibos`;
  }

  getListado(page: number, size: number, filtros: {
    idSucursal?: number; idCliente?: number; desde?: string; hasta?: string; incluirAnulados?: boolean;
  }): Observable<any> {
    let params = new HttpParams().set('size', size.toString());
    if (filtros.idSucursal) {
      params = params.set('idSucursal', filtros.idSucursal.toString());
    }
    if (filtros.idCliente) {
      params = params.set('idCliente', filtros.idCliente.toString());
    }
    if (filtros.desde) {
      params = params.set('desde', filtros.desde);
    }
    if (filtros.hasta) {
      params = params.set('hasta', filtros.hasta);
    }
    if (filtros.incluirAnulados) {
      params = params.set('incluirAnulados', 'true');
    }
    return this.http.get(`${this.url}/page/${page}`, { params });
  }

  emitir(recibo: CrearReciboCajaDto): Observable<ReciboCajaDto> {
    return this.http.post<ReciboCajaDto>(this.url, recibo);
  }

  anular(idRecibo: number, motivo: string): Observable<ReciboCajaDto> {
    return this.http.post<ReciboCajaDto>(`${this.url}/${idRecibo}/anular`, { motivo });
  }

  imprimir(idRecibo: number): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.url}/${idRecibo}/imprimir`, { observe: 'response', responseType: 'blob' });
  }
}
