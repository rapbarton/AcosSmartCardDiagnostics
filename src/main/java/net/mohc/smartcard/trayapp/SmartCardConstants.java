package net.mohc.smartcard.trayapp;

public interface SmartCardConstants {
	public static final int NOT_INITIALISED = 0;
	public static final int NO_TERMINAL = 1;
	public static final int TERMINAL_FOUND = 2;
	public static final int CARD_PRESENT = 3;
	public static final int SESSION_ACTIVE = 4;
		
	public static final String TERMINAL_TYPE_NONE = "None";
	public static final String TERMINAL_TYPE_ACOS = "ACOS";
	public static final String TERMINAL_TYPE_FEITIAN = "FEITIAN";
	public static final String TERMINAL_TYPE_OTHER = "Unrecognised";
	
	public static final String SMART_CARD_TBD = "Checking for card";
	public static final String SMART_CARD_INSERTED = "Found a card";
	public static final String SMART_CARD_NOT_FOUND = "No card found";
	public static final String SMART_CARD_ERROR = "Can't determine if a card is present";
	
	public static final String RESPONSE_CONNECTED = "Connected";
	public static final String RESPONSE_SUCCESS = "TEST OK";
	
	public static final String KEY_PRIMARY_RESPONSE = "primary";
	public static final String KEY_SESSION = "sessionId";
	public static final String KEY_CARD_INFO = "cardInfo";
	
	public static final String TRUE = "True";
	public static final String FALSE = "False";

	public static final String CERT_DETAIL_CN = "CN";
	public static final String CERT_DETAIL_REGISTRATION = "registration";
	public static final String CERT_DETAIL_PRESCRIBER_NAME = "prescriberName";
	public static final String CERT_DETAIL_VALID_TO = "validTo";
	public static final String CERT_DETAIL_VALID_FROM = "validFrom";
	public static final String CERT_IS_VALID = "isValid";
	public static final String CERT_STATUS = "certStatus";
	

}
