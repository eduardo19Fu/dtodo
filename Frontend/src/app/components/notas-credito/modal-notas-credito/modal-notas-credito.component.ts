import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { NotaCreditoDto } from '../../../dtos/nota-credito-dto';
import { NotasCreditoService } from '../../../services/notas-credito.service';
import { JqueryConfigs } from '../../../utils/jquery/jquery-utils';

@Component({
  selector: 'app-modal-notas-credito',
  templateUrl: './modal-notas-credito.component.html',
  styleUrls: ['./modal-notas-credito.component.css']
})
export class ModalNotasCreditoComponent implements OnInit {

  @Output() notaCredito = new EventEmitter<NotaCreditoDto>();

  title: string;
  notasCredito: NotaCreditoDto[];

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

  chooseNotaCredito(notaCredito: NotaCreditoDto): void {
    this.notaCredito.emit(notaCredito);
    console.log(notaCredito);
  }

}
