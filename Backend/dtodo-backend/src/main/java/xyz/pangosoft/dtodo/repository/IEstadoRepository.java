package xyz.pangosoft.dtodo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import xyz.pangosoft.dtodo.model.Estado;

public interface IEstadoRepository extends JpaRepository<Estado, Integer> {
	
	Optional<Estado> findByEstado(String estado);

}
