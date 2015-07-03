package com.skywin.obd.model;

import java.util.Date;

public class ObdLog extends DBModel<Integer> {
	private String content;
	private Integer type;
	private Date optime;

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public Date getOptime() {
		return optime;
	}

	public void setOptime(Date optime) {
		this.optime = optime;
	}

}
