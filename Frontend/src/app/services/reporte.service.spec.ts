import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { ReporteService } from './reporte.service';
import { global } from './global';

describe('ReporteService - mayoristas', () => {
  let service: ReporteService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(ReporteService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('clientes por ruta llama a su endpoint con la ruta y el formato', () => {
    service.generar('MAYORISTAS_CLIENTES_RUTA', { idRuta: 3, formato: 'XLSX' }).subscribe();

    const req = http.expectOne(r => r.url === `${global.url}/reportes/mayoristas/clientes-ruta`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('idRuta')).toBe('3');
    expect(req.request.params.get('formato')).toBe('XLSX');
    req.flush(new Blob());
  });

  it('sin compras y proformas por vendedor envían el período y omiten la ruta cuando son todas', () => {
    service.generar('MAYORISTAS_SIN_COMPRAS', { fechaInicio: '2026-10-01', fechaFin: '2026-10-31', formato: 'PDF' }).subscribe();
    service.generar('MAYORISTAS_PROFORMAS_VENDEDOR',
      { fechaInicio: '2026-10-01', fechaFin: '2026-10-31', idRuta: null, formato: 'PDF' }).subscribe();

    const sinCompras = http.expectOne(r => r.url === `${global.url}/reportes/mayoristas/clientes-sin-compras`);
    expect(sinCompras.request.params.get('fechaInicio')).toBe('2026-10-01');
    expect(sinCompras.request.params.get('fechaFin')).toBe('2026-10-31');
    expect(sinCompras.request.params.has('idRuta')).toBeFalse();
    sinCompras.flush(new Blob());

    const vendedores = http.expectOne(r => r.url === `${global.url}/reportes/mayoristas/proformas-vendedor`);
    expect(vendedores.request.params.has('idRuta')).toBeFalse();
    vendedores.flush(new Blob());
  });

  it('las rutas del selector llevan el vendedor como detalle', () => {
    let opciones: any[];
    service.listarRutasSelector().subscribe(resultado => opciones = resultado);

    http.expectOne(`${global.url}/rutas/activas`).flush([
      { idRuta: 1, nombre: 'RUTA 1', vendedor: 'Ana Lopez' },
      { idRuta: 3, nombre: 'RUTA 3', vendedor: null }
    ]);

    expect(opciones).toEqual([
      { valor: 1, etiqueta: 'RUTA 1', detalle: 'Ana Lopez' },
      { valor: 3, etiqueta: 'RUTA 3', detalle: 'Sin vendedor' }
    ]);
  });

  it('ventas entregadas envía período, ruta y vendedor', () => {
    service.generar('MAYORISTAS_ENTREGADO',
      { fechaInicio: '2026-10-01', fechaFin: '2026-10-31', idRuta: 3, idUsuario: 2, formato: 'XLSX' }).subscribe();

    const req = http.expectOne(r => r.url === `${global.url}/reportes/mayoristas/entregado`);
    expect(req.request.params.get('idRuta')).toBe('3');
    expect(req.request.params.get('idUsuario')).toBe('2');
    expect(req.request.params.get('formato')).toBe('XLSX');
    req.flush(new Blob());
  });

  it('los listados de control usan sus propias rutas y mandan el estado y el formato', () => {
    service.generar('MAYORISTAS_CHEQUES',
      { fechaInicio: '2026-11-01', fechaFin: '2026-11-30', estado: 'RECIBIDO', formato: 'XLSX' }).subscribe();
    const cheques = http.expectOne(r => r.url === `${global.url}/reportes/controles/cheques`);
    expect(cheques.request.params.get('estado')).toBe('RECIBIDO');
    expect(cheques.request.params.get('formato')).toBe('XLSX');
    cheques.flush(new Blob());

    service.generar('MAYORISTAS_DESPACHADOS_CREDITO', { fechaInicio: '2026-11-01', fechaFin: '2026-11-30', idRuta: 3 }).subscribe();
    const credito = http.expectOne(r => r.url === `${global.url}/reportes/controles/despachados-credito`);
    expect(credito.request.params.get('idRuta')).toBe('3');
    credito.flush(new Blob());

    service.generar('MAYORISTAS_LISTADO_PROFORMAS', { fechaInicio: '2026-09-04', fechaFin: '2026-09-10' }).subscribe();
    http.expectOne(r => r.url === `${global.url}/reportes/controles/listado-proformas`).flush(new Blob());
  });
});
