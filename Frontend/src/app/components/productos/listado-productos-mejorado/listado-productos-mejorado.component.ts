import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { Producto } from '../../../models/producto';
import { ProductoDto } from '../../../dtos/productoDto';
import { Sucursal } from '../../../models/sucursal';

import { AuthService } from '../../../services/auth.service';
import { ProductoService } from '../../../services/producto.service';
import { ModalService } from '../../../services/productos/modal.service';
import { InventarioSucursalService } from '../../../services/inventario-sucursal.service';
import { SucursalService } from '../../../services/sucursal.service';
import { ExportacionProductos } from '../exportar-productos/exportar-productos.component';

import Swal from 'sweetalert2';

@Component({
  selector: 'app-listado-productos-mejorado',
  templateUrl: './listado-productos-mejorado.component.html',
  styleUrls: ['./listado-productos-mejorado.component.css']
})
export class ListadoProductosMejoradoComponent implements OnInit, OnDestroy {

  title: string;
  productosDto: ProductoDto[];

  public productoSeleccionado: Producto;

  // Paginación
  paginaActual: number = 0;
  totalPaginas: number = 0;
  totalElementos: number = 0;
  pageSize: number = 5;
  pageSizeOptions: number[] = [5, 10, 15, 25, 50];
  isFirst: boolean = true;
  isLast: boolean = false;

  // Búsqueda
  filtro: string = '';
  orden: string = 'nombre';
  direccion: 'asc' | 'desc' = 'asc';
  private busquedaSubject = new Subject<string>();
  private busquedaSubscription: Subscription;

  cargando: boolean = false;
  exportando: boolean = false;

  // Edición en línea de stock (sustituye a la pantalla independiente de Inventario por Sucursal)
  idSucursalActiva: number = null;
  nombreSucursalActiva: string = '';
  idProductoEditandoStock: number = null;
  stockEdicion: number = null;

  // Importar inventario cuando la sucursal activa todavía no tiene productos registrados
  sucursalesAdmin: Sucursal[] = [];
  idSucursalImportar: number = null;
  importando: boolean = false;

  // Exportar Excel de otra sucursal (solo ROLE_ADMIN)
  modalExportarVisible: boolean = false;

  constructor(
    public modalService: ModalService,
    private productoService: ProductoService,
    private inventarioSucursalService: InventarioSucursalService,
    private sucursalService: SucursalService,
    public auth: AuthService
  ) {
    this.title = 'Productos';
  }

  ngOnInit(): void {
    this.cargarProductos(0);
    this.resolverSucursalActiva();
    this.modalService.notificarUpload.subscribe(producto => {
      this.cargarProductos(this.paginaActual);
    });

    this.busquedaSubscription = this.busquedaSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(filtro => {
      this.filtro = filtro;
      this.cargarProductos(0);
    });

    if (this.auth.hasRole('ROLE_ADMIN')) {
      this.sucursalService.getSucursales().subscribe(sucursales => this.sucursalesAdmin = sucursales);
    }
  }

  private resolverSucursalActiva(): void {
    const sucursal = this.auth.usuario?.sucursal;
    if (sucursal) {
      this.idSucursalActiva = sucursal.idSucursal;
      this.nombreSucursalActiva = sucursal.nombre;
      return;
    }
    this.sucursalService.getPrincipal().subscribe(principal => {
      this.idSucursalActiva = principal.idSucursal;
      this.nombreSucursalActiva = principal.nombre;
    });
  }

  get sucursalesOrigenDisponibles(): Sucursal[] {
    return this.sucursalesAdmin.filter(s => s.idSucursal !== this.idSucursalActiva);
  }

  importarProductos(): void {
    if (!this.idSucursalImportar || !this.idSucursalActiva) {
      return;
    }

    this.importando = true;
    this.sucursalService.clonarInventario(this.idSucursalActiva, this.idSucursalImportar).subscribe(
      () => {
        this.importando = false;
        this.idSucursalImportar = null;
        Swal.fire('Inventario importado', 'El inventario se copi&oacute; correctamente a esta sucursal.', 'success');
        this.cargarProductos(0);
      },
      () => this.importando = false
    );
  }

  ngOnDestroy(): void {
    if (this.busquedaSubscription) {
      this.busquedaSubscription.unsubscribe();
    }
  }

  onBuscar(valor: string): void {
    this.busquedaSubject.next(valor);
  }

  cargarProductos(page: number): void {
    this.cargando = true;
    const request = this.filtro
      ? this.productoService.buscarProductosDto(page, this.filtro, this.pageSize, this.orden, this.direccion)
      : this.productoService.getProductosDtoPaginados(page, this.pageSize, this.orden, this.direccion);

    request.subscribe(
      response => {
        this.productosDto = response.content;
        this.paginaActual = response.number;
        this.totalPaginas = response.totalPages;
        this.totalElementos = response.totalElements;
        this.pageSize = response.size;
        this.isFirst = response.first;
        this.isLast = response.last;
        this.cargando = false;
      },
      error => {
        console.error(error);
        this.cargando = false;
        Swal.fire('Error al cargar productos', error.error?.message || 'Ha ocurrido un error inesperado', 'error');
      }
    );
  }

  irPrimeraPagina(): void {
    this.cargarProductos(0);
  }

  irUltimaPagina(): void {
    this.cargarProductos(this.totalPaginas - 1);
  }

  irPaginaAnterior(): void {
    if (!this.isFirst) {
      this.cargarProductos(this.paginaActual - 1);
    }
  }

  irPaginaSiguiente(): void {
    if (!this.isLast) {
      this.cargarProductos(this.paginaActual + 1);
    }
  }

  irAPagina(pagina: number): void {
    this.cargarProductos(pagina);
  }

  cambiarPageSize(nuevoSize: number): void {
    this.pageSize = nuevoSize;
    this.cargarProductos(0);
  }

  ordenarPor(campo: string): void {
    if (this.orden === campo) {
      this.direccion = this.direccion === 'asc' ? 'desc' : 'asc';
    } else {
      this.orden = campo;
      this.direccion = 'asc';
    }
    this.cargarProductos(0);
  }

  iconoOrden(campo: string): string {
    if (this.orden !== campo) {
      return 'fas fa-sort';
    }
    return this.direccion === 'asc' ? 'fas fa-sort-up' : 'fas fa-sort-down';
  }

  get paginasVisibles(): number[] {
    const paginas: number[] = [];
    const rango = 2;
    let inicio = Math.max(0, this.paginaActual - rango);
    let fin = Math.min(this.totalPaginas - 1, this.paginaActual + rango);

    if (this.paginaActual - rango < 0) {
      fin = Math.min(this.totalPaginas - 1, fin + (rango - this.paginaActual));
    }
    if (this.paginaActual + rango > this.totalPaginas - 1) {
      inicio = Math.max(0, inicio - (this.paginaActual + rango - (this.totalPaginas - 1)));
    }

    for (let i = inicio; i <= fin; i++) {
      paginas.push(i);
    }
    return paginas;
  }

  abrirModal(producto: any): void {
    this.productoSeleccionado = producto;
    this.modalService.abrirModal();
  }

  editarStock(producto: ProductoDto): void {
    this.idProductoEditandoStock = producto.idProducto;
    this.stockEdicion = producto.stock;
  }

  cancelarEdicionStock(): void {
    this.idProductoEditandoStock = null;
  }

  guardarStock(producto: ProductoDto): void {
    if (this.stockEdicion === null || this.stockEdicion < 0) {
      Swal.fire('Stock inv&aacute;lido', 'Ingresa una cantidad v&aacute;lida.', 'warning');
      return;
    }

    this.inventarioSucursalService.ajustarStock(this.idSucursalActiva, producto.idProducto, this.stockEdicion, null).subscribe(
      () => {
        producto.stock = this.stockEdicion;
        this.idProductoEditandoStock = null;
      }
    );
  }

  abrirModalExportar(): void {
    if (this.exportando) {
      return;
    }
    this.modalExportarVisible = true;
  }

  cerrarModalExportar(): void {
    this.modalExportarVisible = false;
  }

  confirmarExportacion(solicitud: ExportacionProductos): void {
    this.cerrarModalExportar();
    this.exportarExcel(solicitud.idSucursal);
  }

  private exportarExcel(idSucursal: number): void {
    this.exportando = true;
    this.productoService.exportarProductosExcel(idSucursal).subscribe(
      response => {
        const disposition = response.headers.get('content-disposition');
        const filenameMatch = disposition && disposition.match(/filename="?([^";]+)"?/i);
        const filename = filenameMatch ? filenameMatch[1] : 'productos.xlsx';
        const url = window.URL.createObjectURL(response.body);
        const link = document.createElement('a');

        link.href = url;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
        this.exportando = false;
      },
      error => {
        console.error(error);
        this.exportando = false;
        Swal.fire(
          'Error al exportar productos',
          error.error?.mensaje || 'No fue posible generar el archivo Excel',
          'error'
        );
      }
    );
  }

}
