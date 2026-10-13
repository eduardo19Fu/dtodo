package xyz.pangosoft.dtodo.util;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Serializa un valor numérico solo cuando quien hace la petición es administrador; para cualquier otro rol (o sin
 * sesión) escribe {@code null}.
 *
 * <p>Se usa en los datos de costo (precio de compra, margen de ganancia, valor de despachos al costo) para que
 * ninguna respuesta de la API los entregue a un rol distinto de administrador, sin importar el endpoint ni la
 * pantalla que los consuma. Solo afecta la salida JSON: las entidades y los reportes no se modifican.</p>
 */
public class SoloAdministradorSerializer extends JsonSerializer<Number> {

	@Override
	public void serialize(Number valor, JsonGenerator generador, SerializerProvider proveedor) throws IOException {
		if (Utils.esAdministrador()) {
			proveedor.defaultSerializeValue(valor, generador);
		} else {
			generador.writeNull();
		}
	}

}
