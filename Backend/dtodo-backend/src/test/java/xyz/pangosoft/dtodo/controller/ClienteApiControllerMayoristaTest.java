package xyz.pangosoft.dtodo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.Ruta;
import xyz.pangosoft.dtodo.model.enums.CategoriaClienteEnum;
import xyz.pangosoft.dtodo.model.enums.TipoClienteEnum;

/** Solo el administrador puede registrar o convertir a un cliente en mayorista. */
class ClienteApiControllerMayoristaTest {

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private static Cliente mayorista() {
        Cliente cliente = Cliente.builder().nombre("Libreria Uno").tipoCliente(TipoClienteEnum.MAYORISTA)
                .categoriaDescuento(CategoriaClienteEnum.B).limiteCredito(new BigDecimal("1000")).diasCredito(15).build();
        Ruta ruta = new Ruta();
        ruta.setIdRuta(1);
        cliente.setRuta(ruta);
        return cliente;
    }

    private void iniciarSesion(String... roles) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("usuario", "x", roles));
    }

    @Test
    void elCobradorSoloRegistraClientesMinoristas() {
        iniciarSesion("ROLE_COBRADOR");
        Cliente cliente = mayorista();

        ClienteApiController.restringirDatosMayoristas(cliente);

        assertEquals(TipoClienteEnum.MINORISTA, cliente.getTipoCliente());
        assertNull(cliente.getCategoriaDescuento());
        assertNull(cliente.getRuta());
        assertNull(cliente.getLimiteCredito());
        assertNull(cliente.getDiasCredito());
        assertEquals("Libreria Uno", cliente.getNombre());
    }

    @Test
    void elAdministradorConservaLosDatosMayoristas() {
        iniciarSesion("ROLE_ADMIN");
        Cliente cliente = mayorista();

        ClienteApiController.restringirDatosMayoristas(cliente);

        assertEquals(TipoClienteEnum.MAYORISTA, cliente.getTipoCliente());
        assertEquals(CategoriaClienteEnum.B, cliente.getCategoriaDescuento());
        assertNotNull(cliente.getRuta());
        assertEquals(new BigDecimal("1000"), cliente.getLimiteCredito());
        assertEquals(15, cliente.getDiasCredito());
    }

    @Test
    void sinSesionTampocoSeAceptanDatosMayoristas() {
        Cliente cliente = mayorista();

        ClienteApiController.restringirDatosMayoristas(cliente);

        assertEquals(TipoClienteEnum.MINORISTA, cliente.getTipoCliente());
    }
}
