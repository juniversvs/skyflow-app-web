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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@Entity
@Table(name = "proyectos")
public class Proyecto implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 150)
	private String nombre;

	@Column(columnDefinition = "TEXT")
	private String descripcion;

	@Column(name = "fecha_inicio", nullable = false)
	private LocalDate fechaInicio;

	@Column(name = "fecha_fin", nullable = false)
	private LocalDate fechaFin;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoProyecto estado = EstadoProyecto.PENDIENTE;

	@Column(name = "fecha_creacion", updatable = false)
	private LocalDateTime fechaCreacion;

	@OneToMany(mappedBy = "proyecto", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<MiembroProyecto> miembros = new ArrayList<>();

	@OneToMany(mappedBy = "proyecto", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Tarea> tareas = new ArrayList<>();

	@OneToMany(mappedBy = "proyecto", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<NotaProyecto> notas = new ArrayList<>();

	@PrePersist
	void prePersist() {
		if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
		if (estado == null) estado = EstadoProyecto.PENDIENTE;
	}

	/** Porcentaje (0-100) de tareas completadas; no se guarda en BD. */
	@Transient
	public int getProgreso() {
		if (tareas == null || tareas.isEmpty()) return 0;
		long completadas = tareas.stream()
				.filter(t -> t.getEstado() == EstadoTarea.COMPLETADA)
				.count();
		return (int) Math.round(completadas * 100.0 / tareas.size());
	}

	/** True si hay tareas y todas estan completadas. */
	public boolean todasLasTareasCompletadas() {
		return tareas != null && !tareas.isEmpty()
				&& tareas.stream().allMatch(t -> t.getEstado() == EstadoTarea.COMPLETADA);
	}
}
