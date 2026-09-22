import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ReporteFiltroDto } from '../dtos/reporte-filtro-dto';
import { ReporteSelectorOpcionDto } from '../dtos/reporte-selector-opcion-dto';
import { global } from './global';

@Injectable({ providedIn: 'root' })
export class ReporteService {
  private readonly url = `${global.url}/reportes`;

  constructor(private http: HttpClient) {}

  generar(codigo: string, filtros: ReporteFiltroDto): Observable<HttpResponse<Blob>> {
    const configuracion = this.configuracion(codigo);
    return this.http.get(`${this.url}/${configuracion.ruta}`, {
      params: this.construirParametros(filtros, configuracion.incluirUsuario, configuracion.incluirOpciones),
      observe: 'response',
      responseType: 'blob'
    });
  }

  listarSucursalesSelector(): Observable<ReporteSelectorOpcionDto[]> {
    return this.http.get<ReporteSelectorOpcionDto[]>(`${this.url}/filtros/sucursales`);
  }

  listarCajerosSelector(idSucursal?: number): Observable<ReporteSelectorOpcionDto[]> {
    const params = idSucursal ? new HttpParams().set('idSucursal', idSucursal.toString()) : undefined;
    return this.http.get<ReporteSelectorOpcionDto[]>(`${this.url}/filtros/usuarios/cajeros`, { params });
  }

  listarUsuariosProformasSelector(): Observable<ReporteSelectorOpcionDto[]> {
    return this.http.get<ReporteSelectorOpcionDto[]>(`${this.url}/filtros/usuarios/proformas`);
  }

  listarCategoriasSelector(): Observable<ReporteSelectorOpcionDto[]> {
    return this.http.get<ReporteSelectorOpcionDto[]>(`${this.url}/filtros/categorias`);
  }

  listarClientesSelector(): Observable<ReporteSelectorOpcionDto[]> {
    return this.http.get<ReporteSelectorOpcionDto[]>(`${this.url}/filtros/clientes`);
  }

  listarProveedoresSelector(): Observable<ReporteSelectorOpcionDto[]> {
    return this.http.get<ReporteSelectorOpcionDto[]>(`${this.url}/filtros/proveedores`);
  }

  listarProductosSelector(idSucursal: number): Observable<ReporteSelectorOpcionDto[]> {
    const params = new HttpParams().set('idSucursal', idSucursal.toString());
    return this.http.get<ReporteSelectorOpcionDto[]>(`${this.url}/filtros/productos`, { params });
  }

  obtenerNombreArchivo(response: HttpResponse<Blob>, respaldo: string): string {
    const disposition = response.headers.get('Content-Disposition') || '';
    const coincidencia = /filename\*?=(?:UTF-8''|["']?)([^;"']+)/i.exec(disposition);
    return coincidencia && coincidencia[1] ? decodeURIComponent(coincidencia[1].trim()) : respaldo;
  }

  private configuracion(codigo: string): { ruta: string; incluirUsuario: boolean; incluirOpciones?: boolean } {
    switch (codigo) {
      case 'POLIZA_INDIVIDUAL':
        return { ruta: 'ventas/poliza-individual', incluirUsuario: true };
      case 'POLIZA_GENERAL':
        return { ruta: 'ventas/poliza-general', incluirUsuario: false };
      case 'VENTAS_PRODUCTO':
        return { ruta: 'ventas/productos', incluirUsuario: false, incluirOpciones: true };
      case 'VENTAS_CLIENTE':
        return { ruta: 'ventas/clientes', incluirUsuario: false, incluirOpciones: true };
      case 'RENTABILIDAD_PRODUCTO':
        return { ruta: 'ventas/rentabilidad-productos', incluirUsuario: false, incluirOpciones: true };
      case 'MOVIMIENTOS_INVENTARIO':
        return { ruta: 'inventario/movimientos', incluirUsuario: false, incluirOpciones: true };
      case 'EXISTENCIAS':
        return { ruta: 'inventario/existencias', incluirUsuario: false };
      case 'BAJO_STOCK':
        return { ruta: 'inventario/bajo-stock', incluirUsuario: false, incluirOpciones: true };
      case 'KARDEX':
        return { ruta: 'inventario/kardex', incluirUsuario: false, incluirOpciones: true };
      case 'PROFORMAS_EMITIDAS':
        return { ruta: 'proformas', incluirUsuario: true };
      case 'RESUMEN_NOTAS':
        return { ruta: 'notas-credito/resumen', incluirUsuario: false, incluirOpciones: true };
      case 'COMPRAS_PERIODO':
        return { ruta: 'compras', incluirUsuario: false, incluirOpciones: true };
      default:
        throw new Error(`El reporte ${codigo} todavía no tiene un endpoint habilitado.`);
    }
  }

  private construirParametros(
    filtros: ReporteFiltroDto,
    incluirUsuario: boolean,
    incluirOpciones = false
  ): HttpParams {
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
    if (incluirOpciones && filtros.estado) {
      params = params.set('estado', filtros.estado);
    }
    if (incluirOpciones && filtros.idProveedor) {
      params = params.set('idProveedor', filtros.idProveedor.toString());
    }
    if (incluirOpciones && filtros.idCategoria) {
      params = params.set('idCategoria', filtros.idCategoria.toString());
    }
    if (incluirOpciones && filtros.idCliente) {
      params = params.set('idCliente', filtros.idCliente.toString());
    }
    if (incluirOpciones && filtros.idProducto) {
      params = params.set('idProducto', filtros.idProducto.toString());
    }
    if (incluirOpciones && filtros.formato) {
      params = params.set('formato', filtros.formato);
    }
    return params;
  }
}
