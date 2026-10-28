import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { IndicadoresMesDto } from '../dtos/indicadores-dto';
import { global } from './global';

/** Indicadores del administrador: ventas por vendedor, pedidos entregados a tiempo y cuentas por cobrar. */
@Injectable({
  providedIn: 'root'
})
export class IndicadoresService {

  private url: string;

  constructor(private http: HttpClient) {
    this.url = `${global.url}/indicadores`;
  }

  mes(anio: number, mes: number): Observable<IndicadoresMesDto> {
    const params = new HttpParams().set('anio', String(anio)).set('mes', String(mes));
    return this.http.get<IndicadoresMesDto>(this.url, { params });
  }
}
