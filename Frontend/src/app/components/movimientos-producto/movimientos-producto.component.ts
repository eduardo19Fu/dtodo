import { Component, OnInit } from '@angular/core';

import { MovimientoProducto } from 'src/app/models/movimiento-producto';

import { AuthService } from 'src/app/services/auth.service';
import { MovimientosProductoService } from '../../services/movimientos/movimientos-producto.service';

import { JqueryConfigs } from '../../utils/jquery/jquery-utils';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-movimientos-producto',
  templateUrl: './movimientos-producto.component.html',
  styles: [
  ]
})
export class MovimientosProductoComponent implements OnInit {

  title: string;
  jQueryConfigs: JqueryConfigs;

  movimientosProducto: MovimientoProducto[];

  constructor(
    private movimientosProductoService: MovimientosProductoService,
    public authService: AuthService
  ) {
    this.title = 'Movimientos de Productos Creados';
    this.jQueryConfigs = new JqueryConfigs();
  }

  ngOnInit(): void {
    this.getMovimientosProducto();
  }

  getMovimientosProducto(): void{
    this.movimientosProductoService.getMovimientosProducto().subscribe(
      movimientosProducto => {
        this.movimientosProducto = movimientosProducto;
        this.jQueryConfigs.configDataTable('movimientos');
      }
    ), error => {
      Swal.fire('Ha ocurrido un error', `Codigo: ${error.error.code} : ${error.error.message}`, 'error');
    };
  }

}
