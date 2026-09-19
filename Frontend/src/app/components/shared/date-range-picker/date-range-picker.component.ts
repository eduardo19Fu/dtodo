import { Component, ElementRef, EventEmitter, HostListener, Input, Output } from '@angular/core';

interface DiaCalendario {
  fecha: Date;
  iso: string;
  numero: number;
  mesActual: boolean;
  esHoy: boolean;
  etiqueta: string;
}

@Component({
  selector: 'app-date-range-picker',
  templateUrl: './date-range-picker.component.html',
  styleUrls: ['./date-range-picker.component.css']
})
export class DateRangePickerComponent {

  private static siguienteId = 0;

  @Input() fechaInicio: string;
  @Input() fechaFin: string;
  @Input() tema: 'productos' | 'ventas' | 'proformas' | 'notas' | 'compras' | 'usuarios' = 'productos';
  @Output() fechaInicioChange = new EventEmitter<string>();
  @Output() fechaFinChange = new EventEmitter<string>();

  readonly idComponente = `date-range-picker-${DateRangePickerComponent.siguienteId++}`;
  readonly diasSemana: string[] = ['Do', 'Lu', 'Ma', 'Mi', 'Ju', 'Vi', 'Sá'];
  calendarioAbierto = false;
  selectorFechaActivo: 'inicio' | 'fin' = 'inicio';
  mesVisible = new Date();
  diasCalendario: DiaCalendario[] = [];

  private readonly nombresMes: string[] = [
    'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
    'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'
  ];

  constructor(private elementRef: ElementRef) {
    this.construirCalendario();
  }

  abrirCalendario(selector: 'inicio' | 'fin'): void {
    if (this.calendarioAbierto && this.selectorFechaActivo === selector) {
      this.cerrarCalendario();
      return;
    }

    this.selectorFechaActivo = selector;
    const fechaSeleccionada = selector === 'inicio' ? this.fechaInicio : this.fechaFin;
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
    if (this.calendarioAbierto && !this.elementRef.nativeElement.contains(evento.target)) {
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
      this.actualizarFechaInicio(dia.iso);
      if (this.fechaFin && this.fechaFin < this.fechaInicio) {
        this.actualizarFechaFin(null);
      }
    } else {
      this.actualizarFechaFin(dia.iso);
    }

    this.mesVisible = new Date(dia.fecha.getFullYear(), dia.fecha.getMonth(), 1);
    this.construirCalendario();
    this.cerrarCalendario();
  }

  seleccionarRangoRapido(cantidadDias: number): void {
    const fin = new Date();
    const inicio = new Date(fin.getFullYear(), fin.getMonth(), fin.getDate() - cantidadDias + 1);
    this.actualizarFechaInicio(this.fechaAIso(inicio));
    this.actualizarFechaFin(this.fechaAIso(fin));
    this.mesVisible = new Date(fin.getFullYear(), fin.getMonth(), 1);
    this.construirCalendario();
  }

  seleccionarMesActual(): void {
    const hoy = new Date();
    this.actualizarFechaInicio(this.fechaAIso(new Date(hoy.getFullYear(), hoy.getMonth(), 1)));
    this.actualizarFechaFin(this.fechaAIso(hoy));
    this.mesVisible = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
    this.construirCalendario();
  }

  limpiarSeleccion(): void {
    this.actualizarFechaInicio(null);
    this.actualizarFechaFin(null);
    this.mostrarMesActual();
  }

  estaEnRango(fecha: string): boolean {
    return Boolean(this.fechaInicio && this.fechaFin && fecha > this.fechaInicio && fecha < this.fechaFin);
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
    if (!this.fechaInicio && !this.fechaFin) {
      return 'Sin fechas seleccionadas';
    }
    if (this.fechaInicio && !this.fechaFin) {
      return `Desde ${this.formatearFechaVisible(this.fechaInicio)}`;
    }
    return `${this.formatearFechaVisible(this.fechaInicio)} – ${this.formatearFechaVisible(this.fechaFin)}`;
  }

  private actualizarFechaInicio(fecha: string): void {
    this.fechaInicio = fecha;
    this.fechaInicioChange.emit(fecha);
  }

  private actualizarFechaFin(fecha: string): void {
    this.fechaFin = fecha;
    this.fechaFinChange.emit(fecha);
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
}
