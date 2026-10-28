package com.cibertec.skyflow.service;

import java.util.List;

import com.cibertec.skyflow.entity.NotaProyecto;
import com.cibertec.skyflow.entity.NotaTarea;


public interface NotaService {

	NotaProyecto agregarNotaProyecto(Long proyectoId, Long usuarioId, String contenido);

	NotaTarea agregarNotaTarea(Long tareaId, Long usuarioId, String contenido);

	List<NotaProyecto> listarNotasProyecto(Long proyectoId, Long usuarioId);

	List<NotaTarea> listarNotasTarea(Long tareaId, Long usuarioId);
}
