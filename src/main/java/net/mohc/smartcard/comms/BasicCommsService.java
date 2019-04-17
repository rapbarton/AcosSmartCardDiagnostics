package net.mohc.smartcard.comms;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import net.mohc.smartcard.trayapp.SmartCardApplicationLauncher;
import net.mohc.smartcard.trayapp.SmartCardException;

import org.apache.log4j.Logger;

import static net.mohc.smartcard.trayapp.SmartCardConstants.*;

public class BasicCommsService {
	private static final int DEFAULT_PORT = 9311;
	private static final long DEFAULT_TIMEOUT = 20;
	private static final long INITTIAL_TEST_COMMS_COMMAND_TIME = 10;
	private static final long SCHEDULED_TEST_COMMS_COMMAND_TIME = 30;
	private static BasicCommsService singletonInstance = null;
	private Logger logger;
	private CommsClient rcc;
	private RemoteControlReplyHandler replyHandler;
	private HashMap<String, TAResponseHandler> handlers;
	JSonUtilities jsonUtils;
	private Thread commsStarter = null;
	private Object msgReceivedNotificationObject;

	public static BasicCommsService getInstance(boolean autostart) {
		if (null == singletonInstance) singletonInstance = new BasicCommsService(autostart);
		return singletonInstance;
	}

	private BasicCommsService(boolean autostart) {
		logger = Logger.getLogger(BasicCommsService.class);
		jsonUtils = JSonUtilities.getInstance();
		handlers = new HashMap<>();
		replyHandler = new RemoteControlReplyHandler() {
			@Override
			public void processReply(String msg) {
				processJsonMessage(msg);
			}};
		rcc = new CommsClient(DEFAULT_PORT, replyHandler );
		if (autostart) {
			startComms();
		}
	}
	
	public synchronized void startComms() {
		if (null != commsStarter && !commsStarter.isAlive()) {
			commsStarter = null;
		}		
		if (null == commsStarter) {
			commsStarter = new Thread ("Comms monitor") {
				@Override
				public void run() {
					commsMonitor();
				}
				private void commsMonitor () {
					logger.info("Starting comms monitor");
					boolean abort = false;
					final TestCommand testCommand = new TestCommand();
					do {
						if (!rcc.isConnected()) {
							logger.info("Not connected so trying to connect...");
							tryToConnect(testCommand);
						}
						abort = !sleepForSeconds(1);
						if (rcc.isConnected()) {
							monitorTestCommand(testCommand);
						}
					} while (!abort);
					logger.info("Finished with comms monitor");
			  }
			};
			commsStarter.start();
		}
	}
	
	protected void monitorTestCommand(final TestCommand testCommand) {
		if (!testCommand.isActive() && isTimeToCheck(testCommand.getScheduledCheckTime())) {
			logger.debug("Time to check connection by sending a test command");
			testCommand.create();
			testCommand.setTimeSentTestMessage(timeNow());
			sendCommand(testCommand.get(), new TAResponseHandler(){
				@Override
				public void messageResponse(Map<String, String> responses) {
					if (testCommand.isActive()) {
						logger.debug("Test command response received - all is well");
						testCommand.clear();
						testCommand.setScheduledCheckTime(SCHEDULED_TEST_COMMS_COMMAND_TIME + timeNow());
						SmartCardApplicationLauncher.ping();
					}
				}
				@Override
				public void messageError(String errorMessage) {
					testCommand.clear();
					logger.warn("Test message failure when checking connection - killing connection");
					rcc.disconnect();
				}});
		} else if (testCommand.isActive()) {
			long timeSpentWaitingForTestResponse = timeNow() - testCommand.getTimeSentTestMessage();
			if (timeSpentWaitingForTestResponse > 10) {
				testCommand.clear();
				logger.warn("Been taking too long to check connection with test command - killing connection");
				rcc.disconnect();
			}
		}								
	}

	private void tryToConnect(TestCommand testCommand) {
		try {
			rcc.connect();
			logger.info("Connected");
			testCommand.clear();
			testCommand.setScheduledCheckTime(timeNow() + INITTIAL_TEST_COMMS_COMMAND_TIME);
		} catch (CommsException e) {
			logger.info("Test connection failed, trying to launch app");
			SmartCardApplicationLauncher.launch();
			logger.info("Hang about for app to get going");
			sleepForSeconds(5);
		}
	}

	private boolean sleepForSeconds(int seconds) {
		try {
			Thread.sleep((long)seconds * 1000l);
			return true;
		} catch (InterruptedException e2) {
			logger.error("Unexpected interruption");
			Thread.currentThread().interrupt();
			return false;
		}
	}
	
	private class TestCommand {
		private TACommand command = null;
		private long scheduledCheckTime = 0;
		private long timeSentTestMessage = 0;
		public boolean isActive() {
			return null != command; 
		}
		public TACommand get() {
			return command;
		}
		public void create() {
			command = new TACommand(TACommand.TEST);
		}
		public void clear() {
			command = null;
		}
		public void setScheduledCheckTime(long scheduledCheckTime) {
			this.scheduledCheckTime = scheduledCheckTime;
		}
		public long getScheduledCheckTime() {
			return this.scheduledCheckTime;
		}
		public void setTimeSentTestMessage(long timeSentTestMessage) {
			this.timeSentTestMessage = timeSentTestMessage;
		}
		public long getTimeSentTestMessage() {
			return this.timeSentTestMessage;
		}
	}
	
	protected boolean isTimeToCheck(long scheduledCheckTime) {
		long elapsed = timeNow() - scheduledCheckTime;
		return elapsed >= 0;
	}
	
	protected long timeNow() {
		return Calendar.getInstance().getTimeInMillis() / 1000l;
	}

	private Map<String, String> sendCommandAndWait(final TACommand command) {
		final Map<String, String> retval = new HashMap<>();
		
		TAResponseHandler handler = new TAResponseHandler() {
			@Override
			public void messageResponse(Map<String, String> responses) {
				synchronized (retval) {
					logger.debug("Message received (for \"" + command.getCommand() + "\")");
					SmartCardApplicationLauncher.ping();
					if (command.getCommand().equals("Sign")) {
						logger.debug("PRIMARY RXD HANDLER:" + responses.get(KEY_PRIMARY_RESPONSE));
					}
					retval.putAll(responses);
					retval.notifyAll();
				}
			}
			@Override
			public void messageError(String errorMessage) {
				synchronized (retval) {
					logger.info("Message failure");
					retval.notifyAll();
				}
			}
		};

		synchronized (retval) {
			sendCommand(command, handler);
			try {
				retval.wait(DEFAULT_TIMEOUT * 1000l);
			} catch (InterruptedException e) {
				logger.info("Command interrupted");
			}
			if (command.getCommand().equals("Sign")) {
				logger.debug("PRIMARY RXD MAIN:" + retval.get(KEY_PRIMARY_RESPONSE));
			}
		}
		return retval;
	}

	public void sendCommand(TACommand command, TAResponseHandler handler) {
		if (command.isUsed()) {
			logger.error("Ignoring this request as it has already been used");
			return;
		}
		handlers.put(command.getCommandId(), handler);
		command.setUsed();
		boolean ok = rcc.sendMessage(jsonUtils.convertCommandToJson(command));
		if (!ok) {
			handlers.remove(command.getCommandId());
		}
	}

	private void processJsonMessage(String msgInJson) {
		TAResponse response = jsonUtils.convertResponseFromJson(msgInJson);
		if (null == response) {
			logger.error("Bad response packet received");
			return;
		}	
		String commandId = response.getCommandId();
		if (handlers.containsKey(commandId)) {
			TAResponseHandler handler = handlers.get(commandId);
			handlers.remove(commandId);
			invokeHandler(handler, response);
		} else if (TACommand.CARD_STATUS.equals(commandId)) { 
			synchronized (msgReceivedNotificationObject) {
				msgReceivedNotificationObject.notifyAll();
			}
		} else {
			logger.error("No handler for response packet received");
		}
	}
	
	private void invokeHandler(final TAResponseHandler handler, final TAResponse response) {
		if (response.isOK()) {
			handler.messageResponse(response.getResponses());
		} else {
			handler.messageError(response.getErrorMessage());
		}
	}

	public boolean doTestCommsCommand() {
		Map<String, String> response = sendCommandAndWait(new TACommand(TACommand.TEST));
		return "OK".equals(response.get(KEY_PRIMARY_RESPONSE));
	}

	public boolean doCheckSessionCommand(String sessionId) {
		Map<String, String> response = sendCommandAndWait(new TACommand(TACommand.SESSION_CHECK, sessionId));
		return TRUE.equals(response.get(KEY_PRIMARY_RESPONSE));
	}

	public void doConnectCommand(TAResponseHandler handler) {
		sendCommand(new TACommand(TACommand.LOGIN), handler);
	}

	public void doQuitSessionCommand(TAResponseHandler handler) {
		sendCommand(new TACommand(TACommand.QUIT, "signout"), handler);
	}

	public String[] doCardPresentCommand() {
		String[] reply = new String[2];
		Map<String, String> response = sendCommandAndWait(new TACommand(TACommand.CARD_STATUS));
		String cardStatus = response.get((KEY_PRIMARY_RESPONSE));
		if (null == cardStatus) cardStatus = "";
		String cardInfo = response.get((KEY_CARD_INFO));
		if (null == cardInfo) cardInfo = "";
		reply[0] = cardStatus;
		reply[1] = cardInfo;
		return reply;
	}

	public boolean isAvailable() {
		return rcc.isConnected();
	}

	public String doSignDocumentCommand(String sessionId, String prescReg, String document) {
		Map<String, String> response = sendCommandAndWait(new TACommand(TACommand.SIGN, sessionId, prescReg, document));
		if (response.isEmpty()) {
			throw new SmartCardException("Error: No response");
		}
		String primaryResponse = response.get(KEY_PRIMARY_RESPONSE);
		if (primaryResponse.toLowerCase().startsWith("error:")) {
			throw new SmartCardException(primaryResponse);
		} else {
			return primaryResponse;
		}
	}

	public String doGetEncodedCertificate() {
		Map<String, String> response = sendCommandAndWait(new TACommand(TACommand.CERT_ENCODED));
		return response.get(KEY_PRIMARY_RESPONSE);
	}

	public void setCardAvailableListener(Object msgReceivedNotificationObject) {
		this.msgReceivedNotificationObject = msgReceivedNotificationObject;
	}

}
