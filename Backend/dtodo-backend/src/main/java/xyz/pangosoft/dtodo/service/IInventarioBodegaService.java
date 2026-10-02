package xyz.pangosoft.dtodo.service;

import java.io.InputStream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import xyz.pangosoft.dtodo.dto.ImportacionInventarioDto;
import xyz.pangosoft.dtodo.dto.InventarioBodegaDto;
import xyz.pangosoft.dtodo.dto.MovimientoBodegaDto;
import xyz.pangosoft.dtodo.dto.MovimientoBodegaRequest;
import xyz.pangosoft.dtodo.model.Bodega;
import xyz.pangosoft.dtodo.model.MovimientoBodega;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.OrigenInventarioBodegaEnum;
import xyz.pangosoft.dtodo.model.enums.TipoMovimientoBodegaEnum;

public interface IInventarioBodegaService {

	// Devuelve el listado paginado de existencias de una bodega
	public Page<InventarioBodegaDto> findListado(Integer idBodega, String filtro, Pageable pageable);

	// Busca un producto por su código dentro del inventario de la bodega o lanza NotFoundException
	public InventarioBodegaDto findPorCodigo(Integer idBodega, String codigo);

	// Devuelve el listado paginado de movimientos de una bodega; las fechas (yyyy-MM-dd) y el tipo son opcionales
	public Page<MovimientoBodegaDto> findMovimientos(Integer idBodega, String fechaIni, String fechaFin,
			TipoMovimientoBodegaEnum tipo, String filtro, Pageable pageable);

	// Movimiento "Agregar producto": suma existencias (crea la fila de inventario si el producto aún no está en la bodega)
	public MovimientoBodega agregarProducto(Integer idBodega, MovimientoBodegaRequest request, Integer idUsuario);

	// Movimiento "Reducir existencias": resta unidades sin dejar el stock en negativo
	public MovimientoBodega reducirExistencias(Integer idBodega, MovimientoBodegaRequest request, Integer idUsuario);

	// Movimiento "Eliminar producto": descuenta toda la existencia y retira el producto del inventario de la bodega
	public MovimientoBodega eliminarProducto(Integer idBodega, Integer idProducto, String motivo, Integer idUsuario);

	// Bloquea la fila y devuelve la existencia actual de un producto en la bodega (0 si no tiene fila de inventario)
	public int obtenerStockParaActualizar(Bodega bodega, Producto producto);

	// Registra un movimiento de existencias con su trazabilidad; lo usan los despachos de bodega
	public MovimientoBodega registrarMovimiento(Bodega bodega, Producto producto, TipoMovimientoBodegaEnum tipo,
			int cantidad, String motivo, Usuario usuario, String tipoDocumentoOrigen, Long idDocumentoOrigen);

	// Copia a una bodega vacía el inventario de una sucursal u otra bodega
	public int clonarInventario(Integer idBodegaDestino, OrigenInventarioBodegaEnum origen, Integer idOrigen,
			Integer idUsuario);

	// Importa existencias desde un archivo Excel (.xlsx); es atómica: si hay errores no aplica ninguna fila
	public ImportacionInventarioDto importarDesdeExcel(Integer idBodega, InputStream archivo, Integer idUsuario);

}
