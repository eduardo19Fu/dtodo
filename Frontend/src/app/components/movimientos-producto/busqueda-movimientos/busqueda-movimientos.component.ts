import { Component, OnInit } from '@angular/core';
import { MovimientosProductoService } from '../../../services/movimientos/movimientos-producto.service';
import { AuthService } from '../../../services/auth.service';
import { SucursalService } from '../../../services/sucursal.service';
import { Sucursal } from '../../../models/sucursal';

@Component({
  selector: 'app-busqueda-movimientos',
  templateUrl: './busqueda-movimientos.component.html',
  styleUrls: ['./busqueda-movimientos.component.css']
})
export class BusquedaMovimientosComponent implements OnInit {

  title: string;

  fechaIni: Date;
  fechaFin: Date;

  // Elegir sucursal del reporte (solo ROLE_ADMIN); el resto siempre reporta la suya
  sucursales: Sucursal[] = [];
  idSucursal: number;

  constructor(
    private movimientosProductoService: MovimientosProductoService,
    public auth: AuthService,
    private sucursalService: SucursalService
  ) {
    this.title = 'Reporte de Inventario';
  }

  ngOnInit(): void {
    this.idSucursal = this.auth.usuario?.sucursal?.idSucursal;
    if (this.auth.hasRole('ROLE_ADMIN')) {
      this.sucursalService.getSucursales().subscribe(sucursales => this.sucursales = sucursales);
    }
  }

  onSubmit(): void {
    this.movimientosProductoService.getInventoryPDF(this.fechaIni, this.fechaFin, this.idSucursal).subscribe(response => {
      const url = window.URL.createObjectURL(response.data);
      const a = document.createElement('a');
      document.body.appendChild(a);
      a.setAttribute('style', 'display: none');
      a.setAttribute('target', 'blank');
      a.href = url;
      /*
        opcion para pedir descarga de la respuesta obtenida
        a.download = response.filename;
      */
      window.open(a.toString(), '_blank');
      window.URL.revokeObjectURL(url);
      a.remove();
    },
      error => {
        console.log(error);
      });
  }

}
