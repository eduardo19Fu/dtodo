package xyz.pangosoft.dtodo.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import xyz.pangosoft.dtodo.dto.FacturaDto;
import xyz.pangosoft.dtodo.fel.IFelService;
import xyz.pangosoft.dtodo.fel.model.DatosEmisor;
import xyz.pangosoft.dtodo.model.Emisor;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.repository.IFacturaRepository;
import xyz.pangosoft.dtodo.repository.ITipoFacturaRepository;
import xyz.pangosoft.dtodo.service.ICertificadorService;
import xyz.pangosoft.dtodo.service.ICorrelativoService;
import xyz.pangosoft.dtodo.service.IEmisorService;
import xyz.pangosoft.dtodo.service.IEstadoService;
import xyz.pangosoft.dtodo.service.IMovimientoProductoService;
import xyz.pangosoft.dtodo.service.ITipoFacturaService;
import xyz.pangosoft.dtodo.service.IUsuarioService;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FacturaServiceImplTest {

    @Test
    void ordenaLasUltimasFacturasAntesDePaginar() {
        IFacturaRepository repository = mock(IFacturaRepository.class);
        FacturaServiceImpl service = new FacturaServiceImpl(
                repository,
                mock(ITipoFacturaRepository.class),
                mock(IEmisorService.class),
                mock(IEstadoService.class),
                mock(ITipoFacturaService.class),
                mock(ICorrelativoService.class),
                mock(ICertificadorService.class),
                mock(IMovimientoProductoService.class),
                mock(IUsuarioService.class),
                mock(IFelService.class),
                mock(DataSource.class)
        );
        FacturaDto segunda = FacturaDto.builder().noFactura(200L).build();
        FacturaDto primera = FacturaDto.builder().noFactura(100L).build();
        when(repository.findUltimasListadoDto(any()))
                .thenReturn(new ArrayList<>(Arrays.asList(segunda, primera)));

        Page<FacturaDto> resultado = service.findUltimasListadoDto("",
                PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "noFactura")));

        assertEquals(100L, resultado.getContent().get(0).getNoFactura());
        assertEquals(200L, resultado.getContent().get(1).getNoFactura());
    }

    @Test
    void totalVentasCuentaSoloLasDelUsuarioIndicado() {
        IFacturaRepository repository = mock(IFacturaRepository.class);
        FacturaServiceImpl service = new FacturaServiceImpl(
                repository, mock(ITipoFacturaRepository.class), mock(IEmisorService.class),
                mock(IEstadoService.class), mock(ITipoFacturaService.class), mock(ICorrelativoService.class),
                mock(ICertificadorService.class), mock(IMovimientoProductoService.class), mock(IUsuarioService.class),
                mock(IFelService.class), mock(DataSource.class));
        when(repository.getCantidadVentasPorUsuario(20)).thenReturn(3L);

        Integer total = service.totalVentas(20);

        assertEquals(3, total);
    }

    @Test
    void totalVentasSinUsuarioDevuelveElConteoGlobal() {
        IFacturaRepository repository = mock(IFacturaRepository.class);
        FacturaServiceImpl service = new FacturaServiceImpl(
                repository, mock(ITipoFacturaRepository.class), mock(IEmisorService.class),
                mock(IEstadoService.class), mock(ITipoFacturaService.class), mock(ICorrelativoService.class),
                mock(ICertificadorService.class), mock(IMovimientoProductoService.class), mock(IUsuarioService.class),
                mock(IFelService.class), mock(DataSource.class));
        when(repository.getCantidadVentas()).thenReturn(150);

        Integer total = service.totalVentas(null);

        assertEquals(150, total);
    }

    @Test
    void usaElCodigoDeEstablecimientoDeLaSucursalEnVezDeUnValorFijo() throws Exception {
        FacturaServiceImpl service = new FacturaServiceImpl(
                mock(IFacturaRepository.class), mock(ITipoFacturaRepository.class), mock(IEmisorService.class),
                mock(IEstadoService.class), mock(ITipoFacturaService.class), mock(ICorrelativoService.class),
                mock(ICertificadorService.class), mock(IMovimientoProductoService.class), mock(IUsuarioService.class),
                mock(IFelService.class), mock(DataSource.class));

        Emisor emisor = Emisor.builder().nit("12345678").nombreEmisor("Comercial De Todo").nombreComercial("De Todo").build();
        Sucursal sucursal = Sucursal.builder().idSucursal(2).nombre("Sucursal Norte").codigoEstablecimientoSat(3).build();

        Method metodo = FacturaServiceImpl.class.getDeclaredMethod("configurarDatosEmisor", Emisor.class, Sucursal.class);
        metodo.setAccessible(true);
        DatosEmisor datosEmisor = (DatosEmisor) metodo.invoke(service, emisor, sucursal);

        assertEquals(3, datosEmisor.getCodigoEstablecimiento());
    }

    @Test
    void usaCodigoDeEstablecimientoPorDefectoCuandoNoHaySucursal() throws Exception {
        FacturaServiceImpl service = new FacturaServiceImpl(
                mock(IFacturaRepository.class), mock(ITipoFacturaRepository.class), mock(IEmisorService.class),
                mock(IEstadoService.class), mock(ITipoFacturaService.class), mock(ICorrelativoService.class),
                mock(ICertificadorService.class), mock(IMovimientoProductoService.class), mock(IUsuarioService.class),
                mock(IFelService.class), mock(DataSource.class));

        Emisor emisor = Emisor.builder().nit("12345678").nombreEmisor("Comercial De Todo").nombreComercial("De Todo").build();

        Method metodo = FacturaServiceImpl.class.getDeclaredMethod("configurarDatosEmisor", Emisor.class, Sucursal.class);
        metodo.setAccessible(true);
        DatosEmisor datosEmisor = (DatosEmisor) metodo.invoke(service, emisor, null);

        assertEquals(1, datosEmisor.getCodigoEstablecimiento());
    }
}
