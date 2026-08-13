package xyz.pangosoft.dtodo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "notas_credito_detalle")
public class NotaCreditoDetalle implements Serializable {

    private static final long serialVersionUID = -5627582366345321356L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idNotaDetalle;

    private BigDecimal subTotal;
    private int cantidad;
    private double descuento;
    private BigDecimal subTotalDescuento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Producto producto;

}
