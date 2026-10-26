package xyz.pangosoft.dtodo.service;

import java.util.List;

import xyz.pangosoft.dtodo.dto.ProformaAuditoriaDto;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.enums.AccionAuditoriaEnum;

/**
 * Historial de lo que se hace sobre una proforma. El usuario sale siempre del token, nunca de la petición.
 */
public interface IProformaAuditoriaService {

    /** Largo máximo del motivo; coincide con la columna. */
    int MAX_MOTIVO = 500;

    /** Guarda un renglón del historial a nombre del usuario de la sesión. */
    void registrar(Long idProforma, AccionAuditoriaEnum accion, String motivo, String detalle);

    /** Historial de la proforma, del más reciente al más antiguo. */
    List<ProformaAuditoriaDto> historial(Long idProforma);

    /**
     * Motivo sin espacios sobrantes; 422 si viene vacío o excede {@link #MAX_MOTIVO}.
     *
     * @param queHace lo que se está intentando, para el mensaje ("editar una proforma mayorista")
     */
    String motivoObligatorio(String motivo, String queHace);

    /** Motivo opcional: nulo si viene vacío; 422 si excede {@link #MAX_MOTIVO}. */
    String motivoOpcional(String motivo);

    /** Una línea con lo esencial de la proforma, para comparar el antes y el después de una edición. */
    String resumen(Proforma proforma);
}
