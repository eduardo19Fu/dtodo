import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DevolucionClienteService } from './devolucion-cliente.service';
import { global } from './global';

describe('DevolucionClienteService', () => {
  let servicio: DevolucionClienteService;
  let http: HttpTestingController;
  const base = `${global.url}/devoluciones-cliente`;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    servicio = TestBed.inject(DevolucionClienteService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lista con los filtros indicados y omite los vacíos', () => {
    servicio.getListado(1, 10, { desde: '2026-10-01', hasta: '2026-10-31' }).subscribe();

    const req = http.expectOne(r => r.url === `${base}/page/1`);
    expect(req.request.params.get('size')).toBe('10');
    expect(req.request.params.get('desde')).toBe('2026-10-01');
    expect(req.request.params.has('idCliente')).toBeFalse();
    req.flush({});
  });

  it('consulta los pedidos devolubles de un cliente y el detalle de una devolución', () => {
    servicio.pedidosDevolubles(5).subscribe();
    expect(http.expectOne(`${base}/clientes/5/pedidos`).request.method).toBe('GET');

    servicio.buscar(9).subscribe();
    expect(http.expectOne(`${base}/9`).request.method).toBe('GET');
  });

  it('registra la devolución con lo que se manda', () => {
    servicio.registrar({ idProforma: 7, motivo: 'dañado', lineas: [{ idDetalle: 1, cantidad: 2, destino: 'REINGRESO' }] }).subscribe();

    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.lineas[0].destino).toBe('REINGRESO');
    req.flush({});
  });
});
