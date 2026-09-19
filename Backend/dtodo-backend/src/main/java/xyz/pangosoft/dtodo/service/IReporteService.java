package xyz.pangosoft.dtodo.service;

import java.util.List;

import xyz.pangosoft.dtodo.dto.UsuarioDto;

public interface IReporteService {

    byte[] generarPolizaIndividual(Integer idSucursal, Integer idUsuario, String fechaInicio, String fechaFin);

    byte[] generarPolizaGeneral(Integer idSucursal, String fechaInicio, String fechaFin);

    byte[] generarMovimientosInventario(Integer idSucursal, String fechaInicio, String fechaFin);

    byte[] generarExistencias(Integer idSucursal);

    byte[] generarProformas(Integer idUsuario, String fechaInicio, String fechaFin, boolean todas);

    List<UsuarioDto> listarUsuariosProformas();
}
