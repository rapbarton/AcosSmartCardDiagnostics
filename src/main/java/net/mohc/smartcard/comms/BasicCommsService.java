package net.mohc.smartcard.comms;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.swing.SwingUtilities;

import net.mohc.smartcard.trayapp.SmartCardApplicationLauncher;
import net.mohc.smartcard.trayapp.SmartCardConstants;
import net.mohc.smartcard.trayapp.SmartCardException;

import org.apache.log4j.Logger;

public class BasicCommsService implements SmartCardConstants {
	private static final int DEFAULT_PORT = 9311;
	private static final long DEFAULT_TIMEOUT = 10;
	private static BasicCommsService singletonInstance = null;
	private Logger logger;
	private CommsClient rcc;
	private RemoteControlReplyHandler replyHandler;
	private HashMap<String, TAResponseHandler> handlers;
	JSonUtilities jsonUtils;
	private Thread commsStarter = null;

	public static BasicCommsService getInstance(boolean autostart) {
		if (null == singletonInstance) singletonInstance = new BasicCommsService(autostart);
		return singletonInstance;
	}

	private BasicCommsService(boolean autostart) {
		try {
			logger = Logger.getLogger(BasicCommsService.class);
			jsonUtils = JSonUtilities.getInstance();
			handlers = new HashMap<>();
			replyHandler = new RemoteControlReplyHandler() {
				@Override
				public void processReply(String msg) {
					_processReply(msg);
				}};
			rcc = new CommsClient(DEFAULT_PORT, replyHandler );
			if (autostart) {
				startComms();
			}
		} catch (CommsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public synchronized void startComms() throws CommsException {
		if (null != commsStarter && !commsStarter.isAlive()) {
			commsStarter = null;
		}		
		if (null == commsStarter) {
			commsStarter = new Thread ("Comms monitor") {
				public void run() {
					commsMonitor();
				}
				private void commsMonitor () {
					logger.info("Starting comms monitor");
					boolean abort = false;
					final TestCommand testCommand = new TestCommand();
					do {
						//If not connected, try connect
						if (!rcc.isConnected()) {
							logger.info("Not connected so trying to connect...");
							try {
								rcc.connect();
								logger.info("Connected");
								testCommand.clear();
								testCommand.setScheduledCheckTime(timeNow() + 2);
							} catch (CommsException e) {
								logger.info("Test connection failed, trying to launch app");
								SmartCardApplicationLauncher.launch();
								try {
									logger.info("Hang about for app to get going");
									Thread.sleep(5000);
								} catch (InterruptedException e2) {
									logger.error("Unexpected interruption");
									abort = true;
								}
							}
				  	}
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							logger.error("Unexpected interruption");
							abort = true;
						}
						if (rcc.isConnected()) {
							if (!testCommand.isActive() && isTimeToCheck(testCommand.getScheduledCheckTime())) {
								logger.info("Time to check connection by sending a test command");
								testCommand.create();
								testCommand.setTimeSentTestMessage(timeNow());
								sendCommand(testCommand.get(), new TAResponseHandler(){
									@Override
									public void messageResponse(Map<String, String> responses) {
										if (testCommand.isActive()) {
											logger.info("Test command response received - all is well");
											testCommand.clear();
											testCommand.setScheduledCheckTime(30l + timeNow());
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
								if (timeSpentWaitingForTestResponse > 5) {
									testCommand.clear();
									logger.warn("Been taking too long to check connection with test command - killing connection");
									rcc.disconnect();
								}
							}								
						}
					} while (!abort);
					logger.info("Finished with comms monitor");
			  }
			};
			commsStarter.start();
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
		final Object o = new Object();
		final Map<String, String> retval = new HashMap<>();
		TAResponseHandler handler = new TAResponseHandler() {
			@Override
			public void messageResponse(Map<String, String> responses) {
				synchronized (o) {
					logger.info("Message received (for \"" + command.getCommand() + "\")");
					retval.putAll(responses);
					o.notify();
				}
			}
			@Override
			public void messageError(String errorMessage) {
				synchronized (o) {
					logger.info("Message failure");
					o.notify();
				}
			}
		};
		synchronized (o) {
			sendCommand(command, handler);
			try {
				o.wait(DEFAULT_TIMEOUT * 1000l);
				//logger.info("Timeout waiting for command response");
			} catch (InterruptedException e) {
				logger.info("Command interrupted");
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

	private void _processReply(String msgInJson) {
		//logger.info("Received a reply \"" + msg + "\"");
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
		} else {
			logger.error("No handler for response packet received");
		}
	}
	
	private void invokeHandler(final TAResponseHandler handler, final TAResponse response) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (response.isOK()) {
					handler.messageResponse(response.getResponses());
				} else {
					handler.messageError(response.getErrorMessage());
				}
			}});
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

	public String[] doCardPresentCommand() {
		String reply[] = new String[2];
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
		String primaryResponse = response.get(KEY_PRIMARY_RESPONSE);
		if (primaryResponse.startsWith("Error:")) {
			throw new SmartCardException(primaryResponse);
		} else {
			return primaryResponse;
		}
	}

	public String doGetEncodedCertificate() {
		Map<String, String> response = sendCommandAndWait(new TACommand(TACommand.CERT_ENCODED));
		return response.get(KEY_PRIMARY_RESPONSE);
	}

}
