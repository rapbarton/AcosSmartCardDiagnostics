package net.mohc.smartcard.comms;

public class CommsServer {
  private static final int DEFAULT_PORT = 9311;
  private int portNumber = DEFAULT_PORT;
  private CommsServerSocket socket;

  private CommsServer() {
  }
  
	public CommsServer (int port, CommandProcessor commandProcessor) throws CommsException {
		this();
		this.portNumber = port;
		socket = new CommsServerSocket(portNumber, commandProcessor);
		socket.start();
	}
		
  public int getListenPort() {
    return portNumber;
  }

  public void closeComms() {
    socket.closeComms();
    socket = null;
  }
  
}
