package com.cibertec.skyflow.service;

import java.util.List;

import com.cibertec.skyflow.entity.Tarea;


public interface TareaService {

	Tarea crearTarea(Long proyectoId, Long usuarioId, Tarea tarea);

	Tarea actualizarTarea(Long tareaId, Long usuarioId, Tarea datos);

	Tarea completarTarea(Long tareaId, Long usuarioId);

	int calcularProgreso(Long proyectoId);

	List<Tarea> listarPorProyecto(Long proyectoId, Long usuarioId);
}
