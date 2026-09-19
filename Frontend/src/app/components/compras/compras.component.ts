import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { CompraDto } from 'src/app/dtos/compra-dto';
import { TipoComprobanteCompra } from 'src/app/models/compra';
import { AuthService } from 'src/app/services/auth.service';
import { CompraService } from 'src/app/services/compra.service';
import { DetailCompraService } from 'src/app/services/compras/detail-compra.service';

import Swal from 'sweetalert2';

@Component({
  selector: 'app-compras',
  templateUrl: './compras.component.html',
  styleUrls: ['./compras.component.css']
})
export class ComprasComponent implements OnInit, OnDestroy {

  title = 'Compras';
  compras: CompraDto[] = [];
  compraSeleccionada: CompraDto;
  anulandoId: number = null;
  paginaActual = 0;
  totalPaginas = 0;
  totalElementos = 0;
  pageSize = 5;
  pageSizeOptions: number[] = [5, 10, 15, 25, 50];
  isFirst = true;
  isLast = false;
  filtro = '';
  cargando = false;

  private busquedaSubject = new Subject<string>();
  private busquedaSubscription: Subscription;

  constructor(
    private compraService: CompraService,
    public detailCompraService: DetailCompraService,
    public auth: AuthService
  ) { }

  ngOnInit(): void {
    this.cargarCompras(0);
    this.busquedaSubscription = this.busquedaSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(filtro => {
      this.filtro = filtro;
      this.cargarCompras(0);
    });
  }

  ngOnDestroy(): void {
    if (this.busquedaSubscription) {
      this.busquedaSubscription.unsubscribe();
    }
  }

  cargarCompras(page: number): void {
    this.cargando = true;
    const idSucursal = this.auth.hasRole('ROLE_ADMIN') ? null : this.auth.usuario?.sucursal?.idSucursal;
    this.compraService.getListado(page, this.pageSize, this.filtro, idSucursal).subscribe(
      response => {
        this.compras = response.content;
        this.paginaActual = response.number;
        this.totalPaginas = response.totalPages;
        this.totalElementos = response.totalElements;
        this.pageSize = response.size;
        this.isFirst = response.first;
        this.isLast = response.last;
        this.cargando = false;
      },
      error => {
        this.cargando = false;
        Swal.fire('Error al cargar compras',
          error.error?.message || error.error?.mensaje || 'Ha ocurrido un error inesperado', 'error');
      }
    );
  }

  onBuscar(valor: string): void { this.busquedaSubject.next(valor); }

  cambiarPageSize(size: number): void { this.pageSize = size; this.cargarCompras(0); }
  irPrimeraPagina(): void { this.cargarCompras(0); }
  irUltimaPagina(): void { this.cargarCompras(this.totalPaginas - 1); }
  irPaginaAnterior(): void { if (!this.isFirst) { this.cargarCompras(this.paginaActual - 1); } }
  irPaginaSiguiente(): void { if (!this.isLast) { this.cargarCompras(this.paginaActual + 1); } }
  irAPagina(pagina: number): void { this.cargarCompras(pagina); }

  formatearTipoComprobante(tipo: TipoComprobanteCompra): string {
    const etiquetas: Record<TipoComprobanteCompra, string> = {
      FACTURA: 'Factura',
      RECIBO: 'Recibo',
      NOTA_ENVIO: 'Nota de envío',
      TICKET: 'Ticket',
      OTRO: 'Otro'
    };

    return etiquetas[tipo] || tipo;
  }

  get paginasVisibles(): number[] {
    const paginas: number[] = [];
    const inicio = Math.max(0, Math.min(this.paginaActual - 2, this.totalPaginas - 5));
    const fin = Math.min(this.totalPaginas - 1, inicio + 4);
    for (let pagina = inicio; pagina <= fin; pagina++) {
      paginas.push(pagina);
    }
    return paginas;
  }

  abrirDetalle(compra: CompraDto): void {
    Swal.fire({
      toast: true,
      position: 'top-end',
      icon: 'info',
      title: 'Cargando detalle',
      text: `Preparando la compra #${compra.idCompra}...`,
      showConfirmButton: false,
      customClass: { popup: 'app-loading-toast' },
      didOpen: () => Swal.showLoading()
    });

    this.compraSeleccionada = null;
    setTimeout(() => this.compraSeleccionada = compra);
  }

  anular(compra: CompraDto): void {
    if (this.anulandoId !== null) {
      return;
    }
    Swal.fire({
      title: '¿Anular esta compra?',
      html: `Se revertir&aacute; el stock que ingres&oacute; la compra #${compra.idCompra}. Esta acci&oacute;n no se puede deshacer.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'S&iacute;, anular',
      cancelButtonText: 'Cancelar'
    }).then(resultado => {
      if (resultado.isConfirmed) {
        this.anulandoId = compra.idCompra;
        this.compraService.anular(compra.idCompra, this.auth.usuario.idUsuario).subscribe(
          () => {
            this.anulandoId = null;
            this.cargarCompras(this.paginaActual);
            Swal.fire('Compra anulada', `La compra #${compra.idCompra} fue anulada con &eacute;xito`, 'success');
          },
          () => this.anulandoId = null
        );
      }
    });
  }
}
