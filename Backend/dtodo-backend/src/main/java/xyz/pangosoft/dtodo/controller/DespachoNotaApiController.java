package xyz.pangosoft.dtodo.controller;

import xyz.pangosoft.dtodo.dto.DespachoNotaDto;
import xyz.pangosoft.dtodo.dto.DespachoRequest;
import xyz.pangosoft.dtodo.service.IDespachoNotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping(value = "/api")
@RequiredArgsConstructor
@Slf4j
public class DespachoNotaApiController {

    private final IDespachoNotaService despachoNotaService;

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO"})
    @GetMapping(value = "/despachos-nota/{idNota}")
    public ResponseEntity<List<DespachoNotaDto>> getDespachos(@PathVariable("idNota") Long idNota) {
        log.info("Buscando despachos de nota de credito: {}", idNota);
        return ResponseEntity.ok(despachoNotaService.findByNotaCredito(idNota));
    }

    /**
     * Registra un despacho. El cuerpo encapsula las líneas a despachar y la
     * contraseña del usuario en sesión, que el servicio valida antes de
     * persistir; si no coincide se responde 422 y no se escribe nada.
     * El usuario responsable se toma del {@link Principal} del token, no del
     * cuerpo de la petición.
     */
    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO"})
    @PostMapping(value = "/despachos-nota/{idNota}")
    public ResponseEntity<List<DespachoNotaDto>> registrarDespacho(
            @PathVariable("idNota") Long idNota,
            @RequestBody DespachoRequest request,
            Principal principal) {
        log.info("Registrando despacho para nota de credito: {} por el usuario: {}", idNota, principal.getName());
        List<DespachoNotaDto> despachosRegistrados =
                despachoNotaService.registrarDespacho(idNota, request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(despachosRegistrados);
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO"})
    @GetMapping(value = "/despachos-nota/comprobante/{idEvento}")
    public ResponseEntity<byte[]> generateComprobante(@PathVariable("idEvento") String idEvento) {
        log.info("Generando comprobante PDF de despacho: {}", idEvento);
        byte[] bytesReport = despachoNotaService.generateComprobanteDespacho(idEvento);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(bytesReport);
    }
}
