package com.skywin.obdclient.thread;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.os.Message;
import android.util.Log;

import com.github.pires.obd.reader.io.AbstractGatewayService;
import com.skywin.obd.model.ModelConstant;
import com.skywin.obd.protocol.ProtocolEntity;
import com.skywin.obd.util.ProtocolUtils;

public class SocketThread implements Runnable {

	private static final String TAG = "SocketThread";

	private Socket socket;

	private String ip;

	private int port;

	private boolean runFlag = true;

	private volatile int deviceid = 0;

	private List<ProtocolEntity> sendList;

	private AbstractGatewayService service;

	public SocketThread(String ip, int port, int deviceid,
			AbstractGatewayService service) {
		this.ip = ip;
		this.port = port;
		this.deviceid = deviceid;
		this.service = service;

		socket = new Socket();

		sendList = Collections
				.synchronizedList(new ArrayList<ProtocolEntity>());
	}

	@Override
	public void run() {
		while (runFlag && !connectServer()) {
			try {
				Thread.sleep(5000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// 发送心跳信号
		ProtocolEntity beat = createBeat();
		sendList.add(beat);
		while (runFlag) {
			doDemoTask();
		}
		System.out.println("socket thread exit!");
	}

	private ProtocolEntity createBeat() {
		ProtocolEntity beat = new ProtocolEntity();
		beat.setSide(ModelConstant.SIDE_CLIENT);
		beat.setCode(ModelConstant.CODE_BEAT);
		beat.setContent(null);
		beat.setContentlen((byte) 0);
		beat.setDestdeviceid((short) 0);
		beat.setSequenceid((short) 0);
		beat.setSrcdeviceid((short) deviceid);
		return beat;
	}

	public void sendMsg(String req, String result, short sequenceid,short serverid)
			throws UnsupportedEncodingException {
		ProtocolEntity msg = new ProtocolEntity();
		msg.setSide(ModelConstant.SIDE_CLIENT);
		msg.setCode(ModelConstant.CODE_CMD);
		msg.setDestdeviceid(serverid);
		msg.setSequenceid(sequenceid);
		msg.setSrcdeviceid((short) deviceid);
		byte[] buf1 = req.getBytes("utf-8");
		byte[] buf2 = result.getBytes("utf-8");
		int len = buf1.length + buf2.length + 1;
		byte[] buf = new byte[len];
		int i = 0;
		for (i = 0; i < buf1.length; i++) {
			buf[i] = buf1[i];
		}
		buf[i] = 0;
		for (i = 0; i < buf2.length; i++) {
			buf[i + buf1.length + 1] = buf2[i];
		}
		msg.setContent(buf);
		msg.setContentlen((byte) len);

		msg.setChecksum(msg.calcCheckSum());
		sendList.add(msg);
	}

	public boolean connectServer() {
		boolean flag = true;
		try {
			socket.connect(new InetSocketAddress(ip, port), 30000);
			sendNotice("client连接到" + ip, 0);
			System.out.println("connect to " + ip);
		} catch (Exception e) {
			System.out.println("socket connect error!" + e.getMessage());
			e.printStackTrace();
			flag = false;
			sendNotice("client连接到" + ip + "失败", 1);
			try {
				if (socket != null) {
					socket.close();
				}
			} catch (Exception ex) {
				e.printStackTrace();
			} finally {
				socket = new Socket();
			}
		}
		return flag;
	}

	private void doDemoTask() {
		try {
			InputStream in = socket.getInputStream();
			OutputStream out = socket.getOutputStream();
			byte[] buf = null;
			if (sendList.isEmpty()) {
				
			}else{
				ProtocolEntity send = sendList.get(0);
				buf = ProtocolUtils.transToByte(send);
				out.write(buf);
				Log.i(TAG,"write msg "+send);
				sendList.remove(0);
			}
			buf = new byte[1024];
			int count = in.read(buf);
			if (count > 0) {
				ProtocolEntity recv = ProtocolUtils.createfromByte(buf);
				dealResponse(recv, out);
			}
		} catch (IOException e) {
			e.printStackTrace();
			try {
				if (socket != null) {
					socket.close();
				}
			} catch (Exception ex) {
				e.printStackTrace();
			} finally {
				if (runFlag) {
					socket = new Socket();
					try {
						Thread.sleep(5000);
					} catch (Exception exx) {
						// ignore;
					}
					connectServer();
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void dealResponse(ProtocolEntity msg, OutputStream out)
			throws IOException {
		if (msg.getCode() == ModelConstant.CODE_BEAT) {
			// 心跳的返回
		} else if (msg.getCode() == ModelConstant.CODE_CMD) {
			if (msg.getContentlen() > 0 && msg.getContent() != null
					&& msg.getContentlen() == msg.getContent().length) {

				// 处理命令
				String req = new String(msg.getContent());
				String key = req.substring(0,req.length()-1);
				String result = service.readMsg(key);
				if (result == null || result.isEmpty()) {
					result="41 00 00 00>";
				}
//				Log.i(TAG, "read req "+key+"len: "+key.length());
				sendMsg(req, result, (short) (msg.getSequenceid() &0xFFFF + 1),msg.getSrcdeviceid());
				/*
				 * int contentlen = 2; Random ran = new Random(); byte[] cont =
				 * new byte[] { (byte) ran.nextInt(200), (byte) ran.nextInt(200)
				 * }; hexstr = CommonUtils.toHexString(cont);
				 * res.setContent(cont); res.setContentlen((byte) contentlen);
				 * byte[] buf = ProtocolUtils.transToByte(res); out.write(buf);
				 * sendCmdResponse(hexstr);
				 */
			} else {
				// sendCmdRequest("");
			}
		}
	}

	public void destroy() {
		runFlag = false;
		if (socket != null) {
			try {
				socket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void connectToDevice(int deviceid) {
		this.deviceid = deviceid;
	}

	private void sendNotice(String message, int code) {
		Message msg = Message.obtain();
		msg.what = 0;
		msg.obj = message;
		msg.arg1 = code;
		// handler.sendMessage(msg);
	}

	private void sendCmdRequest(String req) {
		Message msg = Message.obtain();
		msg.what = 1;
		msg.obj = req;
		// handler.sendMessage(msg);
	}

	private void sendCmdResponse(String res) {
		Message msg = Message.obtain();
		msg.what = 2;
		msg.obj = res;
		// handler.sendMessage(msg);
	}

}
