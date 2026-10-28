package com.cibertec.skyflow.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cibertec.skyflow.config.AuthInterceptor;
import com.cibertec.skyflow.entity.Usuario;
import com.cibertec.skyflow.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

	private final UsuarioService usuarioService;

	public AuthController(UsuarioService usuarioService) {
		this.usuarioService = usuarioService;
	}

	@GetMapping("/")
	public String raiz() {
		return "redirect:/home";
	}

	@GetMapping("/login")
	public String loginForm(HttpSession session) {
		if (session.getAttribute(AuthInterceptor.SESION_USUARIO_ID) != null)
			return "redirect:/home";
		return "login";
	}

	@PostMapping("/login")
	public String login(@RequestParam String correo, @RequestParam String password,
			HttpServletRequest request, RedirectAttributes redirect) {
		try {
			Usuario u = usuarioService.buscarPorCorreo(correo.trim());
			if (!u.getPassword().equals(password))
				throw new IllegalArgumentException("Correo o contrasena incorrectos.");

			request.changeSessionId(); // evita fijacion de sesion
			HttpSession session = request.getSession();
			session.setAttribute(AuthInterceptor.SESION_USUARIO_ID, u.getId());
			session.setAttribute(AuthInterceptor.SESION_USUARIO_NOMBRE, u.getNombre() + " " + u.getApellido());
			return "redirect:/home";
		} catch (IllegalArgumentException e) {
			redirect.addFlashAttribute("error", "Correo o contrasena incorrectos.");
			redirect.addFlashAttribute("correo", correo);
			return "redirect:/login";
		}
	}

	@GetMapping("/registro")
	public String registroForm() {
		return "registro";
	}

	@PostMapping("/registro")
	public String registrar(@RequestParam String nombre, @RequestParam String apellido,
			@RequestParam String correo, @RequestParam String password,
			RedirectAttributes redirect) {
		try {
			if (nombre.isBlank() || apellido.isBlank() || correo.isBlank() || password.isBlank())
				throw new IllegalArgumentException("Completa todos los campos.");

			Usuario u = new Usuario();
			u.setNombre(nombre.trim());
			u.setApellido(apellido.trim());
			u.setCorreo(correo.trim());
			u.setPassword(password);
			usuarioService.registrar(u);

			redirect.addFlashAttribute("ok", "Cuenta creada. Ya puedes iniciar sesion.");
			return "redirect:/login";
		} catch (IllegalArgumentException e) {
			redirect.addFlashAttribute("error", e.getMessage());
			redirect.addFlashAttribute("nombre", nombre);
			redirect.addFlashAttribute("apellido", apellido);
			redirect.addFlashAttribute("correo", correo);
			return "redirect:/registro";
		}
	}

	@GetMapping("/logout")
	public String logout(HttpSession session) {
		session.invalidate();
		return "redirect:/login";
	}
}
