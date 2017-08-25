package smartcard;

import java.io.File;
import java.security.GeneralSecurityException;
import java.security.Provider;
import java.security.Provider.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;

public class SmartCardController {
	private static final String STRING_TO_TEST_SIGNATURE = "A test string to test document signing.\n<tag/>";
	private boolean isInitialised = false;
	private String terminalStatus = "Not yet initialised";
	private File selectedLibraryFile;
  TerminalFactory factory;
  List<CardTerminal> terminals;
  CardTerminal selectedCardTerminal;
  Provider provider;
	private Card connectedCard;
	private boolean cardPresent;
	private String cardPresentStatus = "";
	private boolean cardConnected;
	private String cardConnectedStatus = "";
  private SmartCardKeyStore smartCardKeyStore;
	private boolean validCertificate;
	private String certificateStatus = "";
	
	public static SmartCardController getInstance() {
		return new SmartCardController();
	}

	public void setLibraryFile(File libraryFile) {
		this.selectedLibraryFile = libraryFile;
	}
	
	public void connectToCardAndFindKeys() {
		reset();
    factory = TerminalFactory.getDefault();
    try {
    	getProvider();
    	selecteATerminal();
    	isInitialised = true;
    	lookForACard();
    	if (!cardPresent) return;
    	connectToCard();
    	if (!cardConnected) return;
    	smartCardKeyStore = new SmartCardKeyStore(getPkcsLibraryFilename());
    	validCertificate = smartCardKeyStore.isValid();
    	certificateStatus = smartCardKeyStore.getStatus();
		} catch (CardException e) {
			terminals = new ArrayList<CardTerminal>();
			terminalStatus = "FAILED: CardException thrown - " + e.getMessage();
			return;
		} catch (SmartCardControllerException e) {
			terminals = new ArrayList<CardTerminal>();
			terminalStatus = "FAILED: " + e.getMessage();
			return;
		} catch (SmartCardException e) {
			certificateStatus = "FAILED: Smart Card Keystore Exception" + e.getMessage();
			return;
		}
	}

	private void reset() {
		if(cardConnected) {
			try {
				connectedCard.disconnect(false);
			} catch (CardException e) {
			}
		}
		cardConnectedStatus = "";
		cardConnected = false;
		cardPresentStatus = "";
		cardPresent = false;
		isInitialised = false;
		certificateStatus = "";
		validCertificate = false;
		if (null != smartCardKeyStore) smartCardKeyStore.reset();
	}

	private void selecteATerminal() throws CardException {
		List<CardTerminal> terminals = factory.terminals().list();
		if (null == terminals || terminals.size() == 0) {
			terminalStatus = "No terminals detected";
			return;
		}
		selectedCardTerminal = terminals.get(0);
		terminalStatus = "Selected first terminal";
	}

	private void getProvider() {
  	provider = factory.getProvider();
  	if (null == provider) {
  		throw new SmartCardControllerException("No provider");
  	}
  	System.out.println("Looking for services from provider:");
		Set<Service> serviceList = provider.getServices();
		for (Service service : serviceList) {
			System.out.println("Provider " + provider.getName() + " has a service type " + service.getType());
		}
	}

	public String getTerminalStatus() {
		return this.terminalStatus;
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
		return this.isInitialised;
	}
	
	public boolean isCertificateValid() {
		return this.validCertificate;
	}
	
	private void lookForACard() {
		try {
			cardPresent = selectedCardTerminal.isCardPresent();
			cardPresentStatus = cardPresent?"Found a card":"No card found";
		} catch (CardException e) {
			cardPresent = false;
			cardPresentStatus = "Can't determine if a card is present";
		}
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

	public String doTestSignature() {
		String signature = "";
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
		return signature;
	}
}
