package com.skywin.obd.net.bio;

import java.net.Socket;

public class BindCmd {
	private Integer clientid;

	private String req;

	private Integer serverid;

	private Socket socket;

	public Integer getClientid() {
		return clientid;
	}

	public void setClientid(Integer clientid) {
		this.clientid = clientid;
	}

	public String getReq() {
		return req;
	}

	public void setReq(String req) {
		this.req = req;
	}

	public Integer getServerid() {
		return serverid;
	}

	public void setServerid(Integer serverid) {
		this.serverid = serverid;
	}

	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((clientid == null) ? 0 : clientid.hashCode());
		result = prime * result + ((req == null) ? 0 : req.hashCode());
		result = prime * result
				+ ((serverid == null) ? 0 : serverid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BindCmd other = (BindCmd) obj;
		if (clientid == null) {
			if (other.clientid != null)
				return false;
		} else if (!clientid.equals(other.clientid))
			return false;
		if (req == null) {
			if (other.req != null)
				return false;
		} else if (!req.equals(other.req))
			return false;
		if (serverid == null) {
			if (other.serverid != null)
				return false;
		} else if (!serverid.equals(other.serverid))
			return false;
		return true;
	}

}
