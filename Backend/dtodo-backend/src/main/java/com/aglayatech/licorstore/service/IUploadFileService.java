package com.aglayatech.licorstore.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface IUploadFileService {
	
	// Método servicio que devuelve el recurso requerido por el nombre de la imagen
	public Resource cargar(String nombreImagen) throws MalformedURLException;
	
	// Método servicio que permite almacenar la imagen enviada por api
	public String copiar(MultipartFile archivo) throws IOException;
	
	// Método de servicio que elimina la imagen asociada con el producto en caso de existir ya una
	public boolean eliminar(String nombreImagen);
	
	// Método servicio que devuelve la url de la imagen
	public Path getPath(String nombreImagen);
}
