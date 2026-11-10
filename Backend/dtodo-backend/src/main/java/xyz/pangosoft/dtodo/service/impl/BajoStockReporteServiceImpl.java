package xyz.pangosoft.dtodo.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import xyz.pangosoft.dtodo.error.exceptions.ReportGenerationException;
import xyz.pangosoft.dtodo.util.ConsultaJrxml;
import xyz.pangosoft.dtodo.repository.ISucursalRepository;
import xyz.pangosoft.dtodo.repository.ITipoProductoRepository;
import xyz.pangosoft.dtodo.service.IBajoStockReporteService;
import xyz.pangosoft.dtodo.util.Utils;

@Service
@RequiredArgsConstructor
@Slf4j
public class BajoStockReporteServiceImpl implements IBajoStockReporteService {

    private static final String PLANTILLA = "/reports/bajo_stock.jrxml";

    private final DataSource dataSource;
    private final ISucursalRepository sucursalRepository;
    private final ITipoProductoRepository tipoProductoRepository;

    @Override
    public byte[] generar(Integer idSucursal, Integer idCategoria, String formato) {
        try (Connection connection = dataSource.getConnection();
             InputStream template = getClass().getResourceAsStream(PLANTILLA)) {
            if (template == null) {
                throw new ReportGenerationException(
                        "No se encontró la plantilla de productos bajo stock mínimo.", null);
            }

            JasperReport report = JasperCompileManager.compileReport(template);
            Map<String, Object> parametros = crearParametros(idSucursal, idCategoria, formato);
            return ReportesTabulares.bajoStock(ConsultaJrxml.filas(report, parametros, connection), parametros, formato);
        } catch (JRException | SQLException | IOException exception) {
            log.error("No fue posible generar productos bajo stock mínimo. sucursal={}, categoría={}, formato={}",
                    idSucursal, idCategoria, formato, exception);
            throw new ReportGenerationException(
                    "No fue posible generar el reporte de productos bajo stock mínimo.", exception);
        }
    }

    private Map<String, Object> crearParametros(Integer idSucursal, Integer idCategoria, String formato) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ID_SUCURSAL", idSucursal);
        parameters.put("SUCURSAL", sucursalRepository.findById(idSucursal)
                .map(sucursal -> sucursal.getNombre())
                .orElse("Sucursal #" + idSucursal));
        parameters.put("ID_CATEGORIA", idCategoria);
        parameters.put("CATEGORIA", idCategoria == null
                ? "Todas las categorías"
                : tipoProductoRepository.findById(idCategoria)
                        .map(tipo -> tipo.getTipoProducto())
                        .orElse("Categoría #" + idCategoria));
        parameters.put("FORMATO", formato);
        // El precio de compra y la inversión estimada solo los ve un administrador
        parameters.put("VER_COSTOS", Utils.esAdministrador());
        return parameters;
    }
}
