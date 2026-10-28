import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { CobranzaDto } from '../dtos/cobranza-dto';
import { global } from './global';

/**
 * Cobranza de los clientes mayoristas (administrador y cobrador): cuentas por cobrar por antigüedad y estados de cuenta. No
 * muestra alertas: las reglas de negocio llegan como 422 con el mensaje en {@code error.error.message}.
 */
@Injectable({
  providedIn: 'root'
})
export class CobranzaService {

  private url: string;

  constructor(private http: HttpClient) {
    this.url = `${global.url}/cobranza`;
  }

  cuentasPorCobrar(): Observable<CobranzaDto> {
    return this.http.get<CobranzaDto>(`${this.url}/saldos`);
  }

  reporte(formato: 'PDF' | 'XLSX', idRuta: number | null, soloVencidos: boolean): Observable<HttpResponse<Blob>> {
    let params = new HttpParams().set('formato', formato).set('soloVencidos', String(soloVencidos));
    if (idRuta) {
      params = params.set('idRuta', String(idRuta));
    }
    return this.http.get(`${this.url}/saldos/reporte`, { params, observe: 'response', responseType: 'blob' });
  }

  estadoDeCuenta(idCliente: number): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.url}/clientes/${idCliente}/estado-cuenta`, { observe: 'response', responseType: 'blob' });
  }
}
