package com.aglayatech.licorstore.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import java.io.UnsupportedEncodingException;

import java.lang.reflect.InvocationTargetException;

import java.math.BigDecimal;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import java.sql.Connection;
import java.sql.SQLException;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.sql.DataSource;

import com.aglayatech.licorstore.error.exceptions.NoContentException;
import com.aglayatech.licorstore.error.exceptions.NotFoundException;
import com.aglayatech.licorstore.error.exceptions.ReportGenerationException;
import com.aglayatech.licorstore.error.exceptions.SigningDocumentFelException;
import com.aglayatech.licorstore.model.Certificador;
import com.aglayatech.licorstore.model.Cliente;
import com.aglayatech.licorstore.model.Correlativo;
import com.aglayatech.licorstore.model.DetalleFactura;
import com.aglayatech.licorstore.model.Emisor;
import com.aglayatech.licorstore.model.Estado;
import com.aglayatech.licorstore.model.MovimientoProducto;
import com.aglayatech.licorstore.model.Producto;
import com.aglayatech.licorstore.model.TipoFactura;
import com.aglayatech.licorstore.model.Usuario;
import com.aglayatech.licorstore.model.enums.TipoMovimientoEnum;
import com.aglayatech.licorstore.repository.ITipoFacturaRepository;
import com.aglayatech.licorstore.service.ICertificadorService;
import com.aglayatech.licorstore.service.ICorrelativoService;
import com.aglayatech.licorstore.service.IEmisorService;
import com.aglayatech.licorstore.service.IEstadoService;
import com.aglayatech.licorstore.service.IMovimientoProductoService;
import com.aglayatech.licorstore.service.IProductoService;
import com.aglayatech.licorstore.service.ITipoFacturaService;
import com.aglayatech.licorstore.service.IUsuarioService;
import com.aglayatech.licorstore.util.Utils;

import com.fel.firma.emisor.FirmaEmisor;
import com.fel.firma.emisor.RespuestaServicioFirma;
import com.fel.validaciones.documento.Adendas;
import com.fel.validaciones.documento.AnulacionFel;
import com.fel.validaciones.documento.ConexionServicioFel;
import com.fel.validaciones.documento.DatosEmisor;
import com.fel.validaciones.documento.DatosGenerales;
import com.fel.validaciones.documento.DatosReceptor;
import com.fel.validaciones.documento.DocumentoFel;
import com.fel.validaciones.documento.Frases;
import com.fel.validaciones.documento.GenerarXml;
import com.fel.validaciones.documento.ImpuestosDetalle;
import com.fel.validaciones.documento.Items;
import com.fel.validaciones.documento.Respuesta;
import com.fel.validaciones.documento.RespuestaServicioFel;
import com.fel.validaciones.documento.ServicioFel;
import com.fel.validaciones.documento.TotalImpuestos;
import com.fel.validaciones.documento.Totales;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aglayatech.licorstore.model.Factura;
import com.aglayatech.licorstore.repository.IFacturaRepository;
import com.aglayatech.licorstore.service.IFacturaService;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacturaServiceImpl implements IFacturaService {

	private final IFacturaRepository repoFactura;
	private final ITipoFacturaRepository tipoFacturaRepository;
	private final IEmisorService emisorService;
	private final IEstadoService estadoService;
	private final ITipoFacturaService tipoFacturaService;
	private final ICorrelativoService correlativoService;
	private final ICertificadorService certificadorService;
	private final IMovimientoProductoService movimientoProductoService;
	private final IUsuarioService usuarioService;
	protected final DataSource localDataSource;

	@Override
	public List<Factura> findAll() {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		try {
			List<Factura> facturas = repoFactura.findAll(Sort.by(Direction.DESC, "fecha"));
			if(!facturas.isEmpty()) {
				log.info("Devolviendo listado de facturas");
				return facturas;
			} else {
				log.warn("No existen facturas registradas en la base de datos");
				throw new NoContentException("No existen facturas registradas en la base de datos");
			}
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de Base de Datos: {}", e.getMessage());
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos: " + e.getMessage(), e.getCause());
		}
	}

	@Override
	public List<Factura> findAllWithProcedure(Date date1, Date date2) {
		return this.repoFactura.findAllFacturas(date1, date2);
	}

	@Override
	public Page<Factura> findAll(Pageable pageable) {
		return repoFactura.findAll(pageable);
	}

	@Override
	public Factura findFactura(Long idfactura) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		try {
			Optional<Factura> factura = repoFactura.findById(idfactura);
			if(factura.isPresent()) {
				log.info("Devolviendo factura no.: {}", factura.get().getCertificacionSat());
				return factura.get();
			} else {
				log.warn("La Factura no existe en la base de datos");
				throw new NotFoundException("La Factura ".concat(idfactura.toString()).concat(" No existe en la base de datos"));
			}
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a ivel de Base de Datos: {}", e.getMessage());
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos: " + e.getMessage(), e.getCause());
		} catch (Exception e) {
			log.error("Ha ocurrido un error inesperado: {}", e.getMessage());
			throw new RuntimeException("Ha ocurrido un error inesperado: " + e.getMessage(), e.getCause());
		} finally {
			log.debug("{} Exit", __method);
		}
	}

	@Override
	public Factura findFacturaCorrelativo(Long correlativo) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		try {
			Optional<Factura> factura = repoFactura.findFacturaByNoFactura(correlativo);
			if(factura.isPresent()) {
				log.info("Devolviendo factura con correlativo interno: {}", correlativo.toString());
				return factura.get();
			} else {
				log.warn("No existe una factura con correlativo interno de: {}", correlativo.toString());
				throw new NotFoundException("No existe una factura con correlativo interno de: " + correlativo);
			}
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a ivel de Base de Datos: {}", e.getMessage());
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos: " + e.getMessage(), e.getCause());
		} finally {
			log.debug("{} Exit", __method);
		}
	}

	@Override
	public Factura save(Factura factura) {
		return repoFactura.save(factura);
	}

	@Transactional(rollbackFor = {Exception.class, DataAccessException.class})
	@Override
	public Factura facturaFel(Factura factura) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		try {
			Factura newFactura = null;
			Estado estado = estadoService.findByEstado("PAGADO");
			Estado estadoCorrFinalizado = estadoService.findByEstado("FINALIZADO");
			TipoFactura tipoFactura = tipoFacturaService.getTipoFactura(1);
			Correlativo correlativo = correlativoService.findByUsuario(factura.getUsuario().getIdUsuario());

			Emisor emisor = emisorService.getEmisor(1);
			Certificador certificador = certificadorService.getCertificador(1);
			DocumentoFel documentoFel = new DocumentoFel();

			if(factura.getIdFactura() == null) {
				log.info("********** Registrando nueva venta **********");

				log.info("-----------> Iniciando Proceso de Certificación FEL");
				documentoFel.setDatos_emisor(configurarDatosEmisor(emisor));
				documentoFel.setDatos_generales(configurarDatosGenerales());
				documentoFel.setDatos_receptor(configurarDatosReceptor(factura.getCliente()));

				configurarFrasesFacturaFel(1).forEach(documentoFel::setFrases);
				configurarItemsFacturaFel(factura).forEach(documentoFel::setItems);

				double totalImpuestos = documentoFel.getItems().stream().mapToDouble(items -> items.getImpuestos_detalle().get(0).getMontoImpuesto()).sum();
				configurarTotalImpuestos(totalImpuestos).forEach(documentoFel::setImpuestos_resumen);
				documentoFel.setTotales(sumTotales(documentoFel));

				factura.setTotal(new BigDecimal(documentoFel.getTotales().getGranTotal()));

				documentoFel.setAdenda(configurarAdendas(factura.getUsuario(), factura.getNoFactura().toString()));

				RespuestaServicioFirma respuestaFirmaEmisor = procesoFirma(documentoFel, certificador);
				RespuestaServicioFel respuestaServicioFel = new RespuestaServicioFel();


				log.info("--> Resultado: " + respuestaFirmaEmisor.isResultado());
				log.info("--> Descripcion: " + respuestaFirmaEmisor.getDescripcion());

				if(respuestaFirmaEmisor.isResultado()) {
					respuestaServicioFel = enviarAlCertificador(certificador, factura, respuestaFirmaEmisor, emisor, "CERTIFICACION");

					// INSERCIÓN DE FACTURA EN LA BASE DE DATOS DE LA EMPRESA
					if(respuestaServicioFel != null && respuestaServicioFel.getCantidad_errores() <= 0) {
						log.info("---------> Certificación FEL Exitosa");
						log.info("---------> Inserción de Factura en Base de Datos de Sistema");
						newFactura = crearFactura(respuestaServicioFel, factura, estado, totalImpuestos, tipoFactura);
					} else {
						log.error("No se ha podido llevar a cabo la certificación por parte del servicio FEL");
						throw new RuntimeException("No se ha podido llevar a cabo la certificación por parte del servicio FEL");
					}
				}
			}

			return newFactura;
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e.getMessage());
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException | KeyManagementException e) {
			log.error("Error relacionado con la firma del documento: {}", e.getMessage());
			throw new SigningDocumentFelException("Error relacionado con la firma del documento", e);
		} catch (Exception e) {
			log.error("Ha ocurrido un error inesperado: {}", e);
			throw new RuntimeException("Ha ocurrido un error inesperado: ", e);
		}
	}

	@Transactional(rollbackFor = {Exception.class, DataAccessException.class})
	@Override
	public Factura anularFacturaFel(Long idfactura, Integer idusuario) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);
		
		try {
			Factura cancelFactura = findFactura(idfactura);
			Factura voidFactura = null;
			Estado estado = estadoService.findByEstado("ANULADO");
			Usuario usuario = usuarioService.findById(idusuario);

			Emisor emisor = emisorService.getEmisor(1);
			Certificador certificador = certificadorService.getCertificador(1);
			MovimientoProducto movimientoProducto = null;

			if(cancelFactura != null) {
				log.info("********** ANULACIÓN DE FACTURA **********");

				log.info("-----------> Iniciando Proceso de Anulación FEL");
				AnulacionFel anulacionFel = initAnulacionFel(emisor.getNit(), cancelFactura);
				RespuestaServicioFirma respuestaFirmaEmisor = procesoFirma(anulacionFel, certificador);
				RespuestaServicioFel respuestaServicioFel = new RespuestaServicioFel();

				log.info("--> Resultado: " + respuestaFirmaEmisor.isResultado());
				log.info("--> Descripcion: " + respuestaFirmaEmisor.getDescripcion());

				if(respuestaFirmaEmisor.isResultado()) {
					respuestaServicioFel = enviarAlCertificador(certificador, cancelFactura, respuestaFirmaEmisor, emisor, "ANULACION");

					if(respuestaServicioFel != null) {
						log.info("Factura Anulada con exito por parte del certificador. Inicia proceso de anulación en la base de datos");
						cancelFactura.setEstado(estado);
						log.info("**************** Anulación de Factura {} ****************", cancelFactura.getCertificacionSat());
						voidFactura = repoFactura.save(cancelFactura);

						if(voidFactura.getEstado().getEstado().equals("ANULADO")) {

							// RECORRER ITEMS DE FACTURA ANULADA PARA DEVOLVER LAS EXISTENCIAS AL STOCK
							actualizarExistenciasDeItems(voidFactura.getItemsFactura(), usuario, TipoMovimientoEnum.ANULACION_FACTURA);

						} else {
							log.error("No se pudo llevar acabo la anulación de la factura en la base de datos");
						}
					} else {
						log.error("No se pudo llevar a cabo la certificación por parte del servicio FEL");
					}
				}
			}
			return voidFactura;
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de Base de datos: {}", e.getMessage());
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de Base de Datos", e.getCause());
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException | KeyManagementException e) {
			log.error("Error relacionado con la firma del documento: {}", e.getMessage());
			throw new SigningDocumentFelException("Error relacionado con la firma del documento", e);
		}  catch (Exception e) {
			log.error("Ha ocurrido un error inesperado: {}", e.getMessage());
			throw new RuntimeException("Ha ocurrido un error inesperado: " + e.getMessage(), e.getCause());
		}
	}

	@Override
	public TipoFactura findTipoFactura(Integer idTipoFactura) {
		return this.tipoFacturaRepository.findById(idTipoFactura).orElse(null);
	}

	@Override
	public Integer totalVentas() {
		try {
			return repoFactura.getCantidadVentas();
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de Base de Datos: {}", e.getMessage());
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos: " + e.getMessage(), e.getCause());
		}
	}

	@Override
	public List<Factura> facturasPorFecha(String iniDate, String endDate) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		try {

			Date date1;
			Date date2;
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

			date1 = format.parse(iniDate);
			date2 = format.parse(endDate);
			List<Factura> facturas = repoFactura.findByFechaBetween(date1, date2);

			if(!facturas.isEmpty()) {
				log.info("Devolviendo listado de Facturas en el rango de fechas: {} y {}", iniDate, endDate);
				return facturas;
			} else {
				log.warn("No existen facturas registradas en el rango de fechas comprendidas entre: {} y {}", iniDate, endDate);
				throw new NoContentException("No existen facturas registradas en el rango de fechas comprendidas entre: " + iniDate + " y " + endDate);
			}
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de Base de datos: {}", e.getMessage());
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de Base de Datos", e.getCause());
		} catch (ParseException e) {
			log.error("No se puede llevar a cabo la conversión de fechas");
			throw new com.aglayatech.licorstore.error.exceptions.ParseException("No se puede llevar a cabo la conversión de fechas", e.getCause());
		} finally {
			log.debug("{} Exit", __method);
		}
	}

	/**
	 * <p>Inicializa un objeto con los datos necesarios del <strong>Emisor FEL</strong>, es decir, el propietario
	 * del negocio, para el proceso de firma y certificación de emisión de factura
	 * en régimen FEL</p>.
	 * @param emisor Objeto de tipo emisor que contiene los datos almacenado en la base de datos del Emisor
	 * @return Objeto de tipo DatosEmisor inicializado para el proceso de firma y certificación
	 * */
	private DatosEmisor configurarDatosEmisor(Emisor emisor) {
		DatosEmisor datosEmisor = new DatosEmisor();
		datosEmisor.setAfiliacionIVA("GEN");
		datosEmisor.setCodigoEstablecimiento(1);
		datosEmisor.setCodigoPostal(emisor.getCodigoPostal());
		datosEmisor.setCorreoEmisor(emisor.getCorreoEmisor());
		datosEmisor.setDepartamento(emisor.getDepartamento());
		datosEmisor.setMunicipio(emisor.getMunicipio());
		datosEmisor.setDireccion(emisor.getDireccion());
		datosEmisor.setNITEmisor(emisor.getNit());
		datosEmisor.setNombreComercial(emisor.getNombreComercial());
		datosEmisor.setNombreEmisor(emisor.getNombreEmisor());
		datosEmisor.setPais("GT");
		return datosEmisor;
	}

	private DatosGenerales configurarDatosGenerales() {
		DatosGenerales datosGenerales = new DatosGenerales();
		datosGenerales.setCodigoMoneda("GTQ");
		datosGenerales.setFechaHoraEmision(Utils.setDateFormat(new Date()));
		datosGenerales.setTipo("FACT");
		return datosGenerales;
	}

	private DatosReceptor configurarDatosReceptor(Cliente cliente) {
		DatosReceptor datosReceptor = new DatosReceptor();
		datosReceptor.setCodigoPostal("01001");
		datosReceptor.setCorreoReceptor(""); // Quien recibe el pdf por correo, pueden ir varios separados por ;
		datosReceptor.setDepartamento("JALAPA");
		datosReceptor.setDireccion(cliente.getDireccion().trim());
		datosReceptor.setIDReceptor(Utils.formatearNitParaCertificacion(cliente.getNit()));
		datosReceptor.setMunicipio("JALAPA");
		datosReceptor.setNombreReceptor(cliente.getNombre().trim());
		datosReceptor.setPais("GT");
		return datosReceptor;
	}

	private Adendas configurarAdendas(Usuario usuario, String noFactura) {
		Adendas adendas = new Adendas();
		adendas.setAdenda("Cajero", usuario.getPrimerNombre().trim() + " " + usuario.getApellido().trim());
		adendas.setAdenda("Lote", "");
		adendas.setAdenda("OrdenCompra", "");
		adendas.setAdenda("Correlativo", noFactura);

		return adendas;
	}

	/**
	 *
	 * */
	public List<Frases> configurarFrasesFacturaFel(Integer maximum) {
		List<Frases> frases = new ArrayList<>();
		for(int i = 1; i <= maximum; i++) {
			Frases frase = new Frases();
			frase.setCodigoEscenario(i);
			frase.setTipoFrase(1);
			frases.add(frase);
		}

		return frases;
	}

	/**
	 *
	 **/
	private List<Items> configurarItemsFacturaFel(Factura factura) {
		List<Items> itemsFel = new ArrayList<>();
		for (int i = 0; i < factura.getItemsFactura().size(); i++) {
			Producto producto = factura.getItemsFactura().get(i).getProducto();
			Items items = new Items();
			items.setNumeroLinea(i + 1);
			items.setBienOServicio("B");
			items.setCantidad((double) factura.getItemsFactura().get(i).getCantidad());
			items.setDescripcion(factura.getItemsFactura().get(i).getProducto().getNombre());

			// Descuento siempre debe ir a cero para reflejarlo despues en el precio unitario
			items.setDescuento(0.0);

			if (factura.getItemsFactura().get(i).getDescuento().compareTo(new BigDecimal("0.0")) == 1) {
				items.setPrecioUnitario(factura.redondearPrecio(producto.getPrecioVenta().subtract((producto.getPrecioVenta().multiply((factura.getItemsFactura().get(i).getDescuento().divide(new BigDecimal(100)))))).doubleValue()));
			} else {
				items.setPrecioUnitario(producto.getPrecioVenta().doubleValue());
			}

			items.setPrecio(items.getPrecioUnitario() * items.getCantidad());
			items.setUnidadMedida("UND");
			items.setTotal(items.getPrecio() - items.getDescuento());

			// IGUALAR SUBTOTAL DE FEL EN LA FACTURA GUARDADA
			factura.getItemsFactura().get(i).setSubTotal(new BigDecimal(items.getPrecio()));

			for (int j = 1; j <= 1; j++) {
				ImpuestosDetalle impuestos_detalle = new ImpuestosDetalle();
				impuestos_detalle.setNombreCorto("IVA");
				impuestos_detalle.setCodigoUnidadGravable(j);
				impuestos_detalle.setMontoGravable((double) (items.getTotal() / 1.12));

				//impuestos_detalle.setCantidadUnidadesGravables(78.00);
				impuestos_detalle.setMontoImpuesto((double) (items.getTotal() / 1.12) * 0.12);
				items.setImpuestos_detalle(impuestos_detalle);
			}

			itemsFel.add(items);
		}

		return itemsFel;
	}

	/**
	 *
	 * */
	private List<TotalImpuestos> configurarTotalImpuestos(double totalImpuestos) {
		List<TotalImpuestos> listTotalImpuestos = new ArrayList<>();
		for (int k = 1; k <= 1; k++) {
			TotalImpuestos impuestosResumen = new TotalImpuestos();
			impuestosResumen.setNombreCorto("IVA");
			impuestosResumen.setTotalMontoImpuesto(totalImpuestos);
			listTotalImpuestos.add(impuestosResumen);
		}
		return listTotalImpuestos;
	}

	/**
	 *
	 * */
	private Totales sumTotales(DocumentoFel documentoFel) {
		double granTotal = documentoFel.getItems().stream().mapToDouble(Items::getTotal).sum();
		Totales totales = new Totales();
		totales.setGranTotal(granTotal);
		return totales;
	}

	/**
	 * Método encargado de llevar a cabo la formación del archivo XML que será enviado
	 * al servicio FEL del certificador para su firma.
	 * @param objetoFel Objeto de tipo DocumentoFel para la creación del XML
	 * */
	private RespuestaServicioFirma procesoFirma(Object objetoFel, Certificador certificador)
            	throws InvocationTargetException, IllegalAccessException, NoSuchElementException, UnsupportedOperationException,
						KeyManagementException, UnsupportedEncodingException, NoSuchAlgorithmException
	{
		log.info("********** Proceso de Firma por parte del certificador Iniciado ***********");
		// Variable para capturar la respuesta recibida del proceso de formacion del XML.
		Respuesta respuesta;

		// Objeto para enviar los datos para generacion del XML
		GenerarXml generar_xml = new GenerarXml();
		respuesta = generar_xml.ToXml(objetoFel);

		FirmaEmisor firmaEmisor = new FirmaEmisor();
		RespuestaServicioFirma respuestaServicioFirma = new RespuestaServicioFirma();

		if(respuesta.getResultado()) {
				log.info("Generacion de archivo XML se llevo a cabo con éxito!");

				log.info("--> FIRMA POR PARTE DEL EMISOR ");

				log.info("--> Resultado: " + respuestaServicioFirma.isResultado());
				log.info("--> Descripcion: " + respuestaServicioFirma.getDescripcion());

				log.info("--> Enviando Documento al Servicio de Firma del Emisor...");

				respuestaServicioFirma = firmaEmisor.Firmar(respuesta.getXml(), certificador.getPrefijo(), certificador.getTokenSigner());

		}
		log.info("********** Proceso de Firma por parte del certificador Finalizado ***********");
		return respuestaServicioFirma;
	}

	/**
	 * Método encargado del envio del documento firmado y formado al servicio de FEL por medio
	 * del certificador.
	 * @param certificador Objeto de tipo certificador que contiene los datos del mismo
	 * @param factura Objeto de tipo Factura que contiene los datos para llevar a cabo la facturación
	 * @param respuestaServicioFirma Objeto de tipo RespuestaServicioFirma que contiene los datos correspondientes a la firma del certificador
	 * @param emisor Objeto de tipo Emisor que contiene los datos del emisor de la factura
	 * @return RespuestaServicioFel
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * */
	private RespuestaServicioFel enviarAlCertificador(Certificador certificador, Factura factura, RespuestaServicioFirma respuestaServicioFirma,
													  Emisor emisor, String tipoTransaccion)
			throws NoSuchAlgorithmException, KeyManagementException
	{
		log.info("--> Enviando Documento al Servicio FEL...");
		ServicioFel servicioFel = new ServicioFel();
		String correoCopia = "";
		String identificador = "";

		if(tipoTransaccion.equals("CERTIFICACION")) {
			identificador = factura.getNoFactura().toString() + factura.getSerie() + factura.getUsuario().getUsuario();
			correoCopia = certificador.getCorreoCopia();
		}
		else if(tipoTransaccion.equals("ANULACION")) {
			identificador = "ANULACION_" + factura.getNoFactura();
			correoCopia = "N/A";
		}

		RespuestaServicioFel respuestaServicioFel = servicioFel.Certificar(obtenerConexionServicioFel(certificador, factura, identificador), respuestaServicioFirma.getArchivo(),
				emisor.getNit(), correoCopia, tipoTransaccion.toUpperCase());

		if(respuestaServicioFel.getResultado()) {
			log.info("--> Resultado: " + respuestaServicioFel.getResultado());
			log.info("--> Origen: " + respuestaServicioFel.getOrigen());
			log.info("--> Descripcion: " + respuestaServicioFel.getDescripcion());
			log.info("--> Cantidad Errores: " + respuestaServicioFel.getCantidad_errores());
			log.info("--> INFO: " + respuestaServicioFel.getInfo());

			log.info("UUID: " + respuestaServicioFel.getUuid());
			log.info("Serie: " + respuestaServicioFel.getSerie());
			log.info("Numero: " + respuestaServicioFel.getNumero());
			log.info("Fecha_certificacion: " + respuestaServicioFel.getFecha());

			return respuestaServicioFel;
		} else {
			log.warn("******************* No se pudo realizar la certificacion FEL ****************************");

			// MOSTRAR ERRORES EN PANTALLA
			log.warn("--> Resultado: {}",respuestaServicioFel.getResultado());
			log.warn("--> Origen: {}", respuestaServicioFel.getOrigen());
			log.warn("--> Descripcion: {}", respuestaServicioFel.getDescripcion());
			log.warn("--> Cantidad Errores: " + respuestaServicioFel.getCantidad_errores());
			log.warn("--> INFO: " + respuestaServicioFel.getInfo());

			for(int i = 0; i < respuestaServicioFel.getCantidad_errores(); i++)
				log.error(respuestaServicioFel.getDescripcion_errores().get(i).getMensaje_error());

			return null;
		}
	}

	/**
	 * */
	private ConexionServicioFel obtenerConexionServicioFel(Certificador certificador, Factura factura, String identificador) {
		log.info("--> ENVIO AL API DE INFILE");
		ConexionServicioFel conexionServicioFel = new ConexionServicioFel();
		conexionServicioFel.setUrl("");
		conexionServicioFel.setMetodo("POST");
		conexionServicioFel.setContent_type("application/json");
		conexionServicioFel.setUsuario(certificador.getPrefijo());
		conexionServicioFel.setLlave(certificador.getLlaveWs());

		// DEBE VARIAR SIENDO IDENTIFICADOR UNICO
		conexionServicioFel.setIdentificador(identificador);
		return conexionServicioFel;
	}

	/**
	 *
	 * */
	private Factura crearFactura(RespuestaServicioFel respuesta, Factura factura, Estado estado, double totalImpuestos, TipoFactura tipoFactura)
		throws DataAccessException
	{

		Correlativo correlativo = correlativoService.findByUsuario(factura.getUsuario().getIdUsuario());
		Correlativo correlativoActualizado = null;
		Factura newFactura = null;

		factura.setEstado(estado);
		factura.setCorrelativoSat(respuesta.getNumero());
		factura.setCertificacionSat(respuesta.getUuid());
		factura.setSerieSat(respuesta.getSerie());
		factura.setMensajeSat(respuesta.getInfo());
		factura.setFechaCertificacionSat(respuesta.getFecha());
		factura.setIva(new BigDecimal(totalImpuestos));
		factura.setTipoFactura(tipoFactura);
		newFactura = repoFactura.save(factura);

		if(newFactura.getIdFactura() != null) {
			log.info("******************** Factura Guardada en la Base de Datos ************************");
			log.info("-----------> Actualizando correlativo");
			cambiarCorrelativo(correlativo, estadoService.findByEstado("FINALIZADO"));

			actualizarExistenciasDeItems(factura.getItemsFactura(), factura.getUsuario(), TipoMovimientoEnum.VENTA);

		}

		return newFactura;
	}

	/**
	 *
	 * */
	private void cambiarCorrelativo(Correlativo correlativo, Estado estado) {
		correlativo.setCorrelativoActual(correlativo.getCorrelativoActual() + 1);
		if(correlativo.getCorrelativoActual() >= correlativo.getCorrelativoFinal()) {
			correlativo.setEstado(estado);
		}
		correlativoService.save(correlativo);
	}

	/**
	 *
	 * */
	private void actualizarExistenciasDeItems(List<DetalleFactura> items, Usuario usuario, TipoMovimientoEnum tipoMovimiento) {
		log.info("-----------> Actualizando las existencias de los items");
        items.stream().map(item -> buildMovimiento(item.getProducto(), usuario, tipoMovimiento, item.getCantidad())).forEach(movimientoProductoService::save);
	}

	/**
	 *
	 * Método encargado de llevar a cabo la inicialización de un objeto de tipo MovimientoProducto para llevar a cabo
	 * los registros respectivos dentro de la tabla movimientos_producto de la base de datos para cada una de las líneas
	 * de la venta llevada a cabo.
	 * @param producto Objeto principal de cada item del detalle de la venta
	 * @param cantidad Cantidad de productos vendidos de cada item
	 * @param tipoMovimiento El tipo de movimiento que se esta llevando a cabo para ser evaluado
	 * @return MovimientoProducto Objeto resultante del movimiento guardado en la Base de Datos
	 *
	 * */
	private MovimientoProducto buildMovimiento(Producto producto, Usuario usuario, TipoMovimientoEnum tipoMovimiento, int cantidad) {
		return MovimientoProducto.builder()
				.tipoMovimiento(tipoMovimiento)
				.usuario(usuario)
				.producto(producto)
				.stockInicial(producto.getStock())
				.cantidad(cantidad)
				.build();
	}

	/**
	 * Método que inicializa y devuelve un objeto que guarda los elementos clave para llevar a cabo la anulación
	 * de una factura de régimen FEL ya emitida.
	 * @param nitEmisor Cadena de texto que representa el NIT del dueño del negocio
	 * @param cancelFactura Objeto de tipo Factura que contiene todos los datos guardados respectivo a la certificación por parte de la SAT
	 * @return Objeto de tipo AnulacionFel
	 * */
	private AnulacionFel initAnulacionFel(String nitEmisor, Factura cancelFactura) {
		String fechaCertificacion = Utils.fechaCertificacionTransformada(cancelFactura.getFechaCertificacionSat());
		AnulacionFel anulacionFel = new AnulacionFel();
		anulacionFel.setFechaEmisionDocumentoAnular(Utils.getDateFormat(fechaCertificacion).format(new Date()));
		anulacionFel.setFechaHoraAnulacion(Utils.setDateFormat(new Date()));
		anulacionFel.setIDReceptor(Utils.formatearNitParaCertificacion(cancelFactura.getCliente().getNit()));
		anulacionFel.setNITEmisor(nitEmisor);
		anulacionFel.setMotivoAnulacion("Anulacion");
		anulacionFel.setNumeroDocumentoAAnular(cancelFactura.getCertificacionSat());

		return anulacionFel;
	}

	/****************** PDF REPORT SERVICES *******************/

	// REPORTE DE VENTAS DIARIAS
	@Override
	public byte[] resportDailySales(Integer usuario, String fecha) {

		try(Connection con = localDataSource.getConnection()) { // Obtiene la conexión actual a la base de datos
			Date fechaBusqueda;
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			fechaBusqueda = format.parse(fecha);
			Map<String, Object> params = new HashMap<>();
			InputStream file = getClass().getResourceAsStream("/reports/poliza.jrxml");

			if(file == null) {
				throw new NotFoundException("Archivo no encontrado");
			}
			params.put("usuario", usuario);
			params.put("fecha", fecha);

			JasperReport jasperReport = JasperCompileManager.compileReport(file);
			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, con);

			return JasperExportManager.exportReportToPdf(jasperPrint);
		} catch (JRException e) {
			log.error("Ha ocurrido un error durante la generación de la proforma: {}", e.getMessage());
			throw new ReportGenerationException(e.getMessage(), e.getCause());
		} catch (SQLException e) {
			log.error("Ha ocurrido un error al intentar ejecutar una instrucción SQL: {}", e.getMessage());
			throw new com.aglayatech.licorstore.error.exceptions.SQLException(e.getMessage(), e.getCause());
		} catch (Exception e) {
			log.error("Ha ocurrido un error inesperado: {}", e.getMessage());
			throw new RuntimeException("Ha ocurrido un error inesperado: {}", e);
		}
	}

	// GENERADOR DE REPORTE DE FACTURA
	@Override
	public byte[] showBill(Long idfactura)
			throws JRException, FileNotFoundException, SQLException {
		// TODO: Implementar en caso de necesitarse
		return null;
	}

	@Override
	public byte[] showBill2(Long idfactura)
			throws JRException, FileNotFoundException, SQLException {

		// TODO: Implementar en caso de necesitarse formato sin FEL
		return null;
	}
}
