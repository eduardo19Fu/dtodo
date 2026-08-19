import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { ProformaDto } from '../../dtos/proforma-dto';
import { UsuarioDto } from '../../dtos/usuario-dto';
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
  orden = 'fecha';
  direccion: 'asc' | 'desc' = 'desc';
  exportando = false;
  preparandoExportacion = false;

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
      ? this.proformaService.getUltimasProformasDto(
          page, this.filtro, this.pageSize, this.orden, this.direccion)
      : this.filtro.trim()
        ? this.proformaService.buscarProformasDto(
            page, this.fechaIni, this.fechaFin, this.filtro, this.pageSize, this.orden, this.direccion)
        : this.proformaService.getProformasDtoPaginadas(
            page, this.fechaIni, this.fechaFin, this.pageSize, this.orden, this.direccion);
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

  ordenarPor(campo: string): void {
    if (this.orden === campo) {
      this.direccion = this.direccion === 'asc' ? 'desc' : 'asc';
    } else {
      this.orden = campo;
      this.direccion = 'asc';
    }
    this.cargarProformas(0);
  }

  iconoOrden(campo: string): string {
    if (this.orden !== campo) {
      return 'fas fa-sort';
    }
    return this.direccion === 'asc' ? 'fas fa-sort-up' : 'fas fa-sort-down';
  }

  abrirModalExportacion(): void {
    if (this.exportando || this.preparandoExportacion) {
      return;
    }
    this.preparandoExportacion = true;
    Swal.fire({
      title: 'Consultando usuarios...',
      allowEscapeKey: false,
      allowOutsideClick: false,
      showConfirmButton: false,
      didOpen: () => Swal.showLoading()
    });
    this.proformaService.getUsuariosExportacion().subscribe(
      usuarios => {
        this.preparandoExportacion = false;
        if (!usuarios || usuarios.length === 0) {
          Swal.fire('Sin usuarios', 'No existen usuarios con proformas registradas.', 'info');
          return;
        }
        this.mostrarModalExportacion(usuarios);
      },
      error => {
        console.error(error);
        this.preparandoExportacion = false;
        Swal.fire('Error al consultar usuarios',
          error.error?.message || error.error?.mensaje || 'No fue posible cargar los usuarios.', 'error');
      }
    );
  }

  private mostrarModalExportacion(usuarios: UsuarioDto[]): void {
    const opcionesUsuarios = usuarios.map(usuario => {
      const nombre = [usuario.primerNombre, usuario.segundoNombre, usuario.apellido]
        .filter(valor => !!valor).join(' ');
      const etiqueta = nombre ? `${usuario.usuario} - ${nombre}` : usuario.usuario;
      return `<option value="${usuario.idUsuario}">${this.escaparHtml(etiqueta)}</option>`;
    }).join('');
    Swal.fire({
      title: 'Exportar proformas a Excel',
      html: `
        <div class="text-left">
          <p class="text-muted">Selecciona el usuario que generó las proformas y el rango que deseas incluir.</p>
          <div class="form-group">
            <label for="export-usuario">Usuario</label>
            <select id="export-usuario" class="form-control">
              <option value="">Selecciona un usuario</option>
              ${opcionesUsuarios}
            </select>
          </div>
          <div class="form-group">
            <label for="export-fecha-inicio">Fecha de inicio</label>
            <input id="export-fecha-inicio" type="date" class="form-control">
          </div>
          <div class="form-group">
            <label for="export-fecha-fin">Fecha de fin</label>
            <input id="export-fecha-fin" type="date" class="form-control">
          </div>
          <div class="alert alert-warning mb-0">
            <i class="fas fa-exclamation-triangle mr-1"></i>
            Exportar todas las proformas puede demorar según la cantidad de registros.
          </div>
        </div>`,
      icon: 'info',
      showCancelButton: true,
      showDenyButton: true,
      confirmButtonText: '<i class="fas fa-file-excel mr-1"></i> Exportar rango',
      denyButtonText: 'Exportar todas',
      cancelButtonText: 'Cancelar',
      focusConfirm: false,
      preConfirm: () => {
        const usuario = this.obtenerUsuarioExportacion();
        if (!usuario) {
          return false;
        }
        const fechaInicio = (document.getElementById('export-fecha-inicio') as HTMLInputElement).value;
        const fechaFin = (document.getElementById('export-fecha-fin') as HTMLInputElement).value;
        if (!fechaInicio || !fechaFin) {
          Swal.showValidationMessage('Debes ingresar ambas fechas.');
          return false;
        }
        if (fechaFin < fechaInicio) {
          Swal.showValidationMessage('La fecha final no puede ser anterior a la fecha inicial.');
          return false;
        }
        return { fechaInicio, fechaFin, ...usuario };
      },
      preDeny: () => {
        const usuario = this.obtenerUsuarioExportacion();
        return usuario || false;
      }
    }).then(result => {
      if (result.isConfirmed) {
        const rango = result.value as {
          fechaInicio: string; fechaFin: string; idUsuario: number; nombreUsuario: string
        };
        this.generarExcel(rango.idUsuario, rango.nombreUsuario,
          rango.fechaInicio, rango.fechaFin, false);
      } else if (result.isDenied) {
        const usuario = result.value as { idUsuario: number; nombreUsuario: string };
        this.confirmarExportacionCompleta(usuario.idUsuario, usuario.nombreUsuario);
      }
    });
  }

  private obtenerUsuarioExportacion(): { idUsuario: number; nombreUsuario: string } | null {
    const selector = document.getElementById('export-usuario') as HTMLSelectElement;
    const idUsuario = selector ? Number(selector.value) : 0;
    if (!idUsuario) {
      Swal.showValidationMessage('Debes seleccionar el usuario que generó las proformas.');
      return null;
    }
    return {
      idUsuario,
      nombreUsuario: selector.options[selector.selectedIndex].text
    };
  }

  private confirmarExportacionCompleta(idUsuario: number, nombreUsuario: string): void {
    Swal.fire({
      title: '¿Exportar todas las proformas?',
      text: `Se exportarán todas las proformas generadas por ${nombreUsuario}. El archivo puede tardar en generarse.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, exportar todas',
      cancelButtonText: 'Cancelar'
    }).then(result => {
      if (result.isConfirmed) {
        this.generarExcel(idUsuario, nombreUsuario, null, null, true);
      }
    });
  }

  private generarExcel(idUsuario: number, nombreUsuario: string,
                       fechaInicio: string, fechaFin: string, todas: boolean): void {
    this.exportando = true;
    Swal.fire({
      title: 'Generando reporte de proformas...',
      text: todas
        ? `Consultando todas las proformas generadas por ${nombreUsuario}.`
        : `Consultando las proformas de ${nombreUsuario} en el rango seleccionado.`,
      allowEscapeKey: false,
      allowOutsideClick: false,
      showConfirmButton: false,
      didOpen: () => Swal.showLoading()
    });

    this.proformaService.exportarProformasExcel(
      idUsuario, fechaInicio, fechaFin, todas).subscribe(
      response => {
        const disposition = response.headers.get('content-disposition');
        const filenameMatch = disposition && disposition.match(/filename="?([^";]+)"?/i);
        const filename = filenameMatch ? filenameMatch[1] : 'proformas.xlsx';
        const url = window.URL.createObjectURL(response.body);
        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
        this.exportando = false;
        Swal.close();
      },
      error => {
        console.error(error);
        this.exportando = false;
        Swal.fire('Error al exportar proformas',
          'No fue posible generar el archivo Excel.', 'error');
      }
    );
  }

  private escaparHtml(valor: string): string {
    return valor.replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
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
