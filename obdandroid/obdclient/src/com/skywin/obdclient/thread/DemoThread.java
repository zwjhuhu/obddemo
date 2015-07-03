package com.skywin.obdclient.thread;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import android.os.Handler;
import android.os.Message;

import com.skywin.obd.model.ModelConstant;
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

	public DemoThread(String ip, int port, int deviceid,Handler handler) {
		this.ip = ip;
		this.port = port;
		this.handler = handler;
		this.deviceid = deviceid;
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
		ProtocolEntity beat = new ProtocolEntity();
		beat.setSide(ModelConstant.SIDE_CLIENT);
		beat.setCode(ModelConstant.CODE_BEAT);
		beat.setContent(null);
		beat.setContentlen((byte) 0);
		beat.setDestdeviceid((short) 0);
		beat.setSequenceid((short) 0);
		beat.setSrcdeviceid((short) deviceid);
		sendList.add(beat);
		while (runFlag) {
			doDemoTask(beat);
		}
		System.out.println("socket thread exit!");
	}

	public boolean connectServer() {
		boolean flag = true;
		try {
			socket.connect(new InetSocketAddress(ip, port), 30000);
			sendNotice("client连接到" + ip, 0);
			System.out.println("connect to "+ip);
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

	private void doDemoTask(ProtocolEntity beat) {
		try {
			if (sendList.isEmpty()) {
				Thread.sleep(3000);
				sendList.add(beat);
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
				dealResponse(recv,out);
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

	private void dealResponse(ProtocolEntity msg,OutputStream out) throws IOException {
		if (msg.getCode() == ModelConstant.CODE_BEAT) {
			//心跳的返回
		} else if (msg.getCode() == ModelConstant.CODE_CMD) {
			if (msg.getContentlen() > 0 && msg.getContent() != null
					&& msg.getContentlen() == msg.getContent().length) {
				byte[] content = msg.getContent();
				String hexstr = CommonUtils.toHexString(content);
				sendCmdRequest(hexstr);
				ProtocolEntity res = new ProtocolEntity();
				res.setSide(ModelConstant.SIDE_CLIENT);
				res.setDestdeviceid(msg.getSrcdeviceid());
				res.setSequenceid(msg.getSequenceid());
				res.setCode(ModelConstant.CODE_CMD);
				res.setSrcdeviceid((short)deviceid);
				res.setChecksum(res.calcCheckSum());
				//处理命令
				int contentlen = 2; 
				Random ran = new Random();
				byte[] cont = new byte[]{(byte) ran.nextInt(200),(byte) ran.nextInt(200)};
				hexstr = CommonUtils.toHexString(cont);
				res.setContent(cont);
				res.setContentlen((byte)contentlen);
				byte[] buf = ProtocolUtils.transToByte(res);
				out.write(buf);
				sendCmdResponse(hexstr);
			} else {
				sendCmdRequest("");
			}
		}
		sendList.remove(0);
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

	private void sendCmdRequest(String req) {
		Message msg = Message.obtain();
		msg.what = 1;
		msg.obj = req;
		handler.sendMessage(msg);
	}

	private void sendCmdResponse(String res) {
		Message msg = Message.obtain();
		msg.what = 2;
		msg.obj = res;
		handler.sendMessage(msg);
	}

}
