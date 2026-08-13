package xyz.pangosoft.dtodo.fel.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import xyz.pangosoft.dtodo.fel.dto.RespuestaXml;
import xyz.pangosoft.dtodo.fel.model.Adendas;
import xyz.pangosoft.dtodo.fel.model.AnulacionFel;
import xyz.pangosoft.dtodo.fel.model.DatosEmisor;
import xyz.pangosoft.dtodo.fel.model.DatosGenerales;
import xyz.pangosoft.dtodo.fel.model.DatosReceptor;
import xyz.pangosoft.dtodo.fel.model.DocumentoFel;
import xyz.pangosoft.dtodo.fel.model.Frases;
import xyz.pangosoft.dtodo.fel.model.ImpuestosDetalle;
import xyz.pangosoft.dtodo.fel.model.Items;
import xyz.pangosoft.dtodo.fel.model.TotalImpuestos;
import xyz.pangosoft.dtodo.fel.model.Totales;
import xyz.pangosoft.dtodo.fel.validation.FelValidator;

/**
 * Verifica que {@link FelXmlBuilder} produce XML semánticamente idéntico al que
 * generaba {@code GenerarXml} de ConectorJava.jar.
 *
 * <p>Los fixtures {@code fel/jar-factura.xml} y {@code fel/jar-anulacion.xml}
 * fueron generados con el jar original (JDK 8) usando exactamente los mismos
 * datos que construye este test. La comparación es a nivel DOM: el serializador
 * del jar (Xerces) alfabetiza los atributos y el {@code Transformer} no, por lo
 * que la igualdad byte a byte no aplica — la igualdad semántica sí.</p>
 */
class FelXmlBuilderTest {

    private final FelXmlBuilder builder = new FelXmlBuilder(new FelValidator());

    @Test
    void facturaGeneraXmlEquivalenteAlConectorOriginal() throws Exception {
        RespuestaXml respuesta = builder.toXml(documentoFelDeReferencia());

        assertTrue(respuesta.getResultado(), "La generación del XML debió ser exitosa");
        assertEquals(0, respuesta.getCantidad_errores());
        assertDomEquivalente(fixture("/fel/jar-factura.xml"), respuesta.getXml());
    }

    @Test
    void anulacionGeneraXmlEquivalenteAlConectorOriginal() throws Exception {
        RespuestaXml respuesta = builder.toXml(anulacionFelDeReferencia());

        assertTrue(respuesta.getResultado(), "La generación del XML debió ser exitosa");
        assertEquals(0, respuesta.getCantidad_errores());
        assertDomEquivalente(fixture("/fel/jar-anulacion.xml"), respuesta.getXml());
    }

    @Test
    void documentoSinItemsFallaLaValidacion() {
        DocumentoFel documento = documentoFelDeReferencia();
        documento.getItems().clear();

        RespuestaXml respuesta = builder.toXml(documento);

        assertTrue(!respuesta.getResultado(), "La validación debió fallar sin items");
        assertTrue(respuesta.getErrores().contains("Error, debe de existir al menos un Detalle."));
    }

    /* ------------------------------------------------------------------ */
    /* Datos de referencia: deben coincidir con los usados para los fixtures */
    /* ------------------------------------------------------------------ */

    private DocumentoFel documentoFelDeReferencia() {
        DocumentoFel doc = new DocumentoFel();

        DatosEmisor emisor = new DatosEmisor();
        emisor.setAfiliacionIVA("GEN");
        emisor.setCodigoEstablecimiento(1);
        emisor.setCodigoPostal("21001");
        emisor.setCorreoEmisor("detodo.jalapa@gmail.com");
        emisor.setDepartamento("Jalapa");
        emisor.setMunicipio("Jalapa");
        emisor.setDireccion("Barrio La Democracia & Avenida Chipilapa 1-23 Zona 1, Jalapa, Jalapa");
        emisor.setNITEmisor("12345678");
        emisor.setNombreComercial("De Todo & Mas");
        emisor.setNombreEmisor("COMERCIAL DE TODO");
        emisor.setPais("GT");
        doc.setDatos_emisor(emisor);

        DatosGenerales generales = new DatosGenerales();
        generales.setCodigoMoneda("GTQ");
        generales.setFechaHoraEmision("2026-07-29T10:15:30-06:00");
        generales.setTipo("FACT");
        doc.setDatos_generales(generales);

        DatosReceptor receptor = new DatosReceptor();
        receptor.setCodigoPostal("01001");
        receptor.setCorreoReceptor("");
        receptor.setDepartamento(".");
        receptor.setMunicipio(".");
        receptor.setDireccion("Ciudad");
        receptor.setIDReceptor("CF");
        receptor.setNombreReceptor("Consumidor Final");
        receptor.setPais("GT");
        doc.setDatos_receptor(receptor);

        Frases frase = new Frases();
        frase.setCodigoEscenario(1);
        frase.setTipoFrase(1);
        doc.setFrases(frase);

        doc.setItems(item(1, "Aguarras & Solvente Mineral 1 Litro", 2.0, 25.5, 51.0));
        doc.setItems(item(2, "Clavo de 3 pulgadas", 1.0, 10.0, 10.0));

        TotalImpuestos totalImpuestos = new TotalImpuestos();
        totalImpuestos.setNombreCorto("IVA");
        totalImpuestos.setTotalMontoImpuesto((51.0 / 1.12) * 0.12 + (10.0 / 1.12) * 0.12);
        doc.setImpuestos_resumen(totalImpuestos);

        Totales totales = new Totales();
        totales.setGranTotal(61.0);
        doc.setTotales(totales);

        Adendas adendas = new Adendas();
        adendas.setAdenda("Cajero", "Juan Perez");
        adendas.setAdenda("Lote", "");
        adendas.setAdenda("OrdenCompra", "");
        adendas.setAdenda("Correlativo", "123");
        doc.setAdenda(adendas);

        return doc;
    }

    private Items item(int linea, String descripcion, double cantidad, double precioUnitario, double total) {
        Items item = new Items();
        item.setNumeroLinea(linea);
        item.setBienOServicio("B");
        item.setCantidad(cantidad);
        item.setUnidadMedida("UND");
        item.setDescripcion(descripcion);
        item.setPrecioUnitario(precioUnitario);
        item.setPrecio(total);
        item.setDescuento(0.0);
        item.setTotal(total);

        ImpuestosDetalle impuesto = new ImpuestosDetalle();
        impuesto.setNombreCorto("IVA");
        impuesto.setCodigoUnidadGravable(1);
        impuesto.setMontoGravable(total / 1.12);
        impuesto.setMontoImpuesto((total / 1.12) * 0.12);
        item.setImpuestos_detalle(impuesto);
        return item;
    }

    private AnulacionFel anulacionFelDeReferencia() {
        AnulacionFel anulacion = new AnulacionFel();
        anulacion.setFechaHoraAnulacion("2026-07-29T11:00:00-06:00");
        anulacion.setNITEmisor("12345678");
        anulacion.setFechaEmisionDocumentoAnular("2026-07-29T10:15:30-06:00");
        anulacion.setIDReceptor("CF");
        anulacion.setNumeroDocumentoAAnular("ABC12345-1111-2222-3333-444455556666");
        anulacion.setMotivoAnulacion("Anulacion");
        return anulacion;
    }

    /* ------------------------------------------------------------------ */
    /* Comparación DOM                                                      */
    /* ------------------------------------------------------------------ */

    private String fixture(String recurso) throws Exception {
        try (InputStream in = getClass().getResourceAsStream(recurso)) {
            java.util.Scanner scanner = new java.util.Scanner(in, "UTF-8").useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    private void assertDomEquivalente(String xmlEsperado, String xmlObtenido) throws Exception {
        Node esperado = parsear(xmlEsperado).getDocumentElement();
        Node obtenido = parsear(xmlObtenido).getDocumentElement();
        compararNodos(esperado, obtenido, "/" + esperado.getNodeName());
    }

    private Document parsear(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private void compararNodos(Node esperado, Node obtenido, String ruta) {
        assertEquals(esperado.getNodeName(), obtenido.getNodeName(), "Nombre de nodo en " + ruta);
        assertEquals(atributos(esperado), atributos(obtenido), "Atributos de " + ruta);

        List<Node> hijosEsperados = hijosRelevantes(esperado);
        List<Node> hijosObtenidos = hijosRelevantes(obtenido);
        assertEquals(hijosEsperados.size(), hijosObtenidos.size(),
                "Cantidad de hijos de " + ruta + " esperados=" + nombres(hijosEsperados)
                        + " obtenidos=" + nombres(hijosObtenidos));

        for (int i = 0; i < hijosEsperados.size(); i++) {
            Node hijoEsperado = hijosEsperados.get(i);
            Node hijoObtenido = hijosObtenidos.get(i);
            if (hijoEsperado.getNodeType() == Node.TEXT_NODE) {
                assertEquals(Node.TEXT_NODE, hijoObtenido.getNodeType(), "Tipo de nodo en " + ruta);
                assertEquals(normalizar(hijoEsperado.getTextContent()),
                        normalizar(hijoObtenido.getTextContent()), "Texto de " + ruta);
            } else {
                compararNodos(hijoEsperado, hijoObtenido, ruta + "/" + hijoEsperado.getNodeName() + "[" + i + "]");
            }
        }
    }

    private Map<String, String> atributos(Node nodo) {
        Map<String, String> mapa = new HashMap<>();
        NamedNodeMap atributos = nodo.getAttributes();
        if (atributos != null) {
            for (int i = 0; i < atributos.getLength(); i++) {
                Node atributo = atributos.item(i);
                mapa.put(atributo.getNodeName(), atributo.getNodeValue());
            }
        }
        return mapa;
    }

    private List<Node> hijosRelevantes(Node nodo) {
        List<Node> relevantes = new ArrayList<>();
        NodeList hijos = nodo.getChildNodes();
        for (int i = 0; i < hijos.getLength(); i++) {
            Node hijo = hijos.item(i);
            if (hijo.getNodeType() == Node.ELEMENT_NODE) {
                relevantes.add(hijo);
            } else if (hijo.getNodeType() == Node.TEXT_NODE
                    && !hijo.getTextContent().trim().isEmpty()) {
                relevantes.add(hijo);
            }
        }
        return relevantes;
    }

    private List<String> nombres(List<Node> nodos) {
        List<String> nombres = new ArrayList<>();
        for (Node nodo : nodos) {
            nombres.add(nodo.getNodeName());
        }
        return nombres;
    }

    private String normalizar(String texto) {
        return texto.trim().replaceAll("\\s+", " ");
    }
}
