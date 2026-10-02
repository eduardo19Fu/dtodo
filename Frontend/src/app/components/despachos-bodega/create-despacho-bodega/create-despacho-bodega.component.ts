import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { Bodega } from 'src/app/models/bodega';
import { DetalleDespachoBodega } from 'src/app/models/detalle-despacho-bodega';
import { Producto } from 'src/app/models/producto';
import { Sucursal } from 'src/app/models/sucursal';
import { DespachoBodegaRequest } from 'src/app/dtos/despacho-bodega-request';
import { InventarioBodegaDto } from 'src/app/dtos/inventario-bodega-dto';
import { BodegaService } from 'src/app/services/bodega.service';
import { DespachoBodegaService } from 'src/app/services/despacho-bodega.service';
import { SucursalService } from 'src/app/services/sucursal.service';

import swal from 'sweetalert2';

@Component({
  selector: 'app-create-despacho-bodega',
  templateUrl: './create-despacho-bodega.component.html',
  styleUrls: [
    '../../productos/create-producto/create-producto.component.css',
    '../../proformas/create-proforma/create-proforma.component.css',
    './create-despacho-bodega.component.css'
  ]
})
export class CreateDespachoBodegaComponent implements OnInit {

  title = 'Registrar nuevo despacho';
  despacho = new DespachoBodegaRequest();
  lineas: DetalleDespachoBodega[] = [];

  bodegas: Bodega[] = [];
  sucursales: Sucursal[] = [];

  /** Producto elegido (con la existencia de la bodega) antes de agregarlo al detalle. */
  itemActual: InventarioBodegaDto = null;
  codigoBusqueda = '';
  cantidadActual: number = null;
  buscando = false;
  modalProductoVisible = false;

  edicionDetalleAbierta = false;
  indiceDetalleEdicion = -1;
  valorDetalleAnterior: number = null;
  valorDetalleNuevo: number = null;
  errorEdicionDetalle = '';
  private elementoOrigenEdicion: HTMLElement;

  guardando = false;

  constructor(
    private bodegaService: BodegaService,
    private despachoService: DespachoBodegaService,
    private sucursalService: SucursalService,
    private activatedRoute: ActivatedRoute,
    private router: Router
  ) {
    this.despacho.recibidoPor = '';
    this.despacho.observaciones = '';
  }

  ngOnInit(): void {
    this.sucursalService.getSucursales().subscribe(sucursales => {
      this.sucursales = sucursales.filter(sucursal => !sucursal.estado || sucursal.estado.estado === 'ACTIVO');
      this.aplicarSucursalPorDefecto();
    });
    this.bodegaService.getBodegas(true).subscribe(bodegas => {
      this.bodegas = bodegas;
      const idBodega = this.activatedRoute.snapshot.queryParamMap.get('bodega');
      if (idBodega && bodegas.some(bodega => bodega.idBodega === +idBodega)) {
        this.despacho.idBodega = +idBodega;
        this.alCambiarBodega(+idBodega);
      }
    });
  }

  get bodegaSeleccionada(): Bodega {
    return this.bodegas.find(bodega => bodega.idBodega === this.despacho.idBodega);
  }

  get sucursalDestino(): Sucursal {
    return this.sucursales.find(sucursal => sucursal.idSucursal === this.despacho.idSucursalDestino);
  }

  /** La sucursal asignada a la bodega es el destino por defecto, pero el usuario puede elegir otra. */
  alCambiarBodega(idBodega: number): void {
    this.despacho.idBodega = idBodega;
    this.itemActual = null;
    this.codigoBusqueda = '';
    this.cantidadActual = null;
    this.aplicarSucursalPorDefecto();
  }

  private aplicarSucursalPorDefecto(): void {
    const sucursalBodega = this.bodegaSeleccionada?.sucursal;
    if (sucursalBodega && this.sucursales.some(sucursal => sucursal.idSucursal === sucursalBodega.idSucursal)) {
      this.despacho.idSucursalDestino = sucursalBodega.idSucursal;
    } else if (!this.sucursales.some(sucursal => sucursal.idSucursal === this.despacho.idSucursalDestino)) {
      this.despacho.idSucursalDestino = null;
    }
  }

  /*********** SELECCIÓN DE PRODUCTO ***********/

  buscarProductoPorCodigo(): void {
    if (!this.despacho.idBodega) {
      swal.fire('Selecciona la bodega', 'Primero indica la bodega desde la que se despacha.', 'warning');
      return;
    }
    const codigo = (this.codigoBusqueda || '').trim();
    if (!codigo) {
      swal.fire('Código inválido', 'Ingresa un código de producto.', 'warning');
      return;
    }
    this.buscando = true;
    this.bodegaService.getInventarioPorCodigo(this.despacho.idBodega, codigo).subscribe(
      (item: InventarioBodegaDto) => {
        this.buscando = false;
        this.seleccionarItem(item);
      },
      error => {
        this.buscando = false;
        if (error.status === 404) {
          swal.fire('Producto no encontrado', 'Ese código no existe en el inventario de la bodega seleccionada.', 'error');
        } else {
          swal.fire('No se pudo buscar el producto', error.error?.message || 'Intenta nuevamente.', 'error');
        }
      }
    );
  }

  buscarProductoConEnter(evento: KeyboardEvent): void {
    evento.preventDefault();
    this.buscarProductoPorCodigo();
  }

  abrirModalProducto(): void {
    if (!this.despacho.idBodega) {
      swal.fire('Selecciona la bodega', 'Primero indica la bodega desde la que se despacha.', 'warning');
      return;
    }
    this.modalProductoVisible = true;
  }

  cerrarModalProducto(): void {
    this.modalProductoVisible = false;
  }

  elegirDelModal(item: InventarioBodegaDto): void {
    this.cerrarModalProducto();
    this.seleccionarItem(item);
  }

  private seleccionarItem(item: InventarioBodegaDto): void {
    this.itemActual = item;
    this.codigoBusqueda = item.codProducto;
    this.cantidadActual = null;
  }

  /** Existencia de la bodega que todavía no está comprometida en otras líneas del mismo despacho. */
  get disponibleItemActual(): number {
    if (!this.itemActual) {
      return 0;
    }
    return this.itemActual.stock - this.cantidadEnDetalle(this.itemActual.idProducto);
  }

  private cantidadEnDetalle(idProducto: number): number {
    const linea = this.lineas.find(item => item.producto.idProducto === idProducto);
    return linea ? linea.cantidad : 0;
  }

  /*********** DETALLE ***********/

  agregarLinea(): void {
    if (!this.itemActual) {
      swal.fire('Selecciona un producto', 'Busca un producto de la bodega por código o desde el listado.', 'warning');
      return;
    }
    const cantidad = Number(this.cantidadActual);
    if (!Number.isInteger(cantidad) || cantidad <= 0) {
      swal.fire('Cantidad inválida', 'La cantidad debe ser un número entero mayor a 0.', 'warning');
      return;
    }
    if (cantidad > this.disponibleItemActual) {
      swal.fire('Existencias insuficientes',
        `La bodega tiene ${this.itemActual.stock} unidades de este producto` +
        (this.disponibleItemActual < this.itemActual.stock
          ? ` y ya agregaste ${this.cantidadEnDetalle(this.itemActual.idProducto)}.` : '.'),
        'warning');
      return;
    }

    const existente = this.lineas.find(linea => linea.producto.idProducto === this.itemActual.idProducto);
    if (existente) {
      existente.cantidad += cantidad;
      existente.subTotal = existente.calcularSubTotal();
      this.lineas = [...this.lineas];
    } else {
      const detalle = new DetalleDespachoBodega();
      detalle.producto = Object.assign(new Producto(), {
        idProducto: this.itemActual.idProducto,
        codProducto: this.itemActual.codProducto,
        nombre: this.itemActual.nombreProducto,
        precioCompra: this.itemActual.precioCompra
      });
      detalle.cantidad = cantidad;
      detalle.precioUnitario = this.itemActual.precioCompra || 0;
      detalle.existenciaBodega = this.itemActual.stock;
      detalle.subTotal = detalle.calcularSubTotal();
      this.lineas.push(detalle);
    }

    this.itemActual = null;
    this.codigoBusqueda = '';
    this.cantidadActual = null;
  }

  agregarLineaConEnter(evento: KeyboardEvent): void {
    evento.preventDefault();
    this.agregarLinea();
  }

  eliminarLinea(detalle: DetalleDespachoBodega): void {
    swal.fire({
      title: '&iquest;Quitar producto?',
      text: `Se quitar&aacute; ${detalle.producto.nombre} del detalle del despacho.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'S&iacute;, quitar',
      cancelButtonText: 'Cancelar',
      reverseButtons: true,
      focusCancel: true
    }).then(resultado => {
      if (resultado.isConfirmed) {
        this.lineas = this.lineas.filter(item => item !== detalle);
      }
    });
  }

  abrirEdicionDetalle(index: number, origen: EventTarget): void {
    const item = this.lineas[index];
    if (!item) {
      return;
    }
    this.indiceDetalleEdicion = index;
    this.valorDetalleAnterior = Number(item.cantidad);
    this.valorDetalleNuevo = null;
    this.errorEdicionDetalle = '';
    this.elementoOrigenEdicion = origen as HTMLElement;
    this.edicionDetalleAbierta = true;

    setTimeout(() => (document.getElementById('despacho-nuevo-valor-detalle') as HTMLInputElement)?.focus());
  }

  confirmarEdicionDetalle(): void {
    const item = this.lineas[this.indiceDetalleEdicion];
    const valorVacio = this.valorDetalleNuevo === null || this.valorDetalleNuevo === undefined
      || String(this.valorDetalleNuevo).trim() === '';

    if (!item || valorVacio) {
      this.errorEdicionDetalle = 'Ingresa un valor nuevo.';
      return;
    }

    const nuevoValor = Number(this.valorDetalleNuevo);
    if (!Number.isInteger(nuevoValor) || nuevoValor <= 0) {
      this.errorEdicionDetalle = 'La cantidad debe ser un n&uacute;mero entero mayor a 0.';
      return;
    }
    if (nuevoValor > item.existenciaBodega) {
      this.errorEdicionDetalle = `La bodega solo tiene ${item.existenciaBodega} unidades de este producto.`;
      return;
    }

    item.cantidad = nuevoValor;
    item.subTotal = item.calcularSubTotal();
    this.lineas = [...this.lineas];
    this.cerrarEdicionDetalle();
  }

  cancelarEdicionDetalle(): void {
    this.cerrarEdicionDetalle();
  }

  evitarCambioConRueda(event: WheelEvent): void {
    (event.target as HTMLInputElement).blur();
  }

  private cerrarEdicionDetalle(): void {
    const elementoOrigen = this.elementoOrigenEdicion;
    this.edicionDetalleAbierta = false;
    this.indiceDetalleEdicion = -1;
    this.valorDetalleAnterior = null;
    this.valorDetalleNuevo = null;
    this.errorEdicionDetalle = '';
    this.elementoOrigenEdicion = null;
    setTimeout(() => elementoOrigen?.focus());
  }

  get totalUnidades(): number {
    return this.lineas.reduce((acumulado, item) => acumulado + (item.cantidad || 0), 0);
  }

  get totalDespacho(): number {
    return this.lineas.reduce((acumulado, item) => acumulado + item.calcularSubTotal(), 0);
  }

  /*********** GUARDADO ***********/

  crear(): void {
    if (this.lineas.length === 0) {
      swal.fire('Sin productos', 'Agrega al menos un producto al detalle del despacho.', 'warning');
      return;
    }
    if (!this.despacho.idBodega || !this.despacho.idSucursalDestino) {
      swal.fire('Datos incompletos', 'Selecciona la bodega y la sucursal destino del despacho.', 'warning');
      return;
    }
    if (!this.despacho.recibidoPor || !this.despacho.recibidoPor.trim()) {
      swal.fire('Datos incompletos', 'Indica el nombre de quien recibe el despacho.', 'warning');
      return;
    }

    const request: DespachoBodegaRequest = {
      idBodega: this.despacho.idBodega,
      idSucursalDestino: this.despacho.idSucursalDestino,
      recibidoPor: this.despacho.recibidoPor.trim(),
      observaciones: (this.despacho.observaciones || '').trim(),
      items: this.lineas.map(linea => ({ idProducto: linea.producto.idProducto, cantidad: linea.cantidad }))
    };

    this.guardando = true;
    this.despachoService.create(request).subscribe(
      creado => {
        this.guardando = false;
        this.router.navigate(['/despachos-bodega/index']);
        swal.fire({
          title: 'Despacho registrado',
          html: `El despacho <strong>${String(creado.idDespacho).padStart(6, '0')}</strong> qued&oacute; ` +
            '<strong>pendiente de aprobaci&oacute;n</strong>. ' +
            'Las existencias ya salieron de la bodega y ingresar&aacute;n a la sucursal cuando sea aprobado.',
          icon: 'success',
          showCancelButton: true,
          confirmButtonText: '<i class="fas fa-print"></i> Imprimir comprobante',
          cancelButtonText: 'Cerrar'
        }).then(resultado => {
          if (resultado.isConfirmed) {
            this.despachoService.imprimirComprobante(creado.idDespacho).subscribe();
          }
        });
      },
      () => this.guardando = false
    );
  }

}
