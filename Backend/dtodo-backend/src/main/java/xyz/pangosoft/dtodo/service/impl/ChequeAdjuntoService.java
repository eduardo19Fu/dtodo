package xyz.pangosoft.dtodo.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.pangosoft.dtodo.dto.ChequeDto;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.ChequeRecibo;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.repository.IChequeReciboRepository;
import xyz.pangosoft.dtodo.service.IAmbitoVendedorService;
import xyz.pangosoft.dtodo.service.IUsuarioService;
import xyz.pangosoft.dtodo.util.Utils;

/**
 * La foto o el PDF de un cheque. Se guarda en una carpeta del servidor que no es pública: solo se descarga con sesión iniciada
 * y con acceso a la sucursal del cheque. El tipo se verifica por el contenido del archivo (no por su extensión ni por lo que
 * diga el navegador) y el nombre en disco lo genera el servidor.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChequeAdjuntoService {

    static final long TAMANIO_MAXIMO = 5L * 1024 * 1024;

    private final IChequeReciboRepository chequeRepository;
    private final IUsuarioService usuarioService;
    private final IAmbitoVendedorService ambito;

    @Value("${dtodo.cheques.directorio:uploads/cheques}")
    private String directorio = "uploads/cheques";

    /** Un archivo ya leído: lo que se descarga. */
    public static final class Archivo {
        private final String nombre;
        private final String tipo;
        private final byte[] contenido;

        Archivo(String nombre, String tipo, byte[] contenido) {
            this.nombre = nombre;
            this.tipo = tipo;
            this.contenido = contenido;
        }

        public String getNombre() {
            return nombre;
        }

        public String getTipo() {
            return tipo;
        }

        public byte[] getContenido() {
            return contenido;
        }
    }

    void usarDirectorio(Path carpeta) {
        this.directorio = carpeta.toString();
    }

    @Transactional
    public ChequeDto adjuntar(Long idCheque, MultipartFile archivo) {
        ChequeRecibo cheque = cargar(idCheque);
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("Elija el archivo del cheque (foto o PDF).");
        }
        if (archivo.getSize() > TAMANIO_MAXIMO) {
            throw new ReglaNegocioException("El archivo pesa más de 5 MB. Reduzca la foto o use un PDF más liviano.");
        }
        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException exception) {
            throw new ReglaNegocioException("No se pudo leer el archivo.");
        }
        String tipo = tipoPorContenido(contenido);
        if (tipo == null) {
            throw new ReglaNegocioException("El archivo debe ser una foto (PNG o JPG) o un PDF.");
        }
        String guardado = UUID.randomUUID().toString().replace("-", "") + extension(tipo);
        try {
            Path carpeta = Paths.get(directorio).toAbsolutePath().normalize();
            Files.createDirectories(carpeta);
            Path destino = carpeta.resolve(guardado).normalize();
            if (!destino.startsWith(carpeta)) {
                throw new ReglaNegocioException("El nombre del archivo no es válido.");
            }
            Files.copy(new java.io.ByteArrayInputStream(contenido), destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            log.error("No se pudo guardar el adjunto del cheque {}", idCheque, exception);
            throw new ReglaNegocioException("No se pudo guardar el archivo. Intente de nuevo.");
        }
        borrarArchivo(cheque.getAdjuntoArchivo());
        cheque.setAdjuntoNombre(nombreVisible(archivo.getOriginalFilename(), tipo));
        cheque.setAdjuntoArchivo(guardado);
        cheque.setAdjuntoTipo(tipo);
        return ChequeServiceImpl.aDto(chequeRepository.save(cheque));
    }

    @Transactional(readOnly = true)
    public Archivo descargar(Long idCheque) {
        ChequeRecibo cheque = cargar(idCheque);
        if (cheque.getAdjuntoArchivo() == null) {
            throw new NotFoundException("El cheque no tiene un archivo adjunto.");
        }
        try {
            Path carpeta = Paths.get(directorio).toAbsolutePath().normalize();
            Path ruta = carpeta.resolve(cheque.getAdjuntoArchivo()).normalize();
            if (!ruta.startsWith(carpeta) || !Files.isRegularFile(ruta)) {
                throw new NotFoundException("El archivo del cheque ya no está disponible.");
            }
            return new Archivo(cheque.getAdjuntoNombre(), cheque.getAdjuntoTipo(), Files.readAllBytes(ruta));
        } catch (IOException exception) {
            throw new NotFoundException("El archivo del cheque ya no está disponible.");
        }
    }

    @Transactional
    public ChequeDto quitar(Long idCheque) {
        ChequeRecibo cheque = cargar(idCheque);
        borrarArchivo(cheque.getAdjuntoArchivo());
        cheque.setAdjuntoNombre(null);
        cheque.setAdjuntoArchivo(null);
        cheque.setAdjuntoTipo(null);
        return ChequeServiceImpl.aDto(chequeRepository.save(cheque));
    }

    // ------------------------------------------------------------------------------------------ apoyo

    private ChequeRecibo cargar(Long idCheque) {
        ChequeRecibo cheque = chequeRepository.findById(idCheque).orElse(null);
        Integer idUsuario = ambito.idUsuarioActual();
        Usuario usuario = idUsuario == null ? null : usuarioService.findById(idUsuario);
        boolean visible = cheque != null && usuario != null && (Utils.esAdministrador()
                || (usuario.getSucursal() != null
                        && usuario.getSucursal().getIdSucursal().equals(cheque.getRecibo().getSucursal().getIdSucursal())));
        if (!visible) {
            throw new NotFoundException("El cheque solicitado no se encuentra registrado.");
        }
        return cheque;
    }

    /** PDF, PNG o JPG según los primeros bytes del archivo; nulo si es otra cosa. */
    static String tipoPorContenido(byte[] contenido) {
        if (contenido.length >= 5 && contenido[0] == '%' && contenido[1] == 'P' && contenido[2] == 'D' && contenido[3] == 'F' && contenido[4] == '-') {
            return "application/pdf";
        }
        if (contenido.length >= 8 && (contenido[0] & 0xFF) == 0x89 && contenido[1] == 'P' && contenido[2] == 'N' && contenido[3] == 'G') {
            return "image/png";
        }
        if (contenido.length >= 3 && (contenido[0] & 0xFF) == 0xFF && (contenido[1] & 0xFF) == 0xD8 && (contenido[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        return null;
    }

    private static String extension(String tipo) {
        switch (tipo) {
            case "application/pdf":
                return ".pdf";
            case "image/png":
                return ".png";
            default:
                return ".jpg";
        }
    }

    /** El nombre que ve el usuario: sin carpetas ni caracteres raros y de a lo más 200 caracteres. */
    static String nombreVisible(String original, String tipo) {
        String base = original == null ? "" : original.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}\"<>|:*?]", "").trim();
        if (base.isEmpty()) {
            base = "cheque" + extension(tipo);
        }
        return base.length() > 200 ? base.substring(base.length() - 200) : base;
    }

    private void borrarArchivo(String guardado) {
        if (guardado == null) {
            return;
        }
        try {
            Path carpeta = Paths.get(directorio).toAbsolutePath().normalize();
            Path ruta = carpeta.resolve(guardado).normalize();
            if (ruta.startsWith(carpeta)) {
                Files.deleteIfExists(ruta);
            }
        } catch (IOException exception) {
            log.warn("No se pudo borrar el archivo anterior del cheque: {}", guardado);
        }
    }
}
