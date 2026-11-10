package xyz.pangosoft.dtodo.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import xyz.pangosoft.dtodo.dto.ChequeDto;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.service.IChequeService;

/** Control de cheques: los maneja el administrador y el cobrador (este último solo los de su sucursal). */
@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz" })
@RestController
@RequestMapping("/api/cheques")
@RequiredArgsConstructor
@Slf4j
public class ChequeApiController {

    private static final int TAMANIO_MAXIMO = 100;

    private final IChequeService chequeService;

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
