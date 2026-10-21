import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { Bodega } from 'src/app/models/bodega';
import { DetalleDevolucionBodega } from 'src/app/models/detalle-devolucion-bodega';
import { Producto } from 'src/app/models/producto';
import { Sucursal } from 'src/app/models/sucursal';
import { DevolucionBodegaRequest } from 'src/app/dtos/devolucion-bodega-request';
import { ProductoSucursalDto } from 'src/app/dtos/producto-sucursal-dto';
import { AuthService } from 'src/app/services/auth.service';
import { BodegaService } from 'src/app/services/bodega.service';
import { DevolucionBodegaService } from 'src/app/services/devolucion-bodega.service';
import { SucursalService } from 'src/app/services/sucursal.service';

import swal from 'sweetalert2';

@Component({
  selector: 'app-create-devolucion-bodega',
  templateUrl: './create-devolucion-bodega.component.html',
  styleUrls: [
    '../../productos/create-producto/create-producto.component.css',
    '../../proformas/create-proforma/create-proforma.component.css',
    '../../despachos-bodega/create-despacho-bodega/create-despacho-bodega.component.css',
    './create-devolucion-bodega.component.css'
  ]
})
export class CreateDevolucionBodegaComponent implements OnInit {

  title = 'Registrar nueva devolución';
  devolucion = new DevolucionBodegaRequest();
  lineas: DetalleDevolucionBodega[] = [];

  sucursales: Sucursal[] = [];
  bodegas: Bodega[] = [];

  /** Producto elegido (con la existencia de la tienda) antes de agregarlo al detalle. */
  itemActual: ProductoSucursalDto = null;
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
    private devolucionService: DevolucionBodegaService,
    private sucursalService: SucursalService,
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private auth: AuthService
  ) {
    this.devolucion.entregadoPor = '';
    this.devolucion.motivo = '';
    this.devolucion.observaciones = '';
  }

  ngOnInit(): void {
    this.sucursalService.getSucursales().subscribe(sucursales => {
      this.sucursales = sucursales.filter(sucursal => !sucursal.estado || sucursal.estado.estado === 'ACTIVO');
      const sucursalUsuario = this.auth.usuario?.sucursal?.idSucursal;
      if (!this.devolucion.idSucursalOrigen && sucursalUsuario
        && this.sucursales.some(sucursal => sucursal.idSucursal === sucursalUsuario)) {
        this.alCambiarSucursal(sucursalUsuario);
      }
    });
    this.bodegaService.getBodegas(true).subscribe(bodegas => {
      this.bodegas = bodegas;
      const idBodega = this.activatedRoute.snapshot.queryParamMap.get('bodega');
      if (idBodega && bodegas.some(bodega => bodega.idBodega === +idBodega)) {
        this.devolucion.idBodega = +idBodega;
      } else {
        this.aplicarBodegaPorDefecto();
      }
    });
  }

  get sucursalSeleccionada(): Sucursal {
    return this.sucursales.find(sucursal => sucursal.idSucursal === this.devolucion.idSucursalOrigen);
  }

  get bodegaSeleccionada(): Bodega {
    return this.bodegas.find(bodega => bodega.idBodega === this.devolucion.idBodega);
  }

  /** Al elegir la tienda, la bodega asignada a ella (si existe) se propone como destino. */
  alCambiarSucursal(idSucursal: number): void {
    this.devolucion.idSucursalOrigen = idSucursal;
    this.itemActual = null;
    this.codigoBusqueda = '';
    this.cantidadActual = null;
    this.aplicarBodegaPorDefecto();
  }

  private aplicarBodegaPorDefecto(): void {
    if (this.devolucion.idBodega || !this.devolucion.idSucursalOrigen) {
      return;
    }
    const delaSucursal = this.bodegas.filter(
      bodega => bodega.sucursal && bodega.sucursal.idSucursal === this.devolucion.idSucursalOrigen);
    if (delaSucursal.length === 1) {
      this.devolucion.idBodega = delaSucursal[0].idBodega;
    }
  }

  /*********** SELECCIÓN DE PRODUCTO ***********/

  private validarSucursal(): boolean {
    if (!this.devolucion.idSucursalOrigen) {
      swal.fire('Selecciona la tienda', 'Primero indica la tienda que devuelve el producto.', 'warning');
      return false;
    }
    return true;
  }

  buscarProductoPorCodigo(): void {
    if (!this.validarSucursal()) {
      return;
    }
    const codigo = (this.codigoBusqueda || '').trim();
    if (!codigo) {
      swal.fire('Código inválido', 'Ingresa un código de producto.', 'warning');
      return;
    }
    this.buscando = true;
    this.devolucionService.getProductosSucursal(this.devolucion.idSucursalOrigen, 0, 10, codigo).subscribe(
      respuesta => {
        this.buscando = false;
        // La búsqueda filtra por texto: solo se acepta la coincidencia exacta del código
        const item: ProductoSucursalDto = (respuesta.content || []).find(
          (producto: ProductoSucursalDto) => (producto.codProducto || '').toLowerCase() === codigo.toLowerCase());
        if (item) {
          this.seleccionarItem(item);
        } else {
          swal.fire('Producto no encontrado', 'Ese código no existe en el inventario de la tienda seleccionada.', 'error');
        }
      },
      error => {
        this.buscando = false;
        swal.fire('No se pudo buscar el producto', error.error?.message || 'Intenta nuevamente.', 'error');
      }
    );
  }

  buscarProductoConEnter(evento: KeyboardEvent): void {
    evento.preventDefault();
    this.buscarProductoPorCodigo();
  }

  abrirModalProducto(): void {
    if (this.validarSucursal()) {
      this.modalProductoVisible = true;
    }
  }

  cerrarModalProducto(): void {
    this.modalProductoVisible = false;
  }

  elegirDelModal(item: ProductoSucursalDto): void {
    this.cerrarModalProducto();
    this.seleccionarItem(item);
  }

  private seleccionarItem(item: ProductoSucursalDto): void {
    this.itemActual = item;
    this.codigoBusqueda = item.codProducto;
    this.cantidadActual = null;
    this.enfocarCantidad();
  }

  /** El campo de cantidad está deshabilitado hasta que hay producto: se espera a que se habilite para darle el foco. */
  private enfocarCantidad(): void {
    setTimeout(() => (document.getElementById('cantidadActual') as HTMLInputElement)?.focus());
  }

  /** Existencia de la tienda que todavía no está comprometida en otras líneas de la misma devolución. */
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
      swal.fire('Selecciona un producto', 'Busca un producto de la tienda por código o desde el listado.', 'warning');
      return;
    }
    const cantidad = Number(this.cantidadActual);
    if (!Number.isInteger(cantidad) || cantidad <= 0) {
      swal.fire('Cantidad inválida', 'La cantidad debe ser un número entero mayor a 0.', 'warning');
      return;
    }
    if (cantidad > this.disponibleItemActual) {
      swal.fire('Existencias insuficientes',
        `La tienda tiene ${this.itemActual.stock} unidades de este producto` +
        (this.disponibleItemActual < this.itemActual.stock
          ? ` y ya agregaste ${this.cantidadEnDetalle(this.itemActual.idProducto)}.` : '.'),
        'warning');
      return;
    }

    const existente = this.lineas.find(linea => linea.producto.idProducto === this.itemActual.idProducto);
    if (existente) {
      existente.cantidad += cantidad;
      this.lineas = [...this.lineas];
    } else {
      const detalle = new DetalleDevolucionBodega();
      detalle.producto = Object.assign(new Producto(), {
        idProducto: this.itemActual.idProducto,
        codProducto: this.itemActual.codProducto,
        nombre: this.itemActual.nombreProducto
      });
      detalle.cantidad = cantidad;
      detalle.existenciaSucursal = this.itemActual.stock;
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

  eliminarLinea(detalle: DetalleDevolucionBodega): void {
    swal.fire({
      title: '¿Quitar producto?',
      text: `Se quitará ${detalle.producto.nombre} del detalle de la devolución.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, quitar',
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

    setTimeout(() => (document.getElementById('devolucion-nuevo-valor-detalle') as HTMLInputElement)?.focus());
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
      this.errorEdicionDetalle = 'La cantidad debe ser un número entero mayor a 0.';
      return;
    }
    if (nuevoValor > item.existenciaSucursal) {
      this.errorEdicionDetalle = `La tienda solo tiene ${item.existenciaSucursal} unidades de este producto.`;
      return;
    }

    item.cantidad = nuevoValor;
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

  /*********** GUARDADO ***********/

  crear(): void {
    if (this.lineas.length === 0) {
      swal.fire('Sin productos', 'Agrega al menos un producto al detalle de la devolución.', 'warning');
      return;
    }
    if (!this.devolucion.idSucursalOrigen || !this.devolucion.idBodega) {
      swal.fire('Datos incompletos', 'Selecciona la tienda que devuelve y la bodega que recibe.', 'warning');
      return;
    }
    if (!this.devolucion.entregadoPor || !this.devolucion.entregadoPor.trim()) {
      swal.fire('Datos incompletos', 'Indica el nombre de quien entrega la devolución.', 'warning');
      return;
    }
    if (!this.devolucion.motivo || !this.devolucion.motivo.trim()) {
      swal.fire('Datos incompletos', 'Indica el motivo de la devolución.', 'warning');
      return;
    }

    const request: DevolucionBodegaRequest = {
      idSucursalOrigen: this.devolucion.idSucursalOrigen,
      idBodega: this.devolucion.idBodega,
      entregadoPor: this.devolucion.entregadoPor.trim(),
      motivo: this.devolucion.motivo.trim(),
      observaciones: (this.devolucion.observaciones || '').trim(),
      items: this.lineas.map(linea => ({ idProducto: linea.producto.idProducto, cantidad: linea.cantidad }))
    };

    this.guardando = true;
    this.devolucionService.create(request).subscribe(
      creada => {
        this.guardando = false;
        this.router.navigate(['/devoluciones-bodega/index']);
        swal.fire({
          title: 'Devolución registrada',
          html: `La devolución <strong>${String(creada.idDevolucion).padStart(6, '0')}</strong> quedó ` +
            '<strong>pendiente de aprobación</strong>. ' +
            'Las existencias ya salieron de la tienda y ingresarán a la bodega cuando un administrador la apruebe.',
          icon: 'success',
          showCancelButton: true,
          confirmButtonText: '<i class="fas fa-print"></i> Imprimir comprobante',
          cancelButtonText: 'Cerrar'
        }).then(resultado => {
          if (resultado.isConfirmed) {
            this.devolucionService.imprimirComprobante(creada.idDevolucion).subscribe();
          }
        });
      },
      () => this.guardando = false
    );
  }

}
