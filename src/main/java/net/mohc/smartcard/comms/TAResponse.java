package net.mohc.smartcard.comms;

import java.util.HashMap;
import java.util.Map;

public class TAResponse {
	public static final String PRIMARY_RESPONSE = "primary";
	public static final String SESSION = "sessionId";
	private String commandId;
	private boolean isOK;
	private String errorMessage;
	private Map<String,String> responses;
		
	public static TAResponse buildResponse (TACommand commandPacket) {
		return new TAResponse(commandPacket.getCommandId());
	}
	
	public TAResponse() {
		//For json construction
	}	
	
	private TAResponse(String commandId) {
		this.commandId = commandId;
		this.isOK = true;
		this.errorMessage = "";
		this.responses = new HashMap<String, String>();
	}

	public String getCommandId() {
		return commandId;
	}
	public TAResponse setCommandId(String commandId) {
		this.commandId = commandId;
		return this;
	}
	public boolean isOK() {
		return isOK;
	}
	public TAResponse setOK(boolean isOK) {
		this.isOK = isOK;
		return this;
	}
	public String getErrorMessage() {
		return errorMessage;
	}
	public TAResponse setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
		return this;
	}
	public Map<String, String> getResponses() {
		return responses;
	}
	public TAResponse addResponse(String key, String value) {
		this.responses.put(key, value);
		return this;
	}
	public TAResponse addResponse(String key, int value) {
		this.responses.put(key, ""+value);
		return this;
	}
	public TAResponse addPrimaryResponse(String value) {
		this.responses.put(PRIMARY_RESPONSE, value);
		return this;
	}

	public TAResponse setError(String errorMessage) {
		setErrorMessage(errorMessage);
		return setOK(false);
	}

	public void setResponses(Map<String, String> responses) {
		this.responses = responses;
	}

	public TAResponse addResponses(Map<String, String> map) {
		this.responses.putAll(map);
		return this;
	}
	
	
	
}
