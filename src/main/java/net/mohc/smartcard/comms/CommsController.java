package net.mohc.smartcard.comms;

public class CommsController {

  protected static final int MODE_LOCAL = 1;
  protected static final int MODE_REMOTE = 2;
  protected static final int MODE_NORC = 3;

  private static CommsController instance = new CommsController();
	private boolean isConfigured = false;
	
	private CommsController() {}

	public static CommsController getInstance() {
		return instance;
	}
	
	public CommsController startListening(int port, CommandProcessor commandProcessor) throws CommsException {
		if (isConfigured) throw new CommsException("Already started");
		new CommsServer(port, commandProcessor);
		isConfigured = true;
		return this;
	}
	
}
