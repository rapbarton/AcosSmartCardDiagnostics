package net.mohc.cardcomms;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Logger;

public class CommsSocket extends Thread {

  private static final int TIME_BETWEEN_MSGS = 300;
  private static final int TIME_BETWEEN_ACCEPTS = 500;
  private ServerSocket soServer;
  private Socket so;
  private Socket so_new;
  private InputStream  is;
  private OutputStream os;
  private StringBuffer sbDataIn;
  private CommsServer rcParent;
  private boolean bSocketOk = true;
  private boolean useNew = false;
  InetAddress iaLocal = null;
	private Logger logger;

  public CommsSocket (int iListenPortNumber, CommsServer rcParent) throws CommsException {
  	this.logger = Logger.getLogger(CommsSocket.class);
    this.rcParent = rcParent;
    sbDataIn = new StringBuffer();
    this.setName("RC Listener");
    this.setPriority(Thread.MIN_PRIORITY);

    // create a listen socket
    try {
      soServer = new ServerSocket(iListenPortNumber);
      logger.info("Remote Controller Listening on port "+iListenPortNumber+"...");
    } catch (java.net.BindException be) {
      throw new CommsException ("Can't use port " + iListenPortNumber + ", maybe already in use");
    } catch(Exception e) {
      throw new CommsException ("Can't start server: " + e.getMessage());
    }
  }

  public void run() {

    logger.info("TTSocket.run");

    RemoteMessage rm = new RemoteMessage();
    RemoteMessage rmr = new RemoteMessage();

//Sit in a loop waiting for messages...
    while(true) {
      so = null;
      try {
        sleep(TIME_BETWEEN_MSGS);
      } catch (InterruptedException ie) {
        logger.warn("Insomnia " + ie);
      }
      yield();

      // listen & accept an incoming connection
      try {
        if (useNew) {
          useNew = false;
          so = so_new;
        } else {
          soServer.setSoTimeout(0); //Block
          do {
            so = soServer.accept();
          } while (!checkConnectionValid(so));
          logger.info("Connecting soServer = " + soServer.toString());
        }
      } catch(Exception e) {
        logger.warn("TS Error on accept " + e);
        continue;
      }

      System.out.println("Connected to: " + so.getInetAddress());
      //Fetch the streams
      try {
        is  = new BufferedInputStream(so.getInputStream());
        os = new BufferedOutputStream(so.getOutputStream());
      } catch(IOException e) {
        logger.warn("TS Error on getting streams " + e);
        continue;
      }

      bSocketOk = true;
      try {
        soServer.setSoTimeout(TIME_BETWEEN_ACCEPTS); //100mS block
      } catch (Exception e) {
        logger.warn("Cant set timeout: " + e);
      }

      //Look for messages and wait to send messages
      while (bSocketOk) {


        //Check for messages incoming...
        try {
          int iChrs = is.available();

          if (iChrs > 0) {
            byte[] ba = new byte[iChrs];
            is.read(ba, 0, iChrs);
            sbDataIn.append(new String(ba));
          }

          //Check for data AND that a command processor exists
          Command command = rcParent.getRemoteCommand();
          if ((command != null) && (sbDataIn.length() > 0)) {
            //Got some data so scan...
            String sMsg = rm.scan(sbDataIn);
            if (sMsg != null) {
              logger.info( "RC Message Received: " + sMsg);
              processMessage(sMsg);
            }
          }

        } catch (Exception e) {
          logger.warn("TS Error reading char: " + e);
          bSocketOk = false;
          continue;
        }

        //Check for messages to go...
        synchronized (rcParent.vMsgReplies) {
          try {
            if (rcParent.vMsgReplies.size() > 0) {
              String smr = (String)(rcParent.vMsgReplies.get(0));
              rcParent.vMsgReplies.remove(0);
              rmr.setMessage(smr);
              rmr.sendMessage(os);
              logger.info( "RC Message Sent: " + rcParent.sMessageOut);
            }
          } catch (Exception e) {
            logger.warn("TS Error writing message " + e);
            bSocketOk = false;
            continue;
          }
        }

        // listen & accept an incoming connection
        try {
          do {
            so_new = soServer.accept();
          } while (!checkConnectionValid(so_new));
          logger.info("GOT A NEW CONNECTION");
          useNew = true;
          logger.info("Connecting soServer = " + soServer.toString());
          break;
        } catch(InterruptedIOException e) {
        } catch(Exception e) {
          logger.warn("TS Error on accept " + e);
        }
        yield();
      }
      try {
        so.close();
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
    bSocketOk = false;
  }

  private void processMessage(final String sMsg) {
    final Command command = rcParent.getRemoteCommand();
    if (command == null) {
      logger.info("No Remote Command Processor");
      return;
    }
    Runnable r = new Runnable () {
      public void run () {
        StringBuffer sbReply = new StringBuffer();
        if (!command.processMessage (sMsg, sbReply)) {
          logger.info("Remote command failed - " + command.getErrorMessage());
        }
        if (sbReply.length() > 0) {
          rcParent.sendMessage(sbReply.toString());
        }
      }
    };
    try {
      javax.swing.SwingUtilities.invokeAndWait(r);
    } catch (Exception exc) {
      logger.info("Handled exception");
    }
  }

}
