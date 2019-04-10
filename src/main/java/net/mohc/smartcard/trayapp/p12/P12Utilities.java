package net.mohc.smartcard.trayapp.p12;

import java.io.File;
import java.security.GeneralSecurityException;
import net.mohc.smartcard.trayapp.SmartCardException;
import net.mohc.smartcard.trayapp.SmartCardKeyStore;

public class P12Utilities {
	private P12Utilities () {}

	/**
	 * Returns a signature using a p12 key store
	 * @param p12Filename the filename (and path) of the key store 
	 * @param password the password for the p12 store. Null or empty will force a GUI dialogue to request password
	 * @param document a string to sign
	 * @return the signed document
	 * @throws P12Exception there's plenty to go wrong, the message should explain the cause
	 */
	public static String sign(String p12Filename, char[] password, String document) throws P12Exception {
		try {
			String signature = "";
			File p12File = new File(p12Filename);
			SmartCardKeyStore scKeyStore = new SmartCardKeyStore(p12File, password);
			signature = scKeyStore.signDocument(document);
			return signature ;
		} catch (SmartCardException | GeneralSecurityException e) {
			throw new P12Exception("Failed signing because " + e.getMessage());
		}
	}
	
}
