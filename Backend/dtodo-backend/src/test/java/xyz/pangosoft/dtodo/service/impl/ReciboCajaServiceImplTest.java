package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import xyz.pangosoft.dtodo.dto.CrearReciboCajaDto;
import xyz.pangosoft.dtodo.dto.ReciboCajaDto;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.Estado;
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
import xyz.pangosoft.dtodo.service.IUsuarioService;

class ReciboCajaServiceImplTest {

    private final IReciboCajaRepository reciboRepository = mock(IReciboCajaRepository.class);
    private final xyz.pangosoft.dtodo.repository.IChequeReciboRepository chequeRepository = mock(xyz.pangosoft.dtodo.repository.IChequeReciboRepository.class);
    private final IReciboCorrelativoRepository correlativoRepository = mock(IReciboCorrelativoRepository.class);
    private final IClienteRepository clienteRepository = mock(IClienteRepository.class);
    private final IProformaRepository proformaRepository = mock(IProformaRepository.class);
    private final IFacturaRepository facturaRepository = mock(IFacturaRepository.class);
    private final ISucursalRepository sucursalRepository = mock(ISucursalRepository.class);
    private final IUsuarioService usuarioService = mock(IUsuarioService.class);
    private final IAmbitoVendedorService ambito = mock(IAmbitoVendedorService.class);
    private final IProformaAuditoriaService auditoria = mock(IProformaAuditoriaService.class);
    private final ReciboCajaServiceImpl service = new ReciboCajaServiceImpl(reciboRepository, chequeRepository, correlativoRepository, clienteRepository,
            proformaRepository, facturaRepository, sucursalRepository, usuarioService, ambito, auditoria);

    private final Sucursal centro = Sucursal.builder().idSucursal(2).nombre("Centro").build();
    private final Sucursal norte = Sucursal.builder().idSucursal(3).nombre("Norte").build();
    private final Usuario cobrador = Usuario.builder().idUsuario(7).primerNombre("Carla").apellido("Mena").sucursal(centro).build();
    private final Cliente cliente = Cliente.builder().idCliente(5).nombre("Libreria Uno").build();

    @BeforeEach
    void escenario() {
        autenticar("ROLE_COBRADOR");
        when(ambito.idUsuarioActual()).thenReturn(7);
        when(usuarioService.findById(7)).thenReturn(cobrador);
        when(clienteRepository.findById(5)).thenReturn(Optional.of(cliente));
        when(sucursalRepository.findById(3)).thenReturn(Optional.of(norte));
        when(correlativoRepository.bloquear(2)).thenReturn(Optional.of(new ReciboCorrelativo(2, 14L)));
        when(reciboRepository.save(any(ReciboCaja.class))).thenAnswer(inv -> {
            ReciboCaja r = inv.getArgument(0);
            if (r.getIdRecibo() == null) {
                r.setIdRecibo(99L);
            }
            return r;
        });
        when(auditoria.motivoObligatorio(any(), any())).thenAnswer(inv -> {
            String motivo = inv.getArgument(0);
            if (motivo == null || motivo.trim().isEmpty()) {
                throw new ReglaNegocioException("Indique el motivo");
            }
            return motivo.trim();
        });
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private static void autenticar(String rol) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u", "x", Collections.singletonList(new SimpleGrantedAuthority(rol))));
    }

    private CrearReciboCajaDto solicitud() {
        CrearReciboCajaDto dto = new CrearReciboCajaDto();
        dto.setIdCliente(5);
        dto.setMonto(new BigDecimal("1500.505"));
        dto.setFormaPago("efectivo");
        return dto;
    }

    private Proforma pedido(long id, Cliente deCliente, String estado) {
        Proforma p = new Proforma();
        p.setIdProforma(id);
        p.setNoProforma("P-" + id);
        p.setTipoProforma(TipoProformaEnum.MAYORISTA);
        p.setCliente(deCliente);
        p.setEstado(Estado.builder().estado(estado).build());
        when(proformaRepository.findById(id)).thenReturn(Optional.of(p));
        return p;
    }

    private Factura factura(long id, Cliente deCliente, Long origen, String estado) {
        Factura f = new Factura();
        f.setIdFactura(id);
        f.setNoFactura(1000 + id);
        f.setCliente(deCliente);
        f.setIdProformaOrigen(origen);
        f.setEstado(Estado.builder().estado(estado).build());
        when(facturaRepository.findById(id)).thenReturn(Optional.of(f));
        return f;
    }

    // ------------------------------------------------------------------------------------------ emitir

    @Test
    void emiteConElSiguienteNumeroDeLaSucursalDelUsuario() {
        ReciboCajaDto recibo = service.emitir(solicitud());

        assertEquals(Long.valueOf(15), recibo.getNumero());
        assertEquals("S2-000015", recibo.getNumeroVisible());
        assertEquals(Integer.valueOf(2), recibo.getIdSucursal());
        assertEquals(new BigDecimal("1500.51"), recibo.getMonto());
        assertEquals("EFECTIVO", recibo.getFormaPago());
        assertEquals(LocalDate.now(), recibo.getFechaPago());
        assertEquals("Carla Mena", recibo.getUsuario());
        assertFalse(recibo.isAnulado());
        ArgumentCaptor<ReciboCorrelativo> captor = ArgumentCaptor.forClass(ReciboCorrelativo.class);
        verify(correlativoRepository).save(captor.capture());
        assertEquals(Long.valueOf(15), captor.getValue().getUltimo());
    }

    @Test
    void unaSucursalSinCorrelativoEmpiezaEnUno() {
        when(correlativoRepository.bloquear(2)).thenReturn(Optional.empty());

        assertEquals(Long.valueOf(1), service.emitir(solicitud()).getNumero());
    }

    @Test
    void elCobradorNoPuedeElegirOtraSucursalPeroElAdministradorSi() {
        CrearReciboCajaDto dto = solicitud();
        dto.setIdSucursal(3);
        when(correlativoRepository.bloquear(3)).thenReturn(Optional.of(new ReciboCorrelativo(3, 0L)));

        assertEquals(Integer.valueOf(2), service.emitir(dto).getIdSucursal());

        autenticar("ROLE_ADMIN");
        ReciboCajaDto delAdmin = service.emitir(dto);
        assertEquals(Integer.valueOf(3), delAdmin.getIdSucursal());
        assertEquals("S3-000001", delAdmin.getNumeroVisible());
    }

    @Test
    void sinSucursalAsignadaNoSePuedeEmitir() {
        cobrador.setSucursal(null);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> service.emitir(solicitud()));

        assertTrue(error.getMessage().contains("sucursal"));
        verify(reciboRepository, never()).save(any());
    }

    @Test
    void validaClienteMontoYFormaDePago() {
        CrearReciboCajaDto sinCliente = solicitud();
        sinCliente.setIdCliente(null);
        assertThrows(ReglaNegocioException.class, () -> service.emitir(sinCliente));

        CrearReciboCajaDto clienteInexistente = solicitud();
        clienteInexistente.setIdCliente(77);
        assertThrows(ReglaNegocioException.class, () -> service.emitir(clienteInexistente));

        for (String monto : new String[] { "0", "-5", "0.004", "10000000" }) {
            CrearReciboCajaDto dto = solicitud();
            dto.setMonto(new BigDecimal(monto));
            assertThrows(ReglaNegocioException.class, () -> service.emitir(dto), monto);
        }
        CrearReciboCajaDto sinMonto = solicitud();
        sinMonto.setMonto(null);
        assertThrows(ReglaNegocioException.class, () -> service.emitir(sinMonto));

        CrearReciboCajaDto formaRara = solicitud();
        formaRara.setFormaPago("tarjeta");
        assertThrows(ReglaNegocioException.class, () -> service.emitir(formaRara));
        verify(reciboRepository, never()).save(any());
    }

    @Test
    void elDepositoYLaTransferenciaPidenReferencia() {
        for (String forma : new String[] { "DEPOSITO", "transferencia" }) {
            CrearReciboCajaDto dto = solicitud();
            dto.setFormaPago(forma);
            dto.setReferencia("  ");
            assertThrows(ReglaNegocioException.class, () -> service.emitir(dto), forma);

            dto.setReferencia(" 998877 ");
            assertEquals("998877", service.emitir(dto).getReferencia());
        }
        assertTrue(FormaPagoReciboEnum.DEPOSITO.pideReferencia());
        assertTrue(FormaPagoReciboEnum.CHEQUE.pideReferencia());
        assertFalse(FormaPagoReciboEnum.EFECTIVO.pideReferencia());
    }

    @Test
    void rechazaTextosLargosYFechasFuturas() {
        CrearReciboCajaDto largo = solicitud();
        largo.setObservaciones(String.join("", Collections.nCopies(256, "x")));
        assertThrows(ReglaNegocioException.class, () -> service.emitir(largo));

        CrearReciboCajaDto futuro = solicitud();
        futuro.setFechaPago(LocalDate.now().plusDays(1));
        assertThrows(ReglaNegocioException.class, () -> service.emitir(futuro));

        CrearReciboCajaDto antiguo = solicitud();
        antiguo.setFechaPago(LocalDate.now().minusDays(3));
        assertEquals(LocalDate.now().minusDays(3), service.emitir(antiguo).getFechaPago());
    }

    @Test
    void elPedidoYLaFacturaDebenSerDelClienteYNoEstarAnulados() {
        Cliente otro = Cliente.builder().idCliente(6).nombre("Otro").build();
        pedido(30, cliente, "ACTIVO");
        pedido(31, otro, "ACTIVO");
        pedido(32, cliente, "ANULADO");
        factura(40, cliente, 30L, "ACTIVO");
        factura(41, otro, null, "ACTIVO");
        factura(42, cliente, 30L, "ANULADO");
        factura(43, cliente, 99L, "ACTIVO");

        CrearReciboCajaDto bueno = solicitud();
        bueno.setIdProforma(30L);
        bueno.setIdFactura(40L);
        ReciboCajaDto recibo = service.emitir(bueno);
        assertEquals("P-30", recibo.getNoProforma());
        assertEquals(Long.valueOf(1040), recibo.getNoFactura());

        assertRechazado(31L, null);
        assertRechazado(32L, null);
        assertRechazado(77L, null);
        assertRechazado(null, 41L);
        assertRechazado(null, 42L);
        assertRechazado(null, 77L);
        assertRechazado(30L, 43L);
    }

    private void assertRechazado(Long idProforma, Long idFactura) {
        CrearReciboCajaDto dto = solicitud();
        dto.setIdProforma(idProforma);
        dto.setIdFactura(idFactura);
        assertThrows(ReglaNegocioException.class, () -> service.emitir(dto), idProforma + "/" + idFactura);
    }

    @Test
    void soloUnPedidoMayoristaSirveDeDestino() {
        Proforma minorista = pedido(33, cliente, "ACTIVO");
        minorista.setTipoProforma(TipoProformaEnum.MINORISTA);
        CrearReciboCajaDto dto = solicitud();
        dto.setIdProforma(33L);

        assertThrows(ReglaNegocioException.class, () -> service.emitir(dto));
    }

    // ------------------------------------------------------------------------------------------ anular

    private ReciboCaja guardado(boolean anulado) {
        ReciboCaja recibo = ReciboCaja.builder().idRecibo(50L).sucursal(centro).numero(9L).cliente(cliente)
                .monto(new BigDecimal("100.00")).formaPago(FormaPagoReciboEnum.EFECTIVO).fechaPago(LocalDate.now())
                .usuario(cobrador).anulado(anulado).build();
        when(reciboRepository.findById(50L)).thenReturn(Optional.of(recibo));
        return recibo;
    }

    @Test
    void anularPideMotivoYDejaConstancia() {
        ReciboCaja recibo = guardado(false);
        autenticar("ROLE_ADMIN");

        assertThrows(ReglaNegocioException.class, () -> service.anular(50L, " "));
        assertFalse(recibo.isAnulado());

        ReciboCajaDto anulado = service.anular(50L, "  monto equivocado ");

        assertTrue(anulado.isAnulado());
        assertEquals("monto equivocado", anulado.getMotivoAnulacion());
        assertEquals("Carla Mena", anulado.getUsuarioAnulacion());
        assertTrue(anulado.getFechaAnulacion() != null);
    }

    @Test
    void noSeAnulaDosVecesNiUnReciboInexistente() {
        guardado(true);

        assertThrows(ReglaNegocioException.class, () -> service.anular(50L, "otra vez"));
        assertThrows(NotFoundException.class, () -> service.anular(51L, "no existe"));
    }

    // ------------------------------------------------------------------------------------------ consultar

    @Test
    void elCobradorSoloVeLosRecibosDeSuSucursal() {
        ReciboCaja propio = guardado(false);
        assertEquals("S2-000009", service.buscar(50L).getNumeroVisible());

        propio.setSucursal(norte);
        assertThrows(NotFoundException.class, () -> service.buscar(50L));

        autenticar("ROLE_ADMIN");
        assertEquals("S3-000009", service.buscar(50L).getNumeroVisible());
        when(reciboRepository.findById(51L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.buscar(51L));
    }

    @Test
    void laListaDelCobradorSeLimitaASuSucursalYLaDelAdminUsaElFiltro() {
        ReciboCaja recibo = guardado(false);
        when(reciboRepository.buscar(any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean(), any()))
                .thenReturn(new PageImpl<>(Arrays.asList(recibo)));

        assertEquals(1, service.listar(3, null, null, null, false, PageRequest.of(0, 10)).getTotalElements());
        verify(reciboRepository).buscar(org.mockito.ArgumentMatchers.eq(2), any(), any(), any(),
                org.mockito.ArgumentMatchers.eq(false), any());

        autenticar("ROLE_ADMIN");
        service.listar(3, null, null, null, true, PageRequest.of(0, 10));
        verify(reciboRepository).buscar(org.mockito.ArgumentMatchers.eq(3), any(), any(), any(),
                org.mockito.ArgumentMatchers.eq(true), any());
    }

    @Test
    void sinFechasListaLosUltimosTreintaDiasYRechazaUnRangoInvertido() {
        when(reciboRepository.buscar(any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        service.listar(null, null, null, null, false, PageRequest.of(0, 10));

        ArgumentCaptor<LocalDate> desde = ArgumentCaptor.forClass(LocalDate.class);
        verify(reciboRepository).buscar(any(), any(), desde.capture(), any(), org.mockito.ArgumentMatchers.anyBoolean(), any());
        assertEquals(LocalDate.now().minusDays(30), desde.getValue());
        assertThrows(ReglaNegocioException.class,
                () -> service.listar(null, null, LocalDate.now(), LocalDate.now().minusDays(1), false, PageRequest.of(0, 10)));
        assertNull(null);
    }

    private CrearReciboCajaDto conCheque(String banco, String numero, LocalDate fecha) {
        CrearReciboCajaDto dto = solicitud();
        dto.setFormaPago("CHEQUE");
        xyz.pangosoft.dtodo.dto.DatosChequeDto cheque = new xyz.pangosoft.dtodo.dto.DatosChequeDto();
        cheque.setBanco(banco);
        cheque.setNumeroCheque(numero);
        cheque.setFechaCheque(fecha);
        dto.setCheque(cheque);
        return dto;
    }

    @Test
    void unPagoConChequeGuardaSusDatosEnRecibidoYArmaLaReferencia() {
        when(chequeRepository.save(any(xyz.pangosoft.dtodo.model.ChequeRecibo.class))).thenAnswer(inv -> inv.getArgument(0));

        service.emitir(conCheque(" Banrural ", "123456", LocalDate.now().plusDays(10)));

        ArgumentCaptor<xyz.pangosoft.dtodo.model.ChequeRecibo> guardado = ArgumentCaptor.forClass(xyz.pangosoft.dtodo.model.ChequeRecibo.class);
        verify(chequeRepository).save(guardado.capture());
        assertEquals("Banrural", guardado.getValue().getBanco());
        assertEquals("123456", guardado.getValue().getNumeroCheque());
        assertEquals(xyz.pangosoft.dtodo.model.enums.EstadoChequeEnum.RECIBIDO, guardado.getValue().getEstado());
        ArgumentCaptor<ReciboCaja> recibo = ArgumentCaptor.forClass(ReciboCaja.class);
        verify(reciboRepository).save(recibo.capture());
        assertEquals("Banrural #123456", recibo.getValue().getReferencia());
    }

    @Test
    void unChequeSinDatosOIncompletoORepetidoSeRechaza() {
        CrearReciboCajaDto sinDatos = solicitud();
        sinDatos.setFormaPago("CHEQUE");
        assertThrows(ReglaNegocioException.class, () -> service.emitir(sinDatos));
        assertThrows(ReglaNegocioException.class, () -> service.emitir(conCheque("", "1", LocalDate.now())));
        assertThrows(ReglaNegocioException.class, () -> service.emitir(conCheque("Banrural", " ", LocalDate.now())));
        assertThrows(ReglaNegocioException.class, () -> service.emitir(conCheque("Banrural", "1", null)));

        when(chequeRepository.existeVigente("Banrural", "1")).thenReturn(true);
        ReglaNegocioException repetido = assertThrows(ReglaNegocioException.class,
                () -> service.emitir(conCheque("Banrural", "1", LocalDate.now())));
        assertTrue(repetido.getMessage().contains("ya está registrado"), repetido.getMessage());
        verify(chequeRepository, never()).save(any(xyz.pangosoft.dtodo.model.ChequeRecibo.class));
    }
}
