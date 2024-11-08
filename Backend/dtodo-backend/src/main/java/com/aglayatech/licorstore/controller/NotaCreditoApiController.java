package com.aglayatech.licorstore.controller;

import com.aglayatech.licorstore.model.NotaCredito;
import com.aglayatech.licorstore.model.enums.EstadoNotaCreditoEnum;
import com.aglayatech.licorstore.service.INotaCreditoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api")
@RequiredArgsConstructor
@Slf4j
public class NotaCreditoApiController {

    private final INotaCreditoService notaCreditoService;

    @GetMapping(value = "/notas-credito")
    public ResponseEntity<List<NotaCredito>> getNotasCredito() {
        log.info("Buscando listado de Notas de Credito");
        return ResponseEntity.ok(notaCreditoService.findNotas());
    }

    @GetMapping(value = "/notas-credito/{id}")
    public ResponseEntity<NotaCredito> getNotaCredito(@PathVariable("id") Integer idnota) {
        log.info("Buscando Nota de Credito con ID: {}", idnota);
        return ResponseEntity.ok(notaCreditoService.findNota(idnota));
    }

    @PostMapping(value = "/notas-credito")
    public ResponseEntity<NotaCredito> create(@RequestBody NotaCredito notaCredito) {
        log.info("********** Registrando nueva nota de credito **********");
        return ResponseEntity.status(HttpStatus.CREATED).body(notaCreditoService.save(notaCredito, EstadoNotaCreditoEnum.ENTREGA_PENDIENTE));
    }

    @PutMapping(value = "/notas-credito/anular/{id}")
    public ResponseEntity<NotaCredito> voidNota(@PathVariable("id") Integer idnota) {
        log.info("Anulando Nota de Credito: {}", idnota);
        NotaCredito notaCredito = notaCreditoService.findNota(idnota);
        return ResponseEntity.status(HttpStatus.CREATED).body(notaCreditoService.save(notaCredito, EstadoNotaCreditoEnum.ANULADO));
    }

    @PutMapping(value = "/notas-credito/entregar/{id}")
    public ResponseEntity<NotaCredito> despacharNota(@PathVariable("id") Integer idnota) {
        log.info("Entregando producto de Nota de Credito: {}", idnota);
        NotaCredito notaCredito = notaCreditoService.findNota(idnota);
        return ResponseEntity.status(HttpStatus.CREATED).body(notaCreditoService.save(notaCredito, EstadoNotaCreditoEnum.ENTREGADO));
    }

}
