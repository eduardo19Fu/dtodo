package xyz.pangosoft.dtodo.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.pangosoft.dtodo.dto.CobranzaDto;
import xyz.pangosoft.dtodo.service.ICobranzaService;
import xyz.pangosoft.dtodo.service.IReporteCobranzaService;

/** Cobranza de los clientes mayoristas: cuentas por cobrar por antigüedad y estados de cuenta. Administrador y cobrador. */
@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz" })
@RestController
@RequestMapping("/api/cobranza")
@RequiredArgsConstructor
@Slf4j
public class CobranzaApiController {

    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ICobranzaService cobranzaService;
    private final IReporteCobranzaService reporteService;

    @Secured({ "ROLE_ADMIN", "ROLE_COBRADOR" })
    @GetMapping("/saldos")
    public ResponseEntity<CobranzaDto> cuentasPorCobrar(@RequestParam(required = false) Integer idRuta,
            @RequestParam(defaultValue = "false") boolean soloVencidos) {
        return ResponseEntity.ok(cobranzaService.cuentasPorCobrar(idRuta, soloVencidos));
    }

    @Secured({ "ROLE_ADMIN", "ROLE_COBRADOR" })
    @GetMapping("/saldos/reporte")
    public ResponseEntity<byte[]> reporteCuentasPorCobrar(@RequestParam(required = false) Integer idRuta,
            @RequestParam(defaultValue = "false") boolean soloVencidos, @RequestParam(defaultValue = "PDF") String formato) {
        log.info("Generando el reporte de cuentas por cobrar. ruta={}, soloVencidos={}, formato={}", idRuta, soloVencidos, formato);
        byte[] contenido = reporteService.cuentasPorCobrar(idRuta, soloVencidos, formato);
        boolean esPdf = !"XLSX".equalsIgnoreCase(formato);
        return archivo(contenido, esPdf, "cuentas_por_cobrar");
    }

    @Secured({ "ROLE_ADMIN", "ROLE_COBRADOR" })
    @GetMapping("/clientes/{idCliente}/estado-cuenta")
    public ResponseEntity<byte[]> estadoDeCuenta(@PathVariable("idCliente") Integer idCliente) {
        log.info("Generando el estado de cuenta del cliente {}", idCliente);
        return archivo(reporteService.estadoDeCuenta(idCliente), true, "estado_cuenta_" + idCliente);
    }

    private ResponseEntity<byte[]> archivo(byte[] contenido, boolean esPdf, String nombreBase) {
        String disposition = (esPdf ? "inline" : "attachment") + "; filename=\"" + nombreBase + (esPdf ? ".pdf" : ".xlsx") + "\"";
        return ResponseEntity.ok()
                .contentType(esPdf ? MediaType.APPLICATION_PDF : XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        new String(disposition.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentLength(contenido.length)
                .body(contenido);
    }
}
