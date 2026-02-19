import { Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output } from '@angular/core';
import { Proforma } from '../../../models/proforma';
import { DetailService } from '../../../services/facturas/detail.service';
import { ProformaService } from '../../../services/proformas/proforma.service';
import { ProformaDto } from 'src/app/dtos/proformaDto';

@Component({
  selector: 'app-detail-proforma',
  templateUrl: './detail-proforma.component.html',
  styleUrls: ['./detail-proforma.component.css']
})
export class DetailProformaComponent implements OnChanges {
  
  @Input() proformadto: ProformaDto;
  @Output() closeModal = new EventEmitter<void>();

  title: string;
  proforma: Proforma;

  constructor(
    public detailService: DetailService,
    private proformaService: ProformaService
  ) {
    this.title = 'Detalle de Proforma';
    this.proforma = new Proforma();
  }

  ngOnChanges(): void {
    this.loadProforma();
  }

  cerrarModal(): void {
    this.detailService.cerrarModal();
    this.proforma = new Proforma();
    // this.proformadto = undefined;
    this.closeModal.emit;
  }

  loadProforma(): void {
    this.proformaService.getProforma(this.proformadto.idProforma).subscribe(response => {
      console.log(response);
      this.proforma = response;
    });
  }

}
