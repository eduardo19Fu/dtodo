import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MarcaProducto } from 'src/app/models/marca-producto';
import { AuthService } from 'src/app/services/auth.service';
import { MarcaProductoService } from 'src/app/services/marca-producto.service';

import { JqueryConfigs } from '../../utils/jquery/jquery-utils';
import swal from 'sweetalert2';

@Component({
  selector: 'app-marcas-producto',
  templateUrl: './marcas-producto.component.html',
  styles: [
  ]
})
export class MarcasProductoComponent implements OnInit {

  title: string;
  marcas: MarcaProducto[];

  jQueryConfigs: JqueryConfigs;

  swalWithBootstrapButtons = swal.mixin({
    customClass: {
      confirmButton: 'btn btn-success',
      cancelButton: 'btn btn-danger'
    },
    buttonsStyling: true
  });

  constructor(
    private marcaService: MarcaProductoService,
    private activatedRoute: ActivatedRoute,
    public auth: AuthService
  ) {
    this.title = 'Listado de Marcas de Productos';
    this.jQueryConfigs = new JqueryConfigs();
  }

  ngOnInit(): void {
    this.getMarcas();
  }

  getMarcas(): void{
    this.marcaService.getMarcas().subscribe(
      marcas => {
        this.marcas = marcas;
        this.jQueryConfigs.configDataTable('marcas');
        this.jQueryConfigs.configToolTip();
      }
    );
  }

  delete(marcaProducto: MarcaProducto): void{
    this.swalWithBootstrapButtons.fire({
      title: '¿Está seguro?',
      text: `¿Seguro que desea eliminar ${marcaProducto.marca}?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: '¡Si, eliminar!',
      cancelButtonText: '¡No, cancelar!',
      reverseButtons: true
    }).then((result) => {
      if (result.isConfirmed) {

        this.marcaService.delete(marcaProducto.idMarcaProducto).subscribe(
          response => {
            this.marcas = this.marcas.filter(cli => cli !== marcaProducto);
            this.swalWithBootstrapButtons.fire(
              '¡Marca Eliminada!',
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
