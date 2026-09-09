import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';

import { Usuario } from '../../models/usuario';
import { AuthService } from '../../services/auth.service';

type MenuDesplegable = 'productos' | 'facturas';

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
    this.facturasAbierto = !this.facturasAbierto;
  }

  menuAbierto(menu: MenuDesplegable): boolean {
    if (this.filtroNormalizado) {
      return this.mostrarGrupo(menu);
    }
    return menu === 'productos' ? this.productosAbierto : this.facturasAbierto;
  }

  mostrarGrupo(menu: MenuDesplegable): boolean {
    return menu === 'productos'
      ? this.coincide('productos', 'listado', 'marcas', 'categorías', 'categorias')
      : this.coincide('facturas', 'facturas emitidas', 'correlativos');
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
    }
  }
}
