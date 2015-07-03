package com.skywin.obd.net;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

import java.util.List;

import com.skywin.obd.model.ProtocolEntity;

public class CustomCodec extends ByteToMessageCodec<ProtocolEntity> {

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		cause.printStackTrace();
		ctx.close();
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, ProtocolEntity msg,
			ByteBuf out) throws Exception {

		out.writeByte(msg.getSide());
		out.writeShort(msg.getSrcdeviceid());
		out.writeShort(msg.getDestdeviceid());
		out.writeByte(msg.getCode());
		out.writeShort(msg.getSequenceid());
		out.writeByte(msg.getContentlen());
		if (msg.getContentlen() > 0) {
			out.writeBytes(msg.getContent());
		}
		out.writeShort(msg.calcCheckSum());
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in,
			List<Object> out) throws Exception {

		if (in.readableBytes() == 0)
			return;
		ProtocolEntity msg = new ProtocolEntity();

		msg.setSide(in.readByte());
		msg.setSrcdeviceid(in.readShort());
		msg.setDestdeviceid(in.readShort());
		msg.setCode(in.readByte());
		msg.setSequenceid(in.readShort());
		msg.setContentlen(in.readByte());
		if (msg.getContentlen() > 0) {
			byte[] content = new byte[msg.getContentlen()];
			in.readBytes(content);
			msg.setContent(content);
		}
		msg.setChecksum(in.readShort());
		out.add(msg);
	}

}
