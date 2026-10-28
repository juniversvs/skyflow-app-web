package com.cibertec.skyflow.service;

import java.util.List;

import com.cibertec.skyflow.entity.Usuario;


public interface UsuarioService {

	Usuario registrar(Usuario usuario);

	Usuario buscarPorId(Long id);

	Usuario buscarPorCorreo(String correo);

	List<Usuario> listarTodos();
}
