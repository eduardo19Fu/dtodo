import { of, throwError } from 'rxjs';

import { PedidoDto } from '../../dtos/pedido-dto';
import { PedidosComponent } from './pedidos.component';
import Swal from 'sweetalert2';

describe('PedidosComponent', () => {
  let servicio: any;
  let component: PedidosComponent;

  function auth(...roles: string[]): any {
    return { hasRole: (rol: string) => roles.includes(rol) };
  }

  function pedido(valores: Partial<PedidoDto> = {}): PedidoDto {
    return Object.assign(new PedidoDto(), {
      idProforma: 7, noProforma: '123P', cliente: 'ANA PEREZ', negocio: 'LIBRERIA ELIZYOV', vendedor: 'Bryan Vargas',
      idRuta: 1, ruta: 'RUTA 1', estadoPedido: 'CONFIRMADA', unidades: 15
    }, valores);
  }

  function responderSwal(valor: string): void {
    spyOn(Swal, 'fire').and.callFake(((opciones: any) => opciones && opciones.input
      ? Promise.resolve({ isConfirmed: true, value: valor }) : Promise.resolve({ isConfirmed: true })) as any);
  }

  async function esperar(): Promise<void> {
    await Promise.resolve();
    await Promise.resolve();
  }

  beforeEach(() => {
    servicio = {
      listar: jasmine.createSpy('listar').and.returnValue(of([pedido(), pedido({ idProforma: 8, noProforma: '456P', unidades: 5 }),
        pedido({ idProforma: 9, noProforma: '789P', idRuta: 2, ruta: 'RUTA 2', unidades: 3 })])),
      detalle: jasmine.createSpy('detalle').and.returnValue(of({ idProforma: 7, noProforma: '123P', lineas: [], etapas: [] })),
      avanzar: jasmine.createSpy('avanzar').and.returnValue(of({})),
      retroceder: jasmine.createSpy('retroceder').and.returnValue(of({})),
      picking: jasmine.createSpy('picking').and.returnValue(of(new Blob(['x']))),
      empaque: jasmine.createSpy('empaque').and.returnValue(of(new Blob(['x']))),
      notaEnvio: jasmine.createSpy('notaEnvio').and.returnValue(of(new Blob(['x'])))
    };
    component = new PedidosComponent(servicio, auth('ROLE_BODEGA'));
  });

  it('agrupa los pedidos por ruta con sus unidades', () => {
    component.ngOnInit();

    const grupos = component.grupos;
    expect(grupos.length).toBe(2);
    expect(grupos[0].ruta).toBe('RUTA 1');
    expect(grupos[0].pedidos.length).toBe(2);
    expect(grupos[0].unidades).toBe(20);
    expect(grupos[1].unidades).toBe(3);
    expect(component.rutas.map(ruta => ruta.nombre)).toEqual(['RUTA 1', 'RUTA 2']);
  });

  it('filtra por ruta en pantalla y por etapa y fechas en el servidor', () => {
    component.ngOnInit();
    component.idRuta = 2;
    expect(component.grupos.length).toBe(1);
    expect(component.grupos[0].ruta).toBe('RUTA 2');

    component.estado = 'EMPACADA';
    component.fechaInicio = '2026-10-01';
    component.fechaFin = '2026-10-31';
    component.cargar();
    expect(servicio.listar).toHaveBeenCalledWith('EMPACADA', '2026-10-01', '2026-10-31');
  });

  it('si la ruta elegida ya no aparece vuelve a todas', () => {
    component.ngOnInit();
    component.idRuta = 2;
    servicio.listar.and.returnValue(of([pedido()]));

    component.cargar();

    expect(component.idRuta as any).toBe('');
  });

  it('un error de carga lo indica y deja de cargar', () => {
    servicio.listar.and.returnValue(throwError({ status: 500 }));

    component.cargar();

    expect(component.errorCarga).toBeTrue();
    expect(component.cargando).toBeFalse();
  });

  it('bodega y el administrador operan; el vendedor solo consulta', () => {
    expect(component.puedeOperar).toBeTrue();
    expect(component.esAdmin).toBeFalse();
    expect(component.puedeAvanzar(pedido())).toBeTrue();
    expect(component.puedeRetroceder(pedido({ estadoPedido: 'EMPACADA' }))).toBeFalse();

    const vendedor = new PedidosComponent(servicio, auth('ROLE_VENDEDOR'));
    expect(vendedor.puedeOperar).toBeFalse();
    expect(vendedor.puedeAvanzar(pedido())).toBeFalse();

    const admin = new PedidosComponent(servicio, auth('ROLE_ADMIN'));
    expect(admin.puedeRetroceder(pedido({ estadoPedido: 'EMPACADA' }))).toBeTrue();
    expect(admin.puedeRetroceder(pedido({ estadoPedido: 'CONFIRMADA' }))).toBeFalse();
    expect(admin.puedeRetroceder(pedido({ estadoPedido: 'ENTREGADA' }))).toBeFalse();
  });

  it('el texto del botón sigue la etapa y no hay avance desde ruta ni con el descuento pendiente', () => {
    expect(component.textoAvance(pedido({ estadoPedido: 'CONFIRMADA' }))).toBe('Iniciar picking');
    expect(component.textoAvance(pedido({ estadoPedido: 'EN_PREPARACION' }))).toBe('Marcar empacado');
    expect(component.textoAvance(pedido({ estadoPedido: 'EMPACADA' }))).toBe('Cargar y salir a ruta');
    expect(component.puedeAvanzar(pedido({ estadoPedido: 'EN_RUTA' }))).toBeFalse();
    expect(component.puedeAvanzar(pedido({ estadoAutorizacion: 'PENDIENTE' }))).toBeFalse();
    expect(component.puedeAvanzar(pedido({ estadoAutorizacion: 'RECHAZADA' }))).toBeFalse();
    expect(component.puedeAvanzar(pedido({ estadoAutorizacion: 'APROBADA' }))).toBeTrue();
  });

  it('avanzar envía el responsable limpio y recarga la lista', async () => {
    responderSwal('  Irene Ortega ');
    component.cargar();
    servicio.listar.calls.reset();

    component.avanzar(pedido());
    await esperar();

    expect(servicio.avanzar).toHaveBeenCalledWith(7, 'Irene Ortega');
    expect(servicio.listar).toHaveBeenCalledTimes(1);
    expect(component.procesando).toBeNull();
  });

  it('si se cancela el cuadro no se avanza', async () => {
    spyOn(Swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: false } as any));

    component.avanzar(pedido());
    await esperar();

    expect(servicio.avanzar).not.toHaveBeenCalled();
  });

  it('un 422 al avanzar se muestra y la lista se recarga', async () => {
    servicio.avanzar.and.returnValue(throwError({ error: { message: 'pendiente de autorización' } }));
    responderSwal('');
    servicio.listar.calls.reset();

    component.avanzar(pedido());
    await esperar();

    expect(Swal.fire).toHaveBeenCalledWith('No se pudo actualizar el pedido', 'pendiente de autorización', 'error');
    expect(servicio.listar).toHaveBeenCalled();
    expect(component.procesando).toBeNull();
  });

  it('retroceder pide el motivo y lo envía', async () => {
    responderSwal(' faltó un producto ');
    component = new PedidosComponent(servicio, auth('ROLE_ADMIN'));

    component.retroceder(pedido({ estadoPedido: 'EMPACADA' }));
    await esperar();

    expect(servicio.retroceder).toHaveBeenCalledWith(7, 'faltó un producto');
  });

  it('abre el detalle y lo cierra', () => {
    component.verDetalle(pedido());
    expect(servicio.detalle).toHaveBeenCalledWith(7);
    expect(component.detalle.noProforma).toBe('123P');

    component.cerrarDetalle();
    expect(component.detalle).toBeNull();
  });

  it('imprime los documentos del pedido y la nota de envío de la ruta', () => {
    spyOn(window, 'open');
    component.ngOnInit();

    component.imprimirPicking(pedido());
    component.imprimirEmpaque(pedido());
    component.imprimirNotaEnvio(component.grupos[0]);

    expect(servicio.picking).toHaveBeenCalledWith(7);
    expect(servicio.empaque).toHaveBeenCalledWith(7);
    expect(servicio.notaEnvio).toHaveBeenCalledWith(1);
    expect(window.open).toHaveBeenCalledTimes(3);
  });

  it('la nota de envío necesita una ruta', () => {
    const fire = spyOn(Swal, 'fire');

    component.imprimirNotaEnvio({ idRuta: null, ruta: 'Sin ruta', pedidos: [], unidades: 0 });

    expect(fire).toHaveBeenCalled();
    expect(servicio.notaEnvio).not.toHaveBeenCalled();
  });
});
