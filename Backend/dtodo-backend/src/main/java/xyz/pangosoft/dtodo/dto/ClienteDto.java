package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.pangosoft.dtodo.model.enums.CategoriaClienteEnum;
import xyz.pangosoft.dtodo.model.enums.TipoClienteEnum;

@Data
@NoArgsConstructor
public class ClienteDto {
    private Integer idCliente;
    private String nombre;
    private String nit;
    private String direccion;
    private LocalDateTime fechaRegistro;
    private String telefono;

    private String codigoCliente;
    private String nombreNegocio;
    private TipoClienteEnum tipoCliente;
    private CategoriaClienteEnum categoriaDescuento;
    private Integer idRuta;
    private String ruta;
    private String contacto;
    private String telefonoContacto;
    private String referenciaUbicacion;
    private BigDecimal limiteCredito;
    private Integer diasCredito;

    public ClienteDto(Integer idCliente, String nombre, String nit, String direccion, LocalDateTime fechaRegistro,
                      String telefono) {
        this.idCliente = idCliente;
        this.nombre = nombre;
        this.nit = nit;
        this.direccion = direccion;
        this.fechaRegistro = fechaRegistro;
        this.telefono = telefono;
        this.tipoCliente = TipoClienteEnum.MINORISTA;
    }

    public ClienteDto(Integer idCliente, String nombre, String nit, String direccion, LocalDateTime fechaRegistro,
                      String telefono, String codigoCliente, String nombreNegocio, TipoClienteEnum tipoCliente,
                      CategoriaClienteEnum categoriaDescuento, Integer idRuta, String ruta, String contacto,
                      String telefonoContacto, String referenciaUbicacion, BigDecimal limiteCredito,
                      Integer diasCredito) {
        this(idCliente, nombre, nit, direccion, fechaRegistro, telefono);
        this.codigoCliente = codigoCliente;
        this.nombreNegocio = nombreNegocio;
        this.tipoCliente = tipoCliente == null ? TipoClienteEnum.MINORISTA : tipoCliente;
        this.categoriaDescuento = categoriaDescuento;
        this.idRuta = idRuta;
        this.ruta = ruta;
        this.contacto = contacto;
        this.telefonoContacto = telefonoContacto;
        this.referenciaUbicacion = referenciaUbicacion;
        this.limiteCredito = limiteCredito;
        this.diasCredito = diasCredito;
    }
}
