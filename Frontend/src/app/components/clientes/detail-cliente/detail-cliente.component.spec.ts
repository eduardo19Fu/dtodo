import { CommonModule } from '@angular/common';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { ClienteDto } from 'src/app/dtos/cliente-dto';
import { RutaDto } from 'src/app/dtos/ruta-dto';
import { PedidoSaldoDto, SaldoClienteDto } from 'src/app/dtos/saldo-dto';
import { AuthService } from 'src/app/services/auth.service';
import { RutaService } from 'src/app/services/ruta.service';
import { SaldoService } from 'src/app/services/saldo.service';
import { DetailClienteComponent } from './detail-cliente.component';

describe('DetailClienteComponent', () => {
  let fixture: ComponentFixture<DetailClienteComponent>;
  let roles: string[];
  let saldoService: any;
  let rutaService: any;

  function cliente(valores: Partial<ClienteDto> = {}): ClienteDto {
    return Object.assign(new ClienteDto(), {
      idCliente: 7336, nombre: 'CLIENTE PRUEBA MAYORISTA', nombreNegocio: 'Libreria Prueba', nit: '9988776-5',
      direccion: 'Zona 1, Jalapa', telefono: '55551234', fechaRegistro: new Date(2026, 9, 28), codigoCliente: 'a1b2c3',
      tipoCliente: 'MAYORISTA', categoriaDescuento: 'A', idRuta: 3, ruta: 'RUTA PRUEBA', contacto: 'Ana Pérez',
      telefonoContacto: '44441234', referenciaUbicacion: 'Frente a la escuela', limiteCredito: 1000, diasCredito: 30
    }, valores);
  }

  function pedido(valores: Partial<PedidoSaldoDto> = {}): PedidoSaldoDto {
    return Object.assign(new PedidoSaldoDto(), {
      idProforma: 1, noProforma: '584807430P', condicionPago: 'CREDITO', diasCredito: 30, total: 450, entregado: 450,
      cobrado: 0, devuelto: 0, aplicadoDeAbonos: 0, pendiente: 450, primeraEntrega: '2099-01-01', vencimiento: '2099-01-31'
    }, valores);
  }

  function saldo(valores: Partial<SaldoClienteDto> = {}): SaldoClienteDto {
    return Object.assign(new SaldoClienteDto(), {
      idCliente: 7336, cliente: 'CLIENTE PRUEBA MAYORISTA', limiteCredito: 1000, diasCredito: 30, entregado: 1170, devuelto: 0,
      abonado: 500, saldo: 670, disponible: 330, pedidos: [pedido()]
    }, valores);
  }

  function ruta(): RutaDto {
    return Object.assign(new RutaDto(), {
      idRuta: 3, nombre: 'RUTA PRUEBA', zona: 'Jalapa centro', diasVisita: 'Lunes cada 15 días', vendedor: 'Vendedor Prueba', sucursal: 'Sucursal Central'
    });
  }

  async function crear(datos: ClienteDto = cliente()): Promise<void> {
    fixture = TestBed.createComponent(DetailClienteComponent);
    fixture.componentInstance.cliente = datos;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function texto(): string {
    return (fixture.nativeElement as HTMLElement).textContent.replace(/\s+/g, ' ');
  }

  beforeEach(async () => {
    roles = ['ROLE_ADMIN'];
    saldoService = { cliente: jasmine.createSpy('cliente').and.callFake(() => of(saldo())) };
    rutaService = { getRuta: jasmine.createSpy('getRuta').and.callFake(() => of(ruta())) };
    await TestBed.configureTestingModule({
      imports: [CommonModule],
      declarations: [DetailClienteComponent],
      providers: [
        { provide: SaldoService, useValue: saldoService },
        { provide: RutaService, useValue: rutaService },
        { provide: AuthService, useValue: { hasRole: (rol: string) => roles.includes(rol) } }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  });

  afterEach(() => fixture && fixture.destroy());

  it('muestra los datos del cliente, su ruta, el vendedor y la categoría', async () => {
    await crear();

    const t = texto();
    expect(t).toContain('CLIENTE PRUEBA MAYORISTA');
    expect(t).toContain('Libreria Prueba');
    expect(t).toContain('9988776-5');
    expect(t).toContain('Zona 1, Jalapa');
    expect(t).toContain('Ana Pérez');
    expect(t).toContain('Frente a la escuela');
    expect(t).toContain('RUTA PRUEBA');
    expect(t).toContain('Vendedor Prueba');
    expect(t).toContain('Sucursal Central');
    expect(t).toContain('Categoría A');
    expect(t).toContain('Compra frecuente');
    expect(rutaService.getRuta).toHaveBeenCalledWith(3);
  });

  it('con datos vacíos muestra textos claros en lugar de espacios en blanco', async () => {
    await crear(cliente({ nit: null, telefono: null, contacto: null, telefonoContacto: null, referenciaUbicacion: null,
      direccion: null, codigoCliente: null, nombreNegocio: null, categoriaDescuento: null, ruta: null, idRuta: null }));

    const t = texto();
    expect(t).toContain('Sin NIT');
    expect(t).toContain('Sin teléfono');
    expect(t).toContain('Sin dirección');
    expect(t).toContain('Sin código');
    expect(t).toContain('Sin ruta');
    expect(rutaService.getRuta).not.toHaveBeenCalled();
  });

  it('muestra el saldo, lo entregado, devuelto y abonado, y el uso del crédito', async () => {
    await crear();

    const t = texto();
    expect(t).toContain('Saldo pendiente');
    expect(t).toContain('Q670.00');
    expect(t).toContain('Entregado');
    expect(t).toContain('Q1,170.00');
    expect(t).toContain('Abonado');
    expect(t).toContain('Q500.00');
    expect(t).toContain('Crédito · 30 días');
    expect(t).toContain('Disponible');
    expect(fixture.componentInstance.usadoCredito).toBe(670);
    expect(fixture.componentInstance.porcentajeCredito).toBe(67);
    expect(fixture.componentInstance.estadoCredito).toBe('ok');
    const relleno = (fixture.nativeElement as HTMLElement).querySelector('.credit-meter-fill') as HTMLElement;
    expect(relleno.style.width).toBe('67%');
  });

  it('un crédito excedido lo marca en rojo y dice por cuánto', async () => {
    saldoService.cliente.and.returnValue(of(saldo({ saldo: 1170, disponible: -170 })));
    await crear();

    expect(fixture.componentInstance.estadoCredito).toBe('excedido');
    expect(fixture.componentInstance.porcentajeCredito).toBe(100);
    expect(texto()).toContain('Excedido por');
    expect(texto()).toContain('Q170.00');
    expect((fixture.nativeElement as HTMLElement).querySelector('.credit-card').classList).toContain('credit-excedido');
  });

  it('un crédito casi agotado se marca como alto', async () => {
    saldoService.cliente.and.returnValue(of(saldo({ saldo: 850, disponible: 150 })));
    await crear();

    expect(fixture.componentInstance.estadoCredito).toBe('alto');
  });

  it('un saldo a favor se presenta como tal, en positivo', async () => {
    saldoService.cliente.and.returnValue(of(saldo({ saldo: -630, disponible: 1000, pedidos: [] })));
    await crear();

    expect(texto()).toContain('A favor del cliente');
    expect(texto()).toContain('Q630.00');
    expect(fixture.componentInstance.valorSaldo).toBe(630);
  });

  it('un cliente sin límite ni días de crédito se presenta como de contado', async () => {
    await crear(cliente({ limiteCredito: null, diasCredito: null }));

    expect(fixture.componentInstance.tieneCredito).toBeFalse();
    expect(texto()).toContain('De contado');
    expect((fixture.nativeElement as HTMLElement).querySelector('.credit-meter')).toBeNull();
  });

  it('lista los pedidos con lo cobrado y marca como vencido el que ya pasó su plazo', async () => {
    saldoService.cliente.and.returnValue(of(saldo({
      pedidos: [pedido(), pedido({ idProforma: 2, noProforma: '49873732P', vencimiento: '2020-01-31', pendiente: 100 }),
        pedido({ idProforma: 3, noProforma: 'CONTADO1', condicionPago: 'CONTADO', vencimiento: null, pendiente: 0 })]
    })));
    await crear();

    const filas = (fixture.nativeElement as HTMLElement).querySelectorAll('.document-lines-table tbody tr');
    expect(filas.length).toBe(3);
    expect(filas[0].textContent).toContain('584807430P');
    expect(filas[0].textContent).toContain('Crédito a 30 días');
    expect(filas[0].querySelector('.vence-pill')).toBeNull();
    expect(filas[1].querySelector('.vence-pill')).not.toBeNull();
    expect(filas[2].textContent).toContain('Contado');
  });

  it('sin pedidos entregados no muestra la tabla de pedidos', async () => {
    saldoService.cliente.and.returnValue(of(saldo({ pedidos: [] })));
    await crear();

    expect((fixture.nativeElement as HTMLElement).querySelector('.document-lines-table')).toBeNull();
  });

  it('quien no ve cobros no consulta ni ve la cuenta, pero sí los datos del cliente', async () => {
    roles = ['ROLE_INVENTARIO'];
    await crear();

    expect(saldoService.cliente).not.toHaveBeenCalled();
    expect(rutaService.getRuta).not.toHaveBeenCalled();
    expect(texto()).not.toContain('Cuenta del cliente');
    expect(texto()).toContain('Datos del cliente');
    expect(texto()).toContain('RUTA PRUEBA');
  });

  it('el vendedor ve la cuenta pero no consulta la ruta, que es solo del administrador', async () => {
    roles = ['ROLE_VENDEDOR'];
    await crear();

    expect(saldoService.cliente).toHaveBeenCalledWith(7336);
    expect(rutaService.getRuta).not.toHaveBeenCalled();
    expect(texto()).not.toContain('Vendedor Prueba');
  });

  it('si la cuenta no carga lo avisa y permite reintentar', async () => {
    saldoService.cliente.and.returnValue(throwError({ status: 500 }));
    await crear();

    expect(fixture.componentInstance.errorCuenta).toBeTrue();
    expect(texto()).toContain('No se pudo consultar la cuenta');

    saldoService.cliente.and.returnValue(of(saldo()));
    ((fixture.nativeElement as HTMLElement).querySelector('.empty-state button') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(fixture.componentInstance.errorCuenta).toBeFalse();
    expect(fixture.componentInstance.saldo).not.toBeNull();
    expect(texto()).toContain('Q670.00');
  });

  it('se cierra con la X, con Cerrar, con Escape y al pulsar fuera del cuadro', async () => {
    await crear();
    const cerrado = jasmine.createSpy('cerrar');
    fixture.componentInstance.cerrar.subscribe(cerrado);
    const raiz = fixture.nativeElement as HTMLElement;

    (raiz.querySelector('.close-button') as HTMLButtonElement).click();
    expect(cerrado).toHaveBeenCalledTimes(1);

    const botones = Array.from(raiz.querySelectorAll('.footer-actions .close-action')) as HTMLButtonElement[];
    botones[botones.length - 1].click();
    expect(cerrado).toHaveBeenCalledTimes(2);

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    expect(cerrado).toHaveBeenCalledTimes(3);

    (raiz.querySelector('.sucursal-detail-overlay') as HTMLElement).click();
    expect(cerrado).toHaveBeenCalledTimes(4);

    (raiz.querySelector('.sucursal-detail-dialog') as HTMLElement).click();
    expect(cerrado).toHaveBeenCalledTimes(4);
  });

  it('se mueve al body para cubrir toda la pantalla y se quita al destruirse', async () => {
    await crear();
    const raiz = fixture.nativeElement as HTMLElement;

    expect(raiz.parentElement).toBe(document.body);
    const overlay = raiz.querySelector('.sucursal-detail-overlay') as HTMLElement;
    expect(getComputedStyle(overlay).position).toBe('fixed');
    expect(getComputedStyle(raiz.querySelector('.sucursal-detail-header')).backgroundImage).toContain('gradient');
    expect(getComputedStyle(raiz.querySelector('.summary-grid')).display).toBe('grid');

    fixture.destroy();
    expect(document.body.contains(raiz)).toBeFalse();
  });

  it('usa la identidad verde de clientes y no el ocre del módulo de bodegas', async () => {
    await crear();
    const raiz = fixture.nativeElement as HTMLElement;

    expect(getComputedStyle(raiz).getPropertyValue('--green-900').trim()).toBe('#1f5a4c');
    expect(getComputedStyle(raiz.querySelector('.sucursal-detail-body')).backgroundColor).toBe('rgb(245, 250, 248)');
  });
});
