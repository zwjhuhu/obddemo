package com.skywin.obdserver.thread;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.os.Handler;
import android.os.Message;

import com.skywin.obd.model.ModelConstant;
import com.skywin.obd.model.OnlineInfo;
import com.skywin.obd.protocol.ProtocolEntity;
import com.skywin.obd.util.CommonUtils;
import com.skywin.obd.util.ProtocolUtils;

public class DemoThread implements Runnable {

	private Socket socket;

	private String ip;

	private int port;

	private boolean runFlag = true;

	private Handler handler;

	private volatile int deviceid = 0;

	private List<ProtocolEntity> sendList;

	public DemoThread(String ip, int port, Handler handler) {
		this.ip = ip;
		this.port = port;
		this.handler = handler;
		socket = new Socket();

		sendList = Collections
				.synchronizedList(new ArrayList<ProtocolEntity>());
	}

	@Override
	public void run() {
		while (runFlag&&!connectServer()) {
			try {
				Thread.sleep(5000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// 发送获取在线列表
		ProtocolEntity msg = new ProtocolEntity();
		msg.setSide(ModelConstant.SIDE_SERVER);
		msg.setCode(ModelConstant.CODE_BEAT);
		msg.setContent(null);
		msg.setContentlen((byte) 0);
		msg.setDestdeviceid((short) 0);
		msg.setSequenceid((short) 0);
		msg.setSrcdeviceid((short) 0);
		sendList.add(0, msg);
		while (runFlag) {
			doDemoTask();
		}
		System.out.println("socket thread exit!");
	}

	public boolean connectServer() {
		boolean flag = true;
		try {
			socket.connect(new InetSocketAddress(ip, port), 30000);
			sendNotice("连接到" + ip, 0);
			System.out.println("connect to "+ip);
		} catch (Exception e) {
			System.out.println("socket connect error!" + e.getMessage());
			e.printStackTrace();
			flag = false;
			sendNotice("连接到" + ip + "失败", 1);
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
			if (sendList.isEmpty()) {
				Thread.sleep(1000);
				return;
			}
			InputStream in = socket.getInputStream();
			OutputStream out = socket.getOutputStream();
			ProtocolEntity send = sendList.get(0);
			byte[] buf = ProtocolUtils.transToByte(send);
			out.write(buf);
			buf = new byte[1024];
			int count = in.read(buf);
			if (count > 0) {
				ProtocolEntity recv = ProtocolUtils.createfromByte(buf);
				dealResponse(recv);
				sendList.remove(0);
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
					try{
						Thread.sleep(5000);
					}catch(Exception exx){
						//ignore;
					}
					connectServer();
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void dealResponse(ProtocolEntity msg) {
		if (msg.getCode() == ModelConstant.CODE_BEAT) {
			if (msg.getContentlen() > 0 && msg.getContent() != null
					&& msg.getContentlen() == msg.getContent().length) {
				byte[] blist = msg.getContent();
				int len = blist.length;
				List<OnlineInfo> infos = new ArrayList<OnlineInfo>();
				if (len % 2 > 0) {
					len--;
				}
				for (int i = 0; i < len;) {
					int hi = blist[i];
					i++;
					int low = blist[i];
					i++;

					OnlineInfo info = new OnlineInfo();
					info.deviceid = (hi & 0xFF) * 256 + (low & 0xFF);
					info.devicename = "远程终端" + info.deviceid;
					infos.add(info);
				}
				if (infos.size() > 0) {
					sendRefreshOnline(infos);
				}
			} else {
				sendRefreshOnline(null);
			}
		} else if (msg.getCode() == ModelConstant.CODE_CMD) {
			if (msg.getContentlen() > 0 && msg.getContent() != null
					&& msg.getContentlen() == msg.getContent().length) {
				byte[] content = msg.getContent();
				String hexstr = CommonUtils.toHexString(content);
				sendCmdResponse(hexstr);
			} else {
				sendCmdResponse("");
			}
		}
	}

	public void requestCmd(String req) {
		ProtocolEntity msg = new ProtocolEntity();
		msg.setSide(ModelConstant.SIDE_SERVER);
		msg.setCode(ModelConstant.CODE_CMD);
		byte[] content = CommonUtils.parseHexString(req);
		msg.setContent(content);
		msg.setContentlen((byte) content.length);
		msg.setDestdeviceid((short) deviceid);
		msg.setSequenceid((short) 0);
		msg.setSrcdeviceid((short) 0);
		sendList.add(msg);
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
		handler.sendMessage(msg);
	}

	private void sendRefreshOnline(List<OnlineInfo> infos) {
		Message msg = Message.obtain();
		msg.what = 1;
		msg.obj = infos;
		handler.sendMessage(msg);
	}

	private void sendCmdResponse(String res) {
		Message msg = Message.obtain();
		msg.what = 2;
		msg.obj = res;
		handler.sendMessage(msg);
	}

	public void requestOnine() {
		ProtocolEntity msg = new ProtocolEntity();
		msg.setSide(ModelConstant.SIDE_SERVER);
		msg.setCode(ModelConstant.CODE_BEAT);
		msg.setContent(null);
		msg.setContentlen((byte) 0);
		msg.setDestdeviceid((short) 0);
		msg.setSequenceid((short) 0);
		msg.setSrcdeviceid((short) 0);
		sendList.add(msg);
	}

}
