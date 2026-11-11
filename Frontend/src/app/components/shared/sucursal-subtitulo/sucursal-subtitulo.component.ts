import { Component } from '@angular/core';

import { AuthService } from '../../../services/auth.service';

/**
 * La sucursal de la sesión bajo el título de página, con su icono de tienda, como en Compras, Facturas y las demás pantallas.
 * No muestra nada si el usuario no tiene sucursal asignada.
 */
@Component({
  selector: 'app-sucursal-subtitulo',
  template: `
    <p class="sucursal-subtitle" *ngIf="auth.usuario?.sucursal">
      <i class="fas fa-store" aria-hidden="true"></i> {{ auth.usuario.sucursal.nombre }}
    </p>`,
  styles: ['.sucursal-subtitle { margin-bottom: 0.25rem; } .sucursal-subtitle i { color: #2f7d6b; }']
})
export class SucursalSubtituloComponent {
  constructor(public auth: AuthService) { }
}
