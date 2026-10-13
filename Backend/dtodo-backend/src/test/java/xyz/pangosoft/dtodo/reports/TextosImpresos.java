package xyz.pangosoft.dtodo.reports;

import java.util.ArrayList;
import java.util.List;

import net.sf.jasperreports.engine.JRPrintElement;
import net.sf.jasperreports.engine.JRPrintPage;
import net.sf.jasperreports.engine.JRPrintText;
import net.sf.jasperreports.engine.JasperPrint;

/** Los textos que una plantilla imprime realmente, para comprobar qué ve cada rol en un reporte. */
final class TextosImpresos {

    private final List<String> textos = new ArrayList<>();

    TextosImpresos(JasperPrint print) {
        for (JRPrintPage pagina : print.getPages()) {
            for (JRPrintElement elemento : pagina.getElements()) {
                if (elemento instanceof JRPrintText) {
                    textos.add(((JRPrintText) elemento).getFullText());
                }
            }
        }
    }

    boolean contiene(String fragmento) {
        return textos.stream().anyMatch(texto -> texto != null && texto.contains(fragmento));
    }
}
