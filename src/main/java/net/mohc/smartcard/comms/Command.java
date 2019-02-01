package net.mohc.smartcard.comms;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

public class Command {
  /**
   * This object contains all command actions as methods with format
   * void commandName(String msgData);
   */
  Object commandActions;
  HashMap<String, Method> hmMethods;
  String sError = "";

  private static final String METHOD_PREFIX = "command";

  public Command(Object commandActions) {
    this.commandActions = commandActions;
    init();
  }

  private void init () {
    String sf, sc;
    hmMethods = new HashMap<>();
    Class<?> c = commandActions.getClass();
    Method[] allMethods = c.getDeclaredMethods();
    for (int i = 0; i < allMethods.length; i++) {
      sf = allMethods[i].getName();
      if (sf.startsWith(METHOD_PREFIX)) {
        sc = sf.substring(METHOD_PREFIX.length());
        hmMethods.put(sc, allMethods[i]);
      }
    }
  }

  /**
   * Assumes message starts with one of the commands in one of these formats...
   */
  public boolean processMessage (String message, StringBuffer reply) {
    reply.setLength(0);
    Object oReply = null;
    String sCommand = "";
    String sData = "";
    sError = "";
    if (message == null) {
      sError = "Message was null";
      reply.append("ERROR: ");
      reply.append(sError);
      return false;
    }
    int idx_colon = message.indexOf(":");
    int idx_semi = message.indexOf(":");
    if ( idx_colon < 0 ) {
      sData = "";
      if (idx_semi < 0) {
        //ie command without colon or semi-colon
        sCommand = message;
      } else {
        //ie command only terminated with a semi-colon - rest of message ignored
        sCommand = message.substring(0, idx_semi);
      }
    } else {
      //got a message with format CMD:DATA
      sCommand = message.substring(0, idx_colon);
      if ((idx_colon+1) >= message.length()) {
        sData = "";
      } else {
        sData = message.substring(idx_colon+1);
      }
    }

    if ( sCommand.startsWith("Help") ||
         sCommand.startsWith("?") ) {

    }

    Method methodToInvoke = (Method)(hmMethods.get(sCommand));
    if (methodToInvoke == null) {
      sError = "Could not understand command \"" + sCommand + "\"";
      reply.append("ERROR: ");
      reply.append(sError);
      return false;
    }

    try {
      Object[] arguments = new Object[] {sData};             // Set up argument array
      oReply = methodToInvoke.invoke(this.commandActions, arguments);
    } catch (Exception ge) {
      sError = ge.getMessage();
      reply.append("ERROR: ");
      reply.append(sError);
      return false;
//      if(Rel.DEV)Db.out(CN, Dl.Error, "General exception: " + ge.toString());
    }
    if (oReply != null) {
//      if (oReply instanceof String) {
      reply.append(oReply.toString());
    } else {
      reply.append("OK");
    }
    return true;
  }

  /**
   * Returns last error message. This is cleared with each attempt at executing a command
   */
  public String getErrorMessage() {
    return sError;
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
