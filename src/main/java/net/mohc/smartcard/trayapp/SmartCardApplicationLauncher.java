package net.mohc.smartcard.trayapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

public class SmartCardApplicationLauncher {
	private static final long MIN_SECONDS_BEFORE_RETRY_STARTUP_COMMAND = 60;
	private Logger logger;
	private Runnable serviceStarter = null;
	private long msLastRunComplete = 0;
	private static SmartCardApplicationLauncher singletonInstance = null;

	/**
	 * This will dumbly attempt to launch the tray app whenever called. It will ignore request if within 60s of last attempt that appeared to run the command line successfully.
	 */
	public static void launch() {
		if (null == singletonInstance) {
			singletonInstance = new SmartCardApplicationLauncher();
		}
		singletonInstance.tryToStartCardTrayApplication();
	}
	
	private SmartCardApplicationLauncher () {
		logger = Logger.getLogger(SmartCardApplicationLauncher.class);
	}
	
	private void tryToStartCardTrayApplication() {
		if (null == serviceStarter || lastRunSomeTimeAgo()) {
			serviceStarter = new Runnable() {
				@Override
				public void run() {
		    	logger.info("Trying to start tray application");
					Runtime rt = Runtime.getRuntime();
					try {
						String commandLine = getStartTrayCommand();
						logger.info("Running command # " + commandLine);
						Process pr = rt.exec(commandLine);
						
						BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			      String line;
			      boolean started = false;
			      while ((line = in.readLine()) != null) {
			      	logger.info(line);
			      	if (line.contains("Smart Card Application started")) {
			      		started = true;
			      		break;
			      	}
			      }
						if (!started) {
							int retval = pr.waitFor();
							logger.info("Execution of command to start tray application returned " + retval);
						}
					} catch (IOException e) {
						logger.error("Failed to execute command to start tray application");
					} catch (InterruptedException e) {
						logger.error("Command to start tray application was interrupted");
					} catch (IllegalArgumentException e) {
						logger.error("Command to start tray application was invalid");
					}
					msLastRunComplete = Calendar.getInstance().getTimeInMillis();
				}
			};
	  	SwingUtilities.invokeLater(serviceStarter);
		}
	}

	private boolean lastRunSomeTimeAgo() {
		if (msLastRunComplete == 0) return false;
		long msTimeNow =  Calendar.getInstance().getTimeInMillis();
		return (msTimeNow - msLastRunComplete) > (1000l * MIN_SECONDS_BEFORE_RETRY_STARTUP_COMMAND);
	}

	protected String getStartTrayCommand() {
		StringBuilder command = new StringBuilder();
		command.append(pathToJava());
		command.append(separator());
		command.append("java -jar ");
		command.append(pathToTrayJar());
		return command.toString();
	}

	private String separator() {
		return System.getProperty("file.separator");
	}

	private String pathToJava() {
		//TODO May wish to use different jvm in the future  
		return System.getProperty("java.home") + separator() + "bin";
	}

	private Object pathToTrayJar() {
		String trayJar = System.getProperty("smart.card.jar", "");
		if (trayJar.isEmpty()) {
			String base = System.getProperty("user.dir");
			trayJar = base + separator() + "SmartCardApplication.jar";
		}
		return trayJar;
	}

}
