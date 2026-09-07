package xyz.pangosoft.dtodo.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Cuando Usuario ganó el campo `sucursal` (soporte de sucursales), cualquier entidad que
 * embebe Usuario y no ignora esa propiedad de vuelta reproduce el mismo ciclo de
 * deserialización que Sucursal<->Usuario (ver SucursalJacksonTest): Jackson falla al
 * construir el deserializador con "JSON parse error: No _valueDeserializer assigned" al
 * recibir un usuario con su sucursal anidada (ver POST /api/proformas). Proforma.usuario
 * ignoraba password/roles pero no sucursal; el mismo hueco existía en Factura, NotaCredito,
 * Correlativo, MovimientoProducto, MarcaProducto, TipoProducto y DespachoNota.
 */
@SpringBootTest
@ActiveProfiles("context-test")
class ProformaJacksonTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deserializaUnaProformaConUsuarioYSucursalAnidadaSinCicloInfinito() {
        String json = "{"
                + "\"usuario\":{\"idUsuario\":1,\"usuario\":\"admin\","
                + "\"sucursal\":{\"idSucursal\":1,\"nombre\":\"Sucursal Central\"}}"
                + "}";

        assertDoesNotThrow(() -> objectMapper.readValue(json, Proforma.class));
    }

    @Test
    void serializaUnaProformaConUsuarioYSucursalAnidadaSinCicloInfinito() {
        Sucursal sucursal = Sucursal.builder().idSucursal(1).nombre("Sucursal Central").build();
        Usuario usuario = Usuario.builder().idUsuario(1).usuario("admin").sucursal(sucursal).build();
        Proforma proforma = Proforma.builder().usuario(usuario).build();

        assertDoesNotThrow(() -> objectMapper.writeValueAsString(proforma));
    }
}
