import { Component, EventEmitter, HostListener, Input, Output } from '@angular/core';

import { UsuarioDto } from '../../../dtos/usuario-dto';

export interface ExportacionProformas {
  idUsuario: number | null;
  nombreUsuario: string;
  fechaInicio?: string;
  fechaFin?: string;
}

@Component({
  selector: 'app-exportar-proformas',
  templateUrl: './exportar-proformas.component.html',
  styleUrls: ['./exportar-proformas.component.css']
})
export class ExportarProformasComponent {

  /** Valor centinela del select que representa "no filtrar por usuario". */
  readonly TODOS_USUARIOS = -1;

  @Input() usuarios: UsuarioDto[] = [];
  @Output() cerrar = new EventEmitter<void>();
  @Output() exportarRango = new EventEmitter<ExportacionProformas>();
  @Output() exportarTodas = new EventEmitter<ExportacionProformas>();

  idUsuario: number;
  fechaInicio: string;
  fechaFin: string;

  @HostListener('document:keydown.escape')
  cerrarConEscape(): void {
    this.cerrar.emit();
  }

  solicitarExportacionRango(): void {
    const solicitud = this.crearSolicitud();
    if (!solicitud || !this.rangoValido) {
      return;
    }
    this.exportarRango.emit({
      ...solicitud,
      fechaInicio: this.fechaInicio,
      fechaFin: this.fechaFin
    });
  }

  solicitarExportacionCompleta(): void {
    const solicitud = this.crearSolicitud();
    if (solicitud) {
      this.exportarTodas.emit(solicitud);
    }
  }

  get rangoValido(): boolean {
    return !!this.fechaInicio && !!this.fechaFin && this.fechaFin >= this.fechaInicio;
  }

  get rangoInvertido(): boolean {
    return !!this.fechaInicio && !!this.fechaFin && this.fechaFin < this.fechaInicio;
  }

  private crearSolicitud(): ExportacionProformas | null {
    if (this.idUsuario === undefined || this.idUsuario === null) {
      return null;
    }
    if (Number(this.idUsuario) === this.TODOS_USUARIOS) {
      return { idUsuario: null, nombreUsuario: 'todos los usuarios' };
    }
    const usuario = this.usuarios.find(item => item.idUsuario === Number(this.idUsuario));
    if (!usuario) {
      return null;
    }
    return {
      idUsuario: usuario.idUsuario,
      nombreUsuario: this.obtenerNombreUsuario(usuario)
    };
  }

  private obtenerNombreUsuario(usuario: UsuarioDto): string {
    const nombre = [usuario.primerNombre, usuario.segundoNombre, usuario.apellido]
      .filter(valor => !!valor).join(' ');
    return nombre ? `${usuario.usuario} - ${nombre}` : usuario.usuario;
  }
}
