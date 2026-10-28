import { of, throwError } from 'rxjs';

import { IndicadoresMesDto } from '../../dtos/indicadores-dto';
import { IndicadoresComponent } from './indicadores.component';

describe('IndicadoresComponent', () => {
  let servicio: any;
  let component: IndicadoresComponent;

  function datos(valores: Partial<IndicadoresMesDto> = {}): IndicadoresMesDto {
    return Object.assign(new IndicadoresMesDto(), {
      anio: 2026, mes: 10, totalEntregado: 1000, totalDevuelto: 100, totalNeto: 900, netoMesAnterior: 600, pedidosConFecha: 4,
      pedidosATiempo: 3, porcentajeATiempo: 75,
      vendedores: [{ idVendedor: 1, vendedor: 'Ana Lopez', entregado: 1000, devuelto: 100, neto: 900, participacion: 100,
        pedidosConFecha: 4, pedidosATiempo: 3, porcentajeATiempo: 75 }],
      cuentasPorCobrar: { total: 500, porVencer: 200, vencido: 300, clientesConDeuda: 2, clientesConVencido: 1 }
    }, valores);
  }

  beforeEach(() => {
    servicio = { mes: jasmine.createSpy('mes').and.returnValue(of(datos())) };
    component = new IndicadoresComponent(servicio);
  });

  it('carga el mes actual al iniciar', () => {
    const hoy = new Date();

    component.ngOnInit();

    expect(servicio.mes).toHaveBeenCalledWith(hoy.getFullYear(), hoy.getMonth() + 1);
    expect(component.datos.totalNeto).toBe(900);
    expect(component.esMesActual).toBeTrue();
  });

  it('avisa si no pudo cargar', () => {
    servicio.mes.and.returnValue(throwError({ status: 500 }));

    component.cargar();

    expect(component.errorCarga).toBeTrue();
    expect(component.cargando).toBeFalse();
  });

  it('cambia de mes sin pasar del actual y cruzando el año', () => {
    component.anio = 2026;
    component.mes = 1;
    const hoy = new Date();

    component.cambiarMes(-1);
    expect(component.anio).toBe(2025);
    expect(component.mes).toBe(12);
    expect(component.etiquetaMes).toBe('diciembre 2025');

    component.anio = hoy.getFullYear();
    component.mes = hoy.getMonth() + 1;
    servicio.mes.calls.reset();
    component.cambiarMes(1);
    expect(servicio.mes).not.toHaveBeenCalled();
  });

  it('no retrocede antes de enero de 2020', () => {
    component.anio = 2020;
    component.mes = 1;

    component.cambiarMes(-1);

    expect(component.puedeRetroceder).toBeFalse();
    expect(component.anio).toBe(2020);
  });

  it('calcula la variación contra el mes anterior y la omite si no hubo ventas', () => {
    component.datos = datos();
    expect(component.variacion).toBe(50);

    component.datos = datos({ netoMesAnterior: 0 });
    expect(component.variacion).toBeNull();

    component.datos = datos({ totalNeto: 300, netoMesAnterior: 600 });
    expect(component.variacion).toBe(-50);
  });

  it('el aro toma su tono del porcentaje y su relleno de los grados', () => {
    expect(component.claseATiempo(95)).toBe('bueno');
    expect(component.claseATiempo(75)).toBe('regular');
    expect(component.claseATiempo(40)).toBe('malo');
    expect(component.claseATiempo(null)).toBe('sin-datos');
    expect(component.aro(50)).toContain('180deg');
    expect(component.aro(150)).toContain('360deg');
    expect(component.aro(null)).toContain('0deg');
  });

  it('sabe si hubo ventas en el mes', () => {
    component.datos = datos();
    expect(component.hayVentas).toBeTrue();

    component.datos = datos({ vendedores: [{ idVendedor: 1, vendedor: 'Ana', entregado: 0, devuelto: 0, neto: 0, participacion: 0,
      pedidosConFecha: 0, pedidosATiempo: 0 }] });
    expect(component.hayVentas).toBeFalse();
  });
});
