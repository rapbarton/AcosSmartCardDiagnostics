package net.mohc.smartcard.comms;

import java.util.Map;

public interface TAResponseHandler {

	public void messageResponse(Map<String,String> responses);
	public void messageError(String errorMessage);
	
}
