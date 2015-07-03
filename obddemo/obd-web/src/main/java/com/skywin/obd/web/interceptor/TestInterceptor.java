package com.skywin.obd.web.interceptor;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class TestInterceptor implements HandlerInterceptor {

	private static final String LOGIN_PAGE = "/login";
	private static final String ERR_PAGE = "/errors";
	private static final String DOWNLOAD_PAGE = "/apkdown";

	@Override
	public boolean preHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler) throws Exception {

		if (request.getRequestURI().endsWith(LOGIN_PAGE)
				|| request.getRequestURI().endsWith(ERR_PAGE)
				|| request.getRequestURI().endsWith(DOWNLOAD_PAGE)) {
			return true;
		}
		HttpSession session = request.getSession();
		String token = (String) session.getAttribute("user");
		if (token == null || token.trim().isEmpty()) {
			sendRedirectPage(response, request.getContextPath() + LOGIN_PAGE);
			return false;
		}

		return true;
	}

	private void sendRedirectPage(HttpServletResponse response, String url)
			throws IOException {
		response.sendRedirect(url);
	}

	@Override
	public void postHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {

	}

	@Override
	public void afterCompletion(HttpServletRequest request,
			HttpServletResponse response, Object handler, Exception ex)
			throws Exception {

	}

}