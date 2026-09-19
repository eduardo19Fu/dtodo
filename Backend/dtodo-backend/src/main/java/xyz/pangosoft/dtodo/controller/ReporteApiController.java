package xyz.pangosoft.dtodo.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.dto.UsuarioDto;
import xyz.pangosoft.dtodo.service.IReporteService;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@Slf4j
public class ReporteApiController {

    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final IReporteService reporteService;

    @Secured("ROLE_ADMIN")
    @GetMapping("/ventas/poliza-individual")
    public ResponseEntity<byte[]> polizaIndividual(
            @RequestParam Integer idSucursal,
            @RequestParam Integer idUsuario,
            @RequestParam String fechaInicio,
            @RequestParam String fechaFin) {
        log.info("Generando póliza individual. sucursal={}, usuario={}, periodo={}..{}",
                idSucursal, idUsuario, fechaInicio, fechaFin);
        byte[] reporte = reporteService.generarPolizaIndividual(
                idSucursal, idUsuario, fechaInicio, fechaFin);
        return archivo(reporte, MediaType.APPLICATION_PDF,
                nombrePeriodo("poliza_individual", fechaInicio, fechaFin, "pdf"), true);
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/ventas/poliza-general")
    public ResponseEntity<byte[]> polizaGeneral(
            @RequestParam Integer idSucursal,
            @RequestParam String fechaInicio,
            @RequestParam String fechaFin) {
        log.info("Generando póliza general. sucursal={}, periodo={}..{}",
                idSucursal, fechaInicio, fechaFin);
        byte[] reporte = reporteService.generarPolizaGeneral(idSucursal, fechaInicio, fechaFin);
        return archivo(reporte, MediaType.APPLICATION_PDF,
                nombrePeriodo("poliza_general", fechaInicio, fechaFin, "pdf"), true);
    }

    @Secured({"ROLE_ADMIN", "ROLE_INVENTARIO"})
    @GetMapping("/inventario/movimientos")
    public ResponseEntity<byte[]> movimientosInventario(
            @RequestParam(required = false) Integer idSucursal,
            @RequestParam String fechaInicio,
            @RequestParam String fechaFin,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {
        Integer sucursalEfectiva = resolverSucursal(idSucursal, jwt, authentication);
        log.info("Generando movimientos de inventario. sucursal={}, periodo={}..{}",
                sucursalEfectiva, fechaInicio, fechaFin);
        byte[] reporte = reporteService.generarMovimientosInventario(
                sucursalEfectiva, fechaInicio, fechaFin);
        return archivo(reporte, MediaType.APPLICATION_PDF,
                nombrePeriodo("movimientos_inventario", fechaInicio, fechaFin, "pdf"), true);
    }

    @Secured({"ROLE_ADMIN", "ROLE_INVENTARIO"})
    @GetMapping("/inventario/existencias")
    public ResponseEntity<byte[]> existencias(
            @RequestParam(required = false) Integer idSucursal,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {
        Integer sucursalEfectiva = resolverSucursal(idSucursal, jwt, authentication);
        log.info("Generando existencias. sucursal={}", sucursalEfectiva);
        byte[] reporte = reporteService.generarExistencias(sucursalEfectiva);
        return archivo(reporte, XLSX_MEDIA_TYPE,
                "existencias_sucursal_" + sucursalEfectiva + ".xlsx", false);
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/proformas/usuarios")
    public ResponseEntity<List<UsuarioDto>> usuariosProformas() {
        return ResponseEntity.ok(reporteService.listarUsuariosProformas());
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/proformas")
    public ResponseEntity<byte[]> proformas(
            @RequestParam(required = false) Integer idUsuario,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(defaultValue = "false") boolean todas) {
        log.info("Generando proformas. usuario={}, todas={}, periodo={}..{}",
                idUsuario, todas, fechaInicio, fechaFin);
        byte[] reporte = reporteService.generarProformas(
                idUsuario, fechaInicio, fechaFin, todas);
        String nombre = todas
                ? "proformas_historial.xlsx"
                : nombrePeriodo("proformas", fechaInicio, fechaFin, "xlsx");
        return archivo(reporte, XLSX_MEDIA_TYPE, nombre, false);
    }

    private Integer resolverSucursal(Integer solicitada, Jwt jwt, Authentication authentication) {
        Integer asignada = obtenerSucursalJwt(jwt);
        boolean esAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        if (esAdmin && solicitada != null) {
            return solicitada;
        }
        if (asignada == null) {
            throw new AccessDeniedException("El usuario no tiene una sucursal asignada.");
        }
        if (solicitada != null && !asignada.equals(solicitada)) {
            throw new AccessDeniedException("No tienes permiso para consultar otra sucursal.");
        }
        return asignada;
    }

    private Integer obtenerSucursalJwt(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        String claim = jwt.getClaimAsString("id_sucursal");
        if (claim == null || claim.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(claim);
        } catch (NumberFormatException exception) {
            throw new BadRequestException("La sucursal de la sesión no es válida.", exception);
        }
    }

    private ResponseEntity<byte[]> archivo(
            byte[] contenido,
            MediaType mediaType,
            String nombre,
            boolean inline) {
        String disposition = (inline ? "inline" : "attachment") + "; filename=\"" + nombre + "\"";
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        new String(disposition.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentLength(contenido.length)
                .body(contenido);
    }

    private String nombrePeriodo(String prefijo, String inicio, String fin, String extension) {
        return String.format("%s_%s_%s.%s", prefijo, inicio, fin, extension);
    }
}
