import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TipoProducto } from 'src/app/models/tipo-producto';
import { AuthService } from 'src/app/services/auth.service';
import { TipoProductoService } from 'src/app/services/tipo-producto.service';
import { UsuarioService } from 'src/app/services/usuarios/usuario.service';

import swal from 'sweetalert2';

@Component({
  selector: 'app-create-producto',
  templateUrl: './create-tipo.component.html',
  styles: [
  ]
})
export class CreateTipoComponent implements OnInit {

  title: string;
  tipoProducto: TipoProducto;

  constructor(
    private tipoService: TipoProductoService,
    private usuarioService: UsuarioService,
    private authService: AuthService,
    private router: Router,
    private activatedRoute: ActivatedRoute
  ) {
    this.title = 'Registrar Tipo de Producto';
    this.tipoProducto = new TipoProducto();
  }

  ngOnInit(): void {
    this.cargarTipoProducto();
  }

  cargarTipoProducto(): void{
    this.activatedRoute.params.subscribe(params => {
      // tslint:disable-next-line: no-string-literal
      const id = params['id'];
      if (id){
        // tslint:disable-next-line: deprecation
        this.tipoService.getTipoProducto(id).subscribe(
          tipoProducto => this.tipoProducto = tipoProducto
        );
      }
    });
  }

  create(): void{
    this.usuarioService.getUsuario(this.authService.usuario.idUsuario).subscribe(
      usuario => {
        this.tipoProducto.usuario = usuario;

        this.tipoService.create(this.tipoProducto).subscribe(
          response => {
            this.router.navigate(['/productos/categorias/index']);
            swal.fire('¡Categoría Creada!', `Categoría de Producto creada: ${response.tipoProducto}`, 'success');
          }
        );
      }
    );
  }

  update(): void{
    // tslint:disable-next-line: deprecation
    this.tipoService.update(this.tipoProducto).subscribe(
      response => {
        this.router.navigate(['/productos/categorias/index']);
        swal.fire('¡Categoría Actualizado!', `Categoría de Producto Actualizada: ${response.tipoProducto}`, 'success');
      }
    );
  }

}
