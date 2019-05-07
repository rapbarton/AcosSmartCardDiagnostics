package net.mohc.smartcard.comms;
import static net.mohc.smartcard.trayapp.SmartCardConstants.DEFAULT_PORT;

public class CommsServer {
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
