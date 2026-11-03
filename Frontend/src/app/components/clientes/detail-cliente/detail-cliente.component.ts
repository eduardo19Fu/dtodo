import { DOCUMENT } from '@angular/common';
import {
  AfterViewInit, Component, ElementRef, EventEmitter, HostListener, Inject, Input, OnDestroy, OnInit, Output
} from '@angular/core';
import { Subscription } from 'rxjs';

import { ClienteDto } from 'src/app/dtos/cliente-dto';
import { RutaDto } from 'src/app/dtos/ruta-dto';
import { PedidoSaldoDto, SaldoClienteDto } from 'src/app/dtos/saldo-dto';
import { AuthService } from 'src/app/services/auth.service';
import { RutaService } from 'src/app/services/ruta.service';
import { SaldoService } from 'src/app/services/saldo.service';

const DESCRIPCION_CATEGORIA: { [categoria: string]: string } = {
  A: 'Compra frecuente',
  B: 'No frecuente, menos de 12 unidades',
  VOLUMEN: 'Frecuente, pide fardos o cientos'
};

/**
 * Detalle de un cliente mayorista: sus datos, su ruta y, para quien puede ver cobros, su cuenta (saldo, crédito y pedidos).
 * Sigue el patrón de los demás detalles del sistema: el modal se mueve al body y comparte el CSS de bodegas con otra paleta.
 */
@Component({
  selector: 'app-detail-cliente',
  templateUrl: './detail-cliente.component.html',
  styleUrls: [
    '../../bodegas/detail-bodega/detail-bodega.component.css',
    './detail-cliente.component.css'
  ]
})
export class DetailClienteComponent implements OnInit, AfterViewInit, OnDestroy {

  @Input() cliente: ClienteDto;

  @Output() cerrar = new EventEmitter<void>();

  saldo: SaldoClienteDto = null;
  cargandoCuenta = false;
  errorCuenta = false;
  /** La ruta con su vendedor y sucursal; solo la consulta el administrador. */
  ruta: RutaDto = null;

  private suscripciones = new Subscription();

  constructor(
    private elementRef: ElementRef<HTMLElement>,
    @Inject(DOCUMENT) private document: Document,
    private saldoService: SaldoService,
    private rutaService: RutaService,
    public auth: AuthService
  ) {}

  ngOnInit(): void {
    if (this.puedeVerCuenta) {
      this.cargarCuenta();
    }
    if (this.auth.hasRole('ROLE_ADMIN') && this.cliente && this.cliente.idRuta) {
      this.suscripciones.add(this.rutaService.getRuta(this.cliente.idRuta).subscribe(
        ruta => this.ruta = ruta,
        () => this.ruta = null
      ));
    }
  }

  ngAfterViewInit(): void {
    // El modal se mueve al body para que su overlay cubra toda la pantalla, igual que el resto de detalles
    this.document.body.appendChild(this.elementRef.nativeElement);
  }

  ngOnDestroy(): void {
    this.suscripciones.unsubscribe();
    const hostElement = this.elementRef.nativeElement;
    if (hostElement.parentNode === this.document.body) {
      this.document.body.removeChild(hostElement);
    }
  }

  /** Administrador, cobrador y vendedor (de sus clientes) ven el saldo; el servidor decide lo demás. */
  get puedeVerCuenta(): boolean {
    return this.auth.hasRole('ROLE_ADMIN') || this.auth.hasRole('ROLE_COBRADOR') || this.auth.hasRole('ROLE_VENDEDOR');
  }

  cargarCuenta(): void {
    this.cargandoCuenta = true;
    this.errorCuenta = false;
    this.suscripciones.add(this.saldoService.cliente(this.cliente.idCliente).subscribe(saldo => {
      this.saldo = saldo;
      this.cargandoCuenta = false;
    }, () => {
      this.saldo = null;
      this.cargandoCuenta = false;
      this.errorCuenta = true;
    }));
  }

  get descripcionCategoria(): string {
    return this.cliente && this.cliente.categoriaDescuento ? DESCRIPCION_CATEGORIA[this.cliente.categoriaDescuento] : '';
  }

  /** El crédito existe solo si el cliente tiene límite y días. */
  get tieneCredito(): boolean {
    return !!this.cliente && Number(this.cliente.limiteCredito) > 0 && Number(this.cliente.diasCredito) > 0;
  }

  /** Lo que ya se usa del límite: el límite menos lo que le queda. */
  get usadoCredito(): number {
    if (!this.saldo || this.saldo.disponible == null || !this.tieneCredito) {
      return 0;
    }
    return Number(this.cliente.limiteCredito) - Number(this.saldo.disponible);
  }

  /** Porcentaje del límite en uso, entre 0 y 100 (para la barra). */
  get porcentajeCredito(): number {
    if (!this.tieneCredito) {
      return 0;
    }
    const porcentaje = (Math.max(this.usadoCredito, 0) / Number(this.cliente.limiteCredito)) * 100;
    return Math.min(100, Math.round(porcentaje));
  }

  get estadoCredito(): 'ok' | 'alto' | 'excedido' {
    if (this.saldo && this.saldo.disponible != null && Number(this.saldo.disponible) < 0) {
      return 'excedido';
    }
    return this.porcentajeCredito >= 80 ? 'alto' : 'ok';
  }

  get etiquetaSaldo(): string {
    return this.saldo && this.saldo.saldo < 0 ? 'A favor del cliente' : 'Saldo pendiente';
  }

  get valorSaldo(): number {
    return this.saldo ? Math.abs(Number(this.saldo.saldo)) : 0;
  }

  etiquetaCondicion(pedido: PedidoSaldoDto): string {
    return pedido.condicionPago === 'CREDITO' ? 'Crédito a ' + (pedido.diasCredito || 0) + ' días' : 'Contado';
  }

  /** Un pedido a crédito con saldo cuyo plazo ya pasó. */
  estaVencido(pedido: PedidoSaldoDto): boolean {
    if (!pedido.vencimiento || !(Number(pedido.pendiente) > 0)) {
      return false;
    }
    const hoy = new Date();
    const clave = hoy.getFullYear() + '-' + String(hoy.getMonth() + 1).padStart(2, '0') + '-' + String(hoy.getDate()).padStart(2, '0');
    return pedido.vencimiento < clave;
  }

  cerrarModal(): void {
    this.cerrar.emit();
  }

  cerrarDesdeBackdrop(evento: Event): void {
    if (evento.target === evento.currentTarget) {
      this.cerrarModal();
    }
  }

  @HostListener('document:keydown.escape')
  cerrarConEscape(): void {
    this.cerrarModal();
  }
}
