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
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "pagos_parciales")
public class PagoParcial implements Serializable {

    private static final long serialVersionUID = 2989474446274071732L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPagoParcial;

    private BigDecimal pago;
    private LocalDate fechaPago;
    private LocalDate fechaProximoPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nota_credito")
    @JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"})
    private NotaCredito notaCredito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado")
    @JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"})
    private Estado estado;

}
