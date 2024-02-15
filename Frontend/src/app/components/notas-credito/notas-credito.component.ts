import { Component, OnInit } from '@angular/core';
import { NotaCredito } from 'src/app/models/nota-credito';
import { AuthService } from 'src/app/services/auth.service';
import { DetailService } from 'src/app/services/facturas/detail.service';
import { NotasCreditoService } from 'src/app/services/notas-credito.service';
import { JqueryConfigs } from 'src/app/utils/jquery/jquery-utils';

@Component({
  selector: 'app-notas-credito',
  templateUrl: './notas-credito.component.html'
})
export class NotasCreditoComponent implements OnInit {

  title: string;
  jQueryConfigs: JqueryConfigs;

  notasCredito: NotaCredito[];
  notaSeleccionada: NotaCredito;

  constructor(
    private notasService: NotasCreditoService,
    public auth: AuthService,
    public detailService: DetailService
  ) {
    this.title = 'Notas de Crédito';
    this.jQueryConfigs = new JqueryConfigs();
  }

  ngOnInit(): void {
    this.loadNotasCredito();
  }

  loadNotasCredito(): void {
    this.notasService.getNotasCredito().subscribe(res => {
      this.notasCredito = res;
      this.jQueryConfigs.configDataTable('notas-credito');
    });
  }

  abrirDetalle(nota: NotaCredito): void {
    this.notaSeleccionada = nota;
    this.detailService.abrirModal();
  }

}
