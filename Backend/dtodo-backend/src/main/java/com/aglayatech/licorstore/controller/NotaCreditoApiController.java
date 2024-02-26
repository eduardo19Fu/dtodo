package com.aglayatech.licorstore.controller;

import com.aglayatech.licorstore.model.*;
import com.aglayatech.licorstore.service.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = {"http://localhost:4200", "https://dtodojalapa.xyz", "http://dtodojalapa.xyz"})
@RestController
@RequestMapping(value = "/api")
public class NotaCreditoApiController {

    private final static Logger logger = LoggerFactory.getLogger(NotaCreditoApiController.class);

    @Autowired
    private INotaCreditoService notaCreditoService;

    @Autowired
    private IEstadoService estadoService;

    @Autowired
    private IUsuarioService usuarioService;

    @Autowired
    private IProductoService serviceProducto;

    @Autowired
    private IMovimientoProductoService serviceMovimiento;

    @GetMapping(value = "/notas-credito")
    public List<NotaCredito> getNotasCredito() {
        return notaCreditoService.findNotas();
    }

    @GetMapping(value = "/notas-credito/{id}")
    public ResponseEntity<?> getNotaCredito(@PathVariable("id") Integer idnota) {

        Map<String, Object> response = new HashMap<>();
        NotaCredito notaCredito = null;

        try {
            notaCredito = notaCreditoService.findNota(idnota);
        } catch(DataAccessException e) {
            logger.error("Error durante el consumo del API => {}", e.getMessage());
            response.put("mensaje", "¡Ha ocurrido un error en la base de datos!");
            response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if(notaCredito == null) {
            logger.warn("No se pudo localizar la nota con ID: {}", idnota);
            response.put("mensaje", "¡La nota de credito no se encuentra registrada en la base de datos!");
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<NotaCredito>(notaCredito, HttpStatus.OK);
    }

    @GetMapping(value = "/notas-credito/activas")
    public ResponseEntity<?> getNotasCreditoActivas() {

        Map<String, Object> response = new HashMap<>();
        Estado estado = null;
        List<NotaCredito> notasCreditoActivas = new ArrayList<>();

        try{
            estado = estadoService.findById(1);
            logger.debug("Estado obtenido: {}", estado);
            notasCreditoActivas = notaCreditoService.findNotasActivas(estado);
            logger.debug("Listado de Notas de Credito activas: {}", notasCreditoActivas);
        } catch(DataAccessException e) {
            logger.error("Error durante el consumo del API => {}", e.getMessage());
            response.put("mensaje", "¡Ha ocurrido un error en la base de datos!");
            response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<List<NotaCredito>>(notasCreditoActivas, HttpStatus.OK);
    }

    @PostMapping(value = "/notas-credito")
    public ResponseEntity<?> create(@RequestBody NotaCredito notaCredito, BindingResult result) {

        Map<String, Object> response = new HashMap<>();
        NotaCredito newNotaCredito = null;

        if (result.hasErrors()) {
            List<String> errors = result.getFieldErrors().stream()
                    .map(err -> "El campo '".concat(err.getField().concat("' ")).concat(err.getDefaultMessage()))
                    .collect(Collectors.toList());

            response.put("errors", errors);
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.BAD_REQUEST);
        }

        try {
            Estado estado = estadoService.findById(1);
            logger.info("Estado obtenido -> {}", estado);
            notaCredito.setEstado(estado);
            logger.info("Nota de Credito a operar -> {}", notaCredito);
            newNotaCredito = notaCreditoService.save(notaCredito);
            logger.info("Nota de credito guardada en base de datos: {}", newNotaCredito);

            // Actualiza el stock de los productos que forman parte de cada una de las lineas de la factura
            for (NotaCreditoDetalle item : newNotaCredito.getItems()) {
                if (this.updateExistencias(item.getProducto(), item.getCantidad(), "nota".toUpperCase())){
                    this.movimiento(item.getProducto(), newNotaCredito, item.getCantidad(), "nota".toUpperCase());
                }
            }
        } catch(DataAccessException e) {
            logger.error("Error durante el consumo del API => {}", e.getMessage());
            response.put("mensaje", "¡Ha ocurrido un error en la base de datos!");
            response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (newNotaCredito == null) {
            logger.error("Nota de credito {}" + notaCredito + " no fue guardada en base de datos.");
            response.put("mensaje", "Nota de credito no fue creada en la base de datos.");
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.BAD_REQUEST);
        }

        response.put("mensaje", "¡Nota de crédtio guardad con éxito!");
        response.put("notaCredito", newNotaCredito);
        return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
    }

    /**
     * Método que realiza la actualización de productos dependiendo del tipo de operación que se esté realizando
     * en producción.
     * @param producto objeto del tipo Producto para realizar la operación.
     * @param cantidad variable de tipo entera que indica la cantidad de existencias del Producto a operar.
     * @param tipoMovimiento objeto de tipo String que indica el tipo de operación a realizarse
     * @return boolean
     * */
    private boolean updateExistencias(Producto producto, int cantidad, String tipoMovimiento) {
        Producto productoUpdated = new Producto();

        if (tipoMovimiento.equals("nota".toUpperCase())) {
            producto.setStock(producto.getStock() - cantidad);
        } else if (tipoMovimiento.equals("anulacion nota".toUpperCase())) {
            producto.setStock(producto.getStock() + cantidad);
        }

        productoUpdated = serviceProducto.save(producto);
        return (productoUpdated != null ? true : false);
    }

    /**
     * Método encargado de llevar a cabo el procesamiento del tipo de movimiento realizandose durante la operación de creación
     * de la nota de crédito.
     * @param producto objeto de tipo Producto
     * */
    private void movimiento(Producto producto, NotaCredito notaCredito, int cantidad, String tipoMovimiento) {

        MovimientoProducto movimiento = new MovimientoProducto();

        movimiento.setTipoMovimiento(tipoMovimiento);
        movimiento.setUsuario(notaCredito.getUsuario());
        movimiento.setProducto(producto);
        movimiento.setStockInicial(producto.getStock());
        movimiento.setCantidad(cantidad);

        serviceMovimiento.save(movimiento);
    }

}
