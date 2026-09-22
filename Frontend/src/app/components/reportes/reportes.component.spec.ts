import { ReportesComponent } from './reportes.component';

describe('ReportesComponent', () => {
  it('incluye los 16 reportes aprobados en el catálogo', () => {
    const auth: any = { usuario: {}, hasRole: () => true };
    const component = new ReportesComponent(auth, {} as any);
    expect(component.reportes.length).toBe(16);
  });

  it('no permite generar con un rango invertido', () => {
    const auth: any = { usuario: {}, hasRole: () => true };
    const component = new ReportesComponent(auth, {} as any);
    component.reporteSeleccionado = component.reportes.find(reporte => reporte.codigo === 'POLIZA_GENERAL');
    component.idSucursal = 1;
    component.fechaInicio = '2026-09-20';
    component.fechaFin = '2026-09-19';
    expect(component.puedeGenerar()).toBeFalse();
  });

  it('limpia filtros sin cambiar la sucursal seleccionada', () => {
    const auth: any = { usuario: {}, hasRole: () => true };
    const component = new ReportesComponent(auth, {} as any);
    component.reporteSeleccionado = component.reportes.find(reporte => reporte.codigo === 'KARDEX');
    component.idSucursal = 3;
    component.idProducto = 22;
    component.idCategoria = 5;
    component.estado = 'ACTIVA';
    component.formatoSeleccionado = 'XLSX';

    component.limpiarFiltros();

    expect(component.idSucursal).toBe(3);
    expect(component.idProducto).toBeNull();
    expect(component.idCategoria).toBeNull();
    expect(component.estado).toBe('');
    expect(component.formatoSeleccionado).toBe('PDF');
    expect(component.fechaInicio).toBeTruthy();
    expect(component.fechaFin >= component.fechaInicio).toBeTrue();
  });

  it('requiere fecha de corte para valorización', () => {
    const auth: any = { usuario: {}, hasRole: () => true };
    const component = new ReportesComponent(auth, {} as any);
    component.reporteSeleccionado = component.reportes.find(reporte => reporte.codigo === 'VALORIZACION');
    component.idSucursal = 1;
    component.fechaCorte = null;

    expect(component.puedeGenerar()).toBeFalse();

    component.fechaCorte = '2026-09-22';
    expect(component.puedeGenerar()).toBeTrue();
  });
});
