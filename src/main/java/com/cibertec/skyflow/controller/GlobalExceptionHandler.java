package com.cibertec.skyflow.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Errores de negocio de los services (no pertenece, no es lider, no existe...) -> mensaje en el inicio. */
@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
	public String manejarNegocio(RuntimeException e, RedirectAttributes redirect) {
		redirect.addFlashAttribute("error", e.getMessage());
		return "redirect:/home";
	}
}
