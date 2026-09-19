import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { Pais } from '../models/pais';

import { global } from './global';
import swal from 'sweetalert2';

@Injectable({
  providedIn: 'root'
})
export class PaisService {

  private url: string;

  constructor(private http: HttpClient) {
    this.url = global.url;
  }

  getPaises(): Observable<Pais[]> {
    return this.http.get<Pais[]>(`${this.url}/paises`);
  }

  create(pais: Pais): Observable<Pais> {
    return this.http.post<Pais>(`${this.url}/paises`, pais).pipe(
      catchError(e => {
        swal.fire(e.error.status, e.error.message, 'error');
        return throwError(e);
      })
    );
  }
}
