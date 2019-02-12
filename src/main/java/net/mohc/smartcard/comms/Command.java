package net.mohc.smartcard.comms;

import java.util.ArrayList;

public class Command {
	CommandProcessor commandProcessor;
	JSonUtilities jsonUtils;

  public Command(CommandProcessor commandProcessor) {
    this.commandProcessor = commandProcessor;
    this.jsonUtils = JSonUtilities.getInstance();
  }

  /**
   * Assumes message is a json encoded command
   */
  public boolean processMessage (String message, StringBuffer reply) {
  	TACommand taCommand = jsonUtils.convertCommandFromJson(message);
  	reply.setLength(0);
    if (taCommand == null) {
    	reply.append("ERROR: Message was not understood");
      return false;
    }
    TAResponse response = commandProcessor.processCommand(taCommand);
    reply.append(jsonUtils.convertResponseToJson(response));
    return true;
  }

  /**
   * Converts a data string into a string array
   * @param bEscape set true to use escape characters
   */
  public static String[] convertToArgArray (String sData, boolean bEscape) {
    ArrayList<String> v = new ArrayList<>();
    String sCmdLine = sData.trim();
    boolean flagQuote = false;
    boolean flagEscape = false;
    boolean flagSeparator = false;
    StringBuffer argBuff = new StringBuffer();
    int index = 0;
    char chr;
    while (index < sCmdLine.length()) {
      chr = sCmdLine.charAt(index++);

      if (chr != ' ') {
        //Always reset the separator flag when not separating
        flagSeparator = false;
      }

      if (flagEscape) {
        //Character was escaped
        argBuff.append(chr);
        flagEscape = false;
        continue;
      }
/** @todo >>>CHECK INTO THIS, SLASHS ARE PROCESSED<<< */
      if ((chr == '\\') && bEscape ) {
        //Escape chr - the next character is forced
        flagEscape = true;
        continue;
      }

      if (flagQuote) {
        //In quotes...
        if (chr == '"') {
          flagQuote = false;
        } else {
          argBuff.append(chr);
        }
      } else {
        //Not in quotes
        if (chr == ';') {
          //Abort string processing
          break;
        } else if (chr == '"') {
          //Start quotes
          flagQuote = true;
        } else if (chr == ' ') {
          if (!flagSeparator) {
            //Only separate if not already separating...
            v.add(argBuff.toString());
            argBuff.setLength(0);
            flagSeparator = true;
          }
        } else {
          argBuff.append(chr);
        }
      }
    }

    if (argBuff.length() > 0) {
      v.add(argBuff.toString());
    }

    String[] args = new String[v.size()];
    for (int j = 0; j < v.size(); j++) {
      args[j] = (String)(v.get(j));
    }
    return args;

  }
}
