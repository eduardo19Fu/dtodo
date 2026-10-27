package xyz.pangosoft.dtodo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import xyz.pangosoft.dtodo.dto.PedidoSaldoDto;
import xyz.pangosoft.dtodo.dto.SaldoClienteDto;
import xyz.pangosoft.dtodo.service.ISaldoClienteService;

/**
 * Saldo de los clientes mayoristas y cobro de sus pedidos. Lo consultan el administrador y el cobrador; el vendedor, solo para
 * sus clientes y pedidos (el servidor lo limita). El repartidor y bodega no ven dinero.
 */
@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz" })
@RestController
@RequestMapping("/api/saldos")
@RequiredArgsConstructor
public class SaldoApiController {

    private final ISaldoClienteService saldoService;

    @Secured({ "ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_VENDEDOR" })
    @GetMapping("/clientes/{idCliente}")
    public ResponseEntity<SaldoClienteDto> saldoDelCliente(@PathVariable("idCliente") Integer idCliente) {
        return ResponseEntity.ok(saldoService.saldo(idCliente));
    }

    @Secured({ "ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_VENDEDOR" })
    @GetMapping("/pedidos/{idProforma}")
    public ResponseEntity<PedidoSaldoDto> saldoDelPedido(@PathVariable("idProforma") Long idProforma) {
        return ResponseEntity.ok(saldoService.saldoDelPedido(idProforma));
    }
}
