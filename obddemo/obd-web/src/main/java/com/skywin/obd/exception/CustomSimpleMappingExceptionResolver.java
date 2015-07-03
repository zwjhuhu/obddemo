package com.skywin.obd.exception;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;

public class CustomSimpleMappingExceptionResolver extends
		SimpleMappingExceptionResolver {

	@Override
	protected ModelAndView doResolveException(HttpServletRequest request,
			HttpServletResponse response, Object handler, Exception ex) {

		// 打印异常
		logger.error("exception occur!", ex);
		// Expose ModelAndView for chosen error view.
		String viewName = determineViewName(ex, request);
		if (viewName != null) {
			// JSP格式返回
			// 如果不是异步请求
			if (!(request.getHeader("Accept").indexOf("application/json") > -1 || (request
					.getHeader("X-Requested-With") != null && request
					.getHeader("X-Requested-With").indexOf("XMLHttpRequest") > -1))) {
				// Apply HTTP status code for error views, if specified.
				// Only apply it if we're processing a top-level request.
				Integer statusCode = determineStatusCode(request, viewName);
				if (statusCode != null) {
					applyStatusCodeIfPossible(request, response, statusCode);
				}
				ModelAndView view = getModelAndView(viewName, ex, request);
				view.addObject("statusCode", statusCode);
				view.addObject("errorMessage", ex.getMessage());
				return view;
			} else {
				// JSON格式返回
				// 这里暂时设置一种固定的格式来返回{success:false,message:"服务器异常",exception:ex.getMessage()}
				try {
					ObjectMapper mapper = new ObjectMapper();
					response.setContentType("application/json; charset=UTF-8");
					PrintWriter writer = response.getWriter();
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("success", false);
					map.put("message", "服务器异常");
					map.put("exception", ex.getMessage());

					mapper.writeValue(response.getWriter(), map);
					writer.flush();
				} catch (Exception e) {
					logger.error("json format error!", e);
				}
				return null;

			}
		} else {
			return null;
		}
	}
}
