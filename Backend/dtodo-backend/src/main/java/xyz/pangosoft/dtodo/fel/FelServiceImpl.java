package xyz.pangosoft.dtodo.fel;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import xyz.pangosoft.dtodo.fel.client.FelCertificadorClient;
import xyz.pangosoft.dtodo.fel.client.FelSignerClient;
import xyz.pangosoft.dtodo.fel.dto.RespuestaCertificacion;
import xyz.pangosoft.dtodo.fel.dto.RespuestaFirma;
import xyz.pangosoft.dtodo.fel.dto.RespuestaXml;
import xyz.pangosoft.dtodo.fel.xml.FelXmlBuilder;
import xyz.pangosoft.dtodo.model.Certificador;

/**
 * Implementación de {@link IFelService} sobre los servicios web de INFILE.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FelServiceImpl implements IFelService {

    private final FelXmlBuilder felXmlBuilder;
    private final FelSignerClient felSignerClient;
    private final FelCertificadorClient felCertificadorClient;

    @Override
    public RespuestaFirma firmarDocumento(Object objetoFel, Certificador certificador) {
        log.info("********** Proceso de Firma FEL Iniciado ***********");

        RespuestaXml respuestaXml = felXmlBuilder.toXml(objetoFel);
        if (!respuestaXml.getResultado()) {
            log.warn("La generación del XML falló: {} ({} errores)",
                    respuestaXml.getDescripcion(), respuestaXml.getCantidad_errores());
            if (respuestaXml.getErrores() != null) {
                respuestaXml.getErrores().forEach(log::error);
            }
            RespuestaFirma respuestaFirma = new RespuestaFirma();
            respuestaFirma.setResultado(false);
            respuestaFirma.setDescripcion(respuestaXml.getDescripcion());
            return respuestaFirma;
        }

        log.info("Generación de archivo XML se llevó a cabo con éxito");
        log.info("--> Enviando documento al servicio de firma del emisor...");

        RespuestaFirma respuestaFirma = felSignerClient.firmar(
                respuestaXml.getXml(), certificador.getPrefijo(), certificador.getTokenSigner());

        log.info("--> Resultado firma: {}", respuestaFirma.isResultado());
        log.info("--> Descripción firma: {}", respuestaFirma.getDescripcion());
        log.info("********** Proceso de Firma FEL Finalizado ***********");
        return respuestaFirma;
    }

    @Override
    public RespuestaCertificacion certificar(Certificador certificador, String xmlFirmado,
                                             String identificador, String tipoTransaccion) {
        log.info("--> Enviando documento al servicio FEL de INFILE ({})", tipoTransaccion);

        return felCertificadorClient.certificar(xmlFirmado, certificador.getPrefijo(),
                certificador.getLlaveWs(), identificador, tipoTransaccion);
    }
}
