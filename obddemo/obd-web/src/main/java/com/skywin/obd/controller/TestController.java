package com.skywin.obd.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.skywin.obd.db.DbUtils;
import com.skywin.obd.model.DeviceInfo;
import com.skywin.obd.model.ObdLog;
import com.skywin.obd.util.CommonUtils;

@Controller
public class TestController {

	private static final String SESSION_USER_TOKENNAME = "user";

	protected final Log logger = LogFactory.getLog(getClass());

	protected WebApplicationContext wac;

	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public String loginpage() {
		return "login";
	}

	@RequestMapping(value = "/login", method = RequestMethod.POST)
	public String login(@RequestParam("pwd") String pwd) {
		if (pwd.equals("skywin")) {
			RequestAttributes ra = RequestContextHolder.getRequestAttributes();
			HttpServletRequest request = ((ServletRequestAttributes) ra)
					.getRequest();
			HttpSession session = request.getSession();
			session.setAttribute(SESSION_USER_TOKENNAME, pwd);
			return "redirect:/home";
		} else {
			throw new IllegalArgumentException("密码错误");
		}
	}

	@RequestMapping(value = "/home", method = RequestMethod.GET)
	public String homepage() {
		return "home";
	}

	@RequestMapping(value = "/apk", method = RequestMethod.GET)
	public String apkpage() {
		return "apk";
	}

	@RequestMapping(value = "/apkdown", method = RequestMethod.GET)
	public void apkdown(@RequestParam("name") String name,
			HttpServletResponse res) throws IOException {
		OutputStream os = res.getOutputStream();
		InputStream in = null;
		try {
			in = wac.getServletContext().getResourceAsStream(
					"/WEB-INF/" + name + ".apk");
			if(in==null){
				throw new IllegalArgumentException("文件不存在");
			}
			res.setHeader("Content-Disposition", "attachment; filename=" + name
					+ ".apk");
			res.setContentType("application/octet-stream; charset=utf-8");
			byte[] buf = new byte[1024*100];
			int n = 0;
			while (n > -1) {
				n = in.read(buf);
				if (n > 0) {
					os.write(buf, 0, n);
				}
			}
			os.flush();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (os != null) {
				try {
					os.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	@RequestMapping(value = "/online", method = RequestMethod.GET)
	public String onlinepage(Model model) {

		long now = System.currentTimeMillis();
		Date start = new Date(now - 15 * 1000L);
		Date end = new Date(now + 15 * 1000L);
		String sql = "select * from deviceinfo where state = 1 and checktime between '"
				+ CommonUtils.format(start)
				+ "' and '"
				+ CommonUtils.format(end) + "'";
		List<DeviceInfo> list = DbUtils.findBYSql(sql, DeviceInfo.class);

		model.addAttribute("datas", list);
		return "online";
	}

	@RequestMapping(value = "/logs", method = RequestMethod.GET)
	public String logspage(Model model) {
		String sql = "select * from obdlog order by id desc limit 20";
		List<ObdLog> list = DbUtils.findBYSql(sql, ObdLog.class);

		model.addAttribute("datas", list);
		return "logs";
	}

	@Autowired
	public void setWac(WebApplicationContext wac) {
		this.wac = wac;
	}

	/**
	 * 输出json格式响应数据
	 * 
	 * @param obj
	 * @param response
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	protected void writeJsonResponse(Object obj, HttpServletResponse response)
			throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		response.setContentType("text/json; charset=UTF-8");
		PrintWriter writer = response.getWriter();
		mapper.writeValue(writer, obj);
		writer.flush();
	}

	protected String findCurrentUserToken() {
		RequestAttributes ra = RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = ((ServletRequestAttributes) ra)
				.getRequest();
		return (String) request.getSession().getAttribute(
				SESSION_USER_TOKENNAME);
	}

	/**
	 * FIXME 得到user-agent
	 * 
	 * @return
	 */
	protected String findUserAgent() {
		RequestAttributes ra = RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = ((ServletRequestAttributes) ra)
				.getRequest();
		return request.getHeader("User-Agent");
	}

}
