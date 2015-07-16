package com.skywin.obd.net.bio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObdBioServer {

	private static Logger logger = LoggerFactory
			.getLogger(ObdBioServer.class);

	private void start() {
		ServerThread serverThread = new ServerThread();
		new Thread(serverThread).start();

	}

	public static void main(String[] args) {
		new ObdBioServer().start();
	}

	private static class ServerThread implements Runnable {

		private volatile boolean running = true;

		@Override
		public void run() {

			ServerSocket socket = null;
			try {
				socket = new ServerSocket();
				socket.bind(new InetSocketAddress(7000));
				System.out.println("server start!");
			} catch (IOException e) {
				e.printStackTrace();
				running = false;
			}

			while (running) {
				try {
					Socket s = socket.accept();
					logger.info("accept connection from "
							+ s.getInetAddress().getHostName());
					new Thread(new BioClientThread(s)).start();
				} catch (Exception e) {
					e.printStackTrace();
				}

			}

		}

		public void destroy() {
			running = false;
			Thread.currentThread().interrupt();
		}

	}
}
