import { Component, EventEmitter, Input, Output } from '@angular/core';

import { Bodega } from 'src/app/models/bodega';
import { Sucursal } from 'src/app/models/sucursal';
import { BodegaService } from 'src/app/services/bodega.service';

import Swal from 'sweetalert2';

export type TipoOrigenInventario = 'NINGUNO' | 'SUCURSAL' | 'BODEGA' | 'EXCEL';

export interface OrigenInventarioSeleccion {
  tipo: TipoOrigenInventario;
  idOrigen: number;
  archivo: File;
  /** true cuando la selección tiene todo lo necesario para ejecutarse. */
  completa: boolean;
}

/**
 * Selector del origen del inventario de una bodega: ninguno, copia de una sucursal, copia de otra bodega
 * o importación desde un archivo Excel (con la ilustración del formato esperado y la plantilla descargable).
 */
@Component({
  selector: 'app-origen-inventario-bodega',
  templateUrl: './origen-inventario-bodega.component.html',
  styleUrls: ['./origen-inventario-bodega.component.css']
})
export class OrigenInventarioBodegaComponent {

  @Input() sucursales: Sucursal[] = [];
  @Input() bodegas: Bodega[] = [];
  @Input() permitirNinguno = true;
  /** Las copias desde sucursal u otra bodega solo aplican a una bodega vacía; el Excel siempre se permite. */
  @Input() permitirCopias = true;
  @Input() set tipoInicial(tipo: TipoOrigenInventario) {
    if (tipo) {
      this.tipo = tipo;
    }
  }
  @Output() cambio = new EventEmitter<OrigenInventarioSeleccion>();

  tipo: TipoOrigenInventario = 'NINGUNO';
  idOrigen: number = null;
  archivo: File = null;
  descargandoPlantilla = false;

  readonly columnasEjemplo = ['A', 'B', 'C'];
  readonly filasEjemplo = [
    { codigo: '7501234567890', cantidad: 25, minimo: 5 },
    { codigo: '4011200296908', cantidad: 100, minimo: null },
    { codigo: 'LIB-0042', cantidad: 8, minimo: 2 },
    { codigo: 'LIB-0099', cantidad: 0, minimo: null }
  ];

  seleccionarTipo(tipo: TipoOrigenInventario): void {
    this.tipo = tipo;
    this.idOrigen = null;
    this.archivo = null;
    this.emitir();
  }

  seleccionarOrigen(idOrigen: number): void {
    this.idOrigen = idOrigen;
    this.emitir();
  }

  alElegirArchivo(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    const archivo = input.files && input.files.length ? input.files[0] : null;
    if (archivo && !archivo.name.toLowerCase().endsWith('.xlsx')) {
      Swal.fire('Archivo no válido', 'Selecciona un archivo de Excel con formato .xlsx.', 'warning');
      input.value = '';
      this.archivo = null;
    } else {
      this.archivo = archivo;
    }
    this.emitir();
  }

  quitarArchivo(input: HTMLInputElement): void {
    input.value = '';
    this.archivo = null;
    this.emitir();
  }

  descargarPlantilla(): void {
    if (this.descargandoPlantilla) {
      return;
    }
    this.descargandoPlantilla = true;
    this.bodegaService.descargarPlantillaImportacion().subscribe(
      respuesta => {
        const url = window.URL.createObjectURL(respuesta.body);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = 'plantilla_inventario_bodega.xlsx';
        document.body.appendChild(enlace);
        enlace.click();
        enlace.remove();
        window.URL.revokeObjectURL(url);
        this.descargandoPlantilla = false;
      },
      () => {
        this.descargandoPlantilla = false;
        Swal.fire('No se pudo descargar la plantilla', 'Intenta nuevamente.', 'error');
      }
    );
  }

  get seleccion(): OrigenInventarioSeleccion {
    return { tipo: this.tipo, idOrigen: this.idOrigen, archivo: this.archivo, completa: this.completa };
  }

  get completa(): boolean {
    switch (this.tipo) {
      case 'NINGUNO':
        return true;
      case 'EXCEL':
        return !!this.archivo;
      default:
        return !!this.idOrigen;
    }
  }

  get tamanioArchivo(): string {
    if (!this.archivo) {
      return '';
    }
    return this.archivo.size < 1024 * 1024
      ? `${Math.max(1, Math.round(this.archivo.size / 1024))} KB`
      : `${(this.archivo.size / (1024 * 1024)).toFixed(1)} MB`;
  }

  constructor(private bodegaService: BodegaService) { }

  private emitir(): void {
    this.cambio.emit(this.seleccion);
  }
}
