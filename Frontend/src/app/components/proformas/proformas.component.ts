import { Component, OnInit } from '@angular/core';

import { Proforma } from '../../models/proforma';
import { ProformaService } from '../../services/proformas/proforma.service';

import { JqueryConfigs } from 'src/app/utils/jquery/jquery-utils';
import Swal from 'sweetalert2';
import { AuthService } from 'src/app/services/auth.service';
import { DetailService } from 'src/app/services/facturas/detail.service';

@Component({
  selector: 'app-proformas',
  templateUrl: './proformas.component.html',
  styles: [
  ]
})
export class ProformasComponent implements OnInit {

  title: string;
  fechaIni: Date;
  fechaFin: Date;

  proformaSeleccionada: Proforma;

  proformas: Proforma[] = [];
  jQueryConfigs: JqueryConfigs;

  constructor(
    private proformaService: ProformaService,
    public auth: AuthService,
    public detailService: DetailService
  ) {
    this.title = 'Listado de Proformas';
    this.jQueryConfigs = new JqueryConfigs();
  }

  ngOnInit(): void {
  }

  loadProformasSp(): void {
    this.proformaService.getProformasSP(this.fechaIni, this.fechaFin).subscribe(response => {
      if (this.fechaIni === undefined || this.fechaFin === undefined) {
        Swal.fire('Advertencia', 'Porfavor ingrese un rango de fechas valido.', 'warning');
      } else {
        if (this.jQueryConfigs) {
          this.getProformasSp();
        }
      }
    });
  }

  getProformasSp(): void {
    this.proformaService.getProformasSP(this.fechaIni, this.fechaFin).subscribe(response => {
      this.proformas = response;
      this.jQueryConfigs.configDataTable("proformas");
      this.jQueryConfigs = new JqueryConfigs();
    })
  }


  reloadPage() {
    location.reload();
  }

  printProforma(proforma: Proforma): void {
    this.proformaService.getProformaPdf(proforma.idProforma).subscribe(response => {
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

  cancel(): void {}

  abrirDetalle(proforma: Proforma): void {
    this.proformaSeleccionada = proforma;
    this.detailService.abrirModal();
  }
}
