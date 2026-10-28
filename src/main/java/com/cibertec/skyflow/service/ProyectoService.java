package com.cibertec.skyflow.service;

import java.util.List;

import com.cibertec.skyflow.entity.MiembroProyecto;
import com.cibertec.skyflow.entity.Proyecto;


public interface ProyectoService {

	Proyecto crearProyecto(Proyecto proyecto, Long creadorId);

	MiembroProyecto agregarMiembro(Long proyectoId, Long liderId, String correoNuevoMiembro);

	List<Proyecto> listarPorUsuario(Long usuarioId);

	Proyecto buscarPorId(Long proyectoId, Long usuarioId);

	List<MiembroProyecto> listarMiembros(Long proyectoId, Long usuarioId);

	/**
	 * Genera el PDF de la constancia de finalizacion. Solo el LIDER, y solo si el
	 * proyecto esta COMPLETADO (todas sus tareas completadas). No modifica datos.
	 */
	byte[] generarConstanciaPdf(Long proyectoId, Long usuarioId);
}
