import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { DevolucionBodegaService } from './devolucion-bodega.service';
import { global } from './global';
import { DevolucionBodegaRequest } from '../dtos/devolucion-bodega-request';
import swal from 'sweetalert2';

describe('DevolucionBodegaService', () => {
  let service: DevolucionBodegaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(DevolucionBodegaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('consulta el listado con los filtros opcionales solo cuando vienen informados', () => {
    service.getListado(0, 5, '').subscribe();
    const simple = http.expectOne(r => r.url === `${global.url}/devoluciones-bodega/listado`);
    expect(simple.request.params.has('idBodega')).toBeFalse();
    expect(simple.request.params.has('estado')).toBeFalse();
    simple.flush({ content: [] });

    service.getListado(1, 5, 'norte', 3, 'PENDIENTE').subscribe();
    const filtrado = http.expectOne(r => r.url === `${global.url}/devoluciones-bodega/listado`);
    expect(filtrado.request.params.get('idBodega')).toBe('3');
    expect(filtrado.request.params.get('estado')).toBe('PENDIENTE');
    expect(filtrado.request.params.get('filtro')).toBe('norte');
    filtrado.flush({ content: [] });
  });

  it('las devoluciones pendientes salen de la paginación filtrada por estado', () => {
    let total: number;
    service.getTotalPendientes().subscribe(valor => total = valor);

    const req = http.expectOne(r => r.url === `${global.url}/devoluciones-bodega/listado`);
    expect(req.request.params.get('estado')).toBe('PENDIENTE');
    expect(req.request.params.get('size')).toBe('1');
    req.flush({ content: [], totalElements: 2 });

    expect(total).toBe(2);
  });

  it('consulta los productos de la sucursal que devuelve', () => {
    service.getProductosSucursal(4, 1, 5, 'cuaderno').subscribe();

    const req = http.expectOne(r => r.url === `${global.url}/devoluciones-bodega/sucursales/4/productos`);
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('5');
    expect(req.request.params.get('filtro')).toBe('cuaderno');
    req.flush({ content: [] });
  });

  it('registra una devolución enviando solo identificadores, cantidades y textos', () => {
    const request: DevolucionBodegaRequest = {
      idSucursalOrigen: 2, idBodega: 1, entregadoPor: 'Maria', motivo: 'Sobrestock', observaciones: '',
      items: [{ idProducto: 9, cantidad: 3 }]
    };
    service.create(request).subscribe();

    const req = http.expectOne(`${global.url}/devoluciones-bodega`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({ idDevolucion: 5 });
  });

  it('aprueba y rechaza usando los endpoints de resolución', () => {
    service.aprobar(7).subscribe();
    const aprobar = http.expectOne(`${global.url}/devoluciones-bodega/7/aprobar`);
    expect(aprobar.request.method).toBe('PUT');
    aprobar.flush({});

    service.cancelar(7, 'Mal estado').subscribe();
    const cancelar = http.expectOne(`${global.url}/devoluciones-bodega/7/cancelar`);
    expect(cancelar.request.method).toBe('PUT');
    expect(cancelar.request.body).toEqual({ motivo: 'Mal estado' });
    cancelar.flush({});
  });

  it('descarga el comprobante como PDF y lo abre en una pestaña nueva', () => {
    const abrir = spyOn(window, 'open');
    spyOn(window.URL, 'createObjectURL').and.returnValue('blob:comprobante');
    spyOn(window.URL, 'revokeObjectURL');
    service.imprimirComprobante(7).subscribe();

    const req = http.expectOne(`${global.url}/devoluciones-bodega/7/comprobante`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['%PDF'], { type: 'application/pdf' }));

    expect(abrir).toHaveBeenCalledWith('blob:comprobante', '_blank');
  });

  it('avisa si no se pudo generar el comprobante', () => {
    const alerta = spyOn(swal, 'fire');
    let fallo = false;
    service.imprimirComprobante(7).subscribe(() => {}, () => fallo = true);

    http.expectOne(`${global.url}/devoluciones-bodega/7/comprobante`).flush(new Blob(['x']), { status: 500, statusText: 'Error' });

    expect(fallo).toBeTrue();
    expect(alerta).toHaveBeenCalledWith('No se pudo generar el comprobante', jasmine.any(String), 'error');
  });

  it('muestra el mensaje del servidor cuando falla una operación', () => {
    const alerta = spyOn(swal, 'fire');
    let fallo = false;
    service.aprobar(7).subscribe(() => {}, () => fallo = true);

    http.expectOne(`${global.url}/devoluciones-bodega/7/aprobar`)
      .flush({ message: 'La bodega está inactiva' }, { status: 400, statusText: 'Bad Request' });

    expect(fallo).toBeTrue();
    expect(alerta).toHaveBeenCalledWith('No se pudo aprobar la devolución', 'La bodega está inactiva', 'error');
  });
});
