import { Component, EventEmitter, HostListener, Input, OnInit, Output } from '@angular/core';

import { Bodega } from 'src/app/models/bodega';
import { Sucursal } from 'src/app/models/sucursal';
import { ImportacionInventarioDto } from 'src/app/dtos/movimiento-bodega-request';
import { BodegaService } from 'src/app/services/bodega.service';
import { SucursalService } from 'src/app/services/sucursal.service';
import { OrigenInventarioSeleccion } from '../origen-inventario-bodega/origen-inventario-bodega.component';

import Swal from 'sweetalert2';

/** Carga masiva de existencias a una bodega: copia desde una sucursal u otra bodega, o importación de un Excel. */
@Component({
  selector: 'app-modal-importar-inventario',
  templateUrl: './modal-importar-inventario.component.html',
  styleUrls: ['./modal-importar-inventario.component.css']
})
export class ModalImportarInventarioComponent implements OnInit {

  @Input() bodega: Bodega;
  /** Las copias solo se permiten sobre una bodega sin inventario; el Excel siempre. */
  @Input() inventarioVacio = false;
  @Output() completado = new EventEmitter<void>();
  @Output() cerrar = new EventEmitter<void>();

  sucursales: Sucursal[] = [];
  bodegasOrigen: Bodega[] = [];
  origen: OrigenInventarioSeleccion = { tipo: 'EXCEL', idOrigen: null, archivo: null, completa: false };
  tipoInicial: 'SUCURSAL' | 'EXCEL' = 'EXCEL';
  importando = false;
  errores: string[] = [];
  filasLeidas = 0;

  constructor(
    private bodegaService: BodegaService,
    private sucursalService: SucursalService
  ) { }

  ngOnInit(): void {
    this.tipoInicial = this.inventarioVacio ? 'SUCURSAL' : 'EXCEL';
    this.origen = { tipo: this.tipoInicial, idOrigen: null, archivo: null, completa: false };
    if (this.inventarioVacio) {
      this.sucursalService.getSucursales().subscribe(sucursales => this.sucursales = sucursales);
      this.bodegaService.getBodegas(true).subscribe(
        bodegas => this.bodegasOrigen = bodegas.filter(bodega => bodega.idBodega !== this.bodega.idBodega)
      );
    }
  }

  alCambiarOrigen(origen: OrigenInventarioSeleccion): void {
    this.origen = origen;
    this.errores = [];
  }

  importar(): void {
    if (!this.origen.completa || this.importando) {
      return;
    }
    this.importando = true;
    this.errores = [];

    if (this.origen.tipo === 'EXCEL') {
      this.bodegaService.importarExcel(this.bodega.idBodega, this.origen.archivo).subscribe(
        resultado => this.alImportarExcel(resultado),
        () => this.importando = false
      );
    } else if (this.origen.tipo === 'SUCURSAL' || this.origen.tipo === 'BODEGA') {
      this.bodegaService.clonarInventario(this.bodega.idBodega, this.origen.tipo, this.origen.idOrigen).subscribe(
        resultado => {
          this.importando = false;
          Swal.fire('Inventario copiado', `Se copiaron ${resultado.productosCopiados} productos a la bodega.`, 'success');
          this.completado.emit();
        },
        () => this.importando = false
      );
    }
  }

  private alImportarExcel(resultado: ImportacionInventarioDto): void {
    this.importando = false;
    if (resultado.errores.length > 0) {
      this.errores = resultado.errores;
      this.filasLeidas = resultado.filasLeidas;
      return;
    }
    Swal.fire('Inventario importado',
      `Se importaron ${resultado.productosImportados} productos (${resultado.unidadesImportadas} unidades).`, 'success');
    this.completado.emit();
  }

  cerrarModal(): void {
    if (!this.importando) {
      this.cerrar.emit();
    }
  }

  @HostListener('document:keydown.escape')
  alPresionarEscape(): void {
    this.cerrarModal();
  }
}
