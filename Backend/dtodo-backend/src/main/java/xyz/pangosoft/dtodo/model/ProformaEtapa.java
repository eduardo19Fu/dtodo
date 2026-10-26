package xyz.pangosoft.dtodo.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import xyz.pangosoft.dtodo.model.enums.EstadoPedidoEnum;

/**
 * Un paso del pedido mayorista: la etapa a la que llegó, quién lo registró y, si quien lo hizo no tiene usuario,
 * su nombre en {@code responsable}. {@code idProforma} no es una relación a propósito: el historial de una
 * proforma anulada (borrada) se conserva.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = { "usuario" })
@Entity
@Table(name = "proformas_etapas")
public class ProformaEtapa implements Serializable {

    private static final long serialVersionUID = -2814067331590326518L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEtapa;

    private Long idProforma;

    @Enumerated(EnumType.STRING)
    private EstadoPedidoEnum etapa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    private String responsable;

    private String motivo;

    private LocalDateTime fecha;
}
