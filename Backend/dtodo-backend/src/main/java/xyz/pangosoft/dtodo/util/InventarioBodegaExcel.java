package xyz.pangosoft.dtodo.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;

/**
 * Lectura y plantilla del archivo Excel con el que se importa inventario a una bodega.
 *
 * <p>Formato esperado (primera hoja, fila 1 con encabezados, datos desde la fila 2):</p>
 * <pre>
 * | codigo_producto | cantidad | stock_minimo | nombre | precio_compra | precio_venta | marca | categoria |
 * | 7501234567890   | 25       | 5            |        |               |              |       |           |
 * </pre>
 * {@code codigo_producto} y {@code cantidad} son obligatorias; {@code cantidad} puede ser 0 (producto registrado sin
 * existencias). {@code stock_minimo} es opcional. Las demás columnas solo se usan cuando el código no existe en el
 * catálogo y el producto debe crearse ({@code nombre}, {@code precio_compra} y {@code precio_venta} son entonces
 * obligatorias; {@code marca} y {@code categoria} opcionales). Para un producto que ya existe se ignoran: el catálogo
 * no se modifica desde la importación. Las filas totalmente vacías se ignoran.
 */
public final class InventarioBodegaExcel {

	public static final int MAX_FILAS = 10000;

	static final String COLUMNA_CODIGO = "codigo_producto";
	static final String COLUMNA_CANTIDAD = "cantidad";
	static final String COLUMNA_STOCK_MINIMO = "stock_minimo";
	static final String COLUMNA_NOMBRE = "nombre";
	static final String COLUMNA_PRECIO_COMPRA = "precio_compra";
	static final String COLUMNA_PRECIO_VENTA = "precio_venta";
	static final String COLUMNA_MARCA = "marca";
	static final String COLUMNA_CATEGORIA = "categoria";

	private InventarioBodegaExcel() {
	}

	/**
	 * Datos del catálogo que trae una fila, tal como se escribieron (sin interpretar). Solo importan si el producto
	 * no existe; se validan en ese momento para no rechazar un archivo por columnas que se van a ignorar.
	 */
	public record DatosProducto(String nombre, String precioCompra, String precioVenta, String marca, String categoria) {

		public static final DatosProducto VACIOS = new DatosProducto("", "", "", "", "");
	}

	/** Una fila de datos ya validada en su forma (no en su existencia en el catálogo). */
	public record Fila(int numeroFila, String codigo, int cantidad, Integer stockMinimo, DatosProducto producto) {
	}

	/** Resultado de leer el archivo: filas válidas en forma y errores encontrados, con su número de fila de Excel. */
	public record Lectura(List<Fila> filas, List<String> errores, int filasLeidas) {
	}

	public static Lectura leer(InputStream contenido) {
		try (Workbook libro = new XSSFWorkbook(contenido)) {
			if (libro.getNumberOfSheets() == 0) {
				throw new BadRequestException("El archivo no contiene hojas.", null);
			}
			Sheet hoja = libro.getSheetAt(0);
			DataFormatter formato = new DataFormatter(Locale.ROOT);

			Row encabezado = hoja.getRow(hoja.getFirstRowNum());
			int colCodigo = -1;
			int colCantidad = -1;
			int colStockMinimo = -1;
			int colNombre = -1;
			int colPrecioCompra = -1;
			int colPrecioVenta = -1;
			int colMarca = -1;
			int colCategoria = -1;
			if (encabezado != null) {
				for (Cell celda : encabezado) {
					String nombre = normalizar(formato.formatCellValue(celda));
					if (COLUMNA_CODIGO.equals(nombre) || "codigo".equals(nombre)) {
						colCodigo = celda.getColumnIndex();
					} else if (COLUMNA_CANTIDAD.equals(nombre)) {
						colCantidad = celda.getColumnIndex();
					} else if (COLUMNA_STOCK_MINIMO.equals(nombre)) {
						colStockMinimo = celda.getColumnIndex();
					} else if (COLUMNA_NOMBRE.equals(nombre)) {
						colNombre = celda.getColumnIndex();
					} else if (COLUMNA_PRECIO_COMPRA.equals(nombre)) {
						colPrecioCompra = celda.getColumnIndex();
					} else if (COLUMNA_PRECIO_VENTA.equals(nombre)) {
						colPrecioVenta = celda.getColumnIndex();
					} else if (COLUMNA_MARCA.equals(nombre)) {
						colMarca = celda.getColumnIndex();
					} else if (COLUMNA_CATEGORIA.equals(nombre)) {
						colCategoria = celda.getColumnIndex();
					}
				}
			}
			if (colCodigo < 0 || colCantidad < 0) {
				throw new BadRequestException("El archivo debe tener los encabezados \"" + COLUMNA_CODIGO + "\" y \""
						+ COLUMNA_CANTIDAD + "\" en la primera fila. Descarga la plantilla para ver el formato.", null);
			}

			List<Fila> filas = new ArrayList<>();
			List<String> errores = new ArrayList<>();
			int leidas = 0;
			for (int i = hoja.getFirstRowNum() + 1; i <= hoja.getLastRowNum(); i++) {
				Row fila = hoja.getRow(i);
				String codigo = texto(fila, colCodigo, formato);
				String cantidadTexto = texto(fila, colCantidad, formato);
				String minimoTexto = texto(fila, colStockMinimo, formato);
				DatosProducto datos = new DatosProducto(texto(fila, colNombre, formato),
						texto(fila, colPrecioCompra, formato), texto(fila, colPrecioVenta, formato),
						texto(fila, colMarca, formato), texto(fila, colCategoria, formato));
				if (codigo.isEmpty() && cantidadTexto.isEmpty() && minimoTexto.isEmpty() && datos.equals(DatosProducto.VACIOS)) {
					continue;
				}
				if (++leidas > MAX_FILAS) {
					throw new BadRequestException("El archivo supera el máximo de " + MAX_FILAS + " filas por importación.", null);
				}

				int numero = i + 1;
				if (codigo.isEmpty()) {
					errores.add("Fila " + numero + ": falta el código del producto.");
					continue;
				}
				Integer cantidad = enteroNoNegativo(cantidadTexto);
				if (cantidad == null) {
					errores.add("Fila " + numero + ": la cantidad debe ser un número entero mayor o igual a 0 (se leyó \""
							+ cantidadTexto + "\").");
					continue;
				}
				Integer stockMinimo = null;
				if (!minimoTexto.isEmpty()) {
					stockMinimo = enteroNoNegativo(minimoTexto);
					if (stockMinimo == null) {
						errores.add("Fila " + numero + ": el stock mínimo debe ser un número entero mayor o igual a 0 (se leyó \""
								+ minimoTexto + "\").");
						continue;
					}
				}
				filas.add(new Fila(numero, codigo, cantidad, stockMinimo, datos));
			}
			return new Lectura(filas, errores, leidas);
		} catch (BadRequestException e) {
			throw e;
		} catch (IOException | RuntimeException e) {
			throw new BadRequestException("No se pudo leer el archivo. Verifica que sea un Excel (.xlsx) válido.", e);
		}
	}

	/**
	 * Interpreta un precio escrito en el archivo: acepta símbolo de moneda, espacios y separador de miles
	 * ("Q1,200.50"). Devuelve {@code null} si no es un número válido o es negativo.
	 */
	public static BigDecimal precio(String texto) {
		if (texto == null) {
			return null;
		}
		String limpio = texto.replaceAll("[^0-9.,-]", "");
		if (limpio.isEmpty()) {
			return null;
		}
		if (limpio.contains(".")) {
			limpio = limpio.replace(",", "");
		} else if (limpio.indexOf(',') >= 0 && limpio.indexOf(',') == limpio.lastIndexOf(',')
				&& limpio.length() - limpio.indexOf(',') - 1 <= 2) {
			limpio = limpio.replace(',', '.');
		} else {
			limpio = limpio.replace(",", "");
		}
		try {
			BigDecimal valor = new BigDecimal(limpio);
			return valor.signum() < 0 ? null : valor.setScale(2, RoundingMode.HALF_UP);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/** Plantilla descargable: una hoja con encabezados y filas de ejemplo, y otra con las instrucciones. */
	public static byte[] generarPlantilla() {
		try (Workbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
			Font negrita = libro.createFont();
			negrita.setBold(true);
			CellStyle estiloEncabezado = libro.createCellStyle();
			estiloEncabezado.setFont(negrita);
			estiloEncabezado.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
			estiloEncabezado.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
			CellStyle estiloOpcional = libro.createCellStyle();
			estiloOpcional.setFont(negrita);
			estiloOpcional.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
			estiloOpcional.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

			Sheet inventario = libro.createSheet("Inventario");
			Row encabezado = inventario.createRow(0);
			String[] titulos = { COLUMNA_CODIGO, COLUMNA_CANTIDAD, COLUMNA_STOCK_MINIMO, COLUMNA_NOMBRE,
					COLUMNA_PRECIO_COMPRA, COLUMNA_PRECIO_VENTA, COLUMNA_MARCA, COLUMNA_CATEGORIA };
			for (int i = 0; i < titulos.length; i++) {
				Cell celda = encabezado.createCell(i);
				celda.setCellValue(titulos[i]);
				// Azul: columnas del inventario. Amarillo: datos del catálogo, solo para productos que aún no existen
				celda.setCellStyle(i < 3 ? estiloEncabezado : estiloOpcional);
			}
			// Las primeras cuatro filas son de productos que ya existen; las dos últimas crean un producto nuevo
			String[][] ejemplos = {
					{ "EJEMPLO-001", "25", "5", "", "", "", "", "" },
					{ "EJEMPLO-002", "100", "", "", "", "", "", "" },
					{ "EJEMPLO-003", "8", "2", "", "", "", "", "" },
					{ "EJEMPLO-004", "0", "", "", "", "", "", "" },
					{ "NUEVO-001", "24", "5", "CUADERNO RAYADO 100 HOJAS", "8.50", "12.00", "MAPED", "BLOCK" },
					{ "NUEVO-002", "12", "", "LAPICERO AZUL", "3.25", "5.00", "", "" }
			};
			for (int i = 0; i < ejemplos.length; i++) {
				Row fila = inventario.createRow(i + 1);
				fila.createCell(0).setCellValue(ejemplos[i][0]);
				fila.createCell(1).setCellValue(Integer.parseInt(ejemplos[i][1]));
				if (!ejemplos[i][2].isEmpty()) {
					fila.createCell(2).setCellValue(Integer.parseInt(ejemplos[i][2]));
				}
				if (!ejemplos[i][3].isEmpty()) {
					fila.createCell(3).setCellValue(ejemplos[i][3]);
					fila.createCell(4).setCellValue(Double.parseDouble(ejemplos[i][4]));
					fila.createCell(5).setCellValue(Double.parseDouble(ejemplos[i][5]));
				}
				if (!ejemplos[i][6].isEmpty()) {
					fila.createCell(6).setCellValue(ejemplos[i][6]);
				}
				if (!ejemplos[i][7].isEmpty()) {
					fila.createCell(7).setCellValue(ejemplos[i][7]);
				}
			}
			for (int i = 0; i < titulos.length; i++) {
				inventario.setColumnWidth(i, (i == 3 ? 32 : 20) * 256);
			}

			Sheet instrucciones = libro.createSheet("Instrucciones");
			String[] lineas = {
					"Cómo llenar la hoja \"Inventario\"",
					"",
					"Columnas del inventario (encabezado azul):",
					"codigo_producto (obligatorio): código del producto. Si ya está en el catálogo se usa ese producto; si no, se crea.",
					"cantidad (obligatorio): unidades que ingresan a la bodega. Número entero mayor o igual a 0; usa 0 para registrar el producto sin existencias.",
					"stock_minimo (opcional): existencia mínima deseada en la bodega. Número entero mayor o igual a 0.",
					"",
					"Datos del catálogo (encabezado amarillo): solo para productos que NO existen todavía",
					"nombre (obligatorio al crear): nombre del producto.",
					"precio_compra (obligatorio al crear): costo del producto. Número mayor o igual a 0.",
					"precio_venta (obligatorio al crear): precio de venta. Número mayor o igual a 0.",
					"marca (opcional): debe existir en el catálogo; si se deja vacía se usa SIN MARCA.",
					"categoria (opcional): debe existir en el catálogo; si se deja vacía se usa SIN CATEGORIA.",
					"Si el código ya existe en el catálogo, estas cinco columnas se ignoran: el catálogo no se modifica desde la importación.",
					"Solo un administrador puede crear productos nuevos al importar.",
					"",
					"Reglas:",
					"- La primera fila debe conservar los encabezados; los datos empiezan en la fila 2.",
					"- Elimina las filas de ejemplo (EJEMPLO-... y NUEVO-...) antes de importar.",
					"- Incluye también los productos sin existencias (cantidad 0) para que queden registrados en la bodega.",
					"- Un mismo código no puede repetirse en el archivo.",
					"- Si el producto ya existe en la bodega, la cantidad se SUMA a la existencia actual.",
					"- Si una sola fila tiene error, no se importa ninguna ni se crea ningún producto: corrige el archivo y vuelve a intentarlo.",
					"- Máximo " + MAX_FILAS + " filas por archivo."
			};
			for (int i = 0; i < lineas.length; i++) {
				Row fila = instrucciones.createRow(i);
				Cell celda = fila.createCell(0);
				celda.setCellValue(lineas[i]);
				if (i == 0 || i == 2 || i == 7 || i == 16) {
					celda.setCellStyle(estiloEncabezado);
				}
			}
			instrucciones.setColumnWidth(0, 120 * 256);

			libro.write(salida);
			return salida.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException("No se pudo generar la plantilla de importación", e);
		}
	}

	private static String texto(Row fila, int columna, DataFormatter formato) {
		if (fila == null || columna < 0) {
			return "";
		}
		Cell celda = fila.getCell(columna);
		if (celda == null) {
			return "";
		}
		if (celda.getCellType() == CellType.NUMERIC && celda.getNumericCellValue() == Math.rint(celda.getNumericCellValue())
				&& Math.abs(celda.getNumericCellValue()) < 1e15) {
			// Evita que un código numérico se lea como "1.0E7" o "25.0" según el formato de la celda
			return String.valueOf((long) celda.getNumericCellValue());
		}
		return formato.formatCellValue(celda).trim();
	}

	private static Integer enteroNoNegativo(String texto) {
		try {
			int valor = Integer.parseInt(texto.trim());
			return valor < 0 ? null : valor;
		} catch (java.lang.NumberFormatException e) {
			return null;
		}
	}

	private static String normalizar(String texto) {
		String sinAcentos = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "");
		return sinAcentos.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
	}

}
