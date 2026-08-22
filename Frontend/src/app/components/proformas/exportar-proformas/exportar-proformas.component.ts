import { Component, EventEmitter, HostListener, Input, Output } from '@angular/core';

import { UsuarioDto } from '../../../dtos/usuario-dto';

export interface ExportacionProformas {
  idUsuario: number;
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
