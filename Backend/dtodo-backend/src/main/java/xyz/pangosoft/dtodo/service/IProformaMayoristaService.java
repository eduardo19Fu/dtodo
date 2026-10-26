package xyz.pangosoft.dtodo.service;

import xyz.pangosoft.dtodo.model.Proforma;

/**
 * Reglas de la proforma mayorista: solo se hace a un cliente mayorista y toma de él la ruta; las observaciones
 * (por línea y generales) se limpian y se limitan en largo. El vendedor siempre hace proformas mayoristas.
 */
public interface IProformaMayoristaService {

    /**
     * Deja la proforma lista para guardarse: fija su tipo (el vendedor siempre es mayorista), exige un cliente
     * mayorista con ruta cuando es mayorista y copia esa ruta, y normaliza las observaciones (422 si exceden el
     * largo permitido).
     */
    void preparar(Proforma proforma);

    /**
     * Fija el estado de autorización según los descuentos de las líneas (ya validados y tipados): una proforma
     * mayorista con una línea sobre el tope, que no sea de precio mayorista fijo, queda {@code PENDIENTE}; en
     * cualquier otro caso queda sin estado. Una edición vuelve a pedir autorización aunque estuviera aprobada.
     */
    void evaluarAutorizacion(Proforma proforma);

    /** Tope de descuento (en %) sobre el que se pide autorización. */
    double descuentoMaximo();
}
