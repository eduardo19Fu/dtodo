import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { RutaDto } from 'src/app/dtos/ruta-dto';
import { Cliente, CategoriaCliente } from 'src/app/models/cliente';

import { AuthService } from 'src/app/services/auth.service';
import { ClienteService } from 'src/app/services/cliente.service';
import { RutaService } from 'src/app/services/ruta.service';

import swal from 'sweetalert2';

@Component({
  selector: 'app-create-cliente',
  templateUrl: './create-cliente.component.html',
  styleUrls: ['./create-cliente.component.css']
})
export class CreateClienteComponent implements OnInit {

  title: string;
  cliente: Cliente;
  rutas: RutaDto[] = [];
  guardando = false;

  readonly categorias: { valor: CategoriaCliente; etiqueta: string }[] = [
    { valor: 'A', etiqueta: 'A · compra frecuente' },
    { valor: 'B', etiqueta: 'B · no frecuente, menos de 12 unidades' },
    { valor: 'VOLUMEN', etiqueta: 'Volumen · frecuente, pide fardos o cientos' }
  ];

  constructor(
    private serviceCliente: ClienteService,
    private rutaService: RutaService,
    private router: Router,
    private activatedRoute: ActivatedRoute,
    public auth: AuthService
  ) {
    this.title = 'Registrar Nuevo Cliente';
    this.cliente = new Cliente();
  }

  ngOnInit(): void {
    if (this.puedeAdministrarMayoristas) {
      this.rutaService.getActivas().subscribe(rutas => this.rutas = rutas, () => this.rutas = []);
    } else if (this.esVendedor) {
      // Un cliente que registra el vendedor siempre es mayorista y va en una de sus rutas.
      this.cliente.tipoCliente = 'MAYORISTA';
      this.rutaService.getMias().subscribe(rutas => this.rutas = rutas, () => this.rutas = []);
    }
    this.cargarCliente();
  }

  /** Solo el administrador convierte clientes en mayoristas o ajusta su ruta, categoría y crédito. */
  get puedeAdministrarMayoristas(): boolean {
    return this.auth.hasRole('ROLE_ADMIN');
  }

  get esVendedor(): boolean {
    return this.auth.esVendedor();
  }

  /** El administrador y el vendedor ven la sección del cliente mayorista; cada uno con lo que puede cambiar. */
  get veSeccionMayorista(): boolean {
    return this.puedeAdministrarMayoristas || this.esVendedor;
  }

  /** Al editar un cliente, el vendedor solo cambia la dirección y los datos de contacto. */
  get soloContacto(): boolean {
    return this.esVendedor && !!this.cliente.idCliente;
  }

  get esMayorista(): boolean {
    return this.cliente.tipoCliente === 'MAYORISTA';
  }

  set esMayorista(valor: boolean) {
    this.cliente.tipoCliente = valor ? 'MAYORISTA' : 'MINORISTA';
    if (!valor) {
      this.cliente.ruta = null;
      this.cliente.categoriaDescuento = null;
    }
  }

  get idRutaSeleccionada(): number {
    return this.cliente.ruta ? this.cliente.ruta.idRuta : null;
  }

  set idRutaSeleccionada(idRuta: number) {
    const ruta = this.rutasDisponibles.find(opcion => opcion.idRuta === idRuta);
    this.cliente.ruta = idRuta ? { idRuta, nombre: ruta ? ruta.nombre : undefined } : null;
  }

  /** Rutas activas, más la actual del cliente aunque hoy esté inactiva, para poder seguir editándolo. */
  get rutasDisponibles(): { idRuta: number; nombre: string }[] {
    const opciones = this.rutas.map(ruta => ({ idRuta: ruta.idRuta, nombre: ruta.nombre }));
    const actual = this.cliente.ruta;
    if (actual && !opciones.some(opcion => opcion.idRuta === actual.idRuta)) {
      opciones.push({ idRuta: actual.idRuta, nombre: `${actual.nombre || 'Ruta ' + actual.idRuta} (inactiva)` });
    }
    return opciones;
  }

  get faltaRuta(): boolean {
    return this.esMayorista && !this.cliente.ruta;
  }

  cargarCliente(): void {

    this.activatedRoute.params.subscribe(params => {
      // eslint-disable-next-line @typescript-eslint/dot-notation
      const id = params['id'];
      if (id) {
        this.title = 'Editar Cliente';
        this.serviceCliente.getCliente(id).subscribe(
          cliente => this.cliente = Object.assign(new Cliente(), cliente)
        );
      }
    });
  }

  create(): void {
    this.guardando = true;
    this.serviceCliente.create(this.cliente).subscribe(
      response => {
        this.guardando = false;
        this.router.navigate(['/clientes/index']);
        swal.fire('Cliente Registrado con Exito', `Cliente: ${response.nombre}`, 'success');
      },
      error => this.mostrarError('No se pudo registrar el cliente', error)
    );
  }

  update(): void {
    this.guardando = true;
    this.serviceCliente.update(this.cliente).subscribe(
      response => {
        this.guardando = false;
        this.router.navigate(['/clientes/index']);
        swal.fire('Cliente Actualizado', `Cliente: ${response.nombre}`, 'success');
      },
      error => this.mostrarError('No se pudo actualizar el cliente', error)
    );
  }

  private mostrarError(titulo: string, error: any): void {
    this.guardando = false;
    if (error?.status === 422) {
      swal.fire(titulo, error.error?.message || 'El cliente no cumple una regla del sistema.', 'warning');
    }
  }

}
