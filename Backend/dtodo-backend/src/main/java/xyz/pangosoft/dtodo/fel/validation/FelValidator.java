package xyz.pangosoft.dtodo.fel.validation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import xyz.pangosoft.dtodo.fel.model.AnulacionFel;
import xyz.pangosoft.dtodo.fel.model.DocumentoFel;
import xyz.pangosoft.dtodo.fel.model.Frases;
import xyz.pangosoft.dtodo.fel.model.ImpuestosDetalle;
import xyz.pangosoft.dtodo.fel.model.Items;
import xyz.pangosoft.dtodo.fel.model.TotalImpuestos;

/**
 * Validación de integridad de los datos de un DTE antes de enviarlo a INFILE.
 *
 * <p>Puerto de {@code com.fel.validaciones.documento.ValidacionGeneralDatos} de
 * ConectorJava.jar, con las mismas reglas de campos obligatorios para
 * {@link DocumentoFel} y {@link AnulacionFel} (los complementos quedan fuera de
 * alcance: no se usan en este sistema).</p>
 */
@Component
public class FelValidator {

    /**
     * Valida el objeto FEL y devuelve la lista de errores encontrados
     * (vacía si los datos son íntegros).
     */
    public List<String> validar(Object transaccion) {
        List<String> errores = new ArrayList<>();
        if (transaccion instanceof DocumentoFel) {
            validarDocumento((DocumentoFel) transaccion, errores);
        } else if (transaccion instanceof AnulacionFel) {
            validarAnulacion((AnulacionFel) transaccion, errores);
        }
        return errores;
    }

    private void validarDocumento(DocumentoFel documentoFel, List<String> errores) {
        if (documentoFel.getDatos_generales() != null) {
            validarCampo(errores, documentoFel.getDatos_generales().getFechaHoraEmision(), "FechaHoraEmision", true);
            validarCampo(errores, documentoFel.getDatos_generales().getCodigoMoneda(), "CodigoMoneda", true);
            validarCampo(errores, documentoFel.getDatos_generales().getTipo(), "Tipo", true);
        } else {
            errores.add("Error, no esta presente el objecto \"DatosGenerales\" ");
        }

        if (documentoFel.getDatos_emisor() != null) {
            validarCampo(errores, documentoFel.getDatos_emisor().getCodigoEstablecimiento(), "CodigoEstablecimiento", false);
            validarCampo(errores, documentoFel.getDatos_emisor().getNITEmisor(), "NITEmisor", true);
            validarCampo(errores, documentoFel.getDatos_emisor().getNombreComercial(), "NombreComercial", true);
            validarCampo(errores, documentoFel.getDatos_emisor().getAfiliacionIVA(), "AfiliacionIVA", true);
            validarCampo(errores, documentoFel.getDatos_emisor().getNombreEmisor(), "NombreEmisor", true);
            validarCampo(errores, documentoFel.getDatos_emisor().getDireccion(), "Direccion", true);
            validarCampo(errores, documentoFel.getDatos_emisor().getCodigoPostal(), "CodigoPostal", true);
            validarCampo(errores, documentoFel.getDatos_emisor().getMunicipio(), "Municipio", true);
            validarCampo(errores, documentoFel.getDatos_emisor().getDepartamento(), "Departamento", true);
            validarCampo(errores, documentoFel.getDatos_emisor().getPais(), "Pais", true);
        } else {
            errores.add("Error, no esta presente el objecto \"DatosEmisor\" ");
        }

        if (documentoFel.getDatos_receptor() != null) {
            validarCampo(errores, documentoFel.getDatos_receptor().getIDReceptor(), "IDReceptor", true);
            validarCampo(errores, documentoFel.getDatos_receptor().getNombreReceptor(), "NombreReceptor", true);
            validarCampo(errores, documentoFel.getDatos_receptor().getDireccion(), "Direccion del Receptor", true);
            validarCampo(errores, documentoFel.getDatos_receptor().getCodigoPostal(), "CodigoPostal del Receptor", true);
            validarCampo(errores, documentoFel.getDatos_receptor().getMunicipio(), "Municipio del Receptor", true);
            validarCampo(errores, documentoFel.getDatos_receptor().getDepartamento(), "Departamento del Receptor", true);
            validarCampo(errores, documentoFel.getDatos_receptor().getPais(), "Pais del Receptor", true);
        } else {
            errores.add("Error, no esta presente el objecto \"DatosReceptor\" ");
        }

        for (int f = 0; f < documentoFel.getFrases().size(); f++) {
            Frases frase = documentoFel.getFrases().get(f);
            validarCampo(errores, frase.getTipoFrase(), "TipoFrase, de la Frase No. " + (f + 1), false);
            validarCampo(errores, frase.getCodigoEscenario(), "CodigoEscenario de la Frase No. " + (f + 1), false);
        }

        if (documentoFel.getItems().isEmpty()) {
            errores.add("Error, debe de existir al menos un Detalle.");
        } else {
            validarItems(documentoFel.getItems(), errores);
        }

        for (int r = 0; r < documentoFel.getImpuestos_resumen().size(); r++) {
            TotalImpuestos totalImpuestos = documentoFel.getImpuestos_resumen().get(r);
            validarCampo(errores, totalImpuestos.getNombreCorto(), "NombreCorto, de la seccion TotalImpuestos No. " + (r + 1), true);
            validarCampo(errores, totalImpuestos.getTotalMontoImpuesto(), "MontoImpuesto, de la seccion TotalImpuestos No. " + (r + 1), false);
        }

        if (documentoFel.getTotales() != null) {
            validarCampo(errores, documentoFel.getTotales().getGranTotal(), "GranTotal", false);
        } else {
            errores.add("Error, no esta presente el objecto obligatorio \"Totales\"");
        }
    }

    private void validarItems(List<Items> items, List<String> errores) {
        for (int i = 0; i < items.size(); i++) {
            Items item = items.get(i);
            validarCampo(errores, item.getBienOServicio(), "BienOServicio, del Detalle No. " + (i + 1), true);
            validarCampo(errores, item.getCantidad(), "Cantidad, del Detalle No. " + (i + 1), false);
            validarCampo(errores, item.getDescripcion(), "Descripcion, del Detalle No. " + (i + 1), true);
            validarCampo(errores, item.getNumeroLinea(), "NumeroLinea, del Detalle No. " + (i + 1), false);
            validarCampo(errores, item.getPrecio(), "Precio, del Detalle No. " + (i + 1), false);
            validarCampo(errores, item.getPrecioUnitario(), "PrecioUnitario, del Detalle No. " + (i + 1), false);
            validarCampo(errores, item.getTotal(), "Total, del Detalle No. " + (i + 1), false);

            for (int imp = 0; imp < item.getImpuestos_detalle().size(); imp++) {
                ImpuestosDetalle impuesto = item.getImpuestos_detalle().get(imp);
                String sufijo = " del Impuesto No. " + (imp + 1) + " del Detalle No. " + (i + 1);
                validarCampo(errores, impuesto.getNombreCorto(), "NombreCorto" + sufijo, true);
                validarCampo(errores, impuesto.getMontoImpuesto(), "MontoImpuesto" + sufijo, false);
                validarCampo(errores, impuesto.getCodigoUnidadGravable(), "CodigoUnidadGravable" + sufijo, false);
            }
        }
    }

    private void validarAnulacion(AnulacionFel anulacionFel, List<String> errores) {
        validarCampo(errores, anulacionFel.getFechaHoraAnulacion(), "FechaHoraAnulacion", true);
        validarCampo(errores, anulacionFel.getNITEmisor(), "NITEmisor", true);
        validarCampo(errores, anulacionFel.getFechaEmisionDocumentoAnular(), "FechaEmisionDocumentoAnular", true);
        validarCampo(errores, anulacionFel.getIDReceptor(), "IDReceptor", true);
        validarCampo(errores, anulacionFel.getNumeroDocumentoAAnular(), "NumeroDocumentoAAnular", true);
        validarCampo(errores, anulacionFel.getMotivoAnulacion(), "MotivoAnulacion", true);
    }

    private void validarCampo(List<String> errores, Object campo, String nombreCampo, boolean validarVacio) {
        if (campo == null) {
            errores.add("El campo obligatorio \"" + nombreCampo + "\" es nulo o no esta presente.");
        } else if (validarVacio && "".equals(campo)) {
            errores.add("El campo obligatorio \"" + nombreCampo + "\" esta vacio.");
        }
    }
}
