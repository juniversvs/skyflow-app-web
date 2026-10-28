package com.cibertec.skyflow.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cibertec.skyflow.entity.EstadoProyecto;
import com.cibertec.skyflow.entity.EstadoTarea;
import com.cibertec.skyflow.entity.MiembroProyecto;
import com.cibertec.skyflow.entity.Proyecto;
import com.cibertec.skyflow.entity.RolProyecto;
import com.cibertec.skyflow.entity.Tarea;
import com.cibertec.skyflow.service.TareaService;
import com.cibertec.skyflow.repository.MiembroProyectoRepository;
import com.cibertec.skyflow.repository.ProyectoRepository;
import com.cibertec.skyflow.repository.TareaRepository;

@Service
public class TareaServiceImpl implements TareaService {

	private final TareaRepository tareaRepository;
	private final ProyectoRepository proyectoRepository;
	private final MiembroProyectoRepository miembroRepository;

	public TareaServiceImpl(TareaRepository tareaRepository,
			ProyectoRepository proyectoRepository,
			MiembroProyectoRepository miembroRepository) {
		this.tareaRepository = tareaRepository;
		this.proyectoRepository = proyectoRepository;
		this.miembroRepository = miembroRepository;
	}

	/** Solo el LIDER crea tareas. Si el proyecto estaba COMPLETADO, vuelve a PENDIENTE. */
	@Transactional
	@Override
	public Tarea crearTarea(Long proyectoId, Long usuarioId, Tarea tarea) {
		exigirLider(proyectoId, usuarioId);
		if (tarea.getFechaFin().isBefore(tarea.getFechaInicio()))
			throw new IllegalArgumentException("La fecha fin no puede ser anterior a la fecha inicio.");

		Proyecto proyecto = proyectoRepository.findById(proyectoId)
				.orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado."));
		tarea.setProyecto(proyecto);
		tarea.setEstado(EstadoTarea.PENDIENTE);

		if (proyecto.getEstado() == EstadoProyecto.COMPLETADO)
			proyecto.setEstado(EstadoProyecto.PENDIENTE);

		return tareaRepository.save(tarea);
	}

	@Transactional
	@Override
	public Tarea actualizarTarea(Long tareaId, Long usuarioId, Tarea datos) {
		Tarea tarea = buscarTarea(tareaId);
		exigirLider(tarea.getProyecto().getId(), usuarioId);

		tarea.setTitulo(datos.getTitulo());
		tarea.setDescripcion(datos.getDescripcion());
		tarea.setFechaInicio(datos.getFechaInicio());
		tarea.setFechaFin(datos.getFechaFin());
		tarea.setPrioridad(datos.getPrioridad());
		return tareaRepository.save(tarea);
	}

	/**
	 * Proceso principal: el LIDER marca la tarea como completada, y en la misma
	 * transaccion se recalcula el progreso y, si ya no quedan pendientes,
	 * el proyecto pasa a COMPLETADO. Si algo falla, se revierte todo.
	 */
	@Transactional
	@Override
	public Tarea completarTarea(Long tareaId, Long usuarioId) {
		Tarea tarea = buscarTarea(tareaId);
		Proyecto proyecto = tarea.getProyecto();
		exigirLider(proyecto.getId(), usuarioId);

		if (tarea.getEstado() == EstadoTarea.COMPLETADA)
			throw new IllegalStateException("La tarea ya esta completada.");

		tarea.completar();
		tareaRepository.saveAndFlush(tarea);

		long pendientes = tareaRepository.countByProyectoIdAndEstado(
				proyecto.getId(), EstadoTarea.PENDIENTE);
		if (pendientes == 0) {
			proyecto.setEstado(EstadoProyecto.COMPLETADO);
			proyectoRepository.save(proyecto);
		}
		return tarea;
	}

	/** Progreso (0-100) = tareas completadas / total de tareas. */
	@Transactional(readOnly = true)
	@Override
	public int calcularProgreso(Long proyectoId) {
		long total = tareaRepository.countByProyectoId(proyectoId);
		if (total == 0) return 0;
		long completadas = tareaRepository.countByProyectoIdAndEstado(proyectoId, EstadoTarea.COMPLETADA);
		return (int) Math.round(completadas * 100.0 / total);
	}

	@Transactional(readOnly = true)
	@Override
	public List<Tarea> listarPorProyecto(Long proyectoId, Long usuarioId) {
		if (!miembroRepository.existsByProyectoIdAndUsuarioId(proyectoId, usuarioId))
			throw new IllegalStateException("No perteneces a este proyecto.");
		return tareaRepository.findByProyectoId(proyectoId);
	}

	private Tarea buscarTarea(Long tareaId) {
		return tareaRepository.findById(tareaId)
				.orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada."));
	}

	private void exigirLider(Long proyectoId, Long usuarioId) {
		MiembroProyecto m = miembroRepository.findByProyectoIdAndUsuarioId(proyectoId, usuarioId)
				.orElseThrow(() -> new IllegalStateException("No perteneces a este proyecto."));
		if (m.getRol() != RolProyecto.LIDER)
			throw new IllegalStateException("Solo el lider puede realizar esta accion.");
	}
}
