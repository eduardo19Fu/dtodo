package xyz.pangosoft.dtodo.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.pangosoft.dtodo.service.IReporteMayoristasService;

/** Reportes de clientes mayoristas (solo administrador). */
@RestController
@RequestMapping("/api/reportes/mayoristas")
@RequiredArgsConstructor
@Slf4j
public class ReporteMayoristasApiController {

    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final IReporteMayoristasService reporteMayoristasService;

    @Secured("ROLE_ADMIN")
    @GetMapping("/clientes-ruta")
    public ResponseEntity<byte[]> clientesPorRuta(
            @RequestParam(required = false) Integer idRuta,
            @RequestParam(defaultValue = "PDF") String formato) {
        log.info("Generando clientes mayoristas por ruta. ruta={}, formato={}", idRuta, formato);
        byte[] reporte = reporteMayoristasService.clientesPorRuta(idRuta, formato);
        return archivo(reporte, formato, "mayoristas_clientes_ruta");
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/clientes-sin-compras")
    public ResponseEntity<byte[]> clientesSinCompras(
            @RequestParam(required = false) Integer idRuta,
            @RequestParam String fechaInicio,
            @RequestParam String fechaFin,
            @RequestParam(defaultValue = "PDF") String formato) {
        log.info("Generando clientes mayoristas sin compras. ruta={}, formato={}, periodo={}..{}",
                idRuta, formato, fechaInicio, fechaFin);
        byte[] reporte = reporteMayoristasService.clientesSinCompras(idRuta, fechaInicio, fechaFin, formato);
        return archivo(reporte, formato, "mayoristas_sin_compras_" + fechaInicio + "_" + fechaFin);
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/entregado")
    public ResponseEntity<byte[]> entregadoPorRutaYVendedor(
            @RequestParam(required = false) Integer idRuta,
            @RequestParam(required = false) Integer idUsuario,
            @RequestParam String fechaInicio,
            @RequestParam String fechaFin,
            @RequestParam(defaultValue = "PDF") String formato) {
        log.info("Generando ventas entregadas por ruta y vendedor. ruta={}, vendedor={}, formato={}, periodo={}..{}",
                idRuta, idUsuario, formato, fechaInicio, fechaFin);
        byte[] reporte = reporteMayoristasService.entregadoPorRutaYVendedor(idRuta, idUsuario, fechaInicio, fechaFin, formato);
        return archivo(reporte, formato, "mayoristas_entregado_" + fechaInicio + "_" + fechaFin);
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/proformas-vendedor")
    public ResponseEntity<byte[]> proformasPorVendedor(
            @RequestParam(required = false) Integer idRuta,
            @RequestParam String fechaInicio,
            @RequestParam String fechaFin,
            @RequestParam(defaultValue = "PDF") String formato) {
        log.info("Generando proformas mayoristas por vendedor. ruta={}, formato={}, periodo={}..{}",
                idRuta, formato, fechaInicio, fechaFin);
        byte[] reporte = reporteMayoristasService.proformasPorVendedor(idRuta, fechaInicio, fechaFin, formato);
        return archivo(reporte, formato, "mayoristas_proformas_vendedor_" + fechaInicio + "_" + fechaFin);
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
