package xyz.pangosoft.dtodo.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;
import java.util.Date;

@Data
@NoArgsConstructor
@Builder
public class ProformaDto {
    private Long idProforma;
    private String noProforma;
    private Date fechaEmision;
    private BigDecimal total;
    private Integer idEstado;
    private String estado;
    private String usuario;
    private String vendedor;
    private String cliente;
    private String nitCliente;

    /** Factura generada desde esta proforma; {@code null} si todavía no se factura. */
    private Long idFactura;
    private String serieFactura;
    private Long noFactura;
    private String estadoFactura;
    /** Nombre de quien emitió la factura; {@code null} si la proforma no se ha facturado. */
    private String facturadoPor;

    private TipoProformaEnum tipoProforma;
    /** Nombre de la ruta de la proforma mayorista. */
    private String ruta;
    /** PENDIENTE, APROBADA, RECHAZADA o nulo si no necesita autorización. */
    private EstadoAutorizacionEnum estadoAutorizacion;

    public ProformaDto(Long idProforma, String noProforma, Date fechaEmision, BigDecimal total, Integer idEstado,
                       String estado, String usuario, String vendedor, String cliente, String nitCliente,
                       Long idFactura, String serieFactura, Long noFactura, String estadoFactura,
                       String facturadoPor, TipoProformaEnum tipoProforma, String ruta,
                       EstadoAutorizacionEnum estadoAutorizacion) {
        this.idProforma = idProforma;
        this.noProforma = noProforma;
        this.fechaEmision = fechaEmision;
        this.total = total;
        this.idEstado = idEstado;
        this.estado = estado;
        this.usuario = usuario;
        this.vendedor = vendedor;
        this.cliente = cliente;
        this.nitCliente = nitCliente;
        this.idFactura = idFactura;
        this.serieFactura = serieFactura;
        this.noFactura = noFactura;
        this.estadoFactura = estadoFactura;
        this.facturadoPor = idFactura == null || facturadoPor == null || facturadoPor.trim().isEmpty()
                ? null : facturadoPor.trim();
        this.tipoProforma = tipoProforma == null ? TipoProformaEnum.MINORISTA : tipoProforma;
        this.ruta = ruta;
        this.estadoAutorizacion = estadoAutorizacion;
    }
}
