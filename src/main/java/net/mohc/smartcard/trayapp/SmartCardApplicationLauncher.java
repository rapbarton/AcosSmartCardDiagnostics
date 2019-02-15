package net.mohc.smartcard.trayapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

public class SmartCardApplicationLauncher extends Thread {
	private static final long MIN_SECONDS_BEFORE_RETRY_STARTUP_COMMAND = 60;
	private Logger logger;
	private Runnable serviceStarter = null;
	private long lastAttempt = 0;
	private static SmartCardApplicationLauncher singletonInstance = null;
	private boolean running = false;
	private boolean requestRestart = false;

	/**
	 * This will dumbly attempt to launch the tray app whenever called. It will ignore request if within 60s of last attempt that appeared to run the command line successfully.
	 */
	public static void launch() {
		if (null == singletonInstance) {
			singletonInstance = new SmartCardApplicationLauncher("Smart Card Tray App Launcher");
		}
		singletonInstance.tryToStartCardTrayApplication();
	}
	
	private SmartCardApplicationLauncher (String name) {
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

	public void run() {
		while(true) {
			if (!running && dueForAnAttempt()) {
				running = launchAttempt();
			}
			try {
				Thread.sleep(1000l);
			} catch (InterruptedException e) {
				logger.info("Launcher aborted");
				return;
			}
			if (running && requestRestart && dueForAnAttempt()) {
				requestRestart = false;
				running = false;
			}
		}
	}
	
	
	public boolean launchAttempt() {
  	logger.info("Trying to start tray application");
		boolean started = false;
  	Runtime rt = Runtime.getRuntime();
		try {
			String commandLine = getStartTrayCommand();
			logger.info("Running command # " + commandLine);
			final Process pr = rt.exec(commandLine);
			
//			BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
//			BufferedReader er = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
			InputStreamReader in = new InputStreamReader(pr.getInputStream());
			InputStreamReader er = new InputStreamReader(pr.getErrorStream());
      StringBuilder stdLine = new StringBuilder();
      StringBuilder errLine = new StringBuilder();
      started = false;

      long timeout = timeNow() + 20;
      do {
      	while (in.ready()) {
      		char c = (char) in.read();
      		if (c == '\n') {
      			String line = stdLine.toString();
          	stdLine.setLength(0);
          	logger.info(line.trim());
          	if (line.contains("Smart Card Application started")) {
          		started = true;
          	}
      		} else {
      			stdLine.append(c);
      		}
      	}
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
      } while (timeNow() < timeout && !started);
		} catch (IOException e) {
			logger.error("Failed to execute command to start tray application");
		} catch (IllegalArgumentException e) {
			logger.error("Command to start tray application was invalid");
		}
		lastAttempt = timeNow();
		return started;
	}

	private long timeNow() {
		return Calendar.getInstance().getTimeInMillis() / 1000l;
	}
	
	private boolean dueForAnAttempt() {
		if (lastAttempt == 0) return true;
		return (timeNow() - lastAttempt) > (MIN_SECONDS_BEFORE_RETRY_STARTUP_COMMAND);
	}

	protected String getStartTrayCommand() {
		StringBuilder command = new StringBuilder();
		command.append(pathToJava());
		command.append(separator());
		command.append("java -jar -Ddisable.error.popup=true ");
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
