package com.aglayatech.licorstore.model;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tipos_factura")
public class TipoFactura implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idTipoFactura;
    private String tipoFactura;

    public Integer getIdTipoFactura() {
        return idTipoFactura;
    }

    public void setIdTipoFactura(Integer idTipoFactura) {
        this.idTipoFactura = idTipoFactura;
    }

    public String getTipoFactura() {
        return tipoFactura;
    }

    public void setTipoFactura(String tipoFactura) {
        this.tipoFactura = tipoFactura;
    }

    @Override
    public String toString() {
        return "TipoFactura{" +
                "idTipoFactura=" + idTipoFactura +
                ", tipoFactura='" + tipoFactura + '\'' +
                '}';
    }

    private static final long serialVersionUID = 1L;
}
