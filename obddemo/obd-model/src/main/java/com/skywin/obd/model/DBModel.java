package com.skywin.obd.model;

public abstract class DBModel<PK> {
	private PK id;

	public PK getId() {
		return id;
	}

	public void setId(PK id) {
		this.id = id;
	}

}
