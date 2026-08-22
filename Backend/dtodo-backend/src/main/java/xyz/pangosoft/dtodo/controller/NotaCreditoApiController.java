package xyz.pangosoft.dtodo.controller;

import xyz.pangosoft.dtodo.dto.NotaCreditoDto;
import xyz.pangosoft.dtodo.dto.NotaCreditoDetalleDto;
import xyz.pangosoft.dtodo.model.NotaCredito;
import xyz.pangosoft.dtodo.model.enums.EstadoNotaCreditoEnum;
import xyz.pangosoft.dtodo.service.INotaCreditoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

@RestController
@RequestMapping(value = "/api")
@RequiredArgsConstructor
@Slf4j
public class NotaCreditoApiController {

    private final INotaCreditoService notaCreditoService;

    @GetMapping(value = "/notas-credito")
    public ResponseEntity<List<NotaCreditoDto>> getNotasCredito() {
        log.info("Buscando listado de Notas de Credito");
        return ResponseEntity.ok(notaCreditoService.findNotas());
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO"})
    @GetMapping(value = "/notas-credito-dto/ultimas/{page}")
    public ResponseEntity<Page<NotaCreditoDto>> getUltimasNotas(
            @PathVariable("page") Integer page,
            @RequestParam(value = "filtro", defaultValue = "") String filtro,
            @RequestParam(value = "size", defaultValue = "5") Integer size,
            @RequestParam(value = "orden", defaultValue = "fecha") String orden,
            @RequestParam(value = "direccion", defaultValue = "desc") String direccion) {
        return ResponseEntity.ok(notaCreditoService.findUltimas(
                filtro, PageRequest.of(page, size, obtenerOrden(orden, direccion))));
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO"})
    @GetMapping(value = "/notas-credito-dto/fechas/{page}")
    public ResponseEntity<Page<NotaCreditoDto>> getNotasPorFechas(
            @PathVariable("page") Integer page,
            @RequestParam("fechaIni") String fechaIni,
            @RequestParam("fechaFin") String fechaFin,
            @RequestParam(value = "filtro", defaultValue = "") String filtro,
            @RequestParam(value = "size", defaultValue = "5") Integer size,
            @RequestParam(value = "orden", defaultValue = "fecha") String orden,
            @RequestParam(value = "direccion", defaultValue = "desc") String direccion) {
        return ResponseEntity.ok(notaCreditoService.findPorFechas(
                fechaIni, fechaFin, filtro, PageRequest.of(page, size, obtenerOrden(orden, direccion))));
    }

    private Sort obtenerOrden(String orden, String direccion) {
        Sort.Direction sentido = "desc".equalsIgnoreCase(direccion)
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        switch (orden == null ? "" : orden.toLowerCase()) {
            case "id": return Sort.by(sentido, "idNotaCredito");
            case "total": return Sort.by(sentido, "total");
            case "emisor": return Sort.by(sentido, "usuario.usuario");
            case "cliente": return Sort.by(sentido, "cliente.nombre");
            case "origen": return Sort.by(sentido, "tipoDocumentoOrigen");
            case "referencia": return Sort.by(sentido, "correlativoFacturaSat", "noProforma");
            case "entrega": return Sort.by(sentido, "fechaEntregaEstimada");
            case "estado": return Sort.by(sentido, "estado");
            default: return Sort.by(sentido, "fechaCreacion");
        }
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO"})
    @GetMapping(value = "/notas-credito/activas")
    public ResponseEntity<List<NotaCreditoDto>> getNotasCreditoActivas() {
        log.info("Buscando listado de Notas de Credito con estado ENTREGA_PENDIENTE");
        return ResponseEntity.ok(notaCreditoService.findNotasActivas(EstadoNotaCreditoEnum.ENTREGA_PENDIENTE));
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO"})
    @GetMapping(value = "/notas-credito/{id}")
    public ResponseEntity<NotaCredito> getNotaCredito(@PathVariable("id") Long idnota) {
        log.info("Buscando Nota de Credito con ID: {}", idnota);
        return ResponseEntity.ok(notaCreditoService.findNota(idnota));
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO"})
    @GetMapping(value = "/notas-credito/{id}/detalle")
    public ResponseEntity<NotaCreditoDetalleDto> getDetalleNotaCredito(@PathVariable("id") Long idnota) {
        log.info("Buscando detalle DTO de Nota de Credito con ID: {}", idnota);
        return ResponseEntity.ok(notaCreditoService.findDetalle(idnota));
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO"})
    @PostMapping(value = "/notas-credito")
    public ResponseEntity<NotaCredito> create(@RequestBody NotaCredito notaCredito) {
        log.info("********** Registrando nueva nota de credito **********");
        return ResponseEntity.status(HttpStatus.CREATED).body(notaCreditoService.save(notaCredito, EstadoNotaCreditoEnum.ENTREGA_PENDIENTE));
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
    @PutMapping(value = "/notas-credito/anular/{id}")
    public ResponseEntity<NotaCredito> voidNota(@PathVariable("id") Long idnota) {
        log.info("Anulando Nota de Credito: {}", idnota);
        NotaCredito notaCredito = notaCreditoService.findNota(idnota);
        return ResponseEntity.ok(notaCreditoService.save(notaCredito, EstadoNotaCreditoEnum.ANULADO));
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO"})
    @PutMapping(value = "/notas-credito/entregar/{id}")
    public ResponseEntity<NotaCredito> despacharNota(@PathVariable("id") Long idnota) {
        log.info("Entregando producto de Nota de Credito: {}", idnota);
        NotaCredito notaCredito = notaCreditoService.findNota(idnota);
        return ResponseEntity.ok(notaCreditoService.save(notaCredito, EstadoNotaCreditoEnum.ENTREGADO));
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO"})
    @GetMapping(value = "/notas-credito/generate/{id}")
    public ResponseEntity<byte[]> generateReport(@PathVariable("id") Long idnota) {
        log.info("Generando reporte PDF de Nota de Credito: {}", idnota);
        byte[] bytesReport = notaCreditoService.generateReport(idnota);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(bytesReport);
    }

}
