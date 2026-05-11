import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { NotaCredito } from 'src/app/models/nota-credito';
import { NotaCreditoDetalle } from 'src/app/models/nota-credito-detalle';
import { DespachoNota } from 'src/app/models/despacho-nota';
import { DespachoNotaDto } from 'src/app/dtos/despacho-nota-dto';
import { NotaCreditoListDto } from 'src/app/dtos/nota-credito-list-dto';
import { AuthService } from 'src/app/services/auth.service';
import { DetailService } from 'src/app/services/facturas/detail.service';
import { NotasCreditoService } from 'src/app/services/notas-credito.service';
import { JqueryConfigs } from 'src/app/utils/jquery/jquery-utils';

import swal from 'sweetalert2';

@Component({
  selector: 'app-notas-credito',
  templateUrl: './notas-credito.component.html',
  styleUrls: ['./notas-credito.component.css']
})
export class NotasCreditoComponent implements OnInit {

  title: string;
  jQueryConfigs: JqueryConfigs;

  notasCredito: NotaCreditoListDto[];
  notaSeleccionada: NotaCredito;

  // Modal despacho
  mostrarModalDespacho: boolean = false;
  notaDespacho: NotaCredito;
  itemsDespacho: { item: NotaCreditoDetalle, cantidadDespachar: number, despachado: number, seleccionado: boolean }[] = [];
  cargandoDespacho: boolean = false;

  swalWithBootstrapButtons = swal.mixin({
    customClass: {
      confirmButton: 'btn btn-success',
      cancelButton: 'btn btn-danger'
    },
    buttonsStyling: true
  });

  constructor(
    private notasService: NotasCreditoService,
    public auth: AuthService,
    public detailService: DetailService,
    private router: Router
  ) {
    this.title = 'Notas de Crédito';
    this.jQueryConfigs = new JqueryConfigs();
  }

  ngOnInit(): void {
    this.loadNotasCredito();
  }

  loadNotasCredito(): void {
    this.notasService.getNotasCredito().subscribe(res => {
      this.notasCredito = res || [];
      if (this.notasCredito.length > 0) {
        this.jQueryConfigs.configDataTable('notas-credito');
      }
    });
  }

  abrirDetalle(nota: NotaCreditoListDto): void {
    swal.fire({
      toast: true,
      position: 'top-end',
      icon: 'warning',
      title: `Cargando detalle de la nota No. ${nota.idNotaCredito}...`,
      showConfirmButton: false,
      didOpen: () => swal.showLoading()
    });

    this.notasService.getNotaCredito(nota.idNotaCredito).subscribe(
      notaCompleta => {
        this.notaSeleccionada = notaCompleta;
        swal.close();
        this.detailService.abrirModal();
      },
      () => swal.close()
    );
  }

  // --- Modal de Despacho ---

  despacharProductos(nota: NotaCreditoListDto): void {
    this.cargandoDespacho = true;
    this.mostrarModalDespacho = true;

    // Cargar la nota completa con sus items y los despachos existentes
    this.notasService.getNotaCredito(nota.idNotaCredito).subscribe(
      notaCompleta => {
        this.notaDespacho = notaCompleta;
        // Cargar despachos previos para calcular pendientes
        this.notasService.getDespachos(nota.idNotaCredito).subscribe(
          despachos => {
            this.itemsDespacho = notaCompleta.items.map((item: NotaCreditoDetalle) => {
              const despachado = this.calcularDespachadoPorProducto(despachos, item.producto.idProducto);
              return {
                item: item,
                cantidadDespachar: 0,
                despachado: despachado,
                seleccionado: false
              };
            });
            this.cargandoDespacho = false;
          },
          () => {
            // Si falla cargar despachos, asumir 0 despachado
            this.itemsDespacho = notaCompleta.items.map((item: NotaCreditoDetalle) => ({
              item: item,
              cantidadDespachar: 0,
              despachado: 0,
              seleccionado: false
            }));
            this.cargandoDespacho = false;
          }
        );
      },
      () => {
        this.mostrarModalDespacho = false;
        this.cargandoDespacho = false;
      }
    );
  }

  calcularDespachadoPorProducto(despachos: DespachoNotaDto[], idProducto: number): number {
    return despachos
      .filter(d => d.idProducto === idProducto)
      .reduce((sum, d) => sum + d.cantidad, 0);
  }

  cerrarModalDespacho(): void {
    this.mostrarModalDespacho = false;
  }

  toggleDespacho(index: number): void {
    this.itemsDespacho[index].seleccionado = !this.itemsDespacho[index].seleccionado;
    if (!this.itemsDespacho[index].seleccionado) {
      this.itemsDespacho[index].cantidadDespachar = 0;
    }
  }

  actualizarCantidadDespacho(index: number, event): void {
    const cantidad = +event.target.value;
    const pendiente = this.itemsDespacho[index].item.cantidad - this.itemsDespacho[index].despachado;

    if (cantidad < 0) {
      event.target.value = 0;
      this.itemsDespacho[index].cantidadDespachar = 0;
      return;
    }

    if (cantidad > pendiente) {
      swal.fire('Cantidad Excedida', `La cantidad máxima pendiente para este producto es ${pendiente}.`, 'warning');
      event.target.value = pendiente;
      this.itemsDespacho[index].cantidadDespachar = pendiente;
      return;
    }

    this.itemsDespacho[index].cantidadDespachar = cantidad;
  }

  hayItemsParaDespachar(): boolean {
    return this.itemsDespacho.some(i => i.seleccionado && i.cantidadDespachar > 0);
  }

  getPendiente(registro): number {
    return registro.item.cantidad - registro.despachado;
  }

  confirmarDespacho(): void {
    const itemsValidos = this.itemsDespacho.filter(i => i.seleccionado && i.cantidadDespachar > 0);

    if (itemsValidos.length === 0) {
      swal.fire('Sin Productos', 'Seleccione al menos un producto y defina la cantidad a despachar.', 'warning');
      return;
    }

    const resumen = itemsValidos.map(i =>
      `<b>${i.item.producto.nombre}</b>: ${i.cantidadDespachar} unidad(es)`
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
        const usuarioSesion = this.auth.usuario;
        const despachos: DespachoNota[] = itemsValidos.map(i => {
          const despacho = new DespachoNota();
          despacho.producto = i.item.producto;
          despacho.codProducto = i.item.producto.codProducto;
          despacho.cantidad = i.cantidadDespachar;
          despacho.totalDespacho = i.item.producto.precioVenta * i.cantidadDespachar;
          despacho.usuario = usuarioSesion;
          return despacho;
        });

        this.notasService.registrarDespacho(this.notaDespacho.idNotaCredito, despachos).subscribe(
          (despachosResponse) => {
            const idNotaActual = this.notaDespacho.idNotaCredito;
            const idEvento = despachosResponse?.[0]?.idEvento;

            this.cerrarModalDespacho();

            if (idEvento) {
              this.notasService.getComprobanteDespachoPDF(idEvento).subscribe(
                pdfResponse => {
                  const fileURL = URL.createObjectURL(pdfResponse.data);
                  window.open(fileURL);
                }
              );
            }

            swal.fire('Despacho Registrado', `El despacho de la nota No. ${idNotaActual} fue registrado exitosamente.`, 'success')
              .then(() => {
                this.router.navigateByUrl('/', { skipLocationChange: true }).then(() => {
                  this.router.navigate(['/notas-credito/index']);
                });
              });
          }
        );
      }
    });
  }

  // --- Otras acciones ---

  cancel(nota: NotaCreditoListDto): void {
    this.swalWithBootstrapButtons.fire({
      title: '¿Está seguro?',
      text: `¿Seguro que desea anular la nota de crédito No. ${nota.idNotaCredito}?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: '¡Si, anular!',
      cancelButtonText: '¡No, cancelar!',
      reverseButtons: true
    }).then((result) => {
      if (result.isConfirmed) {
        this.notasService.anular(nota.idNotaCredito).subscribe(
          response => {
            nota.estado = response.estado;
            this.swalWithBootstrapButtons.fire(
              '¡Nota Anulada!',
              `La nota de crédito No. ${nota.idNotaCredito} ha sido anulada con éxito.`,
              'success'
            );
          }, error => {
            swal.fire(`Ha ocurrido un error: ${error.error.status}`, `${error.error.message}`, 'error');
          }
        );
      } else if (result.dismiss === swal.DismissReason.cancel) {
        this.swalWithBootstrapButtons.fire(
          'Proceso Cancelado',
          `La nota de crédito No. ${nota.idNotaCredito} no fue anulada.`,
          'error'
        );
      }
    });
  }

  printNote(nota: NotaCreditoListDto): void {
    this.notasService.getNotaCreditoPDF(nota.idNotaCredito).subscribe(
      response => {
        const file = new Blob([response.data], { type: 'application/pdf' });
        const fileURL = URL.createObjectURL(file);
        window.open(fileURL);
      }
    );
  }

}
