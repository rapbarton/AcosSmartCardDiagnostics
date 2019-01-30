package net.mohc.cardcomms;

/**
 * This is an interface that marks a class as being a command processor for
 * remote commands.
 * <br>
 * Classes implementing this interface should provide methods with a format thus:<br>
 * <br>
 * public void command<i>CommandName</i> (String sData);<br>
 *  or<br>
 * public String command<i>CommandName</i> (String sData);<br>
 * <br>
 * Where &quot;CommandName%quot; is the command exactly as required in a remote
 * command message. Commands are therefor case sensitive.<br><br>
 * In general, if a return string is specified, this is returned to the remote
 * controller as a reply.
 *
 * <br><br>
 * Copyright (c) 2002 MOHC Ltd. ALL RIGHTS RESERVED.
 * @author
 * @version 1.0
 */

public interface CommandProcessor {}

