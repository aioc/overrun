package com.ausinformatics.overrun.core;

public class BadProtocolException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7582027440676214725L;

	private String extra;

	public BadProtocolException(String extra) {
		this.extra = extra;
	}

	public String getExtraInfo() {
		return extra;
	}

}
