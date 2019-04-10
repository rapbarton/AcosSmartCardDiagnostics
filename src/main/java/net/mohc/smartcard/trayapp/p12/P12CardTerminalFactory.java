package net.mohc.smartcard.trayapp.p12;

import java.io.File;

import javax.smartcardio.CardTerminal;

public class P12CardTerminalFactory {
	private static P12CardTerminal terminalInstance = null;

	public static CardTerminal getP12CardTerminal() {
		String home = System.getProperty("user.dir");
		String slash = System.getProperty("file.separator");
		String p12Files = home + slash + "p12";
		File file = new File(p12Files);
		if (file.exists() && file.isDirectory()) {
			if (null == terminalInstance) {
				terminalInstance = new P12CardTerminal(file);
			}
		} else {
			terminalInstance = null;
		}
		return terminalInstance;
	}

}
