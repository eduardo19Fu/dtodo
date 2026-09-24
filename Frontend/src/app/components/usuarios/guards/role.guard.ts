import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { AuthService } from '../../../services/auth.service';

import swal from 'sweetalert2';

@Injectable({
  providedIn: 'root'
})
export class RoleGuard implements CanActivate {

  constructor(
    private authService: AuthService,
    private router: Router
  ) { }

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {

    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login']);
      return false;
    }

    // LUGAR DONDE SE RECIBEN LOS ROLES DEL DATA DESDE LA URL PARA SER COMPROBADOS
    // eslint-disable-next-line @typescript-eslint/dot-notation
    const roles = route.data['role'] as string[];
    let hasRole = false;

    roles.forEach(role => {
      if (this.authService.hasRole(role)) {
        hasRole = true;
      }
    });
    if (hasRole) { return true; }

    swal.fire('Acceso no autorizado', `¡El usuario "${this.authService.usuario.usuario}" no posee los permisos necesarios!`, 'warning');
    this.router.navigate(['/home']);
    return false;
  }

}
