package com.skywin.obd.model;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.skywin.obd.db.DbUtils;

public class ModelMapper {
	private static Logger logger = LoggerFactory.getLogger(ModelMapper.class);

	private static ModelMapper modelMapper = new ModelMapper();

	private ModelMapper() {

	}

	public static ModelMapper getInstance() {
		return modelMapper;
	}

	private static Map<String, String> tabMap = new HashMap<String, String>();

	private static ConcurrentHashMap<String, Map<String, Method>> setMap = new ConcurrentHashMap<String, Map<String, Method>>();

	private static ConcurrentHashMap<String, Map<String, Method>> getMap = new ConcurrentHashMap<String, Map<String, Method>>();

	public Map<String, Method> findSetMap(Class<?> clazz) {
		if (clazz == null) {
			throw new IllegalArgumentException("clazz is null!");
		}

		String claname = clazz.getName();
		Map<String, Method> ret = setMap.get(claname);
		if (ret == null) {
			ret = new HashMap<String, Method>();
			Method[] mes = clazz.getMethods();
			String fname;
			String mname;
			for (Method me : mes) {
				mname = me.getName();
				if (mname.startsWith("set")) {
					fname = mname.substring(3);
					fname = fname.toLowerCase().substring(0, 1)
							+ fname.substring(1);
					ret.put(fname, me);
				}
			}
			setMap.putIfAbsent(claname, ret);

		}
		return ret;

	}

	public Map<String, Method> findGetMap(Class<?> clazz) {
		if (clazz == null) {
			throw new IllegalArgumentException("clazz is null!");
		}
		String claname = clazz.getName();
		Map<String, Method> ret = getMap.get(claname);
		if (ret == null) {
			ret = new HashMap<String, Method>();
			Method[] mes = clazz.getMethods();
			String fname;
			String mname;
			for (Method me : mes) {
				mname = me.getName();
				if ((mname.startsWith("get") || mname.startsWith("is"))
						&& !mname.equals("getClass")) {
					fname = mname.substring(3);
					fname = fname.toLowerCase().substring(0, 1)
							+ fname.substring(1);
					ret.put(fname, me);
				}
			}
			getMap.putIfAbsent(claname, ret);

		}
		return ret;

	}

	private static void createMapper() {
		InputStream in = DbUtils.class.getClassLoader().getResourceAsStream(
				"mapper.properties");
		if (in != null) {
			Properties props = new Properties();
			try {
				props.load(in);
				String claname = "";
				String tabname = "";
				for (Object key : props.keySet()) {
					if (key != null) {
						claname = key.toString().trim();
						tabname = props.getProperty(key.toString(), "").trim();
						if (!"".equals(claname) && !"".equals(tabname)) {
							tabMap.put(claname, tabname);
						}
					}
				}

			} catch (IOException e) {
				logger.error("load mapper file error!");
				tabMap.clear();
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						logger.error("error", e);
					}
				}
			}

		} else {
			logger.info("can't find mapper file!");
		}
	}

	static {
		createMapper();
	}

	public String findTabName(Class<?> clazz) {
		if (clazz == null) {
			throw new IllegalArgumentException("model is null!");
		}
		String tab = tabMap.get(clazz.getName());
		if (tab == null) {
			tab = clazz.getSimpleName().toLowerCase();
		}
		return tab;
	}
}
