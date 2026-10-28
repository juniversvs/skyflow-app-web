package com.cibertec.skyflow.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cibertec.skyflow.entity.EstadoProyecto;
import com.cibertec.skyflow.entity.EstadoTarea;
import com.cibertec.skyflow.entity.MiembroProyecto;
import com.cibertec.skyflow.entity.Proyecto;
import com.cibertec.skyflow.entity.RolProyecto;
import com.cibertec.skyflow.entity.Tarea;
import com.cibertec.skyflow.service.NotaService;
import com.cibertec.skyflow.service.ProyectoService;
import com.cibertec.skyflow.service.TareaService;

import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/proyectos")
public class ProyectoController {

	private final ProyectoService proyectoService;
	private final TareaService tareaService;
	private final NotaService notaService;
	private final VistaHelper helper;

	public ProyectoController(ProyectoService proyectoService, TareaService tareaService,
			NotaService notaService, VistaHelper helper) {
		this.proyectoService = proyectoService;
		this.tareaService = tareaService;
		this.notaService = notaService;
		this.helper = helper;
	}

	@GetMapping
	public String listar(@SessionAttribute("usuarioId") Long usuarioId, Model model) {
		List<Proyecto> proyectos = proyectoService.listarPorUsuario(usuarioId);
		model.addAttribute("proyectos", proyectos);
		model.addAttribute("lideres", helper.lideresPorProyecto(proyectos, usuarioId));
		model.addAttribute("activos", proyectos.stream()
				.filter(p -> p.getEstado() == com.cibertec.skyflow.entity.EstadoProyecto.PENDIENTE).count());
		model.addAttribute("finalizados", proyectos.stream()
				.filter(p -> p.getEstado() == com.cibertec.skyflow.entity.EstadoProyecto.COMPLETADO).count());
		return "proyecto/index";
	}

	@GetMapping("/nuevo")
	public String nuevoForm() {
		return "proyecto/create";
	}

	@PostMapping
	public String crear(@SessionAttribute("usuarioId") Long usuarioId,
			@RequestParam String nombre,
			@RequestParam(required = false) String descripcion,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
			RedirectAttributes redirect) {
		try {
			if (nombre.isBlank())
				throw new IllegalArgumentException("El nombre del proyecto es obligatorio.");

			Proyecto p = new Proyecto();
			p.setNombre(nombre.trim());
			p.setDescripcion(descripcion);
			p.setFechaInicio(fechaInicio);
			p.setFechaFin(fechaFin);
			Proyecto creado = proyectoService.crearProyecto(p, usuarioId);

			redirect.addFlashAttribute("ok", "Proyecto creado. Eres el lider.");
			return "redirect:/proyectos/" + creado.getId();
		} catch (IllegalArgumentException e) {
			redirect.addFlashAttribute("error", e.getMessage());
			return "redirect:/proyectos/nuevo";
		}
	}

	@GetMapping("/{id}")
	public String detalle(@PathVariable Long id, @SessionAttribute("usuarioId") Long usuarioId, Model model) {
		Proyecto proyecto = proyectoService.buscarPorId(id, usuarioId);
		List<MiembroProyecto> miembros = proyectoService.listarMiembros(id, usuarioId);
		List<Tarea> tareas = tareaService.listarPorProyecto(id, usuarioId);

		long completadas = tareas.stream().filter(t -> t.getEstado() == EstadoTarea.COMPLETADA).count();
		long pendientes = tareas.size() - completadas;

		model.addAttribute("proyecto", proyecto);
		model.addAttribute("progreso", tareaService.calcularProgreso(id));
		boolean esLider = helper.esLider(miembros, usuarioId);
		cargarDatosDocumento(model, miembros, tareas, completadas, pendientes);
		model.addAttribute("esLider", esLider);
		// La constancia solo la genera el lider y solo con el proyecto COMPLETADO.
		model.addAttribute("puedeConstancia", esLider && proyecto.getEstado() == EstadoProyecto.COMPLETADO);
		model.addAttribute("notas", notaService.listarNotasProyecto(id, usuarioId));
		return "proyecto/detail";
	}

	/** Reporte imprimible del proyecto (cualquier miembro). Solo lectura. */
	@GetMapping("/{id}/reporte")
	public String reporte(@PathVariable Long id, @SessionAttribute("usuarioId") Long usuarioId, Model model) {
		Proyecto proyecto = proyectoService.buscarPorId(id, usuarioId);
		List<MiembroProyecto> miembros = proyectoService.listarMiembros(id, usuarioId);
		List<Tarea> tareas = tareaService.listarPorProyecto(id, usuarioId);
		long completadas = tareas.stream().filter(t -> t.getEstado() == EstadoTarea.COMPLETADA).count();

		model.addAttribute("proyecto", proyecto);
		model.addAttribute("progreso", tareaService.calcularProgreso(id));
		cargarDatosDocumento(model, miembros, tareas, completadas, tareas.size() - completadas);
		model.addAttribute("fechaEmision", LocalDate.now());
		return "proyecto/reporte";
	}

	/** Constancia de finalizacion en PDF (solo lider, solo proyecto COMPLETADO). */
	@GetMapping("/{id}/constancia")
	public String constancia(@PathVariable Long id, @SessionAttribute("usuarioId") Long usuarioId,
			HttpServletResponse response, RedirectAttributes redirect) throws IOException {
		try {
			byte[] pdf = proyectoService.generarConstanciaPdf(id, usuarioId);
			response.setContentType("application/pdf");
			response.setHeader("Content-Disposition", "inline; filename=\"constancia-proyecto-" + id + ".pdf\"");
			response.setContentLength(pdf.length);
			response.getOutputStream().write(pdf);
			return null; // la respuesta ya fue escrita
		} catch (IllegalArgumentException | IllegalStateException e) {
			redirect.addFlashAttribute("error", e.getMessage());
			return "redirect:/proyectos/" + id;
		}
	}

	private void cargarDatosDocumento(Model model, List<MiembroProyecto> miembros, List<Tarea> tareas,
			long completadas, long pendientes) {
		model.addAttribute("miembros", miembros);
		model.addAttribute("lider", miembros.stream()
				.filter(m -> m.getRol() == RolProyecto.LIDER).findFirst().orElse(null));
		model.addAttribute("tareas", tareas);
		model.addAttribute("pendientes", pendientes);
		model.addAttribute("completadas", completadas);
		model.addAttribute("porcentajeCompletadas", tareas.isEmpty() ? 0 : Math.round(completadas * 100.0 / tareas.size()));
	}

	@PostMapping("/{id}/miembros")
	public String agregarMiembro(@PathVariable Long id, @SessionAttribute("usuarioId") Long usuarioId,
			@RequestParam String correo, RedirectAttributes redirect) {
		try {
			proyectoService.agregarMiembro(id, usuarioId, correo.trim());
			redirect.addFlashAttribute("ok", "Miembro agregado como subordinado.");
		} catch (IllegalArgumentException | IllegalStateException e) {
			redirect.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/proyectos/" + id + "?tab=miembros";
	}

	@PostMapping("/{id}/notas")
	public String agregarNota(@PathVariable Long id, @SessionAttribute("usuarioId") Long usuarioId,
			@RequestParam String contenido, RedirectAttributes redirect) {
		try {
			notaService.agregarNotaProyecto(id, usuarioId, contenido);
			redirect.addFlashAttribute("ok", "Nota agregada.");
		} catch (IllegalArgumentException | IllegalStateException e) {
			redirect.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/proyectos/" + id + "?tab=notas";
	}
}
