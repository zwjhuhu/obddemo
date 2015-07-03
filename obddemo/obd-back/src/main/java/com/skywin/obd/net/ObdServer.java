package com.skywin.obd.net;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Date;

public class ObdServer {

	private void start() throws InterruptedException {
		EventLoopGroup group = new NioEventLoopGroup();
		EventLoopGroup child = new NioEventLoopGroup();
		ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap.group(group, child).channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<Channel>() {

					@Override
					protected void initChannel(Channel ch) throws Exception {
						ch.pipeline().addLast(
								new LengthFieldPrepender(1),
								new LengthFieldBasedFrameDecoder(255, 0, 1, 0,
										1), new CustomCodec(),
								new ObdProtocolHandler());
					}
				});
		try {
			ChannelFuture f = bootstrap.bind(new InetSocketAddress(7000))
					.sync();
			System.out.println("obd server started!" + new Date());
			f.channel().closeFuture().sync();
		} finally {
			child.shutdownGracefully().syncUninterruptibly();
			group.shutdownGracefully().syncUninterruptibly();
		}

	}

	public static void main(String[] args) throws IOException,
			InterruptedException {

		new ObdServer().start();

	}
}
