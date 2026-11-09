import { Component, OnInit } from '@angular/core';

import { CategoriaNiveles } from 'src/app/dtos/descuento-dto';
import { CategoriaCliente } from 'src/app/models/cliente';
import { DescuentoService } from 'src/app/services/descuento.service';

import Swal from 'sweetalert2';

const DESCRIPCION_CATEGORIA: Record<CategoriaCliente, string> = {
  A: 'Compra frecuente',
  B: 'No frecuente, menos de 12 unidades',
  VOLUMEN: 'Frecuente, pide fardos o cientos (varios niveles: el vendedor elige)'
};

/**
 * Niveles de descuento por categoría de cliente. El precio mayorista y los descuentos por producto se configuran
 * en el formulario de cada producto y se consultan en Productos con el filtro «Mayorista».
 */
@Component({
  selector: 'app-descuentos',
  templateUrl: './descuentos.component.html',
  styleUrls: ['./descuentos.component.css', '../shared/management-page.css']
})
export class DescuentosComponent implements OnInit {

  title = 'Descuentos mayoristas';

  categorias: CategoriaNiveles[] = [];
  nuevoPorcentaje: { [categoria: string]: number } = {};
  guardandoCategoria: string = null;

  constructor(private descuentoService: DescuentoService) { }

  ngOnInit(): void {
    this.cargarCategorias();
  }

  descripcionCategoria(categoria: CategoriaCliente): string {
    return DESCRIPCION_CATEGORIA[categoria] || '';
  }

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

  private mostrarError(titulo: string, error: any): void {
    const mensaje = error?.error?.message || error?.error?.mensaje || 'Ha ocurrido un error inesperado.';
    Swal.fire(titulo, mensaje, error?.status === 422 ? 'warning' : 'error');
  }
}
