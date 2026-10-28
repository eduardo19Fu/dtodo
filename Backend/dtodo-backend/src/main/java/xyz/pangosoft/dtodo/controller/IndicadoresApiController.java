package xyz.pangosoft.dtodo.controller;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import xyz.pangosoft.dtodo.dto.IndicadoresMesDto;
import xyz.pangosoft.dtodo.service.IIndicadoresService;

/** Tablero de indicadores del administrador. Sin año y mes, el mes actual. */
@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz" })
@RestController
@RequestMapping("/api/indicadores")
@RequiredArgsConstructor
public class IndicadoresApiController {

    private final IIndicadoresService indicadoresService;

    @Secured("ROLE_ADMIN")
    @GetMapping
    public ResponseEntity<IndicadoresMesDto> indicadores(@RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes) {
        LocalDate hoy = LocalDate.now();
        return ResponseEntity.ok(indicadoresService.indicadores(anio == null ? hoy.getYear() : anio, mes == null ? hoy.getMonthValue() : mes));
    }
}
