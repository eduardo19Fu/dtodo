package xyz.pangosoft.dtodo.fel;

import xyz.pangosoft.dtodo.fel.dto.RespuestaCertificacion;
import xyz.pangosoft.dtodo.fel.dto.RespuestaFirma;
import xyz.pangosoft.dtodo.fel.model.AnulacionFel;
import xyz.pangosoft.dtodo.fel.model.DocumentoFel;
import xyz.pangosoft.dtodo.model.Certificador;

/**
 * Orquesta el flujo FEL contra INFILE: generación del XML del DTE, validación,
 * firma electrónica y certificación/anulación ante la SAT.
 *
 * <p>Reemplaza la integración vía ConectorJava.jar. Ningún método lanza
 * excepciones checked: los fallos de red o de validación se reportan con
 * {@code resultado=false} en la respuesta, igual que el conector original.</p>
 */
public interface IFelService {

    /**
     * Genera el XML del objeto FEL, lo valida y lo envía al servicio de firma de INFILE.
     *
     * @param objetoFel     instancia de {@link DocumentoFel} o {@link AnulacionFel}
     * @param certificador  credenciales del emisor ante INFILE
     * @return respuesta de la firma; {@code resultado=false} si la validación o la firma fallan
     */
    RespuestaFirma firmarDocumento(Object objetoFel, Certificador certificador);

    /**
     * Envía el DTE firmado al certificador INFILE.
     *
     * @param certificador     credenciales del emisor ante INFILE
     * @param xmlFirmado       XML firmado (Base64) devuelto por {@link #firmarDocumento}
     * @param identificador    identificador único de la transacción
     * @param tipoTransaccion  {@code "CERTIFICACION"} o {@code "ANULACION"}
     */
    RespuestaCertificacion certificar(Certificador certificador, String xmlFirmado,
                                      String identificador, String tipoTransaccion);
}
