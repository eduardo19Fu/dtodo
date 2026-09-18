import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class DetailProveedorService {

  modal = false;

  constructor() { }

  abrirModal(): void {
    this.modal = true;
  }

  cerrarModal(): void {
    this.modal = false;
  }
}
