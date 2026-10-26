package xyz.pangosoft.dtodo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Fila del listado de rutas: la ruta con su vendedor, sucursal, estado y cantidad de clientes. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RutaDto {
    private Integer idRuta;
    private String nombre;
    private String zona;
    private String diasVisita;
    private Integer idVendedor;
    private String vendedor;
    private Integer idSucursal;
    private String sucursal;
    private Integer idEstado;
    private String estado;
    private Long totalClientes;
}
