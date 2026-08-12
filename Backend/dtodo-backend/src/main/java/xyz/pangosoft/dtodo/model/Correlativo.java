package xyz.pangosoft.dtodo.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "correlativos")
public class Correlativo implements Serializable {

	private static final long serialVersionUID = 6089200377612236893L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idCorrelativo;
	private Long correlativoInicial;
	private Long correlativoFinal;
	private Long correlativoActual;
	private String serie;
	private LocalDateTime fechaCreacion;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_estado")
	@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
	private Estado estado;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario")
	@JsonIgnoreProperties({"password", "roles", "hibernateLazyInitializer", "handler" })
	private Usuario usuario;

	@PrePersist
	public void preConfig() {
		this.fechaCreacion = LocalDateTime.now();
	}


}
