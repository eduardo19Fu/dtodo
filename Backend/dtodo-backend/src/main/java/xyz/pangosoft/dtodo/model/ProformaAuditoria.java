package xyz.pangosoft.dtodo.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
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
import xyz.pangosoft.dtodo.model.enums.AccionAuditoriaEnum;

/**
 * Renglón del historial de una proforma: quién hizo qué, cuándo y por qué. {@code idProforma} no es una relación a
 * propósito: el historial de una proforma anulada (borrada) se conserva.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = { "usuario" })
@Entity
@Table(name = "proformas_auditoria")
public class ProformaAuditoria implements Serializable {

    private static final long serialVersionUID = 6604519283377120114L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAuditoria;

    private Long idProforma;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    private AccionAuditoriaEnum accion;

    private String motivo;

    @Column(length = 4000)
    private String detalle;

    private LocalDateTime fecha;
}
