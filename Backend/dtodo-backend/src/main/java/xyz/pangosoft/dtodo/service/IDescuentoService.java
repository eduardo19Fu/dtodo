package xyz.pangosoft.dtodo.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import xyz.pangosoft.dtodo.dto.CategoriaNivelesDto;
import xyz.pangosoft.dtodo.dto.GuardarDescuentosProductoRequest;
import xyz.pangosoft.dtodo.dto.OpcionesDescuentoDto;
import xyz.pangosoft.dtodo.dto.ProductoDescuentosDto;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.enums.CategoriaClienteEnum;

/**
 * Descuentos de los clientes mayoristas. El usuario elige el tipo de descuento de cada línea (categoría, ciento,
 * fardo, precio mayorista o manual); el sistema calcula las opciones, sugiere una y verifica que lo elegido
 * coincida con las reglas configuradas.
 */
public interface IDescuentoService {

    /**
     * Opciones de descuento de un producto para un cliente y una cantidad. Con un cliente que no es mayorista
     * (o sin cliente) solo se ofrece el descuento manual.
     */
    OpcionesDescuentoDto calcularOpciones(Integer idProducto, Integer idCliente, int cantidad);

    List<CategoriaNivelesDto> findCategorias();

    /** Reemplaza los porcentajes activos de una categoría: los que no vengan quedan inactivos. */
    CategoriaNivelesDto guardarNiveles(CategoriaClienteEnum categoria, List<BigDecimal> porcentajes);

    Page<ProductoDescuentosDto> findProductos(String filtro, boolean soloConDescuentos, Pageable pageable);

    /** Marca en cada producto del listado si tiene precio mayorista o descuentos por ciento o por fardo. */
    void completarResumenMayorista(java.util.List<xyz.pangosoft.dtodo.dto.ProductoDtoMejorado> productos);

    ProductoDescuentosDto findProducto(Integer idProducto);

    /** Guarda el precio mayorista y los descuentos especiales de un producto (reemplaza los anteriores). */
    ProductoDescuentosDto guardarProducto(Integer idProducto, GuardarDescuentosProductoRequest solicitud);

    /**
     * Verifica y completa las líneas de una proforma antes de guardarla: el tipo de descuento de cada línea debe
     * coincidir con su porcentaje (422 si no), y se registran el precio unitario de ese momento y el porcentaje
     * que sugería el sistema. Una línea sin tipo se toma como descuento manual.
     */
    void prepararLineas(Proforma proforma);
}
