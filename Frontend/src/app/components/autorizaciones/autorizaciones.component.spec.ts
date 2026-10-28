import { of, throwError } from 'rxjs';

import { AutorizacionProformaDto } from '../../dtos/autorizacion-dto';
import { AutorizacionesComponent } from './autorizaciones.component';
import Swal from 'sweetalert2';

describe('AutorizacionesComponent', () => {
  let servicio: any;
  let component: AutorizacionesComponent;

  function dto(valores: Partial<AutorizacionProformaDto> = {}): AutorizacionProformaDto {
    return Object.assign(new AutorizacionProformaDto(), {
      idProforma: 7, noProforma: '123P', cliente: 'Libreria Uno', vendedor: 'Ana Lopez', total: 100, tipo: 'DESCUENTO'
    }, valores);
  }

  function confirmarConMotivo(motivo: string): void {
    spyOn(Swal, 'fire').and.callFake(((opciones: any) => opciones && opciones.input
      ? Promise.resolve({ isConfirmed: true, value: motivo }) : Promise.resolve({ isConfirmed: true })) as any);
  }

  async function esperar(): Promise<void> {
    await Promise.resolve();
    await Promise.resolve();
  }

  beforeEach(() => {
    servicio = {
      pendientes: jasmine.createSpy('pendientes').and.returnValue(of([dto()])),
      vencidas: jasmine.createSpy('vencidas').and.returnValue(of([dto({ idProforma: 8, tipo: 'VENCIDA' })])),
      credito: jasmine.createSpy('credito').and.returnValue(of([dto({ idProforma: 9, tipo: 'CREDITO', excesoCredito: 250, limiteCredito: 1000 })])),
      aprobarCredito: jasmine.createSpy('aprobarCredito').and.returnValue(of({})),
      rechazarCredito: jasmine.createSpy('rechazarCredito').and.returnValue(of({})),
      aprobar: jasmine.createSpy('aprobar').and.returnValue(of({})),
      rechazar: jasmine.createSpy('rechazar').and.returnValue(of({})),
      autorizarVencida: jasmine.createSpy('autorizarVencida').and.returnValue(of({}))
    };
    component = new AutorizacionesComponent(servicio);
  });

  it('carga las dos listas al iniciar', () => {
    component.ngOnInit();

    expect(component.pendientes.length).toBe(1);
    expect(component.vencidas.length).toBe(1);
    expect(component.credito.length).toBe(1);
    expect(component.total).toBe(3);
    expect(component.cargando).toBeFalse();
    expect(component.errorCarga).toBeFalse();
  });

  it('si una lista falla lo indica y deja de cargar', () => {
    servicio.vencidas.and.returnValue(throwError({ status: 500 }));

    component.cargar();

    expect(component.errorCarga).toBeTrue();
    expect(component.cargando).toBeFalse();
    expect(component.pendientes.length).toBe(1);
  });

  it('aprobar pide el motivo, lo envía limpio y recarga', async () => {
    confirmarConMotivo('  cliente de volumen ');
    component.cargar();
    servicio.pendientes.calls.reset();

    component.aprobar(dto());
    await esperar();

    expect(servicio.aprobar).toHaveBeenCalledWith(7, 'cliente de volumen');
    expect(servicio.pendientes).toHaveBeenCalledTimes(1);
    expect(component.resolviendo).toBeNull();
  });

  it('rechazar y autorizar una vencida llaman a su operación', async () => {
    confirmarConMotivo('motivo');

    component.rechazar(dto());
    await esperar();
    component.autorizarVencida(dto({ idProforma: 8 }));
    await esperar();

    expect(servicio.rechazar).toHaveBeenCalledWith(7, 'motivo');
    expect(servicio.autorizarVencida).toHaveBeenCalledWith(8, 'motivo');
  });

  it('si se cancela el cuadro del motivo no se llama al servidor', async () => {
    spyOn(Swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: false } as any));

    component.aprobar(dto());
    await esperar();

    expect(servicio.aprobar).not.toHaveBeenCalled();
  });

  it('un 422 del servidor se muestra y la bandeja se recarga', async () => {
    servicio.aprobar.and.returnValue(throwError({ error: { message: 'La proforma ya no está pendiente.' } }));
    const fire = spyOn(Swal, 'fire').and.callFake(((opciones: any) => opciones && opciones.input
      ? Promise.resolve({ isConfirmed: true, value: 'motivo' }) : Promise.resolve({ isConfirmed: true })) as any);
    servicio.pendientes.calls.reset();

    component.aprobar(dto());
    await esperar();

    expect(fire).toHaveBeenCalledWith('No se pudo resolver', 'La proforma ya no está pendiente.', 'error');
    expect(servicio.pendientes).toHaveBeenCalled();
    expect(component.resolviendo).toBeNull();
  });

  it('cuenta los días que lleva vencida una proforma', () => {
    const ayer = new Date();
    ayer.setDate(ayer.getDate() - 3);
    const dos = (n: number) => String(n).padStart(2, '0');
    const fecha = `${ayer.getFullYear()}-${dos(ayer.getMonth() + 1)}-${dos(ayer.getDate())}`;

    expect(component.diasVencida(dto({ fechaVencimiento: fecha }))).toBe(3);
    expect(component.diasVencida(dto({ fechaVencimiento: null }))).toBe(0);
  });

  it('autorizar el crédito pide el motivo y rechazarlo también', async () => {
    confirmarConMotivo('  buen pagador ');
    component.cargar();

    component.aprobarCredito(dto({ idProforma: 9, tipo: 'CREDITO' }));
    await esperar();
    expect(servicio.aprobarCredito).toHaveBeenCalledWith(9, 'buen pagador');

    component.rechazarCredito(dto({ idProforma: 9, tipo: 'CREDITO' }));
    await esperar();
    expect(servicio.rechazarCredito).toHaveBeenCalledWith(9, 'buen pagador');
  });
});
