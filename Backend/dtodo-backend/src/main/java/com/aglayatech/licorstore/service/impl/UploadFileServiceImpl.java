package com.aglayatech.licorstore.service.impl;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.aglayatech.licorstore.service.IUploadFileService;

@Service
public class UploadFileServiceImpl implements IUploadFileService {
	
	private final Logger log = LoggerFactory.getLogger(UploadFileServiceImpl.class);
	
	private final static String DIRECTORIO_UPLOAD = "uploads";


	@Override
	public Resource cargar(String nombreImagen) throws MalformedURLException {
		
		Path rutaArchivo = getPath(nombreImagen);
		log.info(rutaArchivo.toString());
		
		Resource recurso = new UrlResource(rutaArchivo.toUri());
		
		if(!recurso.exists() && !recurso.isReadable()) {
			rutaArchivo = Paths.get("src/main/resources/static/images").resolve("no-image.jpg").toAbsolutePath();
			
			recurso = new UrlResource(rutaArchivo.toUri());
			
			log.error("Error no se puede cargar la imagen: " + nombreImagen);
		}
		
		return recurso;
	}

	@Override
	public String copiar(MultipartFile archivo) throws IOException {
		
		String nombreArchivo = UUID.randomUUID().toString().concat("_" + archivo.getOriginalFilename().replace(" ", ""));
		
		Path rutaArchivo = getPath(nombreArchivo);
		log.info(rutaArchivo.toString());
		
		Files.copy(archivo.getInputStream(), rutaArchivo);
		
		return nombreArchivo;
	}

	@Override
	public boolean eliminar(String nombreImagen) {
		
		if(nombreImagen != null && nombreImagen.length() > 0) {
			
			Path rutaImagenAnterior = Paths.get("uploads").resolve(nombreImagen).toAbsolutePath();
			File archivoImagenAnterior = rutaImagenAnterior.toFile();
			
			if(archivoImagenAnterior.exists() && archivoImagenAnterior.canRead()) {
				archivoImagenAnterior.delete();
				return true;
			}
		}
		
		return false;
	}

	@Override
	public Path getPath(String nombreImagen) {
		return Paths.get(DIRECTORIO_UPLOAD).resolve(nombreImagen).toAbsolutePath();
	}

}
