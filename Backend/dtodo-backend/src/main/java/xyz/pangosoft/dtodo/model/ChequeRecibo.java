package xyz.pangosoft.dtodo.model;

import java.io.Serializable;
import java.time.LocalDate;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import xyz.pangosoft.dtodo.model.enums.EstadoChequeEnum;

/** Los datos y el seguimiento del cheque con que se pagó un recibo de caja. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = { "recibo", "usuarioEstado" })
@Entity
@Table(name = "cheques_recibo")
public class ChequeRecibo implements Serializable {

    private static final long serialVersionUID = 6203417719045527181L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCheque;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_recibo")
    private ReciboCaja recibo;

    private String banco;

    private String numeroCheque;

    /** Quien firma el cheque; si no se indica, es el propio cliente. */
    private String girador;

    /** La fecha escrita en el cheque; si es posterior a la del pago, es postfechado. */
    private LocalDate fechaCheque;

    private String notas;

    @Enumerated(EnumType.STRING)
    private EstadoChequeEnum estado;

    private LocalDateTime fechaEstado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_estado")
    private Usuario usuarioEstado;

    private String motivoRechazo;
}
