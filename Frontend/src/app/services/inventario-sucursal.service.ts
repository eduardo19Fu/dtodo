import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { global } from './global';
import swal from 'sweetalert2';

@Injectable({
  providedIn: 'root'
})
export class InventarioSucursalService {

  private url: string;

  constructor(private http: HttpClient) {
    this.url = global.url;
  }

  ajustarStock(idSucursal: number, idProducto: number, stock: number, stockMinimo: number): Observable<any> {
    return this.http.put<any>(`${this.url}/inventario-sucursal/${idSucursal}/${idProducto}`, { stock, stockMinimo }).pipe(
      catchError(e => {
        swal.fire(e.error.status, e.error.message, 'error');
        return throwError(e);
      })
    );
  }
}
