package xyz.pangosoft.dtodo.fel.model;

import java.util.ArrayList;

/**
 * Representa un DTE (Documento Tributario Electrónico) del régimen FEL de la SAT.
 *
 * <p>Reemplazo del modelo {@code com.fel.validaciones.documento.DocumentoFel} de
 * ConectorJava.jar. Conserva los mismos nombres de accessors — incluidos los
 * setters que en realidad agregan elementos a la lista ({@link #setFrases},
 * {@link #setItems}, {@link #setImpuestos_resumen}) — para mantener compatibilidad
 * con el código que ya consumía el conector.</p>
 */
public class DocumentoFel {

    private DatosEmisor datos_emisor;
    private DatosGenerales datos_generales;
    private DatosReceptor datos_receptor;
    private final ArrayList<Frases> frases = new ArrayList<>();
    private final ArrayList<Items> items = new ArrayList<>();
    private final ArrayList<TotalImpuestos> impuestos_resumen = new ArrayList<>();
    private Totales totales;
    private final ArrayList<Object> complementos = new ArrayList<>();
    private Adendas adenda;

    public Adendas getAdenda() {
        return adenda;
    }

    public void setAdenda(Adendas adenda) {
        this.adenda = adenda;
    }

    public ArrayList<Object> getComplementos() {
        return complementos;
    }

    public void setComplementos(Object complemento) {
        this.complementos.add(complemento);
    }

    public Totales getTotales() {
        return totales;
    }

    public void setTotales(Totales totales) {
        this.totales = totales;
    }

    public DatosEmisor getDatos_emisor() {
        return datos_emisor;
    }

    public void setDatos_emisor(DatosEmisor datos_emisor) {
        this.datos_emisor = datos_emisor;
    }

    public DatosGenerales getDatos_generales() {
        return datos_generales;
    }

    public void setDatos_generales(DatosGenerales datos_generales) {
        this.datos_generales = datos_generales;
    }

    public DatosReceptor getDatos_receptor() {
        return datos_receptor;
    }

    public void setDatos_receptor(DatosReceptor datos_receptor) {
        this.datos_receptor = datos_receptor;
    }

    public ArrayList<Frases> getFrases() {
        return frases;
    }

    public void setFrases(Frases frase) {
        this.frases.add(frase);
    }

    public ArrayList<Items> getItems() {
        return items;
    }

    public void setItems(Items item) {
        this.items.add(item);
    }

    public ArrayList<TotalImpuestos> getImpuestos_resumen() {
        return impuestos_resumen;
    }

    public void setImpuestos_resumen(TotalImpuestos totalImpuestos) {
        this.impuestos_resumen.add(totalImpuestos);
    }
}
