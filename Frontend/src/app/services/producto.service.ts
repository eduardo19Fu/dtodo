import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent, HttpParams, HttpRequest, HttpResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { Producto } from '../models/producto';
import { MovimientoProducto } from '../models/movimiento-producto';

import { global } from './global';
import swal from 'sweetalert2';
import { ProductoDto } from '../dtos/productoDto';


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

  getProductosDto(): Observable<ProductoDto[]> {
    return this.http.get<ProductoDto[]>(this.url + '/productos/dto');
  }

  getProductosActivos(): Observable<ProductoDto[]> {
    return this.http.get<ProductoDto[]>(`${this.url}/productos-activos`);
  }

  getProductosDtoPaginados(page: number, size: number = 5,
                           orden: string = 'nombre', direccion: 'asc' | 'desc' = 'asc'): Observable<any> {
    const params = new HttpParams()
      .set('size', size.toString())
      .set('orden', orden)
      .set('direccion', direccion);
    return this.http.get<any>(`${this.url}/productos-dto/page/${page}`, {params}).pipe(
      catchError(e => {
        console.error(e);
        return throwError(e);
      })
    );
  }

  buscarProductosDto(page: number, filtro: string, size: number = 5,
                     orden: string = 'nombre', direccion: 'asc' | 'desc' = 'asc'): Observable<any> {
    const params = new HttpParams()
      .set('filtro', filtro)
      .set('size', size.toString())
      .set('orden', orden)
      .set('direccion', direccion);
    return this.http.get<any>(`${this.url}/productos-dto/search/${page}`, {params}).pipe(
      catchError(e => {
        console.error(e);
        return throwError(e);
      })
    );
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

  exportarProductosExcel(): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.url}/productos/excel`, {
      observe: 'response',
      responseType: 'blob'
    }).pipe(
      catchError(e => {
        console.error(e);
        return throwError(e);
      })
    );
  }


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
