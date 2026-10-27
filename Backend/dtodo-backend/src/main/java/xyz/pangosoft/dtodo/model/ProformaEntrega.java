package xyz.pangosoft.dtodo.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Una entrega de un pedido mayorista. La fecha de entrega es la real (la escribe quien registra) y la de registro la
 * pone el sistema: pueden ser distintas. Una entrega anulada se conserva. {@code idProforma} no es una relación a
 * propósito: el historial de una proforma anulada (borrada) se conserva.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = { "sucursal", "usuario", "usuarioAnulacion", "detalles" })
@Entity
@Table(name = "proformas_entregas")
public class ProformaEntrega implements Serializable {

    private static final long serialVersionUID = 4127730905512766023L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEntrega;

    private Long idProforma;

    /** Sucursal de la que se descuenta el inventario. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sucursal")
    private Sucursal sucursal;

    /** Quien registra la entrega. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    /** Nombre de quien entregó (piloto o vendedor), si no es el usuario que registra. */
    private String entregadoPor;

    private LocalDate fechaEntrega;

    private LocalDateTime fechaRegistro;

    /** El inventario de lo entregado ya se había descontado a mano: la entrega no lo descuenta otra vez. */
    private boolean descontadoAMano;

    private String observaciones;

    private boolean anulada;

    private LocalDateTime fechaAnulacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_anulacion")
    private Usuario usuarioAnulacion;

    private String motivoAnulacion;

    @Builder.Default
    @OneToMany(mappedBy = "entrega", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProformaEntregaDetalle> detalles = new ArrayList<>();
}
