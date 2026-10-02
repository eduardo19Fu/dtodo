import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { DespachoBodegaService } from './despacho-bodega.service';
import { global } from './global';
import { DespachoBodegaRequest } from '../dtos/despacho-bodega-request';
import swal from 'sweetalert2';

describe('DespachoBodegaService', () => {
  let service: DespachoBodegaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(DespachoBodegaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('consulta el listado con los filtros opcionales solo cuando vienen informados', () => {
    service.getListado(0, 5, '').subscribe();
    const simple = http.expectOne(r => r.url === `${global.url}/despachos-bodega/listado`);
    expect(simple.request.params.has('idBodega')).toBeFalse();
    expect(simple.request.params.has('estado')).toBeFalse();
    simple.flush({ content: [] });

    service.getListado(1, 5, 'maria', 3, 'PENDIENTE').subscribe();
    const filtrado = http.expectOne(r => r.url === `${global.url}/despachos-bodega/listado`);
    expect(filtrado.request.params.get('idBodega')).toBe('3');
    expect(filtrado.request.params.get('estado')).toBe('PENDIENTE');
    expect(filtrado.request.params.get('filtro')).toBe('maria');
    filtrado.flush({ content: [] });
  });

  it('los despachos pendientes salen de la paginación filtrada por estado', () => {
    let total: number;
    service.getTotalPendientes().subscribe(valor => total = valor);

    const req = http.expectOne(r => r.url === `${global.url}/despachos-bodega/listado`);
    expect(req.request.params.get('estado')).toBe('PENDIENTE');
    expect(req.request.params.get('size')).toBe('1');
    req.flush({ content: [], totalElements: 4 });

    expect(total).toBe(4);
  });

  it('registra un despacho enviando solo identificadores y cantidades', () => {
    const request: DespachoBodegaRequest = {
      idBodega: 1, idSucursalDestino: 2, recibidoPor: 'Maria', observaciones: '',
      items: [{ idProducto: 9, cantidad: 3 }]
    };
    service.create(request).subscribe();

    const req = http.expectOne(`${global.url}/despachos-bodega`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({ idDespacho: 5 });
  });

  it('aprueba un despacho', () => {
    service.aprobar(5).subscribe();

    const req = http.expectOne(`${global.url}/despachos-bodega/5/aprobar`);
    expect(req.request.method).toBe('PUT');
    req.flush({});
  });

  it('cancela un despacho con su motivo', () => {
    service.cancelar(5, 'Error de captura').subscribe();

    const req = http.expectOne(`${global.url}/despachos-bodega/5/cancelar`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ motivo: 'Error de captura' });
    req.flush({});
  });

  it('abre el comprobante PDF en una pestaña nueva y libera la URL después', () => {
    const abrir = spyOn(window, 'open');
    spyOn(window.URL, 'createObjectURL').and.returnValue('blob:comprobante');
    spyOn(window.URL, 'revokeObjectURL');
    const temporizador = spyOn(window, 'setTimeout').and.returnValue(0 as any);

    service.imprimirComprobante(5).subscribe();
    const req = http.expectOne(`${global.url}/despachos-bodega/5/comprobante`);
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['%PDF'], { type: 'application/pdf' }));

    expect(abrir).toHaveBeenCalledWith('blob:comprobante', '_blank');
    expect(temporizador).toHaveBeenCalled();
  });

  it('muestra el mensaje del servidor cuando no se puede aprobar', () => {
    const alerta = spyOn(swal, 'fire');
    let fallo: any;
    service.aprobar(5).subscribe({ error: e => fallo = e });

    http.expectOne(`${global.url}/despachos-bodega/5/aprobar`)
      .flush({ message: 'El despacho ya fue aprobado' }, { status: 400, statusText: 'Bad Request' });

    expect(fallo.status).toBe(400);
    expect(alerta).toHaveBeenCalledWith('No se pudo aprobar el despacho', 'El despacho ya fue aprobado', 'error');
  });
});
