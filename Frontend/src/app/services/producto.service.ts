import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent, HttpRequest } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { Producto } from '../models/producto';
import { MovimientoProducto } from '../models/movimiento-producto';

import { global } from './global';
import swal from 'sweetalert2';


@Injectable({
  providedIn: 'root'
})
export class ProductoService {

  private url: string;

  constructor(
    private http: HttpClient
  ) {
    this.url = global.url;
  }

  getProductos(): Observable<Producto[]> {
    return this.http.get<Producto[]>(this.url + '/productos');
  }

  getProductosActivos(): Observable<Producto[]> {
    return this.http.get<Producto[]>(`${this.url}/productos-activos`);
  }

  getProductosPaginados(page: number): Observable<any> {
    return this.http.get(`${this.url}/productos/page/${page}`).pipe(
      map((response: any) => {
        (response.content as Producto[]).map(producto => {
          producto.nombre = producto.nombre.toUpperCase();
          return producto;
        });
        return response;
      })
    );
  }

  getProducto(id: number): Observable<Producto> {
    return this.http.get<Producto>(`${this.url}/productos/${id}`).pipe(
      catchError(e => {
        swal.fire('Error al consultar el producto', e.error.mensaje, 'error');
        return throwError(e);
      })
    );
  }

  getProductoByCode(codigo: string): Observable<Producto> {
    return this.http.get<Producto>(`${this.url}/productos/codigo/${codigo}`).pipe(
      catchError(e => {
        swal.fire('Error al consultar el producto', e.error, 'error');
        return throwError(e);
      })
    );
  }


  getProductosByNombre(nombre: string): Observable<Producto[]> {
    return this.http.get<Producto[]>(`${this.url}/productos/name/${nombre}`);
  }

  getTotalProductos(): Observable<any> {
    return this.http.get<any>(`${this.url}/productos/cantidad-productos`).pipe(
      catchError(e => {
        swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }

  create(producto: Producto): Observable<any> {
    return this.http.post<any>(`${this.url}/productos`, producto).pipe(
      catchError(e => {
        swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }

  update(producto: Producto): Observable<any> {
    return this.http.put<any>(`${this.url}/productos`, producto).pipe(
      catchError(e => {
        swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }

  // Código modificado para agregar barra de progreso
  uploadImage(archivo: File, id): Observable<HttpEvent<{}>> {
    const formData = new FormData();

    formData.append('file', archivo); // primer parametro es el identificador del request en el backend
    formData.append('id', id);

    const req = new HttpRequest('POST', `${this.url}/productos/upload`, formData, {
      reportProgress: true
    });

    return this.http.request(req);
  }

  /********* SERVICIO DE MOVIMIENTOS PRODUCTO **********/

  getMovimientos(idproducto: number, page: number): Observable<MovimientoProducto> {
    return this.http.get<MovimientoProducto>(`${this.url}/productos/movimientos/${idproducto}/${page}`).pipe(
      map((response: any) => {
        (response.content as MovimientoProducto[]).map(movimientoProducto => {
          return movimientoProducto;
        });
        return response;
      })
    );
  }

  /******** SERVICIO DE REPORTES **********/


  // Código original de subida de imagenes para productos
  /*uploadImage(archivo: File, id): Observable<Producto>{
    let formData = new FormData();

    formData.append('file', archivo); // primer parametro es el identificador del request en el backend
    formData.append('id', id);

    return this.http.post(`${this.url}/productos/upload`, formData).pipe(
      map((response: any) => response.producto as Producto),
      catchError(e => {
        swal.fire(e.error.mensaje, e.error.error, 'error');
        return throwError(e);
      })
    );
  }*/
}
