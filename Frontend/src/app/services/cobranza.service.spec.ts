import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CobranzaService } from './cobranza.service';
import { global } from './global';

describe('CobranzaService', () => {
  let servicio: CobranzaService;
  let http: HttpTestingController;
  const base = `${global.url}/cobranza`;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    servicio = TestBed.inject(CobranzaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('consulta las cuentas por cobrar', () => {
    servicio.cuentasPorCobrar().subscribe();

    expect(http.expectOne(`${base}/saldos`).request.method).toBe('GET');
  });

  it('pide el reporte con el formato y los filtros indicados y omite la ruta vacía', () => {
    servicio.reporte('XLSX', 3, true).subscribe();
    const conRuta = http.expectOne(r => r.url === `${base}/saldos/reporte`);
    expect(conRuta.request.params.get('formato')).toBe('XLSX');
    expect(conRuta.request.params.get('idRuta')).toBe('3');
    expect(conRuta.request.params.get('soloVencidos')).toBe('true');
    expect(conRuta.request.responseType).toBe('blob');
    conRuta.flush(new Blob());

    servicio.reporte('PDF', null, false).subscribe();
    const sinRuta = http.expectOne(r => r.url === `${base}/saldos/reporte`);
    expect(sinRuta.request.params.has('idRuta')).toBeFalse();
    sinRuta.flush(new Blob());
  });

  it('pide el estado de cuenta de un cliente como archivo', () => {
    servicio.estadoDeCuenta(5).subscribe();

    const req = http.expectOne(`${base}/clientes/5/estado-cuenta`);
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob());
  });
});
