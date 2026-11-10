package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import xyz.pangosoft.dtodo.dto.ChequeDto;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.ChequeRecibo;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.ReciboCaja;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.EstadoChequeEnum;
import xyz.pangosoft.dtodo.model.enums.FormaPagoReciboEnum;
import xyz.pangosoft.dtodo.repository.IChequeReciboRepository;
import xyz.pangosoft.dtodo.repository.IReciboCajaRepository;
import xyz.pangosoft.dtodo.service.IAmbitoVendedorService;
import xyz.pangosoft.dtodo.service.IUsuarioService;

class ChequeServiceImplTest {

    private final IChequeReciboRepository chequeRepository = mock(IChequeReciboRepository.class);
    private final IReciboCajaRepository reciboRepository = mock(IReciboCajaRepository.class);
    private final IUsuarioService usuarioService = mock(IUsuarioService.class);
    private final IAmbitoVendedorService ambito = mock(IAmbitoVendedorService.class);
    private final ChequeServiceImpl service = new ChequeServiceImpl(chequeRepository, reciboRepository, usuarioService, ambito);

    private final Sucursal centro = Sucursal.builder().idSucursal(2).nombre("Centro").build();
    private final Usuario cobrador = Usuario.builder().idUsuario(7).primerNombre("Carla").apellido("Mena").sucursal(centro).build();

    @BeforeEach
    void escenario() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u", "x", Collections.singletonList(new SimpleGrantedAuthority("ROLE_COBRADOR"))));
        when(ambito.idUsuarioActual()).thenReturn(7);
        when(usuarioService.findById(7)).thenReturn(cobrador);
        when(chequeRepository.save(any(ChequeRecibo.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private ChequeRecibo cheque(EstadoChequeEnum estado, LocalDate fechaCheque, Sucursal sucursal) {
        ReciboCaja recibo = ReciboCaja.builder().idRecibo(10L).numero(4L).sucursal(sucursal)
                .cliente(Cliente.builder().idCliente(5).nombre("Libreria Uno").build()).monto(new BigDecimal("500.00"))
                .formaPago(FormaPagoReciboEnum.CHEQUE).fechaPago(LocalDate.now()).build();
        ChequeRecibo cheque = ChequeRecibo.builder().idCheque(1L).recibo(recibo).banco("Banrural").numeroCheque("123")
                .fechaCheque(fechaCheque).estado(estado).build();
        when(chequeRepository.findById(1L)).thenReturn(Optional.of(cheque));
        return cheque;
    }

    @Test
    void recibidoPasaADepositadoYLuegoACobrado() {
        ChequeRecibo cheque = cheque(EstadoChequeEnum.RECIBIDO, LocalDate.now(), centro);

        assertEquals("DEPOSITADO", service.cambiarEstado(1L, "depositado", null).getEstado());
        ChequeDto cobrado = service.cambiarEstado(1L, "COBRADO", null);

        assertEquals("COBRADO", cobrado.getEstado());
        assertEquals("Carla Mena", cobrado.getUsuarioEstado());
        assertEquals(EstadoChequeEnum.COBRADO, cheque.getEstado());
    }

    @Test
    void losEstadosFinalesYLosSaltosNoSePermiten() {
        cheque(EstadoChequeEnum.RECIBIDO, LocalDate.now(), centro);
        assertThrows(ReglaNegocioException.class, () -> service.cambiarEstado(1L, "COBRADO", null));

        cheque(EstadoChequeEnum.COBRADO, LocalDate.now(), centro);
        assertThrows(ReglaNegocioException.class, () -> service.cambiarEstado(1L, "RECHAZADO", "sin fondos"));
        assertThrows(ReglaNegocioException.class, () -> service.cambiarEstado(1L, "raro", null));
        assertThrows(ReglaNegocioException.class, () -> service.cambiarEstado(1L, null, null));
    }

    @Test
    void unPostfechadoNoSeDepositaAntesDeSuFecha() {
        ChequeRecibo cheque = cheque(EstadoChequeEnum.RECIBIDO, LocalDate.now().plusDays(5), centro);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> service.cambiarEstado(1L, "DEPOSITADO", null));

        assertTrue(error.getMessage().contains("postfechado"), error.getMessage());
        assertEquals(EstadoChequeEnum.RECIBIDO, cheque.getEstado());
        assertTrue(ChequeServiceImpl.aDto(cheque).isPostfechado());
    }

    @Test
    void rechazarPideMotivoYAnulaElRecibo() {
        ChequeRecibo cheque = cheque(EstadoChequeEnum.DEPOSITADO, LocalDate.now(), centro);

        assertThrows(ReglaNegocioException.class, () -> service.cambiarEstado(1L, "RECHAZADO", "  "));
        assertFalse(cheque.getRecibo().isAnulado());

        ChequeDto dto = service.cambiarEstado(1L, "RECHAZADO", "Fondos insuficientes");

        assertEquals("RECHAZADO", dto.getEstado());
        assertEquals("Fondos insuficientes", dto.getMotivoRechazo());
        assertTrue(cheque.getRecibo().isAnulado());
        assertTrue(cheque.getRecibo().getMotivoAnulacion().contains("Fondos insuficientes"));
        verify(reciboRepository).save(cheque.getRecibo());
    }

    @Test
    void elCobradorNoVeNiCambiaChequesDeOtraSucursal() {
        Sucursal norte = Sucursal.builder().idSucursal(3).nombre("Norte").build();
        cheque(EstadoChequeEnum.RECIBIDO, LocalDate.now(), norte);

        assertThrows(NotFoundException.class, () -> service.cambiarEstado(1L, "DEPOSITADO", null));
        verify(chequeRepository, never()).save(any(ChequeRecibo.class));
    }
}
