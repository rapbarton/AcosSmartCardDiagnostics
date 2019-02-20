package net.mohc.smartcard.comms;

import java.util.ArrayList;
import java.util.List;

public class CommsServer {
  private static final int DEFAULT_PORT = 9311;
  private int portNumber = DEFAULT_PORT;
  private CommsSocket socket;
  private ArrayList<String> queue;

  CommandProcessor commandProcessor;

  private CommsServer() {
  }
  
	public CommsServer (int port, CommandProcessor commandProcessor) throws CommsException {
		this();
		this.portNumber = port;
    this.commandProcessor = commandProcessor;
		this.queue = new ArrayList<>();
		socket = new CommsSocket(portNumber, this);
		socket.start();
	}
		
  public int getListenPort() {
    return portNumber;
  }

  public void setListenPort(int port) throws CommsException {
    socket.closeComms();
    portNumber = port;
    socket = new CommsSocket(portNumber, this);
    socket.start();
  }

  public synchronized boolean sendMessage(String sMsg) {
    synchronized (queue) {
      this.queue.add(new String (sMsg));//New reference as we are in a thread
    }
    return true;
  }

  public void clearReplyQueue () {
    synchronized (queue) {
      queue.clear();
    }
  }

  public void closeComms() {
    socket.closeComms();
    socket = null;
  }

	public List<String> getQueue() {
		return queue;
	}

	public String getNextMessageToSend() {
		String smr = null;
		synchronized (queue) {
    	if (!queue.isEmpty()) {
        smr = queue.get(0);
        queue.remove(0);
      }
    }
		return smr;
	}		  
  
}
