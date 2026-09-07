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
