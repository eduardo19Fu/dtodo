import { of, throwError } from 'rxjs';

import { PedidoDetalleDto, PedidoDto } from '../../dtos/pedido-dto';
import { PedidosComponent } from './pedidos.component';
import Swal from 'sweetalert2';

let saldoServicio: any;

beforeEach(() => {
  saldoServicio = { pedido: jasmine.createSpy('pedido').and.returnValue(of({ condicionPago: 'CONTADO', entregado: 100, cobrado: 40 })) };
});

describe('PedidosComponent', () => {
  let servicio: any;
  let component: PedidosComponent;

  function auth(...roles: string[]): any {
    return { hasRole: (rol: string) => roles.includes(rol), esRepartidor: () => roles.length === 1 && roles[0] === 'ROLE_REPARTIDOR' };
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
    component = new PedidosComponent(servicio, auth('ROLE_BODEGA'), saldoServicio);
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

    const vendedor = new PedidosComponent(servicio, auth('ROLE_VENDEDOR'), saldoServicio);
    expect(vendedor.puedeOperar).toBeFalse();
    expect(vendedor.puedeAvanzar(pedido())).toBeFalse();

    const admin = new PedidosComponent(servicio, auth('ROLE_ADMIN'), saldoServicio);
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
    component = new PedidosComponent(servicio, auth('ROLE_ADMIN'), saldoServicio);

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

describe('PedidosComponent - entrega', () => {
  let servicio: any;
  let component: PedidosComponent;

  function auth(...roles: string[]): any {
    return { hasRole: (rol: string) => roles.includes(rol), esRepartidor: () => roles.length === 1 && roles[0] === 'ROLE_REPARTIDOR' };
  }

  function detalle(valores: Partial<PedidoDetalleDto> = {}): PedidoDetalleDto {
    return Object.assign(new PedidoDetalleDto(), {
      idProforma: 7, noProforma: '123P', estadoPedido: 'EN_RUTA', idSucursal: 1,
      lineas: [
        { idDetalle: 1, codigo: 'A-1', producto: 'CUADERNO', cantidad: 10, cantidadEntregada: 0, pendiente: 10 },
        { idDetalle: 2, codigo: 'B-2', producto: 'REGLA', cantidad: 5, cantidadEntregada: 0, pendiente: 5 }
      ],
      entregas: []
    }, valores);
  }

  function pedido(valores: Partial<PedidoDto> = {}): PedidoDto {
    return Object.assign(new PedidoDto(), { idProforma: 7, noProforma: '123P', estadoPedido: 'EN_RUTA', ruta: 'RUTA 1', idRuta: 1,
      unidades: 15 }, valores);
  }

  async function esperar(): Promise<void> {
    await Promise.resolve();
    await Promise.resolve();
  }

  beforeEach(() => {
    servicio = {
      listar: jasmine.createSpy('listar').and.returnValue(of([])),
      detalle: jasmine.createSpy('detalle').and.returnValue(of(detalle())),
      sucursales: jasmine.createSpy('sucursales').and.returnValue(of([{ valor: 1, etiqueta: 'Central' }])),
      registrarEntrega: jasmine.createSpy('registrarEntrega').and.returnValue(of(detalle({ estadoPedido: 'ENTREGADA' }))),
      anularEntrega: jasmine.createSpy('anularEntrega').and.returnValue(of(detalle())),
      cerrarConLoEntregado: jasmine.createSpy('cerrarConLoEntregado').and.returnValue(of(detalle({ estadoPedido: 'ENTREGADA' })))
    };
    component = new PedidosComponent(servicio, auth('ROLE_BODEGA'), saldoServicio);
    component.detalle = detalle();
  });

  it('bodega, el administrador y el vendedor registran entregas de pedidos en ruta o parciales', () => {
    expect(component.puedeEntregar(pedido())).toBeTrue();
    expect(component.puedeEntregar(pedido({ estadoPedido: 'ENTREGADA_PARCIAL' }))).toBeTrue();
    expect(component.puedeEntregar(pedido({ estadoPedido: 'EMPACADA' }))).toBeFalse();
    expect(component.puedeEntregar(pedido({ estadoPedido: 'ENTREGADA' }))).toBeFalse();
    expect(component.puedeEntregar(pedido({ estadoAutorizacion: 'PENDIENTE' }))).toBeFalse();
    expect(new PedidosComponent(servicio, auth('ROLE_VENDEDOR'), saldoServicio).puedeEntregar(pedido())).toBeTrue();
    expect(new PedidosComponent(servicio, auth('ROLE_COBRADOR'), saldoServicio).puedeEntregar(pedido())).toBeFalse();
  });

  it('abrir la entrega propone la sucursal del pedido, hoy y todo lo pendiente', () => {
    component.abrirEntrega();

    expect(component.registrandoEntrega).toBeTrue();
    expect(component.entrega.idSucursal).toBe(1);
    expect(component.entrega.fechaEntrega).toBe(component.hoy);
    expect(component.cantidades[1]).toBe(10);
    expect(component.cantidades[2]).toBe(5);
    expect(servicio.sucursales).toHaveBeenCalledTimes(1);

    component.abrirEntrega();
    expect(servicio.sucursales).toHaveBeenCalledTimes(1);
  });

  it('valida sucursal, fecha y cantidades antes de enviar', () => {
    component.abrirEntrega();

    component.entrega.idSucursal = null;
    expect(component.validarEntrega()).toContain('sucursal');
    component.entrega.idSucursal = 1;

    component.entrega.fechaEntrega = '2000-01-01';
    expect(component.validarEntrega()).toContain('fecha');
    component.entrega.fechaEntrega = component.hoy;

    component.cantidades[1] = 11;
    expect(component.validarEntrega()).toContain('solo quedan 10');
    component.cantidades[1] = -1;
    expect(component.validarEntrega()).toContain('no es válida');

    component.cantidades[1] = 0;
    component.cantidades[2] = 0;
    expect(component.validarEntrega()).toContain('al menos un producto');

    component.cantidades[1] = 4;
    expect(component.validarEntrega()).toBe('');
  });

  it('confirmar envía solo las líneas con cantidad y recarga la lista', () => {
    component.abrirEntrega();
    component.cantidades[1] = 4;
    component.cantidades[2] = 0;
    component.entrega.entregadoPor = ' David Aguilar ';
    component.entrega.descontadoAMano = true;
    component.entrega.observaciones = '';

    component.confirmarEntrega();

    expect(servicio.registrarEntrega).toHaveBeenCalledWith(7, {
      idSucursal: 1, fechaEntrega: component.hoy, entregadoPor: 'David Aguilar', descontadoAMano: true, entregarSinPago: false,
      observaciones: null,
      lineas: [{ idDetalle: 1, cantidad: 4 }]
    });
    expect(component.registrandoEntrega).toBeFalse();
    expect(component.detalle.estadoPedido).toBe('ENTREGADA');
    expect(servicio.listar).toHaveBeenCalled();
  });

  it('un error del servidor se muestra en el formulario y lo deja abierto', () => {
    servicio.registrarEntrega.and.returnValue(throwError({ error: { message: 'No hay existencias suficientes de CUADERNO' } }));
    component.abrirEntrega();

    component.confirmarEntrega();

    expect(component.errorEntrega).toContain('existencias');
    expect(component.registrandoEntrega).toBeTrue();
    expect(component.guardandoEntrega).toBeFalse();
  });

  it('con un dato inválido no llama al servidor', () => {
    component.abrirEntrega();
    component.cantidades[1] = 0;
    component.cantidades[2] = 0;

    component.confirmarEntrega();

    expect(servicio.registrarEntrega).not.toHaveBeenCalled();
    expect(component.errorEntrega).toBeTruthy();
  });

  it('desde la lista abre el detalle y el formulario de entrega', () => {
    component.detalle = null;

    component.verDetalle(pedido(), true);

    expect(servicio.detalle).toHaveBeenCalledWith(7);
    expect(component.registrandoEntrega).toBeTrue();
  });

  it('el administrador anula una entrega y cierra un pedido parcial con motivo', async () => {
    component = new PedidosComponent(servicio, auth('ROLE_ADMIN'), saldoServicio);
    component.detalle = detalle({ estadoPedido: 'ENTREGADA_PARCIAL' });
    spyOn(Swal, 'fire').and.callFake(((opciones: any) => opciones && opciones.input
      ? Promise.resolve({ isConfirmed: true, value: ' se registró de más ' }) : Promise.resolve({ isConfirmed: true })) as any);

    component.anularEntrega({ idEntrega: 50 } as any);
    await esperar();
    expect(servicio.anularEntrega).toHaveBeenCalledWith(7, 50, 'se registró de más');

    component.cerrarPedido();
    await esperar();
    expect(servicio.cerrarConLoEntregado).toHaveBeenCalledWith(7, 'se registró de más');
  });

  it('si se cancela el motivo no se anula la entrega', async () => {
    component = new PedidosComponent(servicio, auth('ROLE_ADMIN'), saldoServicio);
    component.detalle = detalle();
    spyOn(Swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: false } as any));

    component.anularEntrega({ idEntrega: 50 } as any);
    await esperar();

    expect(servicio.anularEntrega).not.toHaveBeenCalled();
  });

  it('la fecha mínima de entrega es hace 7 días', () => {
    const minima = new Date(component.fechaMinimaEntrega + 'T00:00:00');
    const hoy = new Date(component.hoy + 'T00:00:00');
    expect(Math.round((hoy.getTime() - minima.getTime()) / 86400000)).toBe(7);
  });
});

describe('PedidosComponent - repartidor y piloto', () => {
  let servicio: any;

  function auth(...roles: string[]): any {
    return { hasRole: (rol: string) => roles.includes(rol), esRepartidor: () => roles.length === 1 && roles[0] === 'ROLE_REPARTIDOR' };
  }

  function pedido(valores: Partial<PedidoDto> = {}): PedidoDto {
    return Object.assign(new PedidoDto(), { idProforma: 7, noProforma: '123P', estadoPedido: 'EMPACADA', ruta: 'RUTA 1', idRuta: 1,
      unidades: 15 }, valores);
  }

  async function esperar(): Promise<void> {
    for (let i = 0; i < 4; i++) {
      await Promise.resolve();
    }
  }

  beforeEach(() => {
    servicio = {
      listar: jasmine.createSpy('listar').and.returnValue(of([])),
      avanzar: jasmine.createSpy('avanzar').and.returnValue(of({})),
      asignarRepartidor: jasmine.createSpy('asignarRepartidor').and.returnValue(of({})),
      repartidores: jasmine.createSpy('repartidores').and.returnValue(of([
        { valor: 21, etiqueta: 'David Aguilar' }, { valor: 22, etiqueta: 'Denis Reynoso' }])),
      sucursales: jasmine.createSpy('sucursales').and.returnValue(of([])),
      detalle: jasmine.createSpy('detalle').and.returnValue(of({ idProforma: 7, estadoPedido: 'EN_RUTA', idSucursal: 1, lineas: [], etapas: [], entregas: [] })),
      registrarEntrega: jasmine.createSpy('registrarEntrega').and.returnValue(of({ estadoPedido: 'ENTREGADA', lineas: [], etapas: [], entregas: [] }))
    };
  });

  it('cargar a ruta pide elegir el piloto y lo envía', async () => {
    const component = new PedidosComponent(servicio, auth('ROLE_BODEGA'), saldoServicio);
    spyOn(Swal, 'fire').and.callFake(((opciones: any) => Promise.resolve({ isConfirmed: true, value: opciones.input === 'select' ? '22' : '' })) as any);

    component.avanzar(pedido());
    await esperar();

    expect(servicio.repartidores).toHaveBeenCalled();
    expect(servicio.avanzar).toHaveBeenCalledWith(7, undefined, 22);
  });

  it('si se cancela la elección del piloto el pedido no avanza', async () => {
    const component = new PedidosComponent(servicio, auth('ROLE_BODEGA'), saldoServicio);
    spyOn(Swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: false } as any));

    component.avanzar(pedido());
    await esperar();

    expect(servicio.avanzar).not.toHaveBeenCalled();
  });

  it('sin repartidores avisa y no avanza', async () => {
    servicio.repartidores.and.returnValue(of([]));
    const component = new PedidosComponent(servicio, auth('ROLE_BODEGA'), saldoServicio);
    const fire = spyOn(Swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: true } as any));

    component.avanzar(pedido());
    await esperar();

    expect(fire).toHaveBeenCalledWith('No hay repartidores', jasmine.any(String), 'info');
    expect(servicio.avanzar).not.toHaveBeenCalled();
  });

  it('las demás etapas siguen sin pedir piloto', async () => {
    const component = new PedidosComponent(servicio, auth('ROLE_BODEGA'), saldoServicio);
    spyOn(Swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: true, value: 'Irene' } as any));

    component.avanzar(pedido({ estadoPedido: 'CONFIRMADA' }));
    await esperar();

    expect(servicio.repartidores).not.toHaveBeenCalled();
    expect(servicio.avanzar).toHaveBeenCalledWith(7, 'Irene');
  });

  it('bodega y el administrador cambian el piloto de un pedido en ruta', async () => {
    const component = new PedidosComponent(servicio, auth('ROLE_BODEGA'), saldoServicio);
    expect(component.puedeCambiarPiloto(pedido({ estadoPedido: 'EN_RUTA' }))).toBeTrue();
    expect(component.puedeCambiarPiloto(pedido({ estadoPedido: 'ENTREGADA_PARCIAL' }))).toBeTrue();
    expect(component.puedeCambiarPiloto(pedido({ estadoPedido: 'EMPACADA' }))).toBeFalse();
    expect(new PedidosComponent(servicio, auth('ROLE_REPARTIDOR'), saldoServicio).puedeCambiarPiloto(pedido({ estadoPedido: 'EN_RUTA' }))).toBeFalse();
    spyOn(Swal, 'fire').and.callFake(((opciones: any) => Promise.resolve({ isConfirmed: true, value: '21' })) as any);

    component.cambiarPiloto(pedido({ estadoPedido: 'EN_RUTA' }));
    await esperar();

    expect(servicio.asignarRepartidor).toHaveBeenCalledWith(7, 21);
  });

  it('el repartidor solo entrega: no opera etapas ni cambia el piloto ni retrocede', () => {
    const component = new PedidosComponent(servicio, auth('ROLE_REPARTIDOR'), saldoServicio);
    const enRuta = pedido({ estadoPedido: 'EN_RUTA' });

    expect(component.esRepartidor).toBeTrue();
    expect(component.puedeOperar).toBeFalse();
    expect(component.puedeEntregar(enRuta)).toBeTrue();
    expect(component.puedeAvanzar(pedido({ estadoPedido: 'CONFIRMADA' }))).toBeFalse();
    expect(component.puedeRetroceder(enRuta)).toBeFalse();
    expect(component.puedeEntregar(pedido({ estadoPedido: 'EMPACADA' }))).toBeFalse();
  });

  it('el repartidor registra la entrega sin elegir sucursal ni usar la casilla', () => {
    const component = new PedidosComponent(servicio, auth('ROLE_REPARTIDOR'), saldoServicio);
    component.detalle = Object.assign(new PedidoDetalleDto(), { idProforma: 7, estadoPedido: 'EN_RUTA', idSucursal: 1,
      lineas: [{ idDetalle: 1, codigo: 'A', producto: 'CUADERNO', cantidad: 10, cantidadEntregada: 0, pendiente: 10 }], entregas: [] });

    component.abrirEntrega();
    expect(servicio.sucursales).not.toHaveBeenCalled();
    component.entrega.idSucursal = null;
    component.entrega.descontadoAMano = true;
    component.entrega.entregadoPor = 'Otro';
    expect(component.validarEntrega()).toBe('');

    component.confirmarEntrega();

    expect(servicio.registrarEntrega).toHaveBeenCalledWith(7, jasmine.objectContaining({
      descontadoAMano: false, entregadoPor: null, lineas: [{ idDetalle: 1, cantidad: 10 }] }));
  });

  it('quien puede ver dinero consulta el cobro del pedido y bodega no', () => {
    const admin = new PedidosComponent(servicio, auth('ROLE_ADMIN'), saldoServicio);
    admin.verDetalle(pedido());
    expect(saldoServicio.pedido).toHaveBeenCalledWith(7);
    expect(admin.cobro.cobrado).toBe(40);

    saldoServicio.pedido.calls.reset();
    const bodega = new PedidosComponent(servicio, auth('ROLE_BODEGA'), saldoServicio);
    bodega.verDetalle(pedido());
    expect(saldoServicio.pedido).not.toHaveBeenCalled();
    expect(bodega.cobro).toBeNull();
  });

  it('calcula lo que falta cobrar solo en un pedido de contado', () => {
    const admin = new PedidosComponent(servicio, auth('ROLE_ADMIN'), saldoServicio);
    admin.verDetalle(pedido());
    expect(admin.faltaCobrar).toBe(60);

    admin.cobro = { condicionPago: 'CREDITO', entregado: 100, cobrado: 0 } as any;
    expect(admin.faltaCobrar).toBe(0);
  });

  it('solo el administrador manda entregar sin cobro', () => {
    servicio.registrarEntrega = jasmine.createSpy('registrarEntrega').and.returnValue(of({ idProforma: 7, lineas: [], etapas: [] }));
    const admin = new PedidosComponent(servicio, auth('ROLE_ADMIN'), saldoServicio);
    const detalleAbierto = { idProforma: 7, lineas: [{ idDetalle: 1, producto: 'X', pendiente: 5 }] } as any;
    admin.detalle = detalleAbierto;
    admin.cantidades = { 1: 2 } as any;
    admin.entrega = { idSucursal: 1, fechaEntrega: admin.hoy, entregadoPor: '', descontadoAMano: false, observaciones: '',
      entregarSinPago: true };
    admin.confirmarEntrega();
    expect(servicio.registrarEntrega.calls.mostRecent().args[1].entregarSinPago).toBeTrue();

    const bodega = new PedidosComponent(servicio, auth('ROLE_BODEGA'), saldoServicio);
    bodega.detalle = detalleAbierto;
    bodega.cantidades = { 1: 2 } as any;
    bodega.entrega = { ...admin.entrega };
    bodega.confirmarEntrega();
    expect(servicio.registrarEntrega.calls.mostRecent().args[1].entregarSinPago).toBeFalse();
  });
});
