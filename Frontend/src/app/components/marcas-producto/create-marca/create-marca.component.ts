import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { UsuarioAuxiliar } from 'src/app/models/auxiliar/usuario-auxiliar';
import { MarcaProducto } from 'src/app/models/marca-producto';
import { AuthService } from 'src/app/services/auth.service';
import { MarcaProductoService } from 'src/app/services/marca-producto.service';
import { UsuarioService } from 'src/app/services/usuarios/usuario.service';

import swal from 'sweetalert2';

@Component({
  selector: 'app-create-marca',
  templateUrl: './create-marca.component.html',
  styleUrls: ['../../productos/create-producto/create-producto.component.css']
})
export class CreateMarcaComponent implements OnInit {

  title: string;
  marcaProducto: MarcaProducto;

  usuario: UsuarioAuxiliar;

  constructor(
    private marcaService: MarcaProductoService,
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private usuarioService: UsuarioService,
    private authService: AuthService
  ) {
    this.title = 'Registrar nueva marca';
    this.marcaProducto = new MarcaProducto();
  }

  ngOnInit(): void {
    this.cargarMarca();
  }

  cargarMarca(): void{
    // tslint:disable-next-line: deprecation
    this.activatedRoute.params.subscribe(params => {
      // tslint:disable-next-line: no-string-literal
      const id = params['id'];
      if (id){
        // tslint:disable-next-line: deprecation
        this.marcaService.getMarca(id).subscribe(
          marca => this.marcaProducto = marca
        );
      }
    });
  }

  create(): void {
    this.usuarioService.getUsuario(this.authService.usuario.idUsuario).subscribe(
      usuario => {

        this.marcaProducto.usuario = usuario;

        this.marcaService.create(this.marcaProducto).subscribe(
          response => {
            this.router.navigate(['/productos/marcas/index']);
            swal.fire('Marca Guardada', `${response.mensaje}: ${response.marca.marca}`, 'success');
          }
        );
      }
    );
  }

  update(): void{
    // tslint:disable-next-line: deprecation
    this.marcaService.update(this.marcaProducto).subscribe(
      response => {
        this.router.navigate(['/productos/marcas/index']);
        swal.fire('Marca Actualizada', `${response.mensaje}: ${response.marca.marca} fué actualizada con éxito!`, 'success');
      }
    );
  }

}
