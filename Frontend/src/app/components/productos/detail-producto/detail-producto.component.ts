import { DOCUMENT } from '@angular/common';
import { HttpEventType } from '@angular/common/http';
import { AfterViewInit, Component, ElementRef, HostListener, Inject, Input, OnDestroy, OnInit } from '@angular/core';
import { Producto } from 'src/app/models/producto';
import { AuthService } from 'src/app/services/auth.service';
import { ProductoService } from 'src/app/services/producto.service';
import { ModalService } from 'src/app/services/productos/modal.service';

import swal from 'sweetalert2';

@Component({
  selector: 'app-detail-producto',
  templateUrl: './detail-producto.component.html',
  styleUrls: ['./detail-producto.component.css']
})
export class DetailProductoComponent implements OnInit, AfterViewInit, OnDestroy {

  title: string;

  @Input() producto: Producto;

  public imagenSeleccionada: File;
  public progreso: number;

  constructor(
    public modalService: ModalService,
    private serviceProducto: ProductoService,
    public auth: AuthService,
    private elementRef: ElementRef<HTMLElement>,
    @Inject(DOCUMENT) private document: Document
  ) {
    this.title = 'Detalle del Producto';
    this.progreso = 0;
  }

  ngOnInit(): void {
    // this.getProducto();
  }

  ngAfterViewInit(): void {
    const hostElement = this.elementRef.nativeElement;
    this.document.body.appendChild(hostElement);
  }

  ngOnDestroy(): void {
    const hostElement = this.elementRef.nativeElement;

    if (hostElement.parentNode === this.document.body) {
      this.document.body.removeChild(hostElement);
    }
  }

  @HostListener('document:keydown.escape')
  cerrarConEscape(): void {
    if (this.modalService.modal) {
      this.cerrarModal();
    }
  }

  cerrarDesdeBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.cerrarModal();
    }
  }

  /*getProducto(): void {
    // tslint:disable-next-line: deprecation
    this.activatedRoute.paramMap.subscribe(params => {
      let id: number = +params.get('id');

      if (id) {
        // tslint:disable-next-line: deprecation
        this.serviceProducto.getProducto(id).subscribe(
          producto => this.producto = producto
        );
      }
    });
  }*/

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
      // tslint:disable-next-line: deprecation
      this.serviceProducto.uploadImage(this.imagenSeleccionada, this.producto.idProducto).subscribe(
        event => {
          if (event.type === HttpEventType.UploadProgress) {
            this.progreso = Math.round((event.loaded / event.total) * 100);
          } else if (event.type === HttpEventType.Response){
            // tslint:disable-next-line: prefer-const
            let response: any = event.body;

            this.producto = response.producto as Producto;

            this.modalService.notificarUpload.emit(this.producto);
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
    this.modalService.cerrarModal();
    this.imagenSeleccionada = null;
    this.progreso = 0;
  }

}
