import { HttpEventType } from '@angular/common/http';
import { Component, EventEmitter, HostListener, Input, OnDestroy, Output } from '@angular/core';
import { Producto } from 'src/app/models/producto';
import { AuthService } from 'src/app/services/auth.service';
import { ProductoService } from 'src/app/services/producto.service';

import swal from 'sweetalert2';

@Component({
  selector: 'app-detail-producto',
  templateUrl: './detail-producto.component.html',
  styleUrls: ['./detail-producto.component.css']
})
export class DetailProductoComponent implements OnDestroy {

  title: string;

  @Input() producto: Producto;
  @Output() cerrar = new EventEmitter<void>();
  @Output() productoActualizado = new EventEmitter<Producto>();

  public imagenSeleccionada: File;
  public progreso: number;
  cerrando = false;

  private cierreTimer: ReturnType<typeof setTimeout>;

  constructor(
    private serviceProducto: ProductoService,
    public auth: AuthService
  ) {
    this.title = 'Detalle del Producto';
    this.progreso = 0;
  }

  ngOnDestroy(): void {
    if (this.cierreTimer) {
      clearTimeout(this.cierreTimer);
    }
  }

  @HostListener('document:keydown.escape')
  cerrarConEscape(): void {
    this.cerrarModal();
  }

  seleccionarImagen(event): void {
    this.imagenSeleccionada = event.target.files[0];
    this.progreso = 0;

    if (this.imagenSeleccionada.type.indexOf('image') < 0) {
      swal.fire('Error: seleccionar imagen', 'El archivo debe de ser de tipo imagen', 'error');
      this.imagenSeleccionada = null;
    }
  }

  subirImagen(): void {

    if (!this.imagenSeleccionada) {
      swal.fire('Error: debe seleccionar una foto.', 'Debe seleccionar una foto', 'error');
    } else {
      // eslint-disable-next-line import/no-deprecated
      this.serviceProducto.uploadImage(this.imagenSeleccionada, this.producto.idProducto).subscribe(
        event => {
          if (event.type === HttpEventType.UploadProgress) {
            this.progreso = Math.round((event.loaded / event.total) * 100);
          } else if (event.type === HttpEventType.Response){
            // eslint-disable-next-line prefer-const
            let response: any = event.body;

            this.producto = response.producto as Producto;

            this.productoActualizado.emit(this.producto);
            swal.fire('Imagen ha sido subida con éxito', response.mensaje, 'success');
          }
        },
        error => {
          swal.fire(error.error.message, error.error.error, 'error');
        }
      );
    }
  }

  cerrarModal(): void{
    if (this.cerrando) {
      return;
    }

    this.cerrando = true;
    this.imagenSeleccionada = null;
    this.progreso = 0;

    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      this.cerrar.emit();
      return;
    }

    this.cierreTimer = setTimeout(() => this.cerrar.emit(), 180);
  }

}
