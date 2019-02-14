package net.mohc.smartcard.comms;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.swing.SwingUtilities;

import net.mohc.smartcard.trayapp.SmartCardApplicationLauncher;

import org.apache.log4j.Logger;

public class BasicCommsService {
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
								try {
									rcc.connect(InetAddress.getLocalHost());
									logger.info("Connected");
									testCommand.clear();
									testCommand.setScheduledCheckTime(timeNow());
								} catch (CommsException e) {
									logger.info("Test connection failed, maybe tray app needs to start");
								}
								if (!rcc.isConnected()) {
									logger.info("Trying to launch app");
									SmartCardApplicationLauncher.launch();
									try {
										logger.info("Hang about for app to get going");
										Thread.sleep(3000);
									} catch (InterruptedException e2) {
										logger.error("Unexpected interruption");
										abort = true;
									}
								}
							} catch (UnknownHostException e) {
								logger.error("Error determining local host");
								abort = true;
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
											testCommand.setScheduledCheckTime(60l + timeNow());
										}
									}
									@Override
									public void messageError(String errorMessage) {
										testCommand.clear();
										logger.warn("Test message failure when checking connection - killing connection");
										rcc.close();
									}});
							} else if (testCommand.isActive()) {
								long timeSpentWaitingForTestResponse = timeNow() - testCommand.getTimeSentTestMessage();
								if (timeSpentWaitingForTestResponse > 5) {
									testCommand.clear();
									logger.warn("Been taking too long to check connection with test command - killing connection");
									rcc.close();
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

	private Map<String, String> sendCommandAndWait(TACommand command) {
		final Object o = new Object();
		final Map<String, String> retval = new HashMap<>();
		TAResponseHandler handler = new TAResponseHandler() {
			@Override
			public void messageResponse(Map<String, String> responses) {
				synchronized (o) {
					logger.info("Message received");
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
				logger.info("Timeout waiting for command response");
			} catch (InterruptedException e) {
				logger.info("Command response received");
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
		return "OK".equals(response.get(TAResponse.PRIMARY_RESPONSE));
	}

	public String doConnectCommand() {
		Map<String, String> response = sendCommandAndWait(new TACommand(TACommand.LOGIN));
		String session = response.get((TAResponse.PRIMARY_RESPONSE));
		if (null == session) session = "";
		return session;
	}

	public String doCardPresentCommand() {
		Map<String, String> response = sendCommandAndWait(new TACommand(TACommand.CARD_STATUS));
		String reply = response.get((TAResponse.PRIMARY_RESPONSE));
		if (null == reply) reply = "";
		return reply;
	}


}
