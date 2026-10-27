package xyz.pangosoft.dtodo.service;

import java.util.List;

import xyz.pangosoft.dtodo.dto.AvanceComisionDto;
import xyz.pangosoft.dtodo.dto.LiquidarComisionDto;
import xyz.pangosoft.dtodo.dto.MetaComisionDto;

/**
 * Comisiones anuales de los vendedores. Se paga un porcentaje plano sobre todo lo entregado en el año a clientes de ruta,
 * según el nivel que alcance (no por tramos). Solo cobra quien el administrador marque como que completó el año. Un año
 * se liquida cuando termina y queda guardado: no se recalcula. Las reglas responden 422 (401 y 403 los reserva el
 * interceptor del frontend).
 */
public interface IComisionService {

    /** Las metas del año, de la más baja a la más alta; vacío si no se han definido. */
    List<MetaComisionDto> metas(int anio);

    /**
     * Reemplaza las metas del año. Cada nivel pide un monto y un porcentaje mayores que el anterior, y el porcentaje no
     * pasa de 100. No se puede en un año ya liquidado.
     */
    List<MetaComisionDto> guardarMetas(int anio, List<MetaComisionDto> metas);

    /** Copia las metas de un año a otro que todavía no tiene (y no está liquidado). */
    List<MetaComisionDto> copiarMetas(int desde, int hacia);

    /**
     * El avance del año (en vivo) o, si ya se liquidó, lo liquidado. Trae a todos los vendedores activos y a quien haya
     * entregado en el año. En el avance, la comisión es lo que cobraría si completó el año.
     */
    AvanceComisionDto avance(int anio);

    /**
     * Liquida un año terminado: guarda por vendedor lo entregado, la meta alcanzada, el porcentaje y la comisión (cero si
     * no completó el año), con quién liquidó y cuándo. Exige que el año haya terminado, que tenga metas y que no esté
     * liquidado.
     */
    AvanceComisionDto liquidar(int anio, LiquidarComisionDto decision);

    /** Deshace la liquidación de un año con motivo obligatorio; deja constancia de lo que se había liquidado. */
    AvanceComisionDto deshacerLiquidacion(int anio, String motivo);
}
