import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { Cliente } from 'src/app/models/cliente';

import { ClienteService } from 'src/app/services/cliente.service';

import swal from 'sweetalert2';

@Component({
  selector: 'app-create-cliente',
  templateUrl: './create-cliente.component.html',
  styles: [
  ]
})
export class CreateClienteComponent implements OnInit {

  title: string;
  cliente: Cliente;

  constructor(
    private serviceCliente: ClienteService,
    private router: Router,
    private activatedRoute: ActivatedRoute
  ) {
    this.title = 'Registrar Nuevo Cliente';
    this.cliente = new Cliente();
  }

  ngOnInit(): void {
    this.cargarCliente();
  }

  cargarCliente(): void {

    this.activatedRoute.params.subscribe(params => {
      // tslint:disable-next-line: no-string-literal
      const id = params['id'];
      if (id) {
        this.serviceCliente.getCliente(id).subscribe(
          cliente => this.cliente = cliente
        );
      }
    });
  }

  create(): void {
    console.log(this.cliente);
    this.serviceCliente.create(this.cliente).subscribe(
      response => {
        this.router.navigate(['/clientes/index']);
        swal.fire('Cliente Registrado con Exito', `Cliente: ${response.nombre}`, 'success');
      },
      error => { }
    );
  }

  update(): void{
    // tslint:disable-next-line: deprecation
    this.serviceCliente.update(this.cliente).subscribe(
      response => {
        this.router.navigate(['/clientes']);
        swal.fire('Cliente Actualizado', `Cliente: ${response.nombre}`, 'success');
      }
    );
  }

}
