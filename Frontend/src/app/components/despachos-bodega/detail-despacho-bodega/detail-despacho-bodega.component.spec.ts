import { DetailDespachoBodegaComponent } from './detail-despacho-bodega.component';

describe('DetailDespachoBodegaComponent', () => {
  let component: DetailDespachoBodegaComponent;

  beforeEach(() => {
    component = new DetailDespachoBodegaComponent({ nativeElement: document.createElement('div') } as any, document);
    component.despacho = {
      idDespacho: 12,
      estado: 'PENDIENTE',
      sucursalDestino: { idSucursal: 2, nombre: 'Norte' },
      items: [{ cantidad: 4 }, { cantidad: 6 }]
    } as any;
  });

  it('muestra el número con ceros a la izquierda', () => {
    expect(component.numero).toBe('000012');
  });

  it('suma las unidades de todas las líneas', () => {
    expect(component.totalUnidades).toBe(10);
  });

  it('presenta etiqueta, clase e icono según el estado', () => {
    expect(component.etiquetaEstado).toBe('Pendiente de aprobación');
    expect(component.claseEstado).toBe('status-pending');
    expect(component.iconoEstado).toBe('fa-hourglass-half');
    expect(component.pendiente).toBeTrue();

    component.despacho.estado = 'REALIZADO';
    expect(component.claseEstado).toBe('status-done');
    expect(component.pendiente).toBeFalse();

    component.despacho.estado = 'CANCELADO';
    expect(component.claseEstado).toBe('status-cancelled');
    expect(component.iconoEstado).toBe('fa-ban');
  });

  it('arma el nombre completo del usuario o usa su nombre de usuario', () => {
    expect(component.nombreCompleto({ primerNombre: 'Ana', apellido: 'Gómez', usuario: 'agomez' } as any)).toBe('Ana Gómez');
    expect(component.nombreCompleto({ usuario: 'agomez' } as any)).toBe('agomez');
    expect(component.nombreCompleto(null)).toBe('No registrado');
  });

  it('solicita la aprobación indicando la sucursal destino', () => {
    let solicitud: any;
    component.aprobar.subscribe(valor => solicitud = valor);

    component.solicitarAprobacion();

    expect(solicitud).toEqual({ idDespacho: 12, sucursalDestino: 'Norte' });
  });

  it('cierra al hacer clic en el fondo pero no en el contenido', () => {
    let cierres = 0;
    component.cerrar.subscribe(() => cierres++);
    const fondo = document.createElement('div');

    component.cerrarDesdeBackdrop({ target: fondo, currentTarget: fondo } as unknown as MouseEvent);
    component.cerrarDesdeBackdrop({ target: document.createElement('span'), currentTarget: fondo } as unknown as MouseEvent);

    expect(cierres).toBe(1);
  });

  it('un pendiente se cancela y un aprobado solo lo revierte quien puede aprobar', () => {
    expect(component.puedeCancelar).toBeTrue();

    component.despacho.estado = 'REALIZADO';
    component.puedeAprobar = false;
    expect(component.puedeCancelar).toBeFalse();

    component.puedeAprobar = true;
    expect(component.puedeCancelar).toBeTrue();

    component.despacho.estado = 'CANCELADO';
    expect(component.puedeCancelar).toBeFalse();
  });

  it('solicita la cancelación indicando el estado actual del despacho', () => {
    let solicitud: any;
    component.cancelar.subscribe(valor => solicitud = valor);
    component.despacho.estado = 'REALIZADO';

    component.solicitarCancelacion();

    expect(solicitud).toEqual({ idDespacho: 12, estado: 'REALIZADO' });
  });
});
