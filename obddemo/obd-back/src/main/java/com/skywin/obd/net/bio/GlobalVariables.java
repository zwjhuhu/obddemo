package com.skywin.obd.net.bio;

import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GlobalVariables {

	private static final Map<Integer, Socket> clients = new HashMap<Integer, Socket>();

	private static final List<BindCmd> cmds = new LinkedList<BindCmd>();

	private static Logger logger = LoggerFactory
			.getLogger(GlobalVariables.class);

	private GlobalVariables() {

	}

	public synchronized static void addClients(Integer deviceid, Socket socket) {
		if (deviceid > 0 && socket != null) {
			clients.put(deviceid, socket);
		}
	}

	public synchronized static void addCmds(BindCmd cmd) {
		if (cmd != null && cmd.getSocket() != null) {
			cmds.add(cmd);
		}
	}

	public synchronized static Socket findClientSocket(Integer deviceid) {
		if (deviceid == null) {
			return null;
		}
		return clients.get(deviceid);
	}

	public synchronized static Socket findResponseSocket(BindCmd cmd) {
		if (cmd == null) {
			return null;
		}
		for (BindCmd bindcmd : cmds) {
			if (bindcmd.equals(cmd)) {
				return bindcmd.getSocket();
			}
		}
		return null;

	}

	public synchronized static BindCmd findLastCmd(int deviceid) {
		if (cmds == null) {
			return null;
		}

		for (BindCmd cmd : cmds) {
			if (cmd.getClientid().equals(deviceid)) {
				return cmd;
			}
		}
		return null;
	}

	public synchronized static void removeLastCmd(BindCmd cmd) {
		if (cmds == null || cmd == null) {
			return;
		}
		int len = cmds.size();
		int i = 0;
		for (i = 0; i < len; i++) {
			if (cmds.get(i).equals(cmd)) {
				break;
			}
		}
		if (i < len) {
			cmds.remove(i);
		}

	}

	public synchronized static void clearSocket(Socket socket) {
		if (!clients.isEmpty()) {
			List<Integer> reids = new ArrayList<Integer>();
			for (Integer id : clients.keySet()) {
				if (clients.get(id) == socket) {
					reids.add(id);
				}
			}
			if (!reids.isEmpty()) {
				for (Integer id : reids) {
					clients.remove(id);
					logger.info("removed client socket for deviceid " + id);
				}
			}
		}
		if (!cmds.isEmpty()) {
			List<Integer> reids = new ArrayList<Integer>();
			int len = cmds.size();
			for (int i = 0; i < len; i++) {
				if (cmds.get(i).getSocket() == socket) {
					reids.add(i);
				}
			}

			if (!reids.isEmpty()) {
				for (Integer id : reids) {
					BindCmd c = cmds.remove(id.intValue());
					logger.info("removed client socket for client "
							+ c.getClientid() + "server " + c.getServerid());
				}
			}
		}

	}

	public synchronized static List<Integer> findClientIds() {
		if (clients.isEmpty()) {
			return new ArrayList<Integer>(0);
		}
		List<Integer> list = new ArrayList<Integer>();
		for (Integer id : clients.keySet()) {
			list.add(id);
		}
		return list;
	}

}
