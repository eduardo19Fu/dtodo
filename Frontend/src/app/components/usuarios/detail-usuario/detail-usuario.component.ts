import { DOCUMENT } from '@angular/common';
import { AfterViewInit, Component, ElementRef, HostListener, Inject, Input, OnDestroy, OnInit } from '@angular/core';
import { UsuarioAuxiliar } from 'src/app/models/auxiliar/usuario-auxiliar';
import { DetailUsuarioService } from 'src/app/services/usuarios/detail-usuario.service';

@Component({
  selector: 'app-detail-usuario',
  templateUrl: './detail-usuario.component.html',
  styleUrls: ['./detail-usuario.component.css']
})
export class DetailUsuarioComponent implements OnInit, AfterViewInit, OnDestroy {

  title: string;

  @Input() usuario: UsuarioAuxiliar;

  constructor(
    public detailUsuarioService: DetailUsuarioService,
    private elementRef: ElementRef<HTMLElement>,
    @Inject(DOCUMENT) private document: Document
  ) {
    this.title = 'Detalle de Usuario';
  }

  ngOnInit(): void {
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
