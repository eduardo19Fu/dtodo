package xyz.pangosoft.dtodo.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.pangosoft.dtodo.dto.AvanceComisionDto;
import xyz.pangosoft.dtodo.dto.LiquidarComisionDto;
import xyz.pangosoft.dtodo.dto.MetaComisionDto;
import xyz.pangosoft.dtodo.service.IComisionService;
import xyz.pangosoft.dtodo.service.IReporteComisionesService;

/** Comisiones de los vendedores: metas, avance y liquidación del año. Solo el administrador. */
@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz" })
@RestController
@RequestMapping("/api/comisiones")
@RequiredArgsConstructor
@Slf4j
public class ComisionApiController {

    private final IComisionService comisionService;
    private final IReporteComisionesService reporteComisionesService;

    @Secured("ROLE_ADMIN")
    @GetMapping("/metas")
    public ResponseEntity<List<MetaComisionDto>> metas(@RequestParam int anio) {
        return ResponseEntity.ok(comisionService.metas(anio));
    }

    @Secured("ROLE_ADMIN")
    @PutMapping("/metas")
    public ResponseEntity<List<MetaComisionDto>> guardarMetas(@RequestParam int anio, @RequestParam(required = false) String motivo,
            @RequestBody List<MetaComisionDto> metas) {
        log.info("Guardando las metas de comisión del año {}", anio);
        return ResponseEntity.ok(comisionService.guardarMetas(anio, metas, motivo));
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/metas/historial")
    public ResponseEntity<List<xyz.pangosoft.dtodo.dto.HistorialMetaComisionDto>> historialMetas(@RequestParam int anio) {
        return ResponseEntity.ok(comisionService.historialMetas(anio));
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/metas/copiar")
    public ResponseEntity<List<MetaComisionDto>> copiarMetas(@RequestParam int desde, @RequestParam int hacia) {
        log.info("Copiando las metas de comisión de {} a {}", desde, hacia);
        return ResponseEntity.ok(comisionService.copiarMetas(desde, hacia));
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/avance")
    public ResponseEntity<AvanceComisionDto> avance(@RequestParam int anio) {
        return ResponseEntity.ok(comisionService.avance(anio));
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/liquidar")
    public ResponseEntity<AvanceComisionDto> liquidar(@RequestParam int anio, @RequestBody LiquidarComisionDto decision) {
        log.info("Liquidando las comisiones del año {}", anio);
        return ResponseEntity.ok(comisionService.liquidar(anio, decision));
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/deshacer")
    public ResponseEntity<AvanceComisionDto> deshacer(@RequestParam int anio, @RequestBody Map<String, String> cuerpo) {
        log.info("Deshaciendo la liquidación de comisiones del año {}", anio);
        return ResponseEntity.ok(comisionService.deshacerLiquidacion(anio, cuerpo.get("motivo")));
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/reporte")
    public ResponseEntity<byte[]> reporte(@RequestParam int anio, @RequestParam(defaultValue = "PDF") String formato) {
        log.info("Generando reporte de comisiones. anio={}, formato={}", anio, formato);
        byte[] contenido = reporteComisionesService.comisionesDelAnio(anio, formato);
        boolean esPdf = !"XLSX".equalsIgnoreCase(formato);
        String disposition = (esPdf ? "inline" : "attachment") + "; filename=\"comisiones_" + anio + (esPdf ? ".pdf" : ".xlsx") + "\"";
        return ResponseEntity.ok()
                .contentType(esPdf ? MediaType.APPLICATION_PDF
                        : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        new String(disposition.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentLength(contenido.length)
                .body(contenido);
    }
}
