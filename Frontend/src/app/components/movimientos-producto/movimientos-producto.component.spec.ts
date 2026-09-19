import { MovimientosProductoComponent } from './movimientos-producto.component';

describe('MovimientosProductoComponent - modal de reporte', () => {
  let component: MovimientosProductoComponent;

  beforeEach(() => {
    component = new MovimientosProductoComponent(null, null);
  });

  it('controla la visibilidad del reporte con estado Angular', () => {
    component.abrirModalReporte();

    expect(component.modalReporteVisible).toBeTrue();

    component.cerrarModalReporte();

    expect(component.modalReporteVisible).toBeFalse();
  });
});

describe('MovimientosProductoComponent - selector de rango', () => {
  let component: MovimientosProductoComponent;

  beforeEach(() => {
    component = new MovimientosProductoComponent(null, null);
  });

  it('cierra cada selector despues de elegir su fecha', () => {
    component.selectorFechaActivo = 'inicio';
    component.calendarioAbierto = true;

    component.seleccionarFecha({ iso: '2026-09-10', fecha: new Date(2026, 8, 10) } as any);

    expect(component.fechaIni).toBe('2026-09-10');
    expect(component.calendarioAbierto).toBeFalse();

    component.selectorFechaActivo = 'fin';
    component.calendarioAbierto = true;
    component.seleccionarFecha({ iso: '2026-09-18', fecha: new Date(2026, 8, 18) } as any);

    expect(component.fechaFin).toBe('2026-09-18');
    expect(component.calendarioAbierto).toBeFalse();
  });

  it('cierra el calendario al hacer click fuera de los campos', () => {
    component.calendarioAbierto = true;

    component.cerrarCalendarioAlHacerClickFuera({ target: document.body } as any);

    expect(component.calendarioAbierto).toBeFalse();
  });

  it('presenta las fechas ISO en formato local', () => {
    expect(component.formatearFechaVisible('2026-09-18')).toBe('18/09/2026');
    expect(component.formatearFechaVisible(null)).toBe('Seleccionar fecha');
  });
});
