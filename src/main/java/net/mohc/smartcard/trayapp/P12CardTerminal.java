package net.mohc.smartcard.trayapp;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Date;
import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardNotPresentException;
import javax.smartcardio.CardTerminal;

public class P12CardTerminal extends CardTerminal {
	public static final String TERMINAL_NAME = "MOHC PKCS12 File Terminal";
	private File location;

	public P12CardTerminal(File location) {
		this.location = location;
	}

	@Override
	public Card connect(String protocol) throws CardException {
		File[] files = getP12s();
		if (files.length == 0) throw new CardNotPresentException("No card in " + location.getAbsolutePath());
		if (files.length > 1) {
			File selectedFile = DummyCardChoiceDialog.showChoices(files);
			return new P12Card(selectedFile);
		}
		return new P12Card(files[0]);
	}

	@Override
	public String getName() {
		return TERMINAL_NAME;
	}

	@Override
	public boolean isCardPresent() throws CardException {
		return areThereP12s();
	}

	@Override
	public boolean waitForCardAbsent(long timeout) throws CardException {
		synchronized (location) {
			long expireTime = timeNow() + timeout;
			boolean isAbsent = !areThereP12s();
			while(timeNow() < expireTime && !isAbsent) {
				try {
					wait(250); //Check every 1/4s so we aren't hammering the file system
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				isAbsent = !areThereP12s();
			}
			return isAbsent;
		}
	}

	@Override
	public boolean waitForCardPresent(long timeout) throws CardException {
		synchronized (location) {
			long expireTime = timeNow() + timeout;
			boolean isPresent = areThereP12s();
			while(timeNow() < expireTime && !isPresent) {
				try {
					wait(250); //Check every 1/4s so we aren't hammering the file system
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				isPresent = areThereP12s();
			}
			return isPresent;
		}
	}

	private long timeNow() {
		return new Date().getTime();
	}
	
	private boolean areThereP12s() {
		return getP12s().length > 0;
	}

	private File[] getP12s () {
		if (location.exists() && location.isDirectory()) {
			FilenameFilter filter = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".p12");
				}
			};
			return location.listFiles(filter );
		}		
		return new File[0];
	}

}
