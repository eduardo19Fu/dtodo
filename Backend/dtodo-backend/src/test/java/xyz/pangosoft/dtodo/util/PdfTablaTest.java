package xyz.pangosoft.dtodo.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import xyz.pangosoft.dtodo.util.ExcelTabla.Columna;

class PdfTablaTest {

    private static final class Fila {
        final String cliente;
        final BigDecimal monto;
        final LocalDate fecha;

        Fila(String cliente, String monto, LocalDate fecha) {
            this.cliente = cliente;
            this.monto = new BigDecimal(monto);
            this.fecha = fecha;
        }
    }

    private static List<Columna<Fila>> columnas() {
        return Arrays.asList(
                Columna.<Fila>texto("Cliente", 30, f -> f.cliente),
                Columna.<Fila>fecha("Fecha", f -> f.fecha),
                Columna.<Fila>dinero("Monto", f -> f.monto, true),
                Columna.<Fila>manual("Cajas", 8),
                Columna.<Fila>manual("Firma", 16));
    }

    @Test
    void generaUnPdfConTodasLasPaginasYLasColumnasManuales() throws Exception {
        List<Fila> filas = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            filas.add(new Fila("CLIENTE NUMERO " + i, "125.50", LocalDate.of(2026, 11, 9)));
        }

        byte[] pdf = PdfTabla.generar("Listado de prueba", Arrays.asList("Del 01/11/2026 al 09/11/2026", "Todas las rutas"),
                columnas(), filas, 24);

        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
        assertTrue(pdf.length > 3000);
        String destino = System.getProperty("pdftabla.salida");
        if (destino != null) {
            Files.write(new File(destino).toPath(), pdf);
        }
    }

    @Test
    void sinFilasGeneraElAviso() {
        byte[] pdf = PdfTabla.generar("Vacío", null, columnas(), new ArrayList<>(), 24);

        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void formateaMontosFechasYPorcentajes() {
        assertEquals("Q1,234.50", PdfTabla.texto(ExcelTabla.Tipo.DINERO, new BigDecimal("1234.5")));
        assertEquals("09/11/2026", PdfTabla.texto(ExcelTabla.Tipo.FECHA, LocalDate.of(2026, 11, 9)));
        assertEquals("40.00 %", PdfTabla.texto(ExcelTabla.Tipo.PORCENTAJE, new BigDecimal("40")));
        assertEquals("", PdfTabla.texto(ExcelTabla.Tipo.TEXTO, null));
    }
}
