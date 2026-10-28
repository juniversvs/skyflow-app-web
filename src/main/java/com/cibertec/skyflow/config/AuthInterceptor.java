package com.cibertec.skyflow.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Redirige a /login si no hay un usuario en sesion. */
@Component
public class AuthInterceptor implements HandlerInterceptor {

	public static final String SESION_USUARIO_ID = "usuarioId";
	public static final String SESION_USUARIO_NOMBRE = "usuarioNombre";

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		if (request.getSession().getAttribute(SESION_USUARIO_ID) == null) {
			response.sendRedirect(request.getContextPath() + "/login");
			return false;
		}
		return true;
	}
}
