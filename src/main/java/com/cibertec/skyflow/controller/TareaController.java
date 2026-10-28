package com.cibertec.skyflow.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cibertec.skyflow.entity.EstadoTarea;
import com.cibertec.skyflow.entity.MiembroProyecto;
import com.cibertec.skyflow.entity.Prioridad;
import com.cibertec.skyflow.entity.Proyecto;
import com.cibertec.skyflow.entity.Tarea;
import com.cibertec.skyflow.service.NotaService;
import com.cibertec.skyflow.service.ProyectoService;
import com.cibertec.skyflow.service.TareaService;

@Controller
public class TareaController {

	private final TareaService tareaService;
	private final ProyectoService proyectoService;
	private final NotaService notaService;
	private final VistaHelper helper;

	public TareaController(TareaService tareaService, ProyectoService proyectoService,
			NotaService notaService, VistaHelper helper) {
		this.tareaService = tareaService;
		this.proyectoService = proyectoService;
		this.notaService = notaService;
		this.helper = helper;
	}

	
	@GetMapping("/tareas")
	public String listar(@SessionAttribute("usuarioId") Long usuarioId,
			@RequestParam(required = false) Long proyectoId, Model model) {
		List<Proyecto> proyectos = proyectoService.listarPorUsuario(usuarioId);
		model.addAttribute("proyectos", proyectos);
		if (proyectos.isEmpty())
			return "tarea/index";

		Proyecto seleccionado = proyectos.stream()
				.filter(p -> p.getId().equals(proyectoId)).findFirst().orElse(proyectos.get(0));
		List<Tarea> tareas = tareaService.listarPorProyecto(seleccionado.getId(), usuarioId);
		long completadas = tareas.stream().filter(t -> t.getEstado() == EstadoTarea.COMPLETADA).count();

		model.addAttribute("seleccionado", seleccionado);
		model.addAttribute("tareas", tareas);
		model.addAttribute("pendientes", tareas.size() - completadas);
		model.addAttribute("completadas", completadas);
		return "tarea/index";
	}

	@GetMapping("/proyectos/{pid}/tareas/nueva")
	public String nuevaForm(@PathVariable Long pid, @SessionAttribute("usuarioId") Long usuarioId, Model model) {
		exigirLider(pid, usuarioId);
		model.addAttribute("proyecto", proyectoService.buscarPorId(pid, usuarioId));
		return "tarea/create";
	}

	@PostMapping("/proyectos/{pid}/tareas")
	public String crear(@PathVariable Long pid, @SessionAttribute("usuarioId") Long usuarioId,
			@RequestParam String titulo,
			@RequestParam(required = false) String descripcion,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
			@RequestParam Prioridad prioridad, RedirectAttributes redirect) {
		try {
			if (titulo.isBlank())
				throw new IllegalArgumentException("El nombre de la tarea es obligatorio.");
			Tarea t = armar(titulo, descripcion, fechaInicio, fechaFin, prioridad);
			Tarea creada = tareaService.crearTarea(pid, usuarioId, t);
			redirect.addFlashAttribute("ok", "Tarea creada.");
			return "redirect:/proyectos/" + pid + "/tareas/" + creada.getId();
		} catch (IllegalArgumentException | IllegalStateException e) {
			redirect.addFlashAttribute("error", e.getMessage());
			return "redirect:/proyectos/" + pid + "/tareas/nueva";
		}
	}

	@GetMapping("/proyectos/{pid}/tareas/{tid}")
	public String detalle(@PathVariable Long pid, @PathVariable Long tid,
			@SessionAttribute("usuarioId") Long usuarioId, Model model) {
		Tarea tarea = buscarTarea(pid, tid, usuarioId);
		model.addAttribute("tarea", tarea);
		model.addAttribute("proyecto", tarea.getProyecto());
		model.addAttribute("esLider", esLider(pid, usuarioId));
		model.addAttribute("notas", notaService.listarNotasTarea(tid, usuarioId));
		return "tarea/detail";
	}

	@GetMapping("/proyectos/{pid}/tareas/{tid}/editar")
	public String editarForm(@PathVariable Long pid, @PathVariable Long tid,
			@SessionAttribute("usuarioId") Long usuarioId, Model model) {
		exigirLider(pid, usuarioId);
		Tarea tarea = buscarTarea(pid, tid, usuarioId);
		model.addAttribute("tarea", tarea);
		model.addAttribute("proyecto", tarea.getProyecto());
		return "tarea/edit";
	}

	@PostMapping("/proyectos/{pid}/tareas/{tid}/editar")
	public String editar(@PathVariable Long pid, @PathVariable Long tid,
			@SessionAttribute("usuarioId") Long usuarioId,
			@RequestParam String titulo,
			@RequestParam(required = false) String descripcion,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
			@RequestParam Prioridad prioridad, RedirectAttributes redirect) {
		try {
			if (titulo.isBlank())
				throw new IllegalArgumentException("El nombre de la tarea es obligatorio.");
			if (fechaFin.isBefore(fechaInicio))
				throw new IllegalArgumentException("La fecha fin no puede ser anterior a la fecha inicio.");
			tareaService.actualizarTarea(tid, usuarioId, armar(titulo, descripcion, fechaInicio, fechaFin, prioridad));
			redirect.addFlashAttribute("ok", "Tarea actualizada.");
			return "redirect:/proyectos/" + pid + "/tareas/" + tid;
		} catch (IllegalArgumentException | IllegalStateException e) {
			redirect.addFlashAttribute("error", e.getMessage());
			return "redirect:/proyectos/" + pid + "/tareas/" + tid + "/editar";
		}
	}

	/** Proceso principal: el lider completa la tarea y se recalcula el proyecto. */
	@PostMapping("/proyectos/{pid}/tareas/{tid}/completar")
	public String completar(@PathVariable Long pid, @PathVariable Long tid,
			@SessionAttribute("usuarioId") Long usuarioId, RedirectAttributes redirect) {
		try {
			tareaService.completarTarea(tid, usuarioId);
			redirect.addFlashAttribute("ok", "Tarea completada. El progreso del proyecto fue recalculado.");
		} catch (IllegalArgumentException | IllegalStateException e) {
			redirect.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/proyectos/" + pid + "/tareas/" + tid;
	}

	@PostMapping("/proyectos/{pid}/tareas/{tid}/notas")
	public String agregarNota(@PathVariable Long pid, @PathVariable Long tid,
			@SessionAttribute("usuarioId") Long usuarioId, @RequestParam String contenido,
			RedirectAttributes redirect) {
		try {
			notaService.agregarNotaTarea(tid, usuarioId, contenido);
			redirect.addFlashAttribute("ok", "Nota agregada.");
		} catch (IllegalArgumentException | IllegalStateException e) {
			redirect.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/proyectos/" + pid + "/tareas/" + tid;
	}

	// ---------- privados ----------

	private Tarea armar(String titulo, String descripcion, LocalDate inicio, LocalDate fin, Prioridad prioridad) {
		Tarea t = new Tarea();
		t.setTitulo(titulo.trim());
		t.setDescripcion(descripcion);
		t.setFechaInicio(inicio);
		t.setFechaFin(fin);
		t.setPrioridad(prioridad);
		return t;
	}

	/** Busca la tarea dentro del proyecto (valida pertenencia via TareaService). */
	private Tarea buscarTarea(Long pid, Long tid, Long usuarioId) {
		return tareaService.listarPorProyecto(pid, usuarioId).stream()
				.filter(t -> t.getId().equals(tid)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada."));
	}

	private boolean esLider(Long pid, Long usuarioId) {
		List<MiembroProyecto> miembros = proyectoService.listarMiembros(pid, usuarioId);
		return helper.esLider(miembros, usuarioId);
	}

	private void exigirLider(Long pid, Long usuarioId) {
		if (!esLider(pid, usuarioId))
			throw new IllegalStateException("Solo el lider puede realizar esta accion.");
	}
}
