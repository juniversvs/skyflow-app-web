package com.cibertec.skyflow.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cibertec.skyflow.entity.NotaProyecto;
import com.cibertec.skyflow.entity.NotaTarea;
import com.cibertec.skyflow.entity.Tarea;
import com.cibertec.skyflow.service.NotaService;
import com.cibertec.skyflow.repository.MiembroProyectoRepository;
import com.cibertec.skyflow.repository.NotaProyectoRepository;
import com.cibertec.skyflow.repository.NotaTareaRepository;
import com.cibertec.skyflow.repository.ProyectoRepository;
import com.cibertec.skyflow.repository.TareaRepository;
import com.cibertec.skyflow.repository.UsuarioRepository;

/** Lider y subordinados pueden agregar notas, siempre que pertenezcan al proyecto. */
@Service
public class NotaServiceImpl implements NotaService {

	private final NotaProyectoRepository notaProyectoRepository;
	private final NotaTareaRepository notaTareaRepository;
	private final ProyectoRepository proyectoRepository;
	private final TareaRepository tareaRepository;
	private final UsuarioRepository usuarioRepository;
	private final MiembroProyectoRepository miembroRepository;

	public NotaServiceImpl(NotaProyectoRepository notaProyectoRepository,
			NotaTareaRepository notaTareaRepository,
			ProyectoRepository proyectoRepository,
			TareaRepository tareaRepository,
			UsuarioRepository usuarioRepository,
			MiembroProyectoRepository miembroRepository) {
		this.notaProyectoRepository = notaProyectoRepository;
		this.notaTareaRepository = notaTareaRepository;
		this.proyectoRepository = proyectoRepository;
		this.tareaRepository = tareaRepository;
		this.usuarioRepository = usuarioRepository;
		this.miembroRepository = miembroRepository;
	}

	@Transactional
	@Override
	public NotaProyecto agregarNotaProyecto(Long proyectoId, Long usuarioId, String contenido) {
		exigirMiembro(proyectoId, usuarioId);
		exigirContenido(contenido);

		NotaProyecto nota = new NotaProyecto();
		nota.setProyecto(proyectoRepository.getReferenceById(proyectoId));
		nota.setUsuario(usuarioRepository.getReferenceById(usuarioId));
		nota.setContenido(contenido.trim());
		return notaProyectoRepository.save(nota);
	}

	@Transactional
	@Override
	public NotaTarea agregarNotaTarea(Long tareaId, Long usuarioId, String contenido) {
		Tarea tarea = tareaRepository.findById(tareaId)
				.orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada."));
		exigirMiembro(tarea.getProyecto().getId(), usuarioId);
		exigirContenido(contenido);

		NotaTarea nota = new NotaTarea();
		nota.setTarea(tarea);
		nota.setUsuario(usuarioRepository.getReferenceById(usuarioId));
		nota.setContenido(contenido.trim());
		return notaTareaRepository.save(nota);
	}

	@Transactional(readOnly = true)
	@Override
	public List<NotaProyecto> listarNotasProyecto(Long proyectoId, Long usuarioId) {
		exigirMiembro(proyectoId, usuarioId);
		return notaProyectoRepository.findByProyectoIdOrderByFechaCreacionDesc(proyectoId);
	}

	@Transactional(readOnly = true)
	@Override
	public List<NotaTarea> listarNotasTarea(Long tareaId, Long usuarioId) {
		Tarea tarea = tareaRepository.findById(tareaId)
				.orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada."));
		exigirMiembro(tarea.getProyecto().getId(), usuarioId);
		return notaTareaRepository.findByTareaIdOrderByFechaCreacionDesc(tareaId);
	}

	private void exigirMiembro(Long proyectoId, Long usuarioId) {
		if (!miembroRepository.existsByProyectoIdAndUsuarioId(proyectoId, usuarioId))
			throw new IllegalStateException("No perteneces a este proyecto.");
	}

	private void exigirContenido(String contenido) {
		if (contenido == null || contenido.isBlank())
			throw new IllegalArgumentException("La nota no puede estar vacia.");
	}
}
