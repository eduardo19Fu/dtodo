package xyz.pangosoft.dtodo.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignConditionalStyle;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignImage;
import net.sf.jasperreports.engine.design.JRDesignRectangle;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignStyle;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.EvaluationTimeEnum;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.engine.type.VerticalTextAlignEnum;
import net.sf.jasperreports.engine.type.WhenNoDataTypeEnum;
import xyz.pangosoft.dtodo.util.ExcelTabla.Columna;
import xyz.pangosoft.dtodo.util.ExcelTabla.Tipo;

/**
 * Listado en PDF con el mismo estilo de los reportes de Dtodo (logo, título en azul, tabla con recuadros y filas alternadas)
 * armado desde las mismas columnas que usa {@link ExcelTabla}: así el PDF y el Excel de un reporte nunca difieren. Las columnas
 * creadas con {@link Columna#manual} salen vacías y con su recuadro, para llenarlas a mano como en los controles en papel.
 */
public final class PdfTabla {

    private static final String NAVY = "#2B3990";
    private static final String ZEBRA = "#F6F8FD";
    private static final String BORDE = "#9AA5C4";
    private static final String TEXTO = "#1F2A44";
    private static final String LOGO = "this.getClass().getResourceAsStream(\"/static/images/reports-image/logo-dtodo-proforma.png\")";
    private static final String FUENTE = "DejaVu Sans";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private PdfTabla() {
    }

    /**
     * @param altoFila alto de cada fila en puntos (más alto si se va a escribir a mano)
     */
    public static <T> byte[] generar(String titulo, List<String> contexto, List<Columna<T>> columnas, List<T> filas, int altoFila) {
        try {
            List<Map<String, Object>> datos = datos(columnas, filas);
            JasperDesign diseno = diseno(titulo, contexto == null ? new ArrayList<>() : contexto, columnas, altoFila);
            JasperReport reporte = JasperCompileManager.compileReport(diseno);
            JasperPrint print = JasperFillManager.fillReport(reporte, new HashMap<>(), new JRMapCollectionDataSource(
                    new ArrayList<Map<String, ?>>(datos)));
            return JasperExportManager.exportReportToPdf(print);
        } catch (JRException exception) {
            throw new IllegalStateException("No fue posible generar el listado en PDF.", exception);
        }
    }

    // ------------------------------------------------------------------------------------------ datos

    private static <T> List<Map<String, Object>> datos(List<Columna<T>> columnas, List<T> filas) {
        List<Map<String, Object>> datos = new ArrayList<>();
        BigDecimal[] sumas = new BigDecimal[columnas.size()];
        for (T fila : filas) {
            Map<String, Object> mapa = new HashMap<>();
            for (int c = 0; c < columnas.size(); c++) {
                Object valor = columnas.get(c).valorDe(fila);
                mapa.put("c" + c, texto(columnas.get(c).getTipo(), valor));
                if (columnas.get(c).isSumar() && valor instanceof Number) {
                    BigDecimal numero = valor instanceof BigDecimal ? (BigDecimal) valor : new BigDecimal(valor.toString());
                    sumas[c] = sumas[c] == null ? numero : sumas[c].add(numero);
                }
            }
            mapa.put("total", Boolean.FALSE);
            datos.add(mapa);
        }
        boolean hayTotales = columnas.stream().anyMatch(Columna::isSumar);
        if (hayTotales && !filas.isEmpty()) {
            Map<String, Object> totales = new HashMap<>();
            boolean etiqueta = false;
            for (int c = 0; c < columnas.size(); c++) {
                if (columnas.get(c).isSumar()) {
                    totales.put("c" + c, texto(columnas.get(c).getTipo(), sumas[c] == null ? BigDecimal.ZERO : sumas[c]));
                } else if (!etiqueta) {
                    totales.put("c" + c, "Total");
                    etiqueta = true;
                } else {
                    totales.put("c" + c, "");
                }
            }
            totales.put("total", Boolean.TRUE);
            datos.add(totales);
        }
        return datos;
    }

    static String texto(Tipo tipo, Object valor) {
        if (valor == null) {
            return "";
        }
        DecimalFormatSymbols simbolos = DecimalFormatSymbols.getInstance(Locale.US);
        switch (tipo) {
            case DINERO:
                return "Q" + new DecimalFormat("#,##0.00", simbolos).format(valor);
            case ENTERO:
                return new DecimalFormat("#,##0", simbolos).format(valor);
            case PORCENTAJE:
                return new DecimalFormat("0.00", simbolos).format(valor) + " %";
            case FECHA:
                return fecha(valor, FECHA);
            case FECHA_HORA:
                return fecha(valor, FECHA_HORA);
            default:
                return String.valueOf(valor);
        }
    }

    private static String fecha(Object valor, DateTimeFormatter formato) {
        if (valor instanceof LocalDate) {
            return formato == FECHA_HORA ? formato.format(((LocalDate) valor).atStartOfDay()) : formato.format((LocalDate) valor);
        }
        if (valor instanceof LocalDateTime) {
            return formato.format((LocalDateTime) valor);
        }
        if (valor instanceof Date) {
            return formato.format(new java.sql.Timestamp(((Date) valor).getTime()).toLocalDateTime());
        }
        return String.valueOf(valor);
    }

    // ------------------------------------------------------------------------------------------ diseño

    private static <T> JasperDesign diseno(String titulo, List<String> contexto, List<Columna<T>> columnas, int altoFila)
            throws JRException {
        JasperDesign d = new JasperDesign();
        d.setName("listado");
        d.setPageWidth(842);
        d.setPageHeight(595);
        d.setOrientation(net.sf.jasperreports.engine.type.OrientationEnum.LANDSCAPE);
        d.setLeftMargin(24);
        d.setRightMargin(24);
        d.setTopMargin(24);
        d.setBottomMargin(26);
        d.setColumnWidth(794);
        d.setWhenNoDataType(WhenNoDataTypeEnum.ALL_SECTIONS_NO_DETAIL);

        JRDesignStyle base = new JRDesignStyle();
        base.setName("Base");
        base.setDefault(true);
        base.setFontName(FUENTE);
        base.setFontSize(8.5f);
        base.setForecolor(java.awt.Color.decode(TEXTO));
        d.addStyle(base);

        JRDesignStyle celda = new JRDesignStyle();
        celda.setName("Celda");
        celda.setParentStyleNameReference("Base");
        celda.setMode(ModeEnum.OPAQUE);
        celda.setBackcolor(java.awt.Color.WHITE);
        JRDesignConditionalStyle total = new JRDesignConditionalStyle();
        total.setConditionExpression(expresion("$F{total}", Boolean.class));
        total.setBackcolor(java.awt.Color.decode("#E4E9F6"));
        total.setBold(true);
        celda.addConditionalStyle(total);
        JRDesignConditionalStyle zebra = new JRDesignConditionalStyle();
        zebra.setConditionExpression(expresion("Boolean.valueOf($V{REPORT_COUNT}.intValue() % 2 == 0)", Boolean.class));
        zebra.setBackcolor(java.awt.Color.decode(ZEBRA));
        celda.addConditionalStyle(zebra);
        d.addStyle(celda);

        List<String> campos = new ArrayList<>();
        for (int c = 0; c < columnas.size(); c++) {
            campos.add("c" + c);
            d.addField(campo("c" + c, String.class));
        }
        d.addField(campo("total", Boolean.class));

        int[] anchos = anchos(columnas, 794);

        // Título: logo, nombre del reporte y los filtros usados.
        JRDesignBand titleBand = new JRDesignBand();
        int altoTitulo = 58 + Math.max(0, contexto.size() - 1) * 13;
        titleBand.setHeight(altoTitulo + 8);
        JRDesignImage logo = new JRDesignImage(d);
        logo.setX(0);
        logo.setY(0);
        logo.setWidth(86);
        logo.setHeight(44);
        logo.setScaleImage(net.sf.jasperreports.engine.type.ScaleImageEnum.RETAIN_SHAPE);
        logo.setExpression(expresion(LOGO, java.io.InputStream.class));
        titleBand.addElement(logo);
        titleBand.addElement(texto(titulo, 100, 4, 520, 24, 17f, true, NAVY, HorizontalTextAlignEnum.LEFT));
        int y = 30;
        for (String linea : contexto) {
            titleBand.addElement(texto(linea, 100, y, 560, 13, 8.5f, false, "#6B7A90", HorizontalTextAlignEnum.LEFT));
            y += 13;
        }
        JRDesignTextField impreso = textField("\"Impreso: \" + new java.text.SimpleDateFormat(\"dd/MM/yyyy HH:mm\").format(new java.util.Date())",
                String.class, 594, 4, 200, 13, 8f, false, "#6B7A90", HorizontalTextAlignEnum.RIGHT);
        titleBand.addElement(impreso);
        JRDesignRectangle linea = new JRDesignRectangle();
        linea.setX(0);
        linea.setY(altoTitulo - 2);
        linea.setWidth(794);
        linea.setHeight(2);
        linea.setMode(ModeEnum.OPAQUE);
        linea.setBackcolor(java.awt.Color.decode(NAVY));
        linea.getLinePen().setLineWidth(0f);
        titleBand.addElement(linea);
        d.setTitle(titleBand);

        // Encabezado de columnas (se repite en cada página).
        JRDesignBand header = new JRDesignBand();
        header.setHeight(26);
        int x = 0;
        for (int c = 0; c < columnas.size(); c++) {
            JRDesignStaticText encabezado = estatico(columnas.get(c).getTitulo(), x, 0, anchos[c], 26);
            encabezado.setMode(ModeEnum.OPAQUE);
            encabezado.setBackcolor(java.awt.Color.decode(NAVY));
            encabezado.setForecolor(java.awt.Color.WHITE);
            encabezado.setBold(true);
            encabezado.setFontSize(8f);
            encabezado.setHorizontalTextAlign(alineacion(columnas.get(c).getTipo()));
            encabezado.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
            bordes(encabezado.getLineBox(), NAVY, 3, 3);
            header.addElement(encabezado);
            x += anchos[c];
        }
        d.setColumnHeader(header);

        // Detalle: una celda con recuadro por columna.
        JRDesignBand detalle = new JRDesignBand();
        detalle.setHeight(altoFila);
        x = 0;
        for (int c = 0; c < columnas.size(); c++) {
            JRDesignTextField campo = textField("$F{c" + c + "}", String.class, x, 0, anchos[c], altoFila, 8.5f, false, TEXTO,
                    alineacion(columnas.get(c).getTipo()));
            campo.setStyle(celda);
            campo.setMode(ModeEnum.OPAQUE);
            campo.setStretchWithOverflow(true);
            campo.setStretchType(net.sf.jasperreports.engine.type.StretchTypeEnum.RELATIVE_TO_TALLEST_OBJECT);
            campo.setBlankWhenNull(true);
            campo.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
            campo.setPositionType(net.sf.jasperreports.engine.type.PositionTypeEnum.FIX_RELATIVE_TO_TOP);
            bordes(campo.getLineBox(), BORDE, 3, 3);
            detalle.addElement(campo);
            x += anchos[c];
        }
        ((net.sf.jasperreports.engine.design.JRDesignSection) d.getDetailSection()).addBand(detalle);

        // Sin datos.
        JRDesignBand sinDatos = new JRDesignBand();
        sinDatos.setHeight(30);
        sinDatos.addElement(texto("No hay registros para los filtros seleccionados.", 0, 8, 794, 16, 9f, false, "#6B7A90",
                HorizontalTextAlignEnum.CENTER));
        d.setNoData(sinDatos);

        // Pie con el número de página.
        JRDesignBand pie = new JRDesignBand();
        pie.setHeight(18);
        JRDesignTextField pagina = textField("\"Página \" + $V{PAGE_NUMBER} + \" de \"", String.class, 594, 4, 160, 12, 7.5f, false,
                "#6B7A90", HorizontalTextAlignEnum.RIGHT);
        pie.addElement(pagina);
        JRDesignTextField total2 = textField("$V{PAGE_NUMBER}", String.class, 754, 4, 40, 12, 7.5f, false, "#6B7A90",
                HorizontalTextAlignEnum.LEFT);
        total2.setEvaluationTime(EvaluationTimeEnum.REPORT);
        total2.getLineBox().setLeftPadding(3);
        total2.setExpression(expresion("String.valueOf($V{PAGE_NUMBER})", String.class));
        pie.addElement(total2);
        pie.addElement(texto("D'Todo · Gestión comercial", 0, 4, 300, 12, 7.5f, false, "#6B7A90", HorizontalTextAlignEnum.LEFT));
        d.setPageFooter(pie);
        return d;
    }

    /** Reparte el ancho de la página según el ancho sugerido de cada columna (o un valor por defecto según su tipo). */
    private static <T> int[] anchos(List<Columna<T>> columnas, int total) {
        double[] pesos = new double[columnas.size()];
        double suma = 0;
        for (int c = 0; c < columnas.size(); c++) {
            Columna<T> columna = columnas.get(c);
            double peso = columna.getAncho() > 0 ? columna.getAncho() : pesoPorTipo(columna.getTipo(), columna.getTitulo());
            pesos[c] = peso;
            suma += peso;
        }
        int[] anchos = new int[columnas.size()];
        int usado = 0;
        for (int c = 0; c < anchos.length; c++) {
            anchos[c] = (int) Math.floor(total * pesos[c] / suma);
            usado += anchos[c];
        }
        anchos[anchos.length - 1] += total - usado;
        return anchos;
    }

    private static double pesoPorTipo(Tipo tipo, String titulo) {
        switch (tipo) {
            case DINERO:
                return 14;
            case ENTERO:
                return 9;
            case PORCENTAJE:
                return 9;
            case FECHA:
                return 13;
            case FECHA_HORA:
                return 17;
            default:
                return Math.max(12, Math.min(26, titulo.length() + 8));
        }
    }

    private static HorizontalTextAlignEnum alineacion(Tipo tipo) {
        switch (tipo) {
            case DINERO:
            case ENTERO:
            case PORCENTAJE:
                return HorizontalTextAlignEnum.RIGHT;
            case FECHA:
            case FECHA_HORA:
                return HorizontalTextAlignEnum.CENTER;
            default:
                return HorizontalTextAlignEnum.LEFT;
        }
    }

    private static void bordes(net.sf.jasperreports.engine.JRLineBox caja, String color, int izquierda, int derecha) {
        java.awt.Color c = java.awt.Color.decode(color);
        ((net.sf.jasperreports.engine.base.JRBaseLineBox) caja).getPen().setLineWidth(0.5f);
        ((net.sf.jasperreports.engine.base.JRBaseLineBox) caja).getPen().setLineColor(c);
        caja.setLeftPadding(izquierda);
        caja.setRightPadding(derecha);
    }

    private static JRDesignExpression expresion(String texto, Class<?> clase) {
        JRDesignExpression expresion = new JRDesignExpression();
        expresion.setText(texto);
        return expresion;
    }

    private static JRDesignField campo(String nombre, Class<?> clase) {
        JRDesignField campo = new JRDesignField();
        campo.setName(nombre);
        campo.setValueClass(clase);
        return campo;
    }

    private static JRDesignStaticText estatico(String texto, int x, int y, int ancho, int alto) {
        JRDesignStaticText elemento = new JRDesignStaticText();
        elemento.setText(texto);
        elemento.setX(x);
        elemento.setY(y);
        elemento.setWidth(ancho);
        elemento.setHeight(alto);
        return elemento;
    }

    private static JRDesignStaticText texto(String texto, int x, int y, int ancho, int alto, float tamanio, boolean negrita,
            String color, HorizontalTextAlignEnum alineacion) {
        JRDesignStaticText elemento = estatico(texto, x, y, ancho, alto);
        elemento.setFontSize(tamanio);
        elemento.setBold(negrita);
        elemento.setForecolor(java.awt.Color.decode(color));
        elemento.setHorizontalTextAlign(alineacion);
        return elemento;
    }

    private static JRDesignTextField textField(String expresion, Class<?> clase, int x, int y, int ancho, int alto, float tamanio,
            boolean negrita, String color, HorizontalTextAlignEnum alineacion) {
        JRDesignTextField campo = new JRDesignTextField();
        campo.setX(x);
        campo.setY(y);
        campo.setWidth(ancho);
        campo.setHeight(alto);
        campo.setFontSize(tamanio);
        campo.setBold(negrita);
        campo.setForecolor(java.awt.Color.decode(color));
        campo.setHorizontalTextAlign(alineacion);
        campo.setExpression(expresion(expresion, clase));
        return campo;
    }
}
