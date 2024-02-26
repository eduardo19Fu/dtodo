package com.aglayatech.licorstore.controller;

import com.aglayatech.licorstore.service.*;
import com.fel.firma.emisor.FirmaEmisor;
import com.fel.firma.emisor.RespuestaServicioFirma;
import com.fel.validaciones.documento.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@CrossOrigin(value = {"http://localhost:4200", "https://31.220.56.29", "https://dtodojalapa.xyz", "http://dtodojalapa.xyz", "http://dtodo.pangosoft.xyz", "http://dimsa.pangosoft.xyz"})
@RestController
@RequestMapping("/pruebaFel")
public class PruebaFelApiController {

    @GetMapping("/prueba")
    public ResponseEntity<?> pruebaFel() throws InvocationTargetException, IllegalAccessException {

        Map<String, Object> response = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'-06:00'", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("America/Guatemala"));
        //System.out.println("Fecha:" + sdf.format(new Date()));

        //========================================================================
        // Variable para capturar la respuesta recibida del proceso de formacion del XML.

        DocumentoFel documento_fel = new DocumentoFel();

        DatosEmisor datos_emisor = new DatosEmisor();
        datos_emisor.setAfiliacionIVA("GEN");
        datos_emisor.setCodigoEstablecimiento(1);
        datos_emisor.setCodigoPostal("01001");
        datos_emisor.setCorreoEmisor("fel@fel.com.gt");
        datos_emisor.setDepartamento("Jalapa");
        datos_emisor.setMunicipio("Jalapa");
        datos_emisor.setDireccion("1 CALLE 4-32 ZONA 2");
        datos_emisor.setNITEmisor("45146276");
        datos_emisor.setNombreComercial("D'TODO");
        datos_emisor.setNombreEmisor("JAIRON DAVID, AGUILAR REYES");
        datos_emisor.setPais("GT");
        documento_fel.setDatos_emisor(datos_emisor);

        DatosGenerales datos_generales = new DatosGenerales();
        datos_generales.setCodigoMoneda("GTQ");
        datos_generales.setFechaHoraEmision(sdf.format(new Date()));
        //datos_generales.setNumeroAcceso(888777666); //OPCIONAL
        datos_generales.setTipo("FACT");
        // datos_generales.setExportacion("SI"); //OPCIONAL
        documento_fel.setDatos_generales(datos_generales);

        DatosReceptor datos_receptor = new DatosReceptor();
        datos_receptor.setCodigoPostal("01001");
        datos_receptor.setCorreoReceptor("ramieduar@gmail.com"); // Quien recibe el pdf por correo, pueden ir varios separados por ;
        datos_receptor.setDepartamento("JALAPA");
        datos_receptor.setDireccion("CALLE TRANSITO ROJAS ENTRE 5A. Y 6A. AVENIDA BO. EL PORVENIR ZONA 3 JALAPA JALAPA");
        datos_receptor.setIDReceptor("CF");
        datos_receptor.setMunicipio("JALAPA");
        datos_receptor.setNombreReceptor("CONSUMIDOR FINAL");
        datos_receptor.setPais("GT");
        documento_fel.setDatos_receptor(datos_receptor);

        for (int i = 1; i <= 1; i++) {
            Frases frases = new Frases();
            frases.setCodigoEscenario(i);
            frases.setTipoFrase(1);
            documento_fel.setFrases(frases);
        }

        for (int i = 1; i <= 1; i++) {
            Items items = new Items();
            items.setNumeroLinea(i);
            items.setBienOServicio("B");
            items.setCantidad(1.0);
            items.setDescripcion("Caja de Lapiceros");
            items.setDescuento(0.0);
            items.setPrecio(448.00);
            items.setPrecioUnitario(448.00);
            items.setUnidadMedida("UND");
            items.setTotal(448.00);

            for (int j = 1; j <= 1; j++) {
                ImpuestosDetalle impuestos_detalle = new ImpuestosDetalle();
                impuestos_detalle.setNombreCorto("IVA");
                impuestos_detalle.setCodigoUnidadGravable(1); // Preguntar si esto varia
                impuestos_detalle.setMontoGravable(400.00);

                //impuestos_detalle.setCantidadUnidadesGravables(78.00);
                impuestos_detalle.setMontoImpuesto(48.00); // Preguntar sobre el calculo del impuesto
                items.setImpuestos_detalle(impuestos_detalle);
            }

            documento_fel.setItems(items);
        }

        for (int k = 1; k <= 1; k++) {
            TotalImpuestos impuestos_resumen = new TotalImpuestos();
            impuestos_resumen.setNombreCorto("IVA");
            impuestos_resumen.setTotalMontoImpuesto(48.00);
            documento_fel.setImpuestos_resumen(impuestos_resumen);
        }

        Totales totales = new Totales();
        totales.setGranTotal(448.00);
        documento_fel.setTotales(totales);

        Adendas adendas = new Adendas();
        adendas.setAdenda("Cajero", "Alberto Morales");
        adendas.setAdenda("Lote", "45121");
        adendas.setAdenda("OrdenCompra", "1041-85");

        documento_fel.setAdenda(adendas);


        try{
            // Objeto para enviar los datos para generacion del XML
            Respuesta respuesta;
            GenerarXml generar_xml = new GenerarXml();
            respuesta = generar_xml.ToXml(documento_fel);

            // Comprobacion de Datos.
            if (respuesta.getResultado()) {

                System.out.println("--> FIRMA POR PARTE DEL EMISOR ");

                FirmaEmisor firma_emisor = new FirmaEmisor();
                RespuestaServicioFirma respuesta_firma_emisor = new RespuestaServicioFirma();

                System.out.println("--> Enviando Documento al Servicio de Firma del Emisor...");

                try {
                    respuesta_firma_emisor = firma_emisor.Firmar(respuesta.getXml(), "45146276", "a3c079b60c23f5a105e61f56ceb2dd43");
                } catch (NoSuchAlgorithmException ex) {
                    Logger.getLogger(PruebaFelApiController.class.getName()).log(Level.SEVERE, null, ex);
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(PruebaFelApiController.class.getName()).log(Level.SEVERE, null, ex);
                } catch (KeyManagementException e) {
                    e.printStackTrace();
                }

                System.out.println("--> Resultado: " + respuesta_firma_emisor.isResultado());
                System.out.println("--> Descripcion: " + respuesta_firma_emisor.getDescripcion());

                if (respuesta_firma_emisor.isResultado()) {

                    try {
                        System.out.println("--> ENVIO AL API DE INFILE");

                        ConexionServicioFel conexion = new ConexionServicioFel();
                        conexion.setUrl("");
                        conexion.setMetodo("POST");
                        conexion.setContent_type("application/json");
                        conexion.setUsuario("45146276"); // MISMO NIT
                        conexion.setLlave("ECB7BEBC7DD0145F94B3F01F859E5C3F");
                        conexion.setIdentificador("testconexion11"); // DEBE VARIAR SIENDO IDENTIFICADOR UNICO

                        System.out.println("--> Enviando Documento al Servicio FEL...");

                        ServicioFel servicio = new ServicioFel();

                        RespuestaServicioFel respuesta_servicio = servicio.Certificar(conexion, respuesta_firma_emisor.getArchivo(), "86669656", "N/A", "CERTIFICACION");

                        if (respuesta_servicio.getResultado()) {

                            System.out.println("--> Resultado: " + respuesta_servicio.getResultado());
                            System.out.println("--> Origen: " + respuesta_servicio.getOrigen());
                            System.out.println("--> Descripcion: " + respuesta_servicio.getDescripcion());
                            System.out.println("--> Cantidad Errores: " + respuesta_servicio.getCantidad_errores());
                            System.out.println("--> INFO: " + respuesta_servicio.getInfo());

                            System.out.println("UUID: " + respuesta_servicio.getUuid());
                            System.out.println("Serie: " + respuesta_servicio.getSerie());
                            System.out.println("Numero: " + respuesta_servicio.getNumero());
                            System.out.println("Fecha_certificacion: "+ respuesta_servicio.getFecha());

                        } else {

                            System.out.println("--> Resultado: " + respuesta_servicio.getResultado());
                            System.out.println("--> Origen: " + respuesta_servicio.getOrigen());
                            System.out.println("--> Descripcion: " + respuesta_servicio.getDescripcion());
                            System.out.println("--> Cantidad Errores: " + respuesta_servicio.getCantidad_errores());
                            System.out.println("--> INFO: " + respuesta_servicio.getInfo());


                            for (int i = 0; i < respuesta_servicio.getCantidad_errores(); i++) {
                                System.out.println(respuesta_servicio.getDescripcion_errores().get(i).getMensaje_error());

                            }

                        }
                    } catch (NoSuchAlgorithmException | KeyManagementException ex) {
                        Logger.getLogger(PruebaFelApiController.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }


            } else {

                //System.out.println(respuesta.getDescripcion());
                //System.out.println(respuesta.getCantidad_errores());
                // Ciclo para recorrer los errores.
                respuesta.getErrores().forEach((error) -> {
                    System.out.println(error);
                });

            }

            // Luego de obtener el resultado entonces se procede a enviar el xml al servicio de FEL de INFILE
            if (respuesta.getResultado()) {

            }

        } catch(DataAccessException e){
            response.put("message", "Ha ocurrido un error");
            response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return null;
    }
}
