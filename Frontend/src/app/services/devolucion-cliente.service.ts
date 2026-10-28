import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { CrearDevolucionClienteDto, DevolucionClienteDto, PedidoDevolubleDto } from '../dtos/devolucion-cliente-dto';
import { global } from './global';

/**
 * Devoluciones de clientes sobre pedidos mayoristas (solo administrador). No muestra alertas: las reglas de negocio llegan como
 * 422 con el mensaje en {@code error.error.message} y las muestra la pantalla.
 */
@Injectable({
  providedIn: 'root'
})
export class DevolucionClienteService {

  private url: string;

  constructor(private http: HttpClient) {
    this.url = `${global.url}/devoluciones-cliente`;
  }

  getListado(page: number, size: number, filtros: { idCliente?: number; desde?: string; hasta?: string }): Observable<any> {
    let params = new HttpParams().set('size', size.toString());
    if (filtros.idCliente) {
      params = params.set('idCliente', filtros.idCliente.toString());
    }
    if (filtros.desde) {
      params = params.set('desde', filtros.desde);
    }
    if (filtros.hasta) {
      params = params.set('hasta', filtros.hasta);
    }
    return this.http.get(`${this.url}/page/${page}`, { params });
  }

  buscar(idDevolucion: number): Observable<DevolucionClienteDto> {
    return this.http.get<DevolucionClienteDto>(`${this.url}/${idDevolucion}`);
  }

  pedidosDevolubles(idCliente: number): Observable<PedidoDevolubleDto[]> {
    return this.http.get<PedidoDevolubleDto[]>(`${this.url}/clientes/${idCliente}/pedidos`);
  }

  registrar(devolucion: CrearDevolucionClienteDto): Observable<DevolucionClienteDto> {
    return this.http.post<DevolucionClienteDto>(this.url, devolucion);
  }
}
