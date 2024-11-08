package com.aglayatech.licorstore.service.impl;

import com.aglayatech.licorstore.error.exceptions.NoContentException;
import com.aglayatech.licorstore.error.exceptions.NotFoundException;
import com.aglayatech.licorstore.model.Estado;
import com.aglayatech.licorstore.model.MovimientoProducto;
import com.aglayatech.licorstore.model.NotaCredito;
import com.aglayatech.licorstore.model.NotaCreditoDetalle;
import com.aglayatech.licorstore.model.Producto;
import com.aglayatech.licorstore.model.Usuario;
import com.aglayatech.licorstore.model.enums.EstadoNotaCreditoEnum;
import com.aglayatech.licorstore.model.enums.TipoMovimientoEnum;
import com.aglayatech.licorstore.repository.INotaCreditoRepository;
import com.aglayatech.licorstore.service.IMovimientoProductoService;
import com.aglayatech.licorstore.service.INotaCreditoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotaCreditoServiceImpl implements INotaCreditoService {

    private final INotaCreditoRepository notaCreditoRepository;
    private final IMovimientoProductoService movimientoProductoService;

    @Transactional(readOnly = true)
    @Override
    public List<NotaCredito> findNotas() {
        String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug("Enter {}", __method);

        try {
            List<NotaCredito> notas = notaCreditoRepository.findAll();
            if(!notas.isEmpty()) {
                log.info("Retornando listado de notas de credito registradas");
                return notas;
            } else {
                log.warn("No existen notas de credito registradas");
                throw new NoContentException("No existen notas de credito registradas");
            }
        } catch (DataAccessException e) {
            log.error("Ha ocurrido un error a nivel de base de datos: {}", e.getMessage());
            throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos " + e.getMessage(), e.getCause());
        } finally {
            log.debug("{} Exit", __method);
        }

    }

    @Override
    public List<NotaCredito> findNotasActivas(Estado estado) {
        // TODO: Dejarlo pendiente si es necesario utilizarlo
        return null;
    }

    @Override
    public NotaCredito findNota(Integer idNota) {
        String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug("Enter {}", __method);

        try {
            Optional<NotaCredito> notaCredito = notaCreditoRepository.findById(idNota);
            if(notaCredito.isPresent()) {
                log.info("Retornando Nota de Credito: {}", notaCredito.get().getIdNotaCredito());
                return notaCredito.get();
            } else {
                log.warn("No existen nota de credito registradas con el ID: {}", idNota);
                throw new NotFoundException("No existen nota de credito registradas con el ID: " + idNota);
            }
        } catch (DataAccessException e) {
            log.error("Ha ocurrido un error a nivel de base de datos: {}", e.getMessage());
            throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos: " + e.getMessage(), e.getCause());
        } finally {
            log.debug("{} Exit", __method);
        }
    }

    @Override
    public NotaCredito save(NotaCredito notaCredito, EstadoNotaCreditoEnum estado) {
        String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug("Enter {}", __method);

        try {
            NotaCredito notaCreditoSaved = null;

            if(estado.equals(EstadoNotaCreditoEnum.ENTREGA_PENDIENTE)) {
                log.info("------> Registrando nota de Credito");
                notaCredito.setEstado(estado);
                notaCreditoSaved = notaCreditoRepository.save(notaCredito);
            } else if(estado.equals(EstadoNotaCreditoEnum.ANULADO)) {
                log.info("------> Anulando nota de Credito");
                notaCredito.setEstado(estado);
                notaCreditoSaved = notaCreditoRepository.save(notaCredito);
            } else if(estado.equals(EstadoNotaCreditoEnum.ENTREGADO)) {

                log.info("------> Registrando entrega de productos");
                for(NotaCreditoDetalle item : notaCredito.getItems()) {
                    log.info("Registrando entrega de producto: {}", item.getProducto().getCodProducto());
                    movimientoProductoService.save(buildMovimiento(item.getProducto(), item.getCantidad(), TipoMovimientoEnum.ENTREGA_PRODUCTO_NOTA, notaCredito.getUsuario()));
                }

                log.info("------> Registrando actualización de estado de la nota de credito");
                notaCredito.setEstado(estado);
                notaCreditoSaved = notaCreditoRepository.save(notaCredito);
            }

            return notaCreditoSaved;
        } catch (DataAccessException e) {
            log.error("Ha ocurrido un error a nivel de base de datos: {}", e.getMessage());
            throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos: " + e.getMessage(), e.getCause());
        } finally {
            log.debug("{} Exit", __method);
        }
    }

    @Override
    public void delete(Integer idNota) {
        notaCreditoRepository.deleteById(idNota);
    }

    private MovimientoProducto buildMovimiento(Producto producto, int cantidad, TipoMovimientoEnum tipoMovimiento, Usuario usuario) {
        return MovimientoProducto.builder()
                .stockInicial(producto.getStock())
                .cantidad(cantidad)
                .producto(producto)
                .usuario(usuario)
                .tipoMovimiento(tipoMovimiento)
                .build();
    }
}
