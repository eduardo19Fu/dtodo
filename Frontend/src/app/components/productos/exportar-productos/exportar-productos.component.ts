import { Component, EventEmitter, HostListener, Input, OnInit, Output } from '@angular/core';

import { Sucursal } from '../../../models/sucursal';

export interface ExportacionProductos {
  idSucursal: number;
  nombreSucursal: string;
}

@Component({
  selector: 'app-exportar-productos',
  templateUrl: './exportar-productos.component.html',
  styleUrls: ['./exportar-productos.component.css']
})
export class ExportarProductosComponent implements OnInit {

  @Input() sucursales: Sucursal[] = [];
  @Input() idSucursalActual: number;
  @Output() cerrar = new EventEmitter<void>();
  @Output() exportar = new EventEmitter<ExportacionProductos>();

  idSucursal: number;

  ngOnInit(): void {
    this.idSucursal = this.idSucursalActual;
  }

  @HostListener('document:keydown.escape')
  cerrarConEscape(): void {
    this.cerrar.emit();
  }

  solicitarExportacion(): void {
    const sucursal = this.sucursales.find(s => s.idSucursal === Number(this.idSucursal));
    if (!sucursal) {
      return;
    }
    this.exportar.emit({ idSucursal: sucursal.idSucursal, nombreSucursal: sucursal.nombre });
  }
}
