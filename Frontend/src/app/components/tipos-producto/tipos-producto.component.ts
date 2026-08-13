import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { AuthService } from 'src/app/services/auth.service';
import { TipoProductoService } from 'src/app/services/tipo-producto.service';

import { TipoProducto } from 'src/app/models/tipo-producto';

import { JqueryConfigs } from '../../utils/jquery/jquery-utils';
import swal from 'sweetalert2';

@Component({
  selector: 'app-tipos-producto',
  templateUrl: './tipos-producto.component.html',
  styles: [
  ]
})
export class TiposProductoComponent implements OnInit {

  title: string;
  tipos: TipoProducto[];

  jQueryConfigs: JqueryConfigs;

  swalWithBootstrapButtons = swal.mixin({
    customClass: {
      confirmButton: 'btn btn-success',
      cancelButton: 'btn btn-danger'
    },
    buttonsStyling: true
  });

  constructor(
    private tipoService: TipoProductoService,
    public auth: AuthService
  ) {
    this.title = 'Listado de Categorías';
    this.jQueryConfigs = new JqueryConfigs();
  }

  ngOnInit(): void {
    this.getTipos();
  }

  // listado de tipos normal
  getTipos(): void {
    this.tipoService.getTiposProducto().subscribe(
      tiposProducto => {
        this.tipos = tiposProducto;
        this.jQueryConfigs.configDataTable('tipos');
        this.jQueryConfigs.configToolTip();
      }
    );
  }

  delete(tipoProducto: TipoProducto): void {
    this.swalWithBootstrapButtons.fire({
      title: '¿Está seguro?',
      text: `¿Seguro que desea eliminar ${tipoProducto.tipoProducto}?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: '¡Si, eliminar!',
      cancelButtonText: '¡No, cancelar!',
      reverseButtons: true
    }).then((result) => {
      if (result.isConfirmed) {

        this.tipoService.delete(tipoProducto.idTipoProducto).subscribe(
          response => {
            this.tipos = this.tipos.filter(cli => cli !== tipoProducto);
            this.swalWithBootstrapButtons.fire(
              '¡Tipo Producto Eliminado!',
              'El registro ha sido eliminado con éxito!',
              'success'
            );
          }, error => {
            console.log('Ha ocurrido un error inesperado');
          }
        );
      } else if (
        /* Read more about handling dismissals below */
        result.dismiss === swal.DismissReason.cancel
      ) {
        this.swalWithBootstrapButtons.fire(
          'Proceso Cancelado',
          'El registro no fué eliminado de la base de datos.',
          'error'
        );
      }
    });
  }

}
