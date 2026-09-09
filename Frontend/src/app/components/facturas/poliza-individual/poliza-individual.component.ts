import { AfterViewInit, Component, EventEmitter, HostListener, OnDestroy, OnInit, Output } from '@angular/core';
import { Usuario } from 'src/app/models/usuario';
import { Sucursal } from '../../../models/sucursal';
import { UsuarioService } from '../../../services/usuarios/usuario.service';
import { FacturaService } from '../../../services/facturas/factura.service';
import { AuthService } from '../../../services/auth.service';
import { SucursalService } from '../../../services/sucursal.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-poliza-individual',
  templateUrl: './poliza-individual.component.html',
  styleUrls: ['./poliza-individual.component.css']
})
export class PolizaIndividualComponent implements OnInit, AfterViewInit, OnDestroy {

  @Output() cerrar = new EventEmitter<void>();

  title: string;

  fecha: Date;
  cerrando = false;

  private cierreTimer: ReturnType<typeof setTimeout>;

  idCajero: number = null;
  cajeros: Usuario[];
  generando: boolean = false;

  // Elegir la sucursal cuyos cajeros se listan (ROLE_ADMIN y ROLE_COBRADOR)
  sucursales: Sucursal[] = [];
  idSucursal: number;

  constructor(
    private usuarioService: UsuarioService,
    private facturaService: FacturaService,
    public auth: AuthService,
    private sucursalService: SucursalService
  ) {
    this.title = 'Póliza Individual';
  }

  ngOnInit(): void {
    this.idSucursal = this.auth.usuario?.sucursal?.idSucursal;
    this.sucursalService.getSucursales().subscribe(sucursales => this.sucursales = sucursales);
  }

  ngAfterViewInit(): void {
    this.getCajeros();
  }

  ngOnDestroy(): void {
    if (this.cierreTimer) {
      clearTimeout(this.cierreTimer);
    }
  }

  onSucursalChange(idSucursal: number): void {
    this.idSucursal = idSucursal;
    this.idCajero = null;
    this.getCajeros();
  }

  onSubmit(): void {
    if (this.generando) {
      return;
    }

    this.generando = true;
    this.facturaService.getSellsDaillyReportPDF(this.idCajero, this.fecha).subscribe(response => {
      const url = window.URL.createObjectURL(response.data);
      const a = document.createElement('a');
      document.body.appendChild(a);
      a.setAttribute('style', 'display: none');
      a.setAttribute('target', 'blank');
      a.href = url;
      /*
        opcion para pedir descarga de la respuesta obtenida
        a.download = response.filename;
      */
      window.open(a.toString(), '_blank');
      window.URL.revokeObjectURL(url);
      a.remove();
      this.generando = false;
    },
      error => {
        console.log(error);
        this.generando = false;
        Swal.fire('Error al generar el reporte', error.error?.mensaje || 'No fue posible generar la póliza', 'error');
      });
  }

  getCajeros(): void {
    this.usuarioService.getCajeros(this.idSucursal).subscribe(cajeros => this.cajeros = cajeros);
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
