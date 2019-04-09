package net.mohc.smartcard.trayapp;

import java.io.File;

import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;

public class P12Card extends Card {
	private static final String INVOCATION_WAS_UNEXPECTED = "Invocation was unexpected";
	File p12File = null;

	public P12Card(File file) throws CardException {
		if (null == file) throw new CardException("P12 file not defined");
		if (!file.canRead()) throw new CardException("P12 file not accessible: " + file.getAbsolutePath());
		this.p12File = file;		
	}
	
	public File getP12File() {
		return p12File;
	}

	@Override
	public void beginExclusive() throws CardException {
		throw new UnsupportedOperationException(INVOCATION_WAS_UNEXPECTED);
	}

	@Override
	public void disconnect(boolean arg0) throws CardException {
		//Nothing to do
	}

	@Override
	public void endExclusive() throws CardException {
		throw new UnsupportedOperationException(INVOCATION_WAS_UNEXPECTED);
	}

	@Override
	public ATR getATR() {
		throw new UnsupportedOperationException(INVOCATION_WAS_UNEXPECTED);
	}

	@Override
	public CardChannel getBasicChannel() {
		throw new UnsupportedOperationException(INVOCATION_WAS_UNEXPECTED);
	}

	@Override
	public String getProtocol() {
		return "";
	}

	@Override
	public CardChannel openLogicalChannel() throws CardException {
		throw new UnsupportedOperationException(INVOCATION_WAS_UNEXPECTED);
	}

	@Override
	public byte[] transmitControlCommand(int arg0, byte[] arg1)	throws CardException {
		throw new UnsupportedOperationException(INVOCATION_WAS_UNEXPECTED);
	}
	
	@Override
	public String toString() {
		return p12File.getName();
	}

}
