import { fakeAsync, tick } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { ClienteDto } from '../../dtos/cliente-dto';
import { ReciboCajaDto } from '../../dtos/recibo-caja-dto';
import { RecibosComponent } from './recibos.component';
import Swal from 'sweetalert2';

describe('RecibosComponent', () => {
  let recibos: any;
  let clientes: any;
  let sucursales: any;
  let auth: any;
  let saldos: any;
  let component: RecibosComponent;

  function recibo(valores: Partial<ReciboCajaDto> = {}): ReciboCajaDto {
    return Object.assign(new ReciboCajaDto(), {
      idRecibo: 9, numeroVisible: 'S2-000015', cliente: 'Libreria Uno', monto: 100, formaPago: 'EFECTIVO', anulado: false
    }, valores);
  }

  function pagina(contenido: ReciboCajaDto[]): any {
    return { content: contenido, number: 0, totalPages: 1, totalElements: contenido.length, first: true, last: true };
  }

  function cliente(): ClienteDto {
    return Object.assign(new ClienteDto(), { idCliente: 5, nombre: 'Libreria Uno', nit: '4514627-6' });
  }

  async function esperar(): Promise<void> {
    await Promise.resolve();
    await Promise.resolve();
  }

  beforeEach(() => {
    recibos = {
      getListado: jasmine.createSpy('getListado').and.returnValue(of(pagina([recibo()]))),
      emitir: jasmine.createSpy('emitir').and.returnValue(of(recibo())),
      anular: jasmine.createSpy('anular').and.returnValue(of(recibo({ anulado: true }))),
      imprimir: jasmine.createSpy('imprimir').and.returnValue(of({ body: new Blob() }))
    };
    clientes = { getListado: jasmine.createSpy('getListado').and.returnValue(of({ content: [cliente()] })) };
    sucursales = { getSucursales: jasmine.createSpy('getSucursales').and.returnValue(of([])) };
    auth = { hasRole: (rol: string) => rol === 'ROLE_ADMIN' };
    saldos = { cliente: jasmine.createSpy('cliente').and.returnValue(of({ idCliente: 5, saldo: 40, pedidos: [] })) };
    component = new RecibosComponent(recibos, clientes, sucursales, auth, saldos);
    spyOn(window, 'open');
    spyOn(window.URL, 'createObjectURL').and.returnValue('blob:x');
  });

  it('carga la lista al iniciar y, si es administrador, las sucursales', () => {
    component.ngOnInit();

    expect(recibos.getListado).toHaveBeenCalled();
    expect(component.recibos.length).toBe(1);
    expect(sucursales.getSucursales).toHaveBeenCalled();
    component.ngOnDestroy();
  });

  it('el cobrador no pide las sucursales', () => {
    auth.hasRole = (rol: string) => rol === 'ROLE_COBRADOR';

    component.ngOnInit();

    expect(sucursales.getSucursales).not.toHaveBeenCalled();
    component.ngOnDestroy();
  });

  it('avisa si no pudo cargar la lista', () => {
    spyOn(Swal, 'fire');
    recibos.getListado.and.returnValue(throwError({ error: { message: 'Su usuario no tiene una sucursal asignada.' } }));

    component.cargar(0);

    expect(component.errorCarga).toBeTrue();
    expect(Swal.fire).toHaveBeenCalledWith('No se pudo cargar la lista', 'Su usuario no tiene una sucursal asignada.', 'error');
  });

  it('busca clientes con una pausa y desde dos letras', fakeAsync(() => {
    component.ngOnInit();

    component.onBuscarCliente('l');
    tick(300);
    expect(clientes.getListado).not.toHaveBeenCalled();

    component.onBuscarCliente('libre');
    tick(300);
    expect(clientes.getListado).toHaveBeenCalledWith(0, 8, 'libre', 'nombre', 'asc');
    expect(component.clientesEncontrados.length).toBe(1);
    component.ngOnDestroy();
  }));

  it('dice qué falta antes de poder emitir', () => {
    expect(component.faltante).toBe('Elige el cliente.');

    component.elegirCliente(cliente());
    expect(component.faltante).toBe('Indica el monto.');

    component.monto = 150;
    expect(component.faltante).toBe('');

    component.formaPago = 'DEPOSITO';
    expect(component.faltante).toBe('Indica el número de referencia.');
    component.referencia = ' 998877 ';
    expect(component.faltante).toBe('');
  });

  it('al escribir otra búsqueda se descarta el cliente elegido', () => {
    component.elegirCliente(cliente());

    component.onBuscarCliente('otro');

    expect(component.clienteElegido).toBeNull();
  });

  it('emitir manda los datos, solo manda la sucursal si es administrador, imprime y recarga', () => {
    component.elegirCliente(cliente());
    component.monto = 150;
    component.formaPago = 'TRANSFERENCIA';
    component.referencia = ' 998877 ';
    component.idSucursalEmision = 3;
    component.fechaPago = '2026-10-20';

    component.emitir();

    const enviado = recibos.emitir.calls.mostRecent().args[0];
    expect(enviado).toEqual({ idCliente: 5, idProforma: null, monto: 150, formaPago: 'TRANSFERENCIA', referencia: '998877', observaciones: null,
      fechaPago: '2026-10-20', idSucursal: 3 });
    expect(recibos.imprimir).toHaveBeenCalledWith(9);
    expect(component.formularioAbierto).toBeFalse();

    auth.hasRole = () => false;
    component.elegirCliente(cliente());
    component.monto = 10;
    component.idSucursalEmision = 3;
    component.emitir();
    expect(recibos.emitir.calls.mostRecent().args[0].idSucursal).toBeNull();
  });

  it('no emite con el formulario incompleto', () => {
    component.emitir();

    expect(recibos.emitir).not.toHaveBeenCalled();
  });

  it('si el servidor rechaza el recibo muestra el mensaje y deja intentar de nuevo', () => {
    spyOn(Swal, 'fire');
    recibos.emitir.and.returnValue(throwError({ error: { message: 'El monto del recibo debe ser mayor que cero.' } }));
    component.elegirCliente(cliente());
    component.monto = 150;

    component.emitir();

    expect(Swal.fire).toHaveBeenCalledWith('No se pudo emitir el recibo', 'El monto del recibo debe ser mayor que cero.', 'error');
    expect(component.emitiendo).toBeFalse();
  });

  it('anular pide el motivo y recarga; cancelar no llama', async () => {
    spyOn(Swal, 'fire').and.returnValue(Promise.resolve({ isConfirmed: false }) as any);
    component.anular(recibo());
    await esperar();
    expect(recibos.anular).not.toHaveBeenCalled();

    (Swal.fire as jasmine.Spy).and.callFake(((opciones: any) => opciones && opciones.input
      ? Promise.resolve({ isConfirmed: true, value: '  monto equivocado ' }) : Promise.resolve({ isConfirmed: true })) as any);
    component.anular(recibo());
    await esperar();

    expect(recibos.anular).toHaveBeenCalledWith(9, 'monto equivocado');
  });

  it('imprimir abre el PDF en otra pestaña', () => {
    component.imprimir(recibo());

    expect(window.open).toHaveBeenCalledWith('blob:x', '_blank');
  });

  it('al elegir el cliente trae su saldo y propone el monto del pedido de contado sin cobrar', () => {
    saldos.cliente.and.returnValue(of({ idCliente: 5, saldo: 0, pedidos: [
      { idProforma: 30, condicionPago: 'CONTADO', total: 500, cobrado: 200, pendiente: 0 },
      { idProforma: 31, condicionPago: 'CREDITO', total: 900, cobrado: 0, pendiente: 120 }] }));

    component.elegirCliente(cliente());
    component.sugerirMonto(30);
    expect(component.saldo.idCliente).toBe(5);
    expect(component.monto).toBe(300);

    component.sugerirMonto(31);
    expect(component.monto).toBe(120);
  });

  it('manda el pedido elegido y lo olvida al cambiar de cliente', () => {
    component.elegirCliente(cliente());
    component.idProformaElegida = 30;
    component.monto = 50;

    component.emitir();
    expect(recibos.emitir.calls.mostRecent().args[0].idProforma).toBe(30);

    component.elegirCliente(cliente());
    component.onBuscarCliente('otro');
    expect(component.idProformaElegida).toBeNull();
    expect(component.saldo).toBeNull();
  });

  it('limpiar los filtros vuelve a la lista por defecto', () => {
    component.desde = '2026-10-01';
    component.hasta = '2026-10-31';
    component.idSucursalFiltro = 2;
    component.incluirAnulados = true;

    component.limpiarFiltros();

    expect(component.desde).toBe('');
    expect(component.hasta).toBe('');
    expect(component.idSucursalFiltro).toBeNull();
    expect(component.incluirAnulados).toBeFalse();
    expect(recibos.getListado).toHaveBeenCalledWith(0, 10, { idSucursal: null, idCliente: null, desde: '', hasta: '', incluirAnulados: false });
  });
});
