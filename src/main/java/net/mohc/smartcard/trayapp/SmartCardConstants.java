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

}
