import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';

import { Usuario } from '../../models/usuario';
import { AuthService } from '../../services/auth.service';

type MenuDesplegable = 'productos' | 'facturas' | 'compras';

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css']
})
export class SidebarComponent implements OnInit, OnDestroy {
  @Input() collapsed = false;
  @Output() expandir = new EventEmitter<void>();
  @Output() navegacion = new EventEmitter<void>();

  usuario: Usuario;
  filtro = '';
  productosAbierto = false;
  facturasAbierto = false;
  comprasAbierto = false;

  private routerSubscription: Subscription;

  constructor(
    public router: Router,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.usuario = this.authService.usuario;
    this.sincronizarMenusConRuta(this.router.url);
    this.routerSubscription = this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: NavigationEnd) => this.sincronizarMenusConRuta(event.urlAfterRedirects));
  }

  ngOnDestroy(): void {
    if (this.routerSubscription) {
      this.routerSubscription.unsubscribe();
    }
  }

  alternarMenu(menu: MenuDesplegable): void {
    if (this.collapsed) {
      this.expandir.emit();
    }

    if (menu === 'productos') {
      this.productosAbierto = !this.productosAbierto;
      return;
    }
    if (menu === 'facturas') {
      this.facturasAbierto = !this.facturasAbierto;
      return;
    }
    this.comprasAbierto = !this.comprasAbierto;
  }

  menuAbierto(menu: MenuDesplegable): boolean {
    if (this.filtroNormalizado) {
      return this.mostrarGrupo(menu);
    }
    if (menu === 'productos') {
      return this.productosAbierto;
    }
    return menu === 'facturas' ? this.facturasAbierto : this.comprasAbierto;
  }

  mostrarGrupo(menu: MenuDesplegable): boolean {
    if (menu === 'productos') {
      return this.coincide('productos', 'listado', 'marcas', 'categorías', 'categorias');
    }
    if (menu === 'facturas') {
      return this.coincide('facturas', 'facturas emitidas', 'correlativos');
    }
    return this.coincide('compras', 'registrar compra', 'proveedores');
  }

  coincide(...terminos: string[]): boolean {
    const filtroActual = this.filtroNormalizado;
    return !filtroActual || terminos.some(termino => termino.includes(filtroActual));
  }

  limpiarBusqueda(): void {
    this.filtro = '';
  }

  notificarNavegacion(): void {
    this.navegacion.emit();
  }

  cerrarSesion(): void {
    this.authService.logout();
    this.navegacion.emit();
    this.router.navigate(['/login']);
  }

  private get filtroNormalizado(): string {
    return (this.filtro || '').trim().toLocaleLowerCase();
  }

  private sincronizarMenusConRuta(url: string): void {
    if (!this.filtroNormalizado) {
      this.productosAbierto = url.startsWith('/productos') && !url.includes('/inventario');
      this.facturasAbierto = url.startsWith('/facturas');
      this.comprasAbierto = url.startsWith('/compras') || url.startsWith('/proveedores');
    }
  }
}
