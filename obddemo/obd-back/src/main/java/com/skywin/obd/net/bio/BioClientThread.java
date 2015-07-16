package com.skywin.obd.net.bio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.skywin.obd.db.DbUtils;
import com.skywin.obd.model.CmdLog;
import com.skywin.obd.model.DeviceInfo;
import com.skywin.obd.model.ModelConstant;
import com.skywin.obd.model.ObdLog;
import com.skywin.obd.model.ProtocolEntity;
import com.skywin.obd.util.CommonUtils;
import com.skywin.obd.util.ProtocolUtils;

public class BioClientThread implements Runnable {

	private static Logger logger = LoggerFactory
			.getLogger(BioClientThread.class);

	private static final short DEMO_DEVICEID = 1;

	private Socket socket;

	private InputStream in;
	private OutputStream out;

	private boolean running = true;

	public BioClientThread(Socket socket) {
		this.socket = socket;
	}

	@Override
	public void run() {
		try {
			in = socket.getInputStream();
			out = socket.getOutputStream();
		} catch (Exception e) {
			running = false;
		}
		while (running) {
			byte[] buf = new byte[1024];
			try {
				int count = in.read(buf);
				if (count > 0) {
					dealInput(buf, count);
				} else if (count == -1) {
					destroy();
				}
			} catch (Exception e) {
				if (e instanceof InterruptedException) {
					running = false;
				} else if (e instanceof IOException) {
					destroy();
				}
				e.printStackTrace();
			}

		}
	}

	private void destroy() {
		logger.info("connection end!");
		if (socket != null) {
			try {
				socket.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				running = false;
				GlobalVariables.clearSocket(socket);
				socket = null;
			}
		}
	}

	private void dealInput(byte[] buf, int count) throws IOException {
		ProtocolEntity msg = decode(buf, count);
		if (msg != null) {
			logger.info("recv: " + msg);
			if (msg.getSide() == ModelConstant.SIDE_CLIENT) {
				dealClient(msg);
			} else if (msg.getSide() == ModelConstant.SIDE_SERVER) {
				dealServer(msg);
			}
		}
	}

	private void dealServer(ProtocolEntity msg) throws IOException {

		switch (msg.getCode()) {
		case ModelConstant.CODE_BEAT: {
			if (msg.getDestdeviceid() != 0) {
				logger.error("destdevice param error!");
				msg.setDestdeviceid((short) 0);
			} else {
				// 返回在线设备
				List<Integer> ids = GlobalVariables.findClientIds();
				byte[] content = new byte[0];
				if(!ids.isEmpty()){
					content = new byte[ids.size() * 2];
					int i = 0;
					for (int id : ids) {
						content[i] = (byte) (id / 256 & 0xFF);
						i++;
						content[i] = (byte) (id % 256 & 0xFF);
						i++;
					}
				}

				msg.setContent(content);
				msg.setContentlen((byte) content.length);
			}

		}
			break;
		case ModelConstant.CODE_CMD: {

			int destdeviceid = msg.getDestdeviceid() & 0xFFFF;
			int srcdeviceid = msg.getSrcdeviceid() & 0xFFFF;
			int contentlen = msg.getContentlen();
			if (contentlen > 0 && msg.getContent().length == contentlen) {

				if (destdeviceid == DEMO_DEVICEID) {
					// 模拟的数据
					int len = msg.getContentlen();
					String result = "41 00 00 00>";
					byte[] res = result.getBytes();
					len += res.length;
					len += 1;
					byte[] cont = new byte[len];
					System.arraycopy(msg.getContent(), 0, cont, 0,
							msg.getContent().length);
					cont[msg.getContent().length] = 0;
					System.arraycopy(res, 0, cont, msg.getContent().length + 1,
							res.length);
					msg.setContentlen((byte) len);
					msg.setContent(cont);

				} else {
					// 查看是否在线设备，不是就返回一个dummy
					Socket cs = GlobalVariables.findClientSocket(destdeviceid);
					if (cs == null) {
						// 模拟的数据
						int len = msg.getContentlen();
						String result = "41 00 00 00>";
						byte[] res = result.getBytes();
						len += res.length;
						len += 1;
						byte[] cont = new byte[len];
						System.arraycopy(msg.getContent(), 0, cont, 0,
								msg.getContent().length);
						cont[msg.getContent().length] = 0;
						System.arraycopy(res, 0, cont,
								msg.getContent().length + 1, res.length);
						msg.setContentlen((byte) len);
						msg.setContent(cont);
					} else {

						if (msg.getContentlen() > 0) {
							byte[] content = msg.getContent();
							String req = new String(content);

							// 转发到对应的客户端去
							try {
								msg.setSide(ModelConstant.SIDE_CLIENT);
								msg.setChecksum(msg.calcCheckSum());
								OutputStream sout = cs.getOutputStream();
								sout.write(ProtocolUtils.transToByte(msg));
								sout.flush();
								BindCmd cmd = new BindCmd();
								cmd.setClientid(destdeviceid);
								cmd.setReq(req);
								cmd.setServerid(srcdeviceid);
								cmd.setSocket(socket);
								GlobalVariables.addCmds(cmd);
								logger.info("transmit from server: " + srcdeviceid
										+ " to client: " + destdeviceid);
								logger.info("transmited "+msg);
							} catch (Exception e) {
								logger.error("send request to client error!"
										+ e.getMessage());
								e.printStackTrace();
							}

							CmdLog log = new CmdLog();
							log.setDeviceid(destdeviceid);
							log.setReq(req);
							log.setRes("");
							log.setState(ModelConstant.CMDSTATE_INIT);
							log.setUpdatetime(new Date());
							DbUtils.insert(log);
							return;
						}

					}

				}

			} else {
				// 命令不正确，回显
			}

		}
			break;

		default:
			break;
		}
		// 交换顺序
		short temp = msg.getSrcdeviceid();
		msg.setSrcdeviceid(msg.getDestdeviceid());
		msg.setDestdeviceid(temp);
		msg.setSide(ModelConstant.SIDE_SERVER);
		response(msg);
	}

	private void dealClient(ProtocolEntity msg) throws IOException {

		switch (msg.getCode()) {
		case ModelConstant.CODE_BEAT: {
			if (msg.getSrcdeviceid() == 0) {
				logger.error("srcdevice param error!");
				msg.setDestdeviceid((short) 0);
			} else {
				int deviceid = msg.getSrcdeviceid() & 0xFFFF;
				// 保存socket
				GlobalVariables.addClients(deviceid, socket);
				// 更新在线状态
				DeviceInfo info = DbUtils.findById(deviceid, DeviceInfo.class);
				if (info == null) {
					info = new DeviceInfo();
					info.setChecktime(new Date());
					info.setId(deviceid);
					info.setName("test" + System.currentTimeMillis());
					info.setState(ModelConstant.DEVICE_READY);
					DbUtils.insert(info);
				} else {
					info.setChecktime(new Date());
					info.setState(ModelConstant.DEVICE_READY);
					DbUtils.updateById(info);
				}
				// 查看是否有需要处理的命令
				// BindCmd cmd = GlobalVariables.findLastCmd(deviceid);
				// if (cmd != null) {
				// byte[] req = CommonUtils.parseByteString(cmd.getReq());
				// msg.setContent(req);
				// msg.setContentlen((byte) req.length);
				// msg.setCode(ModelConstant.CODE_CMD);
				// msg.setSequenceid((short) (1));
				// }
			}

		}
			break;
		case ModelConstant.CODE_CMD: {

			if (msg.getSrcdeviceid() == 0) {
				logger.error("srcdevice param error!");
				msg.setDestdeviceid((short) 0);
			} else {
				int deviceid = msg.getSrcdeviceid() & 0xFFFF;
				// 保存socket
				GlobalVariables.addClients(deviceid, socket);
				// 处理返回的命令

				if (msg.getContentlen() > 0) {
					byte[] content = msg.getContent();
					int i = 0;
					int len = content.length;

					for (i = 0; i < len; i++) {
						if ((content[i] & 0xFF) == 0) {
							break;
						}
					}
					if (i < len) {
						byte[] buf = new byte[i];
						System.arraycopy(content, 0, buf, 0, i);
						String req = new String(buf);
						BindCmd cmd = new BindCmd();
						cmd.setClientid(deviceid);
						cmd.setReq(req);
						cmd.setServerid(msg.getDestdeviceid() & 0xFFFF);
						Socket sc = GlobalVariables.findResponseSocket(cmd);
						if (sc != null) {
							msg.setSide(ModelConstant.SIDE_SERVER);
							msg.setChecksum(msg.calcCheckSum());
							try {
								OutputStream sout = sc.getOutputStream();
								sout.write(ProtocolUtils.transToByte(msg));
								sout.flush();
								GlobalVariables.removeLastCmd(cmd);
								logger.info("transmit cmd [" + req
										+ "]from client: " + deviceid
										+ " to server: " + msg.getDestdeviceid());
								logger.info("transmited "+msg);
							} catch (Exception e) {
								logger.error("send response to server error!"
										+ e.getMessage());
								e.printStackTrace();
							}

							CmdLog log = new CmdLog();
							log.setDeviceid(deviceid);
							log.setReq(req);
							log.setRes(CommonUtils.toByteString(msg
									.getContent()));
							log.setState(ModelConstant.CMDSTATE_READY);
							log.setUpdatetime(new Date());
							DbUtils.insert(log);

						}
					} else {
						// 有错
						logger.warn("no response socket find for client: "
								+ deviceid + "server: " + msg.getDestdeviceid());
					}
				}
				msg.setContent(null);
				msg.setContentlen((byte) 0);
				msg.setCode(ModelConstant.CODE_BEAT);
				msg.setSequenceid((short) 0);

				// 更新在线状态
				DeviceInfo info = DbUtils.findById(deviceid, DeviceInfo.class);
				if (info == null) {
					info = new DeviceInfo();
					info.setChecktime(new Date());
					info.setId(deviceid);
					info.setName("test" + System.currentTimeMillis());
					info.setState(ModelConstant.DEVICE_READY);
					DbUtils.insert(info);
				} else {
					info.setChecktime(new Date());
					info.setState(ModelConstant.DEVICE_READY);
					DbUtils.updateById(info);
				}

				// 查看是否有需要处理的命令
				// BindCmd cmd = GlobalVariables.findLastCmd(deviceid);
				// if (cmd != null) {
				// byte[] req = CommonUtils.parseByteString(cmd.getReq());
				// msg.setContent(req);
				// msg.setContentlen((byte) req.length);
				// msg.setCode(ModelConstant.CODE_CMD);
				// msg.setSequenceid((short) (msg.getSequenceid() & 0xFFFF +
				// 1));
				// } else {
				// msg.setContent(null);
				// msg.setContentlen((byte) 0);
				// msg.setCode(ModelConstant.CODE_BEAT);
				// msg.setSequenceid((short) 0);
				// }
			}

		}
			break;

		default:
			break;
		}
		// 交换顺序
		short temp = msg.getSrcdeviceid();
		msg.setSrcdeviceid(msg.getDestdeviceid());
		msg.setDestdeviceid(temp);
		msg.setSide(ModelConstant.SIDE_CLIENT);
		response(msg);

	}

	private void response(ProtocolEntity msg) throws IOException {
		logger.info("send: " + msg);
		ObdLog log = new ObdLog();
		log.setType((int) msg.getSide());
		log.setOptime(new Date());
		log.setContent(msg.getContent() == null ? "" : CommonUtils
				.toByteString(msg.getContent()));
		DbUtils.insert(log);
		out.write(encode(msg));
	}

	private byte[] encode(ProtocolEntity msg) {

		return ProtocolUtils.transToByte(msg);
	}

	private ProtocolEntity decode(byte[] buf, int count) {

		byte[] bytemsg = new byte[count];
		System.arraycopy(buf, 0, bytemsg, 0, count);
		return ProtocolUtils.createfromByte(bytemsg);
	}
}
