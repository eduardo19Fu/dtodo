import { of, throwError } from 'rxjs';

import { AvanceComisionDto, FilaComisionDto } from '../../dtos/comision-dto';
import { ComisionesComponent } from './comisiones.component';
import Swal from 'sweetalert2';

describe('ComisionesComponent', () => {
  let servicio: any;
  let component: ComisionesComponent;

  function fila(valores: Partial<FilaComisionDto> = {}): FilaComisionDto {
    return Object.assign(new FilaComisionDto(), {
      idVendedor: 1, vendedor: 'Ana Lopez', activo: true, montoEntregado: 750000, metaAlcanzada: 700000, porcentaje: 1,
      comision: 7500
    }, valores);
  }

  function avance(valores: Partial<AvanceComisionDto> = {}): AvanceComisionDto {
    return Object.assign(new AvanceComisionDto(), {
      anio: 2025, anioTerminado: true, liquidado: false,
      metas: [{ montoMinimo: 300000, porcentaje: 0.5 }, { montoMinimo: 700000, porcentaje: 1 }],
      filas: [fila(), fila({ idVendedor: 2, vendedor: 'Luis Perez', comision: 1500, porcentaje: 0.5 })],
      totalEntregado: 1000000, totalComision: 0
    }, valores);
  }

  function confirmar(valor?: string): void {
    spyOn(Swal, 'fire').and.callFake(((opciones: any) => opciones && opciones.input
      ? Promise.resolve({ isConfirmed: true, value: valor }) : Promise.resolve({ isConfirmed: true })) as any);
  }

  async function esperar(): Promise<void> {
    await Promise.resolve();
    await Promise.resolve();
  }

  beforeEach(() => {
    servicio = {
      avance: jasmine.createSpy('avance').and.returnValue(of(avance())),
      guardarMetas: jasmine.createSpy('guardarMetas').and.returnValue(of([])),
      copiarMetas: jasmine.createSpy('copiarMetas').and.returnValue(of([])),
      liquidar: jasmine.createSpy('liquidar').and.returnValue(of(avance({ liquidado: true }))),
      deshacer: jasmine.createSpy('deshacer').and.returnValue(of(avance())),
      reporte: jasmine.createSpy('reporte')
    };
    component = new ComisionesComponent(servicio);
    component.anio = 2025;
    component.ngOnInit();
  });

  it('carga el avance del año y copia las metas para editarlas', () => {
    expect(servicio.avance).toHaveBeenCalledWith(2025);
    expect(component.metas.length).toBe(2);
    expect(component.metas).not.toBe(component.avance.metas);
    expect(component.metasModificadas).toBeFalse();
  });

  it('cambiar de año recarga', () => {
    component.cambiarAnio(1);

    expect(component.anio).toBe(2026);
    expect(servicio.avance).toHaveBeenCalledWith(2026);
  });

  it('avisa si no pudo cargar', () => {
    servicio.avance.and.returnValue(throwError({ status: 500 }));

    component.cargar();

    expect(component.errorCarga).toBeTrue();
    expect(component.cargando).toBeFalse();
  });

  it('detecta cambios en las metas y bloquea liquidar mientras no se guarden', () => {
    expect(component.puedeLiquidar).toBeTrue();

    component.metas[1].porcentaje = 1.25;

    expect(component.metasModificadas).toBeTrue();
    expect(component.puedeLiquidar).toBeFalse();
    expect(component.motivoSinLiquidar).toContain('Guarda los cambios');

    component.descartarCambios();
    expect(component.metasModificadas).toBeFalse();
  });

  it('agregar y quitar metas se refleja como cambio', () => {
    component.agregarMeta();
    expect(component.metas.length).toBe(3);
    expect(component.metas[2].montoMinimo).toBe(800000);
    expect(component.metasModificadas).toBeTrue();

    component.quitarMeta(2);
    expect(component.metasModificadas).toBeFalse();
  });

  it('guardar las metas manda las editadas y recarga', () => {
    component.metas[0].montoMinimo = 250000;

    component.guardarMetas();

    expect(servicio.guardarMetas).toHaveBeenCalledWith(2025,
      [{ montoMinimo: 250000, porcentaje: 0.5 }, { montoMinimo: 700000, porcentaje: 1 }]);
    expect(servicio.avance).toHaveBeenCalledTimes(2);
  });

  it('un año sin terminar o sin metas no se puede liquidar y dice por qué', () => {
    servicio.avance.and.returnValue(of(avance({ anioTerminado: false })));
    component.cargar();
    expect(component.puedeLiquidar).toBeFalse();
    expect(component.motivoSinLiquidar).toContain('todavía no termina');

    servicio.avance.and.returnValue(of(avance({ metas: [] })));
    component.cargar();
    expect(component.puedeLiquidar).toBeFalse();
    expect(component.motivoSinLiquidar).toContain('Define y guarda las metas');
  });

  it('el total a pagar suma solo a los marcados como que completaron el año', () => {
    expect(component.totalAPagar).toBe(0);

    component.completaron[1] = true;

    expect(component.cantidadMarcados).toBe(1);
    expect(component.totalAPagar).toBe(7500);
  });

  it('liquidar manda la decisión de cada vendedor y muestra lo guardado', async () => {
    component.completaron[2] = true;
    confirmar();

    component.liquidar();
    await esperar();

    expect(servicio.liquidar).toHaveBeenCalledWith(2025, [
      { idVendedor: 1, completoAnio: false }, { idVendedor: 2, completoAnio: true }]);
    expect(component.liquidado).toBeTrue();
  });

  it('si el servidor rechaza la liquidación, muestra el mensaje y desbloquea', async () => {
    servicio.liquidar.and.returnValue(throwError({ error: { message: 'El año ya está liquidado.' } }));
    confirmar();

    component.liquidar();
    await esperar();

    expect(Swal.fire).toHaveBeenCalledWith('No se pudo liquidar', 'El año ya está liquidado.', 'error');
    expect(component.trabajando).toBeFalse();
  });

  it('deshacer pide el motivo y no llama si se cancela', async () => {
    spyOn(Swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: false }) as any);

    component.deshacer();
    await esperar();

    expect(servicio.deshacer).not.toHaveBeenCalled();
  });

  it('deshacer manda el motivo sin espacios sobrantes', async () => {
    confirmar('  se marcó mal  ');

    component.deshacer();
    await esperar();

    expect(servicio.deshacer).toHaveBeenCalledWith(2025, 'se marcó mal');
    expect(component.liquidado).toBeFalse();
  });

  it('el total a pagar no cuenta a quien aún no cumple 365 días', () => {
    servicio.avance.and.returnValue(of(avance({
      filas: [fila({ situacion: 'PENDIENTE' }), fila({ idVendedor: 2, comision: 1500, situacion: 'BLOQUE' })]
    })));
    component.cargar();
    component.completaron[2] = true;

    expect(component.totalAPagar).toBe(1500);
    expect(component.avance.filas[0].situacion).toBe('PENDIENTE');
  });

  it('un año liquidado conserva las decisiones guardadas', () => {
    servicio.avance.and.returnValue(of(avance({
      liquidado: true, filas: [fila({ completoAnio: true }), fila({ idVendedor: 2, completoAnio: false })]
    })));

    component.cargar();

    expect(component.completaron[1]).toBeTrue();
    expect(component.completaron[2]).toBeFalse();
    expect(component.puedeLiquidar).toBeFalse();
  });
});
