import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { AutorizacionProformaService } from './autorizacion-proforma.service';
import { ProformaService } from './proformas/proforma.service';
import { AuthService } from './auth.service';
import { global } from './global';
import { Proforma } from '../models/proforma';

describe('AutorizacionProformaService', () => {
  let service: AutorizacionProformaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(AutorizacionProformaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('consulta las pendientes y las vencidas', () => {
    service.pendientes().subscribe();
    service.vencidas().subscribe();

    expect(http.expectOne(`${global.url}/autorizaciones/pendientes`).request.method).toBe('GET');
    expect(http.expectOne(`${global.url}/autorizaciones/vencidas`).request.method).toBe('GET');
  });

  it('aprueba, rechaza y autoriza la vencida enviando el motivo', () => {
    service.aprobar(7, 'cliente de volumen').subscribe();
    service.rechazar(8, 'margen insuficiente').subscribe();
    service.autorizarVencida(9, 'pagó el depósito').subscribe();

    const aprobar = http.expectOne(`${global.url}/autorizaciones/7/aprobar`);
    expect(aprobar.request.method).toBe('POST');
    expect(aprobar.request.body).toEqual({ motivo: 'cliente de volumen' });
    expect(http.expectOne(`${global.url}/autorizaciones/8/rechazar`).request.body).toEqual({ motivo: 'margen insuficiente' });
    expect(http.expectOne(`${global.url}/autorizaciones/9/autorizar-vencida`).request.body)
      .toEqual({ motivo: 'pagó el depósito' });
  });
});

describe('ProformaService - auditoría', () => {
  let service: ProformaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [{ provide: AuthService, useValue: { hasRole: () => true, usuario: null } }]
    });
    service = TestBed.inject(ProformaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('consulta el historial de la proforma', () => {
    service.getAuditoria(5).subscribe();

    expect(http.expectOne(`${global.url}/proformas/5/auditoria`).request.method).toBe('GET');
  });

  it('envía el motivo al editar y al eliminar, y no lo envía si no hay', () => {
    const proforma = Object.assign(new Proforma(), { idProforma: 5 });

    service.update(proforma, 'cambio de cantidad').subscribe();
    const conMotivo = http.expectOne(r => r.url === `${global.url}/proformas/5`);
    expect(conMotivo.request.method).toBe('PUT');
    expect(conMotivo.request.params.get('motivo')).toBe('cambio de cantidad');

    service.update(proforma).subscribe();
    expect(http.expectOne(r => r.url === `${global.url}/proformas/5`).request.params.has('motivo')).toBeFalse();

    service.delete(5, 'pedido duplicado').subscribe();
    const eliminar = http.expectOne(r => r.url === `${global.url}/proformas/5` && r.method === 'DELETE');
    expect(eliminar.request.params.get('motivo')).toBe('pedido duplicado');
  });
});
