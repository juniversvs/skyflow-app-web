package com.cibertec.skyflow.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

import com.cibertec.skyflow.entity.EstadoTarea;
import com.cibertec.skyflow.entity.Proyecto;
import com.cibertec.skyflow.entity.Tarea;
import com.cibertec.skyflow.service.ProyectoService;
import com.cibertec.skyflow.service.TareaService;

@Controller
public class HomeController {

	private final ProyectoService proyectoService;
	private final TareaService tareaService;
	private final VistaHelper helper;

	public HomeController(ProyectoService proyectoService, TareaService tareaService, VistaHelper helper) {
		this.proyectoService = proyectoService;
		this.tareaService = tareaService;
		this.helper = helper;
	}

	@GetMapping("/home")
	public String home(@SessionAttribute("usuarioId") Long usuarioId, Model model) {
		List<Proyecto> proyectos = proyectoService.listarPorUsuario(usuarioId);

		List<Tarea> tareas = new ArrayList<>();
		for (Proyecto p : proyectos)
			tareas.addAll(tareaService.listarPorProyecto(p.getId(), usuarioId));

		long pendientes = tareas.stream().filter(t -> t.getEstado() == EstadoTarea.PENDIENTE).count();
		long completadas = tareas.size() - pendientes;

		List<Tarea> tareasPendientes = tareas.stream()
				.filter(t -> t.getEstado() == EstadoTarea.PENDIENTE)
				.sorted(Comparator.comparing(Tarea::getFechaFin))
				.limit(8)
				.toList();

		model.addAttribute("proyectos", proyectos);
		model.addAttribute("lideres", helper.lideresPorProyecto(proyectos, usuarioId));
		model.addAttribute("totalProyectos", proyectos.size());
		model.addAttribute("tareasPendientes", pendientes);
		model.addAttribute("tareasCompletadas", completadas);
		model.addAttribute("tareas", tareasPendientes);
		return "home";
	}
}
