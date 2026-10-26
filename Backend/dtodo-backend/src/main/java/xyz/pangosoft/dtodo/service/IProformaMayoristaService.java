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
}
