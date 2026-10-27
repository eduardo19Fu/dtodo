import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { global } from './global';
import { ComisionService } from './comision.service';

describe('ComisionService', () => {
  let servicio: ComisionService;
  let http: HttpTestingController;
  const base = `${global.url}/comisiones`;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    servicio = TestBed.inject(ComisionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lee el avance del año', () => {
    servicio.avance(2026).subscribe();
    const req = http.expectOne(r => r.url === `${base}/avance`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('anio')).toBe('2026');
    req.flush({});
  });

  it('guarda las metas reemplazando las del año', () => {
    servicio.guardarMetas(2026, [{ montoMinimo: 100, porcentaje: 1 }]).subscribe();
    const req = http.expectOne(r => r.url === `${base}/metas`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.params.get('anio')).toBe('2026');
    expect(req.request.body).toEqual([{ montoMinimo: 100, porcentaje: 1 }]);
    req.flush([]);
  });

  it('copia las metas de un año a otro', () => {
    servicio.copiarMetas(2026, 2027).subscribe();
    const req = http.expectOne(r => r.url === `${base}/metas/copiar`);
    expect(req.request.method).toBe('POST');
    expect(req.request.params.get('desde')).toBe('2026');
    expect(req.request.params.get('hacia')).toBe('2027');
    req.flush([]);
  });

  it('liquida con las decisiones y deshace con el motivo', () => {
    servicio.liquidar(2026, [{ idVendedor: 1, completoAnio: true }]).subscribe();
    const liquidar = http.expectOne(r => r.url === `${base}/liquidar`);
    expect(liquidar.request.body).toEqual({ decisiones: [{ idVendedor: 1, completoAnio: true }] });
    liquidar.flush({});

    servicio.deshacer(2026, 'error').subscribe();
    const deshacer = http.expectOne(r => r.url === `${base}/deshacer`);
    expect(deshacer.request.body).toEqual({ motivo: 'error' });
    deshacer.flush({});
  });

  it('pide el reporte como archivo', () => {
    servicio.reporte(2026, 'XLSX').subscribe();
    const req = http.expectOne(r => r.url === `${base}/reporte`);
    expect(req.request.params.get('formato')).toBe('XLSX');
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob());
  });
});
