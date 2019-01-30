package net.mohc.cardcomms;

import java.io.OutputStream;

/**
 * This class is used to process messages used for remote control. <br>
 * The class is mutable in that the message represented may be changed as
 * required.<br><br>
 * <h2>Message format</h2>
 * The message format is as follows:<br>
 * &nbsp;&lt;TTRM&gt;<i>length</i>:<i>crc</i>:<i>body</i>&lt;/TTRM&gt;<br>
 * where:<br>
 * <ul>
 * <li><b>length</b> = the number of characters in the body of the message in hex</li>
 * <li><b>crc</b> = a 16 bit crc of the body in hex</li>
 * <li><b>body</b> = the body of the message</li>
 * </ul><br>
 *
 * <hr width="30%">
 * <h2>CRC</h2>
 * The CRC is 16 bit. It is calculated using the folowing code:<br>
 * <table border="0" width="100%" cellpadding="2" cellspacing="2">
 * <tbody><tr><td bgcolor="#00e0e0"><code><pre>
 *  int getCrc16(String s) {
 *    int crc = 0;
 *    if (s != null) {
 *      byte[] ba = s.getBytes();
 *      for (int i = 0; i < ba.length; i++) {
 *        int data = (int)ba[i];
 *        data <<= 8;
 *        crc = crc ^ data;
 *        for (int count = 8; count > 0; count--) {
 *          if (crc >= 0x08000) {
 *            crc <<= 1;
 *            crc ^= 0x01021;
 *          } else {
 *            crc <<= 1;
 *          }
 *          crc &= 0xffff;
 *        }
 *      }
 *    }
 *    return crc;
 *  }
 * </pre></code>
 * </td></tr></tbody></table>
 * <hr width="30%">
 * <h2>message body</h2>
 * See {@link com.timingtool.rc.RemoteControl RemoteControl} for details of the
 * messages themselves.
 *
 * <br><br>
 * Copyright (c) 2002 MOHC Ltd. ALL RIGHTS RESERVED.
 * @author
 * @version 1.0
 */

public class RemoteMessage {

  private static final String START_TAG = "<TTRM>";
  private static final String END_TAG = "</TTRM>";

  //Derived constants...
  private static final int START_TAG_LENGTH = START_TAG.length();
  private static final int END_TAG_LENGTH = END_TAG.length();

  private String sMessage = null;
  private String sError = "";
  
  /**
   * Creates class instance.
   */
  public RemoteMessage() {
  }

  /**
   * Scans a string buffer for a message. <br>
   * If complete message found then the message is removed from the buffer
   * along with any preceding rubbish.
   * @param inputBuffer a buffer containing a message, this buffer may be
   * affected by this method
   * @return the message or null if none found.
   */
  public String scan (StringBuffer inputBuffer) {
    String result = null;
    String sIn = inputBuffer.toString();
    int iTagStart = sIn.indexOf(START_TAG);

    if (iTagStart >= 0) {
      int iTagEnd = sIn.indexOf(END_TAG, iTagStart);
      if (iTagEnd >=0) {
        //Found a message, remove from buffer
        inputBuffer.delete(0, iTagEnd + END_TAG_LENGTH);
        //Strip start and end tags
        String msg = sIn.substring((iTagStart + START_TAG_LENGTH), iTagEnd );
        //remove header info
        result = removeHeader (msg);
        if (result == null) {
          System.out.println("Bad message received: " + this.sError);
        }
      }
    }
    this.sMessage = result;
    return result;
  }

  /**
   * Returns the message represented by this object
   */
  public String getMessage () {
    return this.sMessage;
  }

  /**
   * Sets the message represented by this object
   */
  public void setMessage (String message) {
    this.sMessage = message;
  }

  /**
   * Returns a formatted message created from the message represented by this
   * object suitable for sending to the main application.
   */
  public String getFormattedMessage () {
    return START_TAG + getHeader() + sMessage + END_TAG;
  }

  /**
   * Sends the message represented by this object onto the specified output stream.
   * The stream is automatically flushed.<br>
   * <b>Note:</b> Remember to set the message first!
   * @param os the output stream to use.
   * @return true if message sent successfully.
   */
  public boolean sendMessage (OutputStream os) {
    try {
      os.write(getFormattedMessage().getBytes());
      os.flush();
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  /**
   * Gets the message content and strips the header. Checks the checksum and
   * returns the message if all OK. If not a null is returned.
   */
  private String removeHeader(String msg) {
    String result = null;
    String sLength, sCrc, sBody, sRemains;
    int iLength, iCrc;
    sError = "bad message format";
    int ix = msg.indexOf(":");
    if (ix > 0) {
      sLength = msg.substring(0,ix++);
      sRemains = msg.substring(ix);

      ix = sRemains.indexOf(":");
      if (ix > 0) {
        sCrc = sRemains.substring(0,ix++);
        sBody = sRemains.substring(ix);

        try {
          iLength = Integer.parseInt(sLength, 16);
          iCrc = Integer.parseInt(sCrc, 16);

          if (sBody.length() != iLength) {
            sError = "message length incorrect";
          } else {
            if (iCrc != getCrc16(sBody)) {
              sError = "message crc failure";
            } else {
              result = sBody;
            }
          }
        } catch (NumberFormatException nfe) {
          sError = nfe.getMessage();
        }
      }
    }
    return result;
  }

  /**
   * Returns a header for the message represented by this object.<br>
   * <pre>
   * Format:
   *   <LEN>:<CHECK>:message
   *
   *   <LEN> = length of message not including header or start and end tags.
   *   <CHECK> = 4 hex digits crc
   *</pre>
   *<b>Note:</b><br>
   *This method returns the header ONLY.
   *@return a header for prefixing to message content
   */
  private String getHeader() {
    int len = sMessage.length();
    int crc = getCrc16(sMessage);
    return Integer.toHexString(len) + ":" + Integer.toHexString(crc) + ":";
  }

  /**
   * Calculate a 16 bit crc from string s
   */
  private int getCrc16(String s) {
    int crc = 0;
    if (s != null) {
      byte[] ba = s.getBytes();
      for (int i = 0; i < ba.length; i++) {
        int data = (int)ba[i];
        data <<= 8;
        crc = crc ^ data;
        for (int count = 8; count > 0; count--) {
          if (crc >= 0x08000) {
            crc <<= 1;
            crc ^= 0x01021;
          } else {
            crc <<= 1;
          }
          crc &= 0xffff;
        }
      }
    }
    return crc;
  }
/*
  private static long getCrc32(String s) {
    CRC32 crc = new CRC32();
    crc.reset();
    if (s != null) {
      crc.update(s.getBytes());
    }
    return crc.getValue();
  }
  */
}