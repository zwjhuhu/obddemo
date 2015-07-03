package com.skywin.obd.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.skywin.obd.protocol.ProtocolEntity;

public final class ProtocolUtils {

	private static final SimpleDateFormat sdf1 = new SimpleDateFormat(
			"MM/dd HH:mm");

	private static final SimpleDateFormat sdf11 = new SimpleDateFormat(
			"yyyy/MM/dd HH:mm");

	private static final SimpleDateFormat sdf2 = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm");

	private static final String DATEFORMAT_NORMAL = "yyyy-MM-dd HH:mm:ss";

	private static String[] HEXS = new String[] { "0", "1", "2", "3", "4", "5",
			"6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };

	private ProtocolUtils() {

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
			ret[i] = Byte.valueOf(t);
			i++;
		}
		return ret;
	}
	
	public static byte[] transToByte(ProtocolEntity msg) {

		byte[] buf = new byte[1024];
		buf[1] = msg.getSide();
		buf[2] = (byte) (msg.getSrcdeviceid() / 256);
		buf[3] = (byte) (msg.getSrcdeviceid() % 256);

		buf[4] = (byte) (msg.getDestdeviceid() / 256);
		buf[5] = (byte) (msg.getDestdeviceid() % 256);

		buf[6] = msg.getCode();

		buf[7] = (byte) (msg.getSequenceid() / 256);
		buf[8] = (byte) (msg.getSequenceid() % 256);
		int offset = 10;
		if (msg.getContent() != null && msg.getContentlen() > 0
				&& msg.getContent().length == msg.getContentlen()) {
			buf[9] = msg.getContentlen();
			int len = msg.getContent().length;
			byte[] ct = msg.getContent();
			int i = 0;
			for (i = 0; i < len; i++) {
				buf[10 + i] = ct[i];
			}
			offset = 10 + len;
		} else {
			buf[9] = 0;
		}
		msg.calcCheckSum();
		buf[offset] = (byte) (msg.getChecksum() / 256);
		offset++;
		buf[offset] = (byte) (msg.getChecksum() % 256);
		buf[0] = (byte) (offset);
		byte[] ret = new byte[offset+1];
		System.arraycopy(buf, 0, ret, 0, offset+1);
		return ret;
	}

	public static ProtocolEntity createfromByte(byte[] buf) {
		// int size = buf[0] & 0xFF;
		ProtocolEntity msg = new ProtocolEntity();
		msg.setSide(buf[1]);
		msg.setSrcdeviceid((short) ((buf[2] & 0xFF) * 265 + (buf[3] & 0xFF)));

		msg.setDestdeviceid((short) ((buf[4] & 0xFF) * 265 + (buf[5] & 0xFF)));

		msg.setCode(buf[6]);

		msg.setSequenceid((short) ((buf[7] & 0xFF) * 265 + (buf[8] & 0xFF)));

		int len = buf[9] & 0xFF;
		msg.setContentlen((byte) len);
		byte[] content = new byte[len];
		for (int i = 0; i < len; i++) {
			content[i] = buf[10 + i];
		}
		msg.setContent(content);

		int offset = 10 + len;
		msg.setChecksum((short) ((buf[offset++] & 0xFF) * 265 + (buf[offset] & 0xFF)));
		return msg;
	}
}

