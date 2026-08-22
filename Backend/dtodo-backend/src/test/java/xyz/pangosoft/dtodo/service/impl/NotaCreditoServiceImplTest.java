package xyz.pangosoft.dtodo.service.impl;

import xyz.pangosoft.dtodo.dto.NotaCreditoDetalleDto;
import xyz.pangosoft.dtodo.dto.NotaCreditoDetalleItemDto;
import xyz.pangosoft.dtodo.dto.NotaCreditoDto;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.NotaCredito;
import xyz.pangosoft.dtodo.model.NotaCreditoDetalle;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.EstadoNotaCreditoEnum;
import xyz.pangosoft.dtodo.model.enums.TipoDocumentoOrigenEnum;
import xyz.pangosoft.dtodo.repository.INotaCreditoRepository;
import xyz.pangosoft.dtodo.service.IMovimientoProductoService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class NotaCreditoServiceImplTest {

    @Test
    void ordenaLasUltimasNotasAntesDePaginar() {
        INotaCreditoRepository repository = mock(INotaCreditoRepository.class);
        NotaCreditoServiceImpl service = new NotaCreditoServiceImpl(
                repository, mock(IMovimientoProductoService.class), mock(DataSource.class));
        NotaCreditoDto segunda = new NotaCreditoDto();
        segunda.setIdNotaCredito(20L);
        NotaCreditoDto primera = new NotaCreditoDto();
        primera.setIdNotaCredito(10L);
        when(repository.findUltimasAsDto(any()))
                .thenReturn(new ArrayList<>(Arrays.asList(segunda, primera)));

        Page<NotaCreditoDto> resultado = service.findUltimas("",
                PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "idNotaCredito")));

        assertEquals(10L, resultado.getContent().get(0).getIdNotaCredito());
        assertEquals(20L, resultado.getContent().get(1).getIdNotaCredito());
    }

    @Test
    void findDetalleMapeaSoloLosDatosNecesariosParaVisualizacion() {
        INotaCreditoRepository repository = mock(INotaCreditoRepository.class);
        NotaCreditoServiceImpl service = new NotaCreditoServiceImpl(
                repository, mock(IMovimientoProductoService.class), mock(DataSource.class));

        Producto producto = Producto.builder()
                .idProducto(21)
                .codProducto("ABC-21")
                .nombre("Producto de prueba")
                .precioCompra(new BigDecimal("10.00"))
                .precioVenta(new BigDecimal("15.00"))
                .build();

        NotaCreditoDetalle item = new NotaCreditoDetalle();
        item.setIdNotaDetalle(7);
        item.setProducto(producto);
        item.setCantidad(2);
        item.setDescuento(5);
        item.setSubTotal(new BigDecimal("30.00"));
        item.setSubTotalDescuento(new BigDecimal("28.50"));

        NotaCredito nota = new NotaCredito();
        nota.setIdNotaCredito(12L);
        nota.setCorrelativoFacturaSat("456789");
        nota.setSerieFacturaSat("SERIE-A");
        nota.setTipoDocumentoOrigen(TipoDocumentoOrigenEnum.FACTURA);
        nota.setTotal(new BigDecimal("28.50"));
        nota.setFechaCreacion(LocalDateTime.of(2026, 8, 20, 10, 30));
        nota.setFechaEntregaEstimada(LocalDate.of(2026, 8, 25));
        nota.setEstado(EstadoNotaCreditoEnum.ENTREGA_PENDIENTE);
        nota.setCliente(Cliente.builder().idCliente(4).nombre("Cliente de prueba").nit("1234").build());
        nota.setUsuario(Usuario.builder().idUsuario(8).usuario("vendedor").password("secreto").build());
        nota.setItems(Collections.singletonList(item));

        when(repository.findById(12L)).thenReturn(Optional.of(nota));

        NotaCreditoDetalleDto resultado = service.findDetalle(12L);

        assertEquals(12L, resultado.getIdNotaCredito());
        assertEquals("Cliente de prueba", resultado.getCliente());
        assertEquals("vendedor", resultado.getVendedor());
        assertEquals(1, resultado.getItems().size());

        NotaCreditoDetalleItemDto itemResultado = resultado.getItems().get(0);
        assertEquals(21, itemResultado.getIdProducto());
        assertEquals("ABC-21", itemResultado.getCodProducto());
        assertEquals("Producto de prueba", itemResultado.getProducto());
        assertEquals(new BigDecimal("28.50"), itemResultado.getSubTotalDescuento());
    }
}
