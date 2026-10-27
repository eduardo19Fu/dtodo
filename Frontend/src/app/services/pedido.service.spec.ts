import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { PedidoService } from './pedido.service';
import { global } from './global';

describe('PedidoService', () => {
  let service: PedidoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(PedidoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lista con la etapa y las fechas solo cuando se indican', () => {
    service.listar().subscribe();
    const sinFiltros = http.expectOne(r => r.url === `${global.url}/pedidos`);
    expect(sinFiltros.request.params.keys().length).toBe(0);
    sinFiltros.flush([]);

    service.listar('EMPACADA', '2026-10-01', '2026-10-31').subscribe();
    const conFiltros = http.expectOne(r => r.url === `${global.url}/pedidos`);
    expect(conFiltros.request.params.get('estado')).toBe('EMPACADA');
    expect(conFiltros.request.params.get('fechaInicio')).toBe('2026-10-01');
    expect(conFiltros.request.params.get('fechaFin')).toBe('2026-10-31');
    conFiltros.flush([]);
  });

  it('avanza con el responsable y retrocede con el motivo', () => {
    service.avanzar(7, 'Irene Ortega').subscribe();
    const avanzar = http.expectOne(`${global.url}/pedidos/7/avanzar`);
    expect(avanzar.request.method).toBe('POST');
    expect(avanzar.request.body).toEqual({ responsable: 'Irene Ortega', idRepartidor: null });

    service.avanzar(8).subscribe();
    expect(http.expectOne(`${global.url}/pedidos/8/avanzar`).request.body).toEqual({ responsable: null, idRepartidor: null });

    service.retroceder(7, 'faltó un producto').subscribe();
    expect(http.expectOne(`${global.url}/pedidos/7/retroceder`).request.body).toEqual({ motivo: 'faltó un producto' });
  });

  it('pide los documentos como archivo', () => {
    service.picking(7).subscribe();
    service.empaque(7).subscribe();
    service.notaEnvio(3, '2026-10-28').subscribe();

    expect(http.expectOne(`${global.url}/pedidos/7/picking`).request.responseType).toBe('blob');
    expect(http.expectOne(`${global.url}/pedidos/7/empaque`).request.responseType).toBe('blob');
    const nota = http.expectOne(r => r.url === `${global.url}/pedidos/nota-envio`);
    expect(nota.request.params.get('idRuta')).toBe('3');
    expect(nota.request.params.get('fecha')).toBe('2026-10-28');
    expect(nota.request.responseType).toBe('blob');
  });

  it('registra, anula y cierra una entrega, y pide las sucursales', () => {
    service.registrarEntrega(7, { idSucursal: 1, descontadoAMano: false, lineas: [{ idDetalle: 1, cantidad: 4 }] }).subscribe();
    const registro = http.expectOne(`${global.url}/pedidos/7/entregas`);
    expect(registro.request.method).toBe('POST');
    expect(registro.request.body.lineas).toEqual([{ idDetalle: 1, cantidad: 4 }]);

    service.anularEntrega(7, 50, 'se registró de más').subscribe();
    expect(http.expectOne(`${global.url}/pedidos/7/entregas/50/anular`).request.body).toEqual({ motivo: 'se registró de más' });

    service.cerrarConLoEntregado(7, 'ya no se llevará').subscribe();
    expect(http.expectOne(`${global.url}/pedidos/7/cerrar`).request.body).toEqual({ motivo: 'ya no se llevará' });

    service.sucursales().subscribe();
    expect(http.expectOne(`${global.url}/pedidos/sucursales`).request.method).toBe('GET');
  });

  it('avanza enviando el piloto, lista los repartidores y cambia el piloto', () => {
    service.avanzar(7, undefined, 21).subscribe();
    expect(http.expectOne(`${global.url}/pedidos/7/avanzar`).request.body).toEqual({ responsable: null, idRepartidor: 21 });

    service.repartidores().subscribe();
    expect(http.expectOne(`${global.url}/pedidos/repartidores`).request.method).toBe('GET');

    service.asignarRepartidor(7, 22).subscribe();
    expect(http.expectOne(`${global.url}/pedidos/7/repartidor`).request.body).toEqual({ idRepartidor: 22 });
  });
});
