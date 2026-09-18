import { fakeAsync, tick } from '@angular/core/testing';
import { ComprasComponent } from './compras.component';
import swal from 'sweetalert2';

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

  it('presenta el tipo de comprobante con una etiqueta legible', () => {
    expect(component.formatearTipoComprobante('NOTA_ENVIO')).toBe('Nota de envío');
    expect(component.formatearTipoComprobante('FACTURA')).toBe('Factura');
  });
});

describe('ComprasComponent - abrirDetalle', () => {
  let component: ComprasComponent;

  beforeEach(() => {
    spyOn(swal, 'fire');
    component = new ComprasComponent(null, null, { hasRole: () => true } as any);
  });

  it('asigna la compra seleccionada para que el modal de detalle cargue su propio detalle', fakeAsync(() => {
    const compraDto: any = { idCompra: 5 };

    component.abrirDetalle(compraDto);
    tick();

    expect(component.compraSeleccionada).toBe(compraDto);
  }));
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
