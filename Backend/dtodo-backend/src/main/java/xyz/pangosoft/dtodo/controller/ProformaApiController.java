package xyz.pangosoft.dtodo.controller;

import xyz.pangosoft.dtodo.dto.ProformaDto;
import xyz.pangosoft.dtodo.dto.ProformaListadoDto;
import xyz.pangosoft.dtodo.dto.DetalleDocumentoDto;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.service.IEstadoService;
import xyz.pangosoft.dtodo.service.IProformaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(value = {"http://localhost:4200", "https://dtodojalapa.xyz", "http://dtodojalapa.xyz"})
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ProformaApiController {

    private final IProformaService proformaService;

    private final IEstadoService estadoService;

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO"})
    @GetMapping("/proformas")
    public ResponseEntity<List<Proforma>> index(){
        log.info("Retornando listado de Proformas registradas");
        return ResponseEntity.ok(proformaService.findAll());
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO"})
    @GetMapping("/proformas-dto/page/{page}")
    public ResponseEntity<Page<ProformaListadoDto>> getListadoDto(
            @PathVariable("page") Integer page,
            @RequestParam("fechaIni") String fechaIni,
            @RequestParam("fechaFin") String fechaFin,
            @RequestParam(value = "size", defaultValue = "5") Integer size) {
        return ResponseEntity.ok(proformaService.findAllListadoDto(
                fechaIni, fechaFin, PageRequest.of(page, size)));
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO"})
    @GetMapping("/proformas-dto/search/{page}")
    public ResponseEntity<Page<ProformaListadoDto>> searchListadoDto(
            @PathVariable("page") Integer page,
            @RequestParam("fechaIni") String fechaIni,
            @RequestParam("fechaFin") String fechaFin,
            @RequestParam(value = "filtro", defaultValue = "") String filtro,
            @RequestParam(value = "size", defaultValue = "5") Integer size) {
        return ResponseEntity.ok(proformaService.searchListadoDto(
                fechaIni, fechaFin, filtro, PageRequest.of(page, size)));
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO"})
    @GetMapping("/proformas-dto/ultimas/{page}")
    public ResponseEntity<Page<ProformaListadoDto>> getUltimasListadoDto(
            @PathVariable("page") Integer page,
            @RequestParam(value = "filtro", defaultValue = "") String filtro,
            @RequestParam(value = "size", defaultValue = "5") Integer size) {
        return ResponseEntity.ok(proformaService.findUltimasListadoDto(filtro, PageRequest.of(page, size)));
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO"})
    @GetMapping("/proformas-dto/{idProforma}/detalle/{page}")
    public ResponseEntity<Page<DetalleDocumentoDto>> getDetalleDto(
            @PathVariable("idProforma") Long idProforma,
            @PathVariable("page") Integer page,
            @RequestParam(value = "size", defaultValue = "5") Integer size) {
        return ResponseEntity.ok(proformaService.findDetalleDto(idProforma, PageRequest.of(page, size)));
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO"})
    @GetMapping("/proformas/{id}")
    public ResponseEntity<Proforma> findProforma(@PathVariable("id") Long id){
        log.info("Retornando Proforma registrada con ID: {}", id);
        return ResponseEntity.ok(proformaService.findProforma(id));
    }

    /**
     * Busca una Proforma por su número único.
     * Usado por el módulo de Notas de Crédito para localizar la proforma origen.
     *
     * @param noProforma número único asignado a la proforma
     * @return la Proforma encontrada o 404 si no existe
     */
    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO"})
    @GetMapping("/proformas/by-no-proforma/{noProforma}")
    public ResponseEntity<Proforma> findByNoProforma(@PathVariable("noProforma") String noProforma) {
        log.info("Retornando Proforma con número: {}", noProforma);
        return ResponseEntity.ok(proformaService.findByNoProforma(noProforma));
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO"})
    @PostMapping(value = "/proformas")
    public ResponseEntity<Proforma> create(@Valid @RequestBody Proforma proforma) {
        log.info("Registrando nueva proforma: {}", proforma.toString());
        Proforma newProforma = proformaService.save(proforma, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(newProforma);
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO"})
    @PutMapping(value = "/proformas/{id}")
    public ResponseEntity<Proforma> udpate(@Valid @RequestBody Proforma proforma, @PathVariable Long id) {
        log.info("Actualizando proforma: {}", proforma.toString());
        Proforma proformaUpdated = proformaService.save(proforma, id);
        return ResponseEntity.status(HttpStatus.CREATED).body(proformaUpdated);
    }

    @Secured(value = "ROLE_ADMIN")
    @DeleteMapping(value = "/proformas/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable("id") Long id) {
        log.info("Eliminando proforma con ID: {}", id);

        Map<String, Object> response = new HashMap<>();
        Proforma proforma = proformaService.findProforma(id);
        proformaService.delete(proforma);
        response.put("mensaje", "¡Proforma ha sido eliminada con éxito!");
        return new ResponseEntity<Map<String, Object>>(response, HttpStatus.OK);
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO"})
    @GetMapping("/proformas/get-listado-sp/get")
    public ResponseEntity<List<Proforma>> getProformasPorFecha(@RequestParam(value = "date1", required = false) String fechaIni,
                                                               @RequestParam(value = "date2", required = false) String fechaFin)
    {
        List<Proforma> proformas = new ArrayList<>();
        proformas = proformaService.proformasPorFecha(fechaIni, fechaFin);
        return ResponseEntity.ok(proformas);
    }

    @Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO"})
    @GetMapping("/proformas/get-listado-dto/get")
    public ResponseEntity<List<ProformaDto>> getProformasPorFechaSp(@RequestParam(value = "date1", required = false) String fechaIni,
                                                                    @RequestParam(value = "date2", required = false) String fechaFin)
    {
        List<ProformaDto> proformas = new ArrayList<>();
        proformas = proformaService.proformasPorFechaSp(fechaIni, fechaFin);
        return ResponseEntity.ok(proformas);
    }

    /**
     * metodos controlador que permiten devolver un pdf con la información solicitada por
     * los usuarios desde el frontend.
     * @param idproforma Valor de tipo entero que refencia el ID de la proforma que se desea generar.
     * */

    @GetMapping("/proformas/generate/{id}")
    public ResponseEntity<byte[]> showProforma(@PathVariable("id") Long idproforma) {
        byte[] proformaPdf = proformaService.showProforma(idproforma);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(proformaPdf);
    }
}
