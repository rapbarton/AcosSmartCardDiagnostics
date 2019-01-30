package net.mohc.cardcomms;

public class ClientCommsService {
	private ClientCommsService instance  = null;
	private String session = null;
	
	public ClientCommsService getInstance() {
		if (null == instance) instance = new ClientCommsService();
		return instance;
	}
	
	private ClientCommsService () {
		
	}
	
	
	
}
