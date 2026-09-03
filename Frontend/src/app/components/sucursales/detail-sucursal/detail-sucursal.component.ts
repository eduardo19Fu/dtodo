import { DOCUMENT } from '@angular/common';
import { AfterViewInit, Component, ElementRef, HostListener, Inject, Input, OnDestroy, OnInit } from '@angular/core';
import { Sucursal } from 'src/app/models/sucursal';
import { DetailSucursalService } from 'src/app/services/sucursales/detail-sucursal.service';

@Component({
  selector: 'app-detail-sucursal',
  templateUrl: './detail-sucursal.component.html',
  styleUrls: ['./detail-sucursal.component.css']
})
export class DetailSucursalComponent implements OnInit, AfterViewInit, OnDestroy {

  title: string;

  @Input() sucursal: Sucursal;

  constructor(
    public detailSucursalService: DetailSucursalService,
    private elementRef: ElementRef<HTMLElement>,
    @Inject(DOCUMENT) private document: Document
  ) {
    this.title = 'Detalle de Sucursal';
  }

  ngOnInit(): void {
  }

  ngAfterViewInit(): void {
    this.document.body.appendChild(this.elementRef.nativeElement);
  }

  ngOnDestroy(): void {
    const hostElement = this.elementRef.nativeElement;

    if (hostElement.parentNode === this.document.body) {
      this.document.body.removeChild(hostElement);
    }
  }

  get nombreCreador(): string {
    if (!this.sucursal?.usuario) {
      return 'No registrado';
    }
    const nombres = [this.sucursal.usuario.primerNombre, this.sucursal.usuario.apellido]
      .filter(nombre => !!nombre)
      .join(' ');
    return nombres || this.sucursal.usuario.usuario;
  }

  cerrarModal(): void {
    this.detailSucursalService.cerrarModal();
  }

  @HostListener('document:keydown.escape')
  cerrarConEscape(): void {
    if (this.detailSucursalService.modal) {
      this.cerrarModal();
    }
  }

  cerrarDesdeBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.cerrarModal();
    }
  }

}
