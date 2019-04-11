package net.mohc.smartcard.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class FileHelper {

	private static FileHelper instance = new FileHelper();
	private Logger logger;

	
	private FileHelper() {
		Logger.getLogger(FileHelper.class);
	}
	
	public static FileHelper getInstance() {
		return instance ;
	}
	
	public List<File> findDlls() {
		String home = System.getProperty("user.dir");
		String slash = System.getProperty("file.separator");
		return findDlls(home+slash);
	}

	public List<File> findDlls(String baseLocation) {
		List<File> filenames = new ArrayList<>();
		File locationOfDlls = new File(baseLocation);
		addDllsToArray(filenames, locationOfDlls, 0);
		return filenames;
	}

	private void addDllsToArray(List<File> array, File locationOfDlls, int safeCount) {
		if (array.size() >= 100 || safeCount > 20) return;//Ensure no loops - no deeper than 20 directories
		logger.info("Searching in " + locationOfDlls);
		if (locationOfDlls.exists() && locationOfDlls.isDirectory()) {
			File[] allFiles = locationOfDlls.listFiles();
			if (null != allFiles && allFiles.length > 0) {
				for (File file : allFiles) {
					if (isProbablySuitableDLL(file)) {
						array.add(file);
					} else if (file.isDirectory()) {
						addDllsToArray(array, file, safeCount+1);
					}
				}
			}
		}		
	}

	private boolean isProbablySuitableDLL(File file) {
		String filename = file.getName().toLowerCase();
		boolean isDll = filename.endsWith(".dll");
		boolean looksABitLikeAPKCSLib = filename.contains("pkcs11");
		return file.isFile() && isDll && looksABitLikeAPKCSLib;
	}
	
}
