package xyz.pangosoft.dtodo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.pangosoft.dtodo.dto.AutorizacionProformaDto;
import xyz.pangosoft.dtodo.service.IAutorizacionProformaService;

/** Bandeja de autorizaciones de proformas mayoristas: solo la ve y la resuelve un administrador. */
@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz" })
@RestController
@RequestMapping(value = "/api/autorizaciones")
@RequiredArgsConstructor
@Slf4j
public class AutorizacionProformaApiController {

    private final IAutorizacionProformaService autorizacionService;

    @Secured(value = { "ROLE_ADMIN" })
    @GetMapping("/pendientes")
    public ResponseEntity<List<AutorizacionProformaDto>> pendientes() {
        return ResponseEntity.ok(autorizacionService.pendientes());
    }

    @Secured(value = { "ROLE_ADMIN" })
    @GetMapping("/vencidas")
    public ResponseEntity<List<AutorizacionProformaDto>> vencidas() {
        return ResponseEntity.ok(autorizacionService.vencidas());
    }

    @Secured(value = { "ROLE_ADMIN" })
    @PostMapping("/{idProforma}/aprobar")
    public ResponseEntity<Map<String, Object>> aprobar(@PathVariable("idProforma") Long idProforma,
            @RequestBody Map<String, String> cuerpo) {
        log.info("Aprobando el descuento de la proforma {}", idProforma);
        autorizacionService.aprobar(idProforma, cuerpo.get("motivo"));
        return ResponseEntity.ok(respuesta("El descuento de la proforma fue aprobado."));
    }

    @Secured(value = { "ROLE_ADMIN" })
    @PostMapping("/{idProforma}/rechazar")
    public ResponseEntity<Map<String, Object>> rechazar(@PathVariable("idProforma") Long idProforma,
            @RequestBody Map<String, String> cuerpo) {
        log.info("Rechazando el descuento de la proforma {}", idProforma);
        autorizacionService.rechazar(idProforma, cuerpo.get("motivo"));
        return ResponseEntity.ok(respuesta("El descuento de la proforma fue rechazado."));
    }

    @Secured(value = { "ROLE_ADMIN" })
    @PostMapping("/{idProforma}/autorizar-vencida")
    public ResponseEntity<Map<String, Object>> autorizarVencida(@PathVariable("idProforma") Long idProforma,
            @RequestBody Map<String, String> cuerpo) {
        log.info("Autorizando facturar la proforma vencida {}", idProforma);
        autorizacionService.autorizarVencida(idProforma, cuerpo.get("motivo"));
        return ResponseEntity.ok(respuesta("Se autorizó facturar la proforma vencida."));
    }

    private Map<String, Object> respuesta(String mensaje) {
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", mensaje);
        return respuesta;
    }
}
