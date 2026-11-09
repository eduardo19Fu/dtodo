package xyz.pangosoft.dtodo.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
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

/**
 * Un cambio de las metas de comisión de un año: quién lo hizo, cuándo, por qué y cómo eran las metas antes y después
 * (texto {@code monto:porcentaje;monto:porcentaje}).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = { "usuario" })
@Entity
@Table(name = "comisiones_metas_historial")
public class ComisionMetaHistorial implements Serializable {

    private static final long serialVersionUID = 6049318627715420183L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idHistorial;

    private Integer anio;

    /** CREAR (el año no tenía metas), CAMBIAR o COPIAR (de otro año). */
    private String accion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    private String motivo;

    @Column(length = 2000)
    private String antes;

    @Column(length = 2000)
    private String despues;

    private LocalDateTime fecha;
}
