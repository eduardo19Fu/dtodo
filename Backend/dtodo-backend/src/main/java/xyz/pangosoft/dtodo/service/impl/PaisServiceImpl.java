package xyz.pangosoft.dtodo.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.model.Pais;
import xyz.pangosoft.dtodo.repository.IPaisRepository;
import xyz.pangosoft.dtodo.service.IPaisService;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaisServiceImpl implements IPaisService {

	private final IPaisRepository paisRepository;

	@Transactional(readOnly = true)
	@Override
	public List<Pais> findAll() {
		try {
			return paisRepository.findAll(Sort.by(Direction.ASC, "nombre"));
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public Pais findById(Integer idPais) {
		Optional<Pais> pais = paisRepository.findById(idPais);
		if (pais.isPresent()) {
			return pais.get();
		}
		throw new NotFoundException("El país: " + idPais + ", no se encuentra registrado en la base de datos");
	}

	@Transactional
	@Override
	public Pais save(Pais pais) {
		try {
			if (pais.getIdPais() != null) {
				Pais paisExistente = findById(pais.getIdPais());
				pais.setFechaRegistro(paisExistente.getFechaRegistro());
				pais.setUsuario(paisExistente.getUsuario());
			}
			return paisRepository.save(pais);
		} catch (NotFoundException e) {
			throw e;
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		}
	}

}
