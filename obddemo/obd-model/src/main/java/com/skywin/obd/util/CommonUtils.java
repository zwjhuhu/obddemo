package com.skywin.obd.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public final class CommonUtils {

	private static final SimpleDateFormat sdf1 = new SimpleDateFormat(
			"MM/dd HH:mm");

	private static final SimpleDateFormat sdf11 = new SimpleDateFormat(
			"yyyy/MM/dd HH:mm");

	private static final SimpleDateFormat sdf2 = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm");

	private static final String DATEFORMAT_NORMAL = "yyyy-MM-dd HH:mm:ss";

	private static String[] HEXS = new String[] { "0", "1", "2", "3", "4", "5",
			"6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };

	private CommonUtils() {

	}

	public static String format(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat(DATEFORMAT_NORMAL);
		return sdf.format(date);
	}

	public static String format(Date date, String format) {
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(date);
	}

	public static Date translateDate(String str) {
		Date date = null;
		try {
			date = sdf1.parse(str);
			date = sdf11.parse(Calendar.getInstance().get(Calendar.YEAR) + "/"
					+ str);

		} catch (Exception e) {
			date = null;
		}

		if (date == null) {

			try {
				date = sdf2.parse(str);
			} catch (Exception e) {
				date = null;
			}
		}
		return date;
	}

	public static Date translateDate(String str, String pattern) {
		Date date = null;
		try {
			date = new SimpleDateFormat(pattern).parse(str);
		} catch (ParseException e) {
			// ignore
			date = null;
		}
		return date;
	}

	public static Integer parseInteger(String str) {
		Integer ret = null;
		try {
			ret = Integer.parseInt(str);
		} catch (RuntimeException e) {
			// ignore
			ret = null;
		}
		return ret;
	}

	public static String toHexString(byte[] content, int len) {
		if (content == null || content.length == 0) {
			return "";
		}
		if (len > content.length) {
			len = content.length;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < len; i++) {
			int c = content[i];
			c &= 0xFF;
			sb.append(' ').append(HEXS[c / 16]).append(HEXS[c % 16]);
		}
		sb.deleteCharAt(0);
		return sb.toString();
	}

	public static String toHexString(byte[] content) {
		if (content == null) {
			return "";
		}
		return toHexString(content, content.length);
	}

	public static byte[] parseHexString(String str) {
		str = str.trim();
		String[] temps = str.split(" ");
		byte[] ret = new byte[temps.length];
		int i = 0;
		for (String t : temps) {
			ret[i] = Integer.valueOf(t, 16).byteValue();
			i++;
		}
		return ret;
	}
}
