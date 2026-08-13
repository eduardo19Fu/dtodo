package xyz.pangosoft.dtodo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import xyz.pangosoft.dtodo.model.Correlativo;
import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.Usuario;

public interface ICorrelativoRepository extends JpaRepository<Correlativo, Long> {
	
	// Buscar el correlativo del usuario logueado en el sistema
	public Optional<Correlativo> findByUsuarioAndEstado(Usuario usuario, Estado estado);

}
