package com.cibertec.skyflow.service.impl;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import com.cibertec.skyflow.entity.EstadoProyecto;
import com.cibertec.skyflow.entity.MiembroProyecto;
import com.cibertec.skyflow.entity.Proyecto;
import com.cibertec.skyflow.entity.RolProyecto;
import com.cibertec.skyflow.entity.Usuario;
import com.cibertec.skyflow.service.ProyectoService;
import com.cibertec.skyflow.repository.MiembroProyectoRepository;
import com.cibertec.skyflow.repository.ProyectoRepository;
import com.cibertec.skyflow.repository.UsuarioRepository;

@Service
public class ProyectoServiceImpl implements ProyectoService {

	private final ProyectoRepository proyectoRepository;
	private final MiembroProyectoRepository miembroRepository;
	private final UsuarioRepository usuarioRepository;

	public ProyectoServiceImpl(ProyectoRepository proyectoRepository,
			MiembroProyectoRepository miembroRepository,
			UsuarioRepository usuarioRepository) {
		this.proyectoRepository = proyectoRepository;
		this.miembroRepository = miembroRepository;
		this.usuarioRepository = usuarioRepository;
	}

	/** Crea el proyecto y registra al creador como LIDER (una sola transaccion). */
	@Transactional
	@Override
	public Proyecto crearProyecto(Proyecto proyecto, Long creadorId) {
		Usuario creador = usuarioRepository.findById(creadorId)
				.orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
		if (proyecto.getFechaFin().isBefore(proyecto.getFechaInicio()))
			throw new IllegalArgumentException("La fecha fin no puede ser anterior a la fecha inicio.");

		Proyecto guardado = proyectoRepository.save(proyecto);
		miembroRepository.save(new MiembroProyecto(guardado, creador, RolProyecto.LIDER));
		return guardado;
	}

	/** Solo el LIDER puede agregar miembros; entran como SUBORDINADO. */
	@Transactional
	@Override
	public MiembroProyecto agregarMiembro(Long proyectoId, Long liderId, String correoNuevoMiembro) {
		exigirLider(proyectoId, liderId);

		Usuario nuevo = usuarioRepository.findByCorreo(correoNuevoMiembro)
				.orElseThrow(() -> new IllegalArgumentException("No existe un usuario con ese correo."));
		if (miembroRepository.existsByProyectoIdAndUsuarioId(proyectoId, nuevo.getId()))
			throw new IllegalArgumentException("El usuario ya pertenece al proyecto.");

		Proyecto proyecto = proyectoRepository.getReferenceById(proyectoId);
		return miembroRepository.save(new MiembroProyecto(proyecto, nuevo, RolProyecto.SUBORDINADO));
	}

	/*Metodo para listar tus proyectos*/
	@Transactional(readOnly = true)
	@Override
	public List<Proyecto> listarPorUsuario(Long usuarioId) {
		return proyectoRepository.buscarPorUsuario(usuarioId);
	}

	/** Detalle del proyecto; solo para miembros. */
	@Transactional(readOnly = true)
	@Override
	public Proyecto buscarPorId(Long proyectoId, Long usuarioId) {
		exigirMiembro(proyectoId, usuarioId);
		return proyectoRepository.findById(proyectoId)
				.orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado."));
	}

	@Transactional(readOnly = true)
	@Override
	public List<MiembroProyecto> listarMiembros(Long proyectoId, Long usuarioId) {
		exigirMiembro(proyectoId, usuarioId);
		return miembroRepository.findByProyectoId(proyectoId);
	}

	/** Operacion documental: solo lectura (readOnly), no altera tareas ni progreso. */
	@Transactional(readOnly = true)
	@Override
	public byte[] generarConstanciaPdf(Long proyectoId, Long usuarioId) {
		exigirLider(proyectoId, usuarioId);
		Proyecto p = proyectoRepository.findById(proyectoId)
				.orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado."));
		if (p.getEstado() != EstadoProyecto.COMPLETADO || !p.todasLasTareasCompletadas())
			throw new IllegalStateException(
					"La constancia solo puede generarse cuando todas las tareas estan completadas.");

		List<MiembroProyecto> miembros = miembroRepository.findByProyectoId(proyectoId);
		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Document doc = new Document(PageSize.A4, 56, 56, 64, 56);
		PdfWriter.getInstance(doc, out);
		doc.open();

		Font titulo = new Font(Font.HELVETICA, 20, Font.BOLD, new Color(10, 47, 92));
		Font subtitulo = new Font(Font.HELVETICA, 12, Font.NORMAL, Color.DARK_GRAY);
		Font etiqueta = new Font(Font.HELVETICA, 11, Font.BOLD);
		Font normal = new Font(Font.HELVETICA, 11, Font.NORMAL);

		Paragraph t = new Paragraph("Constancia de Finalización de Proyecto", titulo);
		t.setAlignment(Element.ALIGN_CENTER);
		t.setSpacingAfter(6);
		doc.add(t);
		Paragraph s = new Paragraph("SkyFlow - Gestión colaborativa de proyectos", subtitulo);
		s.setAlignment(Element.ALIGN_CENTER);
		s.setSpacingAfter(26);
		doc.add(s);

		String lider = miembros.stream().filter(m -> m.getRol() == RolProyecto.LIDER)
				.map(m -> m.getUsuario().getNombre() + " " + m.getUsuario().getApellido())
				.findFirst().orElse("-");

		Paragraph cert = new Paragraph("Se deja constancia de que el proyecto \"" + p.getNombre()
				+ "\" ha sido culminado satisfactoriamente, con todas sus tareas completadas.", normal);
		cert.setSpacingAfter(18);
		doc.add(cert);

		PdfPTable datos = new PdfPTable(new float[] { 1.2f, 3f });
		datos.setWidthPercentage(100);
		fila(datos, "Proyecto", p.getNombre(), etiqueta, normal);
		fila(datos, "Descripción", p.getDescripcion() == null || p.getDescripcion().isBlank()
				? "Sin descripción" : p.getDescripcion(), etiqueta, normal);
		fila(datos, "Líder", lider, etiqueta, normal);
		fila(datos, "Fecha de inicio", p.getFechaInicio().format(fmt), etiqueta, normal);
		fila(datos, "Fecha de finalización", p.getFechaFin().format(fmt), etiqueta, normal);
		fila(datos, "Estado final", "COMPLETADO", etiqueta, normal);
		fila(datos, "Avance", "100 %", etiqueta, normal);
		fila(datos, "Total de tareas", String.valueOf(p.getTareas().size()), etiqueta, normal);
		datos.setSpacingAfter(18);
		doc.add(datos);

		doc.add(new Paragraph("Integrantes", etiqueta));
		for (MiembroProyecto m : miembros) {
			doc.add(new Paragraph("- " + m.getUsuario().getNombre() + " " + m.getUsuario().getApellido()
					+ (m.getRol() == RolProyecto.LIDER ? " (Líder)" : " (Subordinado)"), normal));
		}

		Paragraph emision = new Paragraph("Fecha de emisión: " + LocalDate.now().format(fmt), normal);
		emision.setSpacingBefore(30);
		emision.setAlignment(Element.ALIGN_RIGHT);
		doc.add(emision);

		doc.close();
		return out.toByteArray();
	}

	private void fila(PdfPTable tabla, String k, String v, Font fk, Font fv) {
		PdfPCell a = new PdfPCell(new Phrase(k, fk));
		PdfPCell b = new PdfPCell(new Phrase(v, fv));
		a.setPadding(6);
		b.setPadding(6);
		tabla.addCell(a);
		tabla.addCell(b);
	}

	private void exigirMiembro(Long proyectoId, Long usuarioId) {
		if (!miembroRepository.existsByProyectoIdAndUsuarioId(proyectoId, usuarioId))
			throw new IllegalStateException("No perteneces a este proyecto.");
	}

	private void exigirLider(Long proyectoId, Long usuarioId) {
		MiembroProyecto m = miembroRepository.findByProyectoIdAndUsuarioId(proyectoId, usuarioId)
				.orElseThrow(() -> new IllegalStateException("No perteneces a este proyecto."));
		if (m.getRol() != RolProyecto.LIDER)
			throw new IllegalStateException("Solo el lider puede realizar esta accion.");
	}
}
