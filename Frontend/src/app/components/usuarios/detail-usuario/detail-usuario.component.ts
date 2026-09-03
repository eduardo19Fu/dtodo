import { DOCUMENT } from '@angular/common';
import {
  AfterViewInit, Component, ElementRef, HostListener, Inject, Input, OnChanges, OnDestroy, OnInit, SimpleChanges
} from '@angular/core';
import { UsuarioAuxiliar } from 'src/app/models/auxiliar/usuario-auxiliar';
import { Sucursal } from 'src/app/models/sucursal';
import { DetailUsuarioService } from 'src/app/services/usuarios/detail-usuario.service';
import { SucursalService } from 'src/app/services/sucursal.service';
import { UsuarioService } from 'src/app/services/usuarios/usuario.service';

import swal from 'sweetalert2';

@Component({
  selector: 'app-detail-usuario',
  templateUrl: './detail-usuario.component.html',
  styleUrls: ['./detail-usuario.component.css']
})
export class DetailUsuarioComponent implements OnInit, OnChanges, AfterViewInit, OnDestroy {

  title: string;

  @Input() usuario: UsuarioAuxiliar;

  sucursales: Sucursal[] = [];
  idSucursalSeleccionada: number = null;
  editandoSucursal = false;
  guardandoSucursal = false;

  constructor(
    public detailUsuarioService: DetailUsuarioService,
    private sucursalService: SucursalService,
    private usuarioService: UsuarioService,
    private elementRef: ElementRef<HTMLElement>,
    @Inject(DOCUMENT) private document: Document
  ) {
    this.title = 'Detalle de Usuario';
  }

  ngOnInit(): void {
    this.sucursalService.getSucursales().subscribe(sucursales => this.sucursales = sucursales);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.usuario) {
      this.idSucursalSeleccionada = this.usuario?.sucursal ? this.usuario.sucursal.idSucursal : null;
      this.editandoSucursal = false;
    }
  }

  ngAfterViewInit(): void {
    this.document.body.appendChild(this.elementRef.nativeElement);
  }

  ngOnDestroy(): void {
    const hostElement = this.elementRef.nativeElement;

    if (hostElement.parentNode === this.document.body) {
      this.document.body.removeChild(hostElement);
    }
  }

  get nombreCompleto(): string {
    if (!this.usuario) {
      return '';
    }
    return [this.usuario.primerNombre, this.usuario.segundoNombre, this.usuario.apellido]
      .filter(nombre => !!nombre)
      .join(' ');
  }

  get iniciales(): string {
    if (!this.usuario) {
      return 'US';
    }
    const nombres = [this.usuario.primerNombre, this.usuario.apellido].filter(nombre => !!nombre);
    return nombres.map(nombre => nombre.trim().charAt(0)).join('').toUpperCase() || 'US';
  }

  nombreRol(rol: string): string {
    if (!rol) {
      return 'Rol sin nombre';
    }
    return rol.replace(/^ROLE_/, '').replace(/_/g, ' ');
  }

  get sucursalCambio(): boolean {
    const idActual = this.usuario?.sucursal ? this.usuario.sucursal.idSucursal : null;
    return this.idSucursalSeleccionada !== idActual;
  }

  iniciarEdicionSucursal(): void {
    this.editandoSucursal = true;
  }

  cancelarEdicionSucursal(): void {
    this.idSucursalSeleccionada = this.usuario?.sucursal ? this.usuario.sucursal.idSucursal : null;
    this.editandoSucursal = false;
  }

  guardarSucursal(): void {
    if (this.guardandoSucursal || !this.sucursalCambio) {
      return;
    }

    this.guardandoSucursal = true;
    const usuarioActualizado: UsuarioAuxiliar = {
      ...this.usuario,
      sucursal: this.idSucursalSeleccionada
        ? this.sucursales.find(item => item.idSucursal === this.idSucursalSeleccionada)
        : null
    };

    this.usuarioService.update(usuarioActualizado).subscribe(
      () => {
        this.usuario.sucursal = usuarioActualizado.sucursal;
        this.guardandoSucursal = false;
        this.editandoSucursal = false;
        swal.fire('Sucursal actualizada', `El usuario ${this.usuario.usuario} fue reasignado con éxito`, 'success');
      },
      () => this.guardandoSucursal = false
    );
  }

  cerrarModal(): void {
    this.detailUsuarioService.cerrarModal();
  }

  @HostListener('document:keydown.escape')
  cerrarConEscape(): void {
    if (this.detailUsuarioService.modal) {
      this.cerrarModal();
    }
  }

  cerrarDesdeBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.cerrarModal();
    }
  }

}
