package xyz.pangosoft.dtodo.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import xyz.pangosoft.dtodo.dto.ReporteSelectorDto;
import xyz.pangosoft.dtodo.repository.IClienteRepository;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.repository.IProveedorRepository;
import xyz.pangosoft.dtodo.repository.IProductoRepository;
import xyz.pangosoft.dtodo.repository.ISucursalRepository;
import xyz.pangosoft.dtodo.repository.ITipoProductoRepository;
import xyz.pangosoft.dtodo.repository.IUsuarioRepository;
import xyz.pangosoft.dtodo.service.IReporteSelectorService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteSelectorServiceImpl implements IReporteSelectorService {

    private final ISucursalRepository sucursalRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IProformaRepository proformaRepository;
    private final ITipoProductoRepository tipoProductoRepository;
    private final IClienteRepository clienteRepository;
    private final IProveedorRepository proveedorRepository;
    private final IProductoRepository productoRepository;

    @Override
    public List<ReporteSelectorDto> listarSucursales() {
        return sucursalRepository.findOpcionesReporte();
    }

    @Override
    public List<ReporteSelectorDto> listarCajeros(Integer idSucursal) {
        return usuarioRepository.findOpcionesCajeroReporte(idSucursal);
    }

    @Override
    public List<ReporteSelectorDto> listarUsuariosProformas(Integer idSucursal) {
        return proformaRepository.findOpcionesUsuarioReporte(idSucursal);
    }

    @Override
    public List<ReporteSelectorDto> listarCategorias() {
        return tipoProductoRepository.findOpcionesReporte();
    }

    @Override
    public List<ReporteSelectorDto> listarClientes() {
        return clienteRepository.findOpcionesReporte();
    }

    @Override
    public List<ReporteSelectorDto> listarProveedores() {
        return proveedorRepository.findOpcionesReporte();
    }

    @Override
    public List<ReporteSelectorDto> listarProductos(Integer idSucursal) {
        return productoRepository.findOpcionesReporte(idSucursal);
    }
}
