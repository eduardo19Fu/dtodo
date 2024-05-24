package com.aglayatech.licorstore.controller;

import com.aglayatech.licorstore.model.Proforma;
import com.aglayatech.licorstore.service.IEstadoService;
import com.aglayatech.licorstore.service.IProformaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.sf.jasperreports.engine.JRException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(value = {"http://localhost:4200", "https://dtodojalapa.xyz", "http://dtodojalapa.xyz"})
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ProformaApiController {

    private final IProformaService proformaService;

    private final IEstadoService estadoService;

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
    @GetMapping("/proformas")
    public ResponseEntity<List<Proforma>> index(){
        log.info("Retornando listado de Proformas registradas");
        return ResponseEntity.ok(proformaService.findAll());
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
    @GetMapping("/proformas/{id}")
    public ResponseEntity<Proforma> findProforma(@PathVariable("id") Long id){
        log.info("Retornando Proforma registrada con ID: {}", id);
        return ResponseEntity.ok(proformaService.findProforma(id));
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
    @PostMapping(value = "/proformas")
    public ResponseEntity<Proforma> create(@RequestBody Proforma proforma) {
        log.info("Registrando nueva proforma: {}", proforma.toString());
        Proforma newProforma = proformaService.save(proforma);
        return ResponseEntity.status(HttpStatus.CREATED).body(newProforma);
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_ADMIN"})
    @PutMapping(value = "/proformas")
    public ResponseEntity<Proforma> udpate(@RequestBody Proforma proforma) {
        log.info("Actualizando proforma: {}", proforma.toString());
        Proforma proformaUpdated = proformaService.save(proforma);
        return ResponseEntity.status(HttpStatus.CREATED).body(proformaUpdated);
    }

    @Secured(value = "ROLE_ADMIN")
    @DeleteMapping(value = "/proformas/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable("id") Long id) {
        log.info("Eliminando proforma con ID: {}", id);

        Map<String, Object> response = new HashMap<>();
        Proforma proforma = proformaService.findProforma(id);
        proformaService.delete(proforma);
        response.put("mensaje", "¡Proforma ha sido eliminada con éxito!");
        return new ResponseEntity<Map<String, Object>>(response, HttpStatus.OK);
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
    @GetMapping("/proformas/get-listado-sp/get")
    public ResponseEntity<List<Proforma>> getProformasPorFecha(@RequestParam(value = "date1", required = false) String fechaIni,
                                                               @RequestParam(value = "date2", required = false) String fechaFin)
    {
        List<Proforma> proformas = new ArrayList<>();
        proformas = proformaService.proformasPorFecha(fechaIni, fechaFin);
        return ResponseEntity.ok(proformas);
    }

    /**
     * metodos controlador que permiten devolver un pdf con la información solicitada por
     * los usuarios desde el frontend.
     * @param idproforma Valor de tipo entero que refencia el ID de la proforma que se desea generar.
     * */

    @GetMapping("/proformas/generate/{id}")
    public ResponseEntity<byte[]> showProforma(@PathVariable("id") Long idproforma) {
        byte[] proformaPdf = proformaService.showProforma(idproforma);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(proformaPdf);
    }
}
