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

/** Constancia de una liquidación que el administrador deshizo: quién, cuándo, por qué y lo que se había liquidado. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = { "usuario" })
@Entity
@Table(name = "comisiones_deshechas")
public class ComisionDeshecha implements Serializable {

    private static final long serialVersionUID = 8128046115339247901L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDeshecha;

    private Integer anio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    private String motivo;

    @Column(length = 4000)
    private String resumen;

    private LocalDateTime fecha;
}
