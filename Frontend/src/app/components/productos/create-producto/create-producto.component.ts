import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { MarcaProductoService } from 'src/app/services/marca-producto.service';
import { ProductoService } from 'src/app/services/producto.service';
import { TipoProductoService } from 'src/app/services/tipo-producto.service';
import { SucursalService } from 'src/app/services/sucursal.service';
import { AuthService } from 'src/app/services/auth.service';
import { DescuentoService } from 'src/app/services/descuento.service';

import { DescuentoProducto, GuardarDescuentosProducto, ProductoDescuentos } from 'src/app/dtos/descuento-dto';

import { MarcaProducto } from 'src/app/models/marca-producto';
import { Producto } from 'src/app/models/producto';
import { TipoProducto } from 'src/app/models/tipo-producto';
import { Sucursal } from 'src/app/models/sucursal';
import { SucursalStock } from 'src/app/models/sucursal-stock';

import swal from 'sweetalert2';

/** Fila del selector de sucursales al registrar un producto nuevo. */
interface SeleccionSucursal {
  sucursal: Sucursal;
  seleccionada: boolean;
  stock: number;
}

/** Una regla de descuento mayorista por cantidad (por ciento o fardo cerrado) tal como se edita en el formulario. */
export interface ReglaMayorista {
  activo: boolean;
  cantidadMinima: number | null;
  porcentaje: number | null;
}

@Component({
  selector: 'app-create-producto',
  templateUrl: './create-producto.component.html',
  styleUrls: ['./create-producto.component.css']
})
export class CreateProductoComponent implements OnInit {

  title: string;
  producto: Producto;

  tipos: TipoProducto[];
  marcas: MarcaProducto[];

  // Selección de sucursales donde sembrar el producto nuevo (solo aplica al registrar, no al editar)
  seleccionSucursales: SeleccionSucursal[] = [];

  // Precio y descuentos mayoristas: opcionales y solo del administrador (el servidor lo exige).
  precioMayorista: number | null = null;
  ciento: ReglaMayorista = { activo: false, cantidadMinima: 100, porcentaje: null };
  fardo: ReglaMayorista = { activo: false, cantidadMinima: null, porcentaje: null };
  cargandoMayorista = false;
  errorMayorista = false;
  /** Falso mientras se cargan o si no se pudieron cargar: así no se sobrescribe lo que el producto ya tiene. */
  mayoristaDisponible = true;
  private mayoristaOriginal = '';

  constructor(
    private serviceMarca: MarcaProductoService,
    private serviceTipo: TipoProductoService,
    private serviceProducto: ProductoService,
    private serviceSucursal: SucursalService,
    private auth: AuthService,
    private descuentoService: DescuentoService,
    private router: Router,
    private activatedRoute: ActivatedRoute
  ) {
    this.title = 'Registro de Productos';
    this.producto = new Producto();
  }

  ngOnInit(): void {
    // eslint-disable-next-line import/no-deprecated
    this.activatedRoute.params.subscribe(params => {
      // eslint-disable-next-line @typescript-eslint/dot-notation
      const id = params['id'];

      if (id) {
        // eslint-disable-next-line import/no-deprecated
        this.serviceProducto.getProducto(id).subscribe(
          producto => this.producto = producto
        );
        this.cargarMayorista(Number(id));
      } else {
        this.cargarSucursales();
        this.mayoristaOriginal = this.firmaMayorista();
      }
    });
    this.cargarMarcas();
    this.cargarTipos();
  }

  private cargarSucursales(): void {
    this.serviceSucursal.getSucursales().subscribe(sucursales => {
      const idSucursalActiva = this.auth.usuario?.sucursal?.idSucursal;
      this.seleccionSucursales = sucursales.map(sucursal => ({
        sucursal,
        seleccionada: sucursal.idSucursal === idSucursalActiva,
        stock: null
      }));

      // Si el usuario no tiene sucursal asignada (ej. sin sesión con sucursal), preselecciona la principal.
      if (!this.seleccionSucursales.some(s => s.seleccionada)) {
        const principal = this.seleccionSucursales.find(s => s.sucursal.esPrincipal);
        if (principal) {
          principal.seleccionada = true;
        }
      }
    });
  }

  cargarProducto(): void {
    // eslint-disable-next-line import/no-deprecated
    this.activatedRoute.params.subscribe(params => {
      // eslint-disable-next-line @typescript-eslint/dot-notation
      const id = params['id'];

      if (id) {
        // eslint-disable-next-line import/no-deprecated
        this.serviceProducto.getProducto(id).subscribe(
          producto => this.producto = producto
        );
      }
    });
  }

  cargarMarcas(): void {
    // eslint-disable-next-line import/no-deprecated
    this.serviceMarca.getMarcas().subscribe(marcas => this.marcas = marcas);
  }

  cargarTipos(): void {
    // eslint-disable-next-line import/no-deprecated
    this.serviceTipo.getTiposProducto().subscribe(tipos => this.tipos = tipos);
  }

  create(): void {
    // this.producto.porcentajeGanancia = Number.parseFloat((document.getElementById('porcentaje-ganancia') as HTMLInputElement).value);
    this.producto.precioVenta = Number.parseFloat((document.getElementById('precio-venta') as HTMLInputElement).value);

    const sucursales = this.sucursalesSeleccionadasParaEnvio();
    if (sucursales.length === 0) {
      swal.fire('Selecciona al menos una sucursal', 'Elige en qué sucursal(es) va a estar disponible el producto.', 'warning');
      return;
    }

    if (!this.producto.codProducto) {
      this.producto.codProducto = this.producto.generarCodigo();
    }

    const problema = this.errorMayoristaAlGuardar();
    if (problema) {
      swal.fire('Revisa el precio y los descuentos mayoristas', problema, 'warning');
      return;
    }

    this.serviceProducto.create(this.producto, sucursales).subscribe(
      producto => this.terminarGuardado(producto, 'Producto Guardado', 'registrado')
    );
  }

  private sucursalesSeleccionadasParaEnvio(): SucursalStock[] {
    return this.seleccionSucursales
      .filter(s => s.seleccionada)
      .map(s => ({ idSucursal: s.sucursal.idSucursal, stock: s.stock || 0 }));
  }

  get algunaSucursalSeleccionada(): boolean {
    return this.seleccionSucursales.some(s => s.seleccionada);
  }

  update(): void {
    // this.producto.porcentajeGanancia = Number.parseFloat((document.getElementById('porcentaje-ganancia') as HTMLInputElement).value);
    this.producto.precioVenta = Number.parseFloat((document.getElementById('precio-venta') as HTMLInputElement).value);
    const problema = this.errorMayoristaAlGuardar();
    if (problema) {
      swal.fire('Revisa el precio y los descuentos mayoristas', problema, 'warning');
      return;
    }

    this.serviceProducto.update(this.producto).subscribe(
      producto => this.terminarGuardado(producto, 'Producto Actualizado', 'actualizado')
    );
  }

  // ---- Precio y descuentos mayoristas

  /** Solo el administrador configura precios y descuentos mayoristas. */
  get puedeConfigurarMayoristas(): boolean {
    return this.auth.hasRole('ROLE_ADMIN');
  }

  private cargarMayorista(idProducto: number): void {
    if (!this.puedeConfigurarMayoristas) {
      return;
    }
    this.cargandoMayorista = true;
    this.errorMayorista = false;
    this.mayoristaDisponible = false;
    this.descuentoService.getProducto(idProducto).subscribe(datos => {
      this.aplicarMayorista(datos);
      this.cargandoMayorista = false;
      this.mayoristaDisponible = true;
    }, () => {
      this.cargandoMayorista = false;
      this.errorMayorista = true;
    });
  }

  reintentarMayorista(): void {
    if (this.producto.idProducto) {
      this.cargarMayorista(this.producto.idProducto);
    }
  }

  private aplicarMayorista(datos: ProductoDescuentos): void {
    this.precioMayorista = datos.precioMayorista;
    for (const [tipo, regla] of [['CIENTO', this.ciento], ['FARDO', this.fardo]] as const) {
      const existente = (datos.descuentos || []).find(descuento => descuento.tipo === tipo);
      regla.activo = !!existente;
      regla.cantidadMinima = existente ? existente.cantidadMinima : (tipo === 'CIENTO' ? 100 : null);
      regla.porcentaje = existente ? existente.porcentaje : null;
    }
    this.mayoristaOriginal = this.firmaMayorista();
  }

  /** Lo que se mandaría al servidor, con los campos vacíos como nulos. */
  solicitudMayorista(): GuardarDescuentosProducto {
    const descuentos: DescuentoProducto[] = [];
    for (const [tipo, regla] of [['CIENTO', this.ciento], ['FARDO', this.fardo]] as const) {
      if (regla.activo) {
        const descuento = new DescuentoProducto();
        descuento.tipo = tipo;
        descuento.cantidadMinima = Number(regla.cantidadMinima);
        descuento.porcentaje = Number(regla.porcentaje);
        descuentos.push(descuento);
      }
    }
    return { precioMayorista: this.numeroOnulo(this.precioMayorista), descuentos };
  }

  private numeroOnulo(valor: any): number | null {
    return valor === null || valor === undefined || valor === '' ? null : Number(valor);
  }

  private firmaMayorista(): string {
    return JSON.stringify(this.solicitudMayorista());
  }

  get hayCambiosMayorista(): boolean {
    return this.firmaMayorista() !== this.mayoristaOriginal;
  }

  /** El precio de venta que tiene el formulario ahora (se calcula con la ganancia y se lee del campo). */
  get precioVentaActual(): number | null {
    const campo = typeof document !== 'undefined' ? document.getElementById('precio-venta') as HTMLInputElement : null;
    const valor = campo && campo.value !== '' ? Number(campo.value) : this.producto.precioVenta;
    return valor && !isNaN(Number(valor)) ? Number(valor) : null;
  }

  /** El descuento que representa el precio mayorista frente al precio de venta, en porcentaje. */
  get equivalenciaMayorista(): number | null {
    const precio = this.numeroOnulo(this.precioMayorista);
    const venta = this.precioVentaActual;
    if (precio === null || !venta || precio <= 0 || precio >= venta) {
      return null;
    }
    return Math.round((1 - precio / venta) * 10000) / 100;
  }

  get precioMayoristaInvalido(): boolean {
    const precio = this.numeroOnulo(this.precioMayorista);
    const venta = this.precioVentaActual;
    return precio !== null && (precio <= 0 || (!!venta && precio >= venta));
  }

  /** Por qué no se puede guardar el precio o los descuentos mayoristas, o texto vacío si están bien. */
  errorMayoristaAlGuardar(): string {
    if (!this.puedeConfigurarMayoristas || !this.mayoristaDisponible) {
      return '';
    }
    const precio = this.numeroOnulo(this.precioMayorista);
    const venta = this.precioVentaActual;
    if (precio !== null) {
      if (!(precio > 0)) {
        return 'El precio mayorista debe ser mayor a cero.';
      }
      if (venta && precio >= venta) {
        return `El precio mayorista (Q${precio.toFixed(2)}) debe ser menor al precio de venta (Q${venta.toFixed(2)}).`;
      }
    }
    for (const [nombre, regla] of [['por ciento', this.ciento], ['por fardo cerrado', this.fardo]] as const) {
      if (!regla.activo) {
        continue;
      }
      const minimo = Number(regla.cantidadMinima);
      if (!Number.isInteger(minimo) || minimo < 2) {
        return `La cantidad mínima del descuento ${nombre} debe ser un número entero de 2 o más.`;
      }
      const porcentaje = Number(regla.porcentaje);
      if (!(porcentaje > 0) || porcentaje > 100) {
        return `El porcentaje del descuento ${nombre} debe estar entre 0.01 y 100.`;
      }
    }
    return '';
  }

  /**
   * Cierra el guardado del producto: si el administrador cambió el precio o los descuentos mayoristas se guardan ahora, porque
   * el servidor compara el precio mayorista con el precio de venta ya guardado. Si fallan, el producto queda guardado y se avisa.
   */
  private terminarGuardado(producto: Producto, titulo: string, accion: string): void {
    const mensaje = `El producto ${producto.nombre} ha sido ${accion} con éxito`;
    const quiereGuardar = this.puedeConfigurarMayoristas && this.mayoristaDisponible && this.hayCambiosMayorista;
    if (!quiereGuardar) {
      this.router.navigate(['/productos/index']);
      swal.fire(titulo, mensaje, 'success');
      return;
    }
    this.descuentoService.guardarProducto(producto.idProducto, this.solicitudMayorista()).subscribe(() => {
      this.router.navigate(['/productos/index']);
      swal.fire(titulo, `${mensaje}, junto con su precio y descuentos mayoristas.`, 'success');
    }, error => {
      this.router.navigate(['/productos/index']);
      const motivo = error?.error?.message || error?.error?.mensaje || 'Ha ocurrido un error inesperado.';
      swal.fire(titulo, `${mensaje}, pero no se pudieron guardar el precio y los descuentos mayoristas: ${motivo} ` +
        'Corrígelos en Mayoristas → Descuentos.', 'warning');
    });
  }

  // Comparar para reemplazar el valor en el select del formulario en caso de existir
  compararMarca(o1: MarcaProducto, o2: MarcaProducto): boolean {
    if (o1 === undefined && o2 === undefined) {
      return true;
    }
    return o1 === null || o2 === null || o1 === undefined || o2 === undefined ? false : o1.idMarcaProducto === o2.idMarcaProducto;
  }

  compararTipo(o1: TipoProducto, o2: TipoProducto): boolean {
    if (o1 === undefined && o2 === undefined) {
      return true;
    }
    return o1 == null || o2 == null || o1 === undefined || o2 === undefined ? false : o1.idTipoProducto === o2.idTipoProducto;
  }

  calcularPorcentajeGanancia(): void {
    const pcompra = ((document.getElementById('precio-compra') as HTMLInputElement).value);
    const pventa = (document.getElementById('precio-venta') as HTMLInputElement).value;
    let porcentaje = 0;

    if (!pcompra || !pventa) {
      console.log('valores incorrectos');
    } else {
      porcentaje = ((Number.parseFloat(pventa) - Number.parseFloat(pcompra)) / Number.parseFloat(pcompra)) * 100;
    }

    (document.getElementById('porcentaje-ganancia') as HTMLInputElement).value = porcentaje.toString();
  }

  calcularPrecioVenta(): void {
    const pcompra = ((document.getElementById('precio-compra') as HTMLInputElement).value);
    const pporcentaje = ((document.getElementById('porcentaje-ganancia') as HTMLInputElement).value);

    let precioVenta = 0;

    if (!pcompra || !pporcentaje) {
      console.log('valores incorrectos');
    } else {
      precioVenta = ((Number.parseFloat(pcompra) + ((Number.parseFloat(pporcentaje) / 100) * Number.parseFloat(pcompra))));
      console.log(precioVenta);
    }

    (document.getElementById('precio-venta') as HTMLInputElement).value = precioVenta.toString();
  }

}
