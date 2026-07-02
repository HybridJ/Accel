package com.accel.api.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class BoardRequestLoggingFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(BoardRequestLoggingFilter.class);

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String path = request.getRequestURI();

		if (!path.startsWith("/boards")) {
			filterChain.doFilter(request, response);
			return;
		}

		long startedAt = System.currentTimeMillis();
		String query = request.getQueryString();
		String fullPath = query == null ? path : path + "?" + query;

		log.info(
				"[boards-request] incoming method={} path={} contentType={} contentLength={} remote={}",
				request.getMethod(),
				fullPath,
				request.getContentType(),
				request.getContentLengthLong(),
				request.getRemoteAddr());

		try {
			filterChain.doFilter(request, response);
		} finally {
			log.info(
					"[boards-request] completed method={} path={} status={} elapsedMs={}",
					request.getMethod(),
					fullPath,
					response.getStatus(),
					System.currentTimeMillis() - startedAt);
		}
	}
}
