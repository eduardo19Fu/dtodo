import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { global } from './global';
import { SaldoService } from './saldo.service';

describe('SaldoService', () => {
  let servicio: SaldoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    servicio = TestBed.inject(SaldoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('consulta el saldo del cliente y el cobro de un pedido', () => {
    servicio.cliente(5).subscribe();
    const cliente = http.expectOne(`${global.url}/saldos/clientes/5`);
    expect(cliente.request.method).toBe('GET');
    cliente.flush({});

    servicio.pedido(9).subscribe();
    const pedido = http.expectOne(`${global.url}/saldos/pedidos/9`);
    expect(pedido.request.method).toBe('GET');
    pedido.flush({});
  });
});
