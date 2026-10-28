import { Component, OnInit } from '@angular/core';

import { IndicadoresMesDto, VentaVendedorDto } from '../../dtos/indicadores-dto';
import { IndicadoresService } from '../../services/indicadores.service';

/**
 * Tablero de indicadores del administrador: ventas del mes por vendedor (lo entregado menos lo devuelto), pedidos entregados a
 * tiempo contra la fecha estimada y cuentas por cobrar de hoy.
 */
@Component({
  selector: 'app-indicadores',
  templateUrl: './indicadores.component.html',
  styleUrls: ['./indicadores.component.css']
})
export class IndicadoresComponent implements OnInit {

  title = 'Indicadores';
  private readonly nombresMes = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre',
    'noviembre', 'diciembre'];
  private readonly anioMinimo = 2020;

  anio = new Date().getFullYear();
  mes = new Date().getMonth() + 1;
  datos: IndicadoresMesDto = null;
  cargando = false;
  errorCarga = false;

  constructor(private indicadoresService: IndicadoresService) { }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.errorCarga = false;
    this.indicadoresService.mes(this.anio, this.mes).subscribe(datos => {
      this.datos = datos;
      this.cargando = false;
    }, () => {
      this.cargando = false;
      this.errorCarga = true;
    });
  }

  get etiquetaMes(): string {
    return `${this.nombresMes[this.mes - 1]} ${this.anio}`;
  }

  get esMesActual(): boolean {
    const hoy = new Date();
    return this.anio === hoy.getFullYear() && this.mes === hoy.getMonth() + 1;
  }

  get puedeRetroceder(): boolean {
    return this.anio > this.anioMinimo || this.mes > 1;
  }

  /** Mueve el mes; nunca pasa del mes actual ni de enero de 2020. */
  cambiarMes(delta: number): void {
    if (delta < 0 && !this.puedeRetroceder || delta > 0 && this.esMesActual) {
      return;
    }
    const fecha = new Date(this.anio, this.mes - 1 + delta, 1);
    this.anio = fecha.getFullYear();
    this.mes = fecha.getMonth() + 1;
    this.cargar();
  }

  /** Variación de las ventas netas contra el mes anterior, en por ciento; nula si el mes anterior no vendió. */
  get variacion(): number | null {
    if (!this.datos || !(Number(this.datos.netoMesAnterior) > 0)) {
      return null;
    }
    const cambio = (Number(this.datos.totalNeto) - Number(this.datos.netoMesAnterior)) / Number(this.datos.netoMesAnterior) * 100;
    return Math.round(cambio * 10) / 10;
  }

  /** Tonos del aro de «a tiempo»: verde desde 90 %, ámbar desde 70 %, rojo por debajo. */
  claseATiempo(porcentaje: number | null | undefined): string {
    if (porcentaje === null || porcentaje === undefined) {
      return 'sin-datos';
    }
    return porcentaje >= 90 ? 'bueno' : porcentaje >= 70 ? 'regular' : 'malo';
  }

  /** Fondo del aro: la parte cumplida en el color del tono y el resto en gris. */
  aro(porcentaje: number | null | undefined): string {
    const grados = Math.max(0, Math.min(100, Number(porcentaje) || 0)) * 3.6;
    return `conic-gradient(var(--aro-color) ${grados}deg, #e6ecf4 0deg)`;
  }

  get hayVentas(): boolean {
    return !!this.datos && this.datos.vendedores.some(v => Number(v.entregado) > 0 || Number(v.devuelto) > 0);
  }

  trackVendedor(_: number, vendedor: VentaVendedorDto): number {
    return vendedor.idVendedor;
  }
}
