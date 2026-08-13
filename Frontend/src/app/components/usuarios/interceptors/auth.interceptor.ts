import { Injectable } from '@angular/core';
import {
  HttpEvent, HttpInterceptor, HttpHandler, HttpRequest
} from '@angular/common/http';
import { Router } from '@angular/router';

import { Observable, throwError } from 'rxjs';
import { catchError, finalize, shareReplay, switchMap, tap } from 'rxjs/operators';
import swal from 'sweetalert2';

import { AuthService } from '../../../services/auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  private refreshRequest: Observable<any>;

  constructor(
    private auth: AuthService,
    private router: Router
  ) { }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError(error => {
        if (error.status === 401 && !this.isTokenEndpoint(req) && this.auth.refreshToken) {
          return this.handleUnauthorized(req, next);
        }

        if (error.status === 401 && !this.isTokenEndpoint(req)) {
          this.endSession();
        }

        if (error.status === 403) {
          swal.fire(
            'Acceso Denegado',
            'El usuario no cuenta con las credenciales correctas para acceder a este recurso',
            'warning'
          );
          this.router.navigate(['/home']);
        }

        return throwError(error);
      })
    );
  }

  private handleUnauthorized(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (!this.refreshRequest) {
      this.refreshRequest = this.auth.refreshAccessToken().pipe(
        tap(response => this.auth.guardarSesion(response.access_token, response.refresh_token)),
        catchError(error => {
          this.endSession();
          return throwError(error);
        }),
        finalize(() => this.refreshRequest = null),
        shareReplay(1)
      );
    }

    return this.refreshRequest.pipe(
      switchMap(response => next.handle(this.withBearerToken(req, response.access_token)))
    );
  }

  private withBearerToken(req: HttpRequest<any>, token: string): HttpRequest<any> {
    return req.clone({
      headers: req.headers.set('Authorization', 'Bearer ' + token)
    });
  }

  private isTokenEndpoint(req: HttpRequest<any>): boolean {
    return req.url.endsWith('/oauth/token');
  }

  private endSession(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
