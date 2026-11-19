import { of, Subject, throwError } from 'rxjs';

import { ChequeDto } from '../../dtos/recibo-caja-dto';
import { ChequesComponent } from './cheques.component';
import Swal from 'sweetalert2';

describe('ChequesComponent', () => {
  let servicio: any;
  let sucursales: any;
  let auth: any;
  let component: ChequesComponent;

  function cheque(valores: Partial<ChequeDto> = {}): ChequeDto {
    return Object.assign(new ChequeDto(), {
      idCheque: 1, idRecibo: 9, numeroRecibo: 'S2-000015', cliente: 'Libreria Uno', banco: 'Banrural', numeroCheque: '0045123',
      monto: 500, estado: 'RECIBIDO', postfechado: false, fechaPago: '2026-11-09', fechaCheque: '2026-11-09'
    }, valores);
  }

  function pagina(contenido: ChequeDto[]): any {
    return { content: contenido, number: 0, totalPages: 1, totalElements: contenido.length, first: true, last: true };
  }

  async function esperar(): Promise<void> {
    await Promise.resolve();
    await Promise.resolve();
  }

  beforeEach(() => {
    servicio = {
      getListado: jasmine.createSpy('getListado').and.returnValue(of(pagina([cheque()]))),
      cambiarEstado: jasmine.createSpy('cambiarEstado').and.returnValue(of(cheque({ estado: 'DEPOSITADO' }))),
      reporte: jasmine.createSpy('reporte').and.returnValue(of({ body: new Blob(['x']) })),
      adjuntar: jasmine.createSpy('adjuntar').and.returnValue(of(cheque({ tieneAdjunto: true, adjuntoNombre: 'foto.png' }))),
      descargarAdjunto: jasmine.createSpy('descargarAdjunto').and.returnValue(of({ body: new Blob(['x']) })),
      quitarAdjunto: jasmine.createSpy('quitarAdjunto').and.returnValue(of(cheque({ tieneAdjunto: false })))
    };
    sucursales = { getSucursales: jasmine.createSpy('getSucursales').and.returnValue(of([])) };
    auth = { hasRole: (rol: string) => rol === 'ROLE_ADMIN' };
    component = new ChequesComponent(servicio, sucursales, auth);
    spyOn(window, 'open');
    spyOn(window.URL, 'createObjectURL').and.returnValue('blob:x');
  });

  it('carga la lista al iniciar y, si es administrador, las sucursales', () => {
    component.ngOnInit();

    expect(servicio.getListado).toHaveBeenCalled();
    expect(component.cheques.length).toBe(1);
    expect(sucursales.getSucursales).toHaveBeenCalled();
  });

  it('el cobrador no pide las sucursales', () => {
    auth.hasRole = () => false;

    component.ngOnInit();

    expect(sucursales.getSucursales).not.toHaveBeenCalled();
  });

  it('avisa si no pudo cargar la lista', () => {
    spyOn(Swal, 'fire');
    servicio.getListado.and.returnValue(throwError({ status: 500 }));

    component.cargar(0);

    expect(component.errorCarga).toBeTrue();
    expect(component.cargando).toBeFalse();
  });

  it('los filtros se mandan al servicio y limpiarlos los quita', () => {
    component.estado = 'COBRADO';
    component.desde = '2026-11-01';
    component.hasta = '2026-11-30';
    component.aplicarFiltros();

    expect(servicio.getListado).toHaveBeenCalledWith(0, 10, jasmine.objectContaining({ estado: 'COBRADO', desde: '2026-11-01', hasta: '2026-11-30' }));

    component.limpiarFiltros();
    expect(component.estado).toBe('');
    expect(component.desde).toBe('');
  });

  it('cada estado ofrece solo los pasos válidos y los finales no ofrecen ninguno', () => {
    expect(component.siguientes(cheque())).toEqual(['DEPOSITADO', 'RECHAZADO']);
    expect(component.siguientes(cheque({ estado: 'DEPOSITADO' }))).toEqual(['COBRADO', 'RECHAZADO']);
    expect(component.siguientes(cheque({ estado: 'COBRADO' }))).toEqual([]);
    expect(component.siguientes(cheque({ estado: 'RECHAZADO' }))).toEqual([]);
  });

  it('un postfechado no se puede depositar pero sí rechazar', () => {
    const postfechado = cheque({ postfechado: true });

    expect(component.puedeCambiar(postfechado, 'DEPOSITADO')).toBeFalse();
    expect(component.puedeCambiar(postfechado, 'RECHAZADO')).toBeTrue();
  });

  it('marcar depositado pide confirmación y recarga', async () => {
    spyOn(Swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: true } as any));

    component.cambiarEstado(cheque(), 'DEPOSITADO');
    await esperar();

    expect(servicio.cambiarEstado).toHaveBeenCalledWith(1, 'DEPOSITADO', undefined);
    expect(servicio.getListado).toHaveBeenCalled();
  });

  it('si se cancela la confirmación no cambia nada', async () => {
    spyOn(Swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: false } as any));

    component.cambiarEstado(cheque(), 'DEPOSITADO');
    await esperar();

    expect(servicio.cambiarEstado).not.toHaveBeenCalled();
  });

  it('rechazar exige el motivo y lo manda sin espacios sobrantes', async () => {
    const fire = spyOn(Swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: true, value: '  Fondos insuficientes ' } as any));

    component.cambiarEstado(cheque({ estado: 'DEPOSITADO' }), 'RECHAZADO');
    await esperar();

    const opciones: any = fire.calls.first().args[0];
    expect(opciones.inputValidator('  ')).toBe('Indica el motivo.');
    expect(opciones.inputValidator('sin fondos')).toBeNull();
    expect(servicio.cambiarEstado).toHaveBeenCalledWith(1, 'RECHAZADO', 'Fondos insuficientes');
  });

  it('si el servidor rechaza el cambio muestra el mensaje y libera el botón', async () => {
    const fire = spyOn(Swal, 'fire');
    fire.and.returnValue(Promise.resolve({ isConfirmed: true } as any));
    servicio.cambiarEstado.and.returnValue(throwError({ error: { message: 'El cheque es postfechado.' } }));

    component.cambiarEstado(cheque(), 'DEPOSITADO');
    await esperar();

    expect(fire).toHaveBeenCalledWith('No se pudo actualizar el cheque', 'El cheque es postfechado.', 'error');
    expect(component.trabajando).toBeNull();
  });

  it('mientras se genera el reporte marca el formato y lo libera al terminar', () => {
    const respuesta = new Subject<any>();
    servicio.reporte.and.returnValue(respuesta);
    component.desde = '2026-11-01';
    component.hasta = '2026-11-30';
    component.estado = 'RECIBIDO';

    component.descargar('PDF');
    expect(component.generando).toBe('PDF');
    component.descargar('XLSX');
    expect(servicio.reporte).toHaveBeenCalledTimes(1);

    respuesta.next({ body: new Blob(['x']) });
    expect(component.generando).toBeNull();
    expect(servicio.reporte).toHaveBeenCalledWith('PDF', jasmine.objectContaining({ desde: '2026-11-01', hasta: '2026-11-30', estado: 'RECIBIDO' }));
  });

  it('sin fechas el reporte cubre los últimos 30 días y si falla libera el indicador', () => {
    spyOn(Swal, 'fire');
    servicio.reporte.and.returnValue(throwError({ status: 500, error: {} }));

    component.descargar('XLSX');

    const filtros = servicio.reporte.calls.mostRecent().args[1];
    expect(filtros.desde < filtros.hasta).toBeTrue();
    expect(component.generando).toBeNull();
  });

  it('da etiqueta y color a cada estado', () => {
    expect(component.etiquetaEstado('DEPOSITADO')).toBe('Depositado');
    expect(component.claseEstado('COBRADO')).toBe('listing-status-success');
    expect(component.claseEstado('RECHAZADO')).toBe('listing-status-danger');
    expect(component.claseEstado('RECIBIDO')).toBe('listing-status-warning');
  });

  function entradaCon(archivo: File | null): HTMLInputElement {
    return { files: archivo ? [archivo] : [], value: 'x' } as any;
  }

  it('adjuntar sube el archivo y actualiza la fila con su nombre', () => {
    spyOn(Swal, 'fire');
    component.cheques = [cheque()];
    const archivo = new File([new Uint8Array(10)], 'foto.png', { type: 'image/png' });

    component.elegirArchivo(component.cheques[0], entradaCon(archivo));

    expect(servicio.adjuntar).toHaveBeenCalledWith(1, archivo);
    expect(component.cheques[0].tieneAdjunto).toBeTrue();
    expect(component.cheques[0].adjuntoNombre).toBe('foto.png');
    expect(component.subiendoAdjunto).toBeNull();
  });

  it('un archivo de más de 5 MB no se sube', () => {
    const fire = spyOn(Swal, 'fire');
    const grande = new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'grande.pdf', { type: 'application/pdf' });

    component.elegirArchivo(cheque(), entradaCon(grande));

    expect(servicio.adjuntar).not.toHaveBeenCalled();
    expect(fire).toHaveBeenCalled();
  });

  it('si el servidor rechaza el archivo muestra el mensaje y libera el botón', () => {
    const fire = spyOn(Swal, 'fire');
    servicio.adjuntar.and.returnValue(throwError({ error: { message: 'El archivo debe ser una foto (PNG o JPG) o un PDF.' } }));

    component.elegirArchivo(cheque(), entradaCon(new File([new Uint8Array(3)], 'a.pdf')));

    expect(fire).toHaveBeenCalledWith('No se pudo guardar el archivo', 'El archivo debe ser una foto (PNG o JPG) o un PDF.', 'error');
    expect(component.subiendoAdjunto).toBeNull();
  });

  it('ver el adjunto lo abre en otra pestaña y quitarlo pide confirmación', async () => {

    component.verAdjunto(cheque({ tieneAdjunto: true }));
    expect(window.open).toHaveBeenCalledWith('blob:x', '_blank');

    spyOn(Swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: true } as any));
    component.cheques = [cheque({ tieneAdjunto: true, adjuntoNombre: 'foto.png' })];
    component.quitarAdjunto(component.cheques[0]);
    await esperar();
    expect(servicio.quitarAdjunto).toHaveBeenCalledWith(1);
    expect(component.cheques[0].tieneAdjunto).toBeFalse();
  });
});
