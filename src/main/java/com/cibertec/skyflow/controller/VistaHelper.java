package com.cibertec.skyflow.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.cibertec.skyflow.entity.MiembroProyecto;
import com.cibertec.skyflow.entity.Proyecto;
import com.cibertec.skyflow.entity.RolProyecto;
import com.cibertec.skyflow.service.ProyectoService;

/** Utilidades compartidas por los controllers para armar datos de las vistas. */
@Component
public class VistaHelper {

	private final ProyectoService proyectoService;

	public VistaHelper(ProyectoService proyectoService) {
		this.proyectoService = proyectoService;
	}

	/** proyectoId -> "Nombre Apellido" del lider. */
	public Map<Long, String> lideresPorProyecto(List<Proyecto> proyectos, Long usuarioId) {
		Map<Long, String> lideres = new HashMap<>();
		for (Proyecto p : proyectos) {
			for (MiembroProyecto m : proyectoService.listarMiembros(p.getId(), usuarioId)) {
				if (m.getRol() == RolProyecto.LIDER) {
					lideres.put(p.getId(), m.getUsuario().getNombre() + " " + m.getUsuario().getApellido());
					break;
				}
			}
		}
		return lideres;
	}

	public boolean esLider(List<MiembroProyecto> miembros, Long usuarioId) {
		return miembros.stream().anyMatch(
				m -> m.getUsuario().getId().equals(usuarioId) && m.getRol() == RolProyecto.LIDER);
	}
}
