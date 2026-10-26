import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  CategoriaNiveles, GuardarDescuentosProducto, OpcionesDescuento, ProductoDescuentos
} from '../dtos/descuento-dto';
import { CategoriaCliente } from '../models/cliente';
import { global } from './global';

/**
 * Descuentos de los clientes mayoristas. No muestra alertas: las reglas de negocio llegan como 422 con el
 * mensaje en {@code error.error.message} y las muestra cada pantalla.
 */
@Injectable({
  providedIn: 'root'
})
export class DescuentoService {

  private url: string;

  constructor(private http: HttpClient) {
    this.url = global.url;
  }

  /** Opciones de descuento de un producto para un cliente y una cantidad, con la sugerencia del sistema. */
  getOpciones(idProducto: number, idCliente: number | null, cantidad: number): Observable<OpcionesDescuento> {
    let params = new HttpParams().set('idProducto', idProducto.toString()).set('cantidad', cantidad.toString());
    if (idCliente) {
      params = params.set('idCliente', idCliente.toString());
    }
    return this.http.get<OpcionesDescuento>(`${this.url}/descuentos/opciones`, { params });
  }

  getCategorias(): Observable<CategoriaNiveles[]> {
    return this.http.get<CategoriaNiveles[]>(`${this.url}/descuentos/categorias`);
  }

  guardarCategoria(categoria: CategoriaCliente, porcentajes: number[]): Observable<CategoriaNiveles> {
    return this.http.put<CategoriaNiveles>(`${this.url}/descuentos/categorias/${categoria}`, porcentajes);
  }

  getProductos(page: number, size: number, filtro: string, soloConDescuentos: boolean): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('filtro', filtro || '')
      .set('soloConDescuentos', String(soloConDescuentos));
    return this.http.get(`${this.url}/descuentos/productos`, { params });
  }

  getProducto(idProducto: number): Observable<ProductoDescuentos> {
    return this.http.get<ProductoDescuentos>(`${this.url}/descuentos/productos/${idProducto}`);
  }

  guardarProducto(idProducto: number, solicitud: GuardarDescuentosProducto): Observable<ProductoDescuentos> {
    return this.http.put<ProductoDescuentos>(`${this.url}/descuentos/productos/${idProducto}`, solicitud);
  }
}
