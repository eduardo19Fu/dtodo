package xyz.pangosoft.dtodo.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.pangosoft.dtodo.dto.ProformaAuditoriaDto;
import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.DetalleProforma;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.ProformaAuditoria;
import xyz.pangosoft.dtodo.model.enums.AccionAuditoriaEnum;
import xyz.pangosoft.dtodo.repository.IProformaAuditoriaRepository;
import xyz.pangosoft.dtodo.service.IProformaAuditoriaService;
import xyz.pangosoft.dtodo.service.IUsuarioService;
import xyz.pangosoft.dtodo.util.Utils;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProformaAuditoriaServiceImpl implements IProformaAuditoriaService {

    static final int MAX_DETALLE = 4000;

    private final IProformaAuditoriaRepository auditoriaRepository;
    private final IUsuarioService usuarioService;

    @Transactional
    @Override
    public void registrar(Long idProforma, AccionAuditoriaEnum accion, String motivo, String detalle) {
        Integer idUsuario = Utils.idUsuarioAutenticado();
        if (idProforma == null || idUsuario == null) {
            // Todo endpoint que audita está protegido: sin usuario en el token no hay a quién atribuir el cambio.
            log.warn("No se registró la auditoría {} de la proforma {}: no hay usuario autenticado.", accion, idProforma);
            return;
        }
        auditoriaRepository.save(ProformaAuditoria.builder()
                .idProforma(idProforma)
                .usuario(usuarioService.findById(idUsuario))
                .accion(accion)
                .motivo(motivo)
                .detalle(recortar(detalle))
                .fecha(LocalDateTime.now())
                .build());
    }

    @Transactional(readOnly = true)
    @Override
    public List<ProformaAuditoriaDto> historial(Long idProforma) {
        return auditoriaRepository.findHistorial(idProforma);
    }

    @Override
    public String motivoObligatorio(String motivo, String queHace) {
        String limpio = motivoOpcional(motivo);
        if (limpio == null) {
            throw new ReglaNegocioException("Indique el motivo para " + queHace + ".");
        }
        return limpio;
    }

    @Override
    public String motivoOpcional(String motivo) {
        if (motivo == null || motivo.trim().isEmpty()) {
            return null;
        }
        String limpio = motivo.trim();
        if (limpio.length() > MAX_MOTIVO) {
            throw new ReglaNegocioException("El motivo admite hasta " + MAX_MOTIVO + " caracteres y trae " + limpio.length() + ".");
        }
        return limpio;
    }

    @Override
    public String resumen(Proforma proforma) {
        if (proforma == null) {
            return "";
        }
        int lineas = proforma.getItemsProforma() == null ? 0 : proforma.getItemsProforma().size();
        float descuentoMaximo = 0;
        if (proforma.getItemsProforma() != null) {
            for (DetalleProforma linea : proforma.getItemsProforma()) {
                if (linea != null && linea.getDescuento() != null && linea.getDescuento() > descuentoMaximo) {
                    descuentoMaximo = linea.getDescuento();
                }
            }
        }
        String cliente = proforma.getCliente() == null || proforma.getCliente().getNombre() == null ? "sin cliente"
                : proforma.getCliente().getNombre().trim();
        StringBuilder texto = new StringBuilder();
        texto.append("Tipo ").append(proforma.getTipoProforma() == null ? "MINORISTA" : proforma.getTipoProforma())
                .append("; cliente ").append(cliente)
                .append("; ").append(lineas).append(lineas == 1 ? " línea" : " líneas")
                .append("; total Q").append(proforma.getTotal() == null ? "0.00"
                        : proforma.getTotal().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
                .append("; descuento máximo ").append(String.format(Locale.ROOT, "%.2f", descuentoMaximo)).append(" %");
        if (proforma.getEstadoAutorizacion() != null) {
            texto.append("; autorización ").append(proforma.getEstadoAutorizacion());
        }
        return texto.toString();
    }

    private static String recortar(String detalle) {
        if (detalle == null) {
            return null;
        }
        return detalle.length() > MAX_DETALLE ? detalle.substring(0, MAX_DETALLE) : detalle;
    }
}
