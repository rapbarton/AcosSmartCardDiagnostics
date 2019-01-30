package net.mohc.cardcomms;

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
  private boolean bConnected = false;
  private Socket socket;
  private InetAddress ipAddr;
  private Object lock;
  private RemoteMessage remoteMessage;
  private Receiver receiver;
  private RemoteControlReplyHandler rcrh;
  private Logger logger;
  
  CommsClient() {
  	logger = Logger.getLogger(this.getClass());
  }

  public CommsClient (int port, RemoteControlReplyHandler rcrh) throws CommsException {
    lock = new Object();
    this.iTTPort = port;
    this.rcrh = rcrh;
    remoteMessage = new RemoteMessage();
    receiver = new Receiver (this);
    receiver.start();
  }

  public void setRemoteControlReplyHandler (RemoteControlReplyHandler rcrh) {
    this.rcrh = rcrh;
  }

  public void connect() throws CommsException {
    try {
      InetAddress ip = InetAddress.getLocalHost();
      connect(ip);
    } catch (UnknownHostException uhe) {
      throw new CommsException("Cant resolve local host");
    }
  }

  public void connect(InetAddress ip) throws CommsException {
    try {
      synchronized (lock) {
        bConnected = false;
        ipAddr = ip;
        if (socket != null) {
          socket.close();
        }
        socket = new Socket(ipAddr, iTTPort);
        bos = new BufferedOutputStream(socket.getOutputStream()) ;
        bis = new BufferedInputStream(socket.getInputStream()) ;
      }
    } catch(Exception ex) {
      throw new CommsException (ex.getMessage());
    }
  }

  /**
   * Sends a message.
   */
  public void sendMessage (String msg) {
    synchronized (lock) {
      remoteMessage.setMessage(new String(msg));
      remoteMessage.sendMessage(bos);
    }
  }

  public void close () {
    receiver.close();
    receiver = null;
  }

  /**
   * Returns true once a valid message has been received, false if connection
   * broken or not yet established.
   */
  public boolean isConnected () {
    return bConnected;
  }

  void invokeProcessMessage(final String sMsg) {
    bConnected = true;
    Runnable r = new Runnable () {
      public void run () {
        rcrh.processReply(new String (sMsg));
      }
    };
    try {
      javax.swing.SwingUtilities.invokeLater(r);
    } catch (Exception exc) {
      logger.info("Handled exception", exc);
    }
  }

  class Receiver extends Thread {
    CommsClient parent;
    boolean abort = false;
    

    public Receiver (CommsClient parent) {
      this.parent = parent; //For easy reference
      this.setPriority(Thread.MIN_PRIORITY);
    }

    public void close() {
      abort = true;
    }

    public void run () {
      RemoteMessage rm = new RemoteMessage();
      StringBuffer sbDataIn = new StringBuffer();
      while (!abort) {

        while (parent.bis == null) {
          try {
            sleep(100);
          } catch (InterruptedException ie) {
          }
        }

        try {
          sleep(100);
        } catch (InterruptedException ie) {
        }
        
        
        yield();
        //Check for messages incoming...
        try {
          int iChrs = parent.bis.available();
          if (iChrs > 0) {
//System.out.println("Got some characters");
            byte[] ba = new byte[iChrs];
            parent.bis.read(ba, 0, iChrs);
            sbDataIn.append(new String(ba));
            //Got some data so scan...
            String sMsg = rm.scan(sbDataIn);
            if (sMsg != null) {
              parent.invokeProcessMessage(sMsg);
            }
          }
        } catch (Exception e) {
          System.out.println("Socket error - " + e.toString());
          try {
            parent.socket.close();
          } catch (IOException ioe) {}
          parent.bis = null;
          parent.bos = null;
          bConnected = false;
          continue;
        }
      }
      try {
        parent.socket.close();
      } catch (IOException ioe) {}
      parent.bis = null;
      parent.bos = null;
      bConnected = false;
     logger.info("RemoteControlClient killed");
    }
  }


}
