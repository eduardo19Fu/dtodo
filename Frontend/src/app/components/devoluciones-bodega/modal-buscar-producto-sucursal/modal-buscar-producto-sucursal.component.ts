import { Component, EventEmitter, HostListener, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { PaginacionTabla } from 'src/app/models/auxiliar/paginacion-tabla';
import { ProductoSucursalDto } from 'src/app/dtos/producto-sucursal-dto';
import { DevolucionBodegaService } from 'src/app/services/devolucion-bodega.service';

import Swal from 'sweetalert2';

/** Buscador de los productos que tiene una sucursal (tienda) con su existencia actual, para elegir qué devolver. */
@Component({
  selector: 'app-modal-buscar-producto-sucursal',
  templateUrl: './modal-buscar-producto-sucursal.component.html',
  styleUrls: [
    '../../movimientos-producto/create-movimiento/modal-buscar-producto-movimiento/modal-buscar-producto-movimiento.component.css'
  ]
})
export class ModalBuscarProductoSucursalComponent implements OnInit, OnDestroy {

  @Input() idSucursal: number;
  @Input() nombreSucursal: string;
  @Output() producto = new EventEmitter<ProductoSucursalDto>();
  @Output() cerrar = new EventEmitter<void>();

  title = 'Productos de la tienda';
  items: ProductoSucursalDto[] = [];
  paginacion = new PaginacionTabla(5, [5, 10, 15, 25]);
  filtro = '';
  cargando = false;

  private busquedaSubject = new Subject<string>();
  private busquedaSubscription: Subscription;

  constructor(private devolucionService: DevolucionBodegaService) { }

  ngOnInit(): void {
    this.cargar(0);
    this.busquedaSubscription = this.busquedaSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(filtro => {
      this.filtro = filtro;
      this.cargar(0);
    });
  }

  ngOnDestroy(): void {
    if (this.busquedaSubscription) {
      this.busquedaSubscription.unsubscribe();
    }
  }

  onBuscar(valor: string): void {
    this.busquedaSubject.next(valor);
  }

  cargar(page: number): void {
    this.cargando = true;
    this.devolucionService.getProductosSucursal(this.idSucursal, page, this.paginacion.pageSize, this.filtro).subscribe(
      response => {
        this.items = response.content;
        this.paginacion.actualizar(response);
        this.cargando = false;
      },
      error => {
        this.cargando = false;
        Swal.fire('Error al cargar productos', error.error?.message || 'Ha ocurrido un error inesperado', 'error');
      }
    );
  }

  cambiarPageSize(size: number): void {
    this.paginacion.pageSize = size;
    this.cargar(0);
  }

  elegir(item: ProductoSucursalDto): void {
    this.producto.emit(item);
  }

  cerrarModal(): void {
    this.cerrar.emit();
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.cerrarModal();
  }
}
