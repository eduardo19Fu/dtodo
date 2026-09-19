package xyz.pangosoft.dtodo.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.error.exceptions.ReportGenerationException;
import xyz.pangosoft.dtodo.dto.UsuarioDto;
import xyz.pangosoft.dtodo.service.IFacturaService;
import xyz.pangosoft.dtodo.service.IMovimientoProductoService;
import xyz.pangosoft.dtodo.service.IProductoService;
import xyz.pangosoft.dtodo.service.IProformaService;
import xyz.pangosoft.dtodo.service.IReporteService;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReporteServiceImpl implements IReporteService {

    private final IFacturaService facturaService;
    private final IMovimientoProductoService movimientoProductoService;
    private final IProductoService productoService;
    private final IProformaService proformaService;

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
    public byte[] generarMovimientosInventario(Integer idSucursal, String fechaInicio, String fechaFin) {
        validarId(idSucursal, "La sucursal seleccionada no es válida.");
        LocalDate[] rango = validarRango(fechaInicio, fechaFin);
        try {
            Date inicio = java.sql.Date.valueOf(rango[0]);
            Date fin = java.sql.Date.valueOf(rango[1]);
            return movimientoProductoService.inventory(inicio, fin, idSucursal);
        } catch (Exception exception) {
            log.error("No fue posible generar el reporte de movimientos de inventario", exception);
            throw new ReportGenerationException(
                    "No fue posible generar el reporte de movimientos de inventario.", exception);
        }
    }

    @Override
    public byte[] generarExistencias(Integer idSucursal) {
        validarId(idSucursal, "La sucursal seleccionada no es válida.");
        return productoService.productosExcel(idSucursal);
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
    public List<UsuarioDto> listarUsuariosProformas() {
        return proformaService.findUsuariosExportacion();
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

    private void validarId(Integer id, String mensaje) {
        if (id == null || id <= 0) {
            throw new BadRequestException(mensaje, null);
        }
    }
}
