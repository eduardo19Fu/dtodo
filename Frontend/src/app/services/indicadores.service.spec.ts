import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { global } from './global';
import { IndicadoresService } from './indicadores.service';

describe('IndicadoresService', () => {
  let servicio: IndicadoresService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    servicio = TestBed.inject(IndicadoresService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pide los indicadores del año y mes indicados', () => {
    servicio.mes(2026, 10).subscribe();

    const req = http.expectOne(r => r.url === `${global.url}/indicadores`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('anio')).toBe('2026');
    expect(req.request.params.get('mes')).toBe('10');
    req.flush({});
  });
});
