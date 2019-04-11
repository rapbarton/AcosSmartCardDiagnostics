package net.mohc.smartcard.comms;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

public class RemoteConsoleConnectionMonitor extends Timer {
	private static final long serialVersionUID = -3678451853240159247L;

	private RemoteConsoleConnectionMonitor(int delay, ActionListener listener) {
		super(delay, listener);
		this.setRepeats(true);
		this.setCoalesce(true);
	}
	
	public static RemoteConsoleConnectionMonitor startMonitoring(final RemoteConsoleFrame parent) {
		RemoteConsoleConnectionMonitor instance = new RemoteConsoleConnectionMonitor(250, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				parent.doCheck();
			}
		});
		instance.start();
		return instance;
	}

}

