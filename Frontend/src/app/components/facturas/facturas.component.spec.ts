import { FacturasComponent } from './facturas.component';

describe('FacturasComponent - modal de póliza', () => {
  let component: FacturasComponent;

  beforeEach(() => {
    component = new FacturasComponent(null, { usuario: null } as any);
  });

  it('controla la visibilidad de la póliza con estado Angular', () => {
    component.abrirModalPoliza();

    expect(component.modalPolizaVisible).toBeTrue();

    component.cerrarModalPoliza();

    expect(component.modalPolizaVisible).toBeFalse();
  });
});
