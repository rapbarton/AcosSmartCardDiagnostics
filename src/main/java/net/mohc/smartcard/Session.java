package net.mohc.smartcard;

public class Session {
	private boolean valid = false;
	private char[] pin = new char[0];
	private String sessionId = "";
	
	public boolean isValid() {
		return valid;
	}
	public void setValid(boolean valid) {
		this.valid = valid;
	}
	public char[] getPin() {
		return pin;
	}
	public void setPin(char[] pin) {
		this.pin = pin;
	}
	public String getSessionId() {
		return sessionId;
	}
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	
	
	
}
