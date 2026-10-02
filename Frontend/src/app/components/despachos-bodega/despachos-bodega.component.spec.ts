import { of, throwError } from 'rxjs';

import { DespachosBodegaComponent } from './despachos-bodega.component';
import swal from 'sweetalert2';

describe('DespachosBodegaComponent', () => {
  let component: DespachosBodegaComponent;
  let despachoService: any;
  let bodegaService: any;
  let auth: any;

  const pagina = (contenido: any[]) => ({
    content: contenido, number: 0, totalPages: 1, totalElements: contenido.length, size: 5, first: true, last: true
  });

  function crear(queryBodega: string = null): DespachosBodegaComponent {
    const ruta: any = { snapshot: { queryParamMap: { get: () => queryBodega } } };
    return new DespachosBodegaComponent(despachoService, bodegaService, ruta, auth);
  }

  beforeEach(() => {
    despachoService = {
      getListado: jasmine.createSpy('getListado').and.returnValue(of(pagina([{ idDespacho: 12, estado: 'PENDIENTE' }]))),
      getDespacho: jasmine.createSpy('getDespacho'),
      aprobar: jasmine.createSpy('aprobar').and.returnValue(of({})),
      cancelar: jasmine.createSpy('cancelar').and.returnValue(of({})),
      imprimirComprobante: jasmine.createSpy('imprimirComprobante').and.returnValue(of(undefined))
    };
    bodegaService = { getBodegas: jasmine.createSpy('getBodegas').and.returnValue(of([{ idBodega: 1 }])) };
    auth = { usuario: {}, hasRole: (rol: string) => rol === 'ROLE_ADMIN' };
    component = crear();
  });

  afterEach(() => component.ngOnDestroy());

  it('al iniciar carga las bodegas y la primera página de despachos', () => {
    component.ngOnInit();

    expect(bodegaService.getBodegas).toHaveBeenCalled();
    expect(despachoService.getListado).toHaveBeenCalledWith(0, 5, '', null, undefined);
    expect(component.despachos.length).toBe(1);
  });

  it('toma el filtro de bodega de la ruta cuando se llega desde una bodega', () => {
    component = crear('4');

    component.ngOnInit();

    expect(component.idBodegaFiltro).toBe(4);
    expect(despachoService.getListado).toHaveBeenCalledWith(0, 5, '', 4, undefined);
    component.ngOnDestroy();
  });

  it('el estado elegido se envía como filtro', () => {
    component.estadoFiltro = 'PENDIENTE';

    component.aplicarFiltros();

    expect(despachoService.getListado).toHaveBeenCalledWith(0, 5, '', null, 'PENDIENTE');
    expect(component.hayFiltros).toBeTrue();
  });

  it('solo un administrador puede aprobar', () => {
    expect(component.puedeAprobar).toBeTrue();

    auth.hasRole = () => false;
    expect(component.puedeAprobar).toBeFalse();
  });

  it('presenta el número con ceros a la izquierda y la etiqueta y clase de cada estado', () => {
    expect(component.numeroDespacho(12)).toBe('000012');
    expect(component.etiquetaEstado('PENDIENTE')).toBe('Pendiente de aprobación');
    expect(component.claseEstado('REALIZADO')).toBe('listing-status-success');
    expect(component.claseEstado('PENDIENTE')).toBe('listing-status-warning');
    expect(component.claseEstado('CANCELADO')).toBe('listing-status-danger');
  });

  it('informa el error al cargar el listado', () => {
    const alerta = spyOn(swal, 'fire');
    despachoService.getListado.and.returnValue(throwError({ error: { message: 'Sin conexión' } }));

    component.cargarDespachos(0);

    expect(component.cargando).toBeFalse();
    expect(alerta).toHaveBeenCalledWith('Error al cargar despachos', 'Sin conexión', 'error');
  });

  it('abre el detalle cargando el despacho completo', () => {
    spyOn(swal, 'fire');
    spyOn(swal, 'close');
    despachoService.getDespacho.and.returnValue(of({ idDespacho: 12, items: [] }));

    component.abrirDetalle({ idDespacho: 12 } as any);

    expect(despachoService.getDespacho).toHaveBeenCalledWith(12);
    expect(component.despachoSeleccionado.idDespacho).toBe(12);
    expect(component.detalleCargandoId).toBeNull();
  });

  it('imprime el comprobante del despacho', () => {
    component.imprimir(12);

    expect(despachoService.imprimirComprobante).toHaveBeenCalledWith(12);
  });

  it('aprueba el despacho solo después de que el usuario confirme', async () => {
    spyOn(swal, 'fire').and.returnValues(Promise.resolve({ isConfirmed: true } as any), Promise.resolve({} as any));

    component.aprobar(12, 'Norte');
    await Promise.resolve();
    await Promise.resolve();

    expect(despachoService.aprobar).toHaveBeenCalledWith(12);
    expect(component.procesandoId).toBeNull();
  });

  it('no aprueba si el usuario cancela la confirmación', async () => {
    spyOn(swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: false } as any));

    component.aprobar(12, 'Norte');
    await Promise.resolve();

    expect(despachoService.aprobar).not.toHaveBeenCalled();
  });

  it('no inicia otra aprobación mientras hay una en curso', () => {
    const alerta = spyOn(swal, 'fire');
    component.procesandoId = 5;

    component.aprobar(12, 'Norte');
    component.cancelar(12);

    expect(alerta).not.toHaveBeenCalled();
  });

  it('cancela el despacho con el motivo capturado', async () => {
    spyOn(swal, 'fire').and.returnValues(
      Promise.resolve({ isConfirmed: true, value: 'Error de captura' } as any), Promise.resolve({} as any));

    component.cancelar(12);
    await Promise.resolve();
    await Promise.resolve();

    expect(despachoService.cancelar).toHaveBeenCalledWith(12, 'Error de captura');
  });

  it('un error al aprobar libera el estado de procesamiento', async () => {
    despachoService.aprobar.and.returnValue(throwError({ status: 400 }));
    spyOn(swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: true } as any));

    component.aprobar(12, 'Norte');
    await Promise.resolve();
    await Promise.resolve();

    expect(component.procesandoId).toBeNull();
  });

  it('el motivo de la cancelación es obligatorio en el diálogo', async () => {
    const alerta = spyOn(swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: false } as any));

    component.cancelar(12);
    await Promise.resolve();

    const opciones: any = alerta.calls.mostRecent().args[0];
    expect(opciones.inputValidator('  ')).toBeTruthy();
    expect(opciones.inputValidator('Motivo')).toBeNull();
  });
});
