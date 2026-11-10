package xyz.pangosoft.dtodo.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.pangosoft.dtodo.dto.ChequeDto;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.ChequeRecibo;
import xyz.pangosoft.dtodo.model.ReciboCaja;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.EstadoChequeEnum;
import xyz.pangosoft.dtodo.repository.IChequeReciboRepository;
import xyz.pangosoft.dtodo.repository.IReciboCajaRepository;
import xyz.pangosoft.dtodo.service.IAmbitoVendedorService;
import xyz.pangosoft.dtodo.service.IChequeService;
import xyz.pangosoft.dtodo.service.IUsuarioService;
import xyz.pangosoft.dtodo.util.Utils;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChequeServiceImpl implements IChequeService {

    static final int DIAS_POR_DEFECTO = 30;

    private final IChequeReciboRepository chequeRepository;
    private final IReciboCajaRepository reciboRepository;
    private final IUsuarioService usuarioService;
    private final IAmbitoVendedorService ambito;

    @Transactional(readOnly = true)
    @Override
    public Page<ChequeDto> listar(Integer idSucursal, Integer idCliente, String estado, LocalDate desde, LocalDate hasta,
            Pageable pageable) {
        LocalDate[] rango = rango(desde, hasta);
        return chequeRepository.pagina(sucursalVisible(idSucursal), idCliente, estado(estado), rango[0], rango[1], pageable)
                .map(ChequeServiceImpl::aDto);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ChequeDto> todos(Integer idSucursal, Integer idCliente, String estado, LocalDate desde, LocalDate hasta) {
        LocalDate[] rango = rango(desde, hasta);
        return chequeRepository.buscar(sucursalVisible(idSucursal), idCliente, estado(estado), rango[0], rango[1]).stream()
                .map(ChequeServiceImpl::aDto).collect(Collectors.toList());
    }

    @Transactional
    @Override
    public ChequeDto cambiarEstado(Long idCheque, String estado, String motivo) {
        ChequeRecibo cheque = chequeRepository.findById(idCheque).orElse(null);
        Usuario usuario = usuarioDeLaSesion();
        if (cheque == null || !puedeVerSucursal(usuario, cheque.getRecibo().getSucursal().getIdSucursal())) {
            throw new NotFoundException("El cheque solicitado no se encuentra registrado.");
        }
        EstadoChequeEnum destino = estado(estado);
        if (destino == null) {
            throw new ReglaNegocioException("Indique el nuevo estado del cheque.");
        }
        if (!cheque.getEstado().puedePasarA(destino)) {
            throw new ReglaNegocioException("Un cheque " + texto(cheque.getEstado()) + " no puede pasar a " + texto(destino) + ".");
        }
        if (destino == EstadoChequeEnum.DEPOSITADO && cheque.getFechaCheque().isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("El cheque es postfechado: se puede depositar a partir del "
                    + cheque.getFechaCheque().format(CobranzaServiceImpl.FECHA_VISIBLE) + ".");
        }
        String motivoLimpio = motivo == null ? null : motivo.trim();
        if (destino == EstadoChequeEnum.RECHAZADO) {
            if (motivoLimpio == null || motivoLimpio.isEmpty()) {
                throw new ReglaNegocioException("Indique el motivo por el que se rechaza el cheque (por ejemplo, fondos insuficientes).");
            }
            if (motivoLimpio.length() > 500) {
                motivoLimpio = motivoLimpio.substring(0, 500);
            }
            anularRecibo(cheque.getRecibo(), usuario, motivoLimpio);
            cheque.setMotivoRechazo(motivoLimpio);
        }
        cheque.setEstado(destino);
        cheque.setFechaEstado(LocalDateTime.now());
        cheque.setUsuarioEstado(usuario);
        log.info("Cheque {} pasó a {}", idCheque, destino);
        return aDto(chequeRepository.save(cheque));
    }

    /** Un cheque rechazado no es un pago: su recibo se anula y deja de contar para el saldo del cliente. */
    private void anularRecibo(ReciboCaja recibo, Usuario usuario, String motivo) {
        if (recibo.isAnulado()) {
            return;
        }
        recibo.setAnulado(true);
        recibo.setFechaAnulacion(LocalDateTime.now());
        recibo.setUsuarioAnulacion(usuario);
        String texto = "Cheque rechazado: " + motivo;
        recibo.setMotivoAnulacion(texto.length() > 500 ? texto.substring(0, 500) : texto);
        reciboRepository.save(recibo);
    }

    // ------------------------------------------------------------------------------------------ apoyo

    private Usuario usuarioDeLaSesion() {
        Integer idUsuario = ambito.idUsuarioActual();
        if (idUsuario == null) {
            throw new ReglaNegocioException("No se pudo identificar al usuario de la sesión.");
        }
        return usuarioService.findById(idUsuario);
    }

    /** El administrador ve todas las sucursales (o la que pida); el cobrador, solo la suya. */
    private Integer sucursalVisible(Integer idSolicitada) {
        if (Utils.esAdministrador()) {
            return idSolicitada;
        }
        return sucursalPropia(usuarioDeLaSesion());
    }

    private static Integer sucursalPropia(Usuario usuario) {
        if (usuario.getSucursal() == null) {
            throw new ReglaNegocioException("Su usuario no tiene una sucursal asignada; pida al administrador que se la asigne.");
        }
        return usuario.getSucursal().getIdSucursal();
    }

    private static boolean puedeVerSucursal(Usuario usuario, Integer idSucursal) {
        return Utils.esAdministrador() || sucursalPropia(usuario).equals(idSucursal);
    }

    static LocalDate[] rango(LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta == null ? LocalDate.now() : hasta;
        LocalDate inicio = desde == null ? fin.minusDays(DIAS_POR_DEFECTO) : desde;
        if (inicio.isAfter(fin)) {
            throw new ReglaNegocioException("La fecha inicial no puede ser posterior a la final.");
        }
        return new LocalDate[] { inicio, fin };
    }

    static EstadoChequeEnum estado(String estado) {
        if (estado == null || estado.trim().isEmpty()) {
            return null;
        }
        try {
            return EstadoChequeEnum.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ReglaNegocioException("El estado del cheque debe ser recibido, depositado, cobrado o rechazado.");
        }
    }

    static String texto(EstadoChequeEnum estado) {
        return estado.name().toLowerCase();
    }

    static ChequeDto aDto(ChequeRecibo cheque) {
        ReciboCaja recibo = cheque.getRecibo();
        ChequeDto dto = new ChequeDto();
        dto.setIdCheque(cheque.getIdCheque());
        dto.setIdRecibo(recibo.getIdRecibo());
        dto.setNumeroRecibo(ReciboCajaServiceImpl.numeroVisible(recibo));
        dto.setIdSucursal(recibo.getSucursal().getIdSucursal());
        dto.setSucursal(recibo.getSucursal().getNombre());
        dto.setIdCliente(recibo.getCliente().getIdCliente());
        dto.setCliente(recibo.getCliente().getNombre());
        dto.setMonto(recibo.getMonto());
        dto.setFechaPago(recibo.getFechaPago());
        dto.setBanco(cheque.getBanco());
        dto.setNumeroCheque(cheque.getNumeroCheque());
        dto.setGirador(cheque.getGirador());
        dto.setFechaCheque(cheque.getFechaCheque());
        dto.setPostfechado(cheque.getEstado() == EstadoChequeEnum.RECIBIDO && cheque.getFechaCheque().isAfter(LocalDate.now()));
        dto.setNotas(cheque.getNotas());
        dto.setEstado(cheque.getEstado().name());
        dto.setFechaEstado(cheque.getFechaEstado());
        Usuario usuario = cheque.getUsuarioEstado();
        if (usuario != null) {
            dto.setUsuarioEstado(((usuario.getPrimerNombre() == null ? "" : usuario.getPrimerNombre()) + " "
                    + (usuario.getApellido() == null ? "" : usuario.getApellido())).trim());
        }
        dto.setMotivoRechazo(cheque.getMotivoRechazo());
        return dto;
    }
}
