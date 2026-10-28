package com.cibertec.skyflow.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@Entity
@Table(name = "miembros_proyecto",
	uniqueConstraints = @UniqueConstraint(columnNames = {"proyecto_id", "usuario_id"}))
public class MiembroProyecto implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "proyecto_id", nullable = false)
	private Proyecto proyecto;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RolProyecto rol;

	@Column(name = "fecha_union", updatable = false)
	private LocalDateTime fechaUnion;

	public MiembroProyecto(Proyecto proyecto, Usuario usuario, RolProyecto rol) {
		this.proyecto = proyecto;
		this.usuario = usuario;
		this.rol = rol;
	}

	@PrePersist
	void prePersist() {
		if (fechaUnion == null) fechaUnion = LocalDateTime.now();
	}
}
