package com.skywin.obd.model;

import java.util.Date;

public class DeviceInfo extends DBModel<Integer> {
	private String name;
	private Integer state;

	private Date checktime;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getState() {
		return state;
	}

	public void setState(Integer state) {
		this.state = state;
	}

	public Date getChecktime() {
		return checktime;
	}

	public void setChecktime(Date checktime) {
		this.checktime = checktime;
	}
}
