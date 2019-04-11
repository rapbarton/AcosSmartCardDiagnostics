package net.mohc.smartcard.manage;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class CCIDFeature {
	private final byte feature;
	private final Integer ioctl;

	public CCIDFeature(byte feature) {
		this.feature = feature;
		this.ioctl = null; // PPDU
	}

	public CCIDFeature(byte feature, Integer ioctl) {
		this.feature = feature;
		this.ioctl = ioctl;
	}

	public Integer getIoctl() {
		return this.ioctl;
	}

	public ResponseAPDU transmit(byte[] command, Card card, CardChannel cardChannel) throws CardException {
		if (this.ioctl == null) {
			// PPDU
			return cardChannel.transmit(new CommandAPDU(0xff, 0xc2, 0x01, this.feature, command));
		} else {
			byte[] result = card.transmitControlCommand(this.ioctl, command);
			return new ResponseAPDU(result);
		}
	}

	public byte[] transmitByteResponse(byte[] command, Card card, CardChannel cardChannel) throws CardException {
		if (this.ioctl == null) {
			// PPDU
			return cardChannel.transmit(new CommandAPDU(0xff, 0xc2, 0x01, this.feature, command)).getData();
		} else {
			return card.transmitControlCommand(this.ioctl, command);
		}
	}
}
