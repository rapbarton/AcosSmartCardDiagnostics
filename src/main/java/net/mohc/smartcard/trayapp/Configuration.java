package net.mohc.smartcard.trayapp;

import java.io.File;
import java.util.HashMap;
import java.util.Properties;

public class Configuration  implements SmartCardConstants {
	private static final String DEFAULT_DLL_FILENAME_32BIT = "acospkcs11v4x86.dll";
	private static final String DEFAULT_DLL_FILENAME_64BIT = "acospkcs11v4x64.dll";
	private static final String ACOS_DLL_FILENAME_32BIT = "acospkcs11v4x86.dll";
	private static final String ACOS_DLL_FILENAME_64BIT = "acospkcs11v4x64.dll";
	private HashMap<String, String> dllNames32bit;
	private HashMap<String, String> dllNames64bit;
	private String separator = "/";
	
	private static Configuration instance = new Configuration();
	
	private Configuration () {
		dllNames32bit = new HashMap<>();
		dllNames64bit = new HashMap<>();
		Properties sp = System.getProperties();
		separator = sp.getProperty("file.separator", "\\");
		dllNames32bit.put(TERMINAL_TYPE_ACOS, ACOS_DLL_FILENAME_32BIT);
		dllNames32bit.put(TERMINAL_TYPE_FEITIAN, DEFAULT_DLL_FILENAME_32BIT);
		dllNames32bit.put(TERMINAL_TYPE_OTHER, DEFAULT_DLL_FILENAME_32BIT);
		dllNames64bit.put(TERMINAL_TYPE_ACOS, ACOS_DLL_FILENAME_64BIT);
		dllNames64bit.put(TERMINAL_TYPE_FEITIAN, DEFAULT_DLL_FILENAME_64BIT);
		dllNames64bit.put(TERMINAL_TYPE_OTHER, DEFAULT_DLL_FILENAME_64BIT);
	}
	
	public static Configuration getInstance() {
		return instance;
	}

	public File getDllForAcos() {
		return findPathForDll(TERMINAL_TYPE_ACOS);
	}

	private File findPathForDll(String cardType) {
		String filename;
		if (isLinux()) {
			filename = "tbd";
		} else {
			filename = is64bit0()?dllNames64bit.get(cardType):dllNames32bit.get(cardType);
			if (null == filename) {
				filename = is64bit0()?DEFAULT_DLL_FILENAME_64BIT:DEFAULT_DLL_FILENAME_32BIT;
			}
		}
		String dllPath = System.getProperty("location.dll", ".");
		return new File(dllPath + separator + filename);		
	}

	private boolean isLinux() {
		String systemOs = System.getProperty("os.name");
	  if (systemOs != null) {
	     return systemOs.contains("nux");
	  }
	  return false;
	}

	private boolean is64bit0() {
	  String systemProp = System.getProperty("com.ibm.vm.bitmode");
	  if (systemProp != null) {
	     return "64".equals(systemProp);
	  }
	  systemProp = System.getProperty("sun.arch.data.model");
	  if (systemProp != null) {
	     return "64".equals(systemProp);
	  }
	  systemProp = System.getProperty("java.vm.version");
	  return systemProp != null && systemProp.contains("_64");
	}
}
