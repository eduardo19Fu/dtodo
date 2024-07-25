package com.aglayatech.licorstore.controller;

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

import javax.servlet.http.HttpServletResponse;

import com.aglayatech.licorstore.model.Factura;
import com.aglayatech.licorstore.service.ICertificadorService;
import com.aglayatech.licorstore.service.ICorrelativoService;
import com.aglayatech.licorstore.service.IEmisorService;
import com.aglayatech.licorstore.service.IEstadoService;
import com.aglayatech.licorstore.service.IFacturaService;
import com.aglayatech.licorstore.service.IMovimientoProductoService;
import com.aglayatech.licorstore.service.IProductoService;
import com.aglayatech.licorstore.service.ITipoFacturaService;
import com.aglayatech.licorstore.service.IUsuarioService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.sf.jasperreports.engine.JRException;

@CrossOrigin(origins = {"http://localhost:4200", "https://dtodojalapa.xyz", "http://dtodojalapa.xyz"})
@RestController
@RequestMapping(value = {"/api"})
@RequiredArgsConstructor
@Slf4j
public class FacturaApiController {

    private final IFacturaService serviceFactura;

    private final IProductoService serviceProducto;

    private final IEstadoService serviceEstado;

    private final ICorrelativoService serviceCorrelativo;

    private final IMovimientoProductoService serviceMovimiento;

    private final IUsuarioService serviceUsuario;

    // Inyeccion para capturar el Emisor del Regimen FEL
    private final IEmisorService serviceEmisor;

    // Inyeccion para capturar el Certificador del Regimen FEL
    private final ICertificadorService serviceCertificador;

    private final ITipoFacturaService serviceTipoFactura;

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
    @GetMapping(value = "/facturas")
    public List<Factura> index() {
        return this.serviceFactura.findAll();
    }

    @GetMapping(value = "/facturas/page/{page}")
    public Page<Factura> index(@PathVariable("page") Integer page) {
        return this.serviceFactura.findAll(PageRequest.of(page, 5));
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
    @GetMapping(value = "/facturas/cantidad-ventas")
    public ResponseEntity<?> cantidadVentas() {
        Integer cantidadVentas = 0;
        Map<String, Object> response = new HashMap<>();

        try {
            cantidadVentas = this.serviceFactura.totalVentas();
        } catch (DataAccessException e) {
            response.put("mensaje", "¡Error en la base de datos!");
            response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<Integer>(cantidadVentas, HttpStatus.OK);
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
    @GetMapping(value = "/facturas/factura/{id}")
    public ResponseEntity<?> showFactura(@PathVariable("id") Long idfactura) {

        Factura factura = null;
        Map<String, Object> response = new HashMap<>();

        try {
            factura = serviceFactura.findFactura(idfactura);
        } catch (DataAccessException e) {
            response.put("mensaje", "¡Error en la base de datos!");
            response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (factura == null) {
            response.put("mensaje", "¡La factura con id ".concat(idfactura.toString()).concat(" no existe en la base de datos!"));
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<Factura>(factura, HttpStatus.OK);
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
    @GetMapping(value = "/facturas/get-by-fecha")
    public ResponseEntity<?> getFacturasPorFecha(@RequestParam("fechaIni") String fechaIni, @RequestParam("fechaFin") String fechaFin) {

        Date date1;
        Date date2;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        List<Factura> facturas = new ArrayList<>();
        Map<String, Object> response = new HashMap<>();

        try {
            if (fechaIni != null && fechaFin != null) {
                date1 = format.parse(fechaIni);
                date2 = format.parse(fechaFin);
                facturas = this.serviceFactura.facturasPorFecha(date1, date2);
                for (Factura factura : facturas) {
                    System.out.println(factura);
                }
            } else {
                System.out.println("No hay nada");
            }
        } catch (DataAccessException e) {
            response.put("mensaje", "¡Error en la base de datos!");
            response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ParseException e) {
            response.put("mensaje", "¡Error en la base de datos!");
            response.put("error", e.getMessage().concat(": ").concat(e.getCause().getMessage()));
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (facturas == null) {
            response.put("mensaje", "No existen facturas emitidas que coincidan con las fechas ingresadas.");
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<List<Factura>>(facturas, HttpStatus.OK);
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
    @GetMapping("/facturas/get-listado-sp/get")
    public ResponseEntity<?> getFacturasSP(@RequestParam(required = false) String fechaIni,
                                           @RequestParam(required = false) String fechaFin) {
        Map<String, Object> response = new HashMap<>();
        List<Factura> facturas = new ArrayList<>();

        Date date1;
        Date date2;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        try {
            date1 = format.parse(fechaIni);
            date2 = format.parse(fechaFin);

            facturas = this.serviceFactura.facturasPorFecha(date1, date2);
        } catch (DataAccessException e) {
            response.put("mensaje", "¡Ha ocurrido un error en la Base de Datos!");
            response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        if(facturas.size() <= 0) {
            response.put("mensaje", "No se ha podido encontrar ninguna factura en el rango de fechas indicado.");
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<List<Factura>>(facturas, HttpStatus.OK);
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
    @GetMapping("/facturas/get-by-correlativo/{correlativo}")
    public ResponseEntity<?> buscarCorrelativo(@PathVariable("correlativo") Long correlativo) {

        Map<String, Object> response = new HashMap<>();
        Factura factura = null;

        try {
            factura = this.serviceFactura.findFacturaCorrelativo(correlativo);
        } catch(DataAccessException e) {
            response.put("mensaje", "¡Ha ocurrido un error en la Base de Datos!");
            response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if(factura == null) {
            response.put("mensaje", "No existe la factura con el correlativo: ".concat(correlativo.toString()));
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<Factura>(factura, HttpStatus.OK);
    }

    /**** Nuevas Implementaciones de creación de factura y anulación *****/
    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
    @PostMapping(value = "/facturas/createV2")
    public ResponseEntity<Factura> createV2(@RequestBody Factura factura) {
        log.info("********** Registrar Factura Versión 2 **********");
        Factura facturaCreated = serviceFactura.facturaFel(factura);
        return ResponseEntity.status(HttpStatus.CREATED).body(facturaCreated);
    }

    @Secured(value = {"ROLE_COBRADOR", "ROLE_ADMIN"})
    @PutMapping(value = "/facturas/cancelV2/{idusuario}")
    public ResponseEntity<Object> cancelV2(@RequestBody Factura factura, @PathVariable("idusuario") Integer idusuario) {
        log.info("********** Anulando Factura {} Versión 2 **********", factura.getCertificacionSat());
        Factura voidFactura = serviceFactura.anularFacturaFel(factura.getIdFactura(), idusuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(voidFactura);
    }

    /*************** PDF REPORTS CONTROLLERS ********************/

    // CONTROLADOR DE FACTURA
    @GetMapping(value = "/facturas/generate/{id}")
    public void generateBill(@PathVariable("id") Long idfactura, HttpServletResponse httpServletResponse)
            throws JRException, SQLException, FileNotFoundException {


        try {
            byte[] bytesFactura = serviceFactura.showBill(idfactura);
            ByteArrayOutputStream out = new ByteArrayOutputStream(bytesFactura.length);
            out.write(bytesFactura, 0, bytesFactura.length);

            httpServletResponse.setContentType("application/pdf");
            httpServletResponse.addHeader("Content-Disposition", "inline; filename=bill-" + idfactura + ".pdf");

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

    // CONTROLADOR VENTAS DIARIAS
    @GetMapping(value = "/facturas/daily-sales")
    public void dailySales(@RequestParam("usuario") String usuario, @RequestParam("fecha") String fecha, HttpServletResponse httpServletResponse)
            throws FileNotFoundException, JRException, SQLException, ParseException {

        Date fechaBusqueda;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        fechaBusqueda = format.parse(fecha);
        Integer idusuario = Integer.parseInt(usuario);

        byte[] bytesDailySalesReport = serviceFactura.resportDailySales(idusuario, fechaBusqueda);
        ByteArrayOutputStream out = new ByteArrayOutputStream(bytesDailySalesReport.length);
        out.write(bytesDailySalesReport, 0, bytesDailySalesReport.length);

        httpServletResponse.setContentType("application/pdf");
        httpServletResponse.addHeader("Content-Disposition", "inline; filename=daily-sales.pdf");

        OutputStream os;
        try {
            os = httpServletResponse.getOutputStream();
            out.writeTo(os);
            os.flush();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
