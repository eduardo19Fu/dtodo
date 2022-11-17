import { Component, Input, OnInit } from '@angular/core';
import { Proforma } from 'src/app/models/proforma';
import { DetailService } from 'src/app/services/facturas/detail.service';

@Component({
  selector: 'app-detail-proforma',
  templateUrl: './detail-proforma.component.html',
  styleUrls: ['./detail-proforma.component.css']
})
export class DetailProformaComponent implements OnInit {

  @Input() proforma: Proforma;

  title: string;

  constructor(
    public detailService: DetailService
  ) {
    this.title = 'Detalle de Proforma';
  }

  ngOnInit(): void {
  }

  cerrarModal(): void {
    this.detailService.cerrarModal();
  }

}
