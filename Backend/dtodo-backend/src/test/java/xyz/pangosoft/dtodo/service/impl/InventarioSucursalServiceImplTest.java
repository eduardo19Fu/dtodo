package xyz.pangosoft.dtodo.service.impl;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.model.InventarioSucursal;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.repository.IInventarioSucursalRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventarioSucursalServiceImplTest {

    private final IInventarioSucursalRepository repository = mock(IInventarioSucursalRepository.class);
    private final InventarioSucursalServiceImpl service = new InventarioSucursalServiceImpl(repository);

    @Test
    void obtenerOCrearDevuelveLaFilaExistenteSiYaHayInventario() {
        Sucursal sucursal = Sucursal.builder().idSucursal(1).build();
        Producto producto = Producto.builder().idProducto(5).build();
        InventarioSucursal existente = InventarioSucursal.builder().stock(20).build();
        when(repository.findBySucursal_IdSucursalAndProducto_IdProducto(1, 5))
                .thenReturn(Optional.of(existente));

        InventarioSucursal resultado = service.obtenerOCrear(sucursal, producto);

        assertEquals(existente, resultado);
        verify(repository, never()).save(any(InventarioSucursal.class));
    }

    @Test
    void obtenerOCrearCreaUnaFilaConStockCeroSiNoExiste() {
        Sucursal sucursal = Sucursal.builder().idSucursal(2).build();
        Producto producto = Producto.builder().idProducto(9).build();
        when(repository.findBySucursal_IdSucursalAndProducto_IdProducto(2, 9))
                .thenReturn(Optional.empty());
        when(repository.save(any(InventarioSucursal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InventarioSucursal resultado = service.obtenerOCrear(sucursal, producto);

        assertEquals(0, resultado.getStock());
        assertEquals(sucursal, resultado.getSucursal());
        assertEquals(producto, resultado.getProducto());
    }

    @Test
    void ajustarStockRechazaValoresNegativos() {
        assertThrows(BadRequestException.class, () -> service.ajustarStock(1, 5, -1, null));
    }

    @Test
    void ajustarStockLanzaNotFoundSiNoHayInventarioRegistrado() {
        when(repository.findBySucursal_IdSucursalAndProducto_IdProducto(1, 5)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.ajustarStock(1, 5, 10, null));
    }

    @Test
    void clonarInventarioRechazaSiLaSucursalDestinoYaTieneInventario() {
        when(repository.existsBySucursal_IdSucursal(2)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.clonarInventario(1, 2));
        verify(repository, never()).clonarInventario(any(), any());
    }

    @Test
    void clonarInventarioDelegaEnElRepositorioCuandoElDestinoEstaVacio() {
        when(repository.existsBySucursal_IdSucursal(2)).thenReturn(false);

        service.clonarInventario(1, 2);

        verify(repository).clonarInventario(1, 2);
    }
}
