package xyz.pangosoft.dtodo.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

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
import xyz.pangosoft.dtodo.dto.CrearDevolucionClienteDto;
import xyz.pangosoft.dtodo.dto.DevolucionClienteDto;
import xyz.pangosoft.dtodo.dto.PedidoDevolubleDto;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.service.IDevolucionClienteService;

/** Devoluciones de clientes sobre pedidos mayoristas: solo el administrador. */
@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz" })
@RestController
@RequestMapping("/api/devoluciones-cliente")
@RequiredArgsConstructor
@Slf4j
public class DevolucionClienteApiController {

    private static final int TAMANIO_MAXIMO = 100;

    private final IDevolucionClienteService devolucionService;

    @Secured("ROLE_ADMIN")
    @GetMapping("/page/{page}")
    public ResponseEntity<Page<DevolucionClienteDto>> listar(@PathVariable("page") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer idCliente,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {
        int tamanio = Math.max(1, Math.min(size, TAMANIO_MAXIMO));
        return ResponseEntity.ok(devolucionService.listar(idCliente, parsearFecha(desde), parsearFecha(hasta),
                PageRequest.of(Math.max(page, 0), tamanio)));
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/{idDevolucion}")
    public ResponseEntity<DevolucionClienteDto> buscar(@PathVariable("idDevolucion") Long idDevolucion) {
        return ResponseEntity.ok(devolucionService.buscar(idDevolucion));
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/clientes/{idCliente}/pedidos")
    public ResponseEntity<List<PedidoDevolubleDto>> pedidosDevolubles(@PathVariable("idCliente") Integer idCliente) {
        return ResponseEntity.ok(devolucionService.pedidosDevolubles(idCliente));
    }

    @Secured("ROLE_ADMIN")
    @PostMapping
    public ResponseEntity<DevolucionClienteDto> registrar(@RequestBody CrearDevolucionClienteDto solicitud) {
        log.info("Registrando una devolución de cliente");
        return ResponseEntity.ok(devolucionService.registrar(solicitud));
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
