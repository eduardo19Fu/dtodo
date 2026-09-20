import { ReportesComponent } from './reportes.component';

describe('ReportesComponent', () => {
  it('incluye los 16 reportes aprobados en el catálogo', () => {
    const auth: any = { usuario: {}, hasRole: () => true };
    const component = new ReportesComponent(auth, {} as any, {} as any, {} as any, {} as any);
    expect(component.reportes.length).toBe(16);
  });

  it('no permite generar con un rango invertido', () => {
    const auth: any = { usuario: {}, hasRole: () => true };
    const component = new ReportesComponent(auth, {} as any, {} as any, {} as any, {} as any);
    component.reporteSeleccionado = component.reportes.find(reporte => reporte.codigo === 'POLIZA_GENERAL');
    component.idSucursal = 1;
    component.fechaInicio = '2026-09-20';
    component.fechaFin = '2026-09-19';
    expect(component.puedeGenerar()).toBeFalse();
  });
});
