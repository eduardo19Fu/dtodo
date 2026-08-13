package xyz.pangosoft.dtodo.controller;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import xyz.pangosoft.dtodo.dto.MovimientoProductoDto;
import xyz.pangosoft.dtodo.model.MovimientoProducto;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.service.IMovimientoProductoService;
import xyz.pangosoft.dtodo.service.IProductoService;

import net.sf.jasperreports.engine.JRException;

@CrossOrigin({ "http://localhost:4200", "https://dtodojalapa.xyz", "http://dtodojalapa.xyz" })
@RestController
@RequestMapping(value = "/api")
@RequiredArgsConstructor
@Slf4j
public class MovimientoProductoApiController {

	private final IMovimientoProductoService serviceMove;

	private final IProductoService serviceProducto;

	@GetMapping(value = "/movimientos")
	public ResponseEntity<List<MovimientoProducto>> index() {
		log.info("Listando movimientos de productos realizados.");
		return ResponseEntity.ok(serviceMove.findAll());
	}

	@GetMapping(value = "/movimientos-dto/page/{page}")
	public ResponseEntity<Page<MovimientoProductoDto>> getPageDto(@PathVariable("page") Integer page,
																  @RequestParam(value = "size", defaultValue = "5") Integer size)
	{
		return ResponseEntity.ok(serviceMove.findAllDtoMejorado(PageRequest.of(page, size)));
	}

	@GetMapping(value = "/movimientos-dto/search/{page}")
	public ResponseEntity<Page<MovimientoProductoDto>> searchMovimientosDto(
																  @PathVariable("page") Integer page,
																  @RequestParam(required = false) String filtro,
																  @RequestParam(value = "size", defaultValue = "5") Integer size)
	{
		return ResponseEntity.ok(serviceMove.searchMovimientoDtoMejorado(filtro, PageRequest.of(page, size)));
	}

	@Secured({"ROLE_ADMIN", "ROLE_INVENTARIO"})
	@GetMapping(value = "/movimientos/{idproducto}/{page}")
	public ResponseEntity<Page<MovimientoProducto>> getByProducto(@PathVariable("idproducto") Integer idProducto, @PathVariable("page") Integer page){
		log.info("Devolviendo movimientos de página: {}", page);
		Producto producto = serviceProducto.findById(idProducto);
		return ResponseEntity.ok(serviceMove.findProductoMoves(producto, PageRequest.of(page, 4)));
	}

	@Secured({"ROLE_ADMIN", "ROLE_INVENTARIO"})
	@PostMapping(value = "/movimientos")
	public ResponseEntity<?> create(@RequestBody MovimientoProducto movimientoProducto, BindingResult result) {
		log.info("Creando nuevo movimiento para el producto: {}", movimientoProducto.getProducto().getCodProducto());
		MovimientoProducto newMovimiento = null;
		newMovimiento = serviceMove.save(movimientoProducto);
		return ResponseEntity.status(HttpStatus.CREATED).body(newMovimiento);
	}

	/************ REPORTS CONTROLLERS 
	 * @throws ParseException 
	 * @throws SQLException 
	 * @throws JRException 
	 * @throws FileNotFoundException *****************/
	
	@PostMapping(value = "/movimientos/inventario")
	public void inventario(@RequestParam("fechaIni") String paramFechaIni, @RequestParam("fechaFin") String paramFechaFin, 
			HttpServletResponse httpServletResponse) 
			throws ParseException, FileNotFoundException, JRException, SQLException{
		
		Date fechaIni, fechaFin;
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		fechaIni = format.parse(paramFechaIni);
		fechaFin = format.parse(paramFechaFin);
		
		byte[] bytesInventoryReport = serviceMove.inventory(fechaIni, fechaFin);
		ByteArrayOutputStream out = new ByteArrayOutputStream(bytesInventoryReport.length);
		out.write(bytesInventoryReport, 0, bytesInventoryReport.length);
		
		httpServletResponse.setContentType("application/pdf");
		httpServletResponse.addHeader("Content-Disposition", "inline; filename=daily-sales.pdf");
		
		OutputStream os;
	    try {
	        os = httpServletResponse.getOutputStream();
	        out.writeTo(os);
	        os.flush();
	        os.close();
	    } catch (IOException e) {
	        log.error("Error al escribir el reporte de inventario en la respuesta: {}", e.getMessage());
	    }
	}
}
