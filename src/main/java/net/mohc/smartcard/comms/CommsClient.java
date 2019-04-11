package net.mohc.smartcard.comms;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

public class CommsClient {
  private int iTTPort;
  private BufferedOutputStream bos;
  private BufferedInputStream bis;
  private Socket socket;
  private Receiver receiver;
  private RemoteControlReplyHandler rcrh;
  private Logger logger;
  
  public CommsClient (int port, RemoteControlReplyHandler rcrh) {
  	logger = Logger.getLogger(this.getClass());
    this.iTTPort = port;
    this.rcrh = rcrh;
    receiver = new Receiver (this);
    receiver.start();
  }

  public void connect() throws CommsException {
    try {
    	if (!receiver.isAlive()) {
    		logger.warn("Receiver died, restarting");
    		receiver = new Receiver (this);
        receiver.start();
    	}
      connect(InetAddress.getLocalHost());
    } catch (UnknownHostException uhe) {
      throw new CommsException("Cant resolve local host");
    }
  }

  private synchronized void connect(InetAddress ipAddr) throws CommsException {
    try {
    	if (isConnected()) {
    		logger.info("Connection requested but ignoring because already connected");
    	} else {
        if (socket != null) {
      		logger.info("Closing an old socket");
        	disconnect();
        }
        socket = new Socket(ipAddr, iTTPort);
        bos = new BufferedOutputStream(socket.getOutputStream()) ;
        bis = new BufferedInputStream(socket.getInputStream()) ;
    	}
    } catch(Exception ex) {
    	socket = null;
    	bos = null;
    	bis = null;
      throw new CommsException (ex.getMessage());
    }
  }

  /**
   * Sends a message.
   */
  public synchronized boolean sendMessage (final String msg) {
  	RemoteMessage remoteMessage = new RemoteMessage();
    remoteMessage.setMessage(msg);
    return remoteMessage.sendMessage(bos);
  }

  public synchronized void disconnect() {
  	if (isConnected()) {
  		try {
				socket.close();
			} catch (IOException e) {/*Don't care*/}
  	}
  	socket = null;
  	if (null != bis) {
    	try {
				bis.close();
			} catch (IOException e) {/*Don't care*/}
  	}
  	bis = null;
  	if (null != bos) {
    	try {
    		bos.close();
			} catch (IOException e) {/*Don't care*/}
  	}
		bos = null;
  }  	
  
  /**
   * Returns true once a valid message has been received, false if connection
   * broken or not yet established.
   */
  public synchronized boolean isConnected () {
  	return null != socket && socket.isConnected() && !socket.isClosed();
  }


  class Receiver extends Thread {
    CommsClient parent;
    boolean abort = false;    

    public Receiver (CommsClient parent) {
    	super ("SmartCard Comms Receiver");
      this.parent = parent; //For easy reference
      this.setPriority(Thread.MIN_PRIORITY);
    }

    public void close() {
      abort = true;
    }

    @Override
    public void run () {
      RemoteMessage rm = new RemoteMessage();
      StringBuilder sbDataIn = new StringBuilder();
      while (!abort) {
        while (!parent.isConnected() && !abort) { 
          sleep100();
        }
        if (abort) {
        	break;
        }
        sleep100();
        yield();
        try {
        	if (parent.isConnected()) {
        		scanAndProcessReplies(sbDataIn, rm);
        	}
        } catch (Exception e) {
          logger.info("Socket error - " + e.getMessage());
          parent.disconnect();
        }
      }      
      logger.info("RemoteControlClient killed");
    }
    
    private void scanAndProcessReplies(StringBuilder sbDataIn, RemoteMessage rm) throws IOException {
      int iChrs = parent.bis.available();
      if (iChrs > 0) {
        byte[] ba = new byte[iChrs];
        parent.bis.read(ba, 0, iChrs);
        sbDataIn.append(new String(ba));
        //Got some data so scan...
        String sMsg = rm.scan(sbDataIn);
        if (sMsg != null) {
        	rcrh.processReply(sMsg);
        }
      }
			
		}

		private void sleep100() {
      try {
        sleep(100);
      } catch (InterruptedException ie) {
      	Thread.currentThread().interrupt();
      }
    }
    
  }


}
