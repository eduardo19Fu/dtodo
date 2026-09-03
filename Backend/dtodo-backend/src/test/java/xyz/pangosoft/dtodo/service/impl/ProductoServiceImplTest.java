package xyz.pangosoft.dtodo.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import javax.sql.DataSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.repository.IProductoRepository;
import xyz.pangosoft.dtodo.service.IEstadoService;
import xyz.pangosoft.dtodo.service.IInventarioSucursalService;
import xyz.pangosoft.dtodo.service.IUploadFileService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductoServiceImplTest {

    @Test
    void findAllDtoMejoradoFiltraPorLaSucursalIndicada() {
        IProductoRepository repository = mock(IProductoRepository.class);
        ProductoServiceImpl service = new ProductoServiceImpl(
                repository, mock(IUploadFileService.class), mock(IEstadoService.class),
                mock(IInventarioSucursalService.class), mock(DataSource.class));
        Page<Object[]> paginaVacia = new PageImpl<>(new ArrayList<>());
        when(repository.findAllProductosDto(eq("nombre"), eq("asc"), eq(3), any())).thenReturn(paginaVacia);

        Page<?> resultado = service.findAllDtoMejorado("nombre", "asc", 3, PageRequest.of(0, 5));

        verify(repository).findAllProductosDto(eq("nombre"), eq("asc"), eq(3), any());
        assertEquals(0, resultado.getTotalElements());
    }

    @Test
    void searchProductoDtoMejoradoFiltraPorLaSucursalIndicada() {
        IProductoRepository repository = mock(IProductoRepository.class);
        ProductoServiceImpl service = new ProductoServiceImpl(
                repository, mock(IUploadFileService.class), mock(IEstadoService.class),
                mock(IInventarioSucursalService.class), mock(DataSource.class));
        when(repository.findAll(
                org.mockito.ArgumentMatchers.<Specification<Producto>>any(),
                org.mockito.ArgumentMatchers.any(PageRequest.class)))
                .thenReturn(new PageImpl<>(new ArrayList<>()));

        Page<?> resultado = service.searchProductoDtoMejorado("abaco", "nombre", "asc", 3, PageRequest.of(0, 5));

        assertEquals(0, resultado.getTotalElements());
    }

    @Test
    void separaLosTerminosSinImponerUnaFraseContinua() {
        assertEquals(Arrays.asList("eclipse", "resma"),
                ProductoServiceImpl.obtenerTerminosBusqueda("  Eclipse   resma  "));

        assertEquals(
                new HashSet<>(ProductoServiceImpl.obtenerTerminosBusqueda("Eclipse resma")),
                new HashSet<>(ProductoServiceImpl.obtenerTerminosBusqueda("resma Eclipse"))
        );
    }

    @Test
    void eliminaTerminosDuplicados() {
        assertEquals(Arrays.asList("resma", "eclipse"),
                ProductoServiceImpl.obtenerTerminosBusqueda("Resma RESMA Eclipse"));
    }

    @Test
    void aceptaFiltroVacioONulo() {
        assertEquals(Collections.emptyList(), ProductoServiceImpl.obtenerTerminosBusqueda("   "));
        assertEquals(Collections.emptyList(), ProductoServiceImpl.obtenerTerminosBusqueda(null));
    }
}
