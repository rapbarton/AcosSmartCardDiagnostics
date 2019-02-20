package net.mohc.smartcard.trayapp;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

import javax.swing.Timer;

import org.apache.log4j.Logger;

import net.mohc.smartcard.comms.CommandProcessor;
import net.mohc.smartcard.comms.TACommand;
import net.mohc.smartcard.comms.TAResponse;

public class SmartCardCommandProcessor implements CommandProcessor, SmartCardConstants {
	private Logger logger;
	private SmartCardController controller;
	
	public SmartCardCommandProcessor() {
		logger = Logger.getLogger(this.getClass());
		controller = SmartCardController.getInstance();
	}


	@Override
	public TAResponse processCommand(TACommand commandPacket) {
		String command = commandPacket.getCommand();
		TAResponse response = null;
		
		switch (command) {
		case TACommand.TEST:
			response = commandTest(commandPacket);
			break;
			
		case TACommand.CARD_STATUS:
			response = commandCardPresentStatus(commandPacket);
			break;
			
		case TACommand.LOGIN:
			response = commandConnect(commandPacket);
			break;
			
		case TACommand.SESSION_CHECK:
			response = commandSessionCheck(commandPacket);
			break;		

		case TACommand.CERT_OWNER:
			response = commandCertificateOwner(commandPacket);
			break;

		case TACommand.CERT_DETAIL:
			response = commandCertificateDetail(commandPacket);
			break;

		case TACommand.CERT_ENCODED:
			response = commandCertificateEncoded(commandPacket);
			break;

		case TACommand.QUIT:
			response = commandQuit(commandPacket);
			break;

		case TACommand.SIGN:
			response = commandSign(commandPacket);
			break;

		default:
			response = TAResponse.buildResponse(commandPacket).setErrorMessage("Unrecognised command");
		}
		return response;
	}
	
	/**
   * Does nothing. Just a dummy command for debug.
   */
  private TAResponse commandTest (TACommand commandPacket) {
    logger.info("Remote test command received");
    TAResponse responsePacket = TAResponse.buildResponse(commandPacket)
    		.addPrimaryResponse(RESPONSE_SUCCESS);
    List<String> arguments = commandPacket.getArguments();
    int argNo = 1;
    for (String argument : arguments) {
			responsePacket.addResponse("arg(" + argNo + ")", argument);
		}
    return (responsePacket);
  }
	
  private TAResponse commandCardPresentStatus (TACommand commandPacket) {
    logger.info("Card status command received");
    TAResponse responsePacket = TAResponse.buildResponse(commandPacket)
    		.addPrimaryResponse(controller.getCardPresentStatus())
    		.addResponse(KEY_CARD_INFO, controller.connectedCardInfo());
  	return responsePacket;
  }
  
  private TAResponse commandCertificateOwner (TACommand commandPacket) {
  	String status = controller.getCertificateStatus();
  	if (null == status || status.isEmpty()) {
  		return TAResponse.buildResponse(commandPacket).addPrimaryResponse("No certificate visible");
  	} else {
  		return TAResponse.buildResponse(commandPacket).addPrimaryResponse(status);
  	}
  }
  
  private TAResponse commandCertificateDetail (TACommand commandPacket) {
  	Map<String, String> status = controller.getCertificateDetails();
  	if (null == status || status.isEmpty()) {
  		return TAResponse.buildResponse(commandPacket).addPrimaryResponse("No certificate visible");
  	} else {
  		TAResponse response = TAResponse.buildResponse(commandPacket)
  				.addPrimaryResponse("OK")
  				.addResponses(status);
  		return response;
  	}
  }
  
  private TAResponse commandCertificateEncoded (TACommand commandPacket) {
  	String encodedCertificate = controller.getCertificateEncoded();
  	if (null == encodedCertificate || encodedCertificate.isEmpty()) {
  		return TAResponse.buildResponse(commandPacket).addPrimaryResponse("No certificate visible");
  	} else {
  		TAResponse response = TAResponse.buildResponse(commandPacket)
  				.addPrimaryResponse(encodedCertificate);
  		return response;
  	}
  }
  
  private TAResponse commandConnect (TACommand commandPacket) {
    logger.info("Log in to keystore command received");
    TAResponse responsePacket = TAResponse.buildResponse(commandPacket);
  	controller.openKeystore();
		if (controller.isKeyStoreOpen()) {
			responsePacket.addPrimaryResponse(RESPONSE_CONNECTED);
			responsePacket.addResponse(KEY_SESSION, controller.getSessionId());
			Map<String, String> status = controller.getCertificateDetails();
			responsePacket.addResponses(status);
			String encodedCertificate = controller.getCertificateEncoded();
			responsePacket.addResponse(KEY_ENCODED, encodedCertificate);
		} else {
			responsePacket.setError("Failed to log in");
		}
  	return responsePacket;
	}
  
  private TAResponse commandSessionCheck (TACommand commandPacket) {
    logger.info("Check session is valid");
    TAResponse responsePacket = TAResponse.buildResponse(commandPacket);
    if (commandPacket.getArguments().isEmpty()) {
    	responsePacket.setError("No session passed");
    } else {
	    String result = FALSE;
	  	controller.openKeystore();
			if (controller.isKeyStoreOpen()) {
				String currentSession = controller.getSessionId();
				if (commandPacket.getArguments().get(0).equals(currentSession)) {
					result = TRUE;
				}
			}
			responsePacket.addPrimaryResponse(result);
    }
		return responsePacket;
	}
  
  private TAResponse commandSign (TACommand commandPacket) {
  	List<String> arguments = commandPacket.getArguments();
  	String result = "Error: Bad arguments in request";
  	if (arguments.size() >= 3) {
  		String sessionID = arguments.get(0);
  		String prescReg = arguments.get(1);
  		String dataToSign = arguments.get(2);
  		result = controller.doSignatureInSession(sessionID, prescReg, dataToSign);
  	}  	
  	logger.info("SIGNATURE = " + result);
		return TAResponse.buildResponse(commandPacket).addPrimaryResponse(result);
  }
  
  private TAResponse commandQuit (TACommand commandPacket) {
  	if (commandPacket.getArguments().contains("shutdown")) {
  		Timer timer = new Timer(1000,new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					controller.shutdown();
				}
			});
  		timer.start();
  		return TAResponse.buildResponse(commandPacket).addPrimaryResponse("Closing Down");
  	} else if (commandPacket.getArguments().contains("signout")) {
			controller.closeKeystore();
  		return TAResponse.buildResponse(commandPacket).addPrimaryResponse("Session closed");
  	} else {
  		return TAResponse.buildResponse(commandPacket).addPrimaryResponse("Command ignored due to incorrect argument");
  	}
  	
  }
}
