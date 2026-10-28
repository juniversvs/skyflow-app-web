package com.cibertec.skyflow.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cibertec.skyflow.entity.Usuario;
import com.cibertec.skyflow.service.UsuarioService;
import com.cibertec.skyflow.repository.UsuarioRepository;

@Service
public class UsuarioServiceImpl implements UsuarioService {

	private final UsuarioRepository usuarioRepository;

	public UsuarioServiceImpl(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	@Transactional
	@Override
	public Usuario registrar(Usuario usuario) {
		if (usuarioRepository.existsByCorreo(usuario.getCorreo()))
			throw new IllegalArgumentException("Ya existe una cuenta con ese correo.");
		return usuarioRepository.save(usuario);
	}

	@Transactional(readOnly = true)
	@Override
	public Usuario buscarPorId(Long id) {
		return usuarioRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
	}

	@Transactional(readOnly = true)
	@Override
	public Usuario buscarPorCorreo(String correo) {
		return usuarioRepository.findByCorreo(correo)
				.orElseThrow(() -> new IllegalArgumentException("No existe un usuario con ese correo."));
	}

	@Transactional(readOnly = true)
	@Override
	public List<Usuario> listarTodos() {
		return usuarioRepository.findAll();
	}
}
