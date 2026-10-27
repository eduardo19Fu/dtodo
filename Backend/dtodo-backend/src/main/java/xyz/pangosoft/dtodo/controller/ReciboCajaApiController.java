package xyz.pangosoft.dtodo.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import xyz.pangosoft.dtodo.dto.CrearReciboCajaDto;
import xyz.pangosoft.dtodo.dto.ReciboCajaDto;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.service.IReciboCajaDocumentoService;
import xyz.pangosoft.dtodo.service.IReciboCajaService;

/** Recibos de caja: los emiten el administrador y el cobrador; solo el administrador anula. */
@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz" })
@RestController
@RequestMapping("/api/recibos")
@RequiredArgsConstructor
@Slf4j
public class ReciboCajaApiController {

    private static final int TAMANIO_MAXIMO = 100;

    private final IReciboCajaService reciboService;
    private final IReciboCajaDocumentoService documentoService;

    @Secured({ "ROLE_ADMIN", "ROLE_COBRADOR" })
    @GetMapping("/page/{page}")
    public ResponseEntity<Page<ReciboCajaDto>> listar(@PathVariable("page") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer idSucursal,
            @RequestParam(required = false) Integer idCliente,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(defaultValue = "false") boolean incluirAnulados) {
        int tamanio = Math.max(1, Math.min(size, TAMANIO_MAXIMO));
        return ResponseEntity.ok(reciboService.listar(idSucursal, idCliente, parsearFecha(desde), parsearFecha(hasta),
                incluirAnulados, PageRequest.of(Math.max(page, 0), tamanio)));
    }

    @Secured({ "ROLE_ADMIN", "ROLE_COBRADOR" })
    @GetMapping("/{idRecibo}")
    public ResponseEntity<ReciboCajaDto> buscar(@PathVariable("idRecibo") Long idRecibo) {
        return ResponseEntity.ok(reciboService.buscar(idRecibo));
    }

    @Secured({ "ROLE_ADMIN", "ROLE_COBRADOR" })
    @PostMapping
    public ResponseEntity<ReciboCajaDto> emitir(@RequestBody CrearReciboCajaDto solicitud) {
        log.info("Emitiendo un recibo de caja");
        return ResponseEntity.ok(reciboService.emitir(solicitud));
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/{idRecibo}/anular")
    public ResponseEntity<ReciboCajaDto> anular(@PathVariable("idRecibo") Long idRecibo, @RequestBody Map<String, String> cuerpo) {
        log.info("Anulando el recibo de caja {}", idRecibo);
        return ResponseEntity.ok(reciboService.anular(idRecibo, cuerpo.get("motivo")));
    }

    @Secured({ "ROLE_ADMIN", "ROLE_COBRADOR" })
    @GetMapping("/{idRecibo}/imprimir")
    public ResponseEntity<byte[]> imprimir(@PathVariable("idRecibo") Long idRecibo) {
        byte[] pdf = documentoService.imprimir(idRecibo);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"recibo_" + idRecibo + ".pdf\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentLength(pdf.length)
                .body(pdf);
    }

    private static LocalDate parsearFecha(String fecha) {
        if (fecha == null || fecha.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(fecha.trim());
        } catch (DateTimeParseException exception) {
            throw new BadRequestException("Las fechas deben usar el formato yyyy-MM-dd.", exception);
        }
    }
}
