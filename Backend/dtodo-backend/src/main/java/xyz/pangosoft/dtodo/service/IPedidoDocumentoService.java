package xyz.pangosoft.dtodo.service;

/** Documentos impresos del pedido mayorista para bodega y la carga a ruta. Ninguno lleva precios. */
public interface IPedidoDocumentoService {

    /** Hoja de picking del pedido (PDF). */
    byte[] picking(Long idProforma);

    /** Lista de empaque del pedido (PDF), con la observación general del pedido. */
    byte[] empaque(Long idProforma);

    /**
     * Nota de envío de una ruta (PDF): los pedidos empacados por cargar y los que salieron a ruta en la fecha
     * indicada ({@code yyyy-MM-dd}; hoy si viene vacía).
     */
    byte[] notaEnvio(Integer idRuta, String fecha);

    /**
     * Las notas de envío de todas las rutas que tienen algo por cargar o que salieron en la fecha, en un solo PDF
     * (una nota por ruta, cada una con su propia numeración de páginas).
     */
    byte[] notaEnvioTodasLasRutas(String fecha);
}
