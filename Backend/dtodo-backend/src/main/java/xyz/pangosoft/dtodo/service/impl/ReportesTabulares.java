package xyz.pangosoft.dtodo.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import xyz.pangosoft.dtodo.util.ExcelTabla;
import xyz.pangosoft.dtodo.util.ExcelTabla.Columna;
import xyz.pangosoft.dtodo.util.PdfTabla;
import xyz.pangosoft.dtodo.util.PdfTabla.Cabecera;
import xyz.pangosoft.dtodo.util.PdfTabla.Resumen;
import xyz.pangosoft.dtodo.util.PdfTabla.Resumen.Tono;

/**
 * Los reportes de bodega, compras y ventas que son una tabla: sus columnas, su encabezado y su resumen. Cada uno sale en PDF (con el
 * diseño de Dtodo) y en Excel (una tabla editable) desde las mismas columnas y la misma consulta SQL de su plantilla, que ahora solo
 * aporta la consulta y sus parámetros. Las filas llegan como mapas con el alias de cada columna de la consulta.
 */
final class ReportesTabulares {

    private static final int ALTO = 22;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ReportesTabulares() {
    }

    // ------------------------------------------------------------------------------------------ bodega

    static byte[] despachos(List<Map<String, Object>> filas, Map<String, Object> parametros, String formato) {
        boolean costos = Boolean.TRUE.equals(parametros.get("VER_COSTOS"));
        List<Columna<Map<String, Object>>> columnas = new ArrayList<>(Arrays.asList(
                Columna.texto("No.", 8, f -> f.get("numero") == null ? null : String.format("%06d", ((Number) f.get("numero")).longValue())),
                Columna.fechaHora("Fecha", f -> f.get("fecha")),
                Columna.texto("Bodega", 16, f -> f.get("bodega")),
                Columna.texto("Destino", 16, f -> f.get("sucursal")),
                Columna.texto("Recibe", 16, f -> f.get("recibe")),
                Columna.texto("Despachó", 16, f -> f.get("despacha")),
                Columna.texto("Estado", 11, f -> f.get("estado_etiqueta")),
                Columna.entero("Líneas", f -> f.get("lineas"), false),
                Columna.entero("Unidades", f -> f.get("unidades"), true)));
        if (costos) {
            // Un despacho cancelado no mueve inventario: no suma al total valorizado.
            columnas.add(Columna.dinero("Total al costo", f -> "CANCELADO".equals(f.get("estado_codigo")) ? null : f.get("total"), true));
        }
        long realizados = contar(filas, "estado_codigo", "REALIZADO");
        long pendientes = contar(filas, "estado_codigo", "PENDIENTE");
        List<Resumen> resumen = new ArrayList<>(Arrays.asList(
                new Resumen("Despachos", entero(filas.size()), Tono.FUERTE),
                new Resumen("Realizados", entero(realizados), Tono.NORMAL),
                new Resumen("Pendientes de aprobación", entero(pendientes), Tono.NORMAL)));
        if (costos) {
            BigDecimal total = BigDecimal.ZERO;
            for (Map<String, Object> fila : filas) {
                if (!"CANCELADO".equals(fila.get("estado_codigo"))) {
                    total = total.add(numero(fila.get("total")));
                }
            }
            resumen.add(new Resumen("Total valorizado al costo", dinero(total), Tono.NORMAL));
        } else {
            resumen.add(new Resumen("Unidades despachadas", entero(suma(filas, "unidades").longValue()), Tono.NORMAL));
        }
        Cabecera cabecera = Cabecera.de("Despachos de bodega").eyebrow("BODEGAS · DESPACHOS")
                .subtitulo(texto(parametros.get("BODEGA")) + " · " + texto(parametros.get("ESTADO_ETIQUETA")))
                .derecha("PERÍODO", texto(parametros.get("RANGO")))
                .nota(costos ? "El total valorizado al costo excluye los despachos cancelados." : null)
                .resumen(resumen);
        return salida(formato, "Despachos de bodega", cabecera, columnas, filas, ALTO,
                Arrays.asList(texto(parametros.get("RANGO")), texto(parametros.get("BODEGA")) + " · " + texto(parametros.get("ESTADO_ETIQUETA"))));
    }

    static byte[] existencias(List<Map<String, Object>> filas, Map<String, Object> parametros, String formato) {
        boolean costos = Boolean.TRUE.equals(parametros.get("VER_COSTOS"));
        List<Columna<Map<String, Object>>> columnas = new ArrayList<>(Arrays.asList(
                Columna.texto("Código", 11, f -> f.get("codigo")),
                Columna.texto("Producto", 30, f -> f.get("producto")),
                Columna.texto("Categoría", 18, f -> f.get("categoria")),
                Columna.entero("Existencia", f -> f.get("existencia"), true),
                Columna.entero("Stock mínimo", f -> f.get("stock_minimo"), false),
                Columna.texto("Alerta", 12, f -> bajoMinimo(f) ? "Bajo el mínimo" : "")));
        if (costos) {
            columnas.add(Columna.dinero("Costo unitario", f -> f.get("costo_unitario"), false));
            columnas.add(Columna.dinero("Valor total", f -> f.get("valor_total"), true));
        }
        long bajo = filas.stream().filter(ReportesTabulares::bajoMinimo).count();
        List<Resumen> resumen = new ArrayList<>(Arrays.asList(
                new Resumen("Productos", entero(filas.size()), Tono.FUERTE),
                new Resumen("Unidades en existencia", entero(suma(filas, "existencia").longValue()), Tono.NORMAL),
                new Resumen("Bajo el stock mínimo", entero(bajo), bajo > 0 ? Tono.ALERTA : Tono.NORMAL)));
        if (costos) {
            resumen.add(new Resumen("Valor al costo", dinero(suma(filas, "valor_total")), Tono.NORMAL));
        }
        String hoy = FECHA.format(LocalDate.now());
        Cabecera cabecera = Cabecera.de("Existencias de bodega").eyebrow("BODEGAS · INVENTARIO")
                .subtitulo(texto(parametros.get("BODEGA")))
                .derecha("AL DÍA", hoy)
                .nota("Los productos en o por debajo del stock mínimo configurado se marcan como «Bajo el mínimo»."
                        + (costos ? " El valor se calcula al costo de compra actual." : ""))
                .resumen(resumen);
        return salida(formato, "Existencias de bodega", cabecera, columnas, filas, ALTO,
                Arrays.asList(texto(parametros.get("BODEGA")), "Al " + hoy));
    }

    private static boolean bajoMinimo(Map<String, Object> fila) {
        return fila.get("existencia") != null && fila.get("stock_minimo") != null
                && ((Number) fila.get("existencia")).longValue() <= ((Number) fila.get("stock_minimo")).longValue();
    }

    static byte[] movimientos(List<Map<String, Object>> filas, Map<String, Object> parametros, String formato) {
        List<Columna<Map<String, Object>>> columnas = Arrays.asList(
                Columna.fechaHora("Fecha", f -> f.get("fecha")),
                Columna.texto("Tipo", 16, f -> f.get("tipo")),
                Columna.texto("Código", 10, f -> f.get("codigo")),
                Columna.texto("Producto", 26, f -> f.get("producto")),
                Columna.entero("Entrada", f -> f.get("entrada"), true),
                Columna.entero("Salida", f -> f.get("salida"), true),
                Columna.entero("Saldo", f -> f.get("saldo"), false),
                Columna.texto("Usuario", 11, f -> f.get("usuario")),
                Columna.texto("Detalle", 22, f -> f.get("detalle")));
        List<Resumen> resumen = Arrays.asList(
                new Resumen("Movimientos", entero(filas.size()), Tono.FUERTE),
                new Resumen("Unidades que entraron", entero(suma(filas, "entrada").longValue()), Tono.NORMAL),
                new Resumen("Unidades que salieron", entero(suma(filas, "salida").longValue()), Tono.NORMAL));
        Cabecera cabecera = Cabecera.de("Movimientos de bodega").eyebrow("BODEGAS · BITÁCORA")
                .subtitulo(texto(parametros.get("BODEGA")))
                .derecha("PERÍODO", texto(parametros.get("RANGO")))
                .nota("Bitácora cronológica de ingresos, importaciones, reducciones, eliminaciones y despachos de la bodega.")
                .resumen(resumen);
        return salida(formato, "Movimientos de bodega", cabecera, columnas, filas, ALTO,
                Arrays.asList(texto(parametros.get("RANGO")), texto(parametros.get("BODEGA"))));
    }

    // ------------------------------------------------------------------------------------------ compras

    static byte[] compras(List<Map<String, Object>> filas, Map<String, Object> parametros, String formato) {
        List<Columna<Map<String, Object>>> columnas = Arrays.asList(
                Columna.texto("No.", 9, f -> f.get("numero")),
                Columna.fecha("Fecha", f -> f.get("fecha_compra")),
                Columna.texto("Comprobante", 14, f -> f.get("no_comprobante")),
                Columna.texto("Tipo", 12, f -> f.get("tipo_comprobante")),
                Columna.texto("Proveedor", 26, f -> f.get("proveedor")),
                Columna.texto("Registrado por", 18, f -> f.get("responsable")),
                Columna.texto("Estado", 10, f -> capitalizar(f.get("estado"))),
                Columna.dinero("Total", f -> f.get("total"), true));
        long anuladas = contar(filas, "estado", "ANULADA");
        List<Resumen> resumen = Arrays.asList(
                new Resumen("Compras", entero(filas.size()), Tono.FUERTE),
                new Resumen("Importe total", dinero(suma(filas, "total")), Tono.NORMAL),
                new Resumen("Activas", entero(contar(filas, "estado", "ACTIVA")), Tono.NORMAL),
                new Resumen("Anuladas", entero(anuladas), anuladas > 0 ? Tono.ALERTA : Tono.NORMAL));
        Cabecera cabecera = Cabecera.de("Compras por período").eyebrow("COMPRAS · ABASTECIMIENTO")
                .subtitulo(texto(parametros.get("SUCURSAL")) + " · " + texto(parametros.get("PROVEEDOR")) + " · "
                        + texto(parametros.get("FILTRO_ESTADO")))
                .derecha("PERÍODO", texto(parametros.get("RANGO")))
                .nota("Incluye las compras registradas en el período, proveedor y estado seleccionados. Los importes se expresan en quetzales.")
                .resumen(resumen);
        return salida(formato, "Compras", cabecera, columnas, filas, ALTO, Arrays.asList(texto(parametros.get("RANGO")),
                texto(parametros.get("SUCURSAL")) + " · " + texto(parametros.get("PROVEEDOR")) + " · " + texto(parametros.get("FILTRO_ESTADO"))));
    }

    // ------------------------------------------------------------------------------------------ ventas

    static byte[] ventasCliente(List<Map<String, Object>> filas, Map<String, Object> parametros, String formato) {
        List<Columna<Map<String, Object>>> columnas = Arrays.asList(
                Columna.texto("Cliente", 28, f -> f.get("cliente")),
                Columna.texto("NIT", 12, f -> f.get("nit")),
                Columna.texto("Teléfono", 12, f -> f.get("telefono")),
                Columna.entero("Facturas", f -> f.get("facturas"), true),
                Columna.entero("Unidades", f -> f.get("unidades"), true),
                Columna.dinero("Venta bruta", f -> f.get("venta_bruta"), true),
                Columna.dinero("Descuento", f -> f.get("descuento"), true),
                Columna.dinero("Venta neta", f -> f.get("venta_neta"), true),
                Columna.fecha("Última compra", f -> f.get("ultima_compra")));
        List<Resumen> resumen = Arrays.asList(
                new Resumen("Clientes", entero(filas.size()), Tono.FUERTE),
                new Resumen("Facturas", entero(suma(filas, "facturas").longValue()), Tono.NORMAL),
                new Resumen("Unidades vendidas", entero(suma(filas, "unidades").longValue()), Tono.NORMAL),
                new Resumen("Venta neta", dinero(suma(filas, "venta_neta")), Tono.NORMAL));
        Cabecera cabecera = Cabecera.de("Ventas por cliente").eyebrow("VENTAS · CLIENTES")
                .subtitulo(texto(parametros.get("SUCURSAL")) + " · " + texto(parametros.get("CLIENTE")))
                .derecha("PERÍODO", texto(parametros.get("RANGO")))
                .nota("Resume documentos, unidades e importes vendidos por cliente, de mayor a menor venta neta. Los importes se expresan en quetzales.")
                .resumen(resumen);
        return salida(formato, "Ventas por cliente", cabecera, columnas, filas, ALTO,
                Arrays.asList(texto(parametros.get("RANGO")), texto(parametros.get("SUCURSAL")) + " · " + texto(parametros.get("CLIENTE"))));
    }

    static byte[] ventasProducto(List<Map<String, Object>> filas, Map<String, Object> parametros, String formato) {
        List<Columna<Map<String, Object>>> columnas = Arrays.asList(
                Columna.texto("Código", 11, f -> f.get("codigo")),
                Columna.texto("Producto", 32, f -> f.get("producto")),
                Columna.texto("Categoría", 18, f -> f.get("categoria")),
                Columna.entero("Unidades", f -> f.get("unidades"), true),
                Columna.dinero("Venta bruta", f -> f.get("venta_bruta"), true),
                Columna.dinero("Descuento", f -> f.get("descuento"), true),
                Columna.dinero("Venta neta", f -> f.get("venta_neta"), true));
        List<Resumen> resumen = Arrays.asList(
                new Resumen("Productos", entero(filas.size()), Tono.FUERTE),
                new Resumen("Unidades vendidas", entero(suma(filas, "unidades").longValue()), Tono.NORMAL),
                new Resumen("Descuentos otorgados", dinero(suma(filas, "descuento")), Tono.NORMAL),
                new Resumen("Venta neta", dinero(suma(filas, "venta_neta")), Tono.NORMAL));
        Cabecera cabecera = Cabecera.de("Ventas por producto").eyebrow("VENTAS · PRODUCTOS")
                .subtitulo(texto(parametros.get("SUCURSAL")) + " · " + texto(parametros.get("CATEGORIA")))
                .derecha("PERÍODO", texto(parametros.get("RANGO")))
                .nota("Resume unidades e importes vendidos por producto, de mayor a menor venta neta. Los importes se expresan en quetzales.")
                .resumen(resumen);
        return salida(formato, "Ventas por producto", cabecera, columnas, filas, ALTO,
                Arrays.asList(texto(parametros.get("RANGO")), texto(parametros.get("SUCURSAL")) + " · " + texto(parametros.get("CATEGORIA"))));
    }

    static byte[] rentabilidad(List<Map<String, Object>> filas, Map<String, Object> parametros, String formato) {
        List<Columna<Map<String, Object>>> columnas = Arrays.asList(
                Columna.texto("Código", 11, f -> f.get("codigo")),
                Columna.texto("Producto", 30, f -> f.get("producto")),
                Columna.texto("Categoría", 16, f -> f.get("categoria")),
                Columna.entero("Unidades", f -> f.get("unidades"), true),
                Columna.dinero("Venta neta", f -> f.get("venta_neta"), true),
                Columna.dinero("Costo estimado", f -> f.get("costo_estimado"), true),
                Columna.dinero("Margen", f -> f.get("margen"), true),
                Columna.porcentaje("Margen %", f -> f.get("margen_porcentaje")));
        BigDecimal venta = suma(filas, "venta_neta");
        BigDecimal margen = suma(filas, "margen");
        String porcentaje = venta.signum() == 0 ? "0.0 %"
                : margen.multiply(BigDecimal.valueOf(100)).divide(venta, 1, RoundingMode.HALF_UP) + " %";
        List<Resumen> resumen = Arrays.asList(
                new Resumen("Productos", entero(filas.size()), Tono.FUERTE),
                new Resumen("Venta neta", dinero(venta), Tono.NORMAL),
                new Resumen("Costo estimado", dinero(suma(filas, "costo_estimado")), Tono.NORMAL),
                new Resumen("Margen · " + porcentaje, dinero(margen), margen.signum() < 0 ? Tono.ALERTA : Tono.NORMAL));
        Cabecera cabecera = Cabecera.de("Rentabilidad por producto").eyebrow("VENTAS · RENTABILIDAD")
                .subtitulo(texto(parametros.get("SUCURSAL")) + " · " + texto(parametros.get("CATEGORIA")))
                .derecha("PERÍODO", texto(parametros.get("RANGO")))
                .nota("El costo se estima con el precio de compra actual de cada producto. Los importes se expresan en quetzales.")
                .resumen(resumen);
        return salida(formato, "Rentabilidad", cabecera, columnas, filas, ALTO,
                Arrays.asList(texto(parametros.get("RANGO")), texto(parametros.get("SUCURSAL")) + " · " + texto(parametros.get("CATEGORIA"))));
    }

    // ------------------------------------------------------------------------------------------ apoyo

    private static byte[] salida(String formato, String hoja, Cabecera cabecera, List<Columna<Map<String, Object>>> columnas,
            List<Map<String, Object>> filas, int altoFila, List<String> contextoExcel) {
        if ("XLSX".equals(formato)) {
            return ExcelTabla.generar(hoja, cabecera.getTitulo(), contextoExcel, columnas, filas);
        }
        return PdfTabla.generar(cabecera, columnas, filas, altoFila);
    }

    private static String texto(Object valor) {
        return valor == null ? "" : String.valueOf(valor);
    }

    private static String capitalizar(Object valor) {
        String texto = texto(valor);
        return texto.isEmpty() ? texto : texto.substring(0, 1) + texto.substring(1).toLowerCase();
    }

    private static BigDecimal numero(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        return valor instanceof BigDecimal ? (BigDecimal) valor : new BigDecimal(valor.toString());
    }

    private static BigDecimal suma(List<Map<String, Object>> filas, String clave) {
        BigDecimal suma = BigDecimal.ZERO;
        for (Map<String, Object> fila : filas) {
            suma = suma.add(numero(fila.get(clave)));
        }
        return suma;
    }

    private static long contar(List<Map<String, Object>> filas, String clave, String valor) {
        return filas.stream().filter(f -> valor.equals(f.get(clave))).count();
    }

    private static String entero(long valor) {
        return new java.text.DecimalFormat("#,##0", java.text.DecimalFormatSymbols.getInstance(java.util.Locale.US)).format(valor);
    }

    private static String dinero(BigDecimal valor) {
        return "Q" + new java.text.DecimalFormat("#,##0.00", java.text.DecimalFormatSymbols.getInstance(java.util.Locale.US)).format(valor);
    }
}
