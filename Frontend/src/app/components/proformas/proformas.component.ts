import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { ProformaDto } from '../../dtos/proforma-dto';
import { ProformaService } from '../../services/proformas/proforma.service';
import { AuthService } from '../../services/auth.service';

import Swal from 'sweetalert2';

@Component({
  selector: 'app-proformas',
  templateUrl: './proformas.component.html',
  styleUrls: ['./proformas.component.css']
})
export class ProformasComponent implements OnInit, OnDestroy {

  title = 'Listado de Proformas';
  fechaIni: string;
  fechaFin: string;
  proformaSeleccionada: ProformaDto;
  proformasDto: ProformaDto[] = [];

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

  constructor(
    private proformaService: ProformaService,
    public auth: AuthService
  ) {}

  ngOnInit(): void {
    this.busquedaRealizada = true;
    this.cargarProformas(0);
    this.busquedaSubscription = this.busquedaSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(filtro => {
      this.filtro = filtro;
      if (this.busquedaRealizada) {
        this.cargarProformas(0);
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
    this.cargarProformas(0);
  }

  onBuscar(valor: string): void {
    this.busquedaSubject.next(valor);
  }

  cargarProformas(page: number): void {
    this.cargando = true;
    const request = this.mostrandoUltimas
      ? this.proformaService.getUltimasProformasDto(page, this.filtro, this.pageSize)
      : this.filtro.trim()
        ? this.proformaService.buscarProformasDto(page, this.fechaIni, this.fechaFin, this.filtro, this.pageSize)
        : this.proformaService.getProformasDtoPaginadas(page, this.fechaIni, this.fechaFin, this.pageSize);
    request.subscribe(
      response => {
        this.proformasDto = response.content;
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
        Swal.fire('Error al cargar proformas',
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
    this.cargarProformas(0);
  }

  printProforma(proforma: ProformaDto): void {
    this.proformaService.getProformaPdf(proforma.idProforma).subscribe(response => {
      const url = window.URL.createObjectURL(response.data);
      window.open(url, '_blank');
      window.URL.revokeObjectURL(url);
    }, error => Swal.fire('Error al generar proforma',
      error.error?.message || 'Ha ocurrido un error inesperado', 'error'));
  }

  abrirDetalle(proforma: ProformaDto): void {
    Swal.fire({
      toast: true,
      position: 'top-end',
      icon: 'warning',
      title: `Cargando detalle de la proforma No. ${proforma.noProforma}...`,
      showConfirmButton: false,
      didOpen: () => Swal.showLoading()
    });

    this.proformaSeleccionada = null;
    setTimeout(() => this.proformaSeleccionada = proforma);
  }

  cerrarDetalle(): void {
    this.proformaSeleccionada = null;
  }

  irPrimeraPagina(): void { this.cargarProformas(0); }
  irUltimaPagina(): void { this.cargarProformas(this.totalPaginas - 1); }
  irPaginaAnterior(): void { if (!this.isFirst) { this.cargarProformas(this.paginaActual - 1); } }
  irPaginaSiguiente(): void { if (!this.isLast) { this.cargarProformas(this.paginaActual + 1); } }
  irAPagina(pagina: number): void { this.cargarProformas(pagina); }
  cambiarPageSize(size: number): void { this.pageSize = size; this.cargarProformas(0); }

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
