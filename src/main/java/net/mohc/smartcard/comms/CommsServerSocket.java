package net.mohc.smartcard.comms;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class CommsServerSocket extends Thread {

//  private static final int TIME_BETWEEN_MSGS = 300;
  private static final int TIME_BETWEEN_ACCEPTS = 500;
//	private static final long TIME_BETWEEN_CONNECTION_ATTEMPTS = 1000;
  private ServerSocket serverSocket;
  private CommandProcessor commandProcessor;
  InetAddress iaLocal = null;
	private Logger logger;
	private boolean isCloseRequested = false;
  private ArrayList<CommsClientConnection> connectionPool;

  /**
   * Creates a server listening on the port specified. Creates client socket connections.
   * @param listeningPortNumber
   * @param rcParent
   * @throws CommsException
   */
  public CommsServerSocket (int listeningPortNumber, CommandProcessor commandProcessor) throws CommsException {
  	super("Comms Listener");
    this.setPriority(Thread.MIN_PRIORITY);
  	this.logger = Logger.getLogger(CommsServerSocket.class);
    this.commandProcessor = commandProcessor;
    this.connectionPool = new ArrayList<>();

    // create a listen socket
    try {
      serverSocket = new ServerSocket(listeningPortNumber);
      logger.info("Remote Controller Listening on port "+listeningPortNumber+"...");
    } catch (java.net.BindException be) {
      throw new CommsException ("Can't use port " + listeningPortNumber + ", maybe already in use");
    } catch(Exception e) {
      throw new CommsException ("Can't start server: " + e.getMessage());
    }
  }

  public void run() {
    logger.info("Listener up");
    try {
      serverSocket.setSoTimeout(TIME_BETWEEN_ACCEPTS); //100mS block
    } catch (IOException e) {
      logger.warn("Cant set timeout: " + e);
    }
    //Sit in a loop checking connection pool...
    while(!isCloseRequested) {
    	waitBrieflyForNewConnections();
      yield();
      removeDeadConnections();
    }
    removeAllConnections();
    try {
			serverSocket.close();
		} catch (IOException e) {
		}
    logger.info("Listener down");
  }

  private void removeDeadConnections() {
    ArrayList<CommsClientConnection> staleConnections = new ArrayList<>();
    for (CommsClientConnection connection : connectionPool) {
			if (connection.isInactive()) {
				staleConnections.add(connection);
			}
		}
    removeConnections(staleConnections);
	}
  
  private void removeConnections(List<CommsClientConnection> connectionsToBeRemoved) {
    for (CommsClientConnection connection : connectionsToBeRemoved) {
    	connection.shutdown();
    	connectionPool.remove(connection);
    }
  }

  private void removeAllConnections() {
    for (CommsClientConnection connection : connectionPool) {
    	connection.shutdown();
    }
    connectionPool.clear();
  }

  
  
  private void waitBrieflyForNewConnections() {
  	try {
			Socket socket = serverSocket.accept();
			if (null != socket) {
				if (checkConnectionValid(socket)) {
					CommsClientConnection newConnection = CommsClientConnection.create(socket, commandProcessor);
					connectionPool.add(newConnection);
				} else {
					tryToSleep(TIME_BETWEEN_ACCEPTS);
				}
			}			
		} catch (IOException e1) {
			//Get here if nothing connecting - no big deal
		}
	}

	private void tryToSleep(int time) {
    try {
      sleep(time);
    } catch (InterruptedException ie) {
      logger.warn("Insomnia " + ie);
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





  
  
  
  
  
  
  
  
  
  
}
