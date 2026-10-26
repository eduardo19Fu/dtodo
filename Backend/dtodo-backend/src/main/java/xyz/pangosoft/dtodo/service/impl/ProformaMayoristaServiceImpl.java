package xyz.pangosoft.dtodo.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.DetalleProforma;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.enums.TipoClienteEnum;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;
import xyz.pangosoft.dtodo.repository.IClienteRepository;
import xyz.pangosoft.dtodo.service.IAmbitoVendedorService;
import xyz.pangosoft.dtodo.service.IProformaMayoristaService;

@Service
@RequiredArgsConstructor
public class ProformaMayoristaServiceImpl implements IProformaMayoristaService {

    static final int MAX_OBSERVACION_LINEA = 255;
    static final int MAX_OBSERVACION_GENERAL = 500;

    private final IClienteRepository clienteRepository;
    private final IAmbitoVendedorService ambito;

    @Transactional(readOnly = true)
    @Override
    public void preparar(Proforma proforma) {
        TipoProformaEnum tipo = proforma.getTipoProforma() == null ? TipoProformaEnum.MINORISTA : proforma.getTipoProforma();
        if (ambito.esVendedor()) {
            tipo = TipoProformaEnum.MAYORISTA;
        }

        if (tipo == TipoProformaEnum.MAYORISTA) {
            Cliente cliente = proforma.getCliente() == null || proforma.getCliente().getIdCliente() == null ? null
                    : clienteRepository.findById(proforma.getCliente().getIdCliente()).orElse(null);
            if (cliente == null || cliente.getTipoCliente() != TipoClienteEnum.MAYORISTA || cliente.getRuta() == null) {
                throw new ReglaNegocioException("Una proforma mayorista solo puede hacerse a un cliente mayorista con ruta. "
                        + "Para otro cliente use una proforma normal.");
            }
            proforma.setRuta(cliente.getRuta());
        } else {
            proforma.setRuta(null);
        }
        proforma.setTipoProforma(tipo);

        proforma.setObservaciones(limpiar(proforma.getObservaciones(), MAX_OBSERVACION_GENERAL, "La observación general"));
        if (proforma.getItemsProforma() != null) {
            for (DetalleProforma linea : proforma.getItemsProforma()) {
                if (linea != null) {
                    String producto = linea.getProducto() == null || linea.getProducto().getNombre() == null ? "una línea"
                            : linea.getProducto().getNombre();
                    linea.setObservacion(limpiar(linea.getObservacion(), MAX_OBSERVACION_LINEA,
                            "La observación de " + producto));
                }
            }
        }
    }

    /** Texto sin espacios sobrantes; vacío es nulo; más largo que el máximo se rechaza en lugar de cortarse. */
    static String limpiar(String texto, int maximo, String que) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        String limpio = texto.trim();
        if (limpio.length() > maximo) {
            throw new ReglaNegocioException(que + " admite hasta " + maximo + " caracteres y trae " + limpio.length() + ".");
        }
        return limpio;
    }
}
