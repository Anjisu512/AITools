package com.aitool.aitool.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class LicenseAuthFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String uri = request.getRequestURI();

		// 허용 경로
		if (uri.startsWith("/loginLicense") || uri.startsWith("/api/login") || uri.startsWith("/css")
				|| uri.startsWith("/js")) {

			filterChain.doFilter(request, response);
			return;
		}

		HttpSession session = request.getSession(false);
		boolean authenticated = session != null && Boolean.TRUE.equals(session.getAttribute("LICENSE_AUTH"));

		if (!authenticated) {
			response.sendRedirect("/loginLicense"); // 세션이 없다면 무조건 loginLicense페이지 
			return;
		}

		filterChain.doFilter(request, response);
	}
}
