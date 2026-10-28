package com.cibertec.skyflow.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@Entity
@Table(name = "tareas")
public class Tarea implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "proyecto_id", nullable = false)
	private Proyecto proyecto;

	@Column(nullable = false, length = 200)
	private String titulo;

	@Column(columnDefinition = "TEXT")
	private String descripcion;

	@Column(name = "fecha_inicio", nullable = false)
	private LocalDate fechaInicio;

	@Column(name = "fecha_fin", nullable = false)
	private LocalDate fechaFin;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Prioridad prioridad;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoTarea estado = EstadoTarea.PENDIENTE;

	@Column(name = "fecha_creacion", updatable = false)
	private LocalDateTime fechaCreacion;

	@OneToMany(mappedBy = "tarea", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<NotaTarea> notas = new ArrayList<>();

	@PrePersist
	void prePersist() {
		if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
		if (estado == null) estado = EstadoTarea.PENDIENTE;
	}

	public void completar() {
		this.estado = EstadoTarea.COMPLETADA;
	}
}
