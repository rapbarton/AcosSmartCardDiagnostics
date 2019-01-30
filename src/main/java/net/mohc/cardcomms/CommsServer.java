package net.mohc.cardcomms;

import java.util.ArrayList;

public class CommsServer {
  private static final int DEFAULT_PORT = 9311;
  private int iTTPort = DEFAULT_PORT;
  private CommsSocket socket;
  protected String sMessageOut;
  protected ArrayList<String> vMsgReplies;

  Command command;

  private CommsServer() {
  }
  
	public CommsServer (int port) throws CommsException {
		this();
		this.iTTPort = port;
		this.command = null;
		this.vMsgReplies = new ArrayList<>();
		
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
    synchronized (vMsgReplies) {
      this.vMsgReplies.add(new String (sMsg));//New reference as we are in a thread
    }
    return true;
  }

  public void clearReplyQueue () {
    synchronized (vMsgReplies) {
      vMsgReplies.clear();
    }
  }

  public void closeComms() {
    socket.closeComms();
    socket = null;
  }		  
  
}
