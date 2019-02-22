package net.mohc.smartcard.comms;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

public class CommsClientConnection extends Thread {
	private static final long TIME_TO_CHECK_CONNECTION_DEATH = 5;
	private static int autoincrementIdNumber = 1;
	private Logger logger;
	private CommandProcessor commandProcessor;
	private Socket currentSocket;
	private StringBuffer sbDataIn;
  private InputStream  input;
  private OutputStream output;
  private RemoteMessage remoteMessageHelper;
	private JSonUtilities jsonUtils;
  private ArrayList<String> queue;
  private boolean shutdownRequested;
  private long timeOfLastConnectionTest;

	private CommsClientConnection(Socket socket, CommandProcessor commandProcessor) {
		this.logger = Logger.getLogger(CommsClientConnection.class);
		this.setName("Client Connection " + getNextId());
		this.commandProcessor = commandProcessor;
		this.currentSocket = socket;
		this.sbDataIn = new StringBuffer();
		this.remoteMessageHelper = new RemoteMessage();
    this.jsonUtils = JSonUtilities.getInstance();
		this.queue = new ArrayList<>();
		this.timeOfLastConnectionTest = timeNow();
		this.shutdownRequested = false;
	}
	
	private static synchronized int getNextId() {
		return autoincrementIdNumber++;
	}

	public static CommsClientConnection create(Socket socket, CommandProcessor commandProcessor) {
		CommsClientConnection connection = new CommsClientConnection(socket, commandProcessor);
		connection.start();
		return connection;
	}
	
  public void run() {
    logger.info("Client connection made: " + currentSocket.toString());
    
    try {
			input = new BufferedInputStream(currentSocket.getInputStream());
	    output =  new BufferedOutputStream(currentSocket.getOutputStream());
		} catch (IOException e) {
			logger.warn("Failed to create streams for connection");
			return;
		}

    //Sit in a loop waiting for messages...
    while(!shutdownRequested) {
    	lookForIncomingMessages();
    	sendReplies();
    	try {
				sleep(250);
			} catch (InterruptedException e) {
			}
    	testConnection();
    }    
    clearReplyQueue();
    closeAllStreams();
    logger.info("Client connection down");
  }

  private void closeAllStreams() {
    try {
			if (null != currentSocket) {
				currentSocket.close();
			}
		} catch (IOException e) {
			logger.warn("socket did not close cleanly");
		}
    try {
    	if (null != input) {
    		input.close();
    	}
		} catch (IOException e) {
			logger.warn("input did not close cleanly");
		}
    try {
    	if (null != output) {
    		output.close();
    	}
		} catch (IOException e) {
			logger.warn("output did not close cleanly");
		}
  }
  
	private void testConnection() {
		long timeNow = timeNow();
		if ((timeNow - timeOfLastConnectionTest) > TIME_TO_CHECK_CONNECTION_DEATH) {
			timeOfLastConnectionTest = timeNow;
			try {
				output.write(0); 
				output.flush(); //This is the only way to truly detect other end has gone away
			} catch (IOException e) {
				logger.warn("Connection test failed - maybe client has gone away");
				shutdownRequested = true;
			}
		}
	}

	protected long timeNow() {
		return Calendar.getInstance().getTimeInMillis() / 1000l;
	}

	private void lookForIncomingMessages() {
    try {
      int iChrs = input.available();
      if (iChrs > 0) {
        byte[] ba = new byte[iChrs];
        input.read(ba, 0, iChrs);
        sbDataIn.append(new String(ba));
      }
      if (sbDataIn.length() > 0) {
        String rawMessage = remoteMessageHelper.scan(sbDataIn);
        if (rawMessage != null) {
          logger.info( "RC Message Received");
          processMessageAndSendReply(rawMessage);
        }
      }
    } catch (IOException e) {
      logger.warn("Error reading char: " + e);
    }
	}
	
  private void processMessageAndSendReply(final String sMsg) {
  	SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
		  	StringBuffer sbReply = new StringBuffer();
		    if (!processMessage (sMsg, sbReply)) {
		    	logger.info("Remote command failed - " + sbReply.toString());
		    }
		    if (sbReply.length() > 0) {
		    	addToMessageQueue(sbReply.toString());
		    }
			}
		});
  }
  
  /**
   * Assumes message is a json encoded command
   */
  private boolean processMessage (String message, StringBuffer reply) {
  	TACommand taCommand = jsonUtils.convertCommandFromJson(message);
  	reply.setLength(0);
    if (taCommand == null) {
    	reply.append("ERROR: Message was not understood");
      return false;
    }
    TAResponse response = commandProcessor.processCommand(taCommand);
    reply.append(jsonUtils.convertResponseToJson(response));
    return true;
  }

  private boolean addToMessageQueue(String sMsg) {
    synchronized (queue) {
      this.queue.add(new String (sMsg));//New reference as we are in a thread
    }
    return true;
  }

	private void sendReplies() {
    String smr = getNextMessageToSend();
    if (null != smr) {
      RemoteMessage rmr = new RemoteMessage();
      rmr.setMessage(smr);
			if (rmr.sendMessage(output)) {
				logger.info( "RC Message Sent: " + smr);
				timeOfLastConnectionTest = timeNow();
			} else {
				logger.error("RC Message failed to send");
			}
    }
	}
  
  private void clearReplyQueue () {
    synchronized (queue) {
      queue.clear();
    }
  }
  
  private String getNextMessageToSend() {
		String smr = null;
		synchronized (queue) {
    	if (!queue.isEmpty()) {
        smr = queue.get(0);
        queue.remove(0);
      }
    }
		return smr;
	}

	public synchronized boolean isInactive() {
		if (!isAlive()) {
			return true;
		}
		if (null == currentSocket) {
			return true;
		}
		if (currentSocket.isClosed()) {
			return true;
		}
		return false;
	}

	public synchronized void shutdown() {
		shutdownRequested = true;		
	}		  
}
