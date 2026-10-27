import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, Subject, Subscription } from 'rxjs';
import { debounceTime } from 'rxjs/operators';

import { Producto } from '../../../models/producto';
import { Cliente } from '../../../models/cliente';
import { Proforma } from '../../../models/proforma';
import { UsuarioAuxiliar } from '../../../models/auxiliar/usuario-auxiliar';
import { DetalleProforma } from '../../../models/detalle-proforma';
import {
  ETIQUETAS_TIPO_DESCUENTO, OpcionDescuento, OpcionesDescuento, TipoDescuento
} from '../../../dtos/descuento-dto';

import { AuthService } from '../../../services/auth.service';
import { ProductoService } from '../../../services/producto.service';
import { ClienteService } from '../../../services/cliente.service';
import { ClienteCreateService } from '../../../services/facturas/cliente-create.service';
import { UsuarioService } from '../../../services/usuarios/usuario.service';
import { ProformaService } from '../../../services/proformas/proforma.service';
import { DescuentoService } from '../../../services/descuento.service';
import swal from 'sweetalert2';

type CampoDetalleEditable = 'cantidad' | 'descuento';

@Component({
  selector: 'app-create-proforma',
  templateUrl: './create-proforma.component.html',
  styleUrls: [
    '../../productos/create-producto/create-producto.component.css',
    './create-proforma.component.css'
  ]
})
export class CreateProformaComponent implements OnInit, OnDestroy {

  title: string;
  nitIngresado: string;
  nitBusqueda = '';
  noProforma: string;
  cantidadProducto: number = null;
  descuentoProducto = 0;
  isSaving = false;
  modalProductoVisible = false;
  modalClienteVisible = false;

  edicionDetalleAbierta = false;
  campoDetalleEdicion: CampoDetalleEditable = null;
  indiceDetalleEdicion = -1;
  valorDetalleAnterior: number = null;
  valorDetalleNuevo: number = null;
  errorEdicionDetalle = '';

  private elementoOrigenEdicion: HTMLElement;

  /** Descuentos que ofrece el sistema para el producto elegido y el cliente mayorista; nulo si no aplican. */
  opciones: OpcionesDescuento = null;
  tipoDescuento: TipoDescuento = 'MANUAL';
  nivelSeleccionado: number = null;
  readonly etiquetasTipoDescuento = ETIQUETAS_TIPO_DESCUENTO;
  private tipoElegidoPorUsuario = false;
  private idClienteDescuentos: number = null;
  private cantidadCambio$ = new Subject<void>();
  private cantidadSubscription: Subscription;

  producto: Producto;
  cliente: Cliente;
  usuario: UsuarioAuxiliar;
  proforma: Proforma;
  proformaCargada: Proforma;

  constructor(
    private proformaService: ProformaService,
    private productoService: ProductoService,
    private clienteService: ClienteService,
    private clienteCreateService: ClienteCreateService,
    private usuarioService: UsuarioService,
    public authService: AuthService,
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private descuentoService: DescuentoService
  ) {
    this.title = 'Crear Proforma';
    this.producto = new Producto();
    this.cliente = new Cliente();
    this.usuario = new UsuarioAuxiliar();
    this.proforma = new Proforma();
    this.proformaCargada = new Proforma();
  }

  ngOnInit(): void {
    this.loadUsuario();
    this.cargarProforma();
    // Al cambiar la cantidad pueden aplicar o dejar de aplicar el ciento o el fardo.
    this.cantidadSubscription = this.cantidadCambio$.pipe(debounceTime(300)).subscribe(() => this.cargarOpciones());
  }

  ngOnDestroy(): void {
    if (this.cantidadSubscription) {
      this.cantidadSubscription.unsubscribe();
    }
  }

  // ------------------------------------------------------------ descuentos del cliente mayorista

  get esClienteMayorista(): boolean {
    return !!this.cliente && this.cliente.tipoCliente === 'MAYORISTA';
  }

  /** Proforma mayorista: cliente mayorista con ruta. Habilita observaciones, entrega estimada y las dos copias. */
  get esProformaMayorista(): boolean {
    return this.esClienteMayorista && !!this.cliente.ruta;
  }

  /** El cliente puede comprar a crédito: tiene límite y días de crédito. */
  get clienteConCredito(): boolean {
    return !!this.cliente && Number(this.cliente.limiteCredito) > 0 && Number(this.cliente.diasCredito) > 0;
  }

  readonly maxObservacionLinea = 255;
  readonly maxObservacionGeneral = 500;
  readonly vigenciaDiasMayorista = 15;
  /** Porcentaje sobre el que el servidor pide autorización (solo para avisar; el servidor decide). */
  readonly topeDescuento = 15;

  /** Editar un pedido que bodega ya empezó a preparar lo devuelve a «Confirmado»: hay que imprimir de nuevo el picking. */
  get pedidoEnPreparacion(): boolean {
    const etapa = this.proforma.estadoPedido;
    return this.esProformaMayorista && (etapa === 'EN_PREPARACION' || etapa === 'EMPACADA' || etapa === 'EN_RUTA');
  }

  /** Una línea mayorista con más del tope, que no sea precio mayorista fijo, deja la proforma pendiente de autorización. */
  get requiereAutorizacion(): boolean {
    return this.esProformaMayorista && this.proforma.itemsProforma.some(item =>
      item.tipoDescuento !== 'PRECIO_MAYORISTA' && Number(item.descuento) > this.topeDescuento);
  }

  /** Hoy en formato yyyy-MM-dd (zona del navegador), como mínimo de la entrega estimada. */
  get hoy(): string {
    const fecha = new Date();
    const dos = (n: number) => String(n).padStart(2, '0');
    return `${fecha.getFullYear()}-${dos(fecha.getMonth() + 1)}-${dos(fecha.getDate())}`;
  }

  get opcionActual(): OpcionDescuento {
    return this.opciones ? this.opciones.opciones.find(opcion => opcion.tipo === this.tipoDescuento) || null : null;
  }

  /** Niveles entre los que elegir cuando la categoría del cliente tiene varios (Volumen). */
  get nivelesDisponibles(): number[] {
    const opcion = this.opcionActual;
    return opcion && opcion.tipo === 'CATEGORIA' && opcion.niveles ? opcion.niveles : [];
  }

  /** El porcentaje solo se escribe a mano en el descuento manual; en los demás lo fija la regla elegida. */
  get descuentoEditable(): boolean {
    return !this.esClienteMayorista || !this.opciones || this.tipoDescuento === 'MANUAL';
  }

  /** Aviso (no bloqueo) cuando la cantidad no llega al mínimo del descuento elegido. */
  get avisoMinimo(): string {
    const opcion = this.opcionActual;
    if (opcion && opcion.cantidadMinima && !opcion.alcanzaMinimo) {
      return `Este descuento aplica desde ${opcion.cantidadMinima} unidades y llevas ${Number(this.cantidadProducto) || 0}. `
        + 'Puedes agregarlo de todos modos.';
    }
    return '';
  }

  etiquetaTipo(tipo: TipoDescuento): string {
    return ETIQUETAS_TIPO_DESCUENTO[tipo] || '';
  }

  alCambiarCantidad(): void {
    if (this.esClienteMayorista && this.producto && this.producto.idProducto) {
      this.cantidadCambio$.next();
    }
  }

  cambiarTipoDescuento(tipo: TipoDescuento): void {
    this.tipoElegidoPorUsuario = true;
    this.tipoDescuento = tipo;
    this.nivelSeleccionado = null;
    const opcion = this.opcionActual;
    if (tipo !== 'MANUAL') {
      this.descuentoProducto = opcion && opcion.porcentaje !== null ? opcion.porcentaje : 0;
    }
  }

  elegirNivel(nivel: number): void {
    this.nivelSeleccionado = nivel;
    this.descuentoProducto = nivel;
  }

  /** Pide al servidor las opciones del producto elegido para este cliente y cantidad. */
  cargarOpciones(): void {
    if (!this.esClienteMayorista || !this.producto || !this.producto.idProducto) {
      this.opciones = null;
      this.tipoDescuento = 'MANUAL';
      return;
    }
    const cantidad = Math.max(Number(this.cantidadProducto) || 1, 1);
    this.descuentoService.getOpciones(this.producto.idProducto, this.cliente.idCliente, cantidad).subscribe(
      opciones => {
        this.opciones = opciones;
        if (!this.tipoElegidoPorUsuario) {
          this.aplicarSugerida();
        }
      },
      () => {
        this.opciones = null;
        this.tipoDescuento = 'MANUAL';
      }
    );
  }

  private aplicarSugerida(): void {
    const sugerida = this.opciones ? this.opciones.sugerida : null;
    this.nivelSeleccionado = null;
    if (sugerida) {
      this.tipoDescuento = sugerida.tipo;
      this.descuentoProducto = sugerida.porcentaje !== null ? sugerida.porcentaje : 0;
    } else {
      this.tipoDescuento = 'MANUAL';
      this.descuentoProducto = 0;
    }
  }

  private restablecerDescuento(): void {
    this.opciones = null;
    this.tipoDescuento = 'MANUAL';
    this.nivelSeleccionado = null;
    this.descuentoProducto = 0;
    this.tipoElegidoPorUsuario = false;
  }

  /**
   * Al cambiar de cliente las líneas ya agregadas pueden quedar con un descuento que ya no le corresponde
   * (otra categoría, o un cliente que no es mayorista): se conservan los porcentajes, pero como descuento manual.
   */
  private alCambiarCliente(): void {
    const cambio = this.idClienteDescuentos !== null && this.idClienteDescuentos !== this.cliente.idCliente;
    this.idClienteDescuentos = this.cliente.idCliente;
    if (cambio) {
      const conTipo = this.proforma.itemsProforma.filter(item => item.tipoDescuento && item.tipoDescuento !== 'MANUAL');
      if (conTipo.length > 0) {
        conTipo.forEach(item => item.tipoDescuento = 'MANUAL');
        swal.fire('Descuentos revisados', 'Cambió el cliente: los descuentos de las líneas ya agregadas se conservan '
          + 'como descuentos manuales.', 'info');
      }
    }
    this.cargarOpciones();
  }

  loadUsuario(): void {
    this.usuarioService.getUsuario(this.authService.usuario.idUsuario).subscribe(
      usuario => {
        this.usuario = usuario;
      }, error => {
        swal.fire('Error al Cargar Usuario', `${error.error.message}`, 'error');
      }
    );
  }

  abrirModalProducto(): void {
    this.modalProductoVisible = true;
  }

  cerrarModalProducto(): void {
    this.modalProductoVisible = false;
  }

  abrirModalCliente(): void {
    this.modalClienteVisible = true;
  }

  cerrarModalCliente(): void {
    this.modalClienteVisible = false;
  }

  loadProducto(producto: Producto): void {
    this.producto.codProducto = producto.codProducto;
    this.cerrarModalProducto();
    this.buscarProducto();
  }

  loadCliente(cliente: Cliente): void {
    this.cerrarModalCliente();
    // El directorio entrega una fila del listado; se pide el cliente completo por su id. Buscar de nuevo por NIT
    // fallaría con un cliente sin NIT, que es válido hasta que se factura.
    this.clienteService.getCliente(cliente.idCliente).subscribe(
      completo => this.seleccionarCliente(completo)
    );
  }

  cargarCliente(cliente: Cliente): void {
    this.cliente = cliente;
    this.nitBusqueda = cliente.nit;
    this.nitIngresado = null;
    this.alCambiarCliente();
  }

  buscarProducto(): void {
    const codigo = this.producto.codProducto;
    if (!codigo) {
      swal.fire('Código Inválido', 'Ingrese un código de producto válido para realizar la búsqueda.', 'warning');
      return;
    }

    this.productoService.getProductoByCode(codigo).subscribe(
      producto => {
        this.producto = producto;
        this.tipoElegidoPorUsuario = false;
        this.cargarOpciones();
        this.enfocarCantidad();
      },
      error => {
        const status = error.status;
        const message = status === 400
          ? 'Petición no se puede llevar a cabo.'
        : error.error?.message || 'Error desconocido';
        swal.fire(`Error: ${error.error?.status || status}`, message, 'error');
      }
    );
  }

  private enfocarCantidad(): void {
    setTimeout(() => (document.getElementById('cantidad') as HTMLInputElement)?.focus(), 350);
  }

  buscarCliente(): void {
    const valor = (this.nitBusqueda || '').trim();

    if (valor) {
      this.clienteService.getClienteByNit(valor).subscribe(
        cliente => this.seleccionarCliente(cliente),
        error => {
          if (error.status === 400) {
            swal.fire(`Error: ${error.error.status}`, 'Petición Equivocada', 'error');
          }
          if (error.status === 404) {
            this.buscarClientePorCodigo(valor);
          }
        }
      );
    } else {
      swal.fire('NIT Vacío', 'Ingrese un valor valido para realizar la búsqueda.', 'warning');
    }
  }

  /** Si el valor no es un NIT registrado se prueba como código de cliente; si tampoco existe, se ofrece registrarlo. */
  private buscarClientePorCodigo(valor: string): void {
    this.clienteService.getClienteByCodigo(valor).subscribe(
      cliente => this.seleccionarCliente(cliente),
      error => {
        if (error.status === 404) {
          this.nitIngresado = valor;
          this.clienteCreateService.abrirModal();
        }
      }
    );
  }

  private seleccionarCliente(cliente: Cliente): void {
    this.cliente = cliente;
    this.nitBusqueda = cliente.nit || cliente.codigoCliente || '';
    this.alCambiarCliente();
    (document.getElementById('codigo') as HTMLInputElement)?.focus();
  }

  agregarLinea(): void {
    if (!this.cliente) { // Comprueba que el cliente exista
      swal.fire('Ha ocurrido un Problema', 'Por favor, elija un cliente antes de llevar a cabo la venta.', 'error');
    } else {
      if (this.producto) { // comprueba que el producto exista
        const itemProforma = new DetalleProforma();

        itemProforma.cantidad = Number(this.cantidadProducto);
        itemProforma.descuento = Number(this.descuentoProducto || 0);
        itemProforma.tipoDescuento = this.esClienteMayorista && this.opciones ? this.tipoDescuento : 'MANUAL';

        if (!Number.isInteger(itemProforma.cantidad) || itemProforma.cantidad <= 0) {
          swal.fire('Cantidad Inválida', 'La cantidad debe ser un número entero mayor a 0.', 'warning');
          return;
        }

        if (!Number.isFinite(itemProforma.descuento)
          || itemProforma.descuento < 0 || itemProforma.descuento > 100) {
          swal.fire('Descuento Inválido', 'El descuento debe estar entre 0% y 100%.', 'warning');
          return;
        }

        if (itemProforma.tipoDescuento === 'CATEGORIA' && this.nivelesDisponibles.length > 0 && this.nivelSeleccionado === null) {
          swal.fire('Elija el nivel', 'La categoría del cliente tiene varios niveles de descuento; elija cuál aplicar.', 'warning');
          return;
        }

        if (itemProforma.cantidad > this.producto.stock) {
          swal.fire('Stock Insuficiente', 'No existen las suficientes existencias de este producto.', 'warning');
          return;
        } else {
          if (itemProforma.cantidad && itemProforma.cantidad !== 0) {
            if (this.existeItem(this.producto.idProducto)) {
              this.incrementaCantidad(this.producto.idProducto, itemProforma.cantidad, itemProforma.descuento,
                itemProforma.tipoDescuento);
              this.producto = new Producto();
              this.cantidadProducto = null;
              this.restablecerDescuento();
            } else {
                itemProforma.producto = this.producto;
                itemProforma.subTotalDescuento = itemProforma.calcularImporteDescuento();
                itemProforma.subTotal = itemProforma.calcularImporte();
                itemProforma.nuevoPrecioVenta = itemProforma.calcularNuevoPrecioVenta();

                this.proforma.itemsProforma = [...this.proforma.itemsProforma, itemProforma];
                this.recalcularTotal();
                this.producto = new Producto();
                this.cantidadProducto = null;
                this.restablecerDescuento();
            }

          } else if (itemProforma.cantidad === 0) {
            swal.fire('Cantidad Erronéa', 'La cantidad a agregar debe ser mayor a 0.', 'warning');
          } else if (!itemProforma.cantidad) {
            swal.fire('Valor Inválido', 'La cantidad no puede estar vacía.  Ingrese un valor válido.', 'warning');
          }
        }
      }
    }
  }

  existeItem(id: number): boolean {
    let existe = false;
    this.proforma.itemsProforma.forEach((item: DetalleProforma) => {
      if (id === item.producto.idProducto) {
        existe = true;
      }
    });
    return existe;
  }

  eliminarItem(index: number): void {
    const item = this.proforma.itemsProforma[index];
    if (!item) {
      return;
    }

    swal.fire({
      title: '¿Eliminar producto?',
      text: `Se eliminará ${item.producto.nombre} del detalle de la proforma.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar',
      reverseButtons: true,
      focusCancel: true
    }).then(result => {
      if (!result.isConfirmed) {
        return;
      }

      this.proforma.itemsProforma = this.proforma.itemsProforma.filter((detalle, itemIndex) => itemIndex !== index);
      this.recalcularTotal();
    });
  }

  abrirEdicionDetalle(index: number, campo: CampoDetalleEditable, origen: EventTarget): void {
    const item = this.proforma.itemsProforma[index];
    if (!item) {
      return;
    }

    this.indiceDetalleEdicion = index;
    this.campoDetalleEdicion = campo;
    this.valorDetalleAnterior = campo === 'cantidad' ? Number(item.cantidad) : Number(item.descuento || 0);
    this.valorDetalleNuevo = null;
    this.errorEdicionDetalle = '';
    this.elementoOrigenEdicion = origen as HTMLElement;
    this.edicionDetalleAbierta = true;

    setTimeout(() => (document.getElementById('nuevo-valor-detalle') as HTMLInputElement)?.focus());
  }

  confirmarEdicionDetalle(): void {
    const item = this.proforma.itemsProforma[this.indiceDetalleEdicion];
    const valorVacio = this.valorDetalleNuevo === null || this.valorDetalleNuevo === undefined
      || String(this.valorDetalleNuevo).trim() === '';

    if (!item || valorVacio) {
      this.errorEdicionDetalle = 'Ingresa un valor nuevo.';
      return;
    }

    const nuevoValor = Number(this.valorDetalleNuevo);
    if (!Number.isFinite(nuevoValor)) {
      this.errorEdicionDetalle = 'Ingresa un valor numérico válido.';
      return;
    }

    if (this.campoDetalleEdicion === 'cantidad') {
      if (!Number.isInteger(nuevoValor) || nuevoValor <= 0) {
        this.errorEdicionDetalle = 'La cantidad debe ser un número entero mayor a 0.';
        return;
      }
      if (nuevoValor > Number(item.producto.stock)) {
        this.errorEdicionDetalle = `La cantidad no puede superar el stock disponible (${item.producto.stock}).`;
        return;
      }
      item.cantidad = nuevoValor;
    } else {
      if (nuevoValor < 0 || nuevoValor > 100) {
        this.errorEdicionDetalle = 'El descuento debe estar entre 0% y 100%.';
        return;
      }
      item.descuento = nuevoValor;
      item.tipoDescuento = 'MANUAL';
    }

    item.subTotal = item.calcularImporte();
    item.subTotalDescuento = item.calcularImporteDescuento();
    item.nuevoPrecioVenta = item.calcularNuevoPrecioVenta();
    this.proforma.itemsProforma = [...this.proforma.itemsProforma];
    this.recalcularTotal();
    this.cerrarEdicionDetalle();
  }

  cancelarEdicionDetalle(): void {
    this.cerrarEdicionDetalle();
  }

  evitarCambioConRueda(event: WheelEvent): void {
    (event.target as HTMLInputElement).blur();
  }

  get tituloEdicionDetalle(): string {
    return this.campoDetalleEdicion === 'cantidad' ? 'Editar cantidad' : 'Editar descuento';
  }

  get stockDetalleEdicion(): number {
    const item = this.proforma.itemsProforma[this.indiceDetalleEdicion];
    return item ? Number(item.producto.stock) : 0;
  }

  private cerrarEdicionDetalle(): void {
    const elementoOrigen = this.elementoOrigenEdicion;
    this.edicionDetalleAbierta = false;
    this.campoDetalleEdicion = null;
    this.indiceDetalleEdicion = -1;
    this.valorDetalleAnterior = null;
    this.valorDetalleNuevo = null;
    this.errorEdicionDetalle = '';
    this.elementoOrigenEdicion = null;
    setTimeout(() => elementoOrigen?.focus());
  }

  incrementaCantidad(idProducto: number, cantidad: number, descuento: number, tipoDescuento: TipoDescuento = 'MANUAL'): void {
    this.proforma.itemsProforma = this.proforma.itemsProforma.map((item: DetalleProforma) => {
      if (idProducto === item.producto.idProducto) {
        item.cantidad = item.cantidad + cantidad;
        item.descuento = descuento;
        item.tipoDescuento = tipoDescuento;
        item.subTotal = item.calcularImporte();
        item.subTotalDescuento = item.calcularImporteDescuento();
        item.nuevoPrecioVenta = item.calcularNuevoPrecioVenta();
      }

      return item;
    });
    this.recalcularTotal();
  }

  cantidadesValidas(): boolean {
    return this.proforma.itemsProforma.every((item: DetalleProforma) =>
      Number.isInteger(Number(item.cantidad)) && Number(item.cantidad) > 0
    );
  }

  recalcularTotal(): void {
    this.proforma.total = this.proforma.calcularTotal();
  }

  createProforma(): void {
    if (!this.cantidadesValidas()) {
      swal.fire('Cantidad Inválida', 'Todos los productos deben tener una cantidad entera mayor a 0.', 'warning');
      return;
    }

    this.isSaving = true;

    if (this.isSaving) {
      this.proforma.noProforma = this.noProforma;
      this.proforma.cliente = this.cliente;
      this.proforma.usuario = this.usuario;
      this.proforma.total = this.proforma.calcularTotal();
      this.prepararMayorista();

      this.proformaService.create(this.proforma).subscribe(response => {
        if (response) {
          const generar = () => this.generarProformaPdf(response.idProforma, this.esProformaMayorista);
          if (response.estadoAutorizacion === 'PENDIENTE') {
            swal.fire('Pendiente de autorización',
              `La proforma ${response.noProforma} tiene un descuento mayor a ${this.topeDescuento} %. `
              + 'No se podrá facturar hasta que un administrador la apruebe.', 'info').then(generar);
          } else {
            generar();
          }
        }
      }, error => {
        this.isSaving = false;
        swal.fire(`Error: ${error.error.status}`, `${error.error.message}`, 'error');
      });
    }
  }

  /** Deja el tipo y las notas coherentes con el cliente: lo que no aplica a una minorista no se envía. */
  private prepararMayorista(): void {
    if (this.esProformaMayorista) {
      this.proforma.tipoProforma = 'MAYORISTA';
      this.proforma.observaciones = (this.proforma.observaciones || '').trim() || null;
      this.proforma.fechaEntregaEstimada = this.proforma.fechaEntregaEstimada || null;
      // Sin crédito en el cliente el pedido es de contado, aunque antes se hubiera elegido crédito.
      this.proforma.condicionPago = this.clienteConCredito && this.proforma.condicionPago === 'CREDITO' ? 'CREDITO' : 'CONTADO';
    } else {
      this.proforma.tipoProforma = 'MINORISTA';
      this.proforma.condicionPago = 'CONTADO';
      this.proforma.observaciones = null;
      this.proforma.fechaEntregaEstimada = null;
      this.proforma.itemsProforma.forEach(item => item.observacion = null);
    }
  }

  cargarProforma(): void {
    this.activatedRoute.params.subscribe(params => {
      const id = params.proformaId;

      if (id) {
        this.buscarProformaPorId(id);
      }
    });
  }

  buscarProformaPorId(id: number): void {
    this.proformaService.getProforma(id).subscribe(
      response => {
        if (!response.mensaje) {
          this.proformaCargada = response;

          this.cliente = response.cliente;
          this.idClienteDescuentos = this.cliente.idCliente;
          this.nitBusqueda = this.cliente.nit || '';

          this.proforma.idProforma = response.idProforma;
          this.proforma.noProforma = response.noProforma;
          this.proforma.cliente = this.cliente;
          this.proforma.fechaEmision = response.fechaEmision;
          this.proforma.estado = response.estado;
          this.proforma.usuario = response.usuario;
          this.proforma.total = response.total;
          this.proforma.tipoProforma = response.tipoProforma;
          this.proforma.estadoAutorizacion = response.estadoAutorizacion;
          this.proforma.estadoPedido = response.estadoPedido;
          this.proforma.observaciones = response.observaciones;
          this.proforma.fechaEntregaEstimada = response.fechaEntregaEstimada;
          this.proforma.condicionPago = response.condicionPago || 'CONTADO';
          this.proforma.diasCredito = response.diasCredito;
          this.proforma.fechaVencimiento = response.fechaVencimiento;


          this.proforma.itemsProforma = response.itemsProforma.map((itemProforma: DetalleProforma) => {
            const item = new DetalleProforma();

            item.cantidad = itemProforma.cantidad;
            item.subTotal = itemProforma.subTotal;
            item.subTotalDescuento = itemProforma.subTotalDescuento;
            item.producto = itemProforma.producto;
            item.descuento = itemProforma.descuento;
            item.nuevoPrecioVenta = itemProforma.nuevoPrecioVenta;
            item.precioUnitario = itemProforma.precioUnitario;
            item.tipoDescuento = itemProforma.tipoDescuento;
            item.porcentajeSugerido = itemProforma.porcentajeSugerido;
            item.observacion = itemProforma.observacion;

            return item;
          });
          this.recalcularTotal();
        }
      }, error => {
        swal.fire(`Error: ${error.error.status}`, `${error.error.message}`, 'error');
      }
    );
  }

  /** La mayorista abre las dos copias (cliente y tienda); la minorista, el único formato. */
  generarProformaPdf(id: number, ambasCopias = false): void {
    const copias: Array<'CLIENTE' | 'TIENDA'> = ambasCopias ? ['CLIENTE', 'TIENDA'] : ['CLIENTE'];
    forkJoin(copias.map(copia => this.proformaService.getProformaPdf(id, copia))).subscribe(respuestas => {
      respuestas.forEach(response => {
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
      });
      location.reload();
    },
      error => {
        this.mostrarErrorGeneracionPdf(error);
      });
  }

  private mostrarErrorGeneracionPdf(error: any): void {
    const mostrar = (detalle: any): void => {
      const estado = detalle && (detalle.status || detalle.code) || error.status || 500;
      const mensaje = detalle && detalle.message
        ? detalle.message
        : 'No fue posible generar el PDF de la proforma.';
      swal.fire(`Error ${estado}`, mensaje, 'error');
    };

    if (error && error.error instanceof Blob) {
      const reader = new FileReader();
      reader.onload = () => {
        try {
          mostrar(JSON.parse(reader.result as string));
        } catch (_) {
          mostrar({ message: reader.result as string });
        }
      };
      reader.onerror = () => mostrar(null);
      reader.readAsText(error.error);
      return;
    }

    mostrar(error && error.error);
  }

  update(): void {
    if (!this.cantidadesValidas()) {
      swal.fire('Cantidad Inválida', 'Todos los productos deben tener una cantidad entera mayor a 0.', 'warning');
      return;
    }

    if (!this.esProformaMayorista) {
      this.guardarCambios();
      return;
    }

    // Editar una proforma mayorista deja constancia: se pide el motivo y queda en su historial.
    swal.fire({
      title: 'Motivo del cambio',
      input: 'textarea',
      inputLabel: 'Queda en el historial de la proforma',
      inputAttributes: { maxlength: '500' },
      showCancelButton: true,
      confirmButtonText: 'Guardar cambios',
      cancelButtonText: 'Cancelar',
      inputValidator: valor => !valor || !valor.trim() ? 'Indica el motivo del cambio.' : null
    }).then(resultado => {
      if (resultado.isConfirmed) {
        this.guardarCambios(String(resultado.value).trim());
      }
    });
  }

  private guardarCambios(motivo?: string): void {
    this.isSaving = true;
    this.prepararMayorista();
    this.proformaService.update(this.proforma, motivo).subscribe(response => {
      this.generarProformaPdf(response.idProforma, this.esProformaMayorista);
      this.router.navigate(['/proformas/index']);
      swal.fire(response.mensaje, `Proforma ${response.noProforma} ha sido actualizada`, 'info');
    }, error => {
      this.isSaving = false;
      swal.fire('Error', `${error.error.message}`, 'error');
    });
  }
}
