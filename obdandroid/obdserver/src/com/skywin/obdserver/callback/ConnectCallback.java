package com.skywin.obdserver.callback;

import com.skywin.obd.model.OnlineInfo;

public interface ConnectCallback {
	void connectCallback(OnlineInfo info);
}
