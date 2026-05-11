import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { NotaCredito } from '../../../models/nota-credito';
import { NotaCreditoDetalle } from '../../../models/nota-credito-detalle';
import { AuthService } from '../../../services/auth.service';
import { NotasCreditoService } from '../../../services/notas-credito.service';

import swal from 'sweetalert2';

@Component({
  selector: 'app-despachar-nota',
  templateUrl: './despachar-nota.component.html',
  styles: [
  ]
})
export class DespacharNotaComponent implements OnInit {

  title: string;
  notaCredito: NotaCredito;
  itemsADespachar: { item: NotaCreditoDetalle, cantidadDespachar: number, seleccionado: boolean }[] = [];

  constructor(
    private notaCreditoService: NotasCreditoService,
    private authService: AuthService,
    private activatedRoute: ActivatedRoute,
    private router: Router
  ) {
    this.title = 'Despacho de Nota de Crédito';
    this.notaCredito = new NotaCredito();
  }

  ngOnInit(): void {
    this.activatedRoute.paramMap.subscribe(params => {
      const id = +params.get('id');
      if (id) {
        this.cargarNota(id);
      }
    });
  }

  cargarNota(id: number): void {
    this.notaCreditoService.getNotaCredito(id).subscribe(
      nota => {
        this.notaCredito = nota;
        this.itemsADespachar = nota.items.map((item: NotaCreditoDetalle) => ({
          item: item,
          cantidadDespachar: 0,
          seleccionado: false
        }));
      },
      error => {
        swal.fire('Error', 'No se pudo cargar la nota de crédito.', 'error');
        this.router.navigate(['/notas-credito/index']);
      }
    );
  }

  toggleSeleccion(index: number): void {
    this.itemsADespachar[index].seleccionado = !this.itemsADespachar[index].seleccionado;
    if (!this.itemsADespachar[index].seleccionado) {
      this.itemsADespachar[index].cantidadDespachar = 0;
    }
  }

  actualizarCantidadDespacho(index: number, event): void {
    const cantidad = +event.target.value;
    const itemOriginal = this.itemsADespachar[index].item;

    if (cantidad < 0) {
      swal.fire('Cantidad Inválida', 'La cantidad no puede ser negativa.', 'warning');
      event.target.value = 0;
      this.itemsADespachar[index].cantidadDespachar = 0;
      return;
    }

    if (cantidad > itemOriginal.cantidad) {
      swal.fire('Cantidad Excedida', `La cantidad máxima para este producto es ${itemOriginal.cantidad}.`, 'warning');
      event.target.value = itemOriginal.cantidad;
      this.itemsADespachar[index].cantidadDespachar = itemOriginal.cantidad;
      return;
    }

    this.itemsADespachar[index].cantidadDespachar = cantidad;
  }

  hayItemsSeleccionados(): boolean {
    return this.itemsADespachar.some(i => i.seleccionado && i.cantidadDespachar > 0);
  }

  todosLosItemsCompletos(): boolean {
    return this.itemsADespachar.every(i => i.seleccionado && i.cantidadDespachar === i.item.cantidad);
  }

  despachar(): void {
    const itemsValidos = this.itemsADespachar.filter(i => i.seleccionado && i.cantidadDespachar > 0);

    if (itemsValidos.length === 0) {
      swal.fire('Sin Productos', 'Seleccione al menos un producto y defina la cantidad a despachar.', 'warning');
      return;
    }

    const resumen = itemsValidos.map(i =>
      `- ${i.item.producto.nombre}: ${i.cantidadDespachar} de ${i.item.cantidad}`
    ).join('<br>');

    swal.fire({
      title: 'Confirmar Despacho',
      html: `<p>Se despacharán los siguientes productos:</p>${resumen}`,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Confirmar Despacho',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        if (this.todosLosItemsCompletos()) {
          this.notaCreditoService.entregar(this.notaCredito.idNotaCredito).subscribe(
            response => {
              swal.fire('Despacho Exitoso', `La nota de crédito No. ${this.notaCredito.idNotaCredito} ha sido entregada completamente.`, 'success');
              this.router.navigate(['/notas-credito/index']);
            },
            error => {
              swal.fire('Error', 'No se pudo completar el despacho.', 'error');
            }
          );
        } else {
          this.notaCreditoService.entregar(this.notaCredito.idNotaCredito).subscribe(
            response => {
              swal.fire('Despacho Parcial Registrado', `Se registró el despacho parcial de la nota No. ${this.notaCredito.idNotaCredito}.`, 'success');
              this.router.navigate(['/notas-credito/index']);
            },
            error => {
              swal.fire('Error', 'No se pudo completar el despacho.', 'error');
            }
          );
        }
      }
    });
  }

  cancelar(): void {
    this.router.navigate(['/notas-credito/index']);
  }

}
