package smartcard;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

public class Status implements SmartCardConstants {
	private int currentStatus = NOT_INITIALISED;
	private String currentTerminal = "None";

	private ArrayList<StatusChangeListener> statusChangeListeners;
	
	public Status () {
		statusChangeListeners = new ArrayList<StatusChangeListener>();
	}

	public int getCurrentStatus() {
		return currentStatus;
	}

	public void setCurrentStatus(int currentStatus) {
		if (this.currentStatus != currentStatus) {
			this.currentStatus = currentStatus;
			invokeListeners(getStatusAsText());
		}
	}
	
	public Image getCurrentStatusImage() {
		String imageName;
		switch (getCurrentStatus()) {
			case NOT_INITIALISED: 
			default: 
				imageName = "smart-card-inactive-24";
				break;
			case NO_TERMINAL:
				imageName = "smart-card-error-24";
				break;
			case TERMINAL_FOUND:
			case CARD_PRESENT:
				imageName = "smart-card-24";
				break;
		}		
		return Toolkit.getDefaultToolkit().getImage("src/main/resources/img/"+imageName+".png");
	}

	public String getStatusAsText() {
		switch (getCurrentStatus()) {
			case NOT_INITIALISED: return "Not initialised";
			case NO_TERMINAL: return "No terminal";
			case TERMINAL_FOUND: return "Terminal detected";
			case CARD_PRESENT: return "Card present";
			default: return "?";
		}		
	}

	public void setCurrentTerminal(String currentTerminal) {
		this.currentTerminal = currentTerminal;
	}

	public String getCurrentTerminal() {
		return currentTerminal;
	}

	public boolean isAcos() {
		return currentTerminal.equals(ACOS);
	}

	public void addStatusChangeListener(StatusChangeListener statusChangeListener ) {
		statusChangeListeners.add(statusChangeListener);
	}
	
	private void invokeListeners(String statusAsText) {
		final ActionEvent event = new ActionEvent(Status.this, ActionEvent.ACTION_PERFORMED, statusAsText);
		for (final StatusChangeListener statusChangeListener : statusChangeListeners) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					statusChangeListener.actionPerformed(event );
				}
			});
		}
	}
	
}
