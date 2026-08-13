package xyz.pangosoft.dtodo.controller;

import java.util.List;

import xyz.pangosoft.dtodo.model.Factura;
import xyz.pangosoft.dtodo.dto.FacturaListadoDto;
import xyz.pangosoft.dtodo.dto.DetalleDocumentoDto;
import xyz.pangosoft.dtodo.service.IFacturaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = {"/api"})
@RequiredArgsConstructor
@Slf4j
public class FacturaApiController {

    private final IFacturaService serviceFactura;

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
    @GetMapping(value = "/facturas")
    public ResponseEntity<List<Factura>> index() {
        log.info("********** Buscando listado de facturas **********");
        return ResponseEntity.ok(serviceFactura.findAll());
    }

    @GetMapping(value = "/facturas/page/{page}")
    public ResponseEntity<Page<Factura>> index(@PathVariable("page") Integer page) {
        log.info("********** Buscando listado de facturas de pagina {} **********", page);
        return ResponseEntity.ok(serviceFactura.findAll(PageRequest.of(page, 5)));
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
    @GetMapping(value = "/facturas-dto/page/{page}")
    public ResponseEntity<Page<FacturaListadoDto>> getListadoDto(
            @PathVariable("page") Integer page,
            @RequestParam("fechaIni") String fechaIni,
            @RequestParam("fechaFin") String fechaFin,
            @RequestParam(value = "size", defaultValue = "5") Integer size) {
        return ResponseEntity.ok(serviceFactura.findAllListadoDto(
                fechaIni, fechaFin, PageRequest.of(page, size)));
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
    @GetMapping(value = "/facturas-dto/search/{page}")
    public ResponseEntity<Page<FacturaListadoDto>> searchListadoDto(
            @PathVariable("page") Integer page,
            @RequestParam("fechaIni") String fechaIni,
            @RequestParam("fechaFin") String fechaFin,
            @RequestParam(value = "filtro", defaultValue = "") String filtro,
            @RequestParam(value = "size", defaultValue = "5") Integer size) {
        return ResponseEntity.ok(serviceFactura.searchListadoDto(
                fechaIni, fechaFin, filtro, PageRequest.of(page, size)));
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
    @GetMapping(value = "/facturas-dto/ultimas/{page}")
    public ResponseEntity<Page<FacturaListadoDto>> getUltimasListadoDto(
            @PathVariable("page") Integer page,
            @RequestParam(value = "filtro", defaultValue = "") String filtro,
            @RequestParam(value = "size", defaultValue = "5") Integer size) {
        return ResponseEntity.ok(serviceFactura.findUltimasListadoDto(filtro, PageRequest.of(page, size)));
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
    @GetMapping(value = "/facturas-dto/{idFactura}/detalle/{page}")
    public ResponseEntity<Page<DetalleDocumentoDto>> getDetalleDto(
            @PathVariable("idFactura") Long idFactura,
            @PathVariable("page") Integer page,
            @RequestParam(value = "size", defaultValue = "5") Integer size) {
        return ResponseEntity.ok(serviceFactura.findDetalleDto(idFactura, PageRequest.of(page, size)));
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
    @GetMapping(value = "/facturas/cantidad-ventas")
    public ResponseEntity<Integer> cantidadVentas() {
        return ResponseEntity.ok(serviceFactura.totalVentas());
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
    @GetMapping(value = "/facturas/factura/{id}")
    public ResponseEntity<Factura> getFactura(@PathVariable("id") Long idfactura) {
        log.info("Buscando Factura con ID: {}", idfactura);
        return ResponseEntity.ok(serviceFactura.findFactura(idfactura));
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
    @GetMapping(value = "/facturas/get-by-fecha")
    public ResponseEntity<List<Factura>> getFacturasPorFecha(@RequestParam("fechaIni") String fechaIni, @RequestParam("fechaFin") String fechaFin) {
        log.info("Buscando Facturas en las fechas comprandidas entre: {} y {}", fechaIni, fechaFin);
        return ResponseEntity.ok(serviceFactura.facturasPorFecha(fechaIni, fechaFin));
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
    @GetMapping("/facturas/get-listado-sp/get")
    public ResponseEntity<List<Factura>> getFacturasSP(@RequestParam(required = false) String fechaIni,
                                           @RequestParam(required = false) String fechaFin) {
        log.info("Buscando Facturas en las fechas comprandidas entre: {} y {}", fechaIni, fechaFin);
        return ResponseEntity.ok(serviceFactura.facturasPorFecha(fechaIni, fechaFin));
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
    @GetMapping("/facturas/get-by-correlativo/{correlativo}")
    public ResponseEntity<Factura> buscarCorrelativo(@PathVariable("correlativo") Long correlativo) {
        log.info("Buscando factura por correlativo");
        return ResponseEntity.ok(serviceFactura.findFacturaCorrelativo(correlativo));
    }

    // Consumido por el flujo de creación de Notas de Crédito con origen FACTURA,
    // disponible para todos los roles operativos.
    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO"})
    @GetMapping("/facturas/get-by-correlativo-sat")
    public ResponseEntity<Factura> buscarPorCorrelativoSat(
            @RequestParam("correlativo") String correlativo,
            @RequestParam("serie") String serie) {
        log.info("Buscando factura por correlativo SAT: {} y serie SAT: {}", correlativo, serie);
        return ResponseEntity.ok(serviceFactura.findFacturaByCorrelativoSatAndSerieSat(correlativo, serie));
    }

    /**** Nuevas Implementaciones de creación de factura y anulación *****/
    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO"})
    @PostMapping(value = "/facturas/createV2")
    public ResponseEntity<Factura> createV2(@RequestBody Factura factura) {
        log.info("********** Registrar Factura Versión 2 **********");
        Factura facturaCreated = serviceFactura.facturaFel(factura);
        return ResponseEntity.status(HttpStatus.CREATED).body(facturaCreated);
    }

    @Secured(value = {"ROLE_COBRADOR", "ROLE_ADMIN"})
    @PutMapping(value = "/facturas/cancelV2/{idusuario}")
    public ResponseEntity<Object> cancelV2(@RequestBody Factura factura, @PathVariable("idusuario") Integer idusuario) {
        log.info("********** Anulando Factura {} Versión 2 **********", factura.getCertificacionSat());
        Factura voidFactura = serviceFactura.anularFacturaFel(factura.getIdFactura(), idusuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(voidFactura);
    }

    // CONTROLADOR VENTAS DIARIAS
    @GetMapping(value = "/facturas/daily-sales")
    public ResponseEntity<byte[]> dailySales(@RequestParam("usuario") Integer usuario, @RequestParam("fecha") String fecha) {
        byte[] bytesDailySalesReport = serviceFactura.resportDailySales(usuario, fecha);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(bytesDailySalesReport);
    }
}
