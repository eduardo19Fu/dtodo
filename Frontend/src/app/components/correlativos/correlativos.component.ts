import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { Correlativo } from '../../models/correlativo';

import { CorrelativoService } from '../../services/correlativos/correlativo.service';
import { AuthService } from '../../services/auth.service';

import { JqueryConfigs } from '../../utils/jquery/jquery-utils';
import Swal from 'sweetalert2';
import { Estado } from '../../models/estado';

@Component({
  selector: 'app-correlativos',
  templateUrl: './correlativos.component.html',
  styles: [
  ]
})
export class CorrelativosComponent implements OnInit {

  title: string;
  jQueryConfigs: JqueryConfigs;

  correlativos: Correlativo[];

  swalWithBootstrapButtons = Swal.mixin({
    customClass: {
      confirmButton: 'btn btn-success',
      cancelButton: 'btn btn-danger'
    },
    buttonsStyling: true
  });

  constructor(
    private correlativoService: CorrelativoService,
    public authService: AuthService,
    private activatedRoute: ActivatedRoute
  ) {
    this.title = 'Listado de Correlativos';
    this.jQueryConfigs = new JqueryConfigs();
  }

  ngOnInit(): void {
    this.getCorrelativos();
  }

  getCorrelativos(): void{
    this.correlativoService.getCorrelativos().subscribe(
      correlativos => {
        this.correlativos = correlativos;
        this.jQueryConfigs.configDataTable('correlativos');
      }
    );
  }

  anularCorrelativo(correlativo: Correlativo): void{
    this.swalWithBootstrapButtons.fire({
      title: '¿Está seguro?',
      text: `¿Seguro que desea anular el correlativo ${correlativo.idCorrelativo} del cajero ${correlativo.usuario.usuario}`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: '¡Si, anular!',
      cancelButtonText: '¡No, cancelar!',
      reverseButtons: true
    }).then((result) => {
      if (result.isConfirmed) {

        // aqui va el codigo de confirmación para anular factura
        this.correlativoService.delete(correlativo.idCorrelativo).subscribe(response => {
          correlativo.estado = response.estado;
          this.swalWithBootstrapButtons.fire(
            '¡Anulado!',
            `El correlativo No. ${correlativo.idCorrelativo} ha sido anulado con éxito`,
            'success'
          );
        });

      } else if (
        /* Read more about handling dismissals below */
        result.dismiss === Swal.DismissReason.cancel
      ) {
        this.swalWithBootstrapButtons.fire(
          'Proceso Cancelado',
          `El correlativo No. ${correlativo.idCorrelativo} no ha sido anulado`,
          'error'
        );
      }
    });
  }

}
