package com.aglayatech.licorstore.controller;

import com.aglayatech.licorstore.model.Estado;
import com.aglayatech.licorstore.model.Factura;
import com.aglayatech.licorstore.model.Proforma;
import com.aglayatech.licorstore.service.IEstadoService;
import com.aglayatech.licorstore.service.IProformaService;
import net.sf.jasperreports.engine.JRException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(value = {"http://localhost:4200", "https://dtodojalapa.xyz", "http://dtodojalapa.xyz"})
@RestController
@RequestMapping("/api")
public class ProformaApiController {

    @Autowired
    private IProformaService proformaService;

    @Autowired
    private IEstadoService estadoService;

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
    @GetMapping("/proformas")
    public List<Proforma> index(){
        return this.proformaService.findAll();
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
    @GetMapping("/proformas/{id}")
    public ResponseEntity<?> findProforma(@PathVariable("id") Long id){

        Map<String, Object> response = new HashMap<>();
        Proforma proforma = null;

        try{
            proforma = this.proformaService.findProforma(id);
        } catch(DataAccessException e){
            response.put("mensaje", "¡Ha ocurrido un error en la Base de Datos!");
            response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if(proforma == null){
            response.put("mensaje", "¡La proforma no se encuentra registrada en la Base de Datos!");
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<Proforma>(proforma, HttpStatus.OK);
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
    @PostMapping(value = "/proformas")
    public ResponseEntity<?> create(@RequestBody Proforma proforma) {

        Proforma newProforma = null;
        Estado estado = null;
        Map<String, Object> response = new HashMap<>();

        try {
            estado = estadoService.findByEstado("activo".toUpperCase());
            proforma.setEstado(estado);
            newProforma = proformaService.save(proforma);
        } catch(DataAccessException e) {
            response.put("mensaje", "¡Ha ocurrido un error en la Base de Datos!");
            response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (newProforma == null) {
            response.put("mensaje", "No fué posible registrar la nueva proforma");
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.BAD_REQUEST);
        }

        response.put("mensaje", "Registro llevado a cabo con éxito");
        response.put("proforma", newProforma);
        return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
    }

    @Secured(value = "ROLE_ADMIN")
    @DeleteMapping(value = "/proformas/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {

        Proforma proforma = null;
        Map<String, Object> response = new HashMap<>();

        try {
            proforma = proformaService.findProforma(id);
            proformaService.delete(id);
        } catch(DataAccessException e) {
            response.put("mensaje", "¡Ha ocurrido un error en la Base de Datos!");
            response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (proforma == null) {
            response.put("mensaje", "¡Proforma no se encuentra registrada en la Base de Datos!");
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
        }

        response.put("mensaje", "proforma eliminada con éxito");
        response.put("proforma", proforma);
        return new ResponseEntity<Map<String, Object>>(response, HttpStatus.OK);
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
    @GetMapping("/proformas/get-listado-sp/get")
    public ResponseEntity<?> getProformasPorFecha(@RequestParam(value = "date1", required = false) String fechaIni,
                                                  @RequestParam(value = "date2", required = false) String fechaFin
    ) {
        Date date1;
        Date date2;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        List<Proforma> proformas = new ArrayList<>();
        Map<String, Object> response = new HashMap<>();

        try {
            if (fechaIni != null && fechaFin != null) {
                date1 = format.parse(fechaIni);
                date2 = format.parse(fechaFin);
                proformas = proformaService.proformasPorFecha(date1, date2);
                for (Proforma proforma : proformas) {
                    System.out.println(proforma);
                }
            } else {
                System.out.println("No hay nada");
            }
        } catch(DataAccessException e) {
            response.put("mensaje", "¡Error en la base de datos!");
            response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch(ParseException e) {
            response.put("mensaje", "¡Error al intentar convertir una fecha con formato inválido");
            response.put("error", e.getMessage().concat(": ").concat(e.getCause().getMessage()));
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (proformas == null) {
            response.put("mensaje", "No existen proformas emitidas que coincidan con las fechas ingresadas.");
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<List<Proforma>>(proformas, HttpStatus.OK);
    }


    /**
     * metodos controlador que permiten devolver un pdf con la información solicitada por
     * los usuarios desde el frontend.
     * */

    @GetMapping("/proformas/generate/{id}")
    public void showProforma(@PathVariable("id") Long idproforma, HttpServletResponse httpServletResponse)
            throws JRException, SQLException, FileNotFoundException {

        try {
            byte[] bytesFactura = proformaService.showProforma(idproforma);
            ByteArrayOutputStream out = new ByteArrayOutputStream(bytesFactura.length);
            out.write(bytesFactura, 0, bytesFactura.length);

            httpServletResponse.setContentType("application/pdf");
            httpServletResponse.addHeader("Content-Disposition", "inline; filename=bill-" + idproforma + ".pdf");

            OutputStream os;

            os = httpServletResponse.getOutputStream();
            out.writeTo(os);
            os.flush();
            os.close();
        } catch (IOException e) {
            // new ServletException(e);
            e.printStackTrace();
        }
    }
}
