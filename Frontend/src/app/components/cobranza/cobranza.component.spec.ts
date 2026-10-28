import { of, throwError } from 'rxjs';

import { CobranzaClienteDto, CobranzaDto } from '../../dtos/cobranza-dto';
import { CobranzaComponent } from './cobranza.component';
import Swal from 'sweetalert2';

describe('CobranzaComponent', () => {
  let servicio: any;
  let component: CobranzaComponent;

  function fila(valores: Partial<CobranzaClienteDto> = {}): CobranzaClienteDto {
    return Object.assign(new CobranzaClienteDto(), {
      idCliente: 1, cliente: 'Libreria Ana', negocio: 'Elizyov', idRuta: 1, ruta: 'RUTA 1', porVencer: 100, vencido1a30: 0,
      vencido31a60: 0, vencidoMasDe60: 0, total: 100, vencido: 0, diasMaxAtraso: 0
    }, valores);
  }

  function cobranza(clientes: CobranzaClienteDto[]): CobranzaDto {
    return Object.assign(new CobranzaDto(), {
      fecha: '2026-11-30', clientes, clientesConVencido: clientes.filter(c => c.vencido > 0).length,
      vencido: clientes.reduce((s, c) => s + c.vencido, 0)
    });
  }

  beforeEach(() => {
    servicio = {
      cuentasPorCobrar: jasmine.createSpy('cuentasPorCobrar').and.returnValue(of(cobranza([
        fila(),
        fila({ idCliente: 2, cliente: 'Tienda Luis', negocio: null, idRuta: 2, ruta: 'RUTA 2', vencido1a30: 50, vencido: 50, total: 50,
          porVencer: 0, diasMaxAtraso: 12 }),
        fila({ idCliente: 3, cliente: 'Papeleria Mara', idRuta: 1, vencidoMasDe60: 300, vencido: 300, total: 300, porVencer: 0,
          diasMaxAtraso: 75 })]))),
      reporte: jasmine.createSpy('reporte').and.returnValue(of({ body: new Blob() })),
      estadoDeCuenta: jasmine.createSpy('estadoDeCuenta').and.returnValue(of({ body: new Blob() }))
    };
    component = new CobranzaComponent(servicio);
    spyOn(window, 'open');
    spyOn(window.URL, 'createObjectURL').and.returnValue('blob:x');
  });

  it('carga las cuentas por cobrar y avisa que hay clientes vencidos', () => {
    component.ngOnInit();

    expect(component.clientes.length).toBe(3);
    expect(component.hayVencidos).toBeTrue();
    expect(component.cobranza.clientesConVencido).toBe(2);
  });

  it('sin vencidos no hay alerta', () => {
    servicio.cuentasPorCobrar.and.returnValue(of(cobranza([fila()])));

    component.ngOnInit();

    expect(component.hayVencidos).toBeFalse();
  });

  it('avisa si no pudo cargar', () => {
    servicio.cuentasPorCobrar.and.returnValue(throwError({ status: 500 }));

    component.cargar();

    expect(component.errorCarga).toBeTrue();
    expect(component.cargando).toBeFalse();
  });

  it('filtra por ruta, por solo vencidos y por texto, y los totales siguen al filtro', () => {
    component.ngOnInit();

    component.idRuta = 1;
    expect(component.clientes.map(c => c.idCliente)).toEqual([1, 3]);
    expect(component.totales.total).toBe(400);

    component.soloVencidos = true;
    expect(component.clientes.map(c => c.idCliente)).toEqual([3]);
    expect(component.totales.vencidoMasDe60).toBe(300);

    component.idRuta = '';
    component.soloVencidos = false;
    component.busqueda = ' ELIZYOV ';
    expect(component.clientes.map(c => c.idCliente)).toEqual([1, 3]);
    component.busqueda = 'luis';
    expect(component.clientes.map(c => c.idCliente)).toEqual([2]);
  });

  it('las rutas del filtro salen de los clientes con deuda y se limpia una que ya no exista', () => {
    component.ngOnInit();
    expect(component.rutas.map(r => r.nombre)).toEqual(['RUTA 1', 'RUTA 2']);

    component.idRuta = 2;
    servicio.cuentasPorCobrar.and.returnValue(of(cobranza([fila()])));
    component.cargar();

    expect(component.idRuta as any).toBe('');
  });

  it('marca la severidad según los días de atraso', () => {
    expect(component.claseAtraso(fila({ diasMaxAtraso: 0 }))).toBe('');
    expect(component.claseAtraso(fila({ diasMaxAtraso: 12 }))).toBe('row-severity-low');
    expect(component.claseAtraso(fila({ diasMaxAtraso: 45 }))).toBe('row-severity-mid');
    expect(component.claseAtraso(fila({ diasMaxAtraso: 61 }))).toBe('row-severity-high');
  });

  it('el reporte manda la ruta y el filtro de vencidos del servidor', () => {
    component.ngOnInit();
    component.idRuta = 1;
    component.soloVencidos = true;

    component.descargarReporte('PDF');

    expect(servicio.reporte).toHaveBeenCalledWith('PDF', 1, true);
    expect(window.open).toHaveBeenCalledWith('blob:x', '_blank');

    component.idRuta = '';
    component.descargarReporte('PDF');
    expect(servicio.reporte).toHaveBeenCalledWith('PDF', null, true);
  });

  it('el estado de cuenta se abre en otra pestaña y un error se muestra', () => {
    component.estadoDeCuenta(fila());
    expect(servicio.estadoDeCuenta).toHaveBeenCalledWith(1);
    expect(window.open).toHaveBeenCalled();

    spyOn(Swal, 'fire');
    servicio.estadoDeCuenta.and.returnValue(throwError({ error: { message: 'El cliente solicitado no se encuentra registrado.' } }));
    component.estadoDeCuenta(fila());
    expect(Swal.fire).toHaveBeenCalledWith('No se pudo generar el estado de cuenta',
      'El cliente solicitado no se encuentra registrado.', 'error');
    expect(component.generando).toBeFalse();
  });
});
