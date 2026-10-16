import { DetailDevolucionBodegaComponent } from './detail-devolucion-bodega.component';

describe('DetailDevolucionBodegaComponent', () => {
  let component: DetailDevolucionBodegaComponent;

  beforeEach(() => {
    component = new DetailDevolucionBodegaComponent({ nativeElement: document.createElement('div') } as any, document);
    component.devolucion = {
      idDevolucion: 12,
      estado: 'PENDIENTE',
      sucursalOrigen: { idSucursal: 2, nombre: 'Norte' },
      bodega: { idBodega: 1, nombre: 'Principal' },
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
    expect(component.rechazada).toBeFalse();

    component.devolucion.estado = 'REALIZADO';
    expect(component.etiquetaEstado).toBe('Aprobada');
    expect(component.claseEstado).toBe('status-done');
    expect(component.pendiente).toBeFalse();

    component.devolucion.estado = 'CANCELADO';
    expect(component.etiquetaEstado).toBe('Rechazada');
    expect(component.claseEstado).toBe('status-cancelled');
    expect(component.iconoEstado).toBe('fa-ban');
    expect(component.rechazada).toBeTrue();
  });

  it('arma el nombre completo del usuario o usa su nombre de usuario', () => {
    expect(component.nombreCompleto({ primerNombre: 'Ana', apellido: 'Gómez', usuario: 'agomez' } as any)).toBe('Ana Gómez');
    expect(component.nombreCompleto({ usuario: 'agomez' } as any)).toBe('agomez');
    expect(component.nombreCompleto(null)).toBe('No registrado');
  });

  it('solicita la aprobación indicando la bodega que recibe', () => {
    let solicitud: any;
    component.aprobar.subscribe(valor => solicitud = valor);

    component.solicitarAprobacion();

    expect(solicitud).toEqual({ idDevolucion: 12, bodega: 'Principal' });
  });

  it('solicita el rechazo indicando la tienda a la que regresa el stock', () => {
    let solicitud: any;
    component.rechazar.subscribe(valor => solicitud = valor);

    component.solicitarRechazo();

    expect(solicitud).toEqual({ idDevolucion: 12, sucursal: 'Norte' });
  });

  it('cierra al hacer clic en el fondo pero no en el contenido', () => {
    let cierres = 0;
    component.cerrar.subscribe(() => cierres++);
    const fondo = document.createElement('div');

    component.cerrarDesdeBackdrop({ target: fondo, currentTarget: fondo } as unknown as MouseEvent);
    component.cerrarDesdeBackdrop({ target: document.createElement('span'), currentTarget: fondo } as unknown as MouseEvent);

    expect(cierres).toBe(1);
  });
});
