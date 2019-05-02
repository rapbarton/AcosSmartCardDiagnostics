package net.mohc.smartcard.trayapp;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;

import org.apache.log4j.Logger;

public class SmartCardApplicationLauncher extends Thread {
	private static final long MIN_SECONDS_BEFORE_RETRY_STARTUP_COMMAND = 60;
	private static final long SECONDS_TO_WAIT_FOR_APP_STARTUP = 40;
	private static final String FILENAME_SMARTCARD_JAR = "smartcard.jar";
	private static final String[] ALL_LIBRARY_JAR_FILENAMES = {FILENAME_SMARTCARD_JAR, 
		"jackson-core.jar", "jackson-databind.jar", "jackson-annotations.jar", "log4j.jar"};

	private static final boolean EXECUTABLE_JAR_FORMAT = false;
	private Logger logger;
	private long lastAttempt = 0;
	private static SmartCardApplicationLauncher singletonInstance = null;
	private boolean running = false;
	private boolean requestRestart = false;
	private boolean pingRxd = false;

	/**
	 * This will dumbly attempt to launch the tray app whenever called. It will ignore request if within 60s of last attempt that appeared to run the command line successfully.
	 */
	public static void launch() {
		if (null == singletonInstance) {
			singletonInstance = new SmartCardApplicationLauncher("Smart Card Tray App Launcher");
		}
		singletonInstance.tryToStartCardTrayApplication();
	}
	
	protected SmartCardApplicationLauncher (String name) {
		super(name);
		setDaemon(true);
		running = false;
		logger = Logger.getLogger(SmartCardApplicationLauncher.class);
	}
	
	private void tryToStartCardTrayApplication() {
		if (!isAlive()) {
			start();
		} else {
			requestRestart  = true;
		}
	}

	@Override
	public void run() {
		while(true) {
			if (!running && dueForAnAttempt()) {
				running = launchAttempt();
			}
			if (!running && pingRxd) {
				logger.debug("A command has been successfully processed - assume application now started");
				pingRxd = false;
			}
			try {
				Thread.sleep(1000l);
			} catch (InterruptedException e) {
				logger.info("Launcher aborted");
				Thread.currentThread().interrupt();
				return;
			}
			if (running && requestRestart && dueForAnAttempt()) {
				requestRestart = false;
				running = false;
			}
		}
	}
		
	private boolean launchAttempt() {
  	logger.info("Trying to start tray application");
		boolean started = false;
  	Runtime rt = Runtime.getRuntime();
		try {
			String commandLine = getStartTrayCommand(EXECUTABLE_JAR_FORMAT);
			logger.info("Running command # " + commandLine);
			final Process pr = rt.exec(commandLine);
			InputStreamReader in = new InputStreamReader(pr.getInputStream());
			InputStreamReader er = new InputStreamReader(pr.getErrorStream());
      StringBuilder stdLine = new StringBuilder();
      StringBuilder errLine = new StringBuilder();
      long timeout = timeNow() + SECONDS_TO_WAIT_FOR_APP_STARTUP;
      boolean timeoutTriggered = false;
      boolean detectedStdoutMsg = false;
      pingRxd = false;
      do {
      	detectedStdoutMsg = readStandardOut(in, stdLine);
      	readErrorOut(er, errLine);
      	timeoutTriggered = timeNow() >= timeout;
      	started = detectedStdoutMsg || pingRxd;
      } while (!timeoutTriggered && !started);
      if (pingRxd) {
      	logger.debug("Application start detected by ping");
      } else if (timeoutTriggered) {
      	logger.debug("Application startup detection timeout");
      } else if (detectedStdoutMsg) {
      	logger.debug("Application startup detected by stdout message");
      } else {
      	logger.warn("Application startup broken");
      }
		} catch (IOException e) {
			logger.error("Failed to execute command to start tray application");
		} catch (IllegalArgumentException e) {
			logger.error("Command to start tray application was invalid");
		}
		resetNextRetryTimer();
		return started;
	}
	
	private boolean readStandardOut(InputStreamReader in, StringBuilder stdLine) throws IOException {
		boolean started = false;
		while (in.ready()) {
  		char c = (char) in.read();
  		if (c == '\n') {
  			String line = stdLine.toString();
      	stdLine.setLength(0);
      	logger.info(line.trim());
      	if (line.contains(SmartCardApplication.APPLICATION_STARTED_MESSAGE)) {
      		started = true;
      	}
  		} else {
  			stdLine.append(c);
  		}
  	}
		return started;
	}

	private void readErrorOut(InputStreamReader er, StringBuilder errLine) throws IOException {
  	while (er.ready()) {
  		char c = (char) er.read();
  		if (c == '\n') {
  			String line = errLine.toString();
      	errLine.setLength(0);
      	logger.warn(line.trim());
  		} else {
  			errLine.append(c);
  		}
  	}
	}

	private long timeNow() {
		return Calendar.getInstance().getTimeInMillis() / 1000l;
	}
	
	private boolean dueForAnAttempt() {
		if (lastAttempt == 0) return true;
		return (timeNow() - lastAttempt) > (MIN_SECONDS_BEFORE_RETRY_STARTUP_COMMAND);
	}

	protected String getStartTrayCommand(boolean isExecutableJarFormat) {
		if (isExecutableJarFormat) {
			return getStartTrayCommandForExecutableJar();
		} else {
			return getStartTrayCommandForSimpleJar();
		}
	}
	
	private String getStartTrayCommandForSimpleJar() {
		StringBuilder command = new StringBuilder();
		command.append(pathToJava());
		command.append(separator());
		command.append("java -classpath ");
		command.append(allJars());
//		command.append(" -Ddisable.error.popup=true -Djava.security.debug=sunpkcs11 ");
		command.append(" -Ddisable.error.popup=true ");
		command.append(pathToLog4jProperties());
		command.append(predefinedPathToDll());
		command.append(" ");
		command.append(SmartCardApplication.class.getCanonicalName());
		return command.toString();
	}

	private String allJars() {
		String lib = pathToJars();
		StringBuilder classpath = new StringBuilder();
		String pathSeparator = System.getProperty("path.separator");
		boolean firstOne = true;
		for(String jarFilename:ALL_LIBRARY_JAR_FILENAMES) {
			if (firstOne) {
				firstOne = false;
			} else {
				classpath.append(pathSeparator);	
			}
			String jarFilePath = findJarFrom(lib, jarFilename);
			if (jarFilePath.isEmpty()) {
//				logger.warn("Library file " + jarFilename + " was not found on path " + lib);
				classpath.append(lib);
				classpath.append(jarFilename);
			} else {
				classpath.append(jarFilePath);
			}
		}
		return classpath.toString();
	}

	private String getStartTrayCommandForExecutableJar() {
		StringBuilder command = new StringBuilder();
		command.append(pathToJava());
		command.append(separator());
//		command.append("java -jar -Ddisable.error.popup=true -Djava.security.debug=sunpkcs11 ");
		command.append("java -jar -Ddisable.error.popup=true ");
		command.append(pathToLog4jProperties());
		command.append(predefinedPathToDll());
		command.append(pathAndFilenameToTrayJar());
		return command.toString();
	}

	/**
	 * Default is to assume being launched for OPMS in which case log4j config is in working directory conf
	 * @return
	 */
	private String pathToLog4jProperties() {
		String configuredPathToLog4jConfig = System.getProperty("smartcard.location.log4j","");
		if (configuredPathToLog4jConfig.isEmpty()) {
			String base = System.getProperty("user.dir");
			configuredPathToLog4jConfig = base + separator() + "conf" + separator();
			File test = new File(configuredPathToLog4jConfig);
			if (!test.exists() || !test.isDirectory()) {
				return "";
			}
		}
		return "-Dlocation.log4j=" + configuredPathToLog4jConfig + " ";
	}

	private String predefinedPathToDll() {
		String systemDefinePathToDll = System.getProperty("smartcard.location.dll","");
		if (systemDefinePathToDll.isEmpty()) return "";
		return "-Dlocation.dll=" + systemDefinePathToDll + " ";
	}

	private String separator() {
		return System.getProperty("file.separator");
	}

	private String pathToJava() {
		String path = System.getProperty("smartcard.location.java", "");
		if (path.trim().isEmpty()) {
			path = System.getProperty("java.home") + separator() + "bin";
		}
		if (path.toLowerCase().contains("program")) {
			logger.warn("Path to Java is suspicious - Tray App unlikely to work well with System Java");
		}
		return path;
	}

	private String pathAndFilenameToTrayJar() {
		String trayJarPath = System.getProperty("smartcard.location.jar", "");
		if (trayJarPath.isEmpty()) {
			String base = System.getProperty("user.dir");
			trayJarPath = base + separator() + FILENAME_SMARTCARD_JAR;
		}
		return trayJarPath;
	}

	private String pathToJars() {
		String trayJarPath = System.getProperty("smartcard.location.lib.jars", "");
		if (trayJarPath.isEmpty()) {
			String base = System.getProperty("user.dir");
			trayJarPath = base + separator() + "lib" + separator();
		}
		return trayJarPath;
	}

	private void resetNextRetryTimer() {
		lastAttempt = timeNow();
	}
	
	/**
	 * This method called from good receipt of test message - i.e. if communications is working then we can assume app has started
	 * This is to make the startup robust against not seeing the standard output message at tray app startup
	 */
	public static void ping() {
		if (null != singletonInstance) {
			singletonInstance.pingRxd = true;
		}
	}

	private String findJarFrom(String libDir, String jarFilename) {
		if (FILENAME_SMARTCARD_JAR.equals(jarFilename)) {
			String trayJarPath = System.getProperty("smartcard.location.jar", "");
			if (!trayJarPath.isEmpty()) {
				return trayJarPath;
			}
		}		
		String filePath = ensureTrailingSeparator(libDir) + jarFilename;
		File jarFile = new File(filePath);
		if (jarFile.exists()) {
			return filePath;
		}
		File dir = new File(libDir);
		if (dir.isDirectory()) {
			File[] all = dir.listFiles();
			for (File file : all) {
				if (file.isDirectory()) {
					String subDir = libDir + file.getName() +  separator();
					String jarPath = findJarFrom(subDir, jarFilename);
					int len = jarPath.length();
					if (len > 0 && len < 2048) {
						logger.info("Library file " + jarFilename + " was found in " + subDir + separator());
						return jarPath;
					}
				}
			}
		}
		return "";
	}

	private String ensureTrailingSeparator(String path) {
		if (path.isEmpty()) return path;
		String sep = separator();
		if (path.endsWith(sep)) return path;
		return path + sep;
	}
	


}
