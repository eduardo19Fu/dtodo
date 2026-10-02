import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { BodegaService } from './bodega.service';
import { global } from './global';
import { Bodega } from '../models/bodega';
import swal from 'sweetalert2';

describe('BodegaService', () => {
  let service: BodegaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(BodegaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lista las bodegas indicando si solo se quieren las activas', () => {
    service.getBodegas(true).subscribe();

    const req = http.expectOne(r => r.url === `${global.url}/bodegas`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('soloActivas')).toBe('true');
    req.flush([]);
  });

  it('consulta el listado paginado con filtro y orden', () => {
    service.getListado(2, 10, 'central', 'nombre', 'desc').subscribe();

    const req = http.expectOne(r => r.url === `${global.url}/bodegas/listado`);
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('10');
    expect(req.request.params.get('filtro')).toBe('central');
    expect(req.request.params.get('orden')).toBe('nombre');
    expect(req.request.params.get('direccion')).toBe('desc');
    req.flush({ content: [] });
  });

  it('el total de bodegas sale de la paginación del listado', () => {
    let total: number;
    service.getTotalBodegas().subscribe(valor => total = valor);

    const req = http.expectOne(r => r.url === `${global.url}/bodegas/listado`);
    expect(req.request.params.get('size')).toBe('1');
    req.flush({ content: [], totalElements: 7 });

    expect(total).toBe(7);
  });

  it('registra y actualiza bodegas', () => {
    const bodega = new Bodega();
    bodega.nombre = 'Principal';

    service.create(bodega).subscribe();
    const post = http.expectOne(`${global.url}/bodegas`);
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual(bodega);
    post.flush(bodega);

    service.update(bodega).subscribe();
    const put = http.expectOne(`${global.url}/bodegas`);
    expect(put.request.method).toBe('PUT');
    put.flush(bodega);
  });

  it('consulta el inventario paginado de una bodega', () => {
    service.getInventario(3, 0, 10, 'lapiz').subscribe();

    const req = http.expectOne(r => r.url === `${global.url}/bodegas/3/inventario`);
    expect(req.request.params.get('filtro')).toBe('lapiz');
    req.flush({ content: [] });
  });

  it('busca un producto por código escapando el valor', () => {
    service.getInventarioPorCodigo(3, 'A/B 1').subscribe();

    const req = http.expectOne(`${global.url}/bodegas/3/inventario/codigo/A%2FB%201`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('agrega producto y reduce existencias con el cuerpo indicado', () => {
    service.agregarProducto(3, { idProducto: 9, cantidad: 5, stockMinimo: 2 }).subscribe();
    const agregar = http.expectOne(`${global.url}/bodegas/3/movimientos/agregar`);
    expect(agregar.request.method).toBe('POST');
    expect(agregar.request.body).toEqual({ idProducto: 9, cantidad: 5, stockMinimo: 2 });
    agregar.flush({});

    service.reducirExistencias(3, { idProducto: 9, cantidad: 1, motivo: 'Merma' }).subscribe();
    const reducir = http.expectOne(`${global.url}/bodegas/3/movimientos/reducir`);
    expect(reducir.request.body.motivo).toBe('Merma');
    reducir.flush({});
  });

  it('elimina un producto enviando el motivo como parámetro', () => {
    service.eliminarProducto(3, 9, 'Ya no se maneja').subscribe();

    const req = http.expectOne(r => r.url === `${global.url}/bodegas/3/inventario/9`);
    expect(req.request.method).toBe('DELETE');
    expect(req.request.params.get('motivo')).toBe('Ya no se maneja');
    req.flush({});
  });

  it('consulta los movimientos solo con rango si vienen ambas fechas', () => {
    service.getMovimientos(3, 0, 10, { fechaIni: '2026-10-01', filtro: 'x' }).subscribe();
    const sinRango = http.expectOne(r => r.url === `${global.url}/bodegas/3/movimientos`);
    expect(sinRango.request.params.has('fechaIni')).toBeFalse();
    sinRango.flush({ content: [] });

    service.getMovimientos(3, 0, 10, { fechaIni: '2026-10-01', fechaFin: '2026-10-02', tipo: 'DESPACHO' }).subscribe();
    const conRango = http.expectOne(r => r.url === `${global.url}/bodegas/3/movimientos`);
    expect(conRango.request.params.get('fechaIni')).toBe('2026-10-01');
    expect(conRango.request.params.get('fechaFin')).toBe('2026-10-02');
    expect(conRango.request.params.get('tipo')).toBe('DESPACHO');
    conRango.flush({ content: [] });
  });

  it('copia el inventario indicando origen y destino', () => {
    service.clonarInventario(3, 'SUCURSAL', 1).subscribe();

    const req = http.expectOne(r => r.url === `${global.url}/bodegas/3/clonar-inventario`);
    expect(req.request.method).toBe('POST');
    expect(req.request.params.get('origen')).toBe('SUCURSAL');
    expect(req.request.params.get('idOrigen')).toBe('1');
    req.flush({});
  });

  it('importa un archivo Excel como multipart con el campo archivo', () => {
    const archivo = new File(['contenido'], 'inventario.xlsx');
    service.importarExcel(3, archivo).subscribe();

    const req = http.expectOne(`${global.url}/bodegas/3/importar-excel`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    expect((req.request.body as FormData).get('archivo')).toBeTruthy();
    req.flush({ filasLeidas: 1, productosImportados: 1, unidadesImportadas: 4, errores: [] });
  });

  it('descarga la plantilla de importación como blob', () => {
    service.descargarPlantillaImportacion().subscribe();

    const req = http.expectOne(`${global.url}/bodegas/plantilla-importacion`);
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['x']));
  });

  it('muestra el mensaje del servidor cuando una operación falla', () => {
    const alerta = spyOn(swal, 'fire');
    let fallo: any;
    service.reducirExistencias(3, { idProducto: 9, cantidad: 99 }).subscribe({ error: e => fallo = e });

    http.expectOne(`${global.url}/bodegas/3/movimientos/reducir`)
      .flush({ message: 'Existencias insuficientes' }, { status: 400, statusText: 'Bad Request' });

    expect(fallo.status).toBe(400);
    expect(alerta).toHaveBeenCalledWith('No se pudieron reducir las existencias', 'Existencias insuficientes', 'error');
  });
});
