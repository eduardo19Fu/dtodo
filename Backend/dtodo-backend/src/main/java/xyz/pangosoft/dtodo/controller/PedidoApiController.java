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
import xyz.pangosoft.dtodo.dto.AvancePedidoDto;
import xyz.pangosoft.dtodo.dto.EntregaSolicitudDto;
import xyz.pangosoft.dtodo.dto.ReporteSelectorDto;
import xyz.pangosoft.dtodo.service.IEntregaPedidoService;
import xyz.pangosoft.dtodo.service.IPedidoDocumentoService;
import xyz.pangosoft.dtodo.service.IReporteSelectorService;
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
    private final IEntregaPedidoService entregaService;
    private final IReporteSelectorService selectorService;

    @Secured({ "ROLE_ADMIN", "ROLE_BODEGA", "ROLE_VENDEDOR", "ROLE_REPARTIDOR" })
    @GetMapping
    public ResponseEntity<List<PedidoDto>> listar(
            @RequestParam(required = false) Integer idRuta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin) {
        return ResponseEntity.ok(pedidoService.listar(idRuta, parsearEstado(estado), parsearFecha(fechaInicio),
                parsearFecha(fechaFin)));
    }

    @Secured({ "ROLE_ADMIN", "ROLE_BODEGA", "ROLE_VENDEDOR", "ROLE_REPARTIDOR" })
    @GetMapping("/{idProforma}")
    public ResponseEntity<PedidoDetalleDto> detalle(@PathVariable("idProforma") Long idProforma) {
        return ResponseEntity.ok(pedidoService.detalle(idProforma));
    }

    @Secured({ "ROLE_ADMIN", "ROLE_BODEGA" })
    @PostMapping("/{idProforma}/avanzar")
    public ResponseEntity<PedidoDetalleDto> avanzar(@PathVariable("idProforma") Long idProforma,
            @RequestBody(required = false) AvancePedidoDto cuerpo) {
        log.info("Avanzando el pedido {}", idProforma);
        return ResponseEntity.ok(pedidoService.avanzar(idProforma, cuerpo == null ? null : cuerpo.getResponsable(),
                cuerpo == null ? null : cuerpo.getIdRepartidor()));
    }

    /** Los repartidores activos, para elegir el piloto que lleva el pedido. */
    @Secured({ "ROLE_ADMIN", "ROLE_BODEGA" })
    @GetMapping("/repartidores")
    public ResponseEntity<List<ReporteSelectorDto>> repartidores() {
        return ResponseEntity.ok(pedidoService.repartidores());
    }

    @Secured({ "ROLE_ADMIN", "ROLE_BODEGA" })
    @PostMapping("/{idProforma}/repartidor")
    public ResponseEntity<PedidoDetalleDto> asignarRepartidor(@PathVariable("idProforma") Long idProforma,
            @RequestBody AvancePedidoDto cuerpo) {
        log.info("Cambiando el piloto del pedido {}", idProforma);
        return ResponseEntity.ok(pedidoService.asignarRepartidor(idProforma, cuerpo.getIdRepartidor()));
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/{idProforma}/retroceder")
    public ResponseEntity<PedidoDetalleDto> retroceder(@PathVariable("idProforma") Long idProforma,
            @RequestBody Map<String, String> cuerpo) {
        log.info("Regresando de etapa el pedido {}", idProforma);
        return ResponseEntity.ok(pedidoService.retroceder(idProforma, cuerpo.get("motivo")));
    }

    /** Sucursales entre las que se elige de cuál se descuenta el inventario al entregar. */
    @Secured({ "ROLE_ADMIN", "ROLE_BODEGA", "ROLE_VENDEDOR" })
    @GetMapping("/sucursales")
    public ResponseEntity<List<ReporteSelectorDto>> sucursales() {
        return ResponseEntity.ok(selectorService.listarSucursales());
    }

    @Secured({ "ROLE_ADMIN", "ROLE_BODEGA", "ROLE_VENDEDOR", "ROLE_REPARTIDOR" })
    @PostMapping("/{idProforma}/entregas")
    public ResponseEntity<PedidoDetalleDto> registrarEntrega(@PathVariable("idProforma") Long idProforma,
            @RequestBody EntregaSolicitudDto solicitud) {
        log.info("Registrando una entrega del pedido {}", idProforma);
        return ResponseEntity.ok(entregaService.registrar(idProforma, solicitud));
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/{idProforma}/entregas/{idEntrega}/anular")
    public ResponseEntity<PedidoDetalleDto> anularEntrega(@PathVariable("idProforma") Long idProforma,
            @PathVariable("idEntrega") Long idEntrega, @RequestBody Map<String, String> cuerpo) {
        log.info("Anulando la entrega {} del pedido {}", idEntrega, idProforma);
        return ResponseEntity.ok(entregaService.anular(idProforma, idEntrega, cuerpo.get("motivo")));
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/{idProforma}/cerrar")
    public ResponseEntity<PedidoDetalleDto> cerrarConLoEntregado(@PathVariable("idProforma") Long idProforma,
            @RequestBody Map<String, String> cuerpo) {
        log.info("Cerrando con lo entregado el pedido {}", idProforma);
        return ResponseEntity.ok(entregaService.cerrarConLoEntregado(idProforma, cuerpo.get("motivo")));
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

    /** Un solo PDF con la nota de envío de cada ruta que tenga pedidos por cargar o que salieron en la fecha. */
    @Secured({ "ROLE_ADMIN", "ROLE_BODEGA" })
    @GetMapping("/nota-envio-todas")
    public ResponseEntity<byte[]> notaEnvioTodas(@RequestParam(required = false) String fecha) {
        return pdf(documentoService.notaEnvioTodasLasRutas(fecha), "notas_envio_todas_las_rutas");
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
