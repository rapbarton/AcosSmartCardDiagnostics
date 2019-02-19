package net.mohc.smartcard.comms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class TACommand {
	public static final String TEST = "Test";
	public static final String LOGIN = "Connect";
	public static final String SESSION_CHECK = "Session";
	public static final String CARD_STATUS = "CardPresentStatus";
	public static final String CERT_OWNER = "CertificateStatus";
	public static final String CERT_DETAIL = "CertificateDetail";
	public static final String CERT_ENCODED = "CertificateEncoded";
	public static final String SIGN = "Sign";
	public static final String QUIT = "Quit";
		
	private String commandId;
	private String command;
	private List<String> arguments;
	private boolean isUsed = false;

	public TACommand () {
	}

	
	public TACommand (String command, List<String> arguments) {
		this.command = command;
		this.arguments = arguments;
		this.commandId = UUID.randomUUID().toString();
	}

	public TACommand (String command, String... arguments) {
		this.command = command;
		this.arguments = Arrays.asList(arguments);
		this.commandId = UUID.randomUUID().toString();
	}
	
	public TACommand (String command) {
		String[] parts = command.split(":");
		this.command = parts[0];
		this.arguments = new ArrayList<String>();
		if (parts.length > 1) {
			for (int i = 1; i < parts.length; i++) {
				arguments.add(parts[i].trim());
			}
		}
		this.commandId = UUID.randomUUID().toString();
	}
	
	public String getCommand() {
		return command;
	}

	public List<String> getArguments() {
		return arguments;
	}

	public String getCommandId() {
		return commandId;
	}

	public boolean isUsed() {
		return isUsed;
	}

	public void setUsed() {
		this.isUsed = true;
	}


	public void setCommandId(String commandId) {
		this.commandId = commandId;
	}


	public void setCommand(String command) {
		this.command = command;
	}


	public void setArguments(List<String> arguments) {
		this.arguments = arguments;
	}


	public void setUsed(boolean isUsed) {
		this.isUsed = isUsed;
	}
	

	
}
