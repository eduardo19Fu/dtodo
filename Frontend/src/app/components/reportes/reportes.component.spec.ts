import { of, throwError } from 'rxjs';
import Swal from 'sweetalert2';

import { ReportesComponent } from './reportes.component';

describe('ReportesComponent', () => {
  it('incluye los 26 reportes aprobados en el catálogo', () => {
    const auth: any = { usuario: {}, hasRole: () => true };
    const component = new ReportesComponent(auth, {} as any);
    expect(component.reportes.length).toBe(26);
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

  it('filtra por palabras en cualquier orden e ignora acentos y mayúsculas', () => {
    const auth: any = { usuario: {}, hasRole: () => true };
    const component = new ReportesComponent(auth, {} as any);

    component.actualizarBusqueda('CRÉDITO resumen');

    expect(component.reportesPorCategoria('NOTAS_CREDITO').map(reporte => reporte.codigo))
      .toEqual(['RESUMEN_NOTAS']);
    expect(component.reportesPorCategoria('VENTAS')).toEqual([]);
    expect(component.hayReportesEncontrados).toBeTrue();
  });

  it('busca también en la descripción y respeta los permisos', () => {
    const auth: any = { usuario: {}, hasRole: (role: string) => role === 'ROLE_INVENTARIO' };
    const component = new ReportesComponent(auth, {} as any);

    component.actualizarBusqueda('pendientes entregar');
    expect(component.reportesPorCategoria('NOTAS_CREDITO').map(reporte => reporte.codigo))
      .toEqual(['PENDIENTES_DESPACHO']);

    component.actualizarBusqueda('compras proveedor');
    expect(component.hayReportesEncontrados).toBeFalse();
    expect(component.reportesPorCategoria('COMPRAS')).toEqual([]);
  });

  it('cierra la configuración si la búsqueda oculta su tarjeta y permite limpiar', () => {
    const auth: any = { usuario: {}, hasRole: () => true };
    const component = new ReportesComponent(auth, {} as any);
    component.reporteSeleccionado = component.reportes.find(reporte => reporte.codigo === 'POLIZA_GENERAL');

    component.actualizarBusqueda('kardex');
    expect(component.reporteSeleccionado).toBeNull();
    expect(component.reportesPorCategoria('INVENTARIO').map(reporte => reporte.codigo)).toEqual(['KARDEX']);

    component.limpiarBusqueda();
    expect(component.busquedaReportes).toBe('');
    expect(component.reportesPorCategoria('VENTAS').length).toBe(5);
  });

  describe('reportes de bodegas', () => {
    function crear(rolesPermitidos: string[]): ReportesComponent {
      const auth: any = { usuario: {}, hasRole: (role: string) => rolesPermitidos.includes(role) };
      return new ReportesComponent(auth, {} as any);
    }

    it('un usuario de bodega solo ve los reportes de la categoría Bodegas', () => {
      const component = crear(['ROLE_BODEGA']);

      expect(component.reportesPorCategoria('BODEGAS').map(reporte => reporte.codigo))
        .toEqual(['BODEGA_EXISTENCIAS', 'BODEGA_MOVIMIENTOS', 'BODEGA_DESPACHOS']);
      ['VENTAS', 'INVENTARIO', 'PROFORMAS', 'NOTAS_CREDITO', 'COMPRAS'].forEach(categoria =>
        expect(component.reportesPorCategoria(categoria as any)).toEqual([]));
    });

    it('un usuario de inventario no ve los reportes de bodegas', () => {
      const component = crear(['ROLE_INVENTARIO']);

      expect(component.reportesPorCategoria('BODEGAS')).toEqual([]);
    });

    it('las existencias de bodega exigen elegir una bodega', () => {
      const component = crear(['ROLE_BODEGA']);
      component.reporteSeleccionado = component.reportes.find(reporte => reporte.codigo === 'BODEGA_EXISTENCIAS');

      expect(component.puedeGenerar()).toBeFalse();

      component.idBodega = 4;
      expect(component.puedeGenerar()).toBeTrue();
    });

    it('los movimientos de bodega exigen bodega y un rango de fechas válido', () => {
      const component = crear(['ROLE_BODEGA']);
      component.reporteSeleccionado = component.reportes.find(reporte => reporte.codigo === 'BODEGA_MOVIMIENTOS');
      component.idBodega = 4;
      component.fechaInicio = '2026-10-05';
      component.fechaFin = '2026-10-01';

      expect(component.puedeGenerar()).toBeFalse();

      component.fechaFin = '2026-10-06';
      expect(component.puedeGenerar()).toBeTrue();
    });

    it('los despachos de bodega permiten generar sin elegir bodega y usan sus propios estados', () => {
      const component = crear(['ROLE_ADMIN']);
      component.reporteSeleccionado = component.reportes.find(reporte => reporte.codigo === 'BODEGA_DESPACHOS');
      component.idBodega = null;

      expect(component.puedeGenerar()).toBeTrue();
      expect(component.opcionesEstado.map(opcion => opcion.valor)).toEqual(['PENDIENTE', 'REALIZADO', 'CANCELADO']);
    });

    it('limpiar filtros reinicia la bodega seleccionada', () => {
      const component = crear(['ROLE_ADMIN']);
      component.reporteSeleccionado = component.reportes.find(reporte => reporte.codigo === 'BODEGA_DESPACHOS');
      component.idBodega = 4;

      component.limpiarFiltros();

      expect(component.idBodega).toBeNull();
    });
  });

  describe('reportes de mayoristas', () => {
    const admin: any = { usuario: {}, hasRole: (rol: string) => rol === 'ROLE_ADMIN' };

    it('son solo del administrador y están en su propia categoría', () => {
      const component = new ReportesComponent(admin, {} as any);
      const mayoristas = component.reportes.filter(reporte => reporte.categoria === 'MAYORISTAS');

      expect(mayoristas.map(reporte => reporte.codigo)).toEqual(
        ['MAYORISTAS_CLIENTES_RUTA', 'MAYORISTAS_SIN_COMPRAS', 'MAYORISTAS_PROFORMAS_VENDEDOR', 'MAYORISTAS_ENTREGADO',
         'MAYORISTAS_CHEQUES', 'MAYORISTAS_DESPACHADOS_CREDITO', 'MAYORISTAS_LISTADO_PROFORMAS']);
      expect(mayoristas.every(reporte => reporte.roles.length === 1 && reporte.roles[0] === 'ROLE_ADMIN')).toBeTrue();
      expect(component.reportesPorCategoria('MAYORISTAS').length).toBe(7);

      const cobrador: any = { usuario: {}, hasRole: (rol: string) => rol === 'ROLE_COBRADOR' };
      expect(new ReportesComponent(cobrador, {} as any).reportesPorCategoria('MAYORISTAS').length).toBe(0);
    });

    it('clientes por ruta se genera sin fechas, con ruta opcional', () => {
      const component = new ReportesComponent(admin, {} as any);
      component.reporteSeleccionado = component.reportes.find(reporte => reporte.codigo === 'MAYORISTAS_CLIENTES_RUTA');
      component.fechaInicio = null;
      component.fechaFin = null;

      expect(component.requiereFiltro('RUTA')).toBeTrue();
      expect(component.requiereFiltro('FECHAS')).toBeFalse();
      expect(component.puedeGenerar()).toBeTrue();
    });

    it('sin compras y proformas por vendedor piden un período válido', () => {
      const component = new ReportesComponent(admin, {} as any);
      component.reporteSeleccionado = component.reportes.find(reporte => reporte.codigo === 'MAYORISTAS_SIN_COMPRAS');
      component.fechaInicio = '2026-10-20';
      component.fechaFin = '2026-10-10';
      expect(component.puedeGenerar()).toBeFalse();

      component.fechaFin = '2026-10-31';
      expect(component.puedeGenerar()).toBeTrue();
    });

    it('ventas entregadas pide período y deja la ruta y el vendedor opcionales', () => {
      const servicio: any = {
        listarRutasSelector: jasmine.createSpy('listarRutasSelector').and.returnValue(of([])),
        listarUsuariosProformasSelector: jasmine.createSpy('listarUsuariosProformasSelector').and.returnValue(of([{ valor: 2, etiqueta: 'Luis Perez' }])),
        generar: jasmine.createSpy('generar').and.returnValue(throwError({ error: { message: 'x' } }))
      };
      const component = new ReportesComponent(admin, servicio);
      const reporte = component.reportes.find(item => item.codigo === 'MAYORISTAS_ENTREGADO');

      component.seleccionarReporte(reporte);
      expect(component.requiereFiltro('VENDEDOR_OPCIONAL')).toBeTrue();
      expect(servicio.listarUsuariosProformasSelector).toHaveBeenCalled();
      expect(component.opcionesUsuario.length).toBe(1);

      component.fechaInicio = '2026-10-01';
      component.fechaFin = '2026-10-31';
      component.idUsuario = null;
      expect(component.puedeGenerar()).toBeTrue();

      spyOn(Swal, 'fire');
      component.idUsuario = 2;
      component.generar();
      expect(servicio.generar).toHaveBeenCalledWith('MAYORISTAS_ENTREGADO', jasmine.objectContaining({ idUsuario: 2 }));
    });

    it('al elegir el reporte carga las rutas una sola vez y envía la ruta al generarlo', () => {
      const rutas = [{ valor: 1, etiqueta: 'RUTA 1', detalle: 'Ana' }];
      const servicio: any = {
        listarRutasSelector: jasmine.createSpy('listarRutasSelector').and.returnValue(of(rutas)),
        generar: jasmine.createSpy('generar').and.returnValue(throwError({ error: { message: 'x' } }))
      };
      const component = new ReportesComponent(admin, servicio);
      const reporte = component.reportes.find(item => item.codigo === 'MAYORISTAS_PROFORMAS_VENDEDOR');

      component.seleccionarReporte(reporte);
      component.seleccionarReporte(reporte);
      expect(servicio.listarRutasSelector).toHaveBeenCalledTimes(1);
      expect(component.opcionesRuta).toEqual(rutas);

      spyOn(Swal, 'fire');
      component.idRuta = 1;
      component.generar();
      expect(servicio.generar).toHaveBeenCalledWith('MAYORISTAS_PROFORMAS_VENDEDOR',
        jasmine.objectContaining({ idRuta: 1 }));
    });
  });
});
