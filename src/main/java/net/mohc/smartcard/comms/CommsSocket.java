package net.mohc.smartcard.comms;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class CommsSocket extends Thread {

  private static final int TIME_BETWEEN_MSGS = 300;
  private static final int TIME_BETWEEN_ACCEPTS = 500;
	private static final long TIME_BETWEEN_CONNECTION_ATTEMPTS = 1000;
  private ServerSocket serverSocket;
  private Socket currentSocket;
  private Socket newSocket;
  private CommsServer rcParent;
  private boolean useNewSocket = false;
  InetAddress iaLocal = null;
	private Logger logger;
	private JSonUtilities jsonUtils;
	private boolean isCloseRequested = false;


  public CommsSocket (int iListenPortNumber, CommsServer rcParent) throws CommsException {
  	super("Comms Listener");
    this.setPriority(Thread.MIN_PRIORITY);
  	this.logger = Logger.getLogger(CommsSocket.class);
    this.jsonUtils = JSonUtilities.getInstance();
    this.rcParent = rcParent;
    

    // create a listen socket
    try {
      serverSocket = new ServerSocket(iListenPortNumber);
      logger.info("Remote Controller Listening on port "+iListenPortNumber+"...");
    } catch (java.net.BindException be) {
      throw new CommsException ("Can't use port " + iListenPortNumber + ", maybe already in use");
    } catch(Exception e) {
      throw new CommsException ("Can't start server: " + e.getMessage());
    }
  }

  public void run() {
    logger.info("Listener up");
    StringBuffer sbDataIn = new StringBuffer();
    InputStream  input = null;
    OutputStream output = null;

    RemoteMessage rm = new RemoteMessage();
    RemoteMessage rmr = new RemoteMessage();

//Sit in a loop waiting for messages...
    while(true) {
    	boolean isGoodConnection = false;
    	currentSocket = null;
      try {
        sleep(TIME_BETWEEN_MSGS);
      } catch (InterruptedException ie) {
        logger.warn("Insomnia " + ie);
      }
      yield();

      // listen & accept an incoming connection
      if (useNewSocket) {
        useNewSocket = false;
        currentSocket = newSocket;
      } else {
        do {
          try {
          	isCloseRequested = false;
            serverSocket.setSoTimeout(0); //Block
          	currentSocket = serverSocket.accept();
            input = new BufferedInputStream(currentSocket.getInputStream());
            output = new BufferedOutputStream(currentSocket.getOutputStream());
            isGoodConnection = checkConnectionValid(currentSocket);
            if (isGoodConnection) {
            	logger.info("Connected to: " + currentSocket.getInetAddress());
            } else {
            	logger.warn("Attempt to connect rejected");
              try {
                sleep(TIME_BETWEEN_CONNECTION_ATTEMPTS);
              } catch (InterruptedException ie) {
                logger.warn("Insomnia " + ie);
              }
            }
          } catch(IOException e) {
            logger.warn("Error on attempting to accept connection because " + e.getMessage());
          }
        } while (!isGoodConnection);
        logger.info("Connecting soServer = " + serverSocket.toString());
      }

      try {
        serverSocket.setSoTimeout(TIME_BETWEEN_ACCEPTS); //100mS block
      } catch (Exception e) {
        logger.warn("Cant set timeout: " + e);
      }

      //Look for messages and wait to send messages
      while (isGoodConnection && !isCloseRequested) {
        //Check for messages incoming...
        try {
          int iChrs = input.available();
          if (iChrs > 0) {
            byte[] ba = new byte[iChrs];
            input.read(ba, 0, iChrs);
            sbDataIn.append(new String(ba));
          }
          if (sbDataIn.length() > 0) {
            String sMsg = rm.scan(sbDataIn);
            if (sMsg != null) {
              logger.info( "RC Message Received");
              processMessageAndSendReply(sMsg);
            }
          }
        } catch (IOException e) {
          logger.warn("Error reading char: " + e);
          isGoodConnection = false;
          continue;
        }

        //Check for messages to go...
        String smr = rcParent.getNextMessageToSend();
        if (null != smr) {
          rmr.setMessage(smr);
					if (rmr.sendMessage(output)) {
						logger.info( "RC Message Sent: " + smr);
					} else {
						logger.error("RC Message failed to send");
					}
        }

        // listen & accept an incoming connection
        try {
          do {
            newSocket = serverSocket.accept();
          } while (!checkConnectionValid(newSocket));
          logger.info("GOT A NEW CONNECTION! Abandoning previous connection");
          useNewSocket = true;
          logger.info("Connecting soServer = " + serverSocket.toString());
          break;
        } catch(InterruptedIOException e) {
        } catch(Exception e) {
          logger.warn("Error on attempting socket connect " + e);
        }
        yield();
      }
      try {
        currentSocket.close();
        logger.info("Socket Closed");
      } catch (Exception e) {
        logger.warn("Socket Closed - with errors: " + e);
      }
    }
  }

  private boolean checkConnectionValid (Socket s) {
    InetAddress iaConnect = s.getInetAddress();
    iaLocal = s.getLocalAddress();
    if (!iaLocal.equals(iaConnect)) {
      logger.warn("ACCESS DENIED - Only local connects permitted");
      logger.info("ACCESS DENIED - Only local connects permitted");
      logger.info("Local host = " + iaLocal);
      logger.info("Remote client = " + iaConnect);
      try {
        s.close();
      } catch (Exception e) {}
      return false;
    }
    return true;
  }


  public void closeComms() {
    isCloseRequested  = true;
  }

  private void processMessageAndSendReply(final String sMsg) {
  	StringBuffer sbReply = new StringBuffer();
    if (!processMessage (sMsg, sbReply)) {
    	logger.info("Remote command failed - " + sbReply.toString());
    }
    if (sbReply.length() > 0) {
    	rcParent.sendMessage(sbReply.toString());
    }
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
    TAResponse response = rcParent.commandProcessor.processCommand(taCommand);
    reply.append(jsonUtils.convertResponseToJson(response));
    return true;
  }

  /**
   * Converts a data string into a string array
   * @param bEscape set true to use escape characters
   */
  private String[] convertToArgArray (String sData, boolean bEscape) {
    ArrayList<String> v = new ArrayList<>();
    String sCmdLine = sData.trim();
    boolean flagQuote = false;
    boolean flagEscape = false;
    boolean flagSeparator = false;
    StringBuffer argBuff = new StringBuffer();
    int index = 0;
    char chr;
    while (index < sCmdLine.length()) {
      chr = sCmdLine.charAt(index++);

      if (chr != ' ') {
        //Always reset the separator flag when not separating
        flagSeparator = false;
      }

      if (flagEscape) {
        //Character was escaped
        argBuff.append(chr);
        flagEscape = false;
        continue;
      }
/** @todo >>>CHECK INTO THIS, SLASHS ARE PROCESSED<<< */
      if ((chr == '\\') && bEscape ) {
        //Escape chr - the next character is forced
        flagEscape = true;
        continue;
      }

      if (flagQuote) {
        //In quotes...
        if (chr == '"') {
          flagQuote = false;
        } else {
          argBuff.append(chr);
        }
      } else {
        //Not in quotes
        if (chr == ';') {
          //Abort string processing
          break;
        } else if (chr == '"') {
          //Start quotes
          flagQuote = true;
        } else if (chr == ' ') {
          if (!flagSeparator) {
            //Only separate if not already separating...
            v.add(argBuff.toString());
            argBuff.setLength(0);
            flagSeparator = true;
          }
        } else {
          argBuff.append(chr);
        }
      }
    }

    if (argBuff.length() > 0) {
      v.add(argBuff.toString());
    }

    String[] args = new String[v.size()];
    for (int j = 0; j < v.size(); j++) {
      args[j] = (String)(v.get(j));
    }
    return args;

  }

  
  
  
  
  
  
  
  
  
  
  
}
