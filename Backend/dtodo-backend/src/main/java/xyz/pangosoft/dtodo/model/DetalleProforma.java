package xyz.pangosoft.dtodo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;
import java.math.BigDecimal;
import xyz.pangosoft.dtodo.model.enums.TipoDescuentoEnum;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "proformas_detalle")
public class DetalleProforma implements Serializable {

    private static final long serialVersionUID = -6591842924041622770L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDetalle;
    @NotNull(message = "La cantidad del producto no puede estar vacía.")
    @Positive(message = "La cantidad del producto debe ser mayor a 0.")
    @Column(nullable = false)
    private Integer cantidad;
    private Float descuento;
    private BigDecimal nuevoPrecioVenta;
    private BigDecimal subTotal;
    private BigDecimal subTotalDescuento;

    /** Precio de venta unitario al registrar la línea (antes de descuento); nulo en las líneas anteriores. */
    private BigDecimal precioUnitario;

    /** De dónde sale el descuento de la línea: lo elige el usuario y el servidor verifica que coincida. */
    @Enumerated(EnumType.STRING)
    private TipoDescuentoEnum tipoDescuento;

    /** Porcentaje que sugería el sistema al registrar la línea, para saber cuánto se aparta el usuario. */
    private BigDecimal porcentajeSugerido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Producto producto;

}

