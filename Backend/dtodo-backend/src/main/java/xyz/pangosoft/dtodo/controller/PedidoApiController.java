package xyz.pangosoft.dtodo.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.pangosoft.dtodo.dto.PedidoDetalleDto;
import xyz.pangosoft.dtodo.dto.PedidoDto;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.model.enums.EstadoPedidoEnum;
import xyz.pangosoft.dtodo.service.IPedidoDocumentoService;
import xyz.pangosoft.dtodo.service.IPedidoService;

/**
 * Pedidos mayoristas. Los ve el administrador, bodega y el vendedor (solo los suyos); bodega y el administrador
 * avanzan las etapas y imprimen los documentos; solo el administrador regresa un pedido de etapa.
 */
@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz" })
@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
@Slf4j
public class PedidoApiController {

    private final IPedidoService pedidoService;
    private final IPedidoDocumentoService documentoService;

    @Secured({ "ROLE_ADMIN", "ROLE_BODEGA", "ROLE_VENDEDOR" })
    @GetMapping
    public ResponseEntity<List<PedidoDto>> listar(
            @RequestParam(required = false) Integer idRuta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin) {
        return ResponseEntity.ok(pedidoService.listar(idRuta, parsearEstado(estado), parsearFecha(fechaInicio),
                parsearFecha(fechaFin)));
    }

    @Secured({ "ROLE_ADMIN", "ROLE_BODEGA", "ROLE_VENDEDOR" })
    @GetMapping("/{idProforma}")
    public ResponseEntity<PedidoDetalleDto> detalle(@PathVariable("idProforma") Long idProforma) {
        return ResponseEntity.ok(pedidoService.detalle(idProforma));
    }

    @Secured({ "ROLE_ADMIN", "ROLE_BODEGA" })
    @PostMapping("/{idProforma}/avanzar")
    public ResponseEntity<PedidoDetalleDto> avanzar(@PathVariable("idProforma") Long idProforma,
            @RequestBody(required = false) Map<String, String> cuerpo) {
        log.info("Avanzando el pedido {}", idProforma);
        return ResponseEntity.ok(pedidoService.avanzar(idProforma, cuerpo == null ? null : cuerpo.get("responsable")));
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/{idProforma}/retroceder")
    public ResponseEntity<PedidoDetalleDto> retroceder(@PathVariable("idProforma") Long idProforma,
            @RequestBody Map<String, String> cuerpo) {
        log.info("Regresando de etapa el pedido {}", idProforma);
        return ResponseEntity.ok(pedidoService.retroceder(idProforma, cuerpo.get("motivo")));
    }

    @Secured({ "ROLE_ADMIN", "ROLE_BODEGA" })
    @GetMapping("/{idProforma}/picking")
    public ResponseEntity<byte[]> picking(@PathVariable("idProforma") Long idProforma) {
        return pdf(documentoService.picking(idProforma), "picking_" + idProforma);
    }

    @Secured({ "ROLE_ADMIN", "ROLE_BODEGA" })
    @GetMapping("/{idProforma}/empaque")
    public ResponseEntity<byte[]> empaque(@PathVariable("idProforma") Long idProforma) {
        return pdf(documentoService.empaque(idProforma), "empaque_" + idProforma);
    }

    @Secured({ "ROLE_ADMIN", "ROLE_BODEGA" })
    @GetMapping("/nota-envio")
    public ResponseEntity<byte[]> notaEnvio(@RequestParam Integer idRuta,
            @RequestParam(required = false) String fecha) {
        return pdf(documentoService.notaEnvio(idRuta, fecha), "nota_envio_ruta_" + idRuta);
    }

    private ResponseEntity<byte[]> pdf(byte[] contenido, String nombre) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nombre + ".pdf\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentLength(contenido.length)
                .body(contenido);
    }

    private static EstadoPedidoEnum parsearEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        try {
            return EstadoPedidoEnum.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("La etapa solicitada no es válida.", exception);
        }
    }

    private static LocalDate parsearFecha(String fecha) {
        if (fecha == null || fecha.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(fecha);
        } catch (DateTimeParseException exception) {
            throw new BadRequestException("Las fechas deben utilizar el formato yyyy-MM-dd.", exception);
        }
    }
}
