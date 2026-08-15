import { Component, Input, OnInit } from '@angular/core';
import { UsuarioAuxiliar } from 'src/app/models/auxiliar/usuario-auxiliar';
import { DetailUsuarioService } from 'src/app/services/usuarios/detail-usuario.service';

@Component({
  selector: 'app-detail-usuario',
  templateUrl: './detail-usuario.component.html',
  styleUrls: ['./detail-usuario.component.css']
})
export class DetailUsuarioComponent implements OnInit {

  title: string;

  @Input() usuario: UsuarioAuxiliar;

  constructor(
    public detailUsuarioService: DetailUsuarioService
  ) {
    this.title = 'Detalle de Usuario';
  }

  ngOnInit(): void {
  }

  cerrarModal(): void{
    this.detailUsuarioService.cerrarModal();
  }

}
