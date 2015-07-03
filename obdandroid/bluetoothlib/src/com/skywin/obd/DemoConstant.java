package com.skywin.obd;

public class DemoConstant {
	// Message types sent from the BluetoothDemoService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	
	public static final int MESSAGE_UI = 10;

	// Key names received from the BluetoothDemoService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";
}
