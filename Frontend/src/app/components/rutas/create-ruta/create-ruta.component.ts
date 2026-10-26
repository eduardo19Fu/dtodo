import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { Estado } from 'src/app/models/estado';
import { Ruta } from 'src/app/models/ruta';
import { Sucursal } from 'src/app/models/sucursal';
import { Usuario } from 'src/app/models/usuario';
import { RutaService } from 'src/app/services/ruta.service';
import { SucursalService } from 'src/app/services/sucursal.service';
import { UsuarioService } from 'src/app/services/usuarios/usuario.service';

import swal from 'sweetalert2';

const ROLE_VENDEDOR = 'ROLE_VENDEDOR';

@Component({
  selector: 'app-create-ruta',
  templateUrl: './create-ruta.component.html',
  styleUrls: ['../../productos/create-producto/create-producto.component.css', './create-ruta.component.css']
})
export class CreateRutaComponent implements OnInit {

  title = 'Registrar nueva ruta';
  ruta: Ruta = new Ruta();
  idRuta: number = null;
  sucursales: Sucursal[] = [];
  vendedores: Usuario[] = [];
  guardando = false;

  /** Estados que admite una ruta (la tabla de estados compartida tiene otros que no aplican). */
  readonly estadosRuta: Estado[] = [
    { idEstado: 1, estado: 'ACTIVO' },
    { idEstado: 2, estado: 'INACTIVO' }
  ];

  constructor(
    private rutaService: RutaService,
    private sucursalService: SucursalService,
    private usuarioService: UsuarioService,
    private router: Router,
    private activatedRoute: ActivatedRoute
  ) { }

  ngOnInit(): void {
    this.cargarCatalogos();
    this.activatedRoute.params.subscribe(params => {
      if (params.id) {
        this.idRuta = Number(params.id);
        this.title = 'Editar ruta';
        this.cargarRuta(this.idRuta);
      }
    });
  }

  get esNueva(): boolean {
    return this.idRuta === null;
  }

  get idSucursalSeleccionada(): number {
    return this.ruta.sucursal ? this.ruta.sucursal.idSucursal : null;
  }

  set idSucursalSeleccionada(idSucursal: number) {
    this.ruta.sucursal = idSucursal ? { idSucursal } : null;
  }

  get idVendedorSeleccionado(): number {
    return this.ruta.vendedor ? this.ruta.vendedor.idUsuario : null;
  }

  set idVendedorSeleccionado(idUsuario: number) {
    this.ruta.vendedor = idUsuario ? { idUsuario } : null;
  }

  get idEstadoSeleccionado(): number {
    return this.ruta.estado ? this.ruta.estado.idEstado : null;
  }

  set idEstadoSeleccionado(idEstado: number) {
    this.ruta.estado = this.estadosRuta.find(estado => estado.idEstado === idEstado) || null;
  }

  /** Solo los usuarios habilitados con el rol de vendedor pueden atender una ruta. */
  static esVendedorHabilitado(usuario: Usuario): boolean {
    const roles: any[] = usuario.roles || [];
    return usuario.enabled !== false && roles.some(rol => (rol && rol.role ? rol.role : rol) === ROLE_VENDEDOR);
  }

  private cargarCatalogos(): void {
    this.sucursalService.getSucursales().subscribe(sucursales => {
      this.sucursales = sucursales;
      if (this.esNueva && !this.ruta.sucursal) {
        const principal = sucursales.find(sucursal => sucursal.esPrincipal) || sucursales[0];
        if (principal) {
          this.idSucursalSeleccionada = principal.idSucursal;
        }
      }
    });
    this.usuarioService.getUsuarios().subscribe(
      usuarios => this.vendedores = usuarios.filter(CreateRutaComponent.esVendedorHabilitado),
      () => this.vendedores = []
    );
  }

  private cargarRuta(id: number): void {
    this.rutaService.getRuta(id).subscribe(
      dto => {
        this.ruta = new Ruta();
        this.ruta.idRuta = dto.idRuta;
        this.ruta.nombre = dto.nombre;
        this.ruta.zona = dto.zona;
        this.ruta.diasVisita = dto.diasVisita;
        this.idSucursalSeleccionada = dto.idSucursal;
        this.idVendedorSeleccionado = dto.idVendedor;
        this.idEstadoSeleccionado = dto.idEstado;
      },
      error => this.mostrarError('No se pudo cargar la ruta', error)
    );
  }

  guardar(): void {
    if (this.guardando) {
      return;
    }
    this.guardando = true;
    const operacion = this.esNueva
      ? this.rutaService.create(this.ruta)
      : this.rutaService.update(this.idRuta, this.ruta);

    operacion.subscribe(
      guardada => {
        this.guardando = false;
        this.router.navigate(['/rutas/index']);
        swal.fire(this.esNueva ? 'Ruta Guardada' : 'Ruta Actualizada', `La ruta ${guardada.nombre} se guardó con éxito.`, 'success');
      },
      error => {
        this.guardando = false;
        this.mostrarError('No se pudo guardar la ruta', error);
      }
    );
  }

  private mostrarError(titulo: string, error: any): void {
    const mensaje = error?.error?.message || error?.error?.mensaje || 'Ha ocurrido un error inesperado.';
    swal.fire(titulo, mensaje, error?.status === 422 ? 'warning' : 'error');
  }
}
