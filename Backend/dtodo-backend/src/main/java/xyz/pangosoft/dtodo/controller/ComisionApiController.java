package xyz.pangosoft.dtodo.controller;

import java.util.List;
import java.util.Map;

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

/** Comisiones de los vendedores: metas, avance y liquidación del año. Solo el administrador. */
@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz" })
@RestController
@RequestMapping("/api/comisiones")
@RequiredArgsConstructor
@Slf4j
public class ComisionApiController {

    private final IComisionService comisionService;

    @Secured("ROLE_ADMIN")
    @GetMapping("/metas")
    public ResponseEntity<List<MetaComisionDto>> metas(@RequestParam int anio) {
        return ResponseEntity.ok(comisionService.metas(anio));
    }

    @Secured("ROLE_ADMIN")
    @PutMapping("/metas")
    public ResponseEntity<List<MetaComisionDto>> guardarMetas(@RequestParam int anio, @RequestBody List<MetaComisionDto> metas) {
        log.info("Guardando las metas de comisión del año {}", anio);
        return ResponseEntity.ok(comisionService.guardarMetas(anio, metas));
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
}
