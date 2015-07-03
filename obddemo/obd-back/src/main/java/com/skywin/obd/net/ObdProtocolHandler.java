package com.skywin.obd.net;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.skywin.obd.db.DbUtils;
import com.skywin.obd.model.CmdLog;
import com.skywin.obd.model.DeviceInfo;
import com.skywin.obd.model.ModelConstant;
import com.skywin.obd.model.ObdLog;
import com.skywin.obd.model.ProtocolEntity;
import com.skywin.obd.util.CommonUtils;

@Sharable
public class ObdProtocolHandler extends
		SimpleChannelInboundHandler<ProtocolEntity> {

	private static final short DEMO_DEVICEID = 1;

	private static Random random = new Random();

	private static Logger logger = LoggerFactory
			.getLogger(ObdProtocolHandler.class);

	private ConcurrentHashMap<String, CmdLog> cmMap = new ConcurrentHashMap<String, CmdLog>();

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		cause.printStackTrace();
		ctx.close();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ProtocolEntity msg)
			throws Exception {

		logger.info("recv: " + msg);
		if (msg.getSide() == ModelConstant.SIDE_CLIENT) {
			dealClient(ctx, msg);
		} else if (msg.getSide() == ModelConstant.SIDE_SERVER) {
			dealServer(ctx, msg);
		}
		/*
		 * System.out.println("client init!"); String filename = new
		 * String(msg.getEntity(), "utf-8"); filename = filedir + File.separator
		 * + filename; File file = new File(filename); if (!file.isFile() ||
		 * file.length() == 0) { msg.setCmd("error"); } else { byte[] contents =
		 * new byte[(int) file.length()]; contMap.put(ctx, contents);
		 * loadFile(file, contents); msg.setChunckSize(1024);
		 * msg.setCmd("init"); msg.setCount(contents.length); msg.setOffset(0);
		 * } ctx.writeAndFlush(msg); } else if (msg.getCmd().equals("tran")) {
		 * byte[] contents = contMap.get(ctx); msg.setCmd("tran"); int offset =
		 * msg.getOffset(); int length = msg.getChunckSize(); if (length +
		 * offset >= contents.length) { length = contents.length - offset;
		 * msg.setCmd("over"); } byte[] buf = new byte[length];
		 * System.arraycopy(contents, offset, buf, 0, length);
		 * msg.setChunckSize(length); msg.setOffset(offset); msg.setEntity(buf);
		 * ctx.writeAndFlush(msg); } else if (msg.getCmd().equals("over")) {
		 * System.out.println("client over!"); ctx.close(); }
		 */
	}

	private void response(ChannelHandlerContext ctx, ProtocolEntity msg) {
		logger.info("send: " + msg);
		ObdLog log = new ObdLog();
		log.setType((int) msg.getSide());
		log.setOptime(new Date());
		log.setContent(msg.getContent() == null ? "" : CommonUtils
				.toHexString(msg.getContent()));
		DbUtils.insert(log);
		ctx.writeAndFlush(msg);
	}

	private void dealServer(ChannelHandlerContext ctx, ProtocolEntity msg) {

		switch (msg.getCode()) {
		case ModelConstant.CODE_BEAT: {
			if (msg.getDestdeviceid() != 0) {
				logger.error("destdevice param error!");
				msg.setDestdeviceid((short) 0);
			} else {
				// 找到数据库中在线的设备返回给主控端
				long now = System.currentTimeMillis();
				Date start = new Date(now - 15 * 1000L);
				Date end = new Date(now + 15 * 1000L);
				String sql = "select * from deviceinfo where state = 1 and checktime between '"
						+ CommonUtils.format(start)
						+ "' and '"
						+ CommonUtils.format(end) + "'";
				List<DeviceInfo> list = DbUtils
						.findBYSql(sql, DeviceInfo.class);
				// 格式化内容
				byte[] content = new byte[list.size() * 2];
				int i = 0;
				for (DeviceInfo info : list) {
					int id = info.getId();
					content[i] = (byte) (id / 256 & 0xFF);
					i++;
					content[i] = (byte) (id % 256 & 0xFF);
					i++;
				}
				msg.setContent(content);
				msg.setContentlen((byte) content.length);
			}

		}
			break;
		case ModelConstant.CODE_CMD: {

			int destdeviceid = msg.getDestdeviceid();
			int contentlen = msg.getContentlen();
			if (contentlen > 0 && msg.getContent().length == contentlen) {

				if (destdeviceid == DEMO_DEVICEID) {
					// 模拟的数据
					msg.setContentlen((byte) 2);
					byte[] content = new byte[] { (byte) random.nextInt(256),
							(byte) random.nextInt(256) };
					msg.setContent(content);

				} else {
					// 从数据库找对应的记录
					long now = System.currentTimeMillis();
					Date start = new Date(now - 15 * 1000L);
					Date end = new Date(now + 15 * 1000L);
					String reqStr = CommonUtils.toHexString(msg.getContent());
					List<CmdLog> list = DbUtils.findBYSql(
							"select * from cmdlog where deviceid = "
									+ destdeviceid
									+ " and state = 1 and updatetime between '"
									+ CommonUtils.format(start) + "' and '"
									+ CommonUtils.format(end) + "' and req = '"
									+ reqStr + "' order by id desc",
							CmdLog.class);
					// 没有合适的记录
					if (list.isEmpty()) {
						List<CmdLog> reqList = DbUtils.findBYSql(
								"select * from cmdlog where deviceid = "
										+ destdeviceid + " and req = '" + reqStr
										+ "' order by id desc", CmdLog.class);
						if (reqList.isEmpty()) {
							CmdLog log = new CmdLog();
							log.setDeviceid(destdeviceid);
							log.setReq(reqStr);
							log.setRes("");
							log.setState(ModelConstant.CMDSTATE_INIT);
							log.setUpdatetime(new Date());
							DbUtils.insert(log);
							msg.setContentlen((byte) 0);
							msg.setContent(null);
						} else {
							CmdLog cur = reqList.get(0);
							cur.setRes("");
							cur.setState(ModelConstant.CMDSTATE_INIT);
							cur.setUpdatetime(new Date());
							DbUtils.updateById(cur);
							DbUtils.execUpdate("delete from cmdlog where deviceid = "
									+ destdeviceid
									+ " and req = '"
									+ reqStr
									+ "' and id != " + cur.getId());

						}

					} else {
						CmdLog cur = list.get(0);
						String resStr = cur.getRes();
						if (resStr != null && !resStr.isEmpty()) {
							byte[] res = CommonUtils.parseHexString(resStr);
							msg.setContent(res);
							msg.setContentlen((byte) res.length);
						} else {
							msg.setContentlen((byte) 0);
							msg.setContent(null);
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
		response(ctx, msg);
	}

	private void dealClient(ChannelHandlerContext ctx, ProtocolEntity msg) {
		switch (msg.getCode()) {
		case ModelConstant.CODE_BEAT: {
			if (msg.getSrcdeviceid() == 0) {
				logger.error("srcdevice param error!");
				msg.setDestdeviceid((short) 0);
			} else {
				int deviceid = msg.getSrcdeviceid();
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
				List<CmdLog> list = DbUtils.findBYSql(
						"select * from cmdlog where deviceid = " + deviceid
								+ " and state = 0 order by id desc",
						CmdLog.class);
				msg.setCode(ModelConstant.CODE_BEAT);
				if (list.isEmpty()) {
					msg.setContent(null);
					msg.setContentlen((byte) 0);
				} else {
					CmdLog cur = list.get(0);
					String reqStr = cur.getReq();
					if (reqStr != null && !reqStr.isEmpty()) {
						byte[] req = CommonUtils.parseHexString(reqStr);
						msg.setContent(req);
						msg.setContentlen((byte) req.length);
						msg.setCode(ModelConstant.CODE_CMD);
						msg.setSequenceid((short) (1));
						cmMap.put(deviceid + "_" + msg.getSequenceid(), cur);
					} else {
						msg.setContentlen((byte) 0);
						msg.setContent(null);
					}
				}
			}

		}
			break;
		case ModelConstant.CODE_CMD: {

			if (msg.getSrcdeviceid() == 0) {
				logger.error("srcdevice param error!");
				msg.setDestdeviceid((short) 0);
			} else {
				int deviceid = msg.getSrcdeviceid();
				// 处理返回的命令
				CmdLog log = cmMap.get(deviceid + "_" + msg.getSequenceid());
				if (log != null) {
					byte[] content = msg.getContent();
					if (msg.getContentlen() > 0
							&& msg.getContentlen() == content.length) {
						String resStr = CommonUtils.toHexString(content);
						log.setState(ModelConstant.CMDSTATE_READY);
						log.setRes(resStr);
						log.setUpdatetime(new Date());
						DbUtils.updateById(log);
						cmMap.remove(deviceid + "_" + msg.getSequenceid());
					}
				}

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
				List<CmdLog> list = DbUtils.findBYSql(
						"select * from cmdlog where deviceid = " + deviceid
								+ " and state = 0 order by id desc",
						CmdLog.class);
				msg.setCode(ModelConstant.CODE_BEAT);
				if (list.isEmpty()) {
					msg.setContent(null);
					msg.setContentlen((byte) 0);
				} else {
					CmdLog cur = list.get(0);
					String reqStr = cur.getReq();
					if (reqStr != null && !reqStr.isEmpty()) {
						byte[] req = CommonUtils.parseHexString(reqStr);
						msg.setContent(req);
						msg.setContentlen((byte) req.length);
						msg.setCode(ModelConstant.CODE_CMD);
						msg.setSequenceid((short) (msg.getSequenceid() + 1));
						cmMap.put(deviceid + "_" + msg.getSequenceid(), cur);
					} else {
						msg.setContentlen((byte) 0);
						msg.setContent(null);
					}
				}
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
		response(ctx, msg);

	}

}
