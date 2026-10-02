package xyz.pangosoft.dtodo.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
 * | codigo_producto | cantidad | stock_minimo |
 * | 7501234567890   | 25       | 5            |
 * </pre>
 * {@code stock_minimo} es opcional. Las filas totalmente vacías se ignoran.
 */
public final class InventarioBodegaExcel {

	public static final int MAX_FILAS = 10000;

	static final String COLUMNA_CODIGO = "codigo_producto";
	static final String COLUMNA_CANTIDAD = "cantidad";
	static final String COLUMNA_STOCK_MINIMO = "stock_minimo";

	private InventarioBodegaExcel() {
	}

	/** Una fila de datos ya validada en su forma (no en su existencia en el catálogo). */
	public record Fila(int numeroFila, String codigo, int cantidad, Integer stockMinimo) {
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
			if (encabezado != null) {
				for (Cell celda : encabezado) {
					String nombre = normalizar(formato.formatCellValue(celda));
					if (COLUMNA_CODIGO.equals(nombre) || "codigo".equals(nombre)) {
						colCodigo = celda.getColumnIndex();
					} else if (COLUMNA_CANTIDAD.equals(nombre)) {
						colCantidad = celda.getColumnIndex();
					} else if (COLUMNA_STOCK_MINIMO.equals(nombre)) {
						colStockMinimo = celda.getColumnIndex();
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
				String minimoTexto = colStockMinimo < 0 ? "" : texto(fila, colStockMinimo, formato);
				if (codigo.isEmpty() && cantidadTexto.isEmpty() && minimoTexto.isEmpty()) {
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
				if (cantidad == null || cantidad <= 0) {
					errores.add("Fila " + numero + ": la cantidad debe ser un número entero mayor a 0 (se leyó \""
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
				filas.add(new Fila(numero, codigo, cantidad, stockMinimo));
			}
			return new Lectura(filas, errores, leidas);
		} catch (BadRequestException e) {
			throw e;
		} catch (IOException | RuntimeException e) {
			throw new BadRequestException("No se pudo leer el archivo. Verifica que sea un Excel (.xlsx) válido.", e);
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

			Sheet inventario = libro.createSheet("Inventario");
			Row encabezado = inventario.createRow(0);
			String[] titulos = { COLUMNA_CODIGO, COLUMNA_CANTIDAD, COLUMNA_STOCK_MINIMO };
			for (int i = 0; i < titulos.length; i++) {
				Cell celda = encabezado.createCell(i);
				celda.setCellValue(titulos[i]);
				celda.setCellStyle(estiloEncabezado);
			}
			String[][] ejemplos = { { "EJEMPLO-001", "25", "5" }, { "EJEMPLO-002", "100", "" }, { "EJEMPLO-003", "8", "2" } };
			for (int i = 0; i < ejemplos.length; i++) {
				Row fila = inventario.createRow(i + 1);
				fila.createCell(0).setCellValue(ejemplos[i][0]);
				fila.createCell(1).setCellValue(Integer.parseInt(ejemplos[i][1]));
				if (!ejemplos[i][2].isEmpty()) {
					fila.createCell(2).setCellValue(Integer.parseInt(ejemplos[i][2]));
				}
			}
			for (int i = 0; i < titulos.length; i++) {
				inventario.setColumnWidth(i, 20 * 256);
			}

			Sheet instrucciones = libro.createSheet("Instrucciones");
			String[] lineas = {
					"Cómo llenar la hoja \"Inventario\"",
					"",
					"codigo_producto (obligatorio): código del producto tal como está registrado en el catálogo.",
					"cantidad (obligatorio): unidades que ingresan a la bodega. Número entero mayor a 0.",
					"stock_minimo (opcional): existencia mínima deseada en la bodega. Número entero mayor o igual a 0.",
					"",
					"Reglas:",
					"- La primera fila debe conservar los encabezados; los datos empiezan en la fila 2.",
					"- Elimina las filas de ejemplo (EJEMPLO-001...) antes de importar.",
					"- Un mismo código no puede repetirse en el archivo.",
					"- Si el producto ya existe en la bodega, la cantidad se SUMA a la existencia actual.",
					"- Si una sola fila tiene error, no se importa ninguna: corrige el archivo y vuelve a intentarlo.",
					"- Máximo " + MAX_FILAS + " filas por archivo."
			};
			for (int i = 0; i < lineas.length; i++) {
				Row fila = instrucciones.createRow(i);
				Cell celda = fila.createCell(0);
				celda.setCellValue(lineas[i]);
				if (i == 0 || i == 6) {
					celda.setCellStyle(estiloEncabezado);
				}
			}
			instrucciones.setColumnWidth(0, 110 * 256);

			libro.write(salida);
			return salida.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException("No se pudo generar la plantilla de importación", e);
		}
	}

	private static String texto(Row fila, int columna, DataFormatter formato) {
		if (fila == null) {
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
