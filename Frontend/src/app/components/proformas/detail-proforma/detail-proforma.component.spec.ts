import { DetailProformaComponent } from './detail-proforma.component';

describe('DetailProformaComponent historial', () => {
  let componente: DetailProformaComponent;

  beforeEach(() => {
    componente = new DetailProformaComponent(null, null, null, document);
  });

  it('arranca plegado y se alterna', () => {
    expect(componente.historialAbierto).toBeFalse();
    componente.alternarHistorial();
    expect(componente.historialAbierto).toBeTrue();
    componente.alternarHistorial();
    expect(componente.historialAbierto).toBeFalse();
  });

  it('el último movimiento es el primero de la lista', () => {
    expect(componente.ultimoMovimiento).toBeNull();
    componente.historial = [{ accion: 'AUTORIZACION' } as any, { accion: 'CREACION' } as any];
    expect(componente.ultimoMovimiento.accion).toBe('AUTORIZACION');
  });

  it('da tono, icono y etiqueta a las acciones nuevas', () => {
    expect(componente.tonoAccion('RECHAZO_CREDITO')).toBe('bad');
    expect(componente.tonoAccion('AUTORIZACION_CREDITO')).toBe('ok');
    expect(componente.tonoAccion('MODIFICACION')).toBe('neutral');
    expect(componente.iconoAccion('DEVOLUCION_CLIENTE')).toBe('fa-undo-alt');
    expect(componente.etiquetaAccion('DEVOLUCION_CLIENTE')).toBe('Devolución de cliente');
  });
});
