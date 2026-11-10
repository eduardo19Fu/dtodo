package xyz.pangosoft.dtodo.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.pangosoft.dtodo.service.IReporteControlesService;

/**
 * Los listados que Dtodo llevaba a mano: control de cheques y despachados al crédito (administrador y cobrador) y el listado de
 * proformas para empaque (solo administrador).
 */
@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz" })
@RestController
@RequestMapping("/api/reportes/controles")
@RequiredArgsConstructor
@Slf4j
public class ReporteControlesApiController {

    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final IReporteControlesService reporteService;

    @Secured({ "ROLE_ADMIN", "ROLE_COBRADOR" })
    @GetMapping("/cheques")
    public ResponseEntity<byte[]> cheques(
            @RequestParam(required = false) Integer idSucursal,
            @RequestParam(required = false) String estado,
            @RequestParam String fechaInicio,
            @RequestParam String fechaFin,
            @RequestParam(defaultValue = "PDF") String formato) {
        log.info("Generando el control de cheques. estado={}, formato={}, periodo={}..{}", estado, formato, fechaInicio, fechaFin);
        return archivo(reporteService.controlDeCheques(idSucursal, estado, fechaInicio, fechaFin, formato), formato,
                "control_cheques_" + fechaInicio + "_" + fechaFin);
    }

    @Secured({ "ROLE_ADMIN", "ROLE_COBRADOR" })
    @GetMapping("/despachados-credito")
    public ResponseEntity<byte[]> despachadosAlCredito(
            @RequestParam(required = false) Integer idRuta,
            @RequestParam(defaultValue = "true") boolean soloConSaldo,
            @RequestParam String fechaInicio,
            @RequestParam String fechaFin,
            @RequestParam(defaultValue = "PDF") String formato) {
        log.info("Generando despachados al crédito. ruta={}, formato={}, periodo={}..{}", idRuta, formato, fechaInicio, fechaFin);
        return archivo(reporteService.despachadosAlCredito(idRuta, soloConSaldo, fechaInicio, fechaFin, formato), formato,
                "despachados_credito_" + fechaInicio + "_" + fechaFin);
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/listado-proformas")
    public ResponseEntity<byte[]> listadoDeProformas(
            @RequestParam(required = false) Integer idRuta,
            @RequestParam String fechaInicio,
            @RequestParam String fechaFin,
            @RequestParam(defaultValue = "PDF") String formato) {
        log.info("Generando el listado de proformas. ruta={}, formato={}, periodo={}..{}", idRuta, formato, fechaInicio, fechaFin);
        return archivo(reporteService.listadoDeProformas(idRuta, fechaInicio, fechaFin, formato), formato,
                "listado_proformas_" + fechaInicio + "_" + fechaFin);
    }

    private ResponseEntity<byte[]> archivo(byte[] contenido, String formato, String nombreBase) {
        boolean esPdf = !"XLSX".equalsIgnoreCase(formato);
        String nombre = nombreBase + (esPdf ? ".pdf" : ".xlsx");
        String disposition = (esPdf ? "inline" : "attachment") + "; filename=\"" + nombre + "\"";
        return ResponseEntity.ok()
                .contentType(esPdf ? MediaType.APPLICATION_PDF : XLSX_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        new String(disposition.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentLength(contenido.length)
                .body(contenido);
    }
}
