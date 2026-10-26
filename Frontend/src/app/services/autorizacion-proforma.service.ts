import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { AutorizacionProformaDto } from '../dtos/autorizacion-dto';
import { global } from './global';

/**
 * Bandeja de autorizaciones de las proformas mayoristas (solo administrador). No muestra alertas: las reglas de
 * negocio llegan como 422 con el mensaje en {@code error.error.message} y las muestra cada pantalla.
 */
@Injectable({
  providedIn: 'root'
})
export class AutorizacionProformaService {

  private url: string;

  constructor(private http: HttpClient) {
    this.url = global.url;
  }

  pendientes(): Observable<AutorizacionProformaDto[]> {
    return this.http.get<AutorizacionProformaDto[]>(`${this.url}/autorizaciones/pendientes`);
  }

  vencidas(): Observable<AutorizacionProformaDto[]> {
    return this.http.get<AutorizacionProformaDto[]>(`${this.url}/autorizaciones/vencidas`);
  }

  aprobar(idProforma: number, motivo: string): Observable<any> {
    return this.http.post<any>(`${this.url}/autorizaciones/${idProforma}/aprobar`, { motivo });
  }

  rechazar(idProforma: number, motivo: string): Observable<any> {
    return this.http.post<any>(`${this.url}/autorizaciones/${idProforma}/rechazar`, { motivo });
  }

  autorizarVencida(idProforma: number, motivo: string): Observable<any> {
    return this.http.post<any>(`${this.url}/autorizaciones/${idProforma}/autorizar-vencida`, { motivo });
  }
}
