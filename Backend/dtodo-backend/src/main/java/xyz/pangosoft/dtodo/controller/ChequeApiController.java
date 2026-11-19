package xyz.pangosoft.dtodo.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
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
import xyz.pangosoft.dtodo.dto.ChequeDto;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.service.IChequeService;
import xyz.pangosoft.dtodo.service.impl.ChequeAdjuntoService;

/** Control de cheques: los maneja el administrador y el cobrador (este último solo los de su sucursal). */
@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz" })
@RestController
@RequestMapping("/api/cheques")
@RequiredArgsConstructor
@Slf4j
public class ChequeApiController {

    private static final int TAMANIO_MAXIMO = 100;

    private final IChequeService chequeService;
    private final ChequeAdjuntoService adjuntoService;

    @Secured({ "ROLE_ADMIN", "ROLE_COBRADOR" })
    @GetMapping("/page/{page}")
    public ResponseEntity<Page<ChequeDto>> listar(@PathVariable("page") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer idSucursal,
            @RequestParam(required = false) Integer idCliente,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {
        int tamanio = Math.max(1, Math.min(size, TAMANIO_MAXIMO));
        return ResponseEntity.ok(chequeService.listar(idSucursal, idCliente, estado, parsearFecha(desde), parsearFecha(hasta),
                PageRequest.of(Math.max(page, 0), tamanio)));
    }

    @Secured({ "ROLE_ADMIN", "ROLE_COBRADOR" })
    @PostMapping("/{idCheque}/estado")
    public ResponseEntity<ChequeDto> cambiarEstado(@PathVariable("idCheque") Long idCheque, @RequestBody Map<String, String> cuerpo) {
        log.info("Cambiando el estado del cheque {}", idCheque);
        return ResponseEntity.ok(chequeService.cambiarEstado(idCheque, cuerpo.get("estado"), cuerpo.get("motivo")));
    }

    @Secured({ "ROLE_ADMIN", "ROLE_COBRADOR" })
    @PostMapping(value = "/{idCheque}/adjunto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChequeDto> adjuntar(@PathVariable("idCheque") Long idCheque, @RequestPart("archivo") MultipartFile archivo) {
        log.info("Adjuntando el archivo del cheque {}", idCheque);
        return ResponseEntity.ok(adjuntoService.adjuntar(idCheque, archivo));
    }

    @Secured({ "ROLE_ADMIN", "ROLE_COBRADOR" })
    @GetMapping("/{idCheque}/adjunto")
    public ResponseEntity<byte[]> descargarAdjunto(@PathVariable("idCheque") Long idCheque) {
        ChequeAdjuntoService.Archivo archivo = adjuntoService.descargar(idCheque);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.getTipo()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(archivo.getNombre(),
                        java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .contentLength(archivo.getContenido().length)
                .body(archivo.getContenido());
    }

    @Secured({ "ROLE_ADMIN", "ROLE_COBRADOR" })
    @DeleteMapping("/{idCheque}/adjunto")
    public ResponseEntity<ChequeDto> quitarAdjunto(@PathVariable("idCheque") Long idCheque) {
        return ResponseEntity.ok(adjuntoService.quitar(idCheque));
    }

    static LocalDate parsearFecha(String fecha) {
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
