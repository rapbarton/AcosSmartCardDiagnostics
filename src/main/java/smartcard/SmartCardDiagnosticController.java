package smartcard;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.KeyStore.CallbackHandlerProtection;
import java.security.KeyStore.ProtectionParameter;
import java.security.Provider.Service;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;

import sun.security.x509.X509CertInfo;

public class SmartCardDiagnosticController {
	private static final String STRING_TO_TEST_SIGNATURE = "A test string to test document signing.\n<tag/>";
	private static final String DIGITAL_SIGNATURE_ALGORITHM_NAME = "SHA1withRSA";

	private File selectedLibraryFile;
  private String pin = "00000000";
	TerminalFactory factory;
  List<CardTerminal> terminals;
  CardTerminal selectedCardTerminal;
  Provider provider;
	private Card connectedCard;
	private KeyStore ks;
	private KeyStore.Builder builder;
	private PrivateKey currentPrivateKey;
	private X509Certificate currentX509Certificate;


	
	public static SmartCardDiagnosticController getInstance(File libraryFile, String pin) {
		return new SmartCardDiagnosticController(libraryFile, pin);
	}
	
	private SmartCardDiagnosticController(File libraryFile, String pin) {
		reset();
		this.selectedLibraryFile = libraryFile;
		this.pin = pin;
	}

	public void runDiagnostics() {
		factory = TerminalFactory.getDefault();
    try {
    	getProvider();
    	selecteATerminal();
    	lookForACard();
    	connectToCard();
    	readKeys();
    	determineWhetherCertificateIsValid();
    	signDocument(STRING_TO_TEST_SIGNATURE);
    	System.out.println("All tests ran OK");
		} catch (Exception e) {
			System.out.println("Diagnosis ended early because " + e.getMessage());
			return;
		}
	}

	private void getProvider() {
		System.out.println("Getting provider...");
  	provider = factory.getProvider();
  	if (null == provider) {
  		reportFatalProblemAndGiveUp("No provider");
  	}
  	System.out.println("Provider: " + provider.getName());
		Set<Service> serviceList = provider.getServices();
		for (Service service : serviceList) {
			System.out.println("Provider " + provider.getName() + " has a service type " + service.getType());
		}
		System.out.println("Provider found OK");
	}

	private void selecteATerminal() throws CardException {
		System.out.println("Getting terminal...");
		List<CardTerminal> terminals = factory.terminals().list();
		if (null == terminals || terminals.size() == 0) {
			reportFatalProblemAndGiveUp("No terminals detected");
		}
		selectedCardTerminal = terminals.get(0);
		System.out.println("Selected terminal \"" + selectedCardTerminal.getName() + "\"");
	}

	private void lookForACard() {
		System.out.println("Looking for a card...");
		try {
			if (!selectedCardTerminal.isCardPresent()) {
				reportFatalProblemAndGiveUp("No card inserted");
			}
		} catch (CardException e) {
			reportFatalProblemAndGiveUp("Card exception: " + e.getMessage()) ;
		}
		System.out.println("Found a card OK");
	}
	
	private void connectToCard() {
		System.out.println("Connecting...");
		try {
			connectedCard = selectedCardTerminal.connect("*");
		} catch (CardException e) {
			reportFatalProblemAndGiveUp("Can't connect to card: " + e.getMessage());
		}
		System.out.println("Connected OK");
	}

	private void readKeys() {
		System.out.println("Reading certificates...");
		String pkcs11ConfigSettings = "name = SmartCard1 " + "library = " + getPkcsLibraryFilename();
		try {
			byte[] pkcs11ConfigBytes = pkcs11ConfigSettings.getBytes();
			ByteArrayInputStream confStream = new ByteArrayInputStream(pkcs11ConfigBytes);
			Provider pkcs11Provider = new sun.security.pkcs11.SunPKCS11(confStream);
			Security.addProvider(pkcs11Provider);
			builder = KeyStore.Builder.newInstance("PKCS11", pkcs11Provider, createPinCallback());
			ks = builder.getKeyStore();
			ks.load(null, null);
			System.out.println("Keystore built and loaded");
			Enumeration<?> aliasesEnum = ks.aliases();
			if (aliasesEnum.hasMoreElements()) {
				String alias = (String) aliasesEnum.nextElement();
				PublicKey currentPublicKey = ks.getCertificate(alias).getPublicKey();
				System.out.println("Public key: " + currentPublicKey.toString());
				currentX509Certificate = (X509Certificate) ks.getCertificate(alias);
				currentPrivateKey = (PrivateKey) ks.getKey(alias, null);
				//X509Certificate[] currentX509CertificateChain = (X509Certificate[]) ks.getCertificateChain(alias);
			} else {
				reportFatalProblemAndGiveUp("The keystore is empty!");
			}
			if (aliasesEnum.hasMoreElements()) {
				System.out.println("WARNING: More than one certificate!!!");
			}
			System.out.println("Certificate Loaded CN:" + getCertificateCommonName());
		} catch (KeyStoreException kse) {
			reportFatalProblemAndGiveUp("KeyStoreException: " + kse.getMessage());
		} catch (NoSuchAlgorithmException e) {
			reportFatalProblemAndGiveUp("NoSuchAlgorithmException: " + e.getMessage());
		} catch (CertificateException e) {
			reportFatalProblemAndGiveUp("CertificateException: " + e.getMessage());
		} catch (IOException e) {
			reportFatalProblemAndGiveUp("IOException: " + e.getMessage());
		} catch (GeneralSecurityException e) {
			reportFatalProblemAndGiveUp("GeneralSecurityException: " + e.getMessage());
		} catch (ProviderException e) {
			if (e.getCause() != null) {
				reportFatalProblemAndGiveUp("ProviderException: " + e.getMessage() + " because " + e.getCause().getMessage());
			}
			reportFatalProblemAndGiveUp("ProviderException: " + e.getMessage());
		}
		System.out.println("Read certificates OK");
	}

	private void determineWhetherCertificateIsValid() {
		if (ks == null) {
			System.out.println("No certificate selected");
		}
		try {
			currentX509Certificate.checkValidity(new Date());
		} catch (CertificateExpiredException e) {
			System.out.println("Certificate status: expired");
		} catch (CertificateNotYetValidException e) {
			System.out.println("Certificate status: not yet valid");
		} catch (NullPointerException e) {
			System.out.println("Certificate status: no certificate Available");
		} catch (java.security.ProviderException pe) {
			System.out.println("Certificate status: Provider exception: " + pe.getMessage());
		}
	}

	public String signDocument(String document) {
		System.out.println("Signing document with private key...");
		String signature = "";
		if (null == currentPrivateKey) {
			reportFatalProblemAndGiveUp("No private key loaded");
		}
		byte[] documentToSign = document.getBytes();
		Signature signatureAlgorithm = null;

		try {
			signatureAlgorithm = Signature.getInstance(DIGITAL_SIGNATURE_ALGORITHM_NAME);
			System.out.println("Private key object: " + currentPrivateKey);
			signatureAlgorithm.initSign(currentPrivateKey);
			System.out.println("Signature object:" + signatureAlgorithm.toString());
			signatureAlgorithm.update(documentToSign); //Error here in Win10, dll version 4.5.2.0, PKCS11Exception CKR_FUNCTION_NOT_SUPPORTED
			byte[] digitalSignature = signatureAlgorithm.sign();
			signature = Base64.encodeBytes(digitalSignature);
			System.out.println("Document signed OK");
		} catch (NoSuchAlgorithmException e) {
			reportFatalProblemAndGiveUp("NoSuchAlgorithmException: " + e.toString());
		} catch (InvalidKeyException e) {
			reportFatalProblemAndGiveUp("InvalidKeyException: " + e.toString());
		} catch (SignatureException e) {
			reportFatalProblemAndGiveUp("SignatureException: " + e.toString());
		}	catch (java.security.ProviderException pe) {
			Throwable cause = pe.getCause();
			if (null != cause && cause instanceof sun.security.pkcs11.wrapper.PKCS11Exception) {
				sun.security.pkcs11.wrapper.PKCS11Exception pkCause = (sun.security.pkcs11.wrapper.PKCS11Exception) cause;
				reportFatalProblemAndGiveUp("ERROR: PKCS11Exception (Error Code " + pkCause.getErrorCode() + ") " + pkCause.getMessage());
			} else {
				reportFatalProblemAndGiveUp("ERROR: Provider exception, " + pe.getMessage());
			}
		}
		return signature;
	}
	

	
//----------------------------------------------------------------------------------------------------------------------------------------------------------------
//Helper methods	
//----------------------------------------------------------------------------------------------------------------------------------------------------------------
	
	private void reset() {
		if(null != connectedCard) {
			try {
				connectedCard.disconnect(false);
			} catch (Exception e) {
			}
		}
		selectedCardTerminal = null;
		connectedCard = null;
	  provider = null;
		ks = null;
		builder = null;
		currentPrivateKey = null;
		currentX509Certificate = null;
	}

	public String getPkcsLibraryFilename() {
		return selectedLibraryFile.getAbsolutePath();
	}
	
	private void reportFatalProblemAndGiveUp(String descriptionOfProblem) {
//		System.out.println("ERROR: " + descriptionOfProblem);
		throw new SmartCardException(descriptionOfProblem);
	}

	private ProtectionParameter createPinCallback() {
		CallbackHandlerProtection callbackHandlerProtection = new KeyStore.CallbackHandlerProtection(new CallbackHandler() {
			public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
				for (Callback callback : callbacks) {
					if (callback instanceof PasswordCallback) {
						PasswordCallback pwdCallback = (PasswordCallback) callback;
						pwdCallback.setPassword(pin.toCharArray());
					}
				}
			}
		});
		return callbackHandlerProtection;
	}
	
	public String getCertificateCommonName () {
		if (null != currentX509Certificate) {
			try {
				X509CertInfo tbsCert=new X509CertInfo(currentX509Certificate.getTBSCertificate());
				String dn = tbsCert.toString();
				return getValByAttributeTypeFromIssuerDN(dn,"CN=");
			} catch (CertificateEncodingException e) {
				e.printStackTrace();
			} catch (CertificateParsingException e) {
				e.printStackTrace();
			}
		}
		return "";
	}
	
  private String getValByAttributeTypeFromIssuerDN(String dn, String attributeType) {
      String[] dnSplits = dn.split(","); 
      for (String dnSplit : dnSplits) {
          if (dnSplit.contains(attributeType)) {
              String[] cnSplits = dnSplit.trim().split("=");
              if(cnSplits[1]!= null) {
                  return cnSplits[1].trim();
              }
          }
      }
      return "";
  }

}
