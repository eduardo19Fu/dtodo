package xyz.pangosoft.dtodo.service;

/** El recibo de caja impreso (PDF, media carta). */
public interface IReciboCajaDocumentoService {

    /** El PDF del recibo; el cobrador solo imprime los de su sucursal (404 si es de otra). */
    byte[] imprimir(Long idRecibo);
}
