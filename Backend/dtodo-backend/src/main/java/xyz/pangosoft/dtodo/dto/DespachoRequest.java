package xyz.pangosoft.dtodo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import xyz.pangosoft.dtodo.model.DespachoNota;

import java.util.List;

/**
 * Payload del registro de un despacho de Nota de Crédito.
 *
 * <p>Encapsula las líneas a despachar junto con la contraseña del usuario en
 * sesión, que actúa como autorización explícita de la operación. La contraseña
 * es de solo escritura ({@link JsonProperty.Access#WRITE_ONLY}) y se excluye de
 * {@code toString()} para que nunca viaje de vuelta en una respuesta ni termine
 * en los logs.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DespachoRequest {

    /** Contraseña en claro del usuario autenticado; viaja únicamente sobre HTTPS. */
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** Líneas de producto a despachar en esta operación. */
    private List<DespachoNota> despachos;
}
