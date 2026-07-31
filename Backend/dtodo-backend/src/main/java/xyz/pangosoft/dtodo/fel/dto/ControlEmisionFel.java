package xyz.pangosoft.dtodo.fel.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Estado de la cuenta del emisor ante INFILE (saldo/créditos de emisión).
 * Reemplazo de {@code com.fel.validaciones.documento.ControlEmision}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ControlEmisionFel {

    @JsonProperty("Saldo")
    private String Saldo;

    @JsonProperty("Creditos")
    private String Creditos;

    public String getSaldo() {
        return Saldo;
    }

    public void setSaldo(String saldo) {
        this.Saldo = saldo;
    }

    public String getCreditos() {
        return Creditos;
    }

    public void setCreditos(String creditos) {
        this.Creditos = creditos;
    }
}
