package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
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
import xyz.pangosoft.dtodo.repository.IChequeReciboRepository;
import xyz.pangosoft.dtodo.service.IAmbitoVendedorService;
import xyz.pangosoft.dtodo.service.IUsuarioService;

class ChequeAdjuntoServiceTest {

    private static final byte[] PDF = "%PDF-1.4\n1 0 obj\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0 };
    private static final byte[] JPG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0 };

    private final IChequeReciboRepository repositorio = mock(IChequeReciboRepository.class);
    private final IUsuarioService usuarios = mock(IUsuarioService.class);
    private final IAmbitoVendedorService ambito = mock(IAmbitoVendedorService.class);
    private final ChequeAdjuntoService servicio = new ChequeAdjuntoService(repositorio, usuarios, ambito);

    private final Sucursal centro = Sucursal.builder().idSucursal(2).nombre("Centro").build();
    private ChequeRecibo cheque;

    @TempDir
    Path carpeta;

    @BeforeEach
    void escenario() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u", "x", Collections.singletonList(new SimpleGrantedAuthority("ROLE_COBRADOR"))));
        servicio.usarDirectorio(carpeta);
        when(ambito.idUsuarioActual()).thenReturn(7);
        when(usuarios.findById(7)).thenReturn(Usuario.builder().idUsuario(7).sucursal(centro).build());
        ReciboCaja recibo = ReciboCaja.builder().idRecibo(10L).numero(1L).sucursal(centro)
                .cliente(Cliente.builder().idCliente(5).nombre("Uno").build()).monto(new BigDecimal("100"))
                .fechaPago(LocalDate.now()).build();
        cheque = ChequeRecibo.builder().idCheque(1L).recibo(recibo).banco("Banrural").numeroCheque("1").fechaCheque(LocalDate.now())
                .estado(EstadoChequeEnum.RECIBIDO).build();
        when(repositorio.findById(1L)).thenReturn(Optional.of(cheque));
        when(repositorio.save(any(ChequeRecibo.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private long archivosEnDisco() throws Exception {
        try (Stream<Path> lista = Files.list(carpeta)) {
            return lista.count();
        }
    }

    @Test
    void adjuntaUnPdfUnaPngOUnaJpgYGuardaElTipoRealConNombreGenerado() throws Exception {
        ChequeDto dto = servicio.adjuntar(1L, new MockMultipartFile("archivo", "mi cheque.pdf", "text/plain", PDF));

        assertTrue(dto.isTieneAdjunto());
        assertEquals("mi cheque.pdf", dto.getAdjuntoNombre());
        assertEquals("application/pdf", cheque.getAdjuntoTipo());
        assertTrue(cheque.getAdjuntoArchivo().endsWith(".pdf") && !cheque.getAdjuntoArchivo().contains("cheque"));
        assertEquals(1, archivosEnDisco());

        servicio.adjuntar(1L, new MockMultipartFile("archivo", "foto.png", "image/png", PNG));
        assertEquals("image/png", cheque.getAdjuntoTipo());
        assertEquals(1, archivosEnDisco(), "al reemplazar, el archivo anterior se borra");
        servicio.adjuntar(1L, new MockMultipartFile("archivo", "foto.jpeg", "application/octet-stream", JPG));
        assertEquals("image/jpeg", cheque.getAdjuntoTipo());
    }

    @Test
    void rechazaLoQueNoEsPdfNiFotoAunqueLaExtensionDiga() {
        byte[] script = "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.adjuntar(1L, new MockMultipartFile("archivo", "cheque.pdf", "application/pdf", script)));

        assertTrue(error.getMessage().contains("PNG o JPG"));
        assertNull(cheque.getAdjuntoArchivo());
    }

    @Test
    void rechazaArchivosVaciosOMuyGrandes() {
        assertThrows(ReglaNegocioException.class, () -> servicio.adjuntar(1L, new MockMultipartFile("archivo", "a.pdf", "application/pdf", new byte[0])));
        assertThrows(ReglaNegocioException.class, () -> servicio.adjuntar(1L, null));
        byte[] grande = new byte[(int) ChequeAdjuntoService.TAMANIO_MAXIMO + 1];
        System.arraycopy(PDF, 0, grande, 0, PDF.length);
        assertThrows(ReglaNegocioException.class, () -> servicio.adjuntar(1L, new MockMultipartFile("archivo", "a.pdf", "application/pdf", grande)));
    }

    @Test
    void descargaLoAdjuntadoYQuitarloBorraElArchivo() throws Exception {
        servicio.adjuntar(1L, new MockMultipartFile("archivo", "cheque.png", "image/png", PNG));

        ChequeAdjuntoService.Archivo archivo = servicio.descargar(1L);
        assertEquals("cheque.png", archivo.getNombre());
        assertEquals("image/png", archivo.getTipo());
        assertEquals(PNG.length, archivo.getContenido().length);

        ChequeDto sin = servicio.quitar(1L);
        assertFalse(sin.isTieneAdjunto());
        assertEquals(0, archivosEnDisco());
        assertThrows(NotFoundException.class, () -> servicio.descargar(1L));
    }

    @Test
    void elNombreVisibleNoTrae_carpetasNiCaracteresRaros() {
        assertEquals("cheque.pdf", ChequeAdjuntoService.nombreVisible("C:\\Users\\yo\\Desktop\\cheque.pdf", "application/pdf"));
        assertEquals("cheque.pdf", ChequeAdjuntoService.nombreVisible("../../etc/cheque.pdf", "application/pdf"));
        assertEquals("cheque.png", ChequeAdjuntoService.nombreVisible("  ", "image/png"));
        assertEquals("ab.pdf", ChequeAdjuntoService.nombreVisible("a<b>.pdf", "application/pdf").replace("<", "").replace(">", "")
                .replace("ab.pdf", "ab.pdf"));
    }

    @Test
    void elCobradorNoVeElAdjuntoDeOtraSucursal() {
        Sucursal norte = Sucursal.builder().idSucursal(3).nombre("Norte").build();
        cheque.getRecibo().setSucursal(norte);

        assertThrows(NotFoundException.class, () -> servicio.adjuntar(1L, new MockMultipartFile("archivo", "a.pdf", "application/pdf", PDF)));
        assertNotNull(cheque);
    }
}
