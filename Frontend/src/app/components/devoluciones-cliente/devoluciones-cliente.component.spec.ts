import { fakeAsync, tick } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { ClienteDto } from '../../dtos/cliente-dto';
import { DevolucionClienteDto, PedidoDevolubleDto } from '../../dtos/devolucion-cliente-dto';
import { DevolucionesClienteComponent } from './devoluciones-cliente.component';
import Swal from 'sweetalert2';

describe('DevolucionesClienteComponent', () => {
  let devoluciones: any;
  let clientes: any;
  let sucursales: any;
  let component: DevolucionesClienteComponent;

  function cliente(): ClienteDto {
    return Object.assign(new ClienteDto(), { idCliente: 5, nombre: 'Libreria Uno', nit: '4514627-6' });
  }

  function pedido(valores: Partial<PedidoDevolubleDto> = {}): PedidoDevolubleDto {
    return Object.assign(new PedidoDevolubleDto(), {
      idProforma: 7, noProforma: 'P-7', estadoPedido: 'ENTREGADA', idSucursal: 1, sucursal: 'Central', facturado: false,
      lineas: [
        { idDetalle: 1, producto: 'CUADERNO', entregada: 8, devuelta: 2, devolvible: 6, precioUnitario: 10 },
        { idDetalle: 2, producto: 'REGLA', entregada: 5, devuelta: 5, devolvible: 0, precioUnitario: 5 }
      ],
      notasCredito: []
    }, valores);
  }

  function devolucion(): DevolucionClienteDto {
    return Object.assign(new DevolucionClienteDto(), { idDevolucion: 50, cliente: 'Libreria Uno', total: 30, lineas: [] });
  }

  function pagina(contenido: DevolucionClienteDto[]): any {
    return { content: contenido, number: 0, totalPages: 1, totalElements: contenido.length, first: true, last: true };
  }

  async function esperar(): Promise<void> {
    await Promise.resolve();
    await Promise.resolve();
  }

  beforeEach(() => {
    devoluciones = {
      getListado: jasmine.createSpy('getListado').and.returnValue(of(pagina([devolucion()]))),
      buscar: jasmine.createSpy('buscar').and.returnValue(of(devolucion())),
      pedidosDevolubles: jasmine.createSpy('pedidosDevolubles').and.returnValue(of([pedido()])),
      registrar: jasmine.createSpy('registrar').and.returnValue(of(devolucion()))
    };
    clientes = { getListado: jasmine.createSpy('getListado').and.returnValue(of({ content: [cliente()] })) };
    sucursales = { getSucursales: jasmine.createSpy('getSucursales').and.returnValue(of([{ idSucursal: 1, nombre: 'Central' }])) };
    component = new DevolucionesClienteComponent(devoluciones, clientes, sucursales);
  });

  function abrirConPedido(): void {
    component.ngOnInit();
    component.elegirCliente(cliente());
    component.elegirPedido(7);
  }

  it('carga la lista y las sucursales al iniciar', () => {
    component.ngOnInit();

    expect(component.devoluciones.length).toBe(1);
    expect(sucursales.getSucursales).toHaveBeenCalled();
    component.ngOnDestroy();
  });

  it('avisa si no pudo cargar la lista', () => {
    spyOn(Swal, 'fire');
    devoluciones.getListado.and.returnValue(throwError({ error: { message: 'Error del servidor' } }));

    component.cargar(0);

    expect(component.errorCarga).toBeTrue();
    expect(Swal.fire).toHaveBeenCalledWith('No se pudo cargar la lista', 'Error del servidor', 'error');
  });

  it('busca solo clientes mayoristas con pausa y desde dos letras', fakeAsync(() => {
    component.ngOnInit();

    component.onBuscarCliente('l');
    tick(300);
    expect(clientes.getListado).not.toHaveBeenCalled();

    component.onBuscarCliente('libre');
    tick(300);
    expect(clientes.getListado).toHaveBeenCalledWith(0, 8, 'libre', 'nombre', 'asc', 'MAYORISTA');
    component.ngOnDestroy();
  }));

  it('al elegir el cliente trae sus pedidos y al elegir el pedido parte de cero con la sucursal del pedido', () => {
    abrirConPedido();

    expect(devoluciones.pedidosDevolubles).toHaveBeenCalledWith(5);
    expect(component.pedido.noProforma).toBe('P-7');
    expect(component.idSucursal).toBe(1);
    expect(component.destinoDe[1]).toBe('REINGRESO');
    expect(component.total).toBe(0);
  });

  it('calcula el valor y dice qué falta antes de poder registrar', () => {
    abrirConPedido();
    expect(component.faltante).toBe('Indica la cantidad que devuelve de al menos un producto.');

    component.cantidades[1] = 7;
    expect(component.faltante).toBe('De CUADERNO solo se pueden devolver 6.');
    component.cantidades[1] = 2.5;
    expect(component.faltante).toBe('La cantidad de CUADERNO no es válida.');

    component.cantidades[1] = 3;
    expect(component.total).toBe(30);
    expect(component.faltante).toBe('Escribe el motivo de la devolución.');

    component.motivo = ' dañado ';
    expect(component.faltante).toBe('');

    component.idSucursal = null;
    expect(component.faltante).toBe('Elige la sucursal que recibe lo que reingresa.');
    component.destinoDe[1] = 'BAJA';
    expect(component.faltante).toBe('');
  });

  it('un pedido facturado pide la nota de crédito y elige sola la única que hay', () => {
    devoluciones.pedidosDevolubles.and.returnValue(of([pedido({
      facturado: true, noFactura: 1234, notasCredito: [{ idNotaCredito: 4, descripcion: 'Nota de crédito #4', total: 30 }] })]));
    abrirConPedido();
    component.cantidades[1] = 1;
    component.motivo = 'dañado';

    expect(component.idNotaCredito).toBe(4);
    expect(component.faltante).toBe('');

    component.idNotaCredito = null;
    expect(component.faltante).toBe('Elige la nota de crédito a la que se liga la devolución.');

    devoluciones.pedidosDevolubles.and.returnValue(of([pedido({ facturado: true, noFactura: 1234, notasCredito: [] })]));
    abrirConPedido();
    component.cantidades[1] = 1;
    component.motivo = 'dañado';
    expect(component.faltante).toContain('no tiene una nota de crédito');
  });

  it('registra solo las líneas con cantidad, manda la sucursal solo si algo reingresa y recarga', () => {
    abrirConPedido();
    component.cantidades[1] = 3;
    component.motivo = '  producto dañado ';
    component.fecha = '2026-10-20';

    component.registrar();

    expect(devoluciones.registrar).toHaveBeenCalledWith({
      idProforma: 7, fecha: '2026-10-20', motivo: 'producto dañado', idSucursal: 1, idNotaCredito: null,
      lineas: [{ idDetalle: 1, cantidad: 3, destino: 'REINGRESO' }]
    });
    expect(component.formularioAbierto).toBeFalse();
    expect(devoluciones.getListado).toHaveBeenCalledTimes(2);

    abrirConPedido();
    component.cantidades[1] = 1;
    component.destinoDe[1] = 'BAJA';
    component.motivo = 'merma';
    component.registrar();
    expect(devoluciones.registrar.calls.mostRecent().args[0].idSucursal).toBeNull();
  });

  it('no registra con el formulario incompleto y muestra el mensaje si el servidor rechaza', () => {
    component.registrar();
    expect(devoluciones.registrar).not.toHaveBeenCalled();

    spyOn(Swal, 'fire');
    devoluciones.registrar.and.returnValue(throwError({ error: { message: 'De CUADERNO solo se pueden devolver 3' } }));
    abrirConPedido();
    component.cantidades[1] = 3;
    component.motivo = 'dañado';

    component.registrar();

    expect(Swal.fire).toHaveBeenCalledWith('No se pudo registrar la devolución', 'De CUADERNO solo se pueden devolver 3', 'error');
    expect(component.registrando).toBeFalse();
  });

  it('abre y cierra el detalle de una devolución', async () => {
    const fila = devolucion();

    component.alternarDetalle(fila);
    await esperar();
    expect(component.abierta.idDevolucion).toBe(50);

    component.alternarDetalle(fila);
    expect(component.abierta).toBeNull();
  });

  it('limpiar los filtros vuelve a la lista por defecto', () => {
    component.desde = '2026-10-01';
    component.hasta = '2026-10-31';

    component.limpiarFiltros();

    expect(component.desde).toBe('');
    expect(devoluciones.getListado).toHaveBeenCalledWith(0, 10, { desde: '', hasta: '' });
  });
});
