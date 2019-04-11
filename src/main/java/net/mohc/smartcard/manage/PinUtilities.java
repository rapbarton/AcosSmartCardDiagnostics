package net.mohc.smartcard.manage;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import net.mohc.smartcard.trayapp.SmartCardController;

import org.apache.log4j.Logger;

public class PinUtilities {
	public static final byte FEATURE_VERIFY_PIN_START_TAG = 0x01;
	public static final byte FEATURE_VERIFY_PIN_FINISH_TAG = 0x02;
	public static final byte FEATURE_MODIFY_PIN_START_TAG = 0x03;
	public static final byte FEATURE_MODIFY_PIN_FINISH_TAG = 0x04;
	public static final byte FEATURE_GET_KEY_PRESSED_TAG = 0x05;
	public static final byte FEATURE_VERIFY_PIN_DIRECT_TAG = 0x06;
	public static final byte FEATURE_MODIFY_PIN_DIRECT_TAG = 0x07;
	public static final byte FEATURE_EID_PIN_PAD_READER_TAG = (byte) 0x80;

	private Logger logger = Logger.getLogger(PinUtilities.class);
	private Set<String> ppduNames = new HashSet<>();
	
	public void doExperiment(SmartCardController smartCardController) {
		addPPDUName("ACS CCID USB");
		CardTerminal cardTerminal = smartCardController.getSelectedCardTerminal();
		CardChannel cardChannel ;
		Card card = smartCardController.getConnectedCard();
		if (null != cardTerminal && null != card) {
			cardChannel = card.getBasicChannel();
			Map<Byte, CCIDFeature> result = getCCIDFeatures(cardTerminal, cardChannel, card);
			logger.info(result.toString());
		}
		
		
	}
	
	
	

	
	private Map<Byte, CCIDFeature> getCCIDFeatures(CardTerminal cardTerminal, CardChannel cardChannel, Card card) {
		final boolean onMsWindows = (System.getProperty("os.name") != null
				&& System.getProperty("os.name").startsWith("Windows"));
		logger.info("CCID GET_FEATURE IOCTL...");
		int ioctl;
		String osName = System.getProperty("os.name");
		if (osName.startsWith("Windows")) {
			ioctl = (0x31 << 16 | (3400) << 2);
		} else {
			ioctl = 0x42000D48;
		}
		byte[] features;
		try {
			features = card.transmitControlCommand(ioctl, new byte[0]);
			Map<Byte, CCIDFeature> ccidFeatures = new HashMap<Byte, CCIDFeature>();
			int idx = 0;
			while (idx < features.length) {
				byte tag = features[idx];
				idx++;
				idx++;
				int featureIoctl = 0;
				for (int count = 0; count < 3; count++) {
					featureIoctl |= features[idx] & 0xff;
					idx++;
					featureIoctl <<= 8;
				}
				featureIoctl |= features[idx] & 0xff;
				idx++;
				ccidFeatures.put(tag, new CCIDFeature(tag, featureIoctl));
			}
			if (ccidFeatures.isEmpty() && onMsWindows && isPPDUCardTerminal(cardTerminal.getName())) {
				// Windows 10 work-around
				logger.info("trying PPDU interface...");
				ResponseAPDU responseAPDU = cardChannel
						.transmit(new CommandAPDU((byte) 0xff, (byte) 0xc2, 0x01, 0x00, new byte[] {}, 32));
				logger.info("PPDU response: " + Integer.toHexString(responseAPDU.getSW()));
				if (responseAPDU.getSW() == 0x9000) {
					features = responseAPDU.getData();
					for (byte feature : features) {
						ccidFeatures.put(feature, new CCIDFeature(feature));
						logger.info("PPDU feature: " + feature);
					}
					return ccidFeatures;
				} else {
					return Collections.emptyMap();
				}
			}
			return ccidFeatures;
		} catch (CardException e) {
			logger.info("GET_FEATURES IOCTL error: " + e.getMessage());
			try {
				if (!onMsWindows || !isPPDUCardTerminal(cardTerminal.getName())) {
					return Collections.emptyMap();
				}
				// try pseudo-APDU (PPDU) interface
				logger.info("trying PPDU interface...");
				ResponseAPDU responseAPDU = cardChannel
						.transmit(new CommandAPDU((byte) 0xff, (byte) 0xc2, 0x01, 0x00, new byte[] {}, 32));
				logger.info("PPDU response: " + Integer.toHexString(responseAPDU.getSW()));
				if (responseAPDU.getSW() == 0x9000) {
					Map<Byte, CCIDFeature> ccidFeatures = new HashMap<Byte, CCIDFeature>();
					features = responseAPDU.getData();
					for (byte feature : features) {
						ccidFeatures.put(feature, new CCIDFeature(feature));
						logger.info("PPDU feature: " + feature);
					}
					return ccidFeatures;
				} else {
					return Collections.emptyMap();
				}
			} catch (CardException e2) {
				logger.info("PPDU failed: " + e2.getMessage());
				Throwable cause = e2.getCause();
				if (null != cause) {
					logger.info("cause: " + cause.getMessage());
					StackTraceElement[] stackTrace = cause.getStackTrace();
					for (StackTraceElement stackTraceElement : stackTrace) {
						logger.info("at " + stackTraceElement.getClassName() + "."
								+ stackTraceElement.getMethodName() + ":" + stackTraceElement.getLineNumber());
					}
				}
				return Collections.emptyMap();
			}
		} finally {
			try {
				Thread.sleep(25);
			} catch (InterruptedException e) {
				// woops
			}
		}
	}
	
	public void addPPDUName(String name) {
		this.ppduNames.add(name.toLowerCase());
	}
	
	private boolean isPPDUCardTerminal(String name) {
		name = name.toLowerCase();
		for (String ppduName : this.ppduNames) {
			if (name.contains(ppduName)) {
				return true;
			}
		}
		return false;
	}
	
	
	
	public String tryTalkingWithCommand() {
		Card connectedCard = null;
		CardChannel channel = connectedCard.getBasicChannel();
		
		//connectedCard.transmitControlCommand(, arg1)
		byte[] baReadUID = new byte[5];
    //FF CA 00 00 00
    baReadUID = new byte[] { (byte) 0xFF, (byte) 0xCA, (byte) 0x00, (byte) 0x00, (byte) 0x00 };
    byte[] commandInByteArray = "Something".getBytes();
		CommandAPDU command = new CommandAPDU(commandInByteArray);
		try {
			ResponseAPDU reply = channel.transmit(command);
			String response = new String(reply.getBytes());
			return response;
		} catch (CardException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "fail";
		}
		//CommandAPDU cmd = new CommandAPDU(SELECT_PKI_APPLET_CMD);
	}


}
