import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { Sucursal } from 'src/app/models/sucursal';
import { AuthService } from 'src/app/services/auth.service';
import { SucursalService } from 'src/app/services/sucursal.service';
import { UsuarioService } from 'src/app/services/usuarios/usuario.service';

import swal from 'sweetalert2';

@Component({
  selector: 'app-create-sucursal',
  templateUrl: './create-sucursal.component.html',
  styleUrls: ['../../productos/create-producto/create-producto.component.css']
})
export class CreateSucursalComponent implements OnInit {

  title: string;
  sucursal: Sucursal;

  sucursalesExistentes: Sucursal[] = [];
  idSucursalOrigenInventario: number = null;
  guardando = false;

  constructor(
    private sucursalService: SucursalService,
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private usuarioService: UsuarioService,
    private authService: AuthService
  ) {
    this.title = 'Registrar nueva sucursal';
    this.sucursal = new Sucursal();
  }

  ngOnInit(): void {
    this.cargarSucursal();
  }

  get esNueva(): boolean {
    return !this.sucursal.idSucursal;
  }

  cargarSucursal(): void {
    this.activatedRoute.params.subscribe(params => {
      const id = params.id;
      if (id) {
        this.title = 'Editar sucursal';
        this.sucursalService.getSucursal(id).subscribe(
          sucursal => this.sucursal = sucursal
        );
      } else {
        this.sucursalService.getSucursales().subscribe(
          sucursales => this.sucursalesExistentes = sucursales
        );
      }
    });
  }

  create(): void {
    this.guardando = true;
    this.usuarioService.getUsuario(this.authService.usuario.idUsuario).subscribe(
      usuario => {
        this.sucursal.usuario = usuario;

        this.sucursalService.create(this.sucursal).subscribe(
          nuevaSucursal => {
            if (this.idSucursalOrigenInventario) {
              this.sucursalService.clonarInventario(nuevaSucursal.idSucursal, this.idSucursalOrigenInventario).subscribe(
                () => this.finalizarCreacion(nuevaSucursal),
                () => this.finalizarCreacion(nuevaSucursal)
              );
            } else {
              this.finalizarCreacion(nuevaSucursal);
            }
          },
          () => this.guardando = false
        );
      },
      () => this.guardando = false
    );
  }

  private finalizarCreacion(nuevaSucursal: Sucursal): void {
    this.guardando = false;
    this.router.navigate(['/sucursales/index']);
    swal.fire('Sucursal Guardada', `La sucursal ${nuevaSucursal.nombre} fue registrada con &eacute;xito`, 'success');
  }

  update(): void {
    this.guardando = true;
    this.sucursalService.update(this.sucursal).subscribe(
      sucursalActualizada => {
        this.guardando = false;
        this.router.navigate(['/sucursales/index']);
        swal.fire('Sucursal Actualizada', `${sucursalActualizada.nombre} fu&eacute; actualizada con &eacute;xito!`, 'success');
      },
      () => this.guardando = false
    );
  }

}
