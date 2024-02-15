import { Component, Input, OnInit } from '@angular/core';

import { NotaCredito } from '../../../models/nota-credito';
import { DetailService } from '../../../services/facturas/detail.service';

@Component({
  selector: 'app-detail-nota',
  templateUrl: './detail-nota.component.html',
  styleUrls: ['./detail-nota.component.css']
})
export class DetailNotaComponent implements OnInit {

  title: string;

  @Input() notaCredito: NotaCredito;

  constructor(
    public detailService: DetailService
  ) {
    this.title = 'Detalle de Nota de Credito ';
  }

  ngOnInit(): void {
  }

  cerrarModal(): void{
    this.detailService.cerrarModal();
  }

}
