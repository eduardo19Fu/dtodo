import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { AuthService } from '../../services/auth.service';
import { FacturaService } from '../../services/facturas/factura.service';
import { Usuario } from '../../models/usuario';
import { FacturaListadoDto } from '../../dtos/facturaListadoDto';

import Swal from 'sweetalert2';

@Component({
  selector: 'app-facturas',
  templateUrl: './facturas.component.html',
  styleUrls: ['./facturas.component.css']
})
export class FacturasComponent implements OnInit, OnDestroy {

  title = 'Facturas';
  fechaIni: string;
  fechaFin: string;
  facturasDto: FacturaListadoDto[] = [];
  facturaSeleccionada: FacturaListadoDto;
  usuario: Usuario;

  paginaActual = 0;
  totalPaginas = 0;
  totalElementos = 0;
  pageSize = 5;
  pageSizeOptions: number[] = [5, 10, 15, 25, 50];
  isFirst = true;
  isLast = false;
  filtro = '';
  cargando = false;
  busquedaRealizada = false;
  mostrandoUltimas = true;

  private busquedaSubject = new Subject<string>();
  private busquedaSubscription: Subscription;

  swalWithBootstrapButtons = Swal.mixin({
    customClass: {
      confirmButton: 'btn btn-success',
      cancelButton: 'btn btn-danger'
    },
    buttonsStyling: true
  });

  constructor(
    private facturaService: FacturaService,
    public auth: AuthService
  ) {
    this.usuario = auth.usuario;
  }

  ngOnInit(): void {
    this.busquedaRealizada = true;
    this.cargarFacturas(0);
    this.busquedaSubscription = this.busquedaSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(filtro => {
      this.filtro = filtro;
      if (this.busquedaRealizada) {
        this.cargarFacturas(0);
      }
    });
  }

  ngOnDestroy(): void {
    if (this.busquedaSubscription) {
      this.busquedaSubscription.unsubscribe();
    }
  }

  buscarPorFechas(): void {
    if (!this.fechaIni || !this.fechaFin) {
      Swal.fire('Advertencia', 'Por favor ingrese un rango de fechas válido.', 'warning');
      return;
    }
    if (this.fechaFin < this.fechaIni) {
      Swal.fire('Advertencia', 'La fecha final no puede ser anterior a la fecha inicial.', 'warning');
      return;
    }
    this.busquedaRealizada = true;
    this.mostrandoUltimas = false;
    this.cargarFacturas(0);
  }

  onBuscar(valor: string): void {
    this.busquedaSubject.next(valor);
  }

  cargarFacturas(page: number): void {
    this.cargando = true;
    const request = this.mostrandoUltimas
      ? this.facturaService.getUltimasFacturasDto(page, this.filtro, this.pageSize)
      : this.filtro.trim()
        ? this.facturaService.buscarFacturasDto(page, this.fechaIni, this.fechaFin, this.filtro, this.pageSize)
        : this.facturaService.getFacturasDtoPaginadas(page, this.fechaIni, this.fechaFin, this.pageSize);

    request.subscribe(
      response => {
        this.facturasDto = response.content;
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
        Swal.fire('Error al cargar facturas',
          error.error?.message || error.error?.mensaje || 'Ha ocurrido un error inesperado', 'error');
      }
    );
  }

  limpiar(): void {
    this.fechaIni = null;
    this.fechaFin = null;
    this.filtro = '';
    this.mostrandoUltimas = true;
    this.busquedaRealizada = true;
    this.cargarFacturas(0);
  }

  abrirDetalle(facturaDto: FacturaListadoDto): void {
    Swal.fire({
      toast: true,
      position: 'top-end',
      icon: 'warning',
      title: `Cargando detalle de la factura No. ${facturaDto.noFactura}...`,
      showConfirmButton: false,
      didOpen: () => Swal.showLoading()
    });

    this.facturaSeleccionada = null;
    setTimeout(() => this.facturaSeleccionada = facturaDto);
  }

  cancel(facturaDto: FacturaListadoDto): void {
    this.swalWithBootstrapButtons.fire({
      title: '¿Está seguro?',
      text: `¿Seguro que desea anular la factura No. ${facturaDto.noFactura}?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: '¡Sí, anular!',
      cancelButtonText: '¡No, cancelar!',
      reverseButtons: true
    }).then(result => {
      if (!result.isConfirmed) {
        return;
      }
      this.facturaService.getFactura(facturaDto.idFactura).subscribe(factura => {
        this.facturaService.cancelV2(this.usuario.idUsuario, factura).subscribe(
          response => {
            facturaDto.idEstado = response.estado.idEstado;
            facturaDto.estado = response.estado.estado;
            this.swalWithBootstrapButtons.fire('¡Factura anulada!',
              `La factura No. ${facturaDto.noFactura} ha sido anulada con éxito`, 'success');
          },
          error => Swal.fire('Error al anular factura',
            error.error?.message || error.error?.mensaje || 'Ha ocurrido un error inesperado', 'error')
        );
      });
    });
  }

  printBill(factura: FacturaListadoDto): void {
    const url = 'https://report.feel.com.gt/ingfacereport/ingfacereport_documento?uuid=' + factura.certificacionSat;
    window.open(url, '_blank').focus();
  }

  irPrimeraPagina(): void { this.cargarFacturas(0); }
  irUltimaPagina(): void { this.cargarFacturas(this.totalPaginas - 1); }
  irPaginaAnterior(): void { if (!this.isFirst) { this.cargarFacturas(this.paginaActual - 1); } }
  irPaginaSiguiente(): void { if (!this.isLast) { this.cargarFacturas(this.paginaActual + 1); } }
  irAPagina(pagina: number): void { this.cargarFacturas(pagina); }
  cambiarPageSize(size: number): void { this.pageSize = size; this.cargarFacturas(0); }

  get paginasVisibles(): number[] {
    const paginas: number[] = [];
    const inicio = Math.max(0, Math.min(this.paginaActual - 2, this.totalPaginas - 5));
    const fin = Math.min(this.totalPaginas - 1, inicio + 4);
    for (let pagina = inicio; pagina <= fin; pagina++) {
      paginas.push(pagina);
    }
    return paginas;
  }
}
