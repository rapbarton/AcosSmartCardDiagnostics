package smartcard;

import java.io.File;

public class Configuration {
	private static final String DEFAULT_DLL_PATH = "C:\\OPMS_J7\\Beta\\QA1\\acospkcs11v4x86.dll";
	private static final String DEFAULT_PIN = "331627";
	
	private static Configuration instance = new Configuration();
	
	public static Configuration getInstance() {
		return instance;
	}

	public File getDllForAcos() {
		return new File(DEFAULT_DLL_PATH);
	}

}
