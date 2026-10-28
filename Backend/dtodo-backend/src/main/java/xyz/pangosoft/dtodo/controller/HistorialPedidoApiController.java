package xyz.pangosoft.dtodo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import xyz.pangosoft.dtodo.dto.HistorialEventoDto;
import xyz.pangosoft.dtodo.service.IHistorialPedidoService;

/**
 * Línea de tiempo de un pedido. La ven el administrador, bodega y el vendedor (solo sus pedidos, lo hace valer el servidor);
 * el repartidor no, porque trae el historial de auditoría.
 */
@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz" })
@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class HistorialPedidoApiController {

    private final IHistorialPedidoService historialService;

    @Secured({ "ROLE_ADMIN", "ROLE_BODEGA", "ROLE_VENDEDOR" })
    @GetMapping("/{idProforma}/historial")
    public ResponseEntity<List<HistorialEventoDto>> historial(@PathVariable("idProforma") Long idProforma) {
        return ResponseEntity.ok(historialService.historial(idProforma));
    }
}
