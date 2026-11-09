import { Component, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';

import { AvanceComisionDto, FilaComisionDto, HistorialMetaComisionDto, MetaComisionDto } from '../../dtos/comision-dto';
import { ComisionService } from '../../services/comision.service';

import { dialogo } from '../../shared/dialogo-swal';
import Swal from 'sweetalert2';

/**
 * Comisiones de los vendedores (solo administrador): las metas de cada año, el avance de cada vendedor y la liquidación
 * al cierre del año. Se paga un porcentaje plano sobre todo lo entregado según la meta más alta alcanzada, y solo a quien
 * el administrador marque como que completó el año.
 */
@Component({
  selector: 'app-comisiones',
  templateUrl: './comisiones.component.html',
  styleUrls: ['./comisiones.component.css', '../shared/management-page.css']
})
export class ComisionesComponent implements OnInit {

  title = 'Comisiones';
  anio = new Date().getFullYear();
  avance: AvanceComisionDto = null;
  /** Copia editable de las metas del año; se compara con la guardada para saber si hay cambios. */
  metas: MetaComisionDto[] = [];
  /** Vendedores marcados como que completaron el año (solo en un año sin liquidar). */
  completaron: { [idVendedor: number]: boolean } = {};
  cargando = false;
  errorCarga = false;
  trabajando = false;
  /** Cambios de las metas del año (quién, cuándo, por qué, antes y después); se despliegan a pedido. */
  historial: HistorialMetaComisionDto[] = [];
  historialAbierto = false;
  /** Formato del reporte que se está generando; su botón muestra el indicador de carga. */
  generandoReporte: 'PDF' | 'XLSX' | null = null;

  constructor(private comisionService: ComisionService) { }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.errorCarga = false;
    this.comisionService.avance(this.anio).subscribe(avance => {
      this.mostrar(avance);
      this.cargando = false;
    }, () => {
      this.cargando = false;
      this.errorCarga = true;
    });
    this.historialAbierto = false;
    this.comisionService.historialMetas(this.anio).subscribe(historial => this.historial = historial, () => this.historial = []);
  }

  private mostrar(avance: AvanceComisionDto): void {
    this.avance = avance;
    this.metas = avance.metas.map(meta => ({ montoMinimo: meta.montoMinimo, porcentaje: meta.porcentaje }));
    this.completaron = {};
    avance.filas.forEach(fila => this.completaron[fila.idVendedor] = !!fila.completoAnio);
  }

  cambiarAnio(delta: number): void {
    this.anio += delta;
    this.cargar();
  }

  get liquidado(): boolean {
    return !!this.avance && this.avance.liquidado;
  }

  get metasModificadas(): boolean {
    const guardadas = this.avance ? this.avance.metas : [];
    return guardadas.length !== this.metas.length || this.metas.some((meta, i) =>
      Number(meta.montoMinimo) !== Number(guardadas[i].montoMinimo) || Number(meta.porcentaje) !== Number(guardadas[i].porcentaje));
  }

  get puedeLiquidar(): boolean {
    return !!this.avance && this.avance.anioTerminado && !this.avance.liquidado && this.avance.metas.length > 0
      && !this.metasModificadas && !this.trabajando;
  }

  get motivoSinLiquidar(): string {
    if (!this.avance || this.avance.liquidado) {
      return '';
    }
    if (!this.avance.anioTerminado) {
      return 'El año todavía no termina: se puede liquidar a partir del 1 de enero siguiente.';
    }
    if (this.avance.metas.length === 0) {
      return 'Define y guarda las metas del año para poder liquidar.';
    }
    return this.metasModificadas ? 'Guarda los cambios de las metas antes de liquidar.' : '';
  }

  /** Lo que se pagaría a los vendedores marcados como que completaron el año. */
  get totalAPagar(): number {
    if (!this.avance) {
      return 0;
    }
    return this.avance.filas.filter(fila => this.cobra(fila))
      .reduce((suma, fila) => suma + Number(fila.comision || 0), 0);
  }

  /** Marcado como que completó el año y con 365 días cumplidos (quien ingresó este año nunca cobra todavía). */
  private cobra(fila: FilaComisionDto): boolean {
    return !!this.completaron[fila.idVendedor] && fila.situacion !== 'PENDIENTE';
  }

  get cantidadMarcados(): number {
    return this.avance ? this.avance.filas.filter(fila => this.cobra(fila)).length : 0;
  }

  // ---- metas

  agregarMeta(): void {
    const ultima = this.metas[this.metas.length - 1];
    this.metas.push({ montoMinimo: ultima ? Number(ultima.montoMinimo) + 100000 : 100000,
      porcentaje: ultima ? Number(ultima.porcentaje) + 0.5 : 0.5 });
  }

  quitarMeta(indice: number): void {
    this.metas.splice(indice, 1);
  }

  descartarCambios(): void {
    this.metas = this.avance.metas.map(meta => ({ montoMinimo: meta.montoMinimo, porcentaje: meta.porcentaje }));
  }

  /** Con el año ya terminado y sin liquidar, cambiar las metas mueve pagos reales y hay que explicar por qué. */
  get motivoObligatorioMetas(): boolean {
    return !!this.avance && this.avance.anioTerminado && !this.avance.liquidado;
  }

  /** Las metas como texto corto: «Q300,000 → 0.5 %  ·  Q700,000 → 1 %». */
  textoMetas(metas: MetaComisionDto[]): string {
    if (!metas || metas.length === 0) {
      return 'Sin metas';
    }
    return metas.map(meta => `Q${Number(meta.montoMinimo).toLocaleString('en-US')} → ${Number(meta.porcentaje)} %`).join('  ·  ');
  }

  etiquetaCambio(accion: string): string {
    return accion === 'CREAR' ? 'Metas creadas' : accion === 'COPIAR' ? 'Metas copiadas de otro año' : 'Metas modificadas';
  }

  alternarHistorial(): void {
    this.historialAbierto = !this.historialAbierto;
  }

  /**
   * Antes de guardar muestra las metas de antes y de después y avisa que se recalcula la comisión de todos los
   * vendedores; si el año ya terminó pide además el motivo del cambio.
   */
  guardarMetas(): void {
    if (this.trabajando) {
      return;
    }
    const obligatorio = this.motivoObligatorioMetas;
    const vendedores = this.avance ? this.avance.filas.length : 0;
    const lista = (metas: MetaComisionDto[]) => metas.length === 0 ? '<li>Sin metas</li>'
      : metas.map(meta => `<li>Q${Number(meta.montoMinimo).toLocaleString('en-US')} → <strong>${Number(meta.porcentaje)} %</strong></li>`).join('');
    dialogo({
      iconoFa: 'fa-hand-holding-usd',
      tono: 'azul',
      title: `Guardar las metas de ${this.anio}`,
      html: `<div class="app-dialog-comparacion">
          <div><span>Antes</span><ul>${lista(this.avance ? this.avance.metas : [])}</ul></div>
          <div><span>Después</span><ul>${lista(this.metas)}</ul></div>
        </div>
        <p class="app-dialog-aviso">Se recalcula la comisión de <strong>${vendedores} ${vendedores === 1 ? 'vendedor' : 'vendedores'}</strong> del año ${this.anio}.</p>`,
      input: 'textarea',
      inputLabel: obligatorio ? 'Motivo del cambio (obligatorio, el año ya terminó)' : 'Motivo del cambio (opcional)',
      inputPlaceholder: 'Por ejemplo: corrección del esquema acordado',
      inputAttributes: { maxlength: '500' },
      showCancelButton: true,
      confirmButtonText: 'Guardar metas',
      cancelButtonText: 'Cancelar',
      inputValidator: valor => obligatorio && (!valor || !valor.trim()) ? 'Indica el motivo del cambio.' : null
    }).then(resultado => {
      if (!resultado.isConfirmed) {
        return;
      }
      this.trabajando = true;
      this.comisionService.guardarMetas(this.anio, this.metas, String(resultado.value || '').trim() || undefined).subscribe(() => {
        this.trabajando = false;
        this.aviso('Metas guardadas');
        this.cargar();
      }, error => this.fallar('No se pudieron guardar las metas', error));
    });
  }

  copiarMetas(): void {
    Swal.fire({
      title: `Copiar las metas al año ${this.anio}`,
      input: 'number',
      inputLabel: 'Año del que se copian',
      inputValue: this.anio - 1,
      showCancelButton: true,
      confirmButtonText: 'Copiar',
      cancelButtonText: 'Cancelar',
      inputValidator: valor => !valor || Number(valor) === this.anio ? 'Indica un año distinto al actual.' : null
    }).then(resultado => {
      if (!resultado.isConfirmed) {
        return;
      }
      this.trabajando = true;
      this.comisionService.copiarMetas(Number(resultado.value), this.anio).subscribe(() => {
        this.trabajando = false;
        this.aviso('Metas copiadas');
        this.cargar();
      }, error => this.fallar('No se pudieron copiar las metas', error));
    });
  }

  // ---- liquidación

  liquidar(): void {
    const decisiones = this.avance.filas.map(fila => ({ idVendedor: fila.idVendedor, completoAnio: this.cobra(fila) }));
    const total = this.totalAPagar.toLocaleString('es-GT', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    Swal.fire({
      title: `Liquidar las comisiones ${this.anio}`,
      text: `Se pagará a ${this.cantidadMarcados} vendedor(es) un total de Q${total}. Los demás quedan sin comisión. `
        + 'La liquidación queda guardada y ya no se recalcula; solo el administrador puede deshacerla, con un motivo.',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Liquidar',
      cancelButtonText: 'Cancelar'
    }).then(resultado => {
      if (!resultado.isConfirmed) {
        return;
      }
      this.trabajando = true;
      this.comisionService.liquidar(this.anio, decisiones).subscribe(avance => {
        this.trabajando = false;
        this.mostrar(avance);
        this.aviso('Año liquidado');
      }, error => this.fallar('No se pudo liquidar', error));
    });
  }

  deshacer(): void {
    Swal.fire({
      title: `Deshacer la liquidación ${this.anio}`,
      input: 'textarea',
      inputLabel: 'Motivo (obligatorio, queda registrado)',
      inputAttributes: { maxlength: '500' },
      showCancelButton: true,
      confirmButtonText: 'Deshacer',
      cancelButtonText: 'Cancelar',
      inputValidator: valor => !valor || !valor.trim() ? 'Indica el motivo.' : null
    }).then(resultado => {
      if (!resultado.isConfirmed) {
        return;
      }
      this.trabajando = true;
      this.comisionService.deshacer(this.anio, String(resultado.value).trim()).subscribe(avance => {
        this.trabajando = false;
        this.mostrar(avance);
        this.aviso('Liquidación deshecha');
      }, error => this.fallar('No se pudo deshacer', error));
    });
  }

  // ---- reporte

  descargar(formato: 'PDF' | 'XLSX'): void {
    this.trabajando = true;
    this.generandoReporte = formato;
    this.comisionService.reporte(this.anio, formato).subscribe(respuesta => {
      this.trabajando = false;
      this.generandoReporte = null;
      this.entregarArchivo(respuesta, formato);
    }, error => this.fallar('No se pudo generar el reporte', error));
  }

  private entregarArchivo(respuesta: HttpResponse<Blob>, formato: 'PDF' | 'XLSX'): void {
    const url = window.URL.createObjectURL(respuesta.body);
    if (formato === 'PDF') {
      window.open(url, '_blank');
      window.setTimeout(() => window.URL.revokeObjectURL(url), 60000);
      return;
    }
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = `comisiones_${this.anio}.xlsx`;
    document.body.appendChild(enlace);
    enlace.click();
    enlace.remove();
    window.URL.revokeObjectURL(url);
  }

  trackFila(_: number, fila: FilaComisionDto): number {
    return fila.idVendedor;
  }

  private aviso(titulo: string): void {
    Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: titulo, showConfirmButton: false, timer: 2500 });
  }

  private fallar(titulo: string, error: any): void {
    this.trabajando = false;
    this.generandoReporte = null;
    Swal.fire(titulo, error?.error?.message || error?.error?.mensaje || 'Ha ocurrido un error inesperado', 'error');
  }
}
