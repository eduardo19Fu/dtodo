import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { PedidoSaldoDto, SaldoClienteDto } from '../dtos/saldo-dto';
import { global } from './global';

/**
 * Saldo de los clientes mayoristas y cobro de sus pedidos (administrador, cobrador y vendedor para lo suyo). No muestra
 * alertas: las reglas de negocio llegan como 422 con el mensaje en {@code error.error.message}.
 */
@Injectable({
  providedIn: 'root'
})
export class SaldoService {

  private url: string;

  constructor(private http: HttpClient) {
    this.url = `${global.url}/saldos`;
  }

  cliente(idCliente: number): Observable<SaldoClienteDto> {
    return this.http.get<SaldoClienteDto>(`${this.url}/clientes/${idCliente}`);
  }

  pedido(idProforma: number): Observable<PedidoSaldoDto> {
    return this.http.get<PedidoSaldoDto>(`${this.url}/pedidos/${idProforma}`);
  }
}
