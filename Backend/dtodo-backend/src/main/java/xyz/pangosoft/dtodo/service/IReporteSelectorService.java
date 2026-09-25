package xyz.pangosoft.dtodo.service;

import java.util.List;

import xyz.pangosoft.dtodo.dto.ReporteSelectorDto;

public interface IReporteSelectorService {

    List<ReporteSelectorDto> listarSucursales();

    List<ReporteSelectorDto> listarCajeros(Integer idSucursal);

    List<ReporteSelectorDto> listarUsuariosProformas(Integer idSucursal);

    List<ReporteSelectorDto> listarCategorias();

    List<ReporteSelectorDto> listarClientes();

    List<ReporteSelectorDto> listarProveedores();

    List<ReporteSelectorDto> listarProductos(Integer idSucursal);
}
