package net.mohc.smartcard.trayapp;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.security.GeneralSecurityException;
import java.security.Provider;
import java.security.Provider.Service;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;
import javax.swing.Icon;
import javax.swing.Timer;

import org.apache.log4j.Logger;

import net.mohc.smartcard.utils.ImageHelper;
import net.mohc.smartcard.trayapp.p12.P12Card;
import net.mohc.smartcard.trayapp.p12.P12CardTerminal;
import net.mohc.smartcard.trayapp.p12.P12CardTerminalFactory;
import net.mohc.smartcard.utils.Base64;

import static net.mohc.smartcard.trayapp.SmartCardConstants.*;

import sun.security.pkcs11.wrapper.PKCS11Exception;

public class SmartCardController {
	private static final String STRING_TO_TEST_SIGNATURE = "A test string to test document signing.\n<tag/>";
	private static final int CONTROL_LOOP_PERIOD = 1000;
	private static SmartCardController instance = null;
	private File selectedLibraryFile = null;
  private TerminalFactory factory;
  private CardTerminal selectedCardTerminal = null;
  private Provider provider;
	private Card connectedCard;
	private boolean cardPresent;
	private String cardPresentStatus = SMART_CARD_TBD;
	private String cardPresentStatusOld = SMART_CARD_TBD;
	private boolean cardConnected;
	private String cardConnectedStatus = "";
  private SmartCardKeyStore smartCardKeyStore;
	private boolean validCertificate;
	private String certificateStatus = "";
	
	private Status status;
	private Logger logger;
	private ImageHelper imageHelper;
	private String defaultCardTerminalName = "";
	
	private SmartCardController() {
		logger = Logger.getLogger(SmartCardController.class);
		imageHelper = new ImageHelper();
		setStatus(new Status());
	}

	public static SmartCardController getInstance() {
		if (null == instance) {
			instance = new SmartCardController();
		}
		return instance;
	}

	public void initialise() {
		factory = TerminalFactory.getDefault();
  	getProvider();
  	startControlLoop();
	}
		
	private void startControlLoop() {
		Timer controlLoop = new Timer(CONTROL_LOOP_PERIOD, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				controlActions();
				if (!cardPresentStatusOld.equals(cardPresentStatus)) {
					cardPresentStatusOld = cardPresentStatus;
					doActionWhenStatusChanges();
				}
			}
		});
		controlLoop.start();		
	}
	
	public void doActionWhenStatusChanges() {
		logger.info("Card state change");
	}

	private void controlActions() {
		try {
			switch (status.getCurrentStatus()) {
			case NOT_INITIALISED: 
			case NO_TERMINAL:
				monitorTerminals();
				break;
				
			case TERMINAL_FOUND:
			case CARD_PRESENT:
			case SESSION_ACTIVE:
				if (!isExpectedTerminalStillConnected()) {
					logger.info("Card reader unplugged");
					reset();
				} else {
					monitorTerminals();
					monitorCards();
				}
				break;
				
				default:
					logger.error("Unexpected status");
			}
		} catch (SmartCardApplicationException e) {
			logger.fatal(e.getMessage());
			shutdown();
		}
	}
	
	private void monitorTerminals() {
		CardTerminal proposed = findBestTerminalOrNull();
		if (proposed != null) {
			changeTerminal(proposed);
		} else if (!status.getCurrentTerminal().equals(TERMINAL_TYPE_NONE) 
				    && !status.getCurrentTerminal().equals(TERMINAL_TYPE_DUMMY)) {
			reset();
		} 
		if (status.getCurrentStatus() == NOT_INITIALISED) {
			reset();
		}
	}

	private void monitorCards() {
		if (null == selectedCardTerminal) return;
		try {
			cardPresent = selectedCardTerminal.isCardPresent();
			cardPresentStatus = cardPresent?SMART_CARD_INSERTED:SMART_CARD_NOT_FOUND;
			if (cardPresent) {
				if(!cardConnected) {
					if (isKeyStoreOpen()) {
						closeKeystore();
					}
					connectToCard();
					status.setCurrentStatus(CARD_PRESENT);
				}
			} else {
				if(cardConnected) {
					disconnectCard();
				}
				status.setCurrentStatus(TERMINAL_FOUND);
			}
		} catch (CardException e) {
			cardPresent = false;
			cardPresentStatus = SMART_CARD_ERROR;
		}		
	}
	
	private void selectAppropriateDll() {
		if (hasATerminal()) {
			if (status.isAcos()) {
				selectedLibraryFile = Configuration.getInstance().getDllForAcos();
			} else if (status.isDummy()) {
				selectedLibraryFile = null;
			} else {
				logger.error("No dll available for this type of terminal");
				selectedLibraryFile = null;
			}
		} else {
			selectedLibraryFile = null;
		}
		
	}

	private boolean hasATerminal() {
		return null != selectedCardTerminal;
	}

	public void setLibraryFile(File libraryFile) {
		this.selectedLibraryFile = libraryFile;
	}
	
	private void reset() {
		if(cardConnected) {
			disconnectCard();
		}
		cardConnectedStatus = "";
		cardConnected = false;
		cardPresentStatus = "";
		cardPresent = false;
		closeKeystore();
		selectedCardTerminal = null;
		status.setCurrentStatus(NO_TERMINAL);
		status.setCurrentTerminal(TERMINAL_TYPE_NONE);
	}

	private boolean isExpectedTerminalStillConnected () {
		if (selectedCardTerminal != null) {
			List<CardTerminal> terminals;
			try {
				terminals = getTerminalList(false);
				for (CardTerminal cardTerminal : terminals) {
					if (selectedCardTerminal.equals(cardTerminal)) {
						return true;
					}
				}
				return false;
			} catch (SmartCardApplicationException e) {
				return false;
			}			
		}
		return true;//Kinda expecting no terminal
	}
	
	public List<CardTerminal> findAvailableTerminals() {
		return getTerminalList(false);
	}
	
	private List<CardTerminal> getTerminalList(boolean throwExceptionIfServiceStopped) {
		ArrayList<CardTerminal> terminals = new ArrayList<>();
		try {
			List<CardTerminal> factoryList = factory.terminals().list();
			if (null != factoryList) {
				terminals.addAll(factoryList) ;
			}
		} catch (CardException e) {
			Throwable cause = e.getCause();
			if (null != cause) {
				String reason = cause.getMessage();
				if (reason.equals("SCARD_E_SERVICE_STOPPED")) {
					if (throwExceptionIfServiceStopped) {
						throw new SmartCardApplicationException("Known bug in library causes fatal problem when reader service stops where no card readers are plugged in");
					} else {
						logger.debug("Can't see any card readers because the service has stopped");
					}
				} else if (!reason.equals("SCARD_E_NO_READERS_AVAILABLE")) {
					logger.debug("Can't see any card readers because " + reason);
				}
			}
		}
		CardTerminal dummyTerminal = getDummyTerminal();
		if (null != dummyTerminal) {
			terminals.add(dummyTerminal);
		}			
		return terminals;
	}
	
	private CardTerminal findBestTerminalOrNull () {
		List<CardTerminal> terminals = getTerminalList(true);
		CardTerminal bestTerminal = null;

		if (terminals.isEmpty()) {
			return bestTerminal;
		}
		
		//Is there a default configured?
		bestTerminal = findDefaultTerminalFromListOrNull(terminals);
		if (null != bestTerminal) return bestTerminal;
		
		//First auto choice is ACOS for Windows
		bestTerminal = findACOSTerminalIfWindowsFromListOrNull(terminals);
		if (null != bestTerminal) return bestTerminal;
		
		//Second auto choice is dummy
		bestTerminal = findDummyTerminalFromListOrNull(terminals);
		if (null != bestTerminal) return bestTerminal;
		
		//Otherwise just pick first in list
		return terminals.get(0);
	}
	
	private CardTerminal findDefaultTerminalFromListOrNull(List<CardTerminal> terminals) {
		if (!defaultCardTerminalName.isEmpty()) {
			for (CardTerminal cardTerminal : terminals) {
				if (defaultCardTerminalName.equals(cardTerminal.getName())) {
					return cardTerminal;
				}
			}
		}
		return null;
	}
	
	private CardTerminal findACOSTerminalIfWindowsFromListOrNull(List<CardTerminal> terminals) {
		if (Configuration.getInstance().isWindows()) {
			for (CardTerminal cardTerminal : terminals) {
				if (isTerminalACOS(cardTerminal)) {
					return cardTerminal;
				}
			}
		}
		return null;
	}

	private CardTerminal findDummyTerminalFromListOrNull(List<CardTerminal> terminals) {
		for (CardTerminal cardTerminal : terminals) {
			if (isTerminalDummy(cardTerminal)) {
				return cardTerminal;
			}
		}
		return null;
	}

	private boolean isTerminalACOS(CardTerminal terminal) {
		if (null == terminal) return false;
		String name = terminal.getName();
		return name.contains("ACS CCID USB Reader") || name.contains("ACS ACR 38");
	}
	
	private boolean isTerminalDummy(CardTerminal terminal) {
		if (null == terminal) return false;
		String name = terminal.getName();
		return name.equals(P12CardTerminal.TERMINAL_NAME);
	}
	
	private boolean isTerminalFeitian(CardTerminal terminal) {
		return null != terminal && terminal.getName().contains("SCR301");
	}
	
	private void changeTerminal(CardTerminal terminal) {
		if (null == terminal) {
			selectedCardTerminal = null;
			status.setCurrentTerminal(TERMINAL_TYPE_NONE);
			selectAppropriateDll();
			status.setCurrentStatus(NO_TERMINAL);
		} else if (null == selectedCardTerminal || !selectedCardTerminal.getName().equals(terminal.getName())) {
			status.setCurrentStatus(NO_TERMINAL);
			selectedCardTerminal = terminal;
			if (isTerminalACOS(terminal)) {
				status.setCurrentTerminal(TERMINAL_TYPE_ACOS);
				logger.info("Found ACOS reader");
			} else if (isTerminalFeitian(terminal)) {
				status.setCurrentTerminal(TERMINAL_TYPE_FEITIAN);
				logger.info("Found Feitian reader");
			} else if (isTerminalDummy(terminal)) {
				status.setCurrentTerminal(TERMINAL_TYPE_DUMMY);
				logger.info("Found P12 directory reader");
			} else {
				status.setCurrentTerminal(TERMINAL_TYPE_OTHER);
				logger.info("Found unrecognised reader");
			}
			selectAppropriateDll();
			status.setCurrentStatus(TERMINAL_FOUND);
		} 		
	}

	private void getProvider() {
  	provider = factory.getProvider();
  	if (null == provider) {
  		throw new SmartCardControllerException("No provider");
  	}
  	logger.info("Looking for services from provider:");
		Set<Service> serviceList = provider.getServices();
		for (Service service : serviceList) {
			logger.info("Provider " + provider.getName() + " has a service type " + service.getType());
		}
	}

	public String getTerminalStatus() {
		return status.getStatusAsText();
	}

	public String getCardPresentStatus() {
		return this.cardPresentStatus;
	}

	public String getCardConnectedStatus() {
		return this.cardConnectedStatus;
	}

	public String getCertificateStatus() {
		return this.certificateStatus;
	}

	public String getProviderName() {
		if (null == provider) {
			return "";
		}
		return provider.getName();
	}
	
	public boolean isInitialised() {
		return status.getCurrentStatus() != NOT_INITIALISED;
	}
	
	public boolean isCertificateValid() {
		return this.validCertificate;
	}
	
	private void connectToCard() {
		try {
			connectedCard = selectedCardTerminal.connect("*");
			cardConnected = true;
			cardConnectedStatus = "Connected";
			status.setCurrentStatus(SESSION_ACTIVE);
		} catch (CardException e) {
			cardConnected = false;
			cardConnectedStatus = "Can't connect to card: " + e.getMessage();
		}
	}
	
	public String connectedCardInfo () {
		if (cardConnected) {
			return connectedCard.toString();
		} else {
			return "Couldn't read card";
		}
	}
	
	private void disconnectCard() {
		if (null != connectedCard) {
			try {
				connectedCard.disconnect(false);
			} catch (CardException e) {
				logger.warn("Had a problem disconnecting card: " + e.getMessage());
			}
		}
		cardConnected = false;
		cardConnectedStatus = "No connection";
		closeKeystore();
	}

	public String getPkcsLibraryFilename() {
		return selectedLibraryFile.getAbsolutePath();
	}
	
	public void openKeystore() {
		if (null == smartCardKeyStore) {
			try {
				if (connectedCard instanceof P12Card) {
					File p12File = ((P12Card)connectedCard).getP12File();
					smartCardKeyStore = new SmartCardKeyStore(p12File, null);
				} else {
			  	smartCardKeyStore = new SmartCardKeyStore(getPkcsLibraryFilename());
				}
		  	validCertificate = smartCardKeyStore.isValidAndSetStatus();
				certificateStatus = smartCardKeyStore.getStatus();
				status.setCurrentStatus(SESSION_ACTIVE);
			} catch (SmartCardException e) {
				validCertificate = false;
				certificateStatus = e.getMessage();
			}
		}
	}
	
	public void closeKeystore() {
		if (null != smartCardKeyStore) {
	  	smartCardKeyStore.reset();
	  	smartCardKeyStore = null;
		}
  	validCertificate = false;
  	certificateStatus = "";
  	if (status.getCurrentStatus() == SESSION_ACTIVE) {
  		status.setCurrentStatus(CARD_PRESENT);
  	}
	}
	
	public boolean isKeyStoreOpen () {
		if (null == smartCardKeyStore) return false;
		return smartCardKeyStore.isKeystoreLoaded();
	}

	public String doTestSignature() {
		String signature = "";
		if (null == smartCardKeyStore) {
			signature = "ERROR: No keystore is not open";
		} else {
			try {
				signature = smartCardKeyStore.signDocument(STRING_TO_TEST_SIGNATURE);
			} catch (GeneralSecurityException e) {
				signature = "ERROR: " + e.getMessage();
			}	catch (java.security.ProviderException pe) {
				Throwable cause = pe.getCause();
				if (cause instanceof sun.security.pkcs11.wrapper.PKCS11Exception) {
					sun.security.pkcs11.wrapper.PKCS11Exception pkCause = (sun.security.pkcs11.wrapper.PKCS11Exception) cause;
					signature = "ERROR: PKCS11Exception (Error Code " + pkCause.getErrorCode() + ") " + pkCause.getMessage();
				} else {
					signature = "ERROR: Provider exception, " + pe.getMessage();
				}
			}
		}
		return signature;
	}

	public String doSignatureInSession(String sessionID, String prescReg, String dataToSign) {
		String signature = "";
		if (null == smartCardKeyStore) {
			signature = "ERROR: Keystore is not open";
		} else if (!smartCardKeyStore.isSessionMatch(sessionID)) {
			signature = "ERROR: Keystore is not open for this session";
		} else if (!smartCardKeyStore.isGMCMatch(prescReg)) {
			signature = "ERROR: Keystore is not intended for this prescriber";
		} else {
			try {
				signature = smartCardKeyStore.signDocument(dataToSign);
			} catch (GeneralSecurityException e) {
				signature = "ERROR: " + e.getMessage();
			}	catch (java.security.ProviderException pe) {
				Throwable cause = pe.getCause();
				if (cause instanceof PKCS11Exception) {
					PKCS11Exception pkCause = (PKCS11Exception) cause;
					signature = "ERROR: PKCS11Exception (Error Code " + pkCause.getErrorCode() + ") " + pkCause.getMessage();
				} else {
					signature = "ERROR: Provider exception, " + pe.getMessage();
				}
			}
		}
		return signature;
	}

	public void shutdown() {
		logger.info("Shutdown Now");
		System.exit(0);             
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Icon getIconForTerminal(CardTerminal terminal) {
		String imageName = "genericReader_70";
		if (isTerminalACOS(terminal)) {
			imageName = "ACR38_70";
		} else if (isTerminalFeitian(terminal)) {
			imageName = "FT_R301_70";
		} else if (isTerminalDummy(terminal)) {
			imageName = "Certificate_70";
		}
		return imageHelper.getIcon(imageName);
	}

	public String getNameOfTerminal(CardTerminal terminal) {
		if (terminal == null) return "";
		String name = terminal.getName();
		if (isTerminalACOS(terminal)) {
			name = "ACOS ACR38";
		} else if (isTerminalFeitian(terminal)) {
			name = "Feitian R301";
		}
		return name;
	}

	public Icon getIconForSelectedTerminal() {
		return getIconForTerminal(selectedCardTerminal);
	}

	public String getNameOfSelectedTerminal() {
		return getNameOfTerminal(selectedCardTerminal);
	}

	public boolean isCardPresent() {
		int statusValue = status.getCurrentStatus();
		return statusValue == CARD_PRESENT || statusValue == SESSION_ACTIVE;
	}

	public String getSessionId() {
		return smartCardKeyStore.getSessionId();
	}

	public String getCardSessionStatus() {
		if (isKeyStoreOpen()) {
			if (smartCardKeyStore.hasSession()) {
				return "Logged in";
			} else {
				return "Error reading credentials";
			}
		} else {
			return "Not logged in";
		}
	}

	public Map<String,String> getCertificateDetails() {
		if (isKeyStoreOpen()) {
			return smartCardKeyStore.getCertificateDetail();
		} else {
			return new HashMap<>();
		}
	}

	public String getCertificateEncoded() {
		if (isKeyStoreOpen()) {
			X509Certificate[] chain = smartCardKeyStore.getCertificateChain();
			if (chain.length == 0) return "";
			String encodedCertificate;
			try {
				encodedCertificate = encodeX509CertChainToBase64(chain);
			} catch (CertificateException e) {
				encodedCertificate = "";
			}
			return encodedCertificate;
		}
		return "";
	}

/**
	 * @return Base64-encoded ASN.1 DER representation of given X.509
	 *         certification chain.
	 */
	private String encodeX509CertChainToBase64(X509Certificate[] aCertificationChain) throws CertificateException {
		if (null == aCertificationChain) {
			throw new CertificateException("No Certificate Chain");
		}
		X509Certificate firstCertificate = aCertificationChain[0];
		if (firstCertificate != null) {
			byte[] certPathEncoded = aCertificationChain[0].getEncoded();
			return Base64.encodeBytes(certPathEncoded);
		}
		throw new CertificateException("No certificates found");
	}

	public CardTerminal getSelectedCardTerminal() {
		return this.selectedCardTerminal;
	}
	
	public Card getConnectedCard () {
		return this.connectedCard;
	}
	
	private CardTerminal getDummyTerminal() {
		return P12CardTerminalFactory.getP12CardTerminal();
	}
	
	public void setDefaultCardTerminalName(String name) {
		defaultCardTerminalName = name;
	}

	public String getDefaultCardTerminalName() {
		return defaultCardTerminalName;
	}
}
