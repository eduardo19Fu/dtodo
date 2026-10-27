package xyz.pangosoft.dtodo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/** Lo que se envía para avanzar un pedido a la etapa siguiente. */
@Data
@NoArgsConstructor
public class AvancePedidoDto {

    /** Nombre de quien lo hizo cuando no tiene usuario (personal que rota); opcional. */
    private String responsable;

    /** El piloto que lleva el pedido: obligatorio al cargarlo a ruta, se ignora en las demás etapas. */
    private Integer idRepartidor;
}
