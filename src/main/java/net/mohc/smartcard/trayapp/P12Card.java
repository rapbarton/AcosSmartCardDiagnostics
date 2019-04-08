package net.mohc.smartcard.trayapp;

import java.io.File;

import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;

public class P12Card extends Card {
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
	}

	@Override
	public void disconnect(boolean arg0) throws CardException {
	}

	@Override
	public void endExclusive() throws CardException {
	}

	@Override
	public ATR getATR() {
		return null;
	}

	@Override
	public CardChannel getBasicChannel() {
		return null;
	}

	@Override
	public String getProtocol() {
		return "";
	}

	@Override
	public CardChannel openLogicalChannel() throws CardException {
		return null;
	}

	@Override
	public byte[] transmitControlCommand(int arg0, byte[] arg1)
			throws CardException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String toString() {
		return p12File.getName();
	}

}
