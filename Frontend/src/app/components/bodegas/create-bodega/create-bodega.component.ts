import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { Bodega } from 'src/app/models/bodega';
import { Estado } from 'src/app/models/estado';
import { Sucursal } from 'src/app/models/sucursal';
import { BodegaService } from 'src/app/services/bodega.service';
import { SucursalService } from 'src/app/services/sucursal.service';
import { OrigenInventarioSeleccion } from '../origen-inventario-bodega/origen-inventario-bodega.component';

import swal from 'sweetalert2';

@Component({
  selector: 'app-create-bodega',
  templateUrl: './create-bodega.component.html',
  styleUrls: [
    '../../productos/create-producto/create-producto.component.css',
    './create-bodega.component.css'
  ]
})
export class CreateBodegaComponent implements OnInit {

  title: string;
  bodega: Bodega;

  sucursales: Sucursal[] = [];
  bodegasOrigen: Bodega[] = [];
  origenInventario: OrigenInventarioSeleccion = { tipo: 'NINGUNO', idOrigen: null, archivo: null, completa: true };
  guardando = false;

  /** Estados que admite una bodega (la tabla de estados compartida tiene otros que no aplican). */
  readonly estadosBodega: Estado[] = [
    { idEstado: 1, estado: 'ACTIVO' },
    { idEstado: 2, estado: 'INACTIVO' }
  ];

  constructor(
    private bodegaService: BodegaService,
    private sucursalService: SucursalService,
    private router: Router,
    private activatedRoute: ActivatedRoute
  ) {
    this.title = 'Registrar nueva bodega';
    this.bodega = new Bodega();
  }

  ngOnInit(): void {
    this.sucursalService.getSucursales().subscribe(sucursales => this.sucursales = sucursales);
    this.cargarBodega();
  }

  get esNueva(): boolean {
    return !this.bodega.idBodega;
  }

  get idEstadoSeleccionado(): number {
    return this.bodega.estado ? this.bodega.estado.idEstado : null;
  }

  set idEstadoSeleccionado(idEstado: number) {
    this.bodega.estado = this.estadosBodega.find(estado => estado.idEstado === idEstado);
  }

  compararSucursal(o1: Sucursal, o2: Sucursal): boolean {
    return o1 == null || o2 == null ? o1 == o2 : o1.idSucursal === o2.idSucursal;
  }

  alCambiarOrigen(seleccion: OrigenInventarioSeleccion): void {
    this.origenInventario = seleccion;
  }

  private cargarBodega(): void {
    this.activatedRoute.params.subscribe(params => {
      const id = params.id;
      if (id) {
        this.title = 'Editar bodega';
        this.bodegaService.getBodega(id).subscribe(bodega => this.bodega = bodega);
      } else {
        this.bodegaService.getBodegas(true).subscribe(bodegas => this.bodegasOrigen = bodegas);
      }
    });
  }

  create(): void {
    if (!this.origenInventario.completa) {
      swal.fire('Inventario inicial incompleto',
        'Completa el origen del inventario inicial o elige "Sin inventario inicial".', 'warning');
      return;
    }

    this.guardando = true;
    this.bodegaService.create(this.bodega).subscribe(
      nueva => this.cargarInventarioInicial(nueva),
      () => this.guardando = false
    );
  }

  /** La bodega ya quedó registrada: un problema al cargar su inventario inicial no debe perderla. */
  private cargarInventarioInicial(nueva: Bodega): void {
    const origen = this.origenInventario;
    if (origen.tipo === 'SUCURSAL' || origen.tipo === 'BODEGA') {
      this.bodegaService.clonarInventario(nueva.idBodega, origen.tipo, origen.idOrigen).subscribe(
        resultado => this.finalizarCreacion(nueva, `Se copiaron ${resultado.productosCopiados} productos al inventario.`),
        () => this.finalizarCreacion(nueva, 'El inventario inicial no se pudo copiar; puedes hacerlo desde el inventario de la bodega.')
      );
    } else if (origen.tipo === 'EXCEL') {
      this.bodegaService.importarExcel(nueva.idBodega, origen.archivo).subscribe(
        resultado => {
          if (resultado.errores.length === 0) {
            this.finalizarCreacion(nueva,
              `Se importaron ${resultado.productosImportados} productos (${resultado.unidadesImportadas} unidades).`);
          } else {
            this.finalizarCreacion(nueva,
              `El archivo tenía ${resultado.errores.length} fila(s) con errores y no se importó. ` +
              'Corrígelo e impórtalo desde el inventario de la bodega.');
          }
        },
        () => this.finalizarCreacion(nueva, 'El archivo no se pudo importar; puedes intentarlo desde el inventario de la bodega.')
      );
    } else {
      this.finalizarCreacion(nueva);
    }
  }

  private finalizarCreacion(nueva: Bodega, detalle?: string): void {
    this.guardando = false;
    this.router.navigate(['/bodegas/index']);
    swal.fire('Bodega Guardada', `La bodega ${nueva.nombre} fue registrada con &eacute;xito.${detalle ? '<br>' + detalle : ''}`, 'success');
  }

  update(): void {
    this.guardando = true;
    this.bodegaService.update(this.bodega).subscribe(
      actualizada => {
        this.guardando = false;
        this.router.navigate(['/bodegas/index']);
        swal.fire('Bodega Actualizada', `${actualizada.nombre} fu&eacute; actualizada con &eacute;xito!`, 'success');
      },
      () => this.guardando = false
    );
  }

}
