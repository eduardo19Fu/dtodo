import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';

import { routing, appRoutingProviders } from './app.routing';
import { TokenInterceptor } from './components/usuarios/interceptors/token.interceptor';
import { AuthInterceptor } from './components/usuarios/interceptors/auth.interceptor';

import { AppComponent } from './app.component';
import { LoginComponent } from './components/login/login.component';
import { SidebarComponent } from './components/sidebar/sidebar.component';
import { HeaderComponent } from './components/header/header.component';
import { HomeComponent } from './components/home/home.component';
import { FooterComponent } from './components/footer/footer.component';
import { ClientesComponent } from './components/clientes/clientes.component';
import { CreateClienteComponent } from './components/clientes/create-cliente/create-cliente.component';
import { ModalCreateComponent } from './components/clientes/modal-create/modal-create.component';
import { CorrelativosComponent } from './components/correlativos/correlativos.component';
import { CreateCorrelativoComponent } from './components/correlativos/create-correlativo/create-correlativo.component';
import { FacturasComponent } from './components/facturas/facturas.component';
import { CreateFacturaComponent } from './components/facturas/create-factura/create-factura.component';
import { DetailFacturaComponent } from './components/facturas/detail/detail-factura.component';
import { MarcasProductoComponent } from './components/marcas-producto/marcas-producto.component';
import { CreateMarcaComponent } from './components/marcas-producto/create-marca/create-marca.component';
import { ProductosComponent } from './components/productos/productos.component';
import { CreateProductoComponent } from './components/productos/create-producto/create-producto.component';
import { DetailProductoComponent } from './components/productos/detail-producto/detail-producto.component';
import { TiposProductoComponent } from './components/tipos-producto/tipos-producto.component';
import { CreateTipoComponent } from './components/tipos-producto/create-producto/create-tipo.component';
import { UsuariosComponent } from './components/usuarios/usuarios.component';
import { CreateUsuarioComponent } from './components/usuarios/create-usuario/create-usuario.component';
import { DetailUsuarioComponent } from './components/usuarios/detail-usuario/detail-usuario.component';
import { ErrorComponent } from './components/error/error.component';
import { MovimientosProductoComponent } from './components/movimientos-producto/movimientos-producto.component';
import { CreateMovimientoComponent } from './components/movimientos-producto/create-movimiento/create-movimiento.component';
import { BusquedaMovimientosComponent } from './components/movimientos-producto/busqueda-movimientos/busqueda-movimientos.component';
import { PolizaIndividualComponent } from './components/facturas/poliza-individual/poliza-individual.component';
import { SearchProductModalComponent } from './components/productos/search-product-modal/search-product-modal.component';
import { HeaderVentasComponent } from './components/header-ventas/header-ventas.component';
import { ModalCambioComponent } from './components/facturas/create-factura/modal-cambio/modal-cambio.component';
import { ModalBuscarProductoComponent } from './components/facturas/create-factura/modal-buscar-producto/modal-buscar-producto.component';
import { ModalBuscarClienteComponent } from './components/facturas/create-factura/modal-buscar-cliente/modal-buscar-cliente.component';
import { ModalBuscarProductoMovimientoComponent } from './components/movimientos-producto/create-movimiento/modal-buscar-producto-movimiento/modal-buscar-producto-movimiento.component';
import { ProformasComponent } from './components/proformas/proformas.component';
import { DetailProformaComponent } from './components/proformas/detail-proforma/detail-proforma.component';
import { CreateProformaComponent } from './components/proformas/create-proforma/create-proforma.component';
import { NotasCreditoComponent } from './components/notas-credito/notas-credito.component';
import { CreateNotaComponent } from './components/notas-credito/create-nota/create-nota.component';
import { DetailNotaComponent } from './components/notas-credito/detail-nota/detail-nota.component';
import { ModalNotasCreditoComponent } from './components/notas-credito/modal-notas-credito/modal-notas-credito.component';

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    SidebarComponent,
    HeaderComponent,
    HomeComponent,
    FooterComponent,
    ClientesComponent,
    CreateClienteComponent,
    ModalCreateComponent,
    CorrelativosComponent,
    CreateCorrelativoComponent,
    FacturasComponent,
    CreateFacturaComponent,
    DetailFacturaComponent,
    MarcasProductoComponent,
    CreateMarcaComponent,
    ProductosComponent,
    CreateProductoComponent,
    DetailProductoComponent,
    TiposProductoComponent,
    CreateTipoComponent,
    UsuariosComponent,
    CreateUsuarioComponent,
    DetailUsuarioComponent,
    ErrorComponent,
    MovimientosProductoComponent,
    CreateMovimientoComponent,
    BusquedaMovimientosComponent,
    PolizaIndividualComponent,
    SearchProductModalComponent,
    HeaderVentasComponent,
    ModalCambioComponent,
    ModalBuscarProductoComponent,
    ModalBuscarClienteComponent,
    ModalBuscarProductoMovimientoComponent,
    ProformasComponent,
    DetailProformaComponent,
    CreateProformaComponent,
    NotasCreditoComponent,
    CreateNotaComponent,
    DetailNotaComponent,
    ModalNotasCreditoComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    FormsModule,
    routing
  ],
  providers: [
    appRoutingProviders,
    { provide: HTTP_INTERCEPTORS, useClass: TokenInterceptor, multi: true },
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
