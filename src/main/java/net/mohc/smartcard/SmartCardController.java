package net.mohc.smartcard;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.security.GeneralSecurityException;
import java.security.Provider;
import java.security.Provider.Service;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;
import javax.swing.Icon;
import javax.swing.Timer;

import org.apache.log4j.Logger;

public class SmartCardController implements SmartCardConstants {
	private static final String STRING_TO_TEST_SIGNATURE = "A test string to test document signing.\n<tag/>";
	private static final int CONTROL_LOOP_PERIOD = 1000;
	private static SmartCardController instance = null;
	private File selectedLibraryFile = null;
  TerminalFactory factory;
  List<CardTerminal> terminals;
  CardTerminal selectedCardTerminal = null;
  Provider provider;
	private Card connectedCard;
	private boolean cardPresent;
	private String cardPresentStatus = "";
	private boolean cardConnected;
	private String cardConnectedStatus = "";
  private SmartCardKeyStore smartCardKeyStore;
	private boolean validCertificate;
	private String certificateStatus = "";
	
	private Status status;
	private Logger logger;
	private ImageHelper imageHelper;
	
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
			}
		});
		controlLoop.start();		
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
		if (!status.getCurrentTerminal().equals(TERMINAL_TYPE_ACOS) && proposed != null) {
			changeTerminal(proposed);
		} else if (!status.getCurrentTerminal().equals(TERMINAL_TYPE_NONE) && proposed == null) {
			reset();
		} 
		if (status.getCurrentStatus() == NOT_INITIALISED) {
			reset();
		}
	}

	private void monitorCards() {
		if (null != selectedCardTerminal) {
			try {
				cardPresent = selectedCardTerminal.isCardPresent();
				cardPresentStatus = cardPresent?"Found a card":"No card found";
				if (cardPresent) {
					if(!cardConnected) {
						connectToCard();
						closeKeystore();
					}				
					status.setCurrentStatus(CARD_PRESENT);
				} else {
					if(cardConnected) {
						disconnectCard();
					}
					status.setCurrentStatus(TERMINAL_FOUND);
				}
			} catch (CardException e) {
				cardPresent = false;
				cardPresentStatus = "Can't determine if a card is present";
			}
		} 
	}
	
	private void selectAppropriateDll() {
		if (hasATerminal()) {
			if (status.isAcos()) {
				selectedLibraryFile = Configuration.getInstance().getDllForAcos();
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
				terminals = factory.terminals().list();
				for (CardTerminal cardTerminal : terminals) {
					if (selectedCardTerminal.equals(cardTerminal)) {
						return true;
					}
				}
				return false;
			} catch (CardException e) {
				return false;
			}			
		}
		return true;//Kinda expecting no terminal
	}
	
	private CardTerminal findBestTerminalOrNull () {
		List<CardTerminal> terminals;
		try {
			terminals = factory.terminals().list();
			if (null == terminals || terminals.size() == 0) {
				return null;
			}
			for (CardTerminal cardTerminal : terminals) {
				if (isTerminalACOS(cardTerminal)) {
					return cardTerminal;
				}
			}		
			return terminals.get(0);
		} catch (CardException e) {
			Throwable cause = e.getCause();
			if (null != cause) {
				String reason = cause.getMessage();
				if (reason.equals("SCARD_E_SERVICE_STOPPED")) {
					throw new SmartCardApplicationException("Known bug in library causes fatal problem when reader service stops where no card readers are plugged in");
				}
			}
			//Service has probably shut down
			return null;
		}
	}

	private boolean isTerminalACOS(CardTerminal terminal) {
		return null != terminal && terminal.getName().contains("ACS CCID USB Reader");
	}
	
	private boolean isTerminalFeitian(CardTerminal terminal) {
		return null != terminal && terminal.getName().contains("SCR301");
	}
	
	private void changeTerminal(CardTerminal terminal) {
		if (null == terminal) {
			selectedCardTerminal = null;
			status.setCurrentTerminal(TERMINAL_TYPE_NONE);
			selectAppropriateDll();
			status.setCurrentStatus(Status.NO_TERMINAL);
		} else if (null == selectedCardTerminal || !selectedCardTerminal.getName().equals(terminal.getName())) {
			selectedCardTerminal = terminal;
			if (isTerminalACOS(terminal)) {
				status.setCurrentTerminal(TERMINAL_TYPE_ACOS);
				logger.info("Found ACOS reader");
			} else if (isTerminalFeitian(terminal)) {
					status.setCurrentTerminal(TERMINAL_TYPE_FEITIAN);
					logger.info("Found Feitian reader");
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
		} catch (CardException e) {
			cardConnected = false;
			cardConnectedStatus = "Can't connect to card: " + e.getMessage();
		}
	}
	
	private void disconnectCard() {
		if (null != connectedCard) {
			try {
				connectedCard.disconnect(false);
			} catch (CardException e) {
			}
		}
		cardConnected = false;
		cardConnectedStatus = "No connection";
		closeKeystore();
	}

	public String getPkcsLibraryFilename() {
		return selectedLibraryFile.getAbsolutePath();
	}
	
	public Vector<File> findDlls() {
		String home = System.getProperty("user.dir");
		String slash = System.getProperty("file.separator");
		return findDlls(home+slash);
	}

	public Vector<File> findDlls(String baseLocation) {
		Vector<File> filenames = new Vector<File>();
		File locationOfDlls = new File(baseLocation);
		addDllsToArray(filenames, locationOfDlls, 0);
		return filenames;
	}

	private void addDllsToArray(Vector<File> array, File locationOfDlls, int safeCount) {
		if (array.size() >= 100 || safeCount > 20) return;//Ensure no loops - no deeper than 20 directories
		System.out.println("Searching in " + locationOfDlls);
		if (locationOfDlls.exists() && locationOfDlls.isDirectory()) {
			File[] allFiles = locationOfDlls.listFiles();
			if (null != allFiles && allFiles.length > 0) {
				for (File file : allFiles) {
					String filename = file.getName().toLowerCase();
					boolean isDll = filename.endsWith(".dll");
					boolean looksABitLikeAPKCSLib = filename.contains("pkcs11");
					if (file.isFile() && isDll & looksABitLikeAPKCSLib) {
						array.add(file);
					} else if (file.isDirectory()) {
						addDllsToArray(array, file, safeCount+1);
					}
				}
			}
		}		
	}

	public void openKeystore() {
		if (null == smartCardKeyStore) {
	  	smartCardKeyStore = new SmartCardKeyStore(getPkcsLibraryFilename());
	  	validCertificate = smartCardKeyStore.isValid();
	  	certificateStatus = smartCardKeyStore.getStatus();
		}
	}
	
	private void closeKeystore() {
		if (null != smartCardKeyStore) {
	  	smartCardKeyStore.reset();
	  	smartCardKeyStore = null;
		}
  	validCertificate = false;
  	certificateStatus = "";
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
				if (null != cause && cause instanceof sun.security.pkcs11.wrapper.PKCS11Exception) {
					sun.security.pkcs11.wrapper.PKCS11Exception pkCause = (sun.security.pkcs11.wrapper.PKCS11Exception) cause;
					signature = "ERROR: PKCS11Exception (Error Code " + pkCause.getErrorCode() + ") " + pkCause.getMessage();
				} else {
					signature = "ERROR: Provider exception, " + pe.getMessage();
				}
			}
		}
		return signature;
	}

	public String doSignatureInSession(String sessionID, String dataToSign) {
		String signature = "";
		if (null == smartCardKeyStore) {
			signature = "ERROR: Keystore is not open";
		} else if (!smartCardKeyStore.isSessionMatch(sessionID)) {
			signature = "ERROR: Keystore is not open for this session";
		} else {
			try {
				signature = smartCardKeyStore.signDocument(dataToSign);
			} catch (GeneralSecurityException e) {
				signature = "ERROR: " + e.getMessage();
			}	catch (java.security.ProviderException pe) {
				Throwable cause = pe.getCause();
				if (null != cause && cause instanceof sun.security.pkcs11.wrapper.PKCS11Exception) {
					sun.security.pkcs11.wrapper.PKCS11Exception pkCause = (sun.security.pkcs11.wrapper.PKCS11Exception) cause;
					signature = "ERROR: PKCS11Exception (Error Code " + pkCause.getErrorCode() + ") " + pkCause.getMessage();
				} else {
					signature = "ERROR: Provider exception, " + pe.getMessage();
				}
			}
		}
		return signature;
	}

	public void shutdown() {
		//TODO Shut down in an orderly fashion
		System.exit(0);             
		
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Icon getIconForSelectedTerminal() {
		String imageName = "genericReader_70";
		if (isTerminalACOS(selectedCardTerminal)) {
			imageName = "ACR38_70";
		} else if (isTerminalFeitian(selectedCardTerminal)) {
			imageName = "FT_R301_70";
		}
		return imageHelper.getIcon(imageName);
	}

	public String getNameOfSelectedTerminal() {
		if (selectedCardTerminal == null) return "";
		String name = selectedCardTerminal.getName();
		if (isTerminalACOS(selectedCardTerminal)) {
			name = "ACOS ACR38";
		} else if (isTerminalFeitian(selectedCardTerminal)) {
			name = "Feitian R301";
		}
		return name;
	}

	public boolean isCardPresent() {
		return status.getCurrentStatus() == Status.CARD_PRESENT;
	}

	public String getSessionId() {
		return smartCardKeyStore.getSessionId();
	}
}
