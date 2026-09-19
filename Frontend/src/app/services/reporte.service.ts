import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ReporteFiltroDto } from '../dtos/reporte-filtro-dto';
import { UsuarioDto } from '../dtos/usuario-dto';
import { global } from './global';

@Injectable({ providedIn: 'root' })
export class ReporteService {
  private readonly url = `${global.url}/reportes`;

  constructor(private http: HttpClient) {}

  generar(codigo: string, filtros: ReporteFiltroDto): Observable<HttpResponse<Blob>> {
    const configuracion = this.configuracion(codigo);
    return this.http.get(`${this.url}/${configuracion.ruta}`, {
      params: this.construirParametros(filtros, configuracion.incluirUsuario),
      observe: 'response',
      responseType: 'blob'
    });
  }

  listarUsuariosProformas(): Observable<UsuarioDto[]> {
    return this.http.get<UsuarioDto[]>(`${this.url}/proformas/usuarios`);
  }

  obtenerNombreArchivo(response: HttpResponse<Blob>, respaldo: string): string {
    const disposition = response.headers.get('Content-Disposition') || '';
    const coincidencia = /filename\*?=(?:UTF-8''|["']?)([^;"']+)/i.exec(disposition);
    return coincidencia && coincidencia[1] ? decodeURIComponent(coincidencia[1].trim()) : respaldo;
  }

  private configuracion(codigo: string): { ruta: string; incluirUsuario: boolean } {
    switch (codigo) {
      case 'POLIZA_INDIVIDUAL':
        return { ruta: 'ventas/poliza-individual', incluirUsuario: true };
      case 'POLIZA_GENERAL':
        return { ruta: 'ventas/poliza-general', incluirUsuario: false };
      case 'MOVIMIENTOS_INVENTARIO':
        return { ruta: 'inventario/movimientos', incluirUsuario: false };
      case 'EXISTENCIAS':
        return { ruta: 'inventario/existencias', incluirUsuario: false };
      case 'PROFORMAS_EMITIDAS':
        return { ruta: 'proformas', incluirUsuario: true };
      default:
        throw new Error(`El reporte ${codigo} todavía no tiene un endpoint habilitado.`);
    }
  }

  private construirParametros(filtros: ReporteFiltroDto, incluirUsuario: boolean): HttpParams {
    let params = new HttpParams();
    if (filtros.fechaInicio) {
      params = params.set('fechaInicio', filtros.fechaInicio);
    }
    if (filtros.fechaFin) {
      params = params.set('fechaFin', filtros.fechaFin);
    }
    if (filtros.idSucursal) {
      params = params.set('idSucursal', filtros.idSucursal.toString());
    }
    if (incluirUsuario && filtros.idUsuario) {
      params = params.set('idUsuario', filtros.idUsuario.toString());
    }
    return params;
  }
}
