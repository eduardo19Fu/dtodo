import { of } from 'rxjs';
import { ComprasComponent } from './compras.component';

describe('ComprasComponent - paginacion', () => {
  let component: ComprasComponent;

  beforeEach(() => {
    component = new ComprasComponent(null, null, { hasRole: () => true } as any);
  });

  it('calcula las paginas visibles centradas en la pagina actual', () => {
    component.totalPaginas = 10;
    component.paginaActual = 5;

    expect(component.paginasVisibles).toEqual([3, 4, 5, 6, 7]);
  });

  it('no muestra paginas negativas cuando la pagina actual esta al inicio', () => {
    component.totalPaginas = 10;
    component.paginaActual = 0;

    expect(component.paginasVisibles).toEqual([0, 1, 2, 3, 4]);
  });
});

describe('ComprasComponent - abrirDetalle', () => {
  let component: ComprasComponent;
  let compraService: any;
  let detailCompraService: any;

  beforeEach(() => {
    compraService = { getCompra: jasmine.createSpy('getCompra') };
    detailCompraService = { abrirModal: jasmine.createSpy('abrirModal') };
    component = new ComprasComponent(compraService, detailCompraService, { hasRole: () => true } as any);
  });

  it('consulta la compra seleccionada y abre el modal de detalle', () => {
    const compraDetalle: any = { idCompra: 5 };
    compraService.getCompra.and.returnValue(of(compraDetalle));

    component.abrirDetalle({ idCompra: 5 } as any);

    expect(compraService.getCompra).toHaveBeenCalledWith(5);
    expect(component.compraSeleccionada).toBe(compraDetalle);
    expect(component.detalleCargandoId).toBeNull();
    expect(detailCompraService.abrirModal).toHaveBeenCalled();
  });

  it('no dispara una nueva consulta de detalle si ya hay una cargando', () => {
    component.detalleCargandoId = 1;

    component.abrirDetalle({ idCompra: 5 } as any);

    expect(compraService.getCompra).not.toHaveBeenCalled();
  });
});

describe('ComprasComponent - anular', () => {
  let component: ComprasComponent;
  let compraService: any;

  beforeEach(() => {
    compraService = { anular: jasmine.createSpy('anular') };
    component = new ComprasComponent(compraService, null, { hasRole: () => true, usuario: { idUsuario: 1 } } as any);
  });

  it('no dispara una nueva anulacion si ya hay una en curso', () => {
    component.anulandoId = 7;

    component.anular({ idCompra: 7 } as any);

    expect(compraService.anular).not.toHaveBeenCalled();
  });
});
