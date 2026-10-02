import { Component, EventEmitter, HostListener, Input, OnInit, Output } from '@angular/core';

import { Bodega } from 'src/app/models/bodega';
import { InventarioBodegaDto } from 'src/app/dtos/inventario-bodega-dto';
import { MovimientoBodegaRequest } from 'src/app/dtos/movimiento-bodega-request';
import { ProductoDto } from 'src/app/dtos/productoDto';
import { BodegaService } from 'src/app/services/bodega.service';
import { ProductoService } from 'src/app/services/producto.service';

import Swal from 'sweetalert2';

export type ModoMovimientoBodega = 'AGREGAR' | 'REDUCIR' | 'ELIMINAR';

interface ProductoSeleccionado {
  idProducto: number;
  codProducto: string;
  nombre: string;
}

/** Formulario de los movimientos manuales de una bodega: agregar producto, reducir existencias o eliminar producto. */
@Component({
  selector: 'app-modal-movimiento-bodega',
  templateUrl: './modal-movimiento-bodega.component.html',
  styleUrls: ['./modal-movimiento-bodega.component.css']
})
export class ModalMovimientoBodegaComponent implements OnInit {

  @Input() modo: ModoMovimientoBodega = 'AGREGAR';
  @Input() bodega: Bodega;
  /** Fila del inventario sobre la que se reduce o elimina; en AGREGAR es opcional. */
  @Input() item: InventarioBodegaDto;
  @Output() completado = new EventEmitter<void>();
  @Output() cerrar = new EventEmitter<void>();

  producto: ProductoSeleccionado = null;
  /** Existencia actual del producto en la bodega (0 si aún no está en el inventario). */
  existenciaActual: number = null;
  codigoBusqueda = '';
  cantidad: number = null;
  stockMinimo: number = null;
  motivo = '';
  catalogoVisible = false;
  buscando = false;
  guardando = false;

  constructor(
    private bodegaService: BodegaService,
    private productoService: ProductoService
  ) { }

  ngOnInit(): void {
    if (this.item) {
      this.producto = {
        idProducto: this.item.idProducto,
        codProducto: this.item.codProducto,
        nombre: this.item.nombreProducto
      };
      this.existenciaActual = this.item.stock;
      this.stockMinimo = this.item.stockMinimo;
    }
  }

  get titulo(): string {
    switch (this.modo) {
      case 'AGREGAR': return 'Agregar producto a la bodega';
      case 'REDUCIR': return 'Reducir existencias';
      default: return 'Eliminar producto de la bodega';
    }
  }

  get descripcion(): string {
    switch (this.modo) {
      case 'AGREGAR': return 'Suma unidades al inventario. Si el producto ya está en la bodega, se acumulan a su existencia.';
      case 'REDUCIR': return 'Resta unidades por merma, daño o ajuste. Queda registrado con su motivo.';
      default: return 'Retira el producto del inventario descontando toda su existencia. Queda registrado con su motivo.';
    }
  }

  get icono(): string {
    switch (this.modo) {
      case 'AGREGAR': return 'fa-plus-circle';
      case 'REDUCIR': return 'fa-minus-circle';
      default: return 'fa-trash-alt';
    }
  }

  get textoConfirmar(): string {
    switch (this.modo) {
      case 'AGREGAR': return 'Agregar a la bodega';
      case 'REDUCIR': return 'Registrar reducción';
      default: return 'Eliminar producto';
    }
  }

  get motivoObligatorio(): boolean {
    return this.modo !== 'AGREGAR';
  }

  get existenciaResultante(): number {
    if (this.existenciaActual === null) {
      return null;
    }
    if (this.modo === 'ELIMINAR') {
      return 0;
    }
    const cantidad = Number(this.cantidad) || 0;
    return this.modo === 'AGREGAR' ? this.existenciaActual + cantidad : this.existenciaActual - cantidad;
  }

  get cantidadValida(): boolean {
    const cantidad = Number(this.cantidad);
    if (!Number.isInteger(cantidad) || cantidad <= 0) {
      return false;
    }
    return this.modo !== 'REDUCIR' || cantidad <= (this.existenciaActual || 0);
  }

  get errorCantidad(): string {
    if (this.cantidad === null || this.cantidad === undefined || String(this.cantidad) === '') {
      return '';
    }
    const cantidad = Number(this.cantidad);
    if (!Number.isInteger(cantidad) || cantidad <= 0) {
      return 'La cantidad debe ser un número entero mayor a 0.';
    }
    if (this.modo === 'REDUCIR' && cantidad > (this.existenciaActual || 0)) {
      return `Solo hay ${this.existenciaActual} unidades en la bodega.`;
    }
    return '';
  }

  get puedeGuardar(): boolean {
    if (this.guardando || !this.producto) {
      return false;
    }
    if (this.motivoObligatorio && !this.motivo.trim()) {
      return false;
    }
    if (this.stockMinimo !== null && this.stockMinimo !== undefined && String(this.stockMinimo) !== '' &&
      (!Number.isInteger(Number(this.stockMinimo)) || Number(this.stockMinimo) < 0)) {
      return false;
    }
    return this.modo === 'ELIMINAR' || this.cantidadValida;
  }

  /*********** SELECCIÓN DE PRODUCTO (solo AGREGAR) ***********/

  buscarPorCodigo(evento?: Event): void {
    if (evento) {
      evento.preventDefault();
    }
    const codigo = (this.codigoBusqueda || '').trim();
    if (!codigo) {
      Swal.fire('Código inválido', 'Ingresa un código de producto.', 'warning');
      return;
    }
    this.buscando = true;
    this.productoService.buscarProductosDto(0, codigo, 20).subscribe(
      respuesta => {
        this.buscando = false;
        const exactos: ProductoDto[] = (respuesta.content || [])
          .filter((producto: ProductoDto) => (producto.codProducto || '').toLowerCase() === codigo.toLowerCase());
        if (exactos.length === 1) {
          this.seleccionarProducto(exactos[0]);
        } else {
          Swal.fire(exactos.length > 1 ? 'Código repetido' : 'Producto no encontrado',
            exactos.length > 1
              ? 'Varios productos comparten ese código; elígelo desde el catálogo.'
              : 'No existe un producto con ese código. Prueba con el catálogo.',
            'info');
        }
      },
      () => {
        this.buscando = false;
        Swal.fire('No se pudo buscar el producto', 'Intenta nuevamente.', 'error');
      }
    );
  }

  abrirCatalogo(): void {
    this.catalogoVisible = true;
  }

  cerrarCatalogo(): void {
    this.catalogoVisible = false;
  }

  elegirDelCatalogo(producto: any): void {
    this.cerrarCatalogo();
    this.seleccionarProducto(producto as ProductoDto);
  }

  quitarProducto(): void {
    this.producto = null;
    this.existenciaActual = null;
    this.codigoBusqueda = '';
  }

  private seleccionarProducto(dto: ProductoDto): void {
    this.producto = { idProducto: dto.idProducto, codProducto: dto.codProducto, nombre: dto.nombre };
    this.codigoBusqueda = dto.codProducto;
    this.existenciaActual = null;
    this.bodegaService.getInventarioPorCodigo(this.bodega.idBodega, dto.codProducto).subscribe(
      (inventario: InventarioBodegaDto) => {
        this.existenciaActual = inventario.stock;
        this.stockMinimo = inventario.stockMinimo;
      },
      () => this.existenciaActual = 0 // 404: el producto todavía no está en el inventario de la bodega
    );
  }

  /*********** GUARDADO ***********/

  guardar(): void {
    if (!this.puedeGuardar) {
      return;
    }
    this.guardando = true;
    const request: MovimientoBodegaRequest = {
      idProducto: this.producto.idProducto,
      cantidad: Number(this.cantidad),
      motivo: this.motivo.trim() || undefined
    };

    let operacion;
    if (this.modo === 'AGREGAR') {
      const minimo = this.stockMinimo;
      request.stockMinimo = minimo === null || minimo === undefined || String(minimo) === '' ? undefined : Number(minimo);
      operacion = this.bodegaService.agregarProducto(this.bodega.idBodega, request);
    } else if (this.modo === 'REDUCIR') {
      operacion = this.bodegaService.reducirExistencias(this.bodega.idBodega, request);
    } else {
      operacion = this.bodegaService.eliminarProducto(this.bodega.idBodega, this.producto.idProducto, this.motivo.trim());
    }

    operacion.subscribe(
      (resultado: any) => {
        this.guardando = false;
        Swal.fire({
          toast: true,
          position: 'top-end',
          icon: 'success',
          title: resultado.mensaje,
          text: `Existencia: ${resultado.stockInicial} → ${resultado.stockFinal}`,
          showConfirmButton: false,
          timer: 3500
        });
        this.completado.emit();
      },
      () => this.guardando = false
    );
  }

  cerrarModal(): void {
    if (!this.guardando) {
      this.cerrar.emit();
    }
  }

  @HostListener('document:keydown.escape')
  alPresionarEscape(): void {
    if (!this.catalogoVisible) {
      this.cerrarModal();
    }
  }
}
