package xyz.pangosoft.dtodo.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resultado de importar inventario a una bodega desde un archivo Excel.
 *
 * <p>La importación es atómica: si {@code errores} no está vacío no se aplicó ninguna fila.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportacionInventarioDto {
	private int filasLeidas;
	private int productosImportados;
	private long unidadesImportadas;
	private List<String> errores = new ArrayList<>();
}
