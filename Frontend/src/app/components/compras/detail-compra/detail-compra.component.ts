import { DOCUMENT } from '@angular/common';
import { AfterViewInit, Component, ElementRef, HostListener, Inject, Input, OnDestroy, OnInit } from '@angular/core';
import { Compra } from 'src/app/models/compra';
import { DetailCompraService } from 'src/app/services/compras/detail-compra.service';

@Component({
  selector: 'app-detail-compra',
  templateUrl: './detail-compra.component.html',
  styleUrls: ['./detail-compra.component.css']
})
export class DetailCompraComponent implements OnInit, AfterViewInit, OnDestroy {

  title: string;

  @Input() compra: Compra;

  constructor(
    public detailCompraService: DetailCompraService,
    private elementRef: ElementRef<HTMLElement>,
    @Inject(DOCUMENT) private document: Document
  ) {
    this.title = 'Detalle de Compra';
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

  get nombreRegistro(): string {
    if (!this.compra?.usuario) {
      return 'No registrado';
    }
    const nombres = [this.compra.usuario.primerNombre, this.compra.usuario.apellido]
      .filter(nombre => !!nombre)
      .join(' ');
    return nombres || this.compra.usuario.usuario;
  }

  cerrarModal(): void {
    this.detailCompraService.cerrarModal();
  }

  @HostListener('document:keydown.escape')
  cerrarConEscape(): void {
    if (this.detailCompraService.modal) {
      this.cerrarModal();
    }
  }

  cerrarDesdeBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.cerrarModal();
    }
  }

}
