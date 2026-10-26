import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { CategoriaNiveles, DescuentoProducto, ProductoDescuentos } from 'src/app/dtos/descuento-dto';
import { CategoriaCliente } from 'src/app/models/cliente';
import { DescuentoService } from 'src/app/services/descuento.service';

import Swal from 'sweetalert2';

/** Descuento especial en edición: se guarda solo si está activo. */
class DescuentoEnEdicion {
  activo = false;
  cantidadMinima: number = null;
  porcentaje: number = null;
}

class ProductoEnEdicion {
  idProducto: number;
  nombre: string;
  precioVenta: number;
  precioMayorista: number = null;
  ciento = new DescuentoEnEdicion();
  fardo = new DescuentoEnEdicion();
}

const DESCRIPCION_CATEGORIA: Record<CategoriaCliente, string> = {
  A: 'Compra frecuente',
  B: 'No frecuente, menos de 12 unidades',
  VOLUMEN: 'Frecuente, pide fardos o cientos (varios niveles: el vendedor elige)'
};

@Component({
  selector: 'app-descuentos',
  templateUrl: './descuentos.component.html',
  styleUrls: ['./descuentos.component.css']
})
export class DescuentosComponent implements OnInit, OnDestroy {

  title = 'Descuentos mayoristas';

  categorias: CategoriaNiveles[] = [];
  nuevoPorcentaje: { [categoria: string]: number } = {};
  guardandoCategoria: string = null;

  productos: ProductoDescuentos[] = [];
  paginaActual = 0;
  totalPaginas = 0;
  totalElementos = 0;
  readonly pageSize = 10;
  isFirst = true;
  isLast = true;
  filtro = '';
  soloConDescuentos = true;
  cargando = false;

  edicion: ProductoEnEdicion = null;
  guardandoProducto = false;

  private busquedaSubject = new Subject<string>();
  private busquedaSubscription: Subscription;

  constructor(private descuentoService: DescuentoService) { }

  ngOnInit(): void {
    this.cargarCategorias();
    this.cargarProductos(0);
    this.busquedaSubscription = this.busquedaSubject.pipe(debounceTime(300), distinctUntilChanged()).subscribe(filtro => {
      this.filtro = filtro;
      this.cargarProductos(0);
    });
  }

  ngOnDestroy(): void {
    if (this.busquedaSubscription) {
      this.busquedaSubscription.unsubscribe();
    }
  }

  descripcionCategoria(categoria: CategoriaCliente): string {
    return DESCRIPCION_CATEGORIA[categoria] || '';
  }

  // ------------------------------------------------------------ categorías de cliente

  cargarCategorias(): void {
    this.descuentoService.getCategorias().subscribe(
      categorias => this.categorias = categorias,
      error => this.mostrarError('No se pudieron cargar las categorías', error)
    );
  }

  agregarNivel(categoria: CategoriaNiveles): void {
    const valor = Number(this.nuevoPorcentaje[categoria.categoria]);
    if (!Number.isFinite(valor) || valor <= 0 || valor > 100) {
      Swal.fire('Porcentaje inválido', 'Escribe un porcentaje mayor a 0 y hasta 100.', 'warning');
      return;
    }
    if (categoria.porcentajes.some(porcentaje => Number(porcentaje) === valor)) {
      Swal.fire('Nivel repetido', `La categoría ${categoria.categoria} ya tiene el nivel de ${valor} %.`, 'warning');
      return;
    }
    categoria.porcentajes = [...categoria.porcentajes, valor].sort((a, b) => a - b);
    this.nuevoPorcentaje[categoria.categoria] = null;
  }

  quitarNivel(categoria: CategoriaNiveles, porcentaje: number): void {
    categoria.porcentajes = categoria.porcentajes.filter(nivel => nivel !== porcentaje);
  }

  guardarCategoria(categoria: CategoriaNiveles): void {
    this.guardandoCategoria = categoria.categoria;
    this.descuentoService.guardarCategoria(categoria.categoria, categoria.porcentajes).subscribe(
      guardada => {
        this.guardandoCategoria = null;
        categoria.porcentajes = guardada.porcentajes;
        Swal.fire('Categoría actualizada', `Categoría ${categoria.categoria}: ${guardada.porcentajes.join(' %, ') || 'sin niveles'}`
          + (guardada.porcentajes.length ? ' %' : ''), 'success');
      },
      error => {
        this.guardandoCategoria = null;
        this.mostrarError('No se pudo guardar la categoría', error);
      }
    );
  }

  // ------------------------------------------------------------ productos

  cargarProductos(page: number): void {
    this.cargando = true;
    this.descuentoService.getProductos(page, this.pageSize, this.filtro, this.soloConDescuentos).subscribe(
      response => {
        this.productos = response.content;
        this.paginaActual = response.number;
        this.totalPaginas = response.totalPages;
        this.totalElementos = response.totalElements;
        this.isFirst = response.first;
        this.isLast = response.last;
        this.cargando = false;
      },
      error => {
        this.cargando = false;
        this.mostrarError('No se pudieron cargar los productos', error);
      }
    );
  }

  onBuscar(valor: string): void { this.busquedaSubject.next(valor); }

  cambiarSoloConDescuentos(): void { this.cargarProductos(0); }
  irPaginaAnterior(): void { if (!this.isFirst) { this.cargarProductos(this.paginaActual - 1); } }
  irPaginaSiguiente(): void { if (!this.isLast) { this.cargarProductos(this.paginaActual + 1); } }

  especial(producto: ProductoDescuentos, tipo: 'CIENTO' | 'FARDO'): DescuentoProducto {
    return producto.descuentos.find(descuento => descuento.tipo === tipo) || null;
  }

  editar(producto: ProductoDescuentos): void {
    const edicion = new ProductoEnEdicion();
    edicion.idProducto = producto.idProducto;
    edicion.nombre = producto.nombre;
    edicion.precioVenta = producto.precioVenta;
    edicion.precioMayorista = producto.precioMayorista;
    for (const [tipo, destino] of [['CIENTO', edicion.ciento], ['FARDO', edicion.fardo]] as const) {
      const existente = this.especial(producto, tipo);
      if (existente) {
        destino.activo = true;
        destino.cantidadMinima = existente.cantidadMinima;
        destino.porcentaje = existente.porcentaje;
      }
    }
    if (!this.especial(producto, 'CIENTO')) {
      edicion.ciento.cantidadMinima = 100;
    }
    this.edicion = edicion;
  }

  cancelarEdicion(): void {
    this.edicion = null;
  }

  guardarProducto(): void {
    if (!this.edicion || this.guardandoProducto) {
      return;
    }
    const edicion = this.edicion;
    const precio = edicion.precioMayorista === null || (edicion.precioMayorista as any) === ''
      ? null : Number(edicion.precioMayorista);
    const descuentos: DescuentoProducto[] = [];
    for (const [tipo, fila] of [['CIENTO', edicion.ciento], ['FARDO', edicion.fardo]] as const) {
      if (fila.activo) {
        const descuento = new DescuentoProducto();
        descuento.tipo = tipo;
        descuento.cantidadMinima = Number(fila.cantidadMinima);
        descuento.porcentaje = Number(fila.porcentaje);
        descuentos.push(descuento);
      }
    }

    this.guardandoProducto = true;
    this.descuentoService.guardarProducto(edicion.idProducto, { precioMayorista: precio, descuentos }).subscribe(
      guardado => {
        this.guardandoProducto = false;
        this.edicion = null;
        Swal.fire('Producto actualizado', `${guardado.nombre} se guardó con éxito.`, 'success');
        this.cargarProductos(this.paginaActual);
      },
      error => {
        this.guardandoProducto = false;
        this.mostrarError('No se pudo guardar el producto', error);
      }
    );
  }

  private mostrarError(titulo: string, error: any): void {
    const mensaje = error?.error?.message || error?.error?.mensaje || 'Ha ocurrido un error inesperado.';
    Swal.fire(titulo, mensaje, error?.status === 422 ? 'warning' : 'error');
  }
}
