package net.mohc.smartcard.comms;

import java.util.ArrayList;
import java.util.List;

public class CommsServer {
  private static final int DEFAULT_PORT = 9311;
  private int iTTPort = DEFAULT_PORT;
  private CommsSocket socket;
  private ArrayList<String> queue;

  Command command;

  private CommsServer() {
  }
  
	public CommsServer (int port) throws CommsException {
		this();
		this.iTTPort = port;
		this.command = null;
		this.queue = new ArrayList<>();
		
		// create a Proxy socket object
		socket = new CommsSocket(iTTPort, this);
		
		// set the Proxy object running
		socket.start();
	}
		
	public void setCommandProcessor (CommandProcessor rcp) {
    this.command = new Command (rcp);
  }

  protected Command getRemoteCommand () {
    return command;
  }

  public int getListenPort() {
    return iTTPort;
  }

  public void setListenPort(int port) throws CommsException {
    socket.closeComms();
    iTTPort = port;
    socket = new CommsSocket(iTTPort, this);
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
