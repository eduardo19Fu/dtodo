package xyz.pangosoft.dtodo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import xyz.pangosoft.dtodo.dto.CompraDetalleDocumentoDto;
import xyz.pangosoft.dtodo.dto.CompraDto;
import xyz.pangosoft.dtodo.model.Compra;

public interface ICompraService {

	public Page<CompraDto> findListado(String filtro, Integer idSucursal, Pageable pageable);

	public Page<CompraDetalleDocumentoDto> findDetalleDto(Long idCompra, Pageable pageable);

	public Compra findById(Long idCompra);

	public Compra crear(Compra compra);

	public Compra anular(Long idCompra, Integer idUsuario);

}
