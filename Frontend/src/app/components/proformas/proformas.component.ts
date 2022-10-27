import { Component, OnInit } from '@angular/core';

import { Proforma } from '../../models/proforma';
import { ProformaService } from '../../services/proformas/proforma.service';

import { JqueryConfigs } from 'src/app/utils/jquery/jquery-utils';
import Swal from 'sweetalert2';

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

  proformas: Proforma[] = [];
  jQueryConfigs: JqueryConfigs;

  constructor(
    private proformaService: ProformaService
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
  
  getFacturasSP() {
    throw new Error('Method not implemented.');
  }

  cancel(): void {}
}
