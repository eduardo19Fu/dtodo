package xyz.pangosoft.dtodo.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.pangosoft.dtodo.dto.CrearReciboCajaDto;
import xyz.pangosoft.dtodo.dto.ReciboCajaDto;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.Factura;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.ReciboCaja;
import xyz.pangosoft.dtodo.model.ReciboCorrelativo;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.FormaPagoReciboEnum;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;
import xyz.pangosoft.dtodo.repository.IClienteRepository;
import xyz.pangosoft.dtodo.repository.IFacturaRepository;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.repository.IReciboCajaRepository;
import xyz.pangosoft.dtodo.repository.IReciboCorrelativoRepository;
import xyz.pangosoft.dtodo.repository.ISucursalRepository;
import xyz.pangosoft.dtodo.service.IAmbitoVendedorService;
import xyz.pangosoft.dtodo.service.IProformaAuditoriaService;
import xyz.pangosoft.dtodo.service.IReciboCajaService;
import xyz.pangosoft.dtodo.service.IUsuarioService;
import xyz.pangosoft.dtodo.util.Utils;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReciboCajaServiceImpl implements IReciboCajaService {

    static final BigDecimal MONTO_MAXIMO = new BigDecimal("9999999.99");
    static final int DIAS_POR_DEFECTO = 30;

    private final IReciboCajaRepository reciboRepository;
    private final IReciboCorrelativoRepository correlativoRepository;
    private final IClienteRepository clienteRepository;
    private final IProformaRepository proformaRepository;
    private final IFacturaRepository facturaRepository;
    private final ISucursalRepository sucursalRepository;
    private final IUsuarioService usuarioService;
    private final IAmbitoVendedorService ambito;
    private final IProformaAuditoriaService auditoria;

    @Transactional
    @Override
    public ReciboCajaDto emitir(CrearReciboCajaDto solicitud) {
        if (solicitud == null) {
            throw new ReglaNegocioException("Indique los datos del recibo.");
        }
        Usuario usuario = usuarioDeLaSesion();
        Sucursal sucursal = sucursalDeEmision(usuario, solicitud.getIdSucursal());
        Cliente cliente = cliente(solicitud.getIdCliente());
        BigDecimal monto = validarMonto(solicitud.getMonto());
        FormaPagoReciboEnum forma = validarForma(solicitud.getFormaPago());
        String referencia = textoONulo(solicitud.getReferencia());
        if (forma.pideReferencia() && referencia == null) {
            throw new ReglaNegocioException("Un pago por " + forma.name().toLowerCase() + " necesita el número de referencia.");
        }
        validarLongitud(referencia, 100, "La referencia");
        String observaciones = textoONulo(solicitud.getObservaciones());
        validarLongitud(observaciones, 255, "Las observaciones");
        LocalDate fechaPago = solicitud.getFechaPago() == null ? LocalDate.now() : solicitud.getFechaPago();
        if (fechaPago.isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("La fecha de pago no puede ser futura.");
        }
        Proforma proforma = proformaDelCliente(solicitud.getIdProforma(), cliente);
        Factura factura = facturaDelCliente(solicitud.getIdFactura(), cliente);
        if (proforma != null && factura != null && !proforma.getIdProforma().equals(factura.getIdProformaOrigen())) {
            throw new ReglaNegocioException("La factura " + factura.getNoFactura() + " no corresponde al pedido " + proforma.getNoProforma() + ".");
        }

        ReciboCaja recibo = ReciboCaja.builder()
                .sucursal(sucursal)
                .numero(siguienteNumero(sucursal.getIdSucursal()))
                .cliente(cliente)
                .proforma(proforma)
                .factura(factura)
                .monto(monto)
                .formaPago(forma)
                .referencia(referencia)
                .observaciones(observaciones)
                .fechaPago(fechaPago)
                .fechaRegistro(LocalDateTime.now())
                .usuario(usuario)
                .build();
        recibo = reciboRepository.save(recibo);
        log.info("Recibo {} emitido: cliente={}, monto={}", numeroVisible(recibo), cliente.getIdCliente(), monto);
        return aDto(recibo);
    }

    @Transactional
    @Override
    public ReciboCajaDto anular(Long idRecibo, String motivo) {
        String motivoLimpio = auditoria.motivoObligatorio(motivo, "anular un recibo de caja");
        ReciboCaja recibo = reciboRepository.findById(idRecibo)
                .orElseThrow(() -> new NotFoundException("El recibo solicitado no se encuentra registrado."));
        if (recibo.isAnulado()) {
            throw new ReglaNegocioException("El recibo " + numeroVisible(recibo) + " ya está anulado.");
        }
        recibo.setAnulado(true);
        recibo.setFechaAnulacion(LocalDateTime.now());
        recibo.setUsuarioAnulacion(usuarioDeLaSesion());
        recibo.setMotivoAnulacion(motivoLimpio.length() > 500 ? motivoLimpio.substring(0, 500) : motivoLimpio);
        log.info("Recibo {} anulado", numeroVisible(recibo));
        return aDto(reciboRepository.save(recibo));
    }

    @Transactional(readOnly = true)
    @Override
    public ReciboCajaDto buscar(Long idRecibo) {
        ReciboCaja recibo = reciboRepository.findById(idRecibo).orElse(null);
        if (recibo == null || !puedeVerSucursal(recibo.getSucursal().getIdSucursal())) {
            throw new NotFoundException("El recibo solicitado no se encuentra registrado.");
        }
        return aDto(recibo);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ReciboCajaDto> listar(Integer idSucursal, Integer idCliente, LocalDate desde, LocalDate hasta, boolean incluirAnulados,
            Pageable pageable) {
        Integer sucursal = idSucursal;
        if (!Utils.esAdministrador()) {
            sucursal = sucursalPropia(usuarioDeLaSesion()).getIdSucursal();
        }
        LocalDate fin = hasta == null ? LocalDate.now() : hasta;
        LocalDate inicio = desde == null ? fin.minusDays(DIAS_POR_DEFECTO) : desde;
        if (inicio.isAfter(fin)) {
            throw new ReglaNegocioException("La fecha inicial no puede ser posterior a la final.");
        }
        return reciboRepository.buscar(sucursal, idCliente, inicio, fin, incluirAnulados, pageable).map(this::aDto);
    }

    // ------------------------------------------------------------------------------------------ apoyo

    private Usuario usuarioDeLaSesion() {
        Integer idUsuario = ambito.idUsuarioActual();
        if (idUsuario == null) {
            throw new ReglaNegocioException("No se pudo identificar al usuario de la sesión.");
        }
        return usuarioService.findById(idUsuario);
    }

    private static Sucursal sucursalPropia(Usuario usuario) {
        if (usuario.getSucursal() == null) {
            throw new ReglaNegocioException("Su usuario no tiene una sucursal asignada; pida al administrador que se la asigne.");
        }
        return usuario.getSucursal();
    }

    /** El administrador puede emitir desde otra sucursal; los demás, solo desde la suya. */
    private Sucursal sucursalDeEmision(Usuario usuario, Integer idSolicitada) {
        if (Utils.esAdministrador() && idSolicitada != null) {
            return sucursalRepository.findById(idSolicitada)
                    .orElseThrow(() -> new ReglaNegocioException("La sucursal indicada no existe."));
        }
        return sucursalPropia(usuario);
    }

    private boolean puedeVerSucursal(Integer idSucursal) {
        return Utils.esAdministrador() || sucursalPropia(usuarioDeLaSesion()).getIdSucursal().equals(idSucursal);
    }

    private Cliente cliente(Integer idCliente) {
        if (idCliente == null) {
            throw new ReglaNegocioException("Indique el cliente del recibo.");
        }
        return clienteRepository.findById(idCliente).orElseThrow(() -> new ReglaNegocioException("El cliente indicado no existe."));
    }

    static BigDecimal validarMonto(BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) {
            throw new ReglaNegocioException("El monto del recibo debe ser mayor que cero.");
        }
        BigDecimal redondeado = monto.setScale(2, RoundingMode.HALF_UP);
        if (redondeado.signum() <= 0 || redondeado.compareTo(MONTO_MAXIMO) > 0) {
            throw new ReglaNegocioException("El monto del recibo no es válido.");
        }
        return redondeado;
    }

    static FormaPagoReciboEnum validarForma(String forma) {
        try {
            return FormaPagoReciboEnum.valueOf(forma == null ? "" : forma.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ReglaNegocioException("La forma de pago debe ser efectivo, depósito o transferencia.");
        }
    }

    private Proforma proformaDelCliente(Long idProforma, Cliente cliente) {
        if (idProforma == null) {
            return null;
        }
        Proforma proforma = proformaRepository.findById(idProforma)
                .orElseThrow(() -> new ReglaNegocioException("El pedido indicado no existe."));
        if (proforma.getTipoProforma() != TipoProformaEnum.MAYORISTA || proforma.getCliente() == null
                || !proforma.getCliente().getIdCliente().equals(cliente.getIdCliente())) {
            throw new ReglaNegocioException("El pedido " + proforma.getNoProforma() + " no es de este cliente.");
        }
        if (proforma.getEstado() != null && "ANULADO".equals(proforma.getEstado().getEstado())) {
            throw new ReglaNegocioException("El pedido " + proforma.getNoProforma() + " está anulado.");
        }
        return proforma;
    }

    private Factura facturaDelCliente(Long idFactura, Cliente cliente) {
        if (idFactura == null) {
            return null;
        }
        Factura factura = facturaRepository.findById(idFactura)
                .orElseThrow(() -> new ReglaNegocioException("La factura indicada no existe."));
        if (factura.getCliente() == null || !factura.getCliente().getIdCliente().equals(cliente.getIdCliente())) {
            throw new ReglaNegocioException("La factura " + factura.getNoFactura() + " no es de este cliente.");
        }
        if (factura.getEstado() != null && "ANULADO".equals(factura.getEstado().getEstado())) {
            throw new ReglaNegocioException("La factura " + factura.getNoFactura() + " está anulada.");
        }
        return factura;
    }

    /** Toma el siguiente número de la sucursal con la fila bloqueada, para que dos recibos simultáneos no lo repitan. */
    private Long siguienteNumero(Integer idSucursal) {
        ReciboCorrelativo correlativo = correlativoRepository.bloquear(idSucursal)
                .orElseGet(() -> new ReciboCorrelativo(idSucursal, 0L));
        correlativo.setUltimo(correlativo.getUltimo() + 1);
        correlativoRepository.save(correlativo);
        return correlativo.getUltimo();
    }

    static String numeroVisible(ReciboCaja recibo) {
        return String.format("S%d-%06d", recibo.getSucursal().getIdSucursal(), recibo.getNumero());
    }

    private ReciboCajaDto aDto(ReciboCaja recibo) {
        ReciboCajaDto dto = new ReciboCajaDto();
        dto.setIdRecibo(recibo.getIdRecibo());
        dto.setNumero(recibo.getNumero());
        dto.setNumeroVisible(numeroVisible(recibo));
        dto.setIdSucursal(recibo.getSucursal().getIdSucursal());
        dto.setSucursal(recibo.getSucursal().getNombre());
        dto.setIdCliente(recibo.getCliente().getIdCliente());
        dto.setCliente(recibo.getCliente().getNombre());
        if (recibo.getProforma() != null) {
            dto.setIdProforma(recibo.getProforma().getIdProforma());
            dto.setNoProforma(recibo.getProforma().getNoProforma());
        }
        if (recibo.getFactura() != null) {
            dto.setIdFactura(recibo.getFactura().getIdFactura());
            dto.setNoFactura(recibo.getFactura().getNoFactura());
        }
        dto.setMonto(recibo.getMonto());
        dto.setFormaPago(recibo.getFormaPago().name());
        dto.setReferencia(recibo.getReferencia());
        dto.setObservaciones(recibo.getObservaciones());
        dto.setFechaPago(recibo.getFechaPago());
        dto.setFechaRegistro(recibo.getFechaRegistro());
        dto.setUsuario(nombre(recibo.getUsuario()));
        dto.setAnulado(recibo.isAnulado());
        dto.setFechaAnulacion(recibo.getFechaAnulacion());
        dto.setUsuarioAnulacion(nombre(recibo.getUsuarioAnulacion()));
        dto.setMotivoAnulacion(recibo.getMotivoAnulacion());
        return dto;
    }

    private static String nombre(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return ((usuario.getPrimerNombre() == null ? "" : usuario.getPrimerNombre()) + " "
                + (usuario.getApellido() == null ? "" : usuario.getApellido())).trim();
    }

    private static String textoONulo(String texto) {
        return texto == null || texto.trim().isEmpty() ? null : texto.trim();
    }

    private static void validarLongitud(String texto, int maximo, String campo) {
        if (texto != null && texto.length() > maximo) {
            throw new ReglaNegocioException(campo + " no puede pasar de " + maximo + " caracteres.");
        }
    }
}
