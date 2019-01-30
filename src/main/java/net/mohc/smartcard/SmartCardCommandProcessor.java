package net.mohc.smartcard;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import org.apache.log4j.Logger;

import net.mohc.cardcomms.CommandProcessor;

public class SmartCardCommandProcessor implements CommandProcessor {
	private Logger logger;
	private SmartCardController controller;
	
	public SmartCardCommandProcessor() {
		logger = Logger.getLogger(this.getClass());
		controller = SmartCardController.getInstance();
	}
	
	/**
   * Does nothing. Just a dummy command for debug.
   */
  public String commandTest (String sData) {
    logger.info("Remote test command received");
    String response = "TEST OK";
    if (!sData.isEmpty()) {
    	response += " - \"" + sData + "\"";
    }
    return (response);
  }
	
  public String commandCardPresentStatus (String sData) {
  	return controller.getCardPresentStatus();
  }
  
  public String commandCertificateStatus (String sData) {
  	String status = controller.getCertificateStatus();
  	if (null == status || status.isEmpty()) {
  		return "No certificate visible";
  	} else {
  		return status;
  	}
  }
  
  public String commandConnect (String sData) {
		controller.openKeystore();
		if (controller.isKeyStoreOpen()) {
			return "Connected:" + controller.getSessionId();
		} else {
			return "Failed to connect";
		}
	}
  
  public String commandSign (String sData) {
  	String result = "Error: Bad arguments in request";
  	String[] arguments = sData.split(":");
  	if (arguments.length == 2) {
  		String sessionID = arguments[0];
  		String dataToSign = arguments[1];
  		result = controller.doSignatureInSession(sessionID, dataToSign);
  	}  	
  	return result;
  }
  
  public String commandExit (String sData) {
  	if (sData.equalsIgnoreCase("shutdown")) {
  		Timer timer = new Timer(1000,new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					controller.shutdown();
				}
			});
  		timer.start();
  		return "Closing Down";
  	} else {
  		return "Command ignored due to incorrect argument";
  	}
  	
  }
  
}
