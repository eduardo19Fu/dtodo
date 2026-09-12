import { Component, EventEmitter, HostListener, OnDestroy, OnInit, Output } from '@angular/core';
import { Sucursal } from '../../../models/sucursal';
import { FacturaService } from '../../../services/facturas/factura.service';
import { AuthService } from '../../../services/auth.service';
import { SucursalService } from '../../../services/sucursal.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-poliza-general',
  templateUrl: './poliza-general.component.html',
  styleUrls: ['../poliza-individual/poliza-individual.component.css']
})
export class PolizaGeneralComponent implements OnInit, OnDestroy {

  @Output() cerrar = new EventEmitter<void>();

  sucursales: Sucursal[] = [];
  idSucursal: number;
  fechaInicio: string;
  fechaFin: string;
  generando = false;
  cerrando = false;

  private cierreTimer: ReturnType<typeof setTimeout>;

  constructor(
    private facturaService: FacturaService,
    public auth: AuthService,
    private sucursalService: SucursalService
  ) {
    this.seleccionarHoy();
  }

  ngOnInit(): void {
    this.idSucursal = this.auth.usuario?.sucursal?.idSucursal;
    this.sucursalService.getSucursales().subscribe(sucursales => this.sucursales = sucursales);
  }

  ngOnDestroy(): void {
    if (this.cierreTimer) {
      clearTimeout(this.cierreTimer);
    }
  }

  onSubmit(): void {
    if (this.generando) {
      return;
    }
    if (this.fechaFin < this.fechaInicio) {
      Swal.fire('Rango de fechas inválido', 'La fecha final no puede ser anterior a la fecha inicial.', 'warning');
      return;
    }

    this.generando = true;
    this.facturaService.getGeneralPolicyPDF(this.idSucursal, this.fechaInicio, this.fechaFin).subscribe(response => {
      const url = window.URL.createObjectURL(response.data);
      window.open(url, '_blank');
      setTimeout(() => window.URL.revokeObjectURL(url), 1000);
      this.generando = false;
    }, error => {
      this.generando = false;
      Swal.fire('Error al generar el reporte', error.error?.mensaje || 'No fue posible generar la póliza general', 'error');
    });
  }

  seleccionarHoy(): void {
    const hoy = new Date();
    const anio = hoy.getFullYear();
    const mes = (hoy.getMonth() + 1).toString().padStart(2, '0');
    const dia = hoy.getDate().toString().padStart(2, '0');
    const fechaHoy = `${anio}-${mes}-${dia}`;
    this.fechaInicio = fechaHoy;
    this.fechaFin = fechaHoy;
  }

  cerrarModal(): void {
    if (this.cerrando) {
      return;
    }
    this.cerrando = true;
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      this.cerrar.emit();
      return;
    }
    this.cierreTimer = setTimeout(() => this.cerrar.emit(), 180);
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.cerrarModal();
  }
}
