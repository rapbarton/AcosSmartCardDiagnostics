package net.mohc.comms;

/**
 * This is an interface that defines a method to handle the processing of a
 * remote message reply.
 * <br>
 *
 * <br><br>
 * Copyright (c) 2002 MOHC Ltd. ALL RIGHTS RESERVED.
 * @author
 * @version 1.0
 */

public interface RemoteControlReplyHandler {
  /**
   * Message reply processing. Define this method to process replies.<br>
   * <b>Note:</b><br><ol>
   * <li>Any valid message received triggers the connected flag in the
   * RemoteControlClient class.</li>
   * <li>This method is called on the event dispatch thread and may be used to
   * safely update swing gui components</li>
   * </ol>
   * @param the message content
   */
  public void processReply (String msg);
}

