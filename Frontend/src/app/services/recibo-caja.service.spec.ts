import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { global } from './global';
import { ReciboCajaService } from './recibo-caja.service';

describe('ReciboCajaService', () => {
  let servicio: ReciboCajaService;
  let http: HttpTestingController;
  const base = `${global.url}/recibos`;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    servicio = TestBed.inject(ReciboCajaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lista con los filtros que se indiquen y omite los vacíos', () => {
    servicio.getListado(2, 10, { idCliente: 5, desde: '2026-10-01', hasta: '2026-10-31', incluirAnulados: true }).subscribe();

    const req = http.expectOne(r => r.url === `${base}/page/2`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('size')).toBe('10');
    expect(req.request.params.get('idCliente')).toBe('5');
    expect(req.request.params.get('desde')).toBe('2026-10-01');
    expect(req.request.params.get('incluirAnulados')).toBe('true');
    expect(req.request.params.has('idSucursal')).toBeFalse();
    req.flush({});
  });

  it('emite, anula e imprime', () => {
    servicio.emitir({ idCliente: 5, monto: 100, formaPago: 'EFECTIVO' }).subscribe();
    const emitir = http.expectOne(base);
    expect(emitir.request.method).toBe('POST');
    expect(emitir.request.body.monto).toBe(100);
    emitir.flush({});

    servicio.anular(9, 'error').subscribe();
    const anular = http.expectOne(`${base}/9/anular`);
    expect(anular.request.body).toEqual({ motivo: 'error' });
    anular.flush({});

    servicio.imprimir(9).subscribe();
    const imprimir = http.expectOne(`${base}/9/imprimir`);
    expect(imprimir.request.responseType).toBe('blob');
    imprimir.flush(new Blob());
  });
});
