import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { UsuarioAuxiliar } from 'src/app/models/auxiliar/usuario-auxiliar';
import { Cliente } from 'src/app/models/cliente';
import { DetalleFactura } from 'src/app/models/detalle-factura';
import { Factura } from 'src/app/models/factura';
import { NotaCredito } from 'src/app/models/nota-credito';
import { NotaCreditoDetalle } from 'src/app/models/nota-credito-detalle';
import { AuthService } from 'src/app/services/auth.service';
import { ClienteService } from 'src/app/services/cliente.service';
import { ClienteCreateService } from 'src/app/services/facturas/cliente-create.service';
import { FacturaService } from 'src/app/services/facturas/factura.service';
import { NotasCreditoService } from 'src/app/services/notas-credito.service';

import swal from 'sweetalert2';

declare var $: any;

@Component({
  selector: 'app-create-nota',
  templateUrl: './create-nota.component.html',
  styleUrls: ['./create-nota.component.css']
})
export class CreateNotaComponent implements OnInit {

  @ViewChild('mybuscar') myBuscarTexto: ElementRef;
  @ViewChild('myCorrelativoFactura') myCorrelativo: ElementRef;

  nitIngresado: string;
  title: string;

  notaCredito: NotaCredito;
  cliente: Cliente;
  usuario: UsuarioAuxiliar;

  // Modal de selección de productos de factura
  facturaCargada: Factura;
  itemsFactura: { item: DetalleFactura, cantidad: number, seleccionado: boolean }[] = [];
  mostrarModalProductos: boolean = false;

  // Paginación del modal
  paginaActual: number = 0;
  pageSize: number = 5;
  pageSizeOptions: number[] = [5, 10, 15, 25];
  filtroModal: string = '';

  constructor(
    private notaService: NotasCreditoService,
    private facturaService: FacturaService,
    private clienteService: ClienteService,
    private clienteCreateService: ClienteCreateService,
    private authService: AuthService,
    private router: Router
  ) {
    this.title = 'Creación de Notas de Crédito';
    this.cliente = new Cliente();
    this.usuario = new UsuarioAuxiliar();
    this.notaCredito = new NotaCredito();
  }

  ngOnInit(): void {
    this.cargarVendedor();
  }

  cargarVendedor(): void {
    this.usuario.idUsuario = this.authService.usuario.idUsuario;
    this.usuario.primerNombre = this.authService.usuario.primerNombre;
    this.usuario.apellido = this.authService.usuario.apellido;
    this.usuario.usuario = this.authService.usuario.usuario;
  }

  cargarCliente(event): void {
    this.cliente = event;
  }

  loadCliente(event): void {
    this.myBuscarTexto.nativeElement.value = event.nit;
    (document.getElementById('button-2x')).click();
    this.buscarCliente();
  }

  buscarCliente(): void {
    const nit = this.myBuscarTexto.nativeElement.value;
    if (nit) {
      this.clienteService.getClienteByNit(nit).subscribe(
        cliente => {
          this.cliente = cliente;
          this.myCorrelativo.nativeElement.focus();
        },
        error => {
          if (error.status === 400) {
            swal.fire(`Error: ${error.status}`, 'Petición Equivocada', 'error');
          }
          if (error.status === 404) {
            this.nitIngresado = nit;
            this.clienteCreateService.abrirModal();
          }
        }
      );
    } else {
      swal.fire('NIT Vacío', 'Ingrese un valor valido para realizar la búsqueda.', 'warning');
    }
  }

  // --- Flujo de selección de productos desde factura ---

  abrirSeleccionProductos(): void {
    const correlativo = this.notaCredito.correlativoFacturaSat;

    if (!this.cliente || !this.cliente.idCliente) {
      swal.fire('Cliente Requerido', 'Por favor, seleccione un cliente antes de continuar.', 'warning');
      return;
    }

    if (!correlativo) {
      swal.fire('Correlativo Requerido', 'Por favor, ingrese el correlativo de la factura asociada.', 'warning');
      return;
    }

    const serie = this.notaCredito.serieFacturaSat;
    if (!serie) {
      swal.fire('Serie Requerida', 'Por favor, ingrese la serie de la factura asociada.', 'warning');
      return;
    }

    this.facturaService.getFacturaByCorrelativoSat(correlativo, serie).subscribe(
      factura => {
        // Validar que el cliente de la factura coincida con el cliente seleccionado
        if (factura.cliente.idCliente !== this.cliente.idCliente) {
          swal.fire('Cliente No Coincide',
            `La factura con correlativo ${correlativo} y serie ${serie} pertenece al cliente "${factura.cliente.nombre}", pero el cliente seleccionado es "${this.cliente.nombre}".`,
            'error');
          return;
        }

        this.facturaCargada = factura;
        this.itemsFactura = factura.itemsFactura.map((item: DetalleFactura) => ({
          item: item,
          cantidad: item.cantidad,
          seleccionado: false
        }));
        this.paginaActual = 0;
        this.filtroModal = '';
        this.mostrarModalProductos = true;
      },
      error => {
        // El servicio ya muestra el Swal de error
      }
    );
  }

  cerrarModalProductos(): void {
    this.mostrarModalProductos = false;
  }

  toggleProducto(index: number): void {
    this.itemsFactura[index].seleccionado = !this.itemsFactura[index].seleccionado;
    if (!this.itemsFactura[index].seleccionado) {
      this.itemsFactura[index].cantidad = 0;
    } else {
      this.itemsFactura[index].cantidad = this.itemsFactura[index].item.cantidad;
    }
  }

  actualizarCantidadSeleccion(index: number, event): void {
    const cantidad = +event.target.value;
    const maxCantidad = this.itemsFactura[index].item.cantidad;

    if (cantidad < 0) {
      event.target.value = 0;
      this.itemsFactura[index].cantidad = 0;
      return;
    }

    if (cantidad > maxCantidad) {
      swal.fire('Cantidad Excedida', `La cantidad máxima para este producto es ${maxCantidad}.`, 'warning');
      event.target.value = maxCantidad;
      this.itemsFactura[index].cantidad = maxCantidad;
      return;
    }

    this.itemsFactura[index].cantidad = cantidad;
  }

  hayProductosSeleccionados(): boolean {
    return this.itemsFactura.some(i => i.seleccionado && i.cantidad > 0);
  }

  // --- Paginación y filtro del modal ---

  get itemsFiltrados(): { item: DetalleFactura, cantidad: number, seleccionado: boolean }[] {
    if (!this.filtroModal) {
      return this.itemsFactura;
    }
    const filtro = this.filtroModal.toLowerCase();
    return this.itemsFactura.filter(reg =>
      reg.item.producto.nombre.toLowerCase().includes(filtro) ||
      reg.item.producto.codProducto?.toLowerCase().includes(filtro)
    );
  }

  get itemsPaginados(): { item: DetalleFactura, cantidad: number, seleccionado: boolean }[] {
    const inicio = this.paginaActual * this.pageSize;
    return this.itemsFiltrados.slice(inicio, inicio + this.pageSize);
  }

  get totalPaginasModal(): number {
    return Math.ceil(this.itemsFiltrados.length / this.pageSize);
  }

  get paginasVisiblesModal(): number[] {
    const paginas: number[] = [];
    const rango = 2;
    let inicio = Math.max(0, this.paginaActual - rango);
    let fin = Math.min(this.totalPaginasModal - 1, this.paginaActual + rango);

    if (this.paginaActual - rango < 0) {
      fin = Math.min(this.totalPaginasModal - 1, fin + (rango - this.paginaActual));
    }
    if (this.paginaActual + rango > this.totalPaginasModal - 1) {
      inicio = Math.max(0, inicio - (this.paginaActual + rango - (this.totalPaginasModal - 1)));
    }

    for (let i = inicio; i <= fin; i++) {
      paginas.push(i);
    }
    return paginas;
  }

  onFiltrarModal(valor: string): void {
    this.filtroModal = valor;
    this.paginaActual = 0;
  }

  cambiarPageSizeModal(nuevoSize: number): void {
    this.pageSize = nuevoSize;
    this.paginaActual = 0;
  }

  irPaginaModal(pagina: number): void {
    this.paginaActual = pagina;
  }

  getIndiceReal(indicePaginado: number): number {
    const item = this.itemsPaginados[indicePaginado];
    return this.itemsFactura.indexOf(item);
  }

  confirmarSeleccion(): void {
    const seleccionados = this.itemsFactura.filter(i => i.seleccionado && i.cantidad > 0);

    if (seleccionados.length === 0) {
      swal.fire('Sin Productos', 'Seleccione al menos un producto pendiente de entrega.', 'warning');
      return;
    }

    // Convertir los items de factura seleccionados a items de nota de crédito
    this.notaCredito.items = seleccionados.map(sel => {
      const item = new NotaCreditoDetalle();
      item.producto = sel.item.producto;
      item.cantidad = sel.cantidad;
      item.descuento = 0;
      item.subTotal = sel.item.producto.precioVenta * sel.cantidad;
      item.subTotalDescuento = sel.item.producto.precioVenta * sel.cantidad;
      return item;
    });

    this.cerrarModalProductos();
    this.crearNota();
  }

  crearNota(): void {
    this.notaCredito.cliente = this.cliente;
    this.notaCredito.usuario = this.usuario;
    this.notaCredito.total = this.notaCredito.calcularTotal();

    this.notaService.create(this.notaCredito).subscribe(
      response => {
        this.cliente = new Cliente();
        this.notaCredito = new NotaCredito();
        this.myBuscarTexto.nativeElement.value = '';
        this.router.navigate(['/notas-credito/index']);
        swal.fire('Nota Creada', `Nota de Crédito No. ${response.idNotaCredito} creada satisfactoriamente.`, 'success');
        this.printNote(response.idNotaCredito);
      }
    );
  }

  printNote(nota: NotaCredito): void {
    this.notaService.getNotaCreditoPDF(nota.idNotaCredito).subscribe(
      response => {
        const file = new Blob([response.data], { type: 'application/pdf' });
        const fileURL = URL.createObjectURL(file);
        window.open(fileURL);
      }
    );
  }

}
