package net.mohc.smartcard;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.CallbackHandlerProtection;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.UUID;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import org.apache.log4j.Logger;

import net.mohc.utils.Base64;
import sun.security.pkcs11.SunPKCS11;
import sun.security.x509.X509CertInfo;

public class SmartCardKeyStore {
	private static final String MYSQL_DATE_FORMAT = "yyyy-MM-dd";
	private KeyStore ks;
	private KeyStore.Builder builder;
	private PrivateKey currentPrivateKey;
	@SuppressWarnings("unused")
	private PublicKey currentPublicKey;
	private X509Certificate currentX509Certificate;
	@SuppressWarnings("unused")
	private X509Certificate[] currentX509CertificateChain;

	private String status = "Not initialised";
	private String certificateCN = "";
	private static final String DIGITAL_SIGNATURE_ALGORITHM_NAME = "SHA1withRSA";
	Provider pkcs11Provider = null;
	private boolean loaded;
	private Session session;
	private Logger logger;

	public SmartCardKeyStore(String pkcs11LibraryFile) {
		logger = Logger.getLogger(this.getClass());
		session = new Session();
		String pkcs11ConfigSettings = "name = SmartCard1 " + "library = " + pkcs11LibraryFile;
		loaded = false;
		try {
			byte[] pkcs11ConfigBytes = pkcs11ConfigSettings.getBytes();
			ByteArrayInputStream confStream = new ByteArrayInputStream(pkcs11ConfigBytes);
			pkcs11Provider = new sun.security.pkcs11.SunPKCS11(confStream);
			Security.addProvider(pkcs11Provider);
			ProtectionParameter pinDialogue = createPinNumberDialog(); 
			builder = KeyStore.Builder.newInstance("PKCS11", pkcs11Provider, pinDialogue);
			ks = builder.getKeyStore();
			if (!session.isValid()) {
				logger.warn("Original keystore returned");
				((SunPKCS11) pkcs11Provider ).logout();
				pkcs11Provider.clear();
				Security.removeProvider(pkcs11Provider.getName());
				confStream = new ByteArrayInputStream(pkcs11ConfigBytes);
				pkcs11Provider = new sun.security.pkcs11.SunPKCS11(confStream);
				Security.addProvider(pkcs11Provider);
				builder = KeyStore.Builder.newInstance("PKCS11", pkcs11Provider, pinDialogue);
				ks = builder.getKeyStore();
				if (!session.isValid()) {
					reportFatalProblemAndGiveUp("Can't log out of stale session");
				}
			}
			ks.load(null, null);
			loaded = true;
			loadPrivateKeyAndCertChainWithChoice();
			status = "Card OK";
			certificateCN = getCertificateCommonName();
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
	}
	
	public void loadPrivateKeyAndCertChain() throws GeneralSecurityException {
		Enumeration<?> aliasesEnum = ks.aliases();
		String alias;
		if (aliasesEnum.hasMoreElements()) {
			alias = (String) aliasesEnum.nextElement();
			currentPublicKey = ks.getCertificate(alias).getPublicKey();
			currentX509Certificate = (X509Certificate) ks.getCertificate(alias);
			currentPrivateKey = (PrivateKey) ks.getKey(alias, null);
			currentX509CertificateChain = (X509Certificate[]) ks.getCertificateChain(alias);
			
		} else {
			throw new KeyStoreException("The keystore is empty!");
		}
		if (aliasesEnum.hasMoreElements()) {
			StringBuilder list = new StringBuilder(alias);
			while (aliasesEnum.hasMoreElements()) {
				alias = (String) aliasesEnum.nextElement();
				list.append(",");
				list.append(alias);
			}
			logger.warn("More than one certificate! (" + list + ")");
		}
	}
	
	public void loadPrivateKeyAndCertChainWithChoice() throws GeneralSecurityException {
		Enumeration<?> aliasesEnum = ks.aliases();
		ArrayList<String> aliases = new ArrayList<>();
		while (aliasesEnum.hasMoreElements()) {
			aliases.add((String) aliasesEnum.nextElement());
		}
		if (aliases.isEmpty()) {
			throw new KeyStoreException("The keystore is empty!");
		}
		String alias;
		if (aliases.size() == 1) {
			alias = aliases.get(0);
		} else {
			logger.warn("More than one certificate!");
			alias = chooseAlias(aliases);
		}
		currentPublicKey = ks.getCertificate(alias).getPublicKey();
		currentX509Certificate = (X509Certificate) ks.getCertificate(alias);
		currentPrivateKey = (PrivateKey) ks.getKey(alias, null);
		currentX509CertificateChain = (X509Certificate[]) ks.getCertificateChain(alias);
	}
	
	private String chooseAlias(ArrayList<String> aliases) {
		return CertChoiceDialog.showChoices(aliases);
	}

	private void reportFatalProblemAndGiveUp(String descriptionOfProblem) {
		logger.error(descriptionOfProblem);
		throw new SmartCardException("Card Error (" + descriptionOfProblem + ")");
	}

	/**
	 * Is certificate valid in n days time
	 * @param n number of days time
	 * @return
	 */
	public boolean isValid (int daysTime) {
		if (ks == null) {
			return false;
		}
		try {
			Date d = new Date();
			if (daysTime > 0) {
				d = addDays(d, daysTime);
			}
			currentX509Certificate.checkValidity(d);
		} catch (CertificateExpiredException e) {
			return false;
		} catch (CertificateNotYetValidException e) {
			return false;
		} catch (NullPointerException e) {
			return false;
		}
		return true;
	}
	
	private static Date addDays(Date start, int days) {
		Calendar cRef = Calendar.getInstance();
		cRef.setTime(start);
		cRef.add(Calendar.DAY_OF_MONTH, days);
		return cRef.getTime();
	}

	public Date getExpiryDate () {
		if (null!=currentX509Certificate) {
			return currentX509Certificate.getNotAfter();
		} else {
			Date aDateThatWontCauseAnExpiryError = addDays(new Date(), 1000);
			return aDateThatWontCauseAnExpiryError;
		}
	}
	
	/**
	 * Checks valid and sets status string
	 * @return
	 */
	public boolean isValid () {
		if (ks == null) {
			return false;
		}
		try {
			currentX509Certificate.checkValidity(new Date());
		} catch (CertificateExpiredException e) {
			status = "Card expired";
			return false;
		} catch (CertificateNotYetValidException e) {
			status = "Card not yet valid";
			return false;
		} catch (NullPointerException e) {
			status = "No Certificate Available";
			return false;
		} catch (java.security.ProviderException pe) {
			status = "Provider exception: " + pe.getMessage();
			return false;
		}
		status = certificateCN;
		return true;
	}
	
	public String getStatus () {
		return null==status?"":status;
	}

	public String signDocument(String document) throws GeneralSecurityException {
		if (null == currentX509Certificate) {
			throw new GeneralSecurityException("No certificate");
		}
		byte[] documentToSign = document.getBytes();
		Signature signatureAlgorithm = Signature.getInstance(DIGITAL_SIGNATURE_ALGORITHM_NAME);
		logger.info("Private key object: " + currentPrivateKey);
		signatureAlgorithm.initSign(currentPrivateKey);
		logger.info("Signature object:" + signatureAlgorithm.toString());
		signatureAlgorithm.update(documentToSign);
		byte[] digitalSignature = signatureAlgorithm.sign();
		String sSignature = Base64.encodeBytes(digitalSignature) +"";
		return sSignature;
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
	
	/**
	 * Snippet of code from forums
	 * @param dn
	 * @param attributeType
	 * @return
	 */
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

	private ProtectionParameter createPinNumberDialog() {
		CallbackHandlerProtection callbackHandlerProtection = new KeyStore.CallbackHandlerProtection(new CallbackHandler() {
			public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
				for (Callback callback : callbacks) {
					if (callback instanceof PasswordCallback) {
						PasswordCallback pwdCallback = (PasswordCallback) callback;
						char[] pin;
						if (session.isValid()) {
							pin = session.getPin();
						} else {
							pin = PinDialog.showPinDialog(null);
							startSession(pin);
						}
						if (pin != null && pin.length >= 4) {
							pwdCallback.setPassword(pin);
						} else {
							pwdCallback.clearPassword();
							session.setValid(false);
							throw new IOException("Cancelled");
						}
					}
				}
			}
		});
		return callbackHandlerProtection;
	}

	protected boolean startSession(char[] pin) {
		if (pin != null && pin.length >= 4) {
			UUID uuid = UUID.randomUUID();
			session.setSessionId(uuid.toString());
			session.setPin(pin);
			session.setValid(true);
		} else {
			session.setValid(false);
			session.setSessionId("");
		}
		return session.isValid();
	}
	
	public void reset() {
		try {
			if (pkcs11Provider instanceof SunPKCS11) {
				SunPKCS11 sunPKCS11 = (SunPKCS11) pkcs11Provider;
				sunPKCS11.logout();
			}
		} catch (LoginException e) {
			logger.warn("Didn't log out of keystore cleanly");
		} catch (ProviderException e) {
			logger.warn("Didn't log out of keystore cleanly - card removed?");
		}
		pkcs11Provider.clear();
		Security.removeProvider(pkcs11Provider.getName());
		session.setValid(false);
		session.setSessionId("");
		ks = null;
	}	
	
	public boolean isKeystoreLoaded() {
		return loaded;
	}

	public String getSessionId() {
		if (loaded && session.isValid()) {
			return session.getSessionId();
		}
		return "";
	}

	public boolean hasSession() {
		if (loaded && session.isValid()) {
			return !getSessionId().isEmpty();
		}
		return false;
	}

	public boolean isSessionMatch(String proposedSessionID) {
		if (null == proposedSessionID || proposedSessionID.isEmpty()) return false;
		return proposedSessionID.equals(getSessionId());
	}

	public boolean isGMCMatch(String proposedPrescReg) {
		if (!loaded) return false;
		String cardPrescReg = getPresciberRegistration();
		if (cardPrescReg.isEmpty()) return false;
		return cardPrescReg.equals(proposedPrescReg);
	}

	public String getPresciberRegistration() {
		String[] parts = certificateCN.split(":[A-Za-z]*");
		if (parts.length != 2) return "";
		return parts[1].trim();
	}

	public String getPresciberName() {
		String[] parts = certificateCN.split(":[A-Za-z]*");
		if (parts.length != 2) return "";
		return parts[0].trim();
	}

	public String getCertificateDetail() {
		SimpleDateFormat sdf = new SimpleDateFormat(MYSQL_DATE_FORMAT);
		StringBuilder response = new StringBuilder();
		if (isKeystoreLoaded() && hasSession() && null!=currentX509Certificate) {
			Date from = currentX509Certificate.getNotBefore();
			Date to = currentX509Certificate.getNotAfter();
			response.append("validFrom=");
			response.append(sdf.format(from));
			response.append(",validTo=");
			response.append(sdf.format(to));
			response.append(",prescriberName=");
			response.append(getPresciberName());
			response.append(",registration=");
			response.append(getPresciberRegistration());
		} 
		return response.toString();
	}
}