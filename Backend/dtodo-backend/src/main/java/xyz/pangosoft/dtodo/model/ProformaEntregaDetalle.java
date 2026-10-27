package xyz.pangosoft.dtodo.model;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Lo entregado de una línea del pedido en una entrega, y cuánto de eso movió el inventario. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = { "entrega" })
@Entity
@Table(name = "proformas_entregas_detalle")
public class ProformaEntregaDetalle implements Serializable {

    private static final long serialVersionUID = -6205841734905214411L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEntregaDetalle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_entrega")
    private ProformaEntrega entrega;

    /** La línea de la proforma ({@code proformas_detalle}). */
    private Long idDetalle;

    /** Unidades entregadas. */
    private Integer cantidad;

    /** Unidades que esta entrega agregó a lo ya descontado de la línea (para deshacerlo al anularla). */
    private Integer cantidadCubierta;

    /** Unidades que el sistema descontó del inventario en esta entrega (cero si ya estaban descontadas a mano). */
    private Integer cantidadDescontada;
}
