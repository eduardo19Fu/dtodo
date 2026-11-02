import { Component, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';

import { CobranzaClienteDto, CobranzaDto } from '../../dtos/cobranza-dto';
import { CobranzaService } from '../../services/cobranza.service';

import Swal from 'sweetalert2';

/**
 * Cobranza de los clientes mayoristas (administrador y cobrador): cuánto debe cada cliente y desde cuándo. Un pedido vence cuando
 * pasan los días de crédito desde su primera entrega; el de contado entregado sin cobrar vence el día de la entrega.
 */
@Component({
  selector: 'app-cobranza',
  templateUrl: './cobranza.component.html',
  styleUrls: ['./cobranza.component.css', '../shared/management-page.css']
})
export class CobranzaComponent implements OnInit {

  title = 'Cobranza';
  cobranza: CobranzaDto = null;
  cargando = false;
  errorCarga = false;
  generando = false;
  idRuta: number | '' = '';
  soloVencidos = false;
  busqueda = '';

  constructor(private cobranzaService: CobranzaService) { }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.errorCarga = false;
    this.cobranzaService.cuentasPorCobrar().subscribe(cobranza => {
      this.cobranza = cobranza;
      this.cargando = false;
      if (this.idRuta !== '' && !this.rutas.some(ruta => ruta.idRuta === this.idRuta)) {
        this.idRuta = '';
      }
    }, () => {
      this.cargando = false;
      this.errorCarga = true;
    });
  }

  /** Rutas de los clientes con deuda, para el filtro. */
  get rutas(): Array<{ idRuta: number; nombre: string }> {
    const vistas = new Map<number, string>();
    (this.cobranza ? this.cobranza.clientes : []).filter(c => c.idRuta).forEach(c => vistas.set(c.idRuta, c.ruta));
    return Array.from(vistas, ([idRuta, nombre]) => ({ idRuta, nombre })).sort((a, b) => a.nombre.localeCompare(b.nombre));
  }

  /** Los clientes que pasan los filtros de la pantalla. */
  get clientes(): CobranzaClienteDto[] {
    const texto = this.busqueda.trim().toLowerCase();
    return (this.cobranza ? this.cobranza.clientes : []).filter(cliente =>
      (this.idRuta === '' || cliente.idRuta === this.idRuta)
      && (!this.soloVencidos || cliente.vencido > 0)
      && (!texto || `${cliente.cliente} ${cliente.negocio || ''}`.toLowerCase().includes(texto)));
  }

  /** Los totales de lo que se está viendo. */
  get totales(): { porVencer: number; vencido1a30: number; vencido31a60: number; vencidoMasDe60: number; vencido: number; total: number } {
    const suma = (campo: keyof CobranzaClienteDto) => this.clientes.reduce((acumulado, c) => acumulado + Number(c[campo] || 0), 0);
    return { porVencer: suma('porVencer'), vencido1a30: suma('vencido1a30'), vencido31a60: suma('vencido31a60'),
      vencidoMasDe60: suma('vencidoMasDe60'), vencido: suma('vencido'), total: suma('total') };
  }

  /** Alerta: hay clientes con saldo vencido (en toda la cartera, sin importar los filtros). */
  get hayVencidos(): boolean {
    return !!this.cobranza && this.cobranza.clientesConVencido > 0;
  }

  /** Marca la fila según el atraso: más de 60 días, hasta 60 o hasta 30. */
  claseAtraso(cliente: CobranzaClienteDto): string {
    if (cliente.diasMaxAtraso > 60) {
      return 'row-severity-high';
    }
    if (cliente.diasMaxAtraso > 30) {
      return 'row-severity-mid';
    }
    return cliente.diasMaxAtraso > 0 ? 'row-severity-low' : '';
  }

  descargarReporte(formato: 'PDF' | 'XLSX'): void {
    this.generando = true;
    this.cobranzaService.reporte(formato, this.idRuta === '' ? null : this.idRuta, this.soloVencidos).subscribe(respuesta => {
      this.generando = false;
      this.entregar(respuesta, formato, 'cuentas_por_cobrar');
    }, error => this.fallar('No se pudo generar el reporte', error));
  }

  estadoDeCuenta(cliente: CobranzaClienteDto): void {
    this.generando = true;
    this.cobranzaService.estadoDeCuenta(cliente.idCliente).subscribe(respuesta => {
      this.generando = false;
      this.entregar(respuesta, 'PDF', `estado_cuenta_${cliente.idCliente}`);
    }, error => this.fallar('No se pudo generar el estado de cuenta', error));
  }

  private entregar(respuesta: HttpResponse<Blob>, formato: 'PDF' | 'XLSX', nombre: string): void {
    const url = window.URL.createObjectURL(respuesta.body);
    if (formato === 'PDF') {
      window.open(url, '_blank');
      window.setTimeout(() => window.URL.revokeObjectURL(url), 60000);
      return;
    }
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = `${nombre}.xlsx`;
    document.body.appendChild(enlace);
    enlace.click();
    enlace.remove();
    window.URL.revokeObjectURL(url);
  }

  trackCliente(_: number, cliente: CobranzaClienteDto): number {
    return cliente.idCliente;
  }

  private fallar(titulo: string, error: any): void {
    this.generando = false;
    Swal.fire(titulo, error?.error?.message || error?.error?.mensaje || 'Ha ocurrido un error inesperado', 'error');
  }
}
