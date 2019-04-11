package net.mohc.smartcard.comms;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSonUtilities {
	Logger logger;
	
	public static JSonUtilities getInstance() {
		return new JSonUtilities();
	}	
	
	private JSonUtilities () {
		logger = Logger.getLogger(JSonUtilities.class);
	}
	
	public String convertResponseToJson (TAResponse responsePacket) {
		String jsonString = "";
		ObjectMapper jsonParser = new ObjectMapper();
		try {
			jsonString = jsonParser.writeValueAsString(responsePacket);
		} catch (JsonProcessingException e) {
			logger.error("Failed to parse JSON: " + e.toString());
		}
		return jsonString;
	}
	
	public TAResponse convertResponseFromJson (String jsonResponse) {
		ObjectMapper jsonParser = new ObjectMapper();
		TAResponse response = null;
		try {
			response = jsonParser.readValue(jsonResponse, new TypeReference<TAResponse>(){});
		} catch (IOException e) {
			logger.warn("Don't understand response: \"" + jsonResponse + "\"");
		}
		return response;
	}

	public String convertCommandToJson (TACommand commandPacket) {
		String jsonString = "";
		ObjectMapper jsonParser = new ObjectMapper();
		try {
			jsonString = jsonParser.writeValueAsString(commandPacket);
		} catch (JsonProcessingException e) {
			logger.error("Failed to parse JSON: " + e.toString());
		}
		return jsonString;
	}
	
	public TACommand convertCommandFromJson (String jsonCommand) {
		ObjectMapper jsonParser = new ObjectMapper();
		TACommand command = null;
		try {
			command = jsonParser.readValue(jsonCommand, new TypeReference<TACommand>(){});
		} catch (IOException e) {
			logger.warn("Don't understand command: \"" + jsonCommand + "\"");
		}
		return command;
	}

}
