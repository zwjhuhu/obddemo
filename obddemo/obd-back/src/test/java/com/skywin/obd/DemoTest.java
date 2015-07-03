package com.skywin.obd;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.junit.Test;

import com.skywin.obd.model.ModelConstant;
import com.skywin.obd.model.ProtocolEntity;

public class DemoTest {

	@Test
	public void serverSend() throws Exception {
		Socket s = new Socket();
		s.connect(new InetSocketAddress("localhost", 7000));
		OutputStream out = s.getOutputStream();
		InputStream in = s.getInputStream();

		byte[] buf = new byte[1024];
		ProtocolEntity msg = new ProtocolEntity();
		msg.setSide(ModelConstant.SIDE_SERVER);
		msg.setCode(ModelConstant.CODE_BEAT);
		msg.setContent(null);
		msg.setContentlen((byte) 0);
		msg.setDestdeviceid((short) 0);
		msg.setSequenceid((short) 0);
		msg.setSrcdeviceid((short) 0);
		System.out.println("send: " + msg);
		byte[] cont = transToByte(msg);
		out.write(cont);
		in.read(buf);

		ProtocolEntity res = createfromByte(buf);
		System.out.println("recv :" + res);
		msg.setCode(ModelConstant.CODE_CMD);
		msg.setDestdeviceid((short) 1);
		msg.setContentlen((byte) 1);
		msg.setContent(new byte[] { 1 });
		cont = transToByte(msg);
		for (int i = 0; i < 6; i++) {
			Thread.sleep(1000L);
			System.out.println("send: " + msg);
			out.write(cont);
			in.read(buf);
			res = createfromByte(buf);
			System.out.println("recv :" + res);
		}

		s.close();
	}
	
	@Test
	public void clientSend() throws Exception {
		Socket s = new Socket();
		s.connect(new InetSocketAddress("localhost", 7000));
		OutputStream out = s.getOutputStream();
		InputStream in = s.getInputStream();

		byte[] buf = new byte[1024];
		ProtocolEntity msg = new ProtocolEntity();
		msg.setSide(ModelConstant.SIDE_CLIENT);
		msg.setCode(ModelConstant.CODE_BEAT);
		msg.setContent(null);
		msg.setContentlen((byte) 0);
		msg.setDestdeviceid((short) 0);
		msg.setSequenceid((short) 0);
		msg.setSrcdeviceid((short) 2);
		System.out.println("send: " + msg);
		byte[] cont = transToByte(msg);
		out.write(cont);
		in.read(buf);
		
		ProtocolEntity res = createfromByte(buf);
		System.out.println("recv :" + res);

		s.close();
	}

	private byte[] transToByte(ProtocolEntity msg) {
		/*
		 * byte[] out.writeByte(msg.getSide());
		 * out.writeShort(msg.getSrcdeviceid());
		 * out.writeShort(msg.getDestdeviceid()); out.writeByte(msg.getCode());
		 * out.writeShort(msg.getSequenceid());
		 * out.writeByte(msg.getContentlen()); if (msg.getContentlen() > 0) {
		 * out.writeBytes(msg.getContent()); }
		 * out.writeShort(msg.calcCheckSum());
		 */
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

	private ProtocolEntity createfromByte(byte[] buf) {
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
