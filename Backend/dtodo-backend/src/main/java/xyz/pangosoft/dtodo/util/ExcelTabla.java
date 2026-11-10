package xyz.pangosoft.dtodo.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Genera un Excel "de verdad": una tabla plana con su fila de encabezados, una fila por registro, celdas numéricas y de fecha
 * reales (se pueden sumar, ordenar y filtrar), encabezado inmovilizado y autofiltro. Reemplaza al PDF convertido a Excel.
 *
 * <p>Estructura de la hoja: título, líneas de contexto (filtros usados), una fila en blanco, encabezados, datos y, si alguna
 * columna lo pide, una fila de totales con fórmulas SUM.</p>
 */
public final class ExcelTabla {

    public enum Tipo { TEXTO, ENTERO, DINERO, PORCENTAJE, FECHA, FECHA_HORA }

    /** Una columna de la tabla: su título, cómo sacar el valor de cada registro y cómo mostrarlo. */
    public static final class Columna<T> {
        private final String titulo;
        private final Function<T, Object> valor;
        private final Tipo tipo;
        private final boolean sumar;
        private final int ancho;

        private Columna(String titulo, Function<T, Object> valor, Tipo tipo, boolean sumar, int ancho) {
            this.titulo = titulo;
            this.valor = valor;
            this.tipo = tipo;
            this.sumar = sumar;
            this.ancho = ancho;
        }

        /** Una columna vacía para llenar a mano (cajas, firma, hora…): en el PDF queda una celda con su recuadro. */
        public static <T> Columna<T> manual(String titulo, int ancho) {
            return new Columna<>(titulo, fila -> null, Tipo.TEXTO, false, ancho);
        }

        public String getTitulo() {
            return titulo;
        }

        public Object valorDe(T registro) {
            return valor.apply(registro);
        }

        public Tipo getTipo() {
            return tipo;
        }

        public boolean isSumar() {
            return sumar;
        }

        /** Ancho sugerido en caracteres; 0 si se calcula solo. */
        public int getAncho() {
            return ancho;
        }

        public static <T> Columna<T> texto(String titulo, Function<T, Object> valor) {
            return new Columna<>(titulo, valor, Tipo.TEXTO, false, 0);
        }

        public static <T> Columna<T> texto(String titulo, int ancho, Function<T, Object> valor) {
            return new Columna<>(titulo, valor, Tipo.TEXTO, false, ancho);
        }

        public static <T> Columna<T> entero(String titulo, Function<T, Object> valor, boolean sumar) {
            return new Columna<>(titulo, valor, Tipo.ENTERO, sumar, 0);
        }

        public static <T> Columna<T> dinero(String titulo, Function<T, Object> valor, boolean sumar) {
            return new Columna<>(titulo, valor, Tipo.DINERO, sumar, 0);
        }

        /** Una fracción (0.4 = 40 %) que Excel muestra como porcentaje. */
        public static <T> Columna<T> porcentaje(String titulo, Function<T, Object> valor) {
            return new Columna<>(titulo, valor, Tipo.PORCENTAJE, false, 0);
        }

        public static <T> Columna<T> fecha(String titulo, Function<T, Object> valor) {
            return new Columna<>(titulo, valor, Tipo.FECHA, false, 0);
        }

        public static <T> Columna<T> fechaHora(String titulo, Function<T, Object> valor) {
            return new Columna<>(titulo, valor, Tipo.FECHA_HORA, false, 0);
        }
    }

    private ExcelTabla() {
    }

    public static <T> byte[] generar(String hoja, String titulo, List<String> contexto, List<Columna<T>> columnas, List<T> filas) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet sheet = libro.createSheet(nombreDeHoja(hoja));
            Estilos estilos = new Estilos(libro);
            int fila = 0;

            Row filaTitulo = sheet.createRow(fila++);
            Cell celdaTitulo = filaTitulo.createCell(0);
            celdaTitulo.setCellValue(titulo);
            celdaTitulo.setCellStyle(estilos.titulo);
            filaTitulo.setHeightInPoints(22);
            if (contexto != null) {
                for (String linea : contexto) {
                    if (linea != null && !linea.isBlank()) {
                        Cell celda = sheet.createRow(fila++).createCell(0);
                        celda.setCellValue(linea);
                        celda.setCellStyle(estilos.contexto);
                    }
                }
            }
            fila++;

            int filaEncabezado = fila++;
            Row encabezado = sheet.createRow(filaEncabezado);
            encabezado.setHeightInPoints(24);
            for (int c = 0; c < columnas.size(); c++) {
                Cell celda = encabezado.createCell(c);
                celda.setCellValue(columnas.get(c).titulo);
                celda.setCellStyle(estilos.encabezado(columnas.get(c).tipo));
            }

            int primeraDato = fila;
            for (T registro : filas) {
                Row row = sheet.createRow(fila++);
                for (int c = 0; c < columnas.size(); c++) {
                    escribir(row.createCell(c), columnas.get(c), columnas.get(c).valor.apply(registro), estilos, (fila - primeraDato) % 2 == 0);
                }
            }
            int ultimoDato = fila - 1;

            boolean hayTotales = columnas.stream().anyMatch(c -> c.sumar);
            if (hayTotales && !filas.isEmpty()) {
                Row totales = sheet.createRow(fila++);
                boolean etiquetaPuesta = false;
                for (int c = 0; c < columnas.size(); c++) {
                    Cell celda = totales.createCell(c);
                    Columna<T> columna = columnas.get(c);
                    if (columna.sumar) {
                        String letra = CellReference.convertNumToColString(c);
                        celda.setCellFormula("SUM(" + letra + (primeraDato + 1) + ":" + letra + (ultimoDato + 1) + ")");
                        celda.setCellStyle(estilos.total(columna.tipo));
                    } else {
                        if (!etiquetaPuesta) {
                            celda.setCellValue("Total");
                            etiquetaPuesta = true;
                        }
                        celda.setCellStyle(estilos.total(Tipo.TEXTO));
                    }
                }
            }

            if (filas.isEmpty()) {
                Cell vacio = sheet.createRow(fila).createCell(0);
                vacio.setCellValue("No hay registros para los filtros seleccionados.");
                vacio.setCellStyle(estilos.contexto);
            } else {
                sheet.setAutoFilter(new CellRangeAddress(filaEncabezado, ultimoDato, 0, columnas.size() - 1));
            }
            sheet.createFreezePane(0, filaEncabezado + 1);
            ajustarAnchos(sheet, columnas, filaEncabezado, ultimoDato);

            libro.getCreationHelper().createFormulaEvaluator().evaluateAll();
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible generar el archivo de Excel.", exception);
        }
    }

    private static <T> void escribir(Cell celda, Columna<T> columna, Object valor, Estilos estilos, boolean zebra) {
        celda.setCellStyle(estilos.dato(columna.tipo, zebra));
        if (valor == null) {
            return;
        }
        switch (columna.tipo) {
            case ENTERO:
            case DINERO:
            case PORCENTAJE:
                celda.setCellValue(aNumero(valor));
                break;
            case FECHA:
                if (valor instanceof LocalDateTime) {
                    celda.setCellValue(((LocalDateTime) valor).toLocalDate());
                } else if (valor instanceof LocalDate) {
                    celda.setCellValue((LocalDate) valor);
                } else if (valor instanceof Date) {
                    celda.setCellValue(new java.sql.Timestamp(((Date) valor).getTime()).toLocalDateTime().toLocalDate());
                }
                break;
            case FECHA_HORA:
                if (valor instanceof LocalDateTime) {
                    celda.setCellValue((LocalDateTime) valor);
                } else if (valor instanceof LocalDate) {
                    celda.setCellValue(((LocalDate) valor).atStartOfDay());
                } else if (valor instanceof Date) {
                    celda.setCellValue(new java.sql.Timestamp(((Date) valor).getTime()).toLocalDateTime());
                }
                break;
            default:
                celda.setCellValue(String.valueOf(valor));
        }
    }

    private static double aNumero(Object valor) {
        if (valor instanceof BigDecimal) {
            return ((BigDecimal) valor).doubleValue();
        }
        if (valor instanceof Number) {
            return ((Number) valor).doubleValue();
        }
        return Double.parseDouble(String.valueOf(valor));
    }

    private static <T> void ajustarAnchos(Sheet sheet, List<Columna<T>> columnas, int desde, int hasta) {
        for (int c = 0; c < columnas.size(); c++) {
            int ancho;
            if (columnas.get(c).ancho > 0) {
                ancho = columnas.get(c).ancho;
            } else {
                int max = columnas.get(c).titulo.length() + 4;
                for (int r = desde + 1; r <= hasta; r++) {
                    Row row = sheet.getRow(r);
                    Cell celda = row == null ? null : row.getCell(c);
                    if (celda != null && celda.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                        max = Math.max(max, celda.getStringCellValue().length() + 2);
                    }
                }
                ancho = columnas.get(c).tipo == Tipo.TEXTO ? Math.min(max, 48) : Math.max(max, 14);
            }
            sheet.setColumnWidth(c, Math.min(ancho, 60) * 256);
        }
    }

    private static String nombreDeHoja(String hoja) {
        String limpio = hoja == null ? "Hoja1" : hoja.replaceAll("[\\\\/?*\\[\\]:]", " ").trim();
        return limpio.isEmpty() ? "Hoja1" : limpio.substring(0, Math.min(31, limpio.length()));
    }

    /** Estilos del libro, creados una sola vez (Excel limita la cantidad de estilos distintos). */
    private static final class Estilos {
        private static final byte[] NAVY = { (byte) 0x2B, (byte) 0x39, (byte) 0x90 };
        private static final byte[] ZEBRA = { (byte) 0xF6, (byte) 0xF8, (byte) 0xFD };
        private static final byte[] TOTAL = { (byte) 0xE4, (byte) 0xE9, (byte) 0xF6 };

        private final XSSFWorkbook libro;
        private final CellStyle titulo;
        private final CellStyle contexto;
        private final java.util.Map<String, CellStyle> porClave = new java.util.HashMap<>();

        Estilos(XSSFWorkbook libro) {
            this.libro = libro;
            XSSFFont fuenteTitulo = libro.createFont();
            fuenteTitulo.setBold(true);
            fuenteTitulo.setFontHeightInPoints((short) 14);
            fuenteTitulo.setColor(new XSSFColor(NAVY, null));
            this.titulo = libro.createCellStyle();
            this.titulo.setFont(fuenteTitulo);
            XSSFFont fuenteContexto = libro.createFont();
            fuenteContexto.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            this.contexto = libro.createCellStyle();
            this.contexto.setFont(fuenteContexto);
        }

        CellStyle encabezado(Tipo tipo) {
            return porClave.computeIfAbsent("h" + alineacion(tipo), k -> {
                XSSFCellStyle estilo = libro.createCellStyle();
                XSSFFont fuente = libro.createFont();
                fuente.setBold(true);
                fuente.setColor(IndexedColors.WHITE.getIndex());
                estilo.setFont(fuente);
                estilo.setFillForegroundColor(new XSSFColor(NAVY, null));
                estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                estilo.setAlignment(alineacion(tipo));
                estilo.setVerticalAlignment(VerticalAlignment.CENTER);
                estilo.setWrapText(true);
                return estilo;
            });
        }

        CellStyle dato(Tipo tipo, boolean zebra) {
            return porClave.computeIfAbsent("d" + tipo + zebra, k -> {
                XSSFCellStyle estilo = libro.createCellStyle();
                estilo.setDataFormat(libro.createDataFormat().getFormat(formato(tipo)));
                estilo.setAlignment(alineacion(tipo));
                estilo.setBorderBottom(BorderStyle.HAIR);
                estilo.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
                if (zebra) {
                    estilo.setFillForegroundColor(new XSSFColor(ZEBRA, null));
                    estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                }
                return estilo;
            });
        }

        CellStyle total(Tipo tipo) {
            return porClave.computeIfAbsent("t" + tipo, k -> {
                XSSFCellStyle estilo = libro.createCellStyle();
                XSSFFont fuente = libro.createFont();
                fuente.setBold(true);
                estilo.setFont(fuente);
                estilo.setDataFormat(libro.createDataFormat().getFormat(formato(tipo)));
                estilo.setAlignment(alineacion(tipo));
                estilo.setFillForegroundColor(new XSSFColor(TOTAL, null));
                estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                estilo.setBorderTop(BorderStyle.THIN);
                return estilo;
            });
        }

        private static HorizontalAlignment alineacion(Tipo tipo) {
            switch (tipo) {
                case ENTERO:
                case DINERO:
                case PORCENTAJE:
                    return HorizontalAlignment.RIGHT;
                case FECHA:
                case FECHA_HORA:
                    return HorizontalAlignment.CENTER;
                default:
                    return HorizontalAlignment.LEFT;
            }
        }

        private static String formato(Tipo tipo) {
            switch (tipo) {
                case ENTERO:
                    return "#,##0";
                case DINERO:
                    return "\"Q\"#,##0.00";
                case PORCENTAJE:
                    return "0.00%";
                case FECHA:
                    return "dd/mm/yyyy";
                case FECHA_HORA:
                    return "dd/mm/yyyy hh:mm";
                default:
                    return "General";
            }
        }
    }
}
