package xyz.pangosoft.dtodo.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.model.enums.EstadoCompraEnum;
import xyz.pangosoft.dtodo.model.enums.EstadoNotaCreditoEnum;
import xyz.pangosoft.dtodo.service.IBajoStockReporteService;
import xyz.pangosoft.dtodo.service.IComprasPeriodoReporteService;
import xyz.pangosoft.dtodo.service.IComprasProveedorProductoReporteService;
import xyz.pangosoft.dtodo.service.IConversionProformasReporteService;
import xyz.pangosoft.dtodo.service.IFacturaService;
import xyz.pangosoft.dtodo.service.IInventarioMovimientosReporteService;
import xyz.pangosoft.dtodo.service.IKardexProductoReporteService;
import xyz.pangosoft.dtodo.service.IPendientesDespachoReporteService;
import xyz.pangosoft.dtodo.service.IProductoService;
import xyz.pangosoft.dtodo.service.IProformaService;
import xyz.pangosoft.dtodo.service.IRentabilidadProductoReporteService;
import xyz.pangosoft.dtodo.service.IReporteService;
import xyz.pangosoft.dtodo.service.IResumenNotasCreditoReporteService;
import xyz.pangosoft.dtodo.service.IVentasClienteReporteService;
import xyz.pangosoft.dtodo.service.IVentasProductoReporteService;
import xyz.pangosoft.dtodo.service.IValorizacionInventarioReporteService;

@Service
@RequiredArgsConstructor
public class ReporteServiceImpl implements IReporteService {

    private final IFacturaService facturaService;
    private final IInventarioMovimientosReporteService inventarioMovimientosReporteService;
    private final IProductoService productoService;
    private final IProformaService proformaService;
    private final IResumenNotasCreditoReporteService resumenNotasCreditoReporteService;
    private final IComprasPeriodoReporteService comprasPeriodoReporteService;
    private final IVentasProductoReporteService ventasProductoReporteService;
    private final IVentasClienteReporteService ventasClienteReporteService;
    private final IRentabilidadProductoReporteService rentabilidadProductoReporteService;
    private final IBajoStockReporteService bajoStockReporteService;
    private final IKardexProductoReporteService kardexProductoReporteService;
    private final IValorizacionInventarioReporteService valorizacionInventarioReporteService;
    private final IConversionProformasReporteService conversionProformasReporteService;
    private final IPendientesDespachoReporteService pendientesDespachoReporteService;
    private final IComprasProveedorProductoReporteService comprasProveedorProductoReporteService;

    @Override
    public byte[] generarPolizaIndividual(
            Integer idSucursal,
            Integer idUsuario,
            String fechaInicio,
            String fechaFin) {
        validarId(idSucursal, "La sucursal seleccionada no es válida.");
        validarId(idUsuario, "El usuario seleccionado no es válido.");
        validarRango(fechaInicio, fechaFin);
        return facturaService.resportDailySales(idSucursal, idUsuario, fechaInicio, fechaFin);
    }

    @Override
    public byte[] generarPolizaGeneral(Integer idSucursal, String fechaInicio, String fechaFin) {
        validarId(idSucursal, "La sucursal seleccionada no es válida.");
        validarRango(fechaInicio, fechaFin);
        return facturaService.reportGeneralPolicy(idSucursal, fechaInicio, fechaFin);
    }

    @Override
    public byte[] generarVentasProducto(
            Integer idSucursal,
            String fechaInicio,
            String fechaFin,
            Integer idCategoria,
            String formato) {
        validarId(idSucursal, "La sucursal seleccionada no es válida.");
        if (idCategoria != null) {
            validarId(idCategoria, "La categoría seleccionada no es válida.");
        }
        LocalDate[] rango = validarRango(fechaInicio, fechaFin);
        String formatoValido = validarFormato(formato);
        return ventasProductoReporteService.generar(
                idSucursal, rango[0], rango[1], idCategoria, formatoValido);
    }

    @Override
    public byte[] generarVentasCliente(
            Integer idSucursal,
            String fechaInicio,
            String fechaFin,
            Integer idCliente,
            String formato) {
        validarId(idSucursal, "La sucursal seleccionada no es válida.");
        if (idCliente != null) {
            validarId(idCliente, "El cliente seleccionado no es válido.");
        }
        LocalDate[] rango = validarRango(fechaInicio, fechaFin);
        String formatoValido = validarFormato(formato);
        return ventasClienteReporteService.generar(
                idSucursal, rango[0], rango[1], idCliente, formatoValido);
    }

    @Override
    public byte[] generarRentabilidadProducto(
            Integer idSucursal,
            String fechaInicio,
            String fechaFin,
            Integer idCategoria,
            String formato) {
        validarId(idSucursal, "La sucursal seleccionada no es válida.");
        if (idCategoria != null) {
            validarId(idCategoria, "La categoría seleccionada no es válida.");
        }
        LocalDate[] rango = validarRango(fechaInicio, fechaFin);
        String formatoValido = validarFormato(formato);
        return rentabilidadProductoReporteService.generar(
                idSucursal, rango[0], rango[1], idCategoria, formatoValido);
    }

    @Override
    public byte[] generarMovimientosInventario(
            Integer idSucursal,
            String fechaInicio,
            String fechaFin,
            String formato) {
        validarId(idSucursal, "La sucursal seleccionada no es válida.");
        LocalDate[] rango = validarRango(fechaInicio, fechaFin);
        String formatoValido = validarFormato(formato);
        return inventarioMovimientosReporteService.generar(
                idSucursal, rango[0], rango[1], formatoValido);
    }

    @Override
    public byte[] generarExistencias(Integer idSucursal) {
        validarId(idSucursal, "La sucursal seleccionada no es válida.");
        return productoService.productosExcel(idSucursal);
    }

    @Override
    public byte[] generarBajoStock(Integer idSucursal, Integer idCategoria, String formato) {
        validarId(idSucursal, "La sucursal seleccionada no es válida.");
        if (idCategoria != null) {
            validarId(idCategoria, "La categoría seleccionada no es válida.");
        }
        String formatoValido = validarFormato(formato);
        return bajoStockReporteService.generar(idSucursal, idCategoria, formatoValido);
    }

    @Override
    public byte[] generarKardexProducto(
            Integer idSucursal,
            Integer idProducto,
            String fechaInicio,
            String fechaFin,
            String formato) {
        validarId(idSucursal, "La sucursal seleccionada no es válida.");
        validarId(idProducto, "El producto seleccionado no es válido.");
        LocalDate[] rango = validarRango(fechaInicio, fechaFin);
        String formatoValido = validarFormato(formato);
        return kardexProductoReporteService.generar(
                idSucursal, idProducto, rango[0], rango[1], formatoValido);
    }

    @Override
    public byte[] generarValorizacionInventario(Integer idSucursal, String fechaCorte, String formato) {
        validarId(idSucursal, "La sucursal seleccionada no es válida.");
        LocalDate corte = validarFechaCorte(fechaCorte);
        String formatoValido = validarFormato(formato);
        return valorizacionInventarioReporteService.generar(idSucursal, corte, formatoValido);
    }

    @Override
    public byte[] generarProformas(
            Integer idUsuario,
            String fechaInicio,
            String fechaFin,
            boolean todas) {
        if (idUsuario != null) {
            validarId(idUsuario, "El usuario seleccionado no es válido.");
        }
        if (!todas) {
            validarRango(fechaInicio, fechaFin);
        }
        return proformaService.proformasExcel(fechaInicio, fechaFin, todas, idUsuario);
    }

    @Override
    public byte[] generarConversionProformas(
            Integer idSucursal,
            Integer idUsuario,
            String fechaInicio,
            String fechaFin,
            String formato) {
        validarId(idSucursal, "La sucursal seleccionada no es válida.");
        validarId(idUsuario, "El usuario seleccionado no es válido.");
        LocalDate[] rango = validarRango(fechaInicio, fechaFin);
        String formatoValido = validarFormato(formato);
        return conversionProformasReporteService.generar(
                idSucursal, rango[0], rango[1], idUsuario, formatoValido);
    }

    @Override
    public byte[] generarResumenNotasCredito(
            Integer idSucursal,
            String fechaInicio,
            String fechaFin,
            String estado,
            String formato) {
        validarId(idSucursal, "La sucursal seleccionada no es válida.");
        LocalDate[] rango = validarRango(fechaInicio, fechaFin);
        EstadoNotaCreditoEnum estadoValido = validarEstadoNotaCredito(estado);
        String formatoValido = validarFormato(formato);
        return resumenNotasCreditoReporteService.generar(
                idSucursal, rango[0], rango[1], estadoValido, formatoValido);
    }

    @Override
    public byte[] generarPendientesDespacho(
            Integer idSucursal,
            String fechaInicio,
            String fechaFin,
            Integer idCliente,
            String formato) {
        validarId(idSucursal, "La sucursal seleccionada no es válida.");
        if (idCliente != null) {
            validarId(idCliente, "El cliente seleccionado no es válido.");
        }
        LocalDate[] rango = validarRango(fechaInicio, fechaFin);
        String formatoValido = validarFormato(formato);
        return pendientesDespachoReporteService.generar(
                idSucursal, rango[0], rango[1], idCliente, formatoValido);
    }

    @Override
    public byte[] generarComprasPeriodo(
            Integer idSucursal,
            String fechaInicio,
            String fechaFin,
            Integer idProveedor,
            String estado,
            String formato) {
        validarId(idSucursal, "La sucursal seleccionada no es válida.");
        if (idProveedor != null) {
            validarId(idProveedor, "El proveedor seleccionado no es válido.");
        }
        LocalDate[] rango = validarRango(fechaInicio, fechaFin);
        EstadoCompraEnum estadoValido = validarEstadoCompra(estado);
        String formatoValido = validarFormato(formato);
        return comprasPeriodoReporteService.generar(
                idSucursal, rango[0], rango[1], idProveedor, estadoValido, formatoValido);
    }

    @Override
    public byte[] generarComprasProveedorProducto(
            Integer idSucursal,
            String fechaInicio,
            String fechaFin,
            Integer idProveedor) {
        validarId(idSucursal, "La sucursal seleccionada no es válida.");
        if (idProveedor != null) {
            validarId(idProveedor, "El proveedor seleccionado no es válido.");
        }
        LocalDate[] rango = validarRango(fechaInicio, fechaFin);
        return comprasProveedorProductoReporteService.generar(
                idSucursal, rango[0], rango[1], idProveedor);
    }

    private EstadoNotaCreditoEnum validarEstadoNotaCredito(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        try {
            return EstadoNotaCreditoEnum.valueOf(estado.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("El estado de nota de crédito no es válido.", exception);
        }
    }

    private EstadoCompraEnum validarEstadoCompra(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        try {
            return EstadoCompraEnum.valueOf(estado.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("El estado de compra no es válido.", exception);
        }
    }

    private String validarFormato(String formato) {
        String valor = formato == null ? "PDF" : formato.toUpperCase();
        if (!"PDF".equals(valor) && !"XLSX".equals(valor)) {
            throw new BadRequestException("El formato solicitado no es válido.", null);
        }
        return valor;
    }

    private LocalDate[] validarRango(String fechaInicio, String fechaFin) {
        if (fechaInicio == null || fechaFin == null || fechaInicio.isBlank() || fechaFin.isBlank()) {
            throw new BadRequestException("Debes indicar la fecha de inicio y la fecha final.", null);
        }
        try {
            LocalDate inicio = LocalDate.parse(fechaInicio);
            LocalDate fin = LocalDate.parse(fechaFin);
            if (fin.isBefore(inicio)) {
                throw new BadRequestException(
                        "La fecha final no puede ser anterior a la fecha de inicio.", null);
            }
            return new LocalDate[] { inicio, fin };
        } catch (DateTimeParseException exception) {
            throw new BadRequestException("Las fechas deben utilizar el formato yyyy-MM-dd.", exception);
        }
    }

    private LocalDate validarFechaCorte(String fechaCorte) {
        if (fechaCorte == null || fechaCorte.isBlank()) {
            throw new BadRequestException("Debes indicar la fecha de corte.", null);
        }
        try {
            LocalDate corte = LocalDate.parse(fechaCorte);
            if (corte.isAfter(LocalDate.now())) {
                throw new BadRequestException("La fecha de corte no puede ser futura.", null);
            }
            return corte;
        } catch (DateTimeParseException exception) {
            throw new BadRequestException("La fecha de corte debe utilizar el formato yyyy-MM-dd.", exception);
        }
    }

    private void validarId(Integer id, String mensaje) {
        if (id == null || id <= 0) {
            throw new BadRequestException(mensaje, null);
        }
    }
}
