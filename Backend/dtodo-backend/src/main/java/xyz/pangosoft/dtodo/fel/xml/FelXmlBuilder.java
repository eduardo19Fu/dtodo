package xyz.pangosoft.dtodo.fel.xml;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import xyz.pangosoft.dtodo.fel.dto.RespuestaXml;
import xyz.pangosoft.dtodo.fel.model.AnulacionFel;
import xyz.pangosoft.dtodo.fel.model.DocumentoFel;
import xyz.pangosoft.dtodo.fel.model.Frases;
import xyz.pangosoft.dtodo.fel.model.ImpuestosDetalle;
import xyz.pangosoft.dtodo.fel.model.Items;
import xyz.pangosoft.dtodo.fel.model.TotalImpuestos;
import xyz.pangosoft.dtodo.fel.validation.FelValidator;

/**
 * Genera el XML de un DTE ({@code dte:GTDocumento}) o de una anulación
 * ({@code dte:GTAnulacionDocumento}) listo para enviarse al servicio de firma
 * de INFILE.
 *
 * <p>Puerto directo de {@code com.fel.validaciones.documento.GenerarXml} de
 * ConectorJava.jar: conserva los mismos templates de string ya certificados en
 * producción (incluido el reemplazo de {@code &} por <code>{{y}}</code> que espera
 * INFILE). Difiere del original en dos puntos deliberados:</p>
 * <ul>
 *   <li>El pretty-print usa {@link Transformer} en lugar de
 *       {@code com.sun.org.apache.xml.internal.serialize} (API interna removida
 *       en Java 9+).</li>
 *   <li>El {@link DecimalFormat} fija {@link Locale#US} explícitamente; el jar
 *       dependía del locale por defecto de la JVM.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FelXmlBuilder {

    private final FelValidator felValidator;

    /**
     * Genera y valida el XML del objeto FEL recibido.
     *
     * @param transaccion instancia de {@link DocumentoFel} o {@link AnulacionFel}
     * @return respuesta con el XML formateado, o con la lista de errores de validación
     */
    public RespuestaXml toXml(Object transaccion) {
        if (transaccion instanceof DocumentoFel) {
            return toXmlDocumento((DocumentoFel) transaccion);
        }
        if (transaccion instanceof AnulacionFel) {
            return toXmlAnulacion((AnulacionFel) transaccion);
        }
        throw new IllegalArgumentException(
                "Tipo de transacción FEL no soportado: " + transaccion.getClass().getName());
    }

    private RespuestaXml toXmlDocumento(DocumentoFel documentoFel) {
        if (!documentoFel.getComplementos().isEmpty()) {
            throw new UnsupportedOperationException(
                    "Los complementos FEL no están soportados por FelXmlBuilder");
        }

        DecimalFormat df = crearFormatoDecimal();

        String exportacion = documentoFel.getDatos_generales().getExportacion();
        String etiquetaExportacion = campoPresente(exportacion, true)
                ? "Exp=\"" + exportacion + "\"" : "";

        Integer numeroAcceso = documentoFel.getDatos_generales().getNumeroAcceso();
        String etiquetaNumeroAcceso = campoPresente(numeroAcceso, true)
                ? "NumeroAcceso=\"" + numeroAcceso + "\"" : "";

        String tipoPersoneria = documentoFel.getDatos_generales().getTipoPersoneria();
        String etiquetaTipoPersoneria = campoPresente(tipoPersoneria, true)
                ? "TipoPersoneria=\"" + tipoPersoneria + "\"" : "";

        String correoEmisor = documentoFel.getDatos_emisor().getCorreoEmisor();
        String etiquetaCorreoEmisor = campoPresente(correoEmisor, true)
                ? "CorreoEmisor=\"" + correoEmisor + "\"" : "";

        String correoReceptor = documentoFel.getDatos_receptor().getCorreoReceptor();
        String etiquetaCorreoReceptor = campoPresente(correoReceptor, true)
                ? "CorreoReceptor=\"" + correoReceptor.trim() + "\"" : "";

        String tipoEspecial = documentoFel.getDatos_receptor().getTipoEspecial();
        String etiquetaTipoEspecial = campoPresente(tipoEspecial, true)
                ? "TipoEspecial=\"" + tipoEspecial + "\"" : "";

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
           .append("<dte:GTDocumento xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" ")
           .append("xmlns:dte=\"http://www.sat.gob.gt/dte/fel/0.2.0\" ")
           .append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" Version=\"0.1\" ")
           .append("xsi:schemaLocation=\"http://www.sat.gob.gt/dte/fel/0.2.0\">\n")
           .append("  <dte:SAT ClaseDocumento=\"dte\">\n")
           .append("    <dte:DTE ID=\"DatosCertificados\">\n")
           .append("      <dte:DatosEmision ID=\"DatosEmision\">\n")
           .append("        <dte:DatosGenerales FechaHoraEmision=\"")
           .append(documentoFel.getDatos_generales().getFechaHoraEmision()).append("\" ")
           .append(etiquetaExportacion).append(" ")
           .append(etiquetaNumeroAcceso).append(etiquetaTipoPersoneria)
           .append(" CodigoMoneda=\"").append(documentoFel.getDatos_generales().getCodigoMoneda())
           .append("\" Tipo=\"").append(documentoFel.getDatos_generales().getTipo()).append("\"/>\n")
           .append("        <dte:Emisor ").append(etiquetaCorreoEmisor)
           .append(" CodigoEstablecimiento=\"").append(documentoFel.getDatos_emisor().getCodigoEstablecimiento())
           .append("\" NITEmisor=\"").append(documentoFel.getDatos_emisor().getNITEmisor())
           .append("\" NombreComercial=\"").append(escapar(documentoFel.getDatos_emisor().getNombreComercial()))
           .append("\" AfiliacionIVA=\"").append(documentoFel.getDatos_emisor().getAfiliacionIVA())
           .append("\" NombreEmisor=\"").append(escapar(documentoFel.getDatos_emisor().getNombreEmisor()))
           .append("\">\n")
           .append("          <dte:DireccionEmisor>\n")
           .append("            <dte:Direccion>").append(escapar(documentoFel.getDatos_emisor().getDireccion())).append("</dte:Direccion>\n")
           .append("            <dte:CodigoPostal>").append(documentoFel.getDatos_emisor().getCodigoPostal()).append("</dte:CodigoPostal>\n")
           .append("            <dte:Municipio>").append(documentoFel.getDatos_emisor().getMunicipio()).append("</dte:Municipio>\n")
           .append("            <dte:Departamento>").append(documentoFel.getDatos_emisor().getDepartamento()).append("</dte:Departamento>\n")
           .append("            <dte:Pais>").append(documentoFel.getDatos_emisor().getPais()).append("</dte:Pais>\n")
           .append("          </dte:DireccionEmisor>\n")
           .append("        </dte:Emisor>\n")
           .append("        <dte:Receptor IDReceptor=\"").append(documentoFel.getDatos_receptor().getIDReceptor())
           .append("\" ").append(etiquetaCorreoReceptor).append(" ").append(etiquetaTipoEspecial)
           .append(" NombreReceptor=\"").append(escapar(documentoFel.getDatos_receptor().getNombreReceptor()))
           .append("\">\n")
           .append("          <dte:DireccionReceptor>\n")
           .append("            <dte:Direccion>").append(escapar(documentoFel.getDatos_receptor().getDireccion())).append("</dte:Direccion>\n")
           .append("            <dte:CodigoPostal>").append(documentoFel.getDatos_receptor().getCodigoPostal()).append("</dte:CodigoPostal>\n")
           .append("            <dte:Municipio>").append(documentoFel.getDatos_receptor().getMunicipio()).append("</dte:Municipio>\n")
           .append("            <dte:Departamento>").append(documentoFel.getDatos_receptor().getDepartamento()).append("</dte:Departamento>\n")
           .append("            <dte:Pais>").append(documentoFel.getDatos_receptor().getPais()).append("</dte:Pais>\n")
           .append("          </dte:DireccionReceptor>\n")
           .append("        </dte:Receptor>\n");

        agregarFrases(xml, documentoFel.getFrases());
        agregarItems(xml, documentoFel.getItems(), df);
        agregarTotales(xml, documentoFel, df);
        agregarAdenda(xml, documentoFel);

        xml.append("  </dte:SAT>\n</dte:GTDocumento>");

        return validarYFormatear(documentoFel, xml.toString());
    }

    private RespuestaXml toXmlAnulacion(AnulacionFel anulacionFel) {
        // El schemaLocation con ruta local se conserva literal: es lo que envía
        // el conector original certificado en producción.
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<dte:GTAnulacionDocumento xmlns:n1=\"http://www.altova.com/samplexml/other-namespace\" "
                + "xmlns:dte=\"http://www.sat.gob.gt/dte/fel/0.1.0\" "
                + "xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" Version=\"0.1\" "
                + "xsi:schemaLocation=\"http://www.sat.gob.gt/dte/fel/0.1.0 "
                + "C:\\Users\\User\\Desktop\\FEL\\Esquemas\\GT_AnulacionDocumento-0.1.0.xsd\">\n"
                + "\t<dte:SAT>\n"
                + "\t\t<dte:AnulacionDTE ID=\"DatosCertificados\">\n"
                + "\t\t\t<dte:DatosGenerales FechaHoraAnulacion=\"" + anulacionFel.getFechaHoraAnulacion()
                + "\" ID=\"DatosAnulacion\" NITEmisor=\"" + anulacionFel.getNITEmisor()
                + "\" FechaEmisionDocumentoAnular=\"" + anulacionFel.getFechaEmisionDocumentoAnular()
                + "\" IDReceptor=\"" + anulacionFel.getIDReceptor()
                + "\" NumeroDocumentoAAnular=\"" + anulacionFel.getNumeroDocumentoAAnular()
                + "\" MotivoAnulacion=\"" + escapar(anulacionFel.getMotivoAnulacion()) + "\"/>\n"
                + "\t\t</dte:AnulacionDTE>\n"
                + "\t</dte:SAT>\n"
                + "</dte:GTAnulacionDocumento>";

        return validarYFormatear(anulacionFel, xml);
    }

    private void agregarFrases(StringBuilder xml, List<Frases> frases) {
        if (frases.isEmpty()) {
            return;
        }
        xml.append("<dte:Frases>");
        for (Frases frase : frases) {
            String etiquetaNumeroResolucion = campoPresente(frase.getNumeroResolucion(), true)
                    ? "NumeroResolucion=\"" + frase.getNumeroResolucion() + "\"" : "";
            String etiquetaFechaResolucion = campoPresente(frase.getFechaResolucion(), true)
                    ? "FechaResolucion=\"" + frase.getFechaResolucion() + "\"" : "";
            xml.append("<dte:Frase ").append(etiquetaNumeroResolucion).append(" ")
               .append(etiquetaFechaResolucion)
               .append(" CodigoEscenario=\"").append(frase.getCodigoEscenario())
               .append("\" TipoFrase=\"").append(frase.getTipoFrase()).append("\"/>");
        }
        xml.append("</dte:Frases>");
    }

    private void agregarItems(StringBuilder xml, List<Items> items, DecimalFormat df) {
        if (items.isEmpty()) {
            return;
        }
        xml.append("<dte:Items>");
        for (Items item : items) {
            String etiquetaDescuento = campoPresente(item.getDescuento(), false)
                    ? "<dte:Descuento>" + df.format(item.getDescuento()) + "</dte:Descuento>\n" : "";
            String etiquetaUnidadMedida = campoPresente(item.getUnidadMedida(), true)
                    ? "<dte:UnidadMedida>" + item.getUnidadMedida() + "</dte:UnidadMedida>\n" : "";

            xml.append("          <dte:Item NumeroLinea=\"").append(item.getNumeroLinea())
               .append("\" BienOServicio=\"").append(item.getBienOServicio()).append("\">\n")
               .append("            <dte:Cantidad>").append(df.format(item.getCantidad())).append("</dte:Cantidad>\n")
               .append(etiquetaUnidadMedida)
               .append("            <dte:Descripcion>").append(escapar(item.getDescripcion())).append("</dte:Descripcion>\n")
               .append("            <dte:PrecioUnitario>").append(df.format(item.getPrecioUnitario())).append("</dte:PrecioUnitario>\n")
               .append("            <dte:Precio>").append(df.format(item.getPrecio())).append("</dte:Precio>\n")
               .append(etiquetaDescuento);

            agregarImpuestosDeItem(xml, item, df);

            xml.append("            <dte:Total>").append(df.format(item.getTotal())).append("</dte:Total>\n")
               .append("          </dte:Item>\n");
        }
        xml.append("</dte:Items>");
    }

    private void agregarImpuestosDeItem(StringBuilder xml, Items item, DecimalFormat df) {
        if (item.getImpuestos_detalle().isEmpty()) {
            return;
        }
        xml.append("<dte:Impuestos>");
        for (ImpuestosDetalle impuesto : item.getImpuestos_detalle()) {
            String etiquetaCantidadUnidades = campoPresente(impuesto.getCantidadUnidadesGravables(), false)
                    ? "<dte:CantidadUnidadesGravables>" + df.format(impuesto.getCantidadUnidadesGravables())
                            + "</dte:CantidadUnidadesGravables>\n" : "";
            String etiquetaMontoGravable = campoPresente(impuesto.getMontoGravable(), false)
                    ? "<dte:MontoGravable>" + df.format(impuesto.getMontoGravable()) + "</dte:MontoGravable>\n" : "";

            xml.append("              <dte:Impuesto>\n")
               .append("                <dte:NombreCorto>").append(impuesto.getNombreCorto()).append("</dte:NombreCorto>\n")
               .append("                <dte:CodigoUnidadGravable>").append(impuesto.getCodigoUnidadGravable()).append("</dte:CodigoUnidadGravable>\n")
               .append(etiquetaCantidadUnidades)
               .append(etiquetaMontoGravable)
               .append("                <dte:MontoImpuesto>").append(df.format(impuesto.getMontoImpuesto())).append("</dte:MontoImpuesto>\n")
               .append("              </dte:Impuesto>\n");
        }
        xml.append("</dte:Impuestos>");
    }

    private void agregarTotales(StringBuilder xml, DocumentoFel documentoFel, DecimalFormat df) {
        xml.append("        <dte:Totales>\n");
        if (!documentoFel.getImpuestos_resumen().isEmpty()) {
            xml.append("<dte:TotalImpuestos>");
            for (TotalImpuestos impuestoResumen : documentoFel.getImpuestos_resumen()) {
                xml.append("<dte:TotalImpuesto NombreCorto=\"").append(impuestoResumen.getNombreCorto())
                   .append("\" TotalMontoImpuesto=\"").append(df.format(impuestoResumen.getTotalMontoImpuesto()))
                   .append("\"/>");
            }
            xml.append("</dte:TotalImpuestos>");
        }
        xml.append("\t\t<dte:GranTotal>").append(df.format(documentoFel.getTotales().getGranTotal()))
           .append("</dte:GranTotal>\n        </dte:Totales>\n")
           .append("      </dte:DatosEmision>\n    </dte:DTE>\n");
    }

    private void agregarAdenda(StringBuilder xml, DocumentoFel documentoFel) {
        if (documentoFel.getAdenda() == null || documentoFel.getAdenda().getAdenda().isEmpty()) {
            return;
        }
        xml.append(" <dte:Adenda>\n");
        TreeMap<String, String> adendaOrdenada = new TreeMap<>(documentoFel.getAdenda().getAdenda());
        for (Map.Entry<String, String> entrada : adendaOrdenada.entrySet()) {
            String etiqueta = entrada.getKey().toLowerCase();
            xml.append("<").append(etiqueta).append(">")
               .append(escapar(entrada.getValue()))
               .append("</").append(etiqueta).append(">\n");
        }
        xml.append(" </dte:Adenda>");
    }

    private RespuestaXml validarYFormatear(Object transaccion, String xml) {
        RespuestaXml respuesta = new RespuestaXml();

        List<String> errores = felValidator.validar(transaccion);
        if (!errores.isEmpty()) {
            log.warn("El objeto FEL no pasó la validación de integridad: {} errores", errores.size());
            respuesta.setResultado(false);
            respuesta.setCantidad_errores(errores.size());
            respuesta.setDescripcion("Error de Validacion de Integridad de Datos.");
            respuesta.setErrores(errores);
            respuesta.setXml("");
            return respuesta;
        }

        try {
            String xmlFormateado = prettyPrint(xml);
            respuesta.setResultado(true);
            respuesta.setCantidad_errores(0);
            respuesta.setDescripcion("XML Generado Correctamente.");
            respuesta.setErrores(null);
            respuesta.setXml(xmlFormateado);
        } catch (Exception e) {
            log.error("No se pudo formar el XML del DTE: {}", e.getMessage(), e);
            respuesta.setResultado(false);
            respuesta.setCantidad_errores(1);
            respuesta.setDescripcion("Error al formar el XML del DTE: " + e.getMessage());
            respuesta.setXml("");
        }
        return respuesta;
    }

    private String prettyPrint(String xml) throws Exception {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = documentBuilder.parse(new InputSource(new StringReader(xml)));

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        Writer out = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(out));
        return out.toString();
    }

    private DecimalFormat crearFormatoDecimal() {
        DecimalFormat df = new DecimalFormat("##.##", DecimalFormatSymbols.getInstance(Locale.US));
        df.setMaximumFractionDigits(10);
        return df;
    }

    /**
     * INFILE espera los ampersands sustituidos por el marcador <code>{{y}}</code>
     * (convención heredada del conector original).
     */
    private String escapar(String valor) {
        return valor.replace("&", "{{y}}");
    }

    private boolean campoPresente(Object campo, boolean validarVacio) {
        if (campo == null) {
            return false;
        }
        return !(validarVacio && "".equals(campo));
    }
}
