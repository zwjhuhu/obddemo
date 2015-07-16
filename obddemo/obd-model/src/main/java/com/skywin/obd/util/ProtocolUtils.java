package com.skywin.obd.util;

import com.skywin.obd.model.ProtocolEntity;

public final class ProtocolUtils {

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
		byte[] ret = new byte[offset + 1];
		System.arraycopy(buf, 0, ret, 0, offset + 1);
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
