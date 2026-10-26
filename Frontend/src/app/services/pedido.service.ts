import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { EstadoPedido, PedidoDetalleDto, PedidoDto } from '../dtos/pedido-dto';
import { global } from './global';

/**
 * Pedidos mayoristas: lista, etapas y documentos de bodega. No muestra alertas: las reglas de negocio llegan como 422
 * con el mensaje en {@code error.error.message} y las muestra cada pantalla.
 */
@Injectable({
  providedIn: 'root'
})
export class PedidoService {

  private url: string;

  constructor(private http: HttpClient) {
    this.url = `${global.url}/pedidos`;
  }

  /** Sin {@code estado} trae los pedidos por entregar. Las fechas, opcionales, filtran por emisión (yyyy-MM-dd). */
  listar(estado?: EstadoPedido | '', fechaInicio?: string, fechaFin?: string): Observable<PedidoDto[]> {
    let params = new HttpParams();
    if (estado) {
      params = params.set('estado', estado);
    }
    if (fechaInicio) {
      params = params.set('fechaInicio', fechaInicio);
    }
    if (fechaFin) {
      params = params.set('fechaFin', fechaFin);
    }
    return this.http.get<PedidoDto[]>(this.url, { params });
  }

  detalle(idProforma: number): Observable<PedidoDetalleDto> {
    return this.http.get<PedidoDetalleDto>(`${this.url}/${idProforma}`);
  }

  /** Pasa el pedido a la etapa siguiente; {@code responsable} es el nombre de quien lo hizo si no tiene usuario. */
  avanzar(idProforma: number, responsable?: string): Observable<PedidoDetalleDto> {
    return this.http.post<PedidoDetalleDto>(`${this.url}/${idProforma}/avanzar`, { responsable: responsable || null });
  }

  /** Solo administrador; el motivo es obligatorio. */
  retroceder(idProforma: number, motivo: string): Observable<PedidoDetalleDto> {
    return this.http.post<PedidoDetalleDto>(`${this.url}/${idProforma}/retroceder`, { motivo });
  }

  picking(idProforma: number): Observable<Blob> {
    return this.http.get(`${this.url}/${idProforma}/picking`, { responseType: 'blob' });
  }

  empaque(idProforma: number): Observable<Blob> {
    return this.http.get(`${this.url}/${idProforma}/empaque`, { responseType: 'blob' });
  }

  /** Pedidos de la ruta empacados por cargar y los que salieron ese día (hoy si no se indica la fecha). */
  notaEnvio(idRuta: number, fecha?: string): Observable<Blob> {
    let params = new HttpParams().set('idRuta', idRuta.toString());
    if (fecha) {
      params = params.set('fecha', fecha);
    }
    return this.http.get(`${this.url}/nota-envio`, { params, responseType: 'blob' });
  }
}
