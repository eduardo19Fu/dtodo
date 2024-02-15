import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { NotaCredito } from '../../../models/nota-credito';
import { NotasCreditoService } from '../../../services/notas-credito.service';
import { JqueryConfigs } from '../../../utils/jquery/jquery-utils';

@Component({
  selector: 'app-modal-notas-credito',
  templateUrl: './modal-notas-credito.component.html',
  styleUrls: ['./modal-notas-credito.component.css']
})
export class ModalNotasCreditoComponent implements OnInit {

  @Output() notaCredito = new EventEmitter<NotaCredito>();

  title: string;
  notasCredito: NotaCredito[];

  jQueryConfigs: JqueryConfigs = new JqueryConfigs();

  constructor(
    private notasService: NotasCreditoService
  ) {
    this.title = 'Notas de Credito';
  }

  ngOnInit(): void {
    this.loadNotasCredito();
  }

  loadNotasCredito(): void {
    this.notasService.getNotasCreditoActivas().subscribe(
      response => {
        this.notasCredito = response;
        this.jQueryConfigs.configDataTable('notas-credito');
      }
    );
  }

  chooseNotaCredito(notaCredito: NotaCredito): void {
    this.notaCredito.emit(notaCredito);
    console.log(notaCredito);
  }

}
