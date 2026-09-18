import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { MovimientoProductoDto } from 'src/app/dtos/movimiento-productoDto';

import { AuthService } from 'src/app/services/auth.service';
import { MovimientosProductoService } from '../../services/movimientos/movimientos-producto.service';

import Swal from 'sweetalert2';

interface DiaCalendario {
  fecha: Date;
  iso: string;
  numero: number;
  mesActual: boolean;
  esHoy: boolean;
  etiqueta: string;
}

@Component({
  selector: 'app-movimientos-producto',
  templateUrl: './movimientos-producto.component.html',
  styleUrls: ['./movimientos-producto.component.css']
})
export class MovimientosProductoComponent implements OnInit, OnDestroy {

  title: string;
  movimientosDto: MovimientoProductoDto[];

  // Paginación
  paginaActual: number = 0;
  totalPaginas: number = 0;
  totalElementos: number = 0;
  pageSize: number = 5;
  pageSizeOptions: number[] = [5, 10, 15, 25, 50];
  isFirst: boolean = true;
  isLast: boolean = false;

  // Búsqueda
  filtro: string = '';
  orden: string = 'fecha';
  direccion: 'asc' | 'desc' = 'desc';
  fechaIni: string;
  fechaFin: string;
  fechaIniAplicada: string;
  fechaFinAplicada: string;
  calendarioAbierto = false;
  selectorFechaActivo: 'inicio' | 'fin' = 'inicio';
  mesVisible = new Date();
  diasCalendario: DiaCalendario[] = [];
  readonly diasSemana: string[] = ['Do', 'Lu', 'Ma', 'Mi', 'Ju', 'Vi', 'Sá'];
  private readonly nombresMes: string[] = [
    'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
    'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'
  ];
  private busquedaSubject = new Subject<string>();
  private busquedaSubscription: Subscription;

  cargando: boolean = false;
  modalReporteVisible: boolean = false;

  constructor(
    private movimientosProductoService: MovimientosProductoService,
    public authService: AuthService
  ) {
    this.title = 'Movimientos de Productos Creados';
  }

  ngOnInit(): void {
    this.construirCalendario();
    this.cargarMovimientos(0);

    this.busquedaSubscription = this.busquedaSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(filtro => {
      this.filtro = filtro;
      this.cargarMovimientos(0);
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

  cargarMovimientos(page: number): void {
    this.cargando = true;
    this.movimientosProductoService.getListado(
      page, this.pageSize, this.filtro, this.orden, this.direccion,
      this.fechaIniAplicada, this.fechaFinAplicada
    ).subscribe(
      response => {
        this.movimientosDto = response.content;
        this.paginaActual = response.number;
        this.totalPaginas = response.totalPages;
        this.totalElementos = response.totalElements;
        this.pageSize = response.size;
        this.isFirst = response.first;
        this.isLast = response.last;
        this.cargando = false;
      },
      error => {
        console.error(error);
        this.cargando = false;
        Swal.fire('Error al cargar movimientos', error.error?.message || 'Ha ocurrido un error inesperado', 'error');
      }
    );
  }

  irPrimeraPagina(): void {
    this.cargarMovimientos(0);
  }

  irUltimaPagina(): void {
    this.cargarMovimientos(this.totalPaginas - 1);
  }

  irPaginaAnterior(): void {
    if (!this.isFirst) {
      this.cargarMovimientos(this.paginaActual - 1);
    }
  }

  irPaginaSiguiente(): void {
    if (!this.isLast) {
      this.cargarMovimientos(this.paginaActual + 1);
    }
  }

  irAPagina(pagina: number): void {
    this.cargarMovimientos(pagina);
  }

  cambiarPageSize(nuevoSize: number): void {
    this.pageSize = nuevoSize;
    this.cargarMovimientos(0);
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
    this.fechaIniAplicada = this.fechaIni;
    this.fechaFinAplicada = this.fechaFin;
    this.cerrarCalendario();
    this.cargarMovimientos(0);
  }

  limpiarFechas(): void {
    this.fechaIni = null;
    this.fechaFin = null;
    this.fechaIniAplicada = null;
    this.fechaFinAplicada = null;
    this.cerrarCalendario();
    this.cargarMovimientos(0);
  }

  abrirCalendario(selector: 'inicio' | 'fin'): void {
    if (this.calendarioAbierto && this.selectorFechaActivo === selector) {
      this.cerrarCalendario();
      return;
    }

    this.selectorFechaActivo = selector;
    const fechaSeleccionada = selector === 'inicio' ? this.fechaIni : this.fechaFin;
    this.mesVisible = fechaSeleccionada ? this.fechaDesdeIso(fechaSeleccionada) : new Date();
    this.calendarioAbierto = true;
    this.construirCalendario();
  }

  cerrarCalendario(): void {
    this.calendarioAbierto = false;
  }

  @HostListener('document:keydown.escape')
  cerrarCalendarioConEscape(): void {
    this.cerrarCalendario();
  }

  @HostListener('document:click', ['$event'])
  cerrarCalendarioAlHacerClickFuera(evento: MouseEvent): void {
    const elemento = evento.target as HTMLElement;
    if (this.calendarioAbierto && (!elemento || !elemento.closest('.date-picker-column'))) {
      this.cerrarCalendario();
    }
  }

  cambiarMes(desplazamiento: number): void {
    this.mesVisible = new Date(
      this.mesVisible.getFullYear(), this.mesVisible.getMonth() + desplazamiento, 1
    );
    this.construirCalendario();
  }

  mostrarMesActual(): void {
    this.mesVisible = new Date();
    this.construirCalendario();
  }

  seleccionarFecha(dia: DiaCalendario): void {
    if (this.selectorFechaActivo === 'inicio') {
      this.fechaIni = dia.iso;
      if (this.fechaFin && this.fechaFin < this.fechaIni) {
        this.fechaFin = null;
      }
      this.mesVisible = new Date(dia.fecha.getFullYear(), dia.fecha.getMonth(), 1);
      this.construirCalendario();
      this.cerrarCalendario();
      return;
    }

    this.fechaFin = dia.iso;
    this.construirCalendario();
    this.cerrarCalendario();
  }

  seleccionarRangoRapido(cantidadDias: number): void {
    const fin = new Date();
    const inicio = new Date(fin.getFullYear(), fin.getMonth(), fin.getDate() - cantidadDias + 1);
    this.fechaIni = this.fechaAIso(inicio);
    this.fechaFin = this.fechaAIso(fin);
    this.mesVisible = new Date(fin.getFullYear(), fin.getMonth(), 1);
    this.selectorFechaActivo = 'fin';
    this.construirCalendario();
  }

  seleccionarMesActual(): void {
    const hoy = new Date();
    this.fechaIni = this.fechaAIso(new Date(hoy.getFullYear(), hoy.getMonth(), 1));
    this.fechaFin = this.fechaAIso(hoy);
    this.mesVisible = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
    this.selectorFechaActivo = 'fin';
    this.construirCalendario();
  }

  limpiarSeleccionCalendario(): void {
    this.fechaIni = null;
    this.fechaFin = null;
    this.selectorFechaActivo = 'inicio';
    this.mostrarMesActual();
  }

  estaEnRango(fecha: string): boolean {
    return Boolean(this.fechaIni && this.fechaFin && fecha > this.fechaIni && fecha < this.fechaFin);
  }

  formatearFechaVisible(fecha: string): string {
    if (!fecha) {
      return 'Seleccionar fecha';
    }
    const partes = fecha.split('-');
    return `${partes[2]}/${partes[1]}/${partes[0]}`;
  }

  get tituloMesVisible(): string {
    return `${this.nombresMes[this.mesVisible.getMonth()]} de ${this.mesVisible.getFullYear()}`;
  }

  get resumenRango(): string {
    if (!this.fechaIni && !this.fechaFin) {
      return 'Sin fechas seleccionadas';
    }
    if (this.fechaIni && !this.fechaFin) {
      return `Desde ${this.formatearFechaVisible(this.fechaIni)}`;
    }
    return `${this.formatearFechaVisible(this.fechaIni)} – ${this.formatearFechaVisible(this.fechaFin)}`;
  }

  private construirCalendario(): void {
    const anio = this.mesVisible.getFullYear();
    const mes = this.mesVisible.getMonth();
    const primerDia = new Date(anio, mes, 1);
    const inicioGrilla = new Date(anio, mes, 1 - primerDia.getDay());
    const hoy = this.fechaAIso(new Date());
    const dias: DiaCalendario[] = [];

    for (let indice = 0; indice < 42; indice++) {
      const fecha = new Date(
        inicioGrilla.getFullYear(), inicioGrilla.getMonth(), inicioGrilla.getDate() + indice
      );
      const iso = this.fechaAIso(fecha);
      dias.push({
        fecha,
        iso,
        numero: fecha.getDate(),
        mesActual: fecha.getMonth() === mes,
        esHoy: iso === hoy,
        etiqueta: `${fecha.getDate()} de ${this.nombresMes[fecha.getMonth()]} de ${fecha.getFullYear()}`
      });
    }
    this.diasCalendario = dias;
  }

  private fechaAIso(fecha: Date): string {
    const mes = (`0${fecha.getMonth() + 1}`).slice(-2);
    const dia = (`0${fecha.getDate()}`).slice(-2);
    return `${fecha.getFullYear()}-${mes}-${dia}`;
  }

  private fechaDesdeIso(fecha: string): Date {
    const partes = fecha.split('-').map(valor => Number(valor));
    return new Date(partes[0], partes[1] - 1, partes[2]);
  }

  abrirModalReporte(): void {
    this.modalReporteVisible = true;
  }

  cerrarModalReporte(): void {
    this.modalReporteVisible = false;
  }

  ordenarPor(campo: string): void {
    if (this.orden === campo) {
      this.direccion = this.direccion === 'asc' ? 'desc' : 'asc';
    } else {
      this.orden = campo;
      this.direccion = 'asc';
    }
    this.cargarMovimientos(0);
  }

  iconoOrden(campo: string): string {
    if (this.orden !== campo) {
      return 'fas fa-sort';
    }
    return this.direccion === 'asc' ? 'fas fa-sort-up' : 'fas fa-sort-down';
  }

  get paginasVisibles(): number[] {
    const paginas: number[] = [];
    const rango = 2;
    let inicio = Math.max(0, this.paginaActual - rango);
    let fin = Math.min(this.totalPaginas - 1, this.paginaActual + rango);

    if (this.paginaActual - rango < 0) {
      fin = Math.min(this.totalPaginas - 1, fin + (rango - this.paginaActual));
    }
    if (this.paginaActual + rango > this.totalPaginas - 1) {
      inicio = Math.max(0, inicio - (this.paginaActual + rango - (this.totalPaginas - 1)));
    }

    for (let i = inicio; i <= fin; i++) {
      paginas.push(i);
    }
    return paginas;
  }

}
