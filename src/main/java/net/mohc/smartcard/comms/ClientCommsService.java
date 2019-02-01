package net.mohc.smartcard.comms;

public class ClientCommsService {
	private static ClientCommsService instance  = null;
	//private String session = null;
	
	public static ClientCommsService getInstance() {
		if (null == instance) instance = new ClientCommsService();
		return instance;
	}
	
	private ClientCommsService () {
		//TODO
	}

	public boolean isAvailable() {
		// TODO Auto-generated method stub
		return false;
	}
	
	
	
}
