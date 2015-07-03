package com.example;

import java.io.IOException;
import java.io.InputStream;

import eu.lighthouselabs.obd.commands.ObdCommand;

/**
 * Created with IntelliJ IDEA.
 * User: marshal
 * Date: 13-4-2
 * Time: 下午6:47
 * To change this template use File | Settings | File Templates.
 */
public class MyCommand extends ObdCommand {

    private String command;

    public MyCommand(String command) {
        super(command);
        this.command=command;
    }
    
    //get original result
    @Override
    protected void readResult(InputStream in) throws IOException {
    	
    	byte[] buf = new byte[1024];
		int len = in.read(buf);

		rawData = new String(buf,0,len);
    }
    
    @Override
    public String getResult() {
    	return rawData;
    }

    @Override
    public String getFormattedResult() {
        return "Custom command result: " + this.getResult();
    }

    @Override
    public String getName() {
        return this.command;
    }
}
