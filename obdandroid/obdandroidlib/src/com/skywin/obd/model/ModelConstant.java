package com.skywin.obd.model;

public class ModelConstant {
	private ModelConstant(){
		
	}
	
	public static final byte SIDE_CLIENT = 0;
	
	public static final byte SIDE_SERVER = 1;
	
	public static final byte CODE_BEAT = 0;
	
	public static final byte CODE_CMD = 1;

	public static final int CMDSTATE_INIT = 0;
	
	public static final int CMDSTATE_READY = 1;

	public static final Integer DEVICE_READY = 1;
	
	public static final String DEF_SERVER_IP = "192.168.100.231";
	
}
