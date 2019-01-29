package net.mohc.smartcard;

import org.apache.log4j.Logger;

import net.mohc.comms.CommandProcessor;

public class SmartCardCommandProcessor implements CommandProcessor {
	private Logger logger;
	
	public SmartCardCommandProcessor() {
		logger = Logger.getLogger(this.getClass());
	}
	
	/**
   * Does nothing. Just a dummy command for debug.
   */
  public String commandTest (String sData) {
    logger.info("Remote test command received");
    return ("TEST OK");
  }
	
  
  
}
