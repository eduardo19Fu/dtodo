package xyz.pangosoft.dtodo.service.impl;

import java.io.FileNotFoundException;
import java.io.InputStream;

import java.math.BigDecimal;

import java.sql.Connection;
import java.sql.SQLException;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import xyz.pangosoft.dtodo.error.exceptions.NoContentException;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.dto.FacturaDto;
import xyz.pangosoft.dtodo.dto.DetalleDocumentoDto;
import xyz.pangosoft.dtodo.dto.DocumentoOrigenNotaDto;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.error.exceptions.ReportGenerationException;
import xyz.pangosoft.dtodo.model.Certificador;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.Correlativo;
import xyz.pangosoft.dtodo.model.DetalleFactura;
import xyz.pangosoft.dtodo.model.Emisor;
import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.MovimientoProducto;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.TipoFactura;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.TipoMovimientoEnum;
import xyz.pangosoft.dtodo.repository.ITipoFacturaRepository;
import xyz.pangosoft.dtodo.service.ICertificadorService;
import xyz.pangosoft.dtodo.service.ICorrelativoService;
import xyz.pangosoft.dtodo.service.IEmisorService;
import xyz.pangosoft.dtodo.service.IEstadoService;
import xyz.pangosoft.dtodo.service.IMovimientoProductoService;
import xyz.pangosoft.dtodo.service.ITipoFacturaService;
import xyz.pangosoft.dtodo.service.IUsuarioService;
import xyz.pangosoft.dtodo.util.Utils;

import xyz.pangosoft.dtodo.fel.IFelService;
import xyz.pangosoft.dtodo.fel.dto.RespuestaCertificacion;
import xyz.pangosoft.dtodo.fel.dto.RespuestaFirma;
import xyz.pangosoft.dtodo.fel.model.Adendas;
import xyz.pangosoft.dtodo.fel.model.AnulacionFel;
import xyz.pangosoft.dtodo.fel.model.DatosEmisor;
import xyz.pangosoft.dtodo.fel.model.DatosGenerales;
import xyz.pangosoft.dtodo.fel.model.DatosReceptor;
import xyz.pangosoft.dtodo.fel.model.DocumentoFel;
import xyz.pangosoft.dtodo.fel.model.Frases;
import xyz.pangosoft.dtodo.fel.model.ImpuestosDetalle;
import xyz.pangosoft.dtodo.fel.model.Items;
import xyz.pangosoft.dtodo.fel.model.TotalImpuestos;
import xyz.pangosoft.dtodo.fel.model.Totales;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import xyz.pangosoft.dtodo.model.Factura;
import xyz.pangosoft.dtodo.repository.IFacturaRepository;
import xyz.pangosoft.dtodo.service.IFacturaService;

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
	private final IFelService felService;
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
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos: " + e.getMessage(), e.getCause());
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

	@Transactional(readOnly = true)
	@Override
	public Page<FacturaDto> findAllListadoDto(String fechaIni, String fechaFin, Pageable pageable) {
		Date[] rango = parseDateRange(fechaIni, fechaFin);
		try {
			return repoFactura.findAllListadoDto(rango[0], rango[1], pageable);
		} catch (DataAccessException e) {
			log.error("Error al consultar el listado paginado de facturas: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(
					"Ha ocurrido un error al consultar las facturas", e);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public Page<FacturaDto> searchListadoDto(String fechaIni, String fechaFin, String filtro, Pageable pageable) {
		Date[] rango = parseDateRange(fechaIni, fechaFin);
		String filtroAdaptado = filtro == null ? "" : filtro.trim().replaceAll("\\s+", " ");
		try {
			return repoFactura.searchListadoDto(rango[0], rango[1], filtroAdaptado, pageable);
		} catch (DataAccessException e) {
			log.error("Error al filtrar el listado de facturas: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(
					"Ha ocurrido un error al filtrar las facturas", e);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public Page<FacturaDto> findUltimasListadoDto(String filtro, Pageable pageable) {
		try {
			List<FacturaDto> facturas = repoFactura.findUltimasListadoDto(PageRequest.of(0, 500));
			String filtroNormalizado = filtro == null ? "" : filtro.trim().toLowerCase();
			if (!filtroNormalizado.isEmpty()) {
				facturas = facturas.stream()
						.filter(factura -> coincideFiltro(factura, filtroNormalizado))
						.collect(java.util.stream.Collectors.toList());
			}
			ordenarFacturas(facturas, pageable.getSort());
			int inicio = Math.min((int) pageable.getOffset(), facturas.size());
			int fin = Math.min(inicio + pageable.getPageSize(), facturas.size());
			return new PageImpl<>(facturas.subList(inicio, fin), pageable, facturas.size());
		} catch (DataAccessException e) {
			log.error("Error al consultar las últimas 500 facturas: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(
					"Ha ocurrido un error al consultar las últimas facturas", e);
		}
	}

	private boolean coincideFiltro(FacturaDto factura, String filtro) {
		return contiene(factura.getCliente(), filtro)
				|| contiene(factura.getNitCliente(), filtro)
				|| contiene(factura.getVendedor(), filtro)
				|| contiene(factura.getUsuario(), filtro)
				|| contiene(factura.getSerie(), filtro)
				|| contiene(factura.getNoFactura(), filtro);
	}

	private boolean contiene(Object valor, String filtro) {
		return valor != null && valor.toString().toLowerCase().contains(filtro);
	}

	private void ordenarFacturas(List<FacturaDto> facturas, Sort sort) {
		Comparator<FacturaDto> comparador = null;
		for (Sort.Order order : sort) {
			Comparator<FacturaDto> comparadorCampo = (primera, segunda) -> compararValores(
					obtenerValorOrden(primera, order.getProperty()),
					obtenerValorOrden(segunda, order.getProperty()));
			if (order.isDescending()) {
				comparadorCampo = comparadorCampo.reversed();
			}
			comparador = comparador == null ? comparadorCampo : comparador.thenComparing(comparadorCampo);
		}
		if (comparador != null) {
			facturas.sort(comparador);
		}
	}

	private Comparable<?> obtenerValorOrden(FacturaDto factura, String propiedad) {
		switch (propiedad) {
			case "noFactura": return factura.getNoFactura();
			case "serie": return normalizar(factura.getSerie());
			case "cliente.nombre": return normalizar(factura.getCliente());
			case "usuario.primerNombre":
			case "usuario.apellido": return normalizar(factura.getVendedor());
			case "usuario.usuario": return normalizar(factura.getUsuario());
			case "total": return factura.getTotal();
			case "estado.estado": return normalizar(factura.getEstado());
			default: return factura.getFecha();
		}
	}

	private String normalizar(String valor) {
		return valor == null ? null : valor.toLowerCase();
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private int compararValores(Comparable primero, Comparable segundo) {
		if (primero == null && segundo == null) {
			return 0;
		}
		if (primero == null) {
			return 1;
		}
		if (segundo == null) {
			return -1;
		}
		return primero.compareTo(segundo);
	}

	@Transactional(readOnly = true)
	@Override
	public Page<DetalleDocumentoDto> findDetalleDto(Long idFactura, Pageable pageable) {
		if (!repoFactura.existsById(idFactura)) {
			throw new NotFoundException("La factura con ID " + idFactura + " no existe");
		}
		try {
			return repoFactura.findDetalleDto(idFactura, pageable).map(this::mapDetalleDocumentoDto);
		} catch (DataAccessException e) {
			log.error("Error al consultar el detalle de la factura {}: {}", idFactura, e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(
					"Ha ocurrido un error al consultar el detalle de la factura", e);
		}
	}

	private DetalleDocumentoDto mapDetalleDocumentoDto(Object[] fila) {
		return new DetalleDocumentoDto(
				((Number) fila[0]).longValue(),
				((Number) fila[1]).intValue(),
				(String) fila[2],
				(String) fila[3],
				((Number) fila[4]).intValue(),
				toBigDecimal(fila[5]),
				(Number) fila[6],
				toBigDecimal(fila[7]));
	}

	private BigDecimal toBigDecimal(Object valor) {
		return valor instanceof BigDecimal ? (BigDecimal) valor : new BigDecimal(valor.toString());
	}

	@Transactional(readOnly = true)
	@Override
	public DocumentoOrigenNotaDto findOrigenNotaDto(String correlativoSat, String serieSat) {
		return repoFactura.findOrigenNotaDto(correlativoSat, serieSat)
				.orElseThrow(() -> new NotFoundException(
						"No existe una factura con correlativo SAT: " + correlativoSat + " y serie SAT: " + serieSat));
	}

	private Date[] parseDateRange(String fechaIni, String fechaFin) {
		if (fechaIni == null || fechaIni.trim().isEmpty() || fechaFin == null || fechaFin.trim().isEmpty()) {
			throw new BadRequestException("Debe ingresar un rango de fechas válido", null);
		}
		try {
			Date inicio = Utils.stringToDate(fechaIni);
			Date fin = Utils.stringToDate(fechaFin);
			if (fin.before(inicio)) {
				throw new BadRequestException("La fecha final no puede ser anterior a la fecha inicial", null);
			}
			Calendar calendario = Calendar.getInstance();
			calendario.setTime(fin);
			calendario.add(Calendar.DAY_OF_MONTH, 1);
			return new Date[] { inicio, calendario.getTime() };
		} catch (ParseException e) {
			throw new BadRequestException("El formato del rango de fechas no es válido", e);
		}
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
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos: " + e.getMessage(), e.getCause());
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
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos: " + e.getMessage(), e.getCause());
		} finally {
			log.debug("{} Exit", __method);
		}
	}

	@Override
	public Factura findFacturaByCorrelativoSatAndSerieSat(String correlativoSat, String serieSat) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		try {
			Optional<Factura> factura = repoFactura.findFacturaByCorrelativoSatAndSerieSat(correlativoSat, serieSat);
			if(factura.isPresent()) {
				log.info("Devolviendo factura con correlativo SAT: {} y serie SAT: {}", correlativoSat, serieSat);
				return factura.get();
			} else {
				log.warn("No existe una factura con correlativo SAT: {} y serie SAT: {}", correlativoSat, serieSat);
				throw new NotFoundException("No existe una factura con correlativo SAT: " + correlativoSat + " y serie SAT: " + serieSat);
			}
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de Base de Datos: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos: " + e.getMessage(), e.getCause());
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

				RespuestaFirma respuestaFirmaEmisor = procesoFirma(documentoFel, certificador);
				RespuestaCertificacion respuestaServicioFel = new RespuestaCertificacion();


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
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
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
				RespuestaFirma respuestaFirmaEmisor = procesoFirma(anulacionFel, certificador);
				RespuestaCertificacion respuestaServicioFel = new RespuestaCertificacion();

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
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de Base de Datos", e.getCause());
		} catch (Exception e) {
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
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos: " + e.getMessage(), e.getCause());
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
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de Base de Datos", e.getCause());
		} catch (ParseException e) {
			log.error("No se puede llevar a cabo la conversión de fechas");
			throw new xyz.pangosoft.dtodo.error.exceptions.ParseException("No se puede llevar a cabo la conversión de fechas", e.getCause());
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
		datosReceptor.setDepartamento(".");
		datosReceptor.setDireccion(cliente.getDireccion().trim());
		datosReceptor.setIDReceptor(Utils.formatearNitParaCertificacion(cliente.getNit()));
		datosReceptor.setMunicipio(".");
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
	 * @param objetoFel Objeto de tipo DocumentoFel o AnulacionFel para la creación del XML
	 * */
	private RespuestaFirma procesoFirma(Object objetoFel, Certificador certificador) {
		return felService.firmarDocumento(objetoFel, certificador);
	}

	/**
	 * Método encargado del envio del documento firmado y formado al servicio de FEL por medio
	 * del certificador.
	 * @param certificador Objeto de tipo certificador que contiene los datos del mismo
	 * @param factura Objeto de tipo Factura que contiene los datos para llevar a cabo la facturación
	 * @param respuestaFirma Objeto que contiene los datos correspondientes a la firma del emisor
	 * @param emisor Objeto de tipo Emisor que contiene los datos del emisor de la factura
	 * @return RespuestaCertificacion
	 * */
	private RespuestaCertificacion enviarAlCertificador(Certificador certificador, Factura factura, RespuestaFirma respuestaFirma,
													  Emisor emisor, String tipoTransaccion)
	{
		log.info("--> Enviando Documento al Servicio FEL...");
		String identificador = "";

		if(tipoTransaccion.equals("CERTIFICACION")) {
			identificador = factura.getNoFactura().toString() + factura.getSerie() + factura.getUsuario().getUsuario();
		}
		else if(tipoTransaccion.equals("ANULACION")) {
			identificador = "ANULACION_" + factura.getNoFactura();
		}

		RespuestaCertificacion respuestaServicioFel = felService.certificar(certificador, respuestaFirma.getArchivo(),
				identificador, tipoTransaccion.toUpperCase());

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
	 *
	 * */
	private Factura crearFactura(RespuestaCertificacion respuesta, Factura factura, Estado estado, double totalImpuestos, TipoFactura tipoFactura)
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
		correlativoService.update(correlativo);
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
			throw new xyz.pangosoft.dtodo.error.exceptions.SQLException(e.getMessage(), e.getCause());
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
