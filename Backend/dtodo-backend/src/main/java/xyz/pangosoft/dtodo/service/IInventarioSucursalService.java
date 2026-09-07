package xyz.pangosoft.dtodo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import xyz.pangosoft.dtodo.dto.InventarioSucursalDto;
import xyz.pangosoft.dtodo.model.InventarioSucursal;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.Sucursal;

public interface IInventarioSucursalService {

	// Devuelve el listado paginado de existencias de una sucursal
	public Page<InventarioSucursalDto> findListado(Integer idSucursal, String filtro, Pageable pageable);

	// Devuelve la fila de inventario de un producto en una sucursal, creándola con stock 0 si aún no existe
	public InventarioSucursal obtenerOCrear(Sucursal sucursal, Producto producto);

	// Devuelve el stock actual de un producto en una sucursal (0 si no tiene fila de inventario registrada)
	public int obtenerStock(Integer idSucursal, Integer idProducto);

	// Persiste el nuevo stock de una fila de inventario (usado al registrar movimientos de producto)
	public InventarioSucursal guardar(InventarioSucursal inventarioSucursal);

	// Ajuste manual de existencias desde la pantalla de inventario por sucursal
	public InventarioSucursal ajustarStock(Integer idSucursal, Integer idProducto, Integer nuevoStock, Integer stockMinimo);

	// Copia el inventario completo de una sucursal origen hacia una sucursal destino recién creada
	public void clonarInventario(Integer idSucursalOrigen, Integer idSucursalDestino);

	// Cuenta cuántos productos tienen inventario importado/registrado en una sucursal
	public int contarPorSucursal(Integer idSucursal);

}
