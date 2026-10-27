package xyz.pangosoft.dtodo.model;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Último número de recibo emitido por una sucursal. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "recibos_correlativos")
public class ReciboCorrelativo implements Serializable {

    private static final long serialVersionUID = 1865120338825390477L;

    @Id
    private Integer idSucursal;

    private Long ultimo;
}
