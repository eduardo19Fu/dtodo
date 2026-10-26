import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Cliente } from '../models/cliente';
import { Ruta } from '../models/ruta';
import { RutaDto } from '../dtos/ruta-dto';
import { global } from './global';

/**
 * Acceso a las rutas de venta. No muestra alertas: las reglas de negocio llegan como 422 con el mensaje en
 * {@code error.error.message} y las muestra cada pantalla.
 */
@Injectable({
  providedIn: 'root'
})
export class RutaService {

  private url: string;

  constructor(private http: HttpClient) {
    this.url = global.url;
  }

  getListado(page: number, size: number, filtro: string, orden: string, direccion: string): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('filtro', filtro || '')
      .set('orden', orden)
      .set('direccion', direccion);
    return this.http.get(`${this.url}/rutas/listado`, { params });
  }

  /** Rutas activas, para los selectores. */
  getActivas(): Observable<RutaDto[]> {
    return this.http.get<RutaDto[]>(`${this.url}/rutas/activas`);
  }

  getRuta(id: number): Observable<RutaDto> {
    return this.http.get<RutaDto>(`${this.url}/rutas/${id}`);
  }

  getClientes(id: number): Observable<Cliente[]> {
    return this.http.get<Cliente[]>(`${this.url}/rutas/${id}/clientes`);
  }

  create(ruta: Ruta): Observable<RutaDto> {
    return this.http.post<RutaDto>(`${this.url}/rutas`, ruta);
  }

  update(idRuta: number, ruta: Ruta): Observable<RutaDto> {
    return this.http.put<RutaDto>(`${this.url}/rutas/${idRuta}`, ruta);
  }
}
