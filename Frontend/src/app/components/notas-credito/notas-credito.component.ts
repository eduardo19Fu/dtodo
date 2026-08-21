import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { Router } from '@angular/router';
import { NotaCredito } from 'src/app/models/nota-credito';
import { NotaCreditoDetalle } from 'src/app/models/nota-credito-detalle';
import { DespachoNota } from 'src/app/models/despacho-nota';
import { DespachoNotaDto } from 'src/app/dtos/despacho-nota-dto';
import { NotaCreditoDto } from 'src/app/dtos/nota-credito-dto';
import { NotaCreditoDetalleDto } from 'src/app/dtos/nota-credito-detalle-dto';
import { AuthService } from 'src/app/services/auth.service';
import { DetailService } from 'src/app/services/facturas/detail.service';
import { NotasCreditoService } from 'src/app/services/notas-credito.service';

import swal from 'sweetalert2';

@Component({
  selector: 'app-notas-credito',
  templateUrl: './notas-credito.component.html',
  styleUrls: ['./notas-credito.component.css']
})
export class NotasCreditoComponent implements OnInit, OnDestroy {

  @ViewChild('passwordInput') passwordInput: ElementRef;

  title: string;
  notasCredito: NotaCreditoDto[] = [];
  notaSeleccionada: NotaCreditoDetalleDto;

  fechaIni: string;
  fechaFin: string;
  paginaActual = 0;
  totalPaginas = 0;
  totalElementos = 0;
  pageSize = 5;
  pageSizeOptions: number[] = [5, 10, 15, 25, 50];
  isFirst = true;
  isLast = false;
  filtro = '';
  cargando = false;
  mostrandoUltimas = true;
  private busquedaSubject = new Subject<string>();
  private busquedaSubscription: Subscription;

  // Modal despacho
  mostrarModalDespacho: boolean = false;
  notaDespacho: NotaCredito;
  itemsDespacho: { item: NotaCreditoDetalle, cantidadDespachar: number, despachado: number, seleccionado: boolean }[] = [];
  cargandoDespacho: boolean = false;

  // Modal de autorización por contraseña
  mostrarModalPassword: boolean = false;
  passwordAutorizacion: string = '';
  passwordVisible: boolean = false;
  errorPassword: string = '';
  autorizando: boolean = false;
  private despachosPendientes: DespachoNota[] = [];

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
  }

  ngOnInit(): void {
    this.cargarNotasCredito(0);
    this.busquedaSubscription = this.busquedaSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(filtro => {
      this.filtro = filtro;
      this.cargarNotasCredito(0);
    });
  }

  ngOnDestroy(): void {
    if (this.busquedaSubscription) {
      this.busquedaSubscription.unsubscribe();
    }
  }

  /**
   * Roles habilitados para operar notas de crédito. Desde el requerimiento del
   * cliente el módulo está disponible para todos los roles del sistema; la
   * anulación sigue restringida a ROLE_ADMIN en su propio *ngIf.
   */
  get puedeOperar(): boolean {
    return this.auth.hasRole('ROLE_ADMIN')
      || this.auth.hasRole('ROLE_COBRADOR')
      || this.auth.hasRole('ROLE_INVENTARIO');
  }

  cargarNotasCredito(page: number): void {
    this.cargando = true;
    const request = this.mostrandoUltimas
      ? this.notasService.getUltimasNotasCredito(page, this.filtro, this.pageSize)
      : this.notasService.getNotasCreditoPorFechas(
        page, this.fechaIni, this.fechaFin, this.filtro, this.pageSize);

    request.subscribe(response => {
      this.notasCredito = response.content;
      this.paginaActual = response.number;
      this.totalPaginas = response.totalPages;
      this.totalElementos = response.totalElements;
      this.pageSize = response.size;
      this.isFirst = response.first;
      this.isLast = response.last;
      this.cargando = false;
    }, error => {
      this.cargando = false;
      swal.fire('Error al cargar notas de crédito',
        error.error?.message || error.error?.mensaje || 'Ha ocurrido un error inesperado', 'error');
    });
  }

  buscarPorFechas(): void {
    if (!this.fechaIni || !this.fechaFin) {
      swal.fire('Advertencia', 'Por favor ingrese un rango de fechas válido.', 'warning');
      return;
    }
    if (this.fechaFin < this.fechaIni) {
      swal.fire('Advertencia', 'La fecha final no puede ser anterior a la fecha inicial.', 'warning');
      return;
    }
    this.mostrandoUltimas = false;
    this.cargarNotasCredito(0);
  }

  limpiar(): void {
    this.fechaIni = null;
    this.fechaFin = null;
    this.filtro = '';
    this.mostrandoUltimas = true;
    this.cargarNotasCredito(0);
  }

  onBuscar(valor: string): void { this.busquedaSubject.next(valor); }
  irPrimeraPagina(): void { this.cargarNotasCredito(0); }
  irUltimaPagina(): void { this.cargarNotasCredito(this.totalPaginas - 1); }
  irPaginaAnterior(): void { if (!this.isFirst) { this.cargarNotasCredito(this.paginaActual - 1); } }
  irPaginaSiguiente(): void { if (!this.isLast) { this.cargarNotasCredito(this.paginaActual + 1); } }
  irAPagina(pagina: number): void { this.cargarNotasCredito(pagina); }
  cambiarPageSize(size: number): void { this.pageSize = size; this.cargarNotasCredito(0); }

  get paginasVisibles(): number[] {
    const paginas: number[] = [];
    const inicio = Math.max(0, Math.min(this.paginaActual - 2, this.totalPaginas - 5));
    const fin = Math.min(this.totalPaginas - 1, inicio + 4);
    for (let pagina = inicio; pagina <= fin; pagina++) {
      paginas.push(pagina);
    }
    return paginas;
  }

  abrirDetalle(nota: NotaCreditoDto): void {
    swal.fire({
      toast: true,
      position: 'top-end',
      icon: 'warning',
      title: `Cargando detalle de la nota No. ${nota.idNotaCredito}...`,
      showConfirmButton: false,
      didOpen: () => swal.showLoading()
    });

    this.notasService.getNotaCreditoDetalle(nota.idNotaCredito).subscribe(
      notaDetalle => {
        this.notaSeleccionada = notaDetalle;
        swal.close();
        this.detailService.abrirModal();
      },
      () => swal.close()
    );
  }

  // --- Modal de Despacho ---

  despacharProductos(nota: NotaCreditoDto): void {
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
    this.despachosPendientes = [];
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

  get cantidadItemsDespachoSeleccionados(): number {
    return this.itemsDespacho.filter(i => i.seleccionado).length;
  }

  getPendiente(registro): number {
    return registro.item.cantidad - registro.despachado;
  }

  /**
   * Valida la selección, la confirma con el usuario y deja el despacho armado
   * a la espera de la autorización por contraseña. No se envía nada al backend
   * hasta que el modal de autorización se resuelva.
   */
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
        this.despachosPendientes = itemsValidos.map(i => {
          const despacho = new DespachoNota();
          despacho.producto = i.item.producto;
          despacho.codProducto = i.item.producto.codProducto;
          despacho.cantidad = i.cantidadDespachar;
          despacho.totalDespacho = i.item.producto.precioVenta * i.cantidadDespachar;
          return despacho;
        });

        this.abrirModalPassword();
      }
    });
  }

  // --- Modal de autorización por contraseña ---

  abrirModalPassword(): void {
    this.passwordAutorizacion = '';
    this.passwordVisible = false;
    this.errorPassword = '';
    this.autorizando = false;
    this.mostrarModalPassword = true;
    // El input existe hasta que Angular resuelve el *ngIf del modal.
    setTimeout(() => this.passwordInput?.nativeElement.focus());
  }

  /**
   * Cierra el modal descartando la contraseña. La selección de productos se
   * conserva para que el usuario pueda reintentar sin rearmar el despacho.
   */
  cerrarModalPassword(): void {
    this.mostrarModalPassword = false;
    this.passwordAutorizacion = '';
    this.passwordVisible = false;
    this.errorPassword = '';
    this.autorizando = false;
  }

  togglePasswordVisible(): void {
    this.passwordVisible = !this.passwordVisible;
  }

  /**
   * Envía el despacho junto con la contraseña en un único POST. El backend es
   * quien compara la contraseña contra el hash del usuario en sesión: si no
   * coincide responde 422 y no persiste ninguna línea.
   */
  autorizarDespacho(): void {
    if (!this.passwordAutorizacion) {
      this.errorPassword = 'Ingrese su contraseña para autorizar el despacho.';
      return;
    }

    this.autorizando = true;
    this.errorPassword = '';

    const idNotaActual = this.notaDespacho.idNotaCredito;

    this.notasService.registrarDespacho(idNotaActual, this.despachosPendientes, this.passwordAutorizacion).subscribe(
      (despachosResponse) => {
        const idEvento = despachosResponse?.[0]?.idEvento;

        this.despachosPendientes = [];
        this.cerrarModalPassword();
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
      },
      error => {
        this.autorizando = false;
        this.passwordAutorizacion = '';

        if (error.status === 422) {
          // Contraseña incorrecta: el modal permanece abierto para reintentar.
          this.errorPassword = error.error?.message
            || 'La contraseña no coincide con la del usuario en sesión. El despacho no fue registrado.';
          return;
        }

        // Cualquier otro error ya fue notificado por el servicio.
        this.cerrarModalPassword();
      }
    );
  }

  // --- Otras acciones ---

  cancel(nota: NotaCreditoDto): void {
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

  printNote(nota: NotaCreditoDto): void {
    this.notasService.getNotaCreditoPDF(nota.idNotaCredito).subscribe(
      response => {
        const file = new Blob([response.data], { type: 'application/pdf' });
        const fileURL = URL.createObjectURL(file);
        window.open(fileURL);
      }
    );
  }

}
