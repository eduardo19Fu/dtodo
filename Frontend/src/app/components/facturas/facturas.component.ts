import { Component, OnInit, EventEmitter, AfterViewInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { AuthService } from 'src/app/services/auth.service';
import { DetailService } from 'src/app/services/facturas/detail.service';
import { FacturaService } from 'src/app/services/facturas/factura.service';

import { Factura } from 'src/app/models/factura';
import { Usuario } from 'src/app/models/usuario';

import { JqueryConfigs } from '../../utils/jquery/jquery-utils';
import swal from 'sweetalert2';

@Component({
  selector: 'app-facturas',
  templateUrl: './facturas.component.html',
  styleUrls: ['./facturas.component.css']
})
export class FacturasComponent implements OnInit, AfterViewInit {

  title: string;
  fechaIni: Date;
  fechaFin: Date;

  facturas: Factura[];

  usuario: Usuario;
  facturaSeleccionada: Factura;
  jQueryConfigs: JqueryConfigs;

  swalWithBootstrapButtons = swal.mixin({
    customClass: {
      confirmButton: 'btn btn-success',
      cancelButton: 'btn btn-danger'
    },
    buttonsStyling: true
  });

  constructor(
    private detailService: DetailService,
    private facturaService: FacturaService,
    private activatedRoute: ActivatedRoute,
    public auth: AuthService
  ) {
    this.title = 'Facturas';
    this.jQueryConfigs = new JqueryConfigs();
    this.usuario = auth.usuario;
    this.facturas = [];
  }

  ngOnInit(): void {
    // this.getFacturas();
  }

  ngAfterViewInit(): void {
  }

  // getFacturas(): void {
  //   this.facturaService.getFacturas().subscribe(
  //     facturas => {
  //       this.facturas = facturas;
  //       this.jQueryConfigs.configDataTable('facturas');
  //     }
  //   );
  // }

  getFacturasSP(): void {
    this.facturas = [];
    if (this.fechaIni === undefined || this.fechaFin === undefined) {
      swal.fire('Advertencia', 'Porfavor ingrese un rango de fechas valido.', 'warning');
    } else {
      if (this.jQueryConfigs) {
        this.facturaService.getFacturasSP(this.fechaIni, this.fechaFin).subscribe(
          facturas => {
            this.facturas = facturas;
            this.jQueryConfigs.configDataTable('facturas');
            this.jQueryConfigs = new JqueryConfigs();
          }
        );
      }
    }
  }

  reloadPage(): void {
    location.reload();
  }

  abrirDetalle(factura: Factura): void {
    this.facturaSeleccionada = factura;
    this.detailService.abrirModal();
  }

  cancel(factura: Factura): void {
    this.swalWithBootstrapButtons.fire({
      title: '¿Está seguro?',
      text: `¿Seguro que desea anular la factura No. ${factura.noFactura}`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: '¡Si, anular!',
      cancelButtonText: '¡No, cancelar!',
      reverseButtons: true
    }).then((result) => {
      if (result.isConfirmed) {

        // aqui va el codigo de confirmación para anular factura
        this.facturaService.cancel(factura.idFactura, this.usuario.idUsuario).subscribe(
          response => {
            factura.estado = response.factura.estado;
            this.swalWithBootstrapButtons.fire(
              `${response.mensaje}`,
              `La factura No. ${factura.noFactura} ha sido anulada con éxito`,
              'success'
            );
          }
        );

      } else if (
        /* Read more about handling dismissals below */
        result.dismiss === swal.DismissReason.cancel
      ) {
        this.swalWithBootstrapButtons.fire(
          'Proceso Cancelado',
          `La factura No. ${factura.noFactura} no fué anulada.`,
          'error'
        );
      }
    });
  }

  printBill(factura: Factura): void {
    const id = factura.idFactura;

    /************ REGIMEN FEL *************/
    const url = 'https://report.feel.com.gt/ingfacereport/ingfacereport_documento?uuid=' + factura.certificacionSat;
    window.open(url, '_blank').focus();

    /************* FACTURA PDF ***************/

    // this.facturaService.getBillPDF(id).subscribe(res => {
    //   const url = window.URL.createObjectURL(res.data);
    //   const a = document.createElement('a');
    //   document.body.appendChild(a);
    //   a.setAttribute('style', 'display: none');
    //   a.setAttribute('target', 'blank');
    //   a.href = url;
    //   /*
    //     opcion para pedir descarga de la respuesta obtenida
    //     a.download = response.filename;
    //   */
    //   window.open(a.toString(), '_blank');
    //   window.URL.revokeObjectURL(url);
    //   a.remove();
    // },
    //   error => {
    //     console.log(error);
    //   });
  }
}
