import { of, throwError } from 'rxjs';

import { DevolucionesBodegaComponent } from './devoluciones-bodega.component';
import swal from 'sweetalert2';

describe('DevolucionesBodegaComponent', () => {
  let component: DevolucionesBodegaComponent;
  let devolucionService: any;
  let bodegaService: any;
  let auth: any;

  const pagina = (contenido: any[]) => ({
    content: contenido, number: 0, totalPages: 1, totalElements: contenido.length, size: 5, first: true, last: true
  });

  function crear(queryBodega: string = null): DevolucionesBodegaComponent {
    const ruta: any = { snapshot: { queryParamMap: { get: () => queryBodega } } };
    return new DevolucionesBodegaComponent(devolucionService, bodegaService, ruta, auth);
  }

  beforeEach(() => {
    devolucionService = {
      getListado: jasmine.createSpy('getListado').and.returnValue(of(pagina([{ idDevolucion: 12, estado: 'PENDIENTE' }]))),
      getDevolucion: jasmine.createSpy('getDevolucion'),
      aprobar: jasmine.createSpy('aprobar').and.returnValue(of({})),
      cancelar: jasmine.createSpy('cancelar').and.returnValue(of({})),
      imprimirComprobante: jasmine.createSpy('imprimirComprobante').and.returnValue(of(undefined))
    };
    bodegaService = { getBodegas: jasmine.createSpy('getBodegas').and.returnValue(of([{ idBodega: 1 }])) };
    auth = { usuario: {}, hasRole: (rol: string) => rol === 'ROLE_ADMIN' };
    component = crear();
  });

  afterEach(() => component.ngOnDestroy());

  it('al iniciar carga las bodegas y la primera página de devoluciones', () => {
    component.ngOnInit();

    expect(bodegaService.getBodegas).toHaveBeenCalled();
    expect(devolucionService.getListado).toHaveBeenCalledWith(0, 5, '', null, undefined);
    expect(component.devoluciones.length).toBe(1);
  });

  it('toma el filtro de bodega de la ruta', () => {
    component = crear('4');

    component.ngOnInit();

    expect(component.idBodegaFiltro).toBe(4);
    expect(devolucionService.getListado).toHaveBeenCalledWith(0, 5, '', 4, undefined);
    component.ngOnDestroy();
  });

  it('el estado elegido se envía como filtro', () => {
    component.estadoFiltro = 'PENDIENTE';

    component.aplicarFiltros();

    expect(devolucionService.getListado).toHaveBeenCalledWith(0, 5, '', null, 'PENDIENTE');
    expect(component.hayFiltros).toBeTrue();
  });

  it('solo un administrador aprueba y ve los costos', () => {
    expect(component.puedeAprobar).toBeTrue();
    expect(component.verCostos).toBeTrue();

    auth.hasRole = () => false;
    expect(component.puedeAprobar).toBeFalse();
    expect(component.verCostos).toBeFalse();
  });

  it('presenta el número, la etiqueta y la clase de cada estado con el vocabulario de devolución', () => {
    expect(component.numeroDevolucion(12)).toBe('000012');
    expect(component.etiquetaEstado('PENDIENTE')).toBe('Pendiente de aprobación');
    expect(component.etiquetaEstado('REALIZADO')).toBe('Aprobada');
    expect(component.etiquetaEstado('CANCELADO')).toBe('Rechazada');
    expect(component.claseEstado('REALIZADO')).toBe('listing-status-success');
    expect(component.claseEstado('PENDIENTE')).toBe('listing-status-warning');
    expect(component.claseEstado('CANCELADO')).toBe('listing-status-danger');
  });

  it('informa el error al cargar el listado', () => {
    const alerta = spyOn(swal, 'fire');
    devolucionService.getListado.and.returnValue(throwError({ error: { message: 'Sin conexión' } }));

    component.cargarDevoluciones(0);

    expect(component.cargando).toBeFalse();
    expect(alerta).toHaveBeenCalledWith('Error al cargar devoluciones', 'Sin conexión', 'error');
  });

  it('abre el detalle cargando la devolución completa', () => {
    spyOn(swal, 'fire');
    spyOn(swal, 'close');
    devolucionService.getDevolucion.and.returnValue(of({ idDevolucion: 12, items: [] }));

    component.abrirDetalle({ idDevolucion: 12 } as any);

    expect(devolucionService.getDevolucion).toHaveBeenCalledWith(12);
    expect(component.devolucionSeleccionada.idDevolucion).toBe(12);
    expect(component.detalleCargandoId).toBeNull();
  });

  it('imprime el comprobante de la devolución', () => {
    component.imprimir(12);

    expect(devolucionService.imprimirComprobante).toHaveBeenCalledWith(12);
  });

  it('aprueba la devolución solo después de que el usuario confirme', async () => {
    spyOn(swal, 'fire').and.returnValues(Promise.resolve({ isConfirmed: true } as any), Promise.resolve({} as any));

    component.aprobar(12, 'Principal');
    await Promise.resolve();
    await Promise.resolve();

    expect(devolucionService.aprobar).toHaveBeenCalledWith(12);
    expect(component.procesandoId).toBeNull();
  });

  it('no aprueba si el usuario cancela la confirmación', async () => {
    spyOn(swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: false } as any));

    component.aprobar(12, 'Principal');
    await Promise.resolve();

    expect(devolucionService.aprobar).not.toHaveBeenCalled();
  });

  it('no inicia otra operación mientras hay una en curso', () => {
    const alerta = spyOn(swal, 'fire');
    component.procesandoId = 5;

    component.aprobar(12, 'Principal');
    component.rechazar(12, 'Norte');

    expect(alerta).not.toHaveBeenCalled();
  });

  it('rechaza enviando el motivo capturado', async () => {
    spyOn(swal, 'fire').and.returnValues(
      Promise.resolve({ isConfirmed: true, value: 'Mal estado' } as any), Promise.resolve({} as any));

    component.rechazar(12, 'Norte');
    await Promise.resolve();
    await Promise.resolve();

    expect(devolucionService.cancelar).toHaveBeenCalledWith(12, 'Mal estado');
    expect(component.procesandoId).toBeNull();
  });

  it('libera el bloqueo si la aprobación falla', async () => {
    spyOn(swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: true } as any));
    devolucionService.aprobar.and.returnValue(throwError({ error: { message: 'Bodega inactiva' } }));

    component.aprobar(12, 'Principal');
    await Promise.resolve();
    await Promise.resolve();

    expect(component.procesandoId).toBeNull();
  });
});
